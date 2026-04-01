package lineage.processor;

import lineage.annotation.AccessMode;
import lineage.annotation.BizOp;
import lineage.annotation.Effect;
import lineage.annotation.EffectType;
import lineage.annotation.GraphIgnore;
import lineage.annotation.RelationType;
import lineage.annotation.RuleHint;
import lineage.export.JsonGraphExporter;
import lineage.export.MermaidGraphExporter;
import lineage.model.EdgeType;
import lineage.model.GraphEdge;
import lineage.model.GraphModel;
import lineage.model.GraphNode;
import lineage.model.NodeType;
import lineage.model.TouchAccess;
import lineage.support.BusinessException;
import lineage.support.StringUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 编译期业务关系图谱处理器。
 */
@SupportedAnnotationTypes({
        "lineage.annotation.BizOp",
        "lineage.annotation.Effect",
        "lineage.annotation.Effects",
        "lineage.annotation.RuleHint",
        "lineage.annotation.RuleHints",
        "lineage.annotation.GraphIgnore"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({
        "lineage.output",
        "lineage.minConfidence",
        "lineage.strict",
        "lineage.outputDir"
})
public class LineageProcessor extends AbstractProcessor {
    private static final String OUTPUT_JSON = "json";
    private static final String OUTPUT_MERMAID = "mermaid";

    private final GraphModel graphModel = new GraphModel();
    private final Map<String, Set<String>> eventEmitters = new HashMap<>();
    private final Map<String, Set<String>> eventConsumers = new HashMap<>();
    private final Map<String, Set<String>> writeTouchByResourceAndKey = new HashMap<>();

    private Messager messager;
    private Filer filer;
    private double minConfidence;
    private boolean strict;
    private String output;
    private String outputDir;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.filer = processingEnv.getFiler();
        this.output = processingEnv.getOptions().getOrDefault("lineage.output", "json,mermaid");
        this.outputDir = processingEnv.getOptions().getOrDefault("lineage.outputDir", "target/lineage");
        this.minConfidence = parseDouble(processingEnv.getOptions().get("lineage.minConfidence"), 0.6D);
        this.strict = Boolean.parseBoolean(processingEnv.getOptions().getOrDefault("lineage.strict", "false"));
    }

    @Override
    public boolean process(Set<? extends javax.lang.model.element.TypeElement> annotations, RoundEnvironment roundEnv) {
        collectBizOps(roundEnv);

        if (roundEnv.processingOver()) {
            derivePrecedesFromEvents();
            deriveMutexFromWriteConflicts();
            exportArtifacts();
        }
        return false;
    }

    private void collectBizOps(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(BizOp.class)) {
            if (element.getKind() != ElementKind.METHOD) {
                warn("@BizOp 只能标注在方法上: " + element);
                continue;
            }
            if (!Objects.isNull(element.getAnnotation(GraphIgnore.class))) {
                continue;
            }

            ExecutableElement method = (ExecutableElement) element;
            BizOp bizOp = method.getAnnotation(BizOp.class);
            if (Objects.isNull(bizOp)) {
                continue;
            }
            if (StringUtils.isBlank(bizOp.id())) {
                warn("发现空操作 id: " + method);
                continue;
            }

            String opId = opNodeId(bizOp.id());
            String opName = StringUtils.isBlank(bizOp.name()) ? bizOp.id() : bizOp.name();
            graphModel.addNode(new GraphNode(opId, opName, NodeType.OPERATION));

            Effect[] effects = method.getAnnotationsByType(Effect.class);
            RuleHint[] hints = method.getAnnotationsByType(RuleHint.class);

            if ((Objects.isNull(effects) || effects.length == 0) && (Objects.isNull(hints) || hints.length == 0)) {
                addWarning("操作缺少关系证据: " + bizOp.id() + " at " + method);
            }

            collectEffects(bizOp.id(), method, effects);
            collectHints(bizOp.id(), method, hints);
        }
    }

    private void collectEffects(String sourceOpId, ExecutableElement method, Effect[] effects) {
        if (Objects.isNull(effects)) {
            return;
        }
        for (Effect effect : effects) {
            if (Objects.isNull(effect) || StringUtils.isBlank(effect.target())) {
                addWarning("Effect.target 为空，已忽略: " + method);
                continue;
            }
            if (effect.type() == EffectType.EMIT) {
                String eventNodeId = eventNodeId(effect.target());
                graphModel.addNode(new GraphNode(eventNodeId, effect.target(), NodeType.EVENT));
                graphModel.addEdge(new GraphEdge(opNodeId(sourceOpId), eventNodeId, EdgeType.EMITS, 1.0D,
                        "effect:emit", TouchAccess.NONE, ""));
                eventEmitters.computeIfAbsent(effect.target(), k -> new LinkedHashSet<>()).add(opNodeId(sourceOpId));
            }

            if (effect.type() == EffectType.CONSUME) {
                String eventNodeId = eventNodeId(effect.target());
                graphModel.addNode(new GraphNode(eventNodeId, effect.target(), NodeType.EVENT));
                graphModel.addEdge(new GraphEdge(eventNodeId, opNodeId(sourceOpId), EdgeType.CONSUMES, 1.0D,
                        "effect:consume", TouchAccess.NONE, ""));
                eventConsumers.computeIfAbsent(effect.target(), k -> new LinkedHashSet<>()).add(opNodeId(sourceOpId));
            }

            if (effect.type() == EffectType.TOUCH) {
                String resourceNodeId = resourceNodeId(effect.target());
                graphModel.addNode(new GraphNode(resourceNodeId, effect.target(), NodeType.RESOURCE));
                TouchAccess access = convertAccess(effect.access());
                String bizKey = effect.key();
                graphModel.addEdge(new GraphEdge(opNodeId(sourceOpId), resourceNodeId, EdgeType.TOUCHES, 1.0D,
                        "effect:touch", access, bizKey));

                if (access == TouchAccess.WRITE || access == TouchAccess.RW) {
                    if (StringUtils.isBlank(bizKey)) {
                        addWarning("WRITE/RW TOUCH 缺少 key，无法参与 MUTEX 推理: op=" + sourceOpId + ", target=" + effect.target());
                    } else {
                        String indexKey = effect.target() + "::" + bizKey;
                        writeTouchByResourceAndKey.computeIfAbsent(indexKey, k -> new LinkedHashSet<>())
                                .add(opNodeId(sourceOpId));
                    }
                }
            }
        }
    }

    private void collectHints(String sourceOpId, ExecutableElement method, RuleHint[] hints) {
        if (Objects.isNull(hints)) {
            return;
        }
        for (RuleHint hint : hints) {
            if (Objects.isNull(hint) || StringUtils.isBlank(hint.targetOpId())) {
                addWarning("RuleHint.targetOpId 为空，已忽略: " + method);
                continue;
            }
            String from = opNodeId(sourceOpId);
            String to = opNodeId(hint.targetOpId());
            graphModel.addNode(new GraphNode(to, hint.targetOpId(), NodeType.OPERATION));

            EdgeType edgeType = hint.type() == RelationType.PRECEDES ? EdgeType.PRECEDES : EdgeType.MUTEX;
            String evidence = StringUtils.isBlank(hint.reason()) ? "rule-hint" : "rule-hint:" + hint.reason();
            graphModel.addEdge(new GraphEdge(from, to, edgeType, 1.0D, evidence, TouchAccess.NONE, ""));
        }
    }

    private void derivePrecedesFromEvents() {
        for (Map.Entry<String, Set<String>> entry : eventEmitters.entrySet()) {
            String eventName = entry.getKey();
            Set<String> emitOps = entry.getValue();
            Set<String> consumeOps = eventConsumers.get(eventName);
            if (Objects.isNull(consumeOps) || consumeOps.isEmpty()) {
                addWarning("事件无消费方: " + eventName);
                continue;
            }

            for (String emitOp : emitOps) {
                for (String consumeOp : consumeOps) {
                    if (emitOp.equals(consumeOp)) {
                        continue;
                    }
                    graphModel.addEdge(new GraphEdge(emitOp, consumeOp, EdgeType.PRECEDES,
                            0.9D, "rule:event-chain", TouchAccess.NONE, ""));
                }
            }
        }
    }

    private void deriveMutexFromWriteConflicts() {
        for (Map.Entry<String, Set<String>> entry : writeTouchByResourceAndKey.entrySet()) {
            List<String> ops = new ArrayList<>(entry.getValue());
            if (ops.size() < 2) {
                continue;
            }
            ops.sort(Comparator.naturalOrder());
            for (int i = 0; i < ops.size(); i++) {
                for (int j = i + 1; j < ops.size(); j++) {
                    graphModel.addEdge(new GraphEdge(ops.get(i), ops.get(j), EdgeType.MUTEX,
                            0.85D, "rule:write-conflict", TouchAccess.NONE, ""));
                }
            }
        }
    }

    private void exportArtifacts() {
        try {
            if (output.contains(OUTPUT_JSON)) {
                writeJsonToClassOutput();
                writeJsonToTargetDir();
            }
            if (output.contains(OUTPUT_MERMAID)) {
                writeMermaidToTargetDir();
            }
        } catch (Exception e) {
            error("导出图谱失败: " + e.getMessage());
        }

        for (String warning : graphModel.getWarnings()) {
            warn(warning);
        }
    }

    private void writeJsonToClassOutput() throws IOException {
        String json = JsonGraphExporter.export(graphModel, minConfidence);
        FileObject resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/lineage/graph.json");
        try (Writer writer = resource.openWriter()) {
            writer.write(json);
        }
    }

    private void writeJsonToTargetDir() throws IOException {
        String json = JsonGraphExporter.export(graphModel, minConfidence);
        Path dir = Paths.get(outputDir);
        Files.createDirectories(dir);
        Files.write(dir.resolve("graph.json"), json.getBytes(StandardCharsets.UTF_8));
    }

    private void writeMermaidToTargetDir() throws IOException {
        String mmd = MermaidGraphExporter.export(graphModel, minConfidence);
        Path dir = Paths.get(outputDir);
        Files.createDirectories(dir);
        Files.write(dir.resolve("graph.mmd"), mmd.getBytes(StandardCharsets.UTF_8));
    }

    private String opNodeId(String opId) {
        BusinessException.isEmpty(opId, "opId must not be empty");
        return "op:" + opId;
    }

    private String eventNodeId(String eventName) {
        BusinessException.isEmpty(eventName, "eventName must not be empty");
        return "event:" + eventName;
    }

    private String resourceNodeId(String resourceName) {
        BusinessException.isEmpty(resourceName, "resourceName must not be empty");
        return "resource:" + resourceName;
    }

    private TouchAccess convertAccess(AccessMode mode) {
        if (Objects.isNull(mode)) {
            return TouchAccess.NONE;
        }
        if (mode == AccessMode.READ) {
            return TouchAccess.READ;
        }
        if (mode == AccessMode.WRITE) {
            return TouchAccess.WRITE;
        }
        if (mode == AccessMode.RW) {
            return TouchAccess.RW;
        }
        return TouchAccess.NONE;
    }

    private void addWarning(String message) {
        graphModel.addWarning(message);
        if (strict) {
            error(message);
        }
    }

    private void warn(String message) {
        messager.printMessage(Diagnostic.Kind.WARNING, message);
    }

    private void error(String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message);
    }

    private double parseDouble(String value, double defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}

package lineage.export;

import lineage.model.GraphEdge;
import lineage.model.GraphModel;
import lineage.model.GraphNode;
import lineage.support.BusinessException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * JSON 导出器。
 */
public final class JsonGraphExporter {
    private JsonGraphExporter() {
    }

    public static String export(GraphModel model, double minConfidence) {
        BusinessException.isEmpty(model, "graph model must not be empty");
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        List<GraphNode> nodes = new ArrayList<>(model.getNodes());
        sb.append("  \"nodes\": [\n");
        for (int i = 0; i < nodes.size(); i++) {
            GraphNode node = nodes.get(i);
            sb.append("    {\"id\":\"").append(escape(node.getId()))
                    .append("\",\"name\":\"").append(escape(node.getName()))
                    .append("\",\"type\":\"").append(node.getType().name())
                    .append("\"}");
            if (i < nodes.size() - 1) {
                sb.append(',');
            }
            sb.append("\n");
        }
        sb.append("  ],\n");

        List<GraphEdge> edges = new ArrayList<>(model.getEdges());
        sb.append("  \"edges\": [\n");
        boolean first = true;
        for (GraphEdge edge : edges) {
            if (edge.getConfidence() < minConfidence) {
                continue;
            }
            if (!first) {
                sb.append(",\n");
            }
            first = false;
            sb.append("    {\"from\":\"").append(escape(edge.getFrom()))
                    .append("\",\"to\":\"").append(escape(edge.getTo()))
                    .append("\",\"type\":\"").append(edge.getType().name())
                    .append("\",\"confidence\":")
                    .append(String.format(Locale.ROOT, "%.2f", edge.getConfidence()))
                    .append(",\"evidence\":\"").append(escape(edge.getEvidence())).append("\"");
            if (!Objects.isNull(edge.getAccess())) {
                sb.append(",\"access\":\"").append(edge.getAccess().name()).append("\"");
            }
            if (!Objects.isNull(edge.getBizKey())) {
                sb.append(",\"bizKey\":\"").append(escape(edge.getBizKey())).append("\"");
            }
            sb.append("}");
        }
        if (!first) {
            sb.append("\n");
        }
        sb.append("  ],\n");

        sb.append("  \"warnings\": [\n");
        List<String> warnings = model.getWarnings();
        for (int i = 0; i < warnings.size(); i++) {
            sb.append("    \"").append(escape(warnings.get(i))).append("\"");
            if (i < warnings.size() - 1) {
                sb.append(',');
            }
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String escape(String value) {
        if (Objects.isNull(value)) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

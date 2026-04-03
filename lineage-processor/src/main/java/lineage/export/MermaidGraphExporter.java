package lineage.export;

import lineage.model.EdgeType;
import lineage.model.GraphEdge;
import lineage.model.GraphModel;
import lineage.model.GraphNode;
import lineage.support.BusinessException;

import java.util.ArrayList;
import java.util.List;

/**
 * Mermaid 导出器。
 */
public final class MermaidGraphExporter {
    private MermaidGraphExporter() {
    }

    /**
     * 导出为 Mermaid 文本。
     * 仅输出置信度 >= minConfidence 的边。
     */
    public static String export(GraphModel model, double minConfidence) {
        BusinessException.isEmpty(model, "graph model must not be empty");
        StringBuilder sb = new StringBuilder();
        sb.append("flowchart LR\n");

        List<GraphNode> nodes = new ArrayList<>(model.getNodes());
        for (GraphNode node : nodes) {
            String nodeId = toMermaidId(node.getId());
            sb.append("  ").append(nodeId).append("[")
                    .append(escape(node.getName())).append("]\n");
        }

        List<GraphEdge> edges = new ArrayList<>(model.getEdges());
        int styleIndex = 0;
        for (GraphEdge edge : edges) {
            if (edge.getConfidence() < minConfidence) {
                continue;
            }
            String from = toMermaidId(edge.getFrom());
            String to = toMermaidId(edge.getTo());
            sb.append("  ").append(from)
                    .append(linkByType(edge.getType()))
                    .append(to)
                    .append(":::edge").append(edge.getType().name().toLowerCase())
                    .append("\n");
            sb.append("  linkStyle ").append(styleIndex).append(" ")
                    .append(linkStyle(edge.getType())).append("\n");
            styleIndex++;
        }

        sb.append("  classDef edgeprecedes color:#1d4ed8,stroke:#1d4ed8;\n");
        sb.append("  classDef edgemutex color:#dc2626,stroke:#dc2626,stroke-dasharray: 5 5;\n");
        sb.append("  classDef edgetouches color:#6b7280,stroke:#6b7280,stroke-dasharray: 2 4;\n");
        sb.append("  classDef edgeemits color:#6b7280,stroke:#6b7280,stroke-dasharray: 2 4;\n");
        sb.append("  classDef edgeconsumes color:#6b7280,stroke:#6b7280,stroke-dasharray: 2 4;\n");
        return sb.toString();
    }

    private static String toMermaidId(String rawId) {
        return "n_" + rawId.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private static String linkByType(EdgeType type) {
        if (type == EdgeType.MUTEX) {
            return " -. MUTEX .- ";
        }
        return " --> ";
    }

    private static String linkStyle(EdgeType type) {
        if (type == EdgeType.PRECEDES) {
            return "stroke:#1d4ed8,color:#1d4ed8";
        }
        if (type == EdgeType.MUTEX) {
            return "stroke:#dc2626,color:#dc2626,stroke-dasharray:5 5";
        }
        return "stroke:#6b7280,color:#6b7280,stroke-dasharray:2 4";
    }

    private static String escape(String value) {
        return value.replace("\"", "'");
    }
}

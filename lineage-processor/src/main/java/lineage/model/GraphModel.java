package lineage.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 图谱整体模型。
 */
public final class GraphModel {
    private final Set<GraphNode> nodes = new LinkedHashSet<>();
    private final Set<GraphEdge> edges = new LinkedHashSet<>();
    private final List<String> warnings = new ArrayList<>();

    public Set<GraphNode> getNodes() {
        return nodes;
    }

    public Set<GraphEdge> getEdges() {
        return edges;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void addNode(GraphNode node) {
        nodes.add(node);
    }

    public void addEdge(GraphEdge edge) {
        edges.add(edge);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }
}

package lineage.model;

import java.util.Objects;

/**
 * 图谱边。
 */
public final class GraphEdge {
    private final String from;
    private final String to;
    private final EdgeType type;
    private final double confidence;
    private final String evidence;
    private final TouchAccess access;
    private final String bizKey;

    public GraphEdge(String from, String to, EdgeType type, double confidence, String evidence, TouchAccess access, String bizKey) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.confidence = confidence;
        this.evidence = evidence;
        this.access = access;
        this.bizKey = bizKey;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public EdgeType getType() {
        return type;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getEvidence() {
        return evidence;
    }

    public TouchAccess getAccess() {
        return access;
    }

    public String getBizKey() {
        return bizKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GraphEdge)) {
            return false;
        }
        GraphEdge graphEdge = (GraphEdge) o;
        return Objects.equals(from, graphEdge.from)
                && Objects.equals(to, graphEdge.to)
                && type == graphEdge.type
                && access == graphEdge.access
                && Objects.equals(bizKey, graphEdge.bizKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, type, access, bizKey);
    }
}

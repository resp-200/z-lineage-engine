package lineage.model;

/**
 * 图谱边类型。
 */
public enum EdgeType {
    /**
     * 操作发出事件。
     */
    EMITS,

    /**
     * 操作消费事件。
     */
    CONSUMES,

    /**
     * 操作触达资源。
     */
    TOUCHES,

    /**
     * 操作先后关系。
     */
    PRECEDES,

    /**
     * 操作互斥关系。
     */
    MUTEX
}

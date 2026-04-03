package lineage.annotation;

/**
 * 关系类型。
 */
public enum RelationType {
    /**
     * 先后关系，前者应先于后者发生。
     */
    PRECEDES,

    /**
     * 互斥关系，两者不可同时成立。
     */
    MUTEX
}

package lineage.model;

/**
 * TOUCHES 边访问模式。
 */
public enum TouchAccess {
    /**
     * 未指定访问方式。
     */
    NONE,

    /**
     * 只读访问。
     */
    READ,

    /**
     * 只写访问。
     */
    WRITE,

    /**
     * 读写访问。
     */
    RW
}

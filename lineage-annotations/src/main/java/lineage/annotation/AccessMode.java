package lineage.annotation;

/**
 * 资源访问方式。
 */
public enum AccessMode {
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

package lineage.annotation;

/**
 * 关系证据类型。
 */
public enum EffectType {
    /**
     * 操作发出事件。
     */
    EMIT,

    /**
     * 操作消费事件。
     */
    CONSUME,

    /**
     * 操作触达资源。
     */
    TOUCH
}

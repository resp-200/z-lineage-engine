package lineage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明关系推理证据。
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@Repeatable(Effects.class)
public @interface Effect {
    /**
     * 证据类型。
     */
    EffectType type();

    /**
     * 目标名称（事件或资源）。
     */
    String target();

    /**
     * 资源访问方式，仅 TOUCH 时生效。
     */
    AccessMode access() default AccessMode.NONE;

    /**
     * 业务 key，用于冲突判定。
     */
    String key() default "";
}

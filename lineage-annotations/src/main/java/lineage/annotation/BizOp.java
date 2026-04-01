package lineage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记业务操作入口。
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface BizOp {
    /**
     * 操作唯一标识。
     */
    String id();

    /**
     * 业务域。
     */
    String domain() default "default";

    /**
     * 展示名称。
     */
    String name() default "";
}

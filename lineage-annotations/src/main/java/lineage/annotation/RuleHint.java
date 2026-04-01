package lineage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 人工兜底关系提示。
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@Repeatable(RuleHints.class)
public @interface RuleHint {
    /**
     * 关系类型，仅支持 PRECEDES/MUTEX。
     */
    RelationType type();

    /**
     * 目标操作 id。
     */
    String targetOpId();

    /**
     * 关系原因。
     */
    String reason() default "";
}

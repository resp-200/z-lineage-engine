package lineage.support;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * 简单业务异常。
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    /**
     * 为空校验，不满足时抛业务异常。
     */
    public static void isEmpty(Object value, String message) {
        if (Objects.isNull(value)) {
            throw new BusinessException(message);
        }
        if (value instanceof String && StringUtils.isBlank((String) value)) {
            throw new BusinessException(message);
        }
        if (value instanceof Collection && ((Collection<?>) value).isEmpty()) {
            throw new BusinessException(message);
        }
        if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) {
            throw new BusinessException(message);
        }
    }
}

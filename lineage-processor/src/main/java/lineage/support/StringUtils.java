package lineage.support;

import java.util.Objects;

/**
 * 字符串工具。
 */
public final class StringUtils {
    private StringUtils() {
    }

    /**
     * 判断字符串是否为空白。
     */
    public static boolean isBlank(String value) {
        return Objects.isNull(value) || value.trim().isEmpty();
    }
}

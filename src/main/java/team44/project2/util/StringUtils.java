package team44.project2.util;
/**
 * Utility class providing common string-manipulation helpers used throughout the
 * application. This class cannot be instantiated.
 */
public final class StringUtils {
    private StringUtils() {
    }

    /**
     * Converts a string to title case by upper-casing the first character and
     * lower-casing the rest.
     *
     * @param str The input string, or {@code null}.
     * @return The title-cased string, or the original value if it is {@code null} or
     *         empty.
     */
    public static String toTitleCase(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
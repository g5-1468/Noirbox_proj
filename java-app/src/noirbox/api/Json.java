package noirbox.api;

import java.util.Collection;
import java.util.List;

/**
 * Very small JSON writer.
 * We only ever *write* JSON (the browser sends form data), so no JSON
 * parser or external library is needed in this project.
 */
public final class Json {

    private Json() {
    }

    /** Returns the text in double quotes with the special characters escaped. */
    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    /** ["a","b"] from plain strings. */
    public static String strings(Collection<String> values) {
        return values.stream()
                .map(Json::quote)
                .reduce((a, b) -> a + "," + b)
                .map(joined -> "[" + joined + "]")
                .orElse("[]");
    }

    /** Joins already-serialised objects into an array. */
    public static String array(List<String> items) {
        return items.stream()
                .reduce((a, b) -> a + "," + b)
                .map(joined -> "[" + joined + "]")
                .orElse("[]");
    }

    /** 8.0 -> 8.0, 8.43 -> 8.43, 0.0 -> 0.0 (never 8.0E0). */
    public static String number(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(Math.round(value * 100.0) / 100.0);
    }
}

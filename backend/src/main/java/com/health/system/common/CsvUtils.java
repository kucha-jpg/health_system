package com.health.system.common;

import java.nio.charset.StandardCharsets;

public final class CsvUtils {

    private CsvUtils() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "\"\"";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    public static byte[] utf8WithBom(CharSequence csvContent) {
        return ("\uFEFF" + csvContent).getBytes(StandardCharsets.UTF_8);
    }
}
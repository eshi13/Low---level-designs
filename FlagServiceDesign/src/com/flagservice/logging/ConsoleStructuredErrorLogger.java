package com.flagservice.logging;

import java.util.Map;
import java.util.stream.Collectors;

public final class ConsoleStructuredErrorLogger implements StructuredErrorLogger {
    @Override
    public void error(String message, Map<String, Object> fields, Throwable cause) {
        String fieldStr = fields == null || fields.isEmpty()
                ? "{}"
                : fields.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
        System.err.println("[FLAG_ERROR] " + message + " " + fieldStr
                + (cause == null ? "" : " cause=" + cause));
    }
}

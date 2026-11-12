package com.flagservice.logging;

import java.util.Map;

public interface StructuredErrorLogger {
    void error(String message, Map<String, Object> fields, Throwable cause);
}

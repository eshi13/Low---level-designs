package com.flagservice.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class EvaluationContext {
    private final Map<String, Object> attributes;

    public EvaluationContext(Map<String, Object> attributes) {
        this.attributes = Collections.unmodifiableMap(new HashMap<>(
                attributes == null ? Map.of() : attributes
        ));
    }

    public static EvaluationContext of(Map<String, Object> attributes) {
        return new EvaluationContext(attributes);
    }

    public Optional<Object> get(String attribute) {
        return Optional.ofNullable(attributes.get(attribute));
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}

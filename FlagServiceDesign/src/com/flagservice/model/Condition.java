package com.flagservice.model;

import java.util.Objects;

public final class Condition {
    private final String attribute;
    private final Operator operator;
    private final Object expected;

    public Condition(String attribute, Operator operator, Object expected) {
        if (attribute == null || attribute.isBlank()) {
            throw new IllegalArgumentException("condition attribute must be non-blank");
        }
        this.attribute = attribute;
        this.operator = Objects.requireNonNull(operator, "operator");
        this.expected = Objects.requireNonNull(expected, "expected");
    }

    public String getAttribute() {
        return attribute;
    }

    public Operator getOperator() {
        return operator;
    }

    public Object getExpected() {
        return expected;
    }
}

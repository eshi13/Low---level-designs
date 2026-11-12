package com.flagservice.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Rule {
    private final List<Condition> conditions;
    private final Object value;

    public Rule(List<Condition> conditions, Object value) {
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("rule must have at least one condition");
        }
        this.conditions = Collections.unmodifiableList(new ArrayList<>(conditions));
        this.value = Objects.requireNonNull(value, "rule value");
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public Object getValue() {
        return value;
    }
}

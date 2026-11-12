package com.flagservice.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Flag {
    private final String name;
    private final String env;
    private final ValueType valueType;
    private final Object defaultValue;
    private final List<Rule> rules;
    private final PercentageRollout rollout;

    public Flag(
            String name,
            String env,
            ValueType valueType,
            Object defaultValue,
            List<Rule> rules,
            PercentageRollout rollout
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("flag name must be non-blank");
        }
        if (env == null || env.isBlank()) {
            throw new IllegalArgumentException("flag env must be non-blank");
        }
        this.name = name;
        this.env = env;
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        validateType(defaultValue, "defaultValue");
        List<Rule> ruleList = rules == null ? List.of() : new ArrayList<>(rules);
        for (int i = 0; i < ruleList.size(); i++) {
            Rule rule = Objects.requireNonNull(ruleList.get(i), "rule[" + i + "]");
            validateType(rule.getValue(), "rule[" + i + "].value");
        }
        this.rules = Collections.unmodifiableList(ruleList);
        if (rollout != null) {
            validateType(rollout.getRolloutValue(), "rollout.rolloutValue");
        }
        this.rollout = rollout;
    }

    public Flag withAddedRule(Rule rule) {
        List<Rule> updated = new ArrayList<>(rules);
        updated.add(Objects.requireNonNull(rule, "rule"));
        return new Flag(name, env, valueType, defaultValue, updated, rollout);
    }

    public Flag withRollout(PercentageRollout newRollout) {
        return new Flag(name, env, valueType, defaultValue, rules, newRollout);
    }

    private void validateType(Object value, String field) {
        boolean ok = switch (valueType) {
            case BOOLEAN -> value instanceof Boolean;
            case STRING -> value instanceof String;
            case INTEGER -> value instanceof Integer;
        };
        if (!ok) {
            throw new IllegalArgumentException(
                    field + " must be of type " + valueType + " but was "
                            + (value == null ? "null" : value.getClass().getSimpleName())
            );
        }
    }

    public String getName() {
        return name;
    }

    public String getEnv() {
        return env;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public PercentageRollout getRollout() {
        return rollout;
    }
}

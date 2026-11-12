package com.flagservice;

import com.flagservice.eval.FlagEvaluator;
import com.flagservice.logging.ConsoleStructuredErrorLogger;
import com.flagservice.logging.StructuredErrorLogger;
import com.flagservice.model.EvaluationContext;
import com.flagservice.model.Flag;
import com.flagservice.model.Rule;
import com.flagservice.store.ConfigStore;
import com.flagservice.store.InMemoryConfigStore;
import com.flagservice.store.InvalidFlagConfigException;

import java.util.Map;
import java.util.Objects;

/**
 * Facade / single orchestrator for configuring and evaluating feature flags.
 */
public final class FlagService {
    private final ConfigStore store;
    private final FlagEvaluator evaluator;

    public FlagService() {
        this(new InMemoryConfigStore(), new ConsoleStructuredErrorLogger());
    }

    public FlagService(ConfigStore store, StructuredErrorLogger logger) {
        this.store = Objects.requireNonNull(store, "store");
        this.evaluator = new FlagEvaluator(store, Objects.requireNonNull(logger, "logger"));
    }

    public void addOrUpdateFlag(Flag flag) {
        store.set(flag);
    }

    public void addRule(String env, String flagName, Rule rule) {
        Flag existing = store.get(flagName, env)
                .orElseThrow(() -> new InvalidFlagConfigException(
                        "cannot add rule; flag not found: " + env + "/" + flagName));
        store.set(existing.withAddedRule(rule));
    }

    /**
     * Never throws. Returns the flag's default on internal error, or null if the flag is missing.
     */
    public Object evaluate(String flagName, String env, EvaluationContext context) {
        return evaluator.evaluate(flagName, env, context);
    }

    public Object evaluate(String flagName, String env, Map<String, Object> attributes) {
        return evaluate(flagName, env, EvaluationContext.of(attributes));
    }

    public boolean evaluateBoolean(String flagName, String env, EvaluationContext context) {
        Object value = evaluate(flagName, env, context);
        return value instanceof Boolean b ? b : false;
    }

    public String evaluateString(String flagName, String env, EvaluationContext context) {
        Object value = evaluate(flagName, env, context);
        return value instanceof String s ? s : null;
    }

    public Integer evaluateInteger(String flagName, String env, EvaluationContext context) {
        Object value = evaluate(flagName, env, context);
        return value instanceof Integer i ? i : null;
    }
}

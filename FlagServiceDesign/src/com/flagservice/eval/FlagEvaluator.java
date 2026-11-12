package com.flagservice.eval;

import com.flagservice.eval.bucketing.BucketingStrategy;
import com.flagservice.eval.bucketing.SharedBucketingStrategy;
import com.flagservice.eval.bucketing.StrictBucketingStrategy;
import com.flagservice.logging.StructuredErrorLogger;
import com.flagservice.model.BucketingMode;
import com.flagservice.model.EvaluationContext;
import com.flagservice.model.Flag;
import com.flagservice.model.PercentageRollout;
import com.flagservice.model.Rule;
import com.flagservice.store.ConfigStore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class FlagEvaluator {
    private final ConfigStore store;
    private final StructuredErrorLogger logger;
    private final ConditionMatcher conditionMatcher;
    private final BucketingStrategy strictBucketing;
    private final BucketingStrategy sharedBucketing;

    public FlagEvaluator(ConfigStore store, StructuredErrorLogger logger) {
        this(store, logger, new ConditionMatcher(),
                new StrictBucketingStrategy(), new SharedBucketingStrategy());
    }

    FlagEvaluator(
            ConfigStore store,
            StructuredErrorLogger logger,
            ConditionMatcher conditionMatcher,
            BucketingStrategy strictBucketing,
            BucketingStrategy sharedBucketing
    ) {
        this.store = store;
        this.logger = logger;
        this.conditionMatcher = conditionMatcher;
        this.strictBucketing = strictBucketing;
        this.sharedBucketing = sharedBucketing;
    }

    /**
     * Never throws. Returns flag default on internal error, or null if the flag is missing.
     */
    public Object evaluate(String flagName, String env, EvaluationContext context) {
        Optional<Flag> maybeFlag = store.get(flagName, env);
        
        if (maybeFlag.isEmpty()) {
            logger.error(
                    "flag not found",
                    fields(flagName, env, null),
                    null
            );
            return null;
        }

        Flag flag = maybeFlag.get();
        if(killSwitch) {
            return flag.getDefaultVlue();
        }
        try {
            return evaluateInternal(flag, context == null ? EvaluationContext.of(Map.of()) : context);
        } catch (Throwable t) {
            logger.error(
                    "evaluation failed; returning default",
                    fields(flagName, env, t.getMessage()),
                    t
            );
            return flag.getDefaultValue();
        }
    }

    private Object evaluateInternal(Flag flag, EvaluationContext context) {
        for (Rule rule : flag.getRules()) {
            if (matchesAll(rule, context)) {
                return rule.getValue();
            }
        }

        PercentageRollout rollout = flag.getRollout();
        if (rollout != null) {
            BucketingStrategy strategy = rollout.getMode() == BucketingMode.SHARED
                    ? sharedBucketing
                    : strictBucketing;
            int bucket = strategy.bucket(flag, rollout, context);
            if (bucket < rollout.getPercent()) {
                return rollout.getRolloutValue();
            }
        }

        return flag.getDefaultValue();
    }

    private boolean matchesAll(Rule rule, EvaluationContext context) {
        for (var condition : rule.getConditions()) {
            if (!conditionMatcher.matches(condition, context)) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Object> fields(String flagName, String env, String detail) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("flag", flagName);
        map.put("env", env);
        if (detail != null) {
            map.put("detail", detail);
        }
        return map;
    }
}

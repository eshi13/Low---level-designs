package com.flagservice;

import com.flagservice.logging.StructuredErrorLogger;
import com.flagservice.model.BucketingMode;
import com.flagservice.model.Condition;
import com.flagservice.model.EvaluationContext;
import com.flagservice.model.Flag;
import com.flagservice.model.Operator;
import com.flagservice.model.PercentageRollout;
import com.flagservice.model.Rule;
import com.flagservice.model.ValueType;
import com.flagservice.store.InMemoryConfigStore;
import com.flagservice.store.InvalidFlagConfigException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlagServiceTest {
    private RecordingLogger logger;
    private FlagService service;

    @BeforeEach
    void setUp() {
        logger = new RecordingLogger();
        service = new FlagService(new InMemoryConfigStore(), logger);
    }

    @Test
    void evaluatesBooleanStringAndIntegerTypes() {
        service.addOrUpdateFlag(new Flag(
                "bool_flag", "dev", ValueType.BOOLEAN, false, List.of(), null));
        service.addOrUpdateFlag(new Flag(
                "str_flag", "dev", ValueType.STRING, "off", List.of(), null));
        service.addOrUpdateFlag(new Flag(
                "int_flag", "dev", ValueType.INTEGER, 0, List.of(), null));

        EvaluationContext ctx = EvaluationContext.of(Map.of("userId", "u1"));
        assertEquals(false, service.evaluate("bool_flag", "dev", ctx));
        assertEquals("off", service.evaluate("str_flag", "dev", ctx));
        assertEquals(0, service.evaluate("int_flag", "dev", ctx));
    }

    @Test
    void rejectsTypeMismatchAtConfigTime() {
        assertThrows(IllegalArgumentException.class, () -> new Flag(
                "bad", "dev", ValueType.BOOLEAN, "not-bool", List.of(), null));

        assertThrows(IllegalArgumentException.class, () -> new Flag(
                "bad_rule",
                "dev",
                ValueType.INTEGER,
                1,
                List.of(new Rule(List.of(new Condition("a", Operator.EQ, "x")), "string")),
                null
        ));

        assertThrows(IllegalArgumentException.class, () -> new Flag(
                "bad_rollout",
                "dev",
                ValueType.BOOLEAN,
                false,
                List.of(),
                PercentageRollout.strict(10, "userId", "not-bool")
        ));
    }

    @Test
    void rejectsInvalidPercentageAndSharedWithoutShareKey() {
        assertThrows(IllegalArgumentException.class, () ->
                PercentageRollout.strict(101, "userId", true));
        assertThrows(IllegalArgumentException.class, () ->
                new PercentageRollout(10, "userId", BucketingMode.SHARED, null, true));
    }

    @Test
    void targetingRuleMatchesCountryAndFallsBackToDefault() {
        service.addOrUpdateFlag(new Flag(
                "promo",
                "prod",
                ValueType.BOOLEAN,
                false,
                List.of(new Rule(
                        List.of(new Condition("country", Operator.EQ, "IN")),
                        true
                )),
                null
        ));

        assertEquals(true, service.evaluate("promo", "prod",
                EvaluationContext.of(Map.of("country", "IN", "userId", "u1"))));
        assertEquals(false, service.evaluate("promo", "prod",
                EvaluationContext.of(Map.of("country", "US", "userId", "u1"))));
    }

    @Test
    void firstMatchingRuleWinsAndConditionsAreAnded() {
        service.addOrUpdateFlag(new Flag(
                "tier",
                "prod",
                ValueType.STRING,
                "basic",
                List.of(
                        new Rule(List.of(
                                new Condition("country", Operator.EQ, "IN"),
                                new Condition("plan", Operator.EQ, "gold")
                        ), "vip"),
                        new Rule(List.of(
                                new Condition("country", Operator.EQ, "IN")
                        ), "india")
                ),
                null
        ));

        assertEquals("vip", service.evaluate("tier", "prod",
                EvaluationContext.of(Map.of("country", "IN", "plan", "gold"))));
        assertEquals("india", service.evaluate("tier", "prod",
                EvaluationContext.of(Map.of("country", "IN", "plan", "silver"))));
        assertEquals("basic", service.evaluate("tier", "prod",
                EvaluationContext.of(Map.of("country", "US", "plan", "gold"))));
    }

    @Test
    void targetingRuleTakesPrecedenceOverPercentageRollout() {
        service.addOrUpdateFlag(new Flag(
                "feature",
                "prod",
                ValueType.BOOLEAN,
                false,
                List.of(new Rule(
                        List.of(new Condition("country", Operator.EQ, "IN")),
                        true
                )),
                PercentageRollout.strict(0, "userId", true)
        ));

        // Rule matches even though rollout percent is 0
        assertEquals(true, service.evaluate("feature", "prod",
                EvaluationContext.of(Map.of("country", "IN", "userId", "anyone"))));
    }

    @Test
    void percentageRolloutIsStickyForSameUser() {
        service.addOrUpdateFlag(new Flag(
                "rollout",
                "prod",
                ValueType.BOOLEAN,
                false,
                List.of(),
                PercentageRollout.strict(50, "userId", true)
        ));

        EvaluationContext ctx = EvaluationContext.of(Map.of("userId", "sticky-user-42"));
        Object first = service.evaluate("rollout", "prod", ctx);
        for (int i = 0; i < 20; i++) {
            assertEquals(first, service.evaluate("rollout", "prod", ctx));
        }
    }

    @Test
    void percentageRolloutDistributesAroundConfiguredPercent() {
        service.addOrUpdateFlag(new Flag(
                "rollout",
                "prod",
                ValueType.BOOLEAN,
                false,
                List.of(),
                PercentageRollout.strict(30, "userId", true)
        ));

        int enabled = 0;
        int total = 1000;
        for (int i = 0; i < total; i++) {
            Object value = service.evaluate("rollout", "prod",
                    EvaluationContext.of(Map.of("userId", "user-" + i)));
            if (Boolean.TRUE.equals(value)) {
                enabled++;
            }
        }
        double ratio = enabled / (double) total;
        assertTrue(ratio > 0.2 && ratio < 0.4, "expected ~30% enabled, got " + ratio);
    }

    @Test
    void sharedBucketingCorrelatesFlagsWithSameShareKey() {
        String shareKey = "spring_launch";
        service.addOrUpdateFlag(new Flag(
                "checkout",
                "prod",
                ValueType.BOOLEAN,
                false,
                List.of(),
                PercentageRollout.shared(50, "userId", shareKey, true)
        ));
        service.addOrUpdateFlag(new Flag(
                "banner",
                "prod",
                ValueType.BOOLEAN,
                false,
                List.of(),
                PercentageRollout.shared(50, "userId", shareKey, true)
        ));

        for (int i = 0; i < 100; i++) {
            EvaluationContext ctx = EvaluationContext.of(Map.of("userId", "user-" + i));
            Object a = service.evaluate("checkout", "prod", ctx);
            Object b = service.evaluate("banner", "prod", ctx);
            assertEquals(a, b, "shared buckets must match for user-" + i);
        }
    }

    @Test
    void sharedBucketingIsIsolatedAcrossEnvironments() {
        String shareKey = "spring_launch";
        service.addOrUpdateFlag(new Flag(
                "checkout",
                "prod",
                ValueType.BOOLEAN,
                false,
                List.of(),
                PercentageRollout.shared(50, "userId", shareKey, true)
        ));
        service.addOrUpdateFlag(new Flag(
                "checkout",
                "staging",
                ValueType.BOOLEAN,
                false,
                List.of(),
                PercentageRollout.shared(50, "userId", shareKey, true)
        ));

        boolean foundDivergence = false;
        for (int i = 0; i < 500; i++) {
            EvaluationContext ctx = EvaluationContext.of(Map.of("userId", "user-" + i));
            Object prod = service.evaluate("checkout", "prod", ctx);
            Object staging = service.evaluate("checkout", "staging", ctx);
            if (!prod.equals(staging)) {
                foundDivergence = true;
                break;
            }
        }
        assertTrue(foundDivergence,
                "same shareKey should not force the same bucket across environments");
    }

    @Test
    void strictBucketingIsIndependentAcrossFlags() {
        service.addOrUpdateFlag(new Flag(
                "flag_a",
                "prod",
                ValueType.BOOLEAN,
                false,
                List.of(),
                PercentageRollout.strict(50, "userId", true)
        ));
        service.addOrUpdateFlag(new Flag(
                "flag_b",
                "prod",
                ValueType.BOOLEAN,
                false,
                List.of(),
                PercentageRollout.strict(50, "userId", true)
        ));

        boolean foundDivergence = false;
        for (int i = 0; i < 500; i++) {
            EvaluationContext ctx = EvaluationContext.of(Map.of("userId", "user-" + i));
            Object a = service.evaluate("flag_a", "prod", ctx);
            Object b = service.evaluate("flag_b", "prod", ctx);
            if (!a.equals(b)) {
                foundDivergence = true;
                break;
            }
        }
        assertTrue(foundDivergence, "strict flags at 50% should diverge for some users");
    }

    @Test
    void environmentsAreIsolated() {
        service.addOrUpdateFlag(new Flag(
                "feature", "dev", ValueType.BOOLEAN, true, List.of(), null));
        service.addOrUpdateFlag(new Flag(
                "feature", "prod", ValueType.BOOLEAN, false, List.of(), null));

        EvaluationContext ctx = EvaluationContext.of(Map.of());
        assertEquals(true, service.evaluate("feature", "dev", ctx));
        assertEquals(false, service.evaluate("feature", "prod", ctx));
    }

    @Test
    void evaluateReturnsNullAndLogsWhenFlagMissing() {
        Object result = service.evaluate("missing", "prod", EvaluationContext.of(Map.of()));
        assertNull(result);
        assertTrue(logger.errorCount.get() >= 1);
    }

    @Test
    void evaluateReturnsDefaultWhenStickyAttributeMissing() {
        service.addOrUpdateFlag(new Flag(
                "rollout",
                "prod",
                ValueType.BOOLEAN,
                false,
                List.of(),
                PercentageRollout.strict(100, "userId", true)
        ));

        Object result = service.evaluate("rollout", "prod",
                EvaluationContext.of(Map.of("country", "IN")));
        assertEquals(false, result);
        assertTrue(logger.errorCount.get() >= 1);
    }

    @Test
    void liveUpdateIsVisibleOnNextEvaluate() {
        service.addOrUpdateFlag(new Flag(
                "feature", "prod", ValueType.BOOLEAN, false, List.of(), null));
        assertEquals(false, service.evaluate("feature", "prod", Map.of()));

        service.addOrUpdateFlag(new Flag(
                "feature", "prod", ValueType.BOOLEAN, true, List.of(), null));
        assertEquals(true, service.evaluate("feature", "prod", Map.of()));
    }

    @Test
    void addRuleAppendsToExistingFlag() {
        service.addOrUpdateFlag(new Flag(
                "feature", "prod", ValueType.BOOLEAN, false, List.of(), null));
        service.addRule("prod", "feature", new Rule(
                List.of(new Condition("country", Operator.EQ, "IN")),
                true
        ));
        assertEquals(true, service.evaluate("feature", "prod",
                EvaluationContext.of(Map.of("country", "IN"))));
    }

    private static final class RecordingLogger implements StructuredErrorLogger {
        final AtomicInteger errorCount = new AtomicInteger();
        final List<String> messages = new ArrayList<>();

        @Override
        public void error(String message, Map<String, Object> fields, Throwable cause) {
            errorCount.incrementAndGet();
            messages.add(message);
        }
    }
}

package com.flagservice;

import com.flagservice.model.Condition;
import com.flagservice.model.EvaluationContext;
import com.flagservice.model.Flag;
import com.flagservice.model.Operator;
import com.flagservice.model.PercentageRollout;
import com.flagservice.model.Rule;
import com.flagservice.model.ValueType;

import java.util.List;
import java.util.Map;

public final class Main {
    public static void main(String[] args) {
        FlagService flags = new FlagService();

        demoBooleanWithRule(flags);
        demoIntegerWithRule(flags);
        demoStringWithRule(flags);
        demoStrictBucketStickiness(flags);
        demoSharedBucketExperience(flags);
    }

    /** 1. Boolean flag with a basic targeting rule */
    private static void demoBooleanWithRule(FlagService flags) {
        section("1. Boolean flag + basic rule (country == IN → true)");

        flags.addOrUpdateFlag(new Flag(
                "show_promo",
                "prod",
                ValueType.BOOLEAN,
                false,
                List.of(new Rule(
                        List.of(new Condition("country", Operator.EQ, "IN")),
                        true
                )),
                null
        ));

        EvaluationContext india = EvaluationContext.of(Map.of("userId", "u1", "country", "IN"));
        EvaluationContext usa = EvaluationContext.of(Map.of("userId", "u2", "country", "US"));

        System.out.println("  IN user → " + flags.evaluate("show_promo", "prod", india)
                + " (expected: true)");
        System.out.println("  US user → " + flags.evaluate("show_promo", "prod", usa)
                + " (expected: false / default)");
    }

    /** 2. Integer flag with a basic targeting rule */
    private static void demoIntegerWithRule(FlagService flags) {
        section("2. Integer flag + basic rule (plan == gold → 100)");

        flags.addOrUpdateFlag(new Flag(
                "discount_percent",
                "prod",
                ValueType.INTEGER,
                0,
                List.of(new Rule(
                        List.of(new Condition("plan", Operator.EQ, "gold")),
                        100
                )),
                null
        ));

        EvaluationContext gold = EvaluationContext.of(Map.of("userId", "u1", "plan", "gold"));
        EvaluationContext silver = EvaluationContext.of(Map.of("userId", "u2", "plan", "silver"));

        System.out.println("  gold plan  → " + flags.evaluate("discount_percent", "prod", gold)
                + " (expected: 100)");
        System.out.println("  silver plan → " + flags.evaluate("discount_percent", "prod", silver)
                + " (expected: 0 / default)");
    }

    /** 3. String flag with a basic targeting rule */
    private static void demoStringWithRule(FlagService flags) {
        section("3. String flag + basic rule (country == IN → \"hi\")");

        flags.addOrUpdateFlag(new Flag(
                "greeting",
                "prod",
                ValueType.STRING,
                "hello",
                List.of(new Rule(
                        List.of(new Condition("country", Operator.EQ, "IN")),
                        "hi"
                )),
                null
        ));

        EvaluationContext india = EvaluationContext.of(Map.of("userId", "u1", "country", "IN"));
        EvaluationContext usa = EvaluationContext.of(Map.of("userId", "u2", "country", "US"));

        System.out.println("  IN user → " + flags.evaluate("greeting", "prod", india)
                + " (expected: hi)");
        System.out.println("  US user → " + flags.evaluate("greeting", "prod", usa)
                + " (expected: hello / default)");
    }

    /** 4. Strict bucket: same user always lands in the same bucket */
    private static void demoStrictBucketStickiness(FlagService flags) {
        section("4. Strict bucket stickiness (boolean, 40% rollout)");

        flags.addOrUpdateFlag(new Flag(
                "new_checkout",
                "prod",
                ValueType.BOOLEAN,
                false,
                List.of(),
                PercentageRollout.strict(40, "userId", true)
        ));

        EvaluationContext user = EvaluationContext.of(Map.of("userId", "sticky-user-42"));
        Object first = flags.evaluate("new_checkout", "prod", user);
        boolean sticky = true;
        for (int i = 0; i < 10; i++) {
            Object next = flags.evaluate("new_checkout", "prod", user);
            if (!first.equals(next)) {
                sticky = false;
            }
        }

        System.out.println("  user sticky-user-42 first result → " + first);
        System.out.println("  same result across 10 calls? → " + sticky
                + (sticky ? " (expected: true)" : " (UNEXPECTED)"));

        // Show a couple of other users for contrast
        for (String id : List.of("user-a", "user-b", "user-c")) {
            System.out.println("  " + id + " → "
                    + flags.evaluate("new_checkout", "prod",
                    EvaluationContext.of(Map.of("userId", id))));
        }
    }

    /**
     * 5. Shared bucket: two flags with same shareKey give the same experience,
     *    and that experience is sticky for the user.
     */
    private static void demoSharedBucketExperience(FlagService flags) {
        section("5. Shared bucket across two flags (shareKey=spring_launch, 50%)");

        String shareKey = "spring_launch";
        flags.addOrUpdateFlag(new Flag(
                "checkout_v2",
                "prod",
                ValueType.BOOLEAN,
                false,
                List.of(),
                PercentageRollout.shared(50, "userId", shareKey, true)
        ));
        flags.addOrUpdateFlag(new Flag(
                "checkout_banner",
                "prod",
                ValueType.BOOLEAN,
                false,
                List.of(),
                PercentageRollout.shared(50, "userId", shareKey, true)
        ));

        EvaluationContext user = EvaluationContext.of(Map.of("userId", "shared-user-7"));
        Object checkoutFirst = flags.evaluate("checkout_v2", "prod", user);
        Object bannerFirst = flags.evaluate("checkout_banner", "prod", user);

        boolean shared = checkoutFirst.equals(bannerFirst);
        boolean sticky = true;
        for (int i = 0; i < 10; i++) {
            Object c = flags.evaluate("checkout_v2", "prod", user);
            Object b = flags.evaluate("checkout_banner", "prod", user);
            if (!c.equals(checkoutFirst) || !b.equals(bannerFirst) || !c.equals(b)) {
                sticky = false;
            }
        }

        System.out.println("  shared-user-7 checkout_v2    → " + checkoutFirst);
        System.out.println("  shared-user-7 checkout_banner → " + bannerFirst);
        System.out.println("  same experience across both flags? → " + shared
                + (shared ? " (expected: true)" : " (UNEXPECTED)"));
        System.out.println("  sticky across 10 calls for both? → " + sticky
                + (sticky ? " (expected: true)" : " (UNEXPECTED)"));

        System.out.println("  sample of other users (checkout vs banner should match):");
        for (String id : List.of("u10", "u11", "u12", "u13", "u14")) {
            EvaluationContext ctx = EvaluationContext.of(Map.of("userId", id));
            Object c = flags.evaluate("checkout_v2", "prod", ctx);
            Object b = flags.evaluate("checkout_banner", "prod", ctx);
            System.out.println("    " + id + " → checkout=" + c + ", banner=" + b
                    + (c.equals(b) ? " [shared OK]" : " [MISMATCH]"));
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}

package com.flagservice.eval.bucketing;

import com.flagservice.model.EvaluationContext;
import com.flagservice.model.Flag;
import com.flagservice.model.PercentageRollout;

public final class SharedBucketingStrategy implements BucketingStrategy {
    @Override
    public int bucket(Flag flag, PercentageRollout rollout, EvaluationContext context) {
        Object sticky = context.get(rollout.getAttribute())
                .orElseThrow(() -> new IllegalStateException(
                        "missing sticky attribute: " + rollout.getAttribute()));
        // Env-scoped: same shareKey correlates flags within an env, not across envs.
        String material = sticky + ":" + flag.getEnv() + ":" + rollout.getShareKey();
        return Hashing.bucket(material);
    }
}

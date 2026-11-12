package com.flagservice.eval.bucketing;

import com.flagservice.model.EvaluationContext;
import com.flagservice.model.Flag;
import com.flagservice.model.PercentageRollout;

public final class StrictBucketingStrategy implements BucketingStrategy {
    @Override
    public int bucket(Flag flag, PercentageRollout rollout, EvaluationContext context) {
        Object sticky = context.get(rollout.getAttribute())
                .orElseThrow(() -> new IllegalStateException(
                        "missing sticky attribute: " + rollout.getAttribute()));
        String material = sticky + ":" + flag.getEnv() + ":" + flag.getName();
        return Hashing.bucket(material);
    }
}

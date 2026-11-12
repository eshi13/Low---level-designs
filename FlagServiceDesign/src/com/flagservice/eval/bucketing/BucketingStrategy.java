package com.flagservice.eval.bucketing;

import com.flagservice.model.EvaluationContext;
import com.flagservice.model.Flag;
import com.flagservice.model.PercentageRollout;

public interface BucketingStrategy {
    /**
     * @return bucket in range [0, 99]
     */
    int bucket(Flag flag, PercentageRollout rollout, EvaluationContext context);
}

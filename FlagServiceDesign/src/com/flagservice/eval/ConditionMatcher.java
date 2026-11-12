package com.flagservice.eval;

import com.flagservice.model.Condition;
import com.flagservice.model.EvaluationContext;
import com.flagservice.model.Operator;

import java.util.Objects;

public final class ConditionMatcher {
    public boolean matches(Condition condition, EvaluationContext context) {
        Object actual = context.get(condition.getAttribute()).orElse(null);
        if (actual == null) {
            return false;
        }
        boolean equal = Objects.equals(actual, condition.getExpected());
        return condition.getOperator() == Operator.EQ ? equal : !equal;
    }
}

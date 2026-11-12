package com.flagservice.model;

import java.util.Objects;

public final class PercentageRollout {
    private final int percent;
    private final String attribute;
    private final BucketingMode mode;
    private final String shareKey;
    private final Object rolloutValue;

    public PercentageRollout(
            int percent,
            String attribute,
            BucketingMode mode,
            String shareKey,
            Object rolloutValue
    ) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("percent must be between 0 and 100 inclusive");
        }
        if (attribute == null || attribute.isBlank()) {
            throw new IllegalArgumentException("rollout attribute must be non-blank");
        }
        this.percent = percent;
        this.attribute = attribute;
        this.mode = Objects.requireNonNull(mode, "mode");
        if (mode == BucketingMode.SHARED && (shareKey == null || shareKey.isBlank())) {
            throw new IllegalArgumentException("shareKey is required when mode is SHARED");
        }
        this.shareKey = shareKey;
        this.rolloutValue = Objects.requireNonNull(rolloutValue, "rolloutValue");
    }

    public static PercentageRollout strict(int percent, String attribute, Object rolloutValue) {
        return new PercentageRollout(percent, attribute, BucketingMode.STRICT, null, rolloutValue);
    }

    public static PercentageRollout shared(
            int percent,
            String attribute,
            String shareKey,
            Object rolloutValue
    ) {
        return new PercentageRollout(percent, attribute, BucketingMode.SHARED, shareKey, rolloutValue);
    }

    public int getPercent() {
        return percent;
    }

    public String getAttribute() {
        return attribute;
    }

    public BucketingMode getMode() {
        return mode;
    }

    public String getShareKey() {
        return shareKey;
    }

    public Object getRolloutValue() {
        return rolloutValue;
    }
}

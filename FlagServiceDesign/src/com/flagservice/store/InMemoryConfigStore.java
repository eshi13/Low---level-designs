package com.flagservice.store;

import com.flagservice.model.Flag;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryConfigStore implements ConfigStore {
    private final ConcurrentHashMap<String, Flag> flags = new ConcurrentHashMap<>();

    @Override
    public void set(Flag flag) {
        Objects.requireNonNull(flag, "flag");
        try {
            // Re-construct to ensure validation invariants hold for stored snapshot.
            Flag validated = new Flag(
                    flag.getName(),
                    flag.getEnv(),
                    flag.getValueType(),
                    flag.getDefaultValue(),
                    flag.getRules(),
                    flag.getRollout()
            );
            flags.put(key(validated.getEnv(), validated.getName()), validated);
        } catch (IllegalArgumentException e) {
            throw new InvalidFlagConfigException(e.getMessage(), e);
        }
    }

    @Override
    public Optional<Flag> get(String flagName, String env) {
        if (flagName == null || env == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(flags.get(key(env, flagName)));
    }

    static String key(String env, String flagName) {
        return env + "/" + flagName;
    }
}

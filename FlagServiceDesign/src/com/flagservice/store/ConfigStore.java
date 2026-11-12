package com.flagservice.store;

import com.flagservice.model.Flag;

import java.util.Optional;

public interface ConfigStore {
    void set(Flag flag);

    Optional<Flag> get(String flagName, String env);
}

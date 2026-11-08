package kr.moonseungjun.anomalousclasses.state;

import java.util.Optional;

public enum ClassId {
    MENTAL_DOMINATOR("mental_dominator"),
    PUPPETEER("puppeteer"),
    PARASITE_MORPHER("parasite_morpher"),
    BARRIER_CASTER("barrier_caster"),
    PACT_CASTER("pact_caster"),
    SWARM_CASTER("swarm_caster");

    private final String key;

    ClassId(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public int networkIndex() {
        return ordinal();
    }

    public static Optional<ClassId> fromNetworkIndex(int index) {
        ClassId[] values = values();
        return index >= 0 && index < values.length ? Optional.of(values[index]) : Optional.empty();
    }

    public static Optional<ClassId> fromKey(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        for (ClassId value : values()) {
            if (value.key.equals(key)) return Optional.of(value);
        }
        return Optional.empty();
    }
}

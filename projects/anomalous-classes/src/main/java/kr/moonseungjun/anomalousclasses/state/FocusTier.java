package kr.moonseungjun.anomalousclasses.state;

import java.util.Optional;

public enum FocusTier {
    STANDARD("standard"),
    ALTERED("altered"),
    ANOMALOUS("anomalous"),
    ELDRITCH("eldritch"),
    INCOMPREHENSIBLE("incomprehensible");

    private final String key;

    FocusTier(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public int index() {
        return ordinal();
    }

    public static Optional<FocusTier> fromIndex(int index) {
        FocusTier[] values = values();
        return index >= 0 && index < values.length ? Optional.of(values[index]) : Optional.empty();
    }

    public static Optional<FocusTier> fromKey(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        for (FocusTier value : values()) if (value.key.equals(key)) return Optional.of(value);
        return Optional.empty();
    }
}

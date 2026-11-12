package kr.moonseungjun.anomalousclasses.state;

import java.util.Optional;

public record ClassFocus(ClassId classId, FocusTier tier) {
    public ClassFocus {
        if (classId == null || tier == null) throw new IllegalArgumentException("class and tier are required");
    }

    public String itemPath() {
        return "focus_" + classId.key() + "_" + tier.key();
    }

    public String key() {
        return classId.key() + ":" + tier.key();
    }

    public static Optional<ClassFocus> fromKey(String key) {
        if (key == null) return Optional.empty();
        int separator = key.indexOf(':');
        if (separator < 1 || separator == key.length() - 1) return Optional.empty();
        Optional<ClassId> classId = ClassId.fromKey(key.substring(0, separator));
        Optional<FocusTier> tier = FocusTier.fromKey(key.substring(separator + 1));
        return classId.isPresent() && tier.isPresent()
                ? Optional.of(new ClassFocus(classId.get(), tier.get())) : Optional.empty();
    }
}

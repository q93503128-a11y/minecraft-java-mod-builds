package io.github.q93503128.turnbound.presentation;

import java.util.Map;

/** Pure portrait identity/camera contract shared by all UI surfaces. */
public final class HeroPortraitPlan {
    public record Camera(float scale, float offsetY, float xAngle, float yAngle) {
        public Camera {
            scale = Math.max(0.60F, Math.min(1.25F, scale));
        }
    }

    private static final Map<String, Camera> CAMERAS = Map.ofEntries(
            Map.entry("P01", new Camera(1.00F, 0.02F, 0.05F, 0.30F)),
            Map.entry("P02", new Camera(1.05F, 0.03F, 0.04F, 0.26F)),
            Map.entry("P03", new Camera(0.92F, 0.00F, 0.04F, 0.28F)),
            Map.entry("P04", new Camera(1.03F, 0.03F, 0.05F, 0.28F)),
            Map.entry("P05", new Camera(1.02F, 0.02F, 0.04F, 0.31F)),
            Map.entry("P06", new Camera(1.00F, 0.02F, 0.05F, 0.28F)),
            Map.entry("P07", new Camera(1.06F, 0.04F, 0.04F, 0.27F)),
            Map.entry("P08", new Camera(0.94F, 0.00F, 0.05F, 0.32F)),
            Map.entry("P07_SUMMON", new Camera(0.88F, 0.08F, 0.08F, 0.24F))
    );

    private HeroPortraitPlan() {}

    public static boolean coreHero(String id) {
        return id != null && switch (id) {
            case "P01","P02","P03","P04","P05","P06","P07","P08" -> true;
            default -> false;
        };
    }

    public static Camera camera(String id) {
        return CAMERAS.getOrDefault(id, new Camera(0.96F, 0.0F, 0.05F, 0.28F));
    }

    /** Mirrors the authored signature visual family; P07's signature modifies Toto, not Marion. */
    public static String signatureVisualId(String characterId, String signatureItemId) {
        if (characterId == null || signatureItemId == null) return characterId;
        return switch (characterId + "|" + signatureItemId) {
            case "P01|sig_p01_unending_vow" -> "P01_SIG";
            case "P02|sig_p02_moving_hand" -> "P02_SIG";
            case "P03|sig_p03_gate_shield" -> "P03_SIG";
            case "P04|sig_p04_last_ember_chalice" -> "P04_SIG";
            case "P05|sig_p05_never_late_scope" -> "P05_SIG";
            case "P06|sig_p06_unnamed_epitaph" -> "P06_SIG";
            case "P08|sig_p08_blood_grip" -> "P08_SIG";
            default -> characterId;
        };
    }
}

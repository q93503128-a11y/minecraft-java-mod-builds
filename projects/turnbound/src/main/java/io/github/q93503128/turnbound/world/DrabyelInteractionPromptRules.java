package io.github.q93503128.turnbound.world;

import java.util.List;
import java.util.function.Predicate;

/** Pure New Drabyel close-range service prompt selection and player-facing action copy. */
final class DrabyelInteractionPromptRules {
    record Prompt(String id, String label, String action) {
        boolean active() { return id != null && !id.isBlank() && label != null && !label.isBlank(); }
    }

    private static final Prompt NONE = new Prompt("", "", "");

    private DrabyelInteractionPromptRules() {}

    static Prompt none() { return NONE; }

    static Prompt nearest(
            List<DrabyelHubServiceCatalog.Service> services,
            double playerX,
            double playerY,
            double playerZ,
            Predicate<String> visualSupported
    ) {
        if (services == null || services.isEmpty()) return NONE;
        DrabyelHubServiceCatalog.Service best = null;
        double bestDistanceSq = Double.MAX_VALUE;

        for (var service : services) {
            if (service == null || service.runtimePosition() == null) continue;
            if (visualSupported != null && !visualSupported.test(service.visualAsset())) continue;
            var position = service.runtimePosition();
            double dx = playerX - (position.x() + 0.5D);
            double dy = playerY - position.y();
            double dz = playerZ - (position.z() + 0.5D);
            double distanceSq = dx * dx + dy * dy + dz * dz;
            double radius = service.interactionRadius() + 0.75D;
            if (distanceSq <= radius * radius && distanceSq < bestDistanceSq) {
                best = service;
                bestDistanceSq = distanceSq;
            }
        }

        if (best == null) return NONE;
        return new Prompt(best.locator(), best.playerLabel(), action(best.role()));
    }

    static String action(String role) {
        if (role == null) return "상호작용";
        return switch (role) {
            case "MARKET" -> "상점 열기";
            case "BLACKSMITH" -> "대장간 이용";
            case "TRAVEL" -> "이동 지도 보기";
            case "SUMMON" -> "정령 기록 보기";
            case "GREETER", "STORY" -> "대화하기";
            default -> "상호작용";
        };
    }
}

package io.github.q93503128.turnbound.client;

import net.minecraft.network.chat.Component;

/**
 * Small client-only vocabulary adapter for legacy literal labels.
 *
 * <p>Content names stay in canonical data. This adapter exists only until the remaining
 * generic UI vocabulary is fully moved to translation keys; widgets should not own
 * localization rules.</p>
 */
final class TurnboundUiText {
    private TurnboundUiText() { }

    static Component playerFacingLabel(Component source) {
        if (source == null) return Component.empty();
        String raw = source.getString();
        String translated = switch (raw) {
            case "PARTY" -> "파티";
            case "CHARACTERS" -> "캐릭터";
            case "EQUIPMENT" -> "장비";
            case "ARCHIVE" -> "소환";
            case "QUESTS" -> "퀘스트";
            case "CODEX" -> "도감";
            case "SYSTEM" -> "도전";
            case "ENEMIES" -> "적";
            case "BOSSES" -> "보스";
            case "TUTORIAL" -> "도움말";
            case "INVENTORY" -> "인벤토리";
            case "MARKET" -> "상점";
            case "Status" -> "능력치";
            case "Skills" -> "스킬";
            case "Equipment" -> "장비";
            case "Awakening" -> "각성";
            case "Profile" -> "프로필";
            default -> null;
        };
        if (translated != null) return Component.literal(translated);

        if (raw.startsWith("Star: ")) {
            String value = raw.substring("Star: ".length());
            return Component.literal("별 등급: " + ("ALL".equals(value) ? "전체" : value));
        }
        if (raw.startsWith("Level ≥ ")) {
            String value = raw.substring("Level ≥ ".length());
            return Component.literal("레벨: " + ("ALL".equals(value) ? "전체" : value + " 이상"));
        }
        if (raw.startsWith("Role: ")) {
            return Component.literal("역할: " + roleLabel(raw.substring("Role: ".length())));
        }
        if (raw.startsWith("Slot: ")) {
            return Component.literal("부위: " + slotLabel(raw.substring("Slot: ".length())));
        }
        if (raw.startsWith("Sort: ")) {
            return Component.literal("정렬: " + sortLabel(raw.substring("Sort: ".length())));
        }
        if (raw.startsWith("Starter ")) {
            return Component.literal("초기 소환 " + raw.substring("Starter ".length()));
        }
        if (raw.startsWith("LOCK · ")) {
            return Component.literal("잠김 · " + raw.substring("LOCK · ".length()));
        }
        return source;
    }

    private static String roleLabel(String value) {
        return switch (value) {
            case "ALL" -> "전체";
            case "DPS" -> "공격";
            case "SUPPORT" -> "지원";
            case "TANK" -> "수호";
            case "SUMMON" -> "소환";
            default -> value;
        };
    }

    private static String slotLabel(String value) {
        return switch (value) {
            case "ALL" -> "전체";
            case "WEAPON" -> "무기";
            case "ARMOR" -> "방어구";
            case "ACCESSORY" -> "장신구";
            case "SIGNATURE" -> "전용 장비";
            default -> value;
        };
    }

    private static String sortLabel(String value) {
        return switch (value) {
            case "TIER" -> "등급";
            case "LEVEL" -> "강화";
            case "STAT" -> "능력치";
            default -> value;
        };
    }
}

package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.EndgameEncounterCatalog;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Presentation-only briefing for v0.4 post-story encounters. It never invents missing reward/CP rules. */
public final class EndgameBriefing {
    public record Briefing(
            String encounterId, String title, String kind, int level, String composition,
            int partyCp, Integer recommendedCp, String recommendedLevel,
            int gold, int firstCrystal, int firstEssence, String firstExtra, String repeatExtra,
            boolean firstClear, boolean hardPattern, String implementationGap
    ) {}

    private static final Map<String, Integer> NORMAL_CP = Map.of(
            "BATTLE_B01", 9_000, "BATTLE_B02", 10_500, "BATTLE_B03", 11_500,
            "BATTLE_B04", 12_500, "BATTLE_B05", 13_500);
    private static final Map<Integer, Integer> RIFT_MILESTONE_CP = Map.of(10, 18_000, 20, 27_000, 30, 38_000);

    private EndgameBriefing() {}

    public static Briefing build(UUID playerId, String encounterId, String kind, int level) {
        Set<String> clears = CampaignProgressStore.snapshot(playerId).clearedEncounters();
        int partyCp = CampaignProgressStore.activeParty(playerId).stream()
                .mapToInt(id -> CampaignProgressStore.combatPower(playerId, id)).sum();
        boolean firstClear = !clears.contains(encounterId);

        if ("NORMAL".equals(kind) && encounterId.startsWith("BATTLE_B")) {
            V04Catalogs.Encounter spec = V04Catalogs.encounter(encounterId);
            String composition = composition(spec.enemies());
            return new Briefing(encounterId, spec.label() + " · Normal 재도전", kind, spec.level(), composition,
                    partyCp, NORMAL_CP.get(encounterId), "Lv." + spec.level(), V04Catalogs.battleGold(spec),
                    0, 0, "캠페인 첫 클리어 보상은 이미 스토리 진행에서 처리", "재도전 Boss 드랍 규칙 적용",
                    firstClear, false, "");
        }

        if (EndgameEncounterCatalog.hardBoss(encounterId)) {
            String bossId = EndgameEncounterCatalog.bossId(encounterId);
            String bossName = CanonicalData.definition(bossId).name();
            return new Briefing(encounterId, bossName + " · Hard", "HARD", level, bossName + " [Hard]",
                    partyCp, null, "스토리 Boss +5 Lv", V04Catalogs.battleGold(V04Catalogs.encounter("BATTLE_" + bossId)),
                    600, 0, "T4 장비 선택 토큰 ×1", "Gold + T3/T4 Drop",
                    firstClear, true, "Hard 반복 T3/T4 Drop의 세부 분배율은 v0.4에 별도 지정 없음");
        }

        if (EndgameEncounterCatalog.rift(encounterId)) {
            int floor = EndgameEncounterCatalog.riftFloorNumber(encounterId);
            V04Catalogs.RiftFloor spec = V04Catalogs.riftFloor(floor);
            String band = floor <= 10 ? "Lv.20~30" : floor <= 20 ? "Lv.30~45" : "Lv.45~60";
            String extra = floor % 10 == 0 ? "T3/T4 선택 보상 추가" : "";
            String gap = floor % 10 == 0
                    ? "F10/F20/F30 선택 보상의 정확한 T3/T4 티어 배정은 v0.4에 별도 지정 없음"
                    : "";
            return new Briefing(encounterId, "Rift Gate F" + floor, "RIFT", spec.level(), composition(spec.enemies()),
                    partyCp, RIFT_MILESTONE_CP.get(floor), band, V04Catalogs.riftGold(floor),
                    60, 25, extra, "Gold 공식 적용", firstClear, spec.hardBossPattern(), gap);
        }
        throw new IllegalArgumentException("Unsupported endgame briefing " + encounterId);
    }

    public static void send(ServerPlayer player, Briefing b) {
        ChatFormatting accent = "HARD".equals(b.kind()) ? ChatFormatting.RED
                : "RIFT".equals(b.kind()) ? ChatFormatting.AQUA : ChatFormatting.GOLD;
        player.sendSystemMessage(Component.literal("TURNBOUND · " + b.title()).withStyle(accent, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("적 · " + b.composition() + "   /   Lv " + b.level()).withStyle(ChatFormatting.WHITE));
        String guide = b.recommendedCp() == null
                ? "Party CP " + b.partyCp() + "   /   권장 " + b.recommendedLevel()
                : "Party CP " + b.partyCp() + "   /   권장 CP " + b.recommendedCp();
        player.sendSystemMessage(Component.literal(guide).withStyle(
                b.recommendedCp() != null && b.partyCp() < b.recommendedCp() ? ChatFormatting.YELLOW : ChatFormatting.GREEN));
        if (b.firstClear()) {
            String reward = "첫 클리어 · Gold " + b.gold()
                    + (b.firstCrystal() > 0 ? " · Crystal " + b.firstCrystal() : "")
                    + (b.firstEssence() > 0 ? " · Star Essence " + b.firstEssence() : "")
                    + (b.firstExtra().isBlank() ? "" : " · " + b.firstExtra());
            player.sendSystemMessage(Component.literal(reward).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            player.sendSystemMessage(Component.literal("재도전 · Gold " + b.gold()
                    + (b.repeatExtra().isBlank() ? "" : " · " + b.repeatExtra())).withStyle(ChatFormatting.GRAY));
        }
        if (b.hardPattern()) {
            player.sendSystemMessage(Component.literal("Hard · HP×1.65 / ATK×1.25 / DEF×1.15 / SPD+8 / 소환 적 Lv+5")
                    .withStyle(ChatFormatting.RED));
        } else if ("RIFT".equals(b.kind())) {
            player.sendSystemMessage(Component.literal("Rift · 전투 사이 전회복 / 입장 전 파티 변경 가능 / Auto·2x 허용")
                    .withStyle(ChatFormatting.AQUA));
        }
        if (!b.implementationGap().isBlank()) {
            player.sendSystemMessage(Component.literal("정본 미지정 · " + b.implementationGap()).withStyle(ChatFormatting.DARK_GRAY));
        }
        player.sendSystemMessage(Component.literal("같은 표식을 다시 상호작용하면 출전합니다.").withStyle(ChatFormatting.YELLOW));
    }

    private static String composition(Iterable<String> ids) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String id : ids) counts.merge(CanonicalData.definition(id).name(), 1, Integer::sum);
        return counts.entrySet().stream().map(e -> e.getValue() > 1 ? e.getKey() + "×" + e.getValue() : e.getKey())
                .reduce((a, b) -> a + " · " + b).orElse("-");
    }
}

package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.EndgameEncounterCatalog;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.network.EndgameBriefingPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

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
            boolean firstClear, boolean hardPattern
    ) {}

    private static final Map<String, Integer> NORMAL_CP = Map.of(
            "BATTLE_B01", 9_000, "BATTLE_B02", 10_500, "BATTLE_B03", 11_500,
            "BATTLE_B04", 12_500, "BATTLE_B05", 13_500);
    private static final Map<Integer, Integer> RIFT_MILESTONE_CP = Map.of(10, 18_000, 20, 27_000, 30, 38_000);

    private EndgameBriefing() {}

    public static Briefing build(UUID playerId, String encounterId) {
        if (EndgameEncounterCatalog.hardBoss(encounterId)) {
            String bossId = EndgameEncounterCatalog.bossId(encounterId);
            int level = V04Catalogs.encounter("BATTLE_" + bossId).level() + 5;
            return build(playerId, encounterId, "HARD", level);
        }
        if (EndgameEncounterCatalog.rift(encounterId)) {
            int floor = EndgameEncounterCatalog.riftFloorNumber(encounterId);
            return build(playerId, encounterId, "RIFT", V04Catalogs.riftFloor(floor).level());
        }
        if (encounterId != null && encounterId.matches("BATTLE_B0[1-5]")) {
            return build(playerId, encounterId, "NORMAL", V04Catalogs.encounter(encounterId).level());
        }
        throw new IllegalArgumentException("Unsupported endgame briefing " + encounterId);
    }

    public static Briefing build(UUID playerId, String encounterId, String kind, int level) {
        Set<String> clears = CampaignProgressStore.snapshot(playerId).clearedEncounters();
        int partyCp = CampaignProgressStore.activeParty(playerId).stream()
                .mapToInt(id -> CampaignProgressStore.combatPower(playerId, id)).sum();
        boolean firstClear = !clears.contains(encounterId);

        if ("NORMAL".equals(kind) && encounterId.startsWith("BATTLE_B")) {
            V04Catalogs.Encounter spec = V04Catalogs.encounter(encounterId);
            return new Briefing(encounterId, spec.label() + " · 일반 재도전", kind, spec.level(), composition(spec.enemies()),
                    partyCp, NORMAL_CP.get(encounterId), "Lv." + spec.level(), V04Catalogs.battleGold(spec),
                    0, 0, "", "골드 " + V04Catalogs.battleGold(spec), false, false);
        }

        if (EndgameEncounterCatalog.hardBoss(encounterId)) {
            String bossId = EndgameEncounterCatalog.bossId(encounterId);
            String bossName = CanonicalData.definition(bossId).name();
            // Canon fixes the first-clear T4 choice token. Repeat T3/T4 distribution is intentionally not surfaced
            // until its missing distribution rule is authored; player UI must not expose development-gap text.
            return new Briefing(encounterId, bossName + " · 하드", "HARD", level, bossName + " [하드]",
                    partyCp, null, "보스 레벨 +5", V04Catalogs.battleGold(V04Catalogs.encounter("BATTLE_" + bossId)),
                    600, 0, "T4 장비 선택권 ×1", "골드 " + V04Catalogs.battleGold(V04Catalogs.encounter("BATTLE_" + bossId)),
                    firstClear, true);
        }

        if (EndgameEncounterCatalog.rift(encounterId)) {
            int floor = EndgameEncounterCatalog.riftFloorNumber(encounterId);
            V04Catalogs.RiftFloor spec = V04Catalogs.riftFloor(floor);
            String band = floor <= 10 ? "Lv.20~30" : floor <= 20 ? "Lv.30~45" : "Lv.45~60";
            // F10/F20/F30 have a canonical extra choice reward, but v0.4 does not assign the exact T3/T4 tier.
            // Do not promise an unresolved concrete item in player-facing text.
            return new Briefing(encounterId, "균열 관문 F" + floor, "RIFT", spec.level(), composition(spec.enemies()),
                    partyCp, RIFT_MILESTONE_CP.get(floor), band, V04Catalogs.riftGold(floor),
                    60, 25, "", "골드 " + V04Catalogs.riftGold(floor), firstClear, spec.hardBossPattern());
        }
        throw new IllegalArgumentException("Unsupported endgame briefing " + encounterId);
    }

    public static void send(ServerPlayer player, Briefing briefing) {
        PacketDistributor.sendToPlayer(player, new EndgameBriefingPayload(encode(briefing)));
    }

    private static String encode(Briefing b) {
        StringBuilder out = new StringBuilder();
        out.append("H|").append(safe(b.encounterId())).append('|').append(safe(b.title())).append('|').append(b.kind()).append('|')
                .append(b.level()).append('|').append(b.partyCp()).append('|').append(b.recommendedCp() == null ? -1 : b.recommendedCp()).append('|')
                .append(safe(b.recommendedLevel())).append('|').append(b.firstClear() ? 1 : 0).append('|').append(b.hardPattern() ? 1 : 0).append('\n');
        out.append("E|").append(safe(b.composition())).append('\n');
        out.append("R|").append(b.gold()).append('|').append(b.firstCrystal()).append('|').append(b.firstEssence()).append('|')
                .append(safe(b.firstExtra())).append('|').append(safe(b.repeatExtra())).append('\n');
        return out.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }

    private static String composition(Iterable<String> ids) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String id : ids) counts.merge(CanonicalData.definition(id).name(), 1, Integer::sum);
        return counts.entrySet().stream().map(e -> e.getValue() > 1 ? e.getKey() + "×" + e.getValue() : e.getKey())
                .reduce((a, b) -> a + " · " + b).orElse("-");
    }
}

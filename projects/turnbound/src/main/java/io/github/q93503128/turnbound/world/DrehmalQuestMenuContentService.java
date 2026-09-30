package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Current place-backed quest projection for the Drehmal management-menu quest surface. */
final class DrehmalQuestMenuContentService {
    private DrehmalQuestMenuContentService() {}

    static String encode(ServerPlayer player) {
        if (player == null) return "";
        var server = player.level().getServer();
        Set<String> flags = server == null ? Set.of() : ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());
        Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
        Set<String> productionEncounters = new LinkedHashSet<>();
        productionEncounters.addAll(DrehmalAdaptiveRoutePlacement.productionEncounters(player).stream()
                .map(DrehmalFirstRouteCatalog.EncounterSlot::combatEncounterId)
                .filter(id -> id != null && !id.isBlank()).collect(Collectors.toSet()));
        productionEncounters.addAll(AvsalExpansionRuntime.productionEncounters(player).stream()
                .map(DrehmalFirstRouteCatalog.EncounterSlot::combatEncounterId)
                .filter(id -> id != null && !id.isBlank()).collect(Collectors.toSet()));
        String mainObjective = DrehmalFirstRouteRuntime.explorationSnapshot(player).objective();
        return encodeState(flags, clears, Set.copyOf(productionEncounters), mainObjective);
    }

    static String encodeObjective(String objective) {
        return encodeState(Set.of(), Set.of(), Set.of(), objective);
    }

    static String encodeState(Set<String> flags, Set<String> clearedEncounters,
                              Set<String> productionEncounterIds, String mainObjective) {
        StringBuilder out = new StringBuilder();
        for (DrehmalQuestCatalog.Quest quest : DrehmalQuestCatalog.all()) {
            if (!DrehmalQuestCatalog.visible(quest, flags, productionEncounterIds)) continue;
            boolean completed = DrehmalQuestCatalog.completed(quest, flags, clearedEncounters);
            String objective = quest.kind() == DrehmalQuestCatalog.Kind.MAIN && !completed
                    && mainObjective != null && !mainObjective.isBlank() ? mainObjective : quest.objective();
            append(out, quest.title(), quest.kind().label() + " · " + quest.regionLabel(), completed, objective);
        }
        for (AvsalQuestCatalog.Quest quest : AvsalQuestCatalog.all()) {
            if (!AvsalQuestCatalog.visible(quest, flags, productionEncounterIds)) continue;
            boolean completed = AvsalQuestCatalog.completed(quest, flags, clearedEncounters);
            String objective = quest.kind() == AvsalQuestCatalog.Kind.MAIN && !completed
                    && mainObjective != null && !mainObjective.isBlank() ? mainObjective : quest.objective();
            append(out, quest.title(), quest.kind().label() + " · " + quest.regionLabel(), completed, objective);
        }
        return out.toString();
    }

    private static void append(StringBuilder out, String title, String category, boolean completed, String objective) {
        out.append("Q|").append(safe(title)).append('|').append(safe(category)).append("|1|")
                .append(completed ? 1 : 0).append('|').append(safe(objective)).append('\n');
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/').replace('\n', ' ').replace('\r', ' ').trim();
    }
}

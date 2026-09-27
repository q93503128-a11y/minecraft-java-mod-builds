package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.stream.Collectors;

/** Current place-backed quest projection for the Drehmal management-menu quest surface. */
final class DrehmalQuestMenuContentService {
    private DrehmalQuestMenuContentService() {}

    static String encode(ServerPlayer player) {
        if (player == null) return "";
        var server = player.level().getServer();
        Set<String> flags = server == null
                ? Set.of()
                : ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());
        Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
        Set<String> productionEncounters = DrehmalFirstRouteCatalog.productionEncounters().stream()
                .map(DrehmalFirstRouteCatalog.EncounterSlot::combatEncounterId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        String mainObjective = DrehmalFirstRouteRuntime.explorationSnapshot(player).objective();
        return encodeState(flags, clears, productionEncounters, mainObjective);
    }

    static String encodeObjective(String objective) {
        return encodeState(Set.of(), Set.of(), Set.of(), objective);
    }

    static String encodeState(
            Set<String> flags,
            Set<String> clearedEncounters,
            Set<String> productionEncounterIds,
            String mainObjective
    ) {
        StringBuilder out = new StringBuilder();
        for (DrehmalQuestCatalog.Quest quest : DrehmalQuestCatalog.all()) {
            if (!DrehmalQuestCatalog.visible(quest, flags, productionEncounterIds)) continue;
            boolean completed = DrehmalQuestCatalog.completed(quest, flags, clearedEncounters);
            String objective = quest.kind() == DrehmalQuestCatalog.Kind.MAIN
                    && mainObjective != null && !mainObjective.isBlank()
                    ? mainObjective
                    : quest.objective();
            out.append("Q|")
                    .append(safe(quest.title())).append('|')
                    .append(safe(quest.kind().label() + " · " + quest.regionLabel())).append("|1|")
                    .append(completed ? 1 : 0).append('|')
                    .append(safe(objective)).append('\n');
        }
        return out.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/').replace('\n', ' ').replace('\r', ' ').trim();
    }
}

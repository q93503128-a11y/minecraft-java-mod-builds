package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Server-authored auxiliary objective list for the field HUD. Main story remains pinned separately. */
final class OpenworldQuestTrackerService {
    private OpenworldQuestTrackerService() {}

    static List<FieldUiSnapshot.QuestTracker> active(ServerPlayer player) {
        if (player == null || player.level().getServer() == null) return List.of();
        var server = player.level().getServer();
        Set<String> flags = ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());
        Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
        Set<String> production = new LinkedHashSet<>();
        production.addAll(DrehmalAdaptiveRoutePlacement.productionEncounters(player).stream()
                .map(DrehmalFirstRouteCatalog.EncounterSlot::combatEncounterId)
                .filter(id -> id != null && !id.isBlank()).collect(Collectors.toSet()));
        production.addAll(AvsalExpansionRuntime.productionEncounters(player).stream()
                .map(DrehmalFirstRouteCatalog.EncounterSlot::combatEncounterId)
                .filter(id -> id != null && !id.isBlank()).collect(Collectors.toSet()));

        List<FieldUiSnapshot.QuestTracker> out = new ArrayList<>();

        CampaignProgressStore.RegionalContractState regional =
                CampaignProgressStore.regionalContractState(player.getUUID());
        if (!regional.contractId().isBlank()) {
            RegionalContractCatalog.Contract contract = RegionalContractCatalog.contract(regional.contractId());
            if (contract != null) {
                String progress = regional.progress() >= contract.requiredWins()
                        ? "완료 · 의뢰관에게 보고"
                        : "진행 " + regional.progress() + "/" + contract.requiredWins();
                out.add(new FieldUiSnapshot.QuestTracker(
                        "regional:" + contract.id(), contract.title(), "지역 의뢰",
                        contract.objective() + " · " + progress, true));
            }
        }

        for (DrehmalQuestCatalog.Quest quest : DrehmalQuestCatalog.all()) {
            if (quest.kind() == DrehmalQuestCatalog.Kind.MAIN) continue;
            if (!DrehmalQuestCatalog.visible(quest, flags, production)
                    || DrehmalQuestCatalog.completed(quest, flags, clears)) continue;
            if (quest.activationFlag().isBlank()) continue;
            out.add(new FieldUiSnapshot.QuestTracker(
                    quest.id(), quest.title(), quest.kind().label(), quest.objective(), false));
        }

        for (AvsalQuestCatalog.Quest quest : AvsalQuestCatalog.all()) {
            if (quest.kind() == AvsalQuestCatalog.Kind.MAIN) continue;
            if (!AvsalQuestCatalog.visible(quest, flags, production)
                    || AvsalQuestCatalog.completed(quest, flags, clears)) continue;
            if (quest.activationFlag().isBlank()) continue;
            out.add(new FieldUiSnapshot.QuestTracker(
                    quest.id(), quest.title(), quest.kind().label(), quest.objective(), false));
        }
        return List.copyOf(out);
    }
}

package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Physical-NPC entry point and journal projection for repeatable regional contracts. */
final class RegionalContractService {
    static final String NPC_LABEL = "지역 의뢰관 로웬";

    private RegionalContractService() {}

    static boolean unlocked(ServerPlayer player) {
        if (player == null || player.level().getServer() == null) return false;
        Set<String> flags = ExternalWorldSavedData.get(player.level().getServer())
                .onboardingFlags(player.getUUID());
        return flags.contains(DrehmalFirstRouteProgress.HUB_REACHED);
    }

    static boolean interact(ServerPlayer player) {
        if (!unlocked(player)) return false;
        CampaignProgressStore.RegionalContractState state =
                CampaignProgressStore.regionalContractState(player.getUUID());

        if (state.contractId().isBlank()) {
            int partyLevel = CampaignProgressStore.averageActivePartyLevel(player.getUUID());
            RegionalContractCatalog.Contract next = next(state.rotation(), partyLevel);
            if (next == null) return false;
            if (!CampaignProgressStore.activateRegionalContract(player.getUUID(), next.id())) return false;
            CampaignPersistence.saveIfDirty(player);
            FieldNetwork.showDialogue(player, NPC_LABEL,
                    "현재 파티에 맞는 " + next.tier() + "단계 지역 의뢰입니다. 스토리를 더 진행하지 않아도 성장하면 더 높은 단계 의뢰가 열립니다.\n\n"
                            + next.title() + "\n" + next.objective() + "\n보상 · "
                            + String.format(Locale.ROOT, "%,d", next.rewardGold()) + " Gold · 파티 XP " + next.rewardXp());
            return true;
        }

        RegionalContractCatalog.Contract active = RegionalContractCatalog.contract(state.contractId());
        if (active == null) return false;

        if (state.progress() >= active.requiredWins()) {
            if (!CampaignProgressStore.claimRegionalContract(player.getUUID())) return false;
            CampaignPersistence.saveIfDirty(player);
            FieldNetwork.showDialogue(player, NPC_LABEL,
                    active.title() + " 완료를 확인했습니다.\n\n보상 · "
                            + String.format(Locale.ROOT, "%,d", active.rewardGold()) + " Gold · 파티 XP " + active.rewardXp()
                            + "\n다음 의뢰가 필요하면 다시 말을 걸어 주세요.");
            return true;
        }

        FieldNetwork.showDialogue(player, NPC_LABEL,
                active.tier() + "단계 · " + active.title() + "\n" + active.objective() + "\n진행 "
                        + state.progress() + "/" + active.requiredWins()
                        + "\n완료하면 여기로 돌아와 주세요.");
        return true;
    }

    static String encodeMenu(ServerPlayer player) {
        if (!unlocked(player)) return "";
        CampaignProgressStore.RegionalContractState state =
                CampaignProgressStore.regionalContractState(player.getUUID());
        int partyLevel = CampaignProgressStore.averageActivePartyLevel(player.getUUID());
        if (state.contractId().isBlank()) {
            int tier = highestTier(partyLevel);
            return line("지역 의뢰", "반복 지역 의뢰 · " + tier + "단계",
                    NPC_LABEL + "에게서 현재 파티 레벨에 맞는 반복 의뢰를 받을 수 있습니다.");
        }
        RegionalContractCatalog.Contract active = RegionalContractCatalog.contract(state.contractId());
        if (active == null) return "";
        String progress = state.progress() >= active.requiredWins()
                ? "완료 · " + NPC_LABEL + "에게 보고"
                : "진행 " + state.progress() + "/" + active.requiredWins();
        String reward = String.format(Locale.ROOT, "%,d", active.rewardGold())
                + " Gold / 파티 XP " + active.rewardXp();
        return line(active.title(), "반복 지역 의뢰 · " + active.tier() + "단계 · " + active.regionLabel(),
                active.objective() + " · " + progress + " · 보상 " + reward);
    }

    private static RegionalContractCatalog.Contract next(int rotation, int partyLevel) {
        int tier = highestTier(partyLevel);
        List<RegionalContractCatalog.Contract> pool = RegionalContractCatalog.all().stream()
                .filter(contract -> contract.tier() == tier)
                .filter(contract -> contract.minPartyLevel() <= partyLevel)
                .sorted(Comparator.comparing(RegionalContractCatalog.Contract::id))
                .toList();
        if (pool.isEmpty()) return null;
        return pool.get(Math.floorMod(rotation, pool.size()));
    }

    private static int highestTier(int partyLevel) {
        return RegionalContractCatalog.all().stream()
                .filter(contract -> contract.minPartyLevel() <= partyLevel)
                .mapToInt(RegionalContractCatalog.Contract::tier)
                .max().orElse(1);
    }

    private static String line(String title, String category, String objective) {
        return "Q|" + safe(title) + "|" + safe(category) + "|1|0|" + safe(objective) + "\n";
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/').replace('\n', ' ').replace('\r', ' ').trim();
    }
}

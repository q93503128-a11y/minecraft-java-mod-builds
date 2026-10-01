package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Physical-NPC entry point and journal projection for repeatable regional contracts. */
final class RegionalContractService {
    static final String NPC_LABEL = "지역 의뢰관";

    private RegionalContractService() {}

    static boolean unlocked(ServerPlayer player) {
        if (player == null || player.level().getServer() == null) return false;
        Set<String> flags = ExternalWorldSavedData.get(player.level().getServer())
                .onboardingFlags(player.getUUID());
        return flags.contains(DrehmalFirstRouteProgress.HUB_REACHED);
    }

    static boolean interact(ServerPlayer player) {
        return interact(player, NPC_LABEL);
    }

    static boolean interact(ServerPlayer player, String speaker) {
        if (!unlocked(player)) return false;
        String npcLabel = speaker == null || speaker.isBlank() ? NPC_LABEL : speaker.trim();
        CampaignProgressStore.RegionalContractState state =
                CampaignProgressStore.regionalContractState(player.getUUID());

        if (state.contractId().isBlank()) {
            int partyLevel = CampaignProgressStore.averageActivePartyLevel(player.getUUID());
            RegionalContractCatalog.Contract next = next(player, state.rotation(), partyLevel);
            if (next == null) return false;
            if (!CampaignProgressStore.activateRegionalContract(player.getUUID(), next.id())) return false;
            CampaignPersistence.saveIfDirty(player);
            FieldNetwork.showDialogue(player, npcLabel,
                    "현재 파티에 맞는 " + next.tier() + "단계 지역 의뢰입니다. 파티가 성장하거나 새로운 지역을 발견하면 더 높은 단계 의뢰가 들어옵니다.\n\n"
                            + next.title() + "\n" + next.objective() + "\n보상 · "
                            + String.format(Locale.ROOT, "%,d", next.rewardGold()) + " Gold · 파티 XP " + next.rewardXp());
            return true;
        }

        RegionalContractCatalog.Contract active = RegionalContractCatalog.contract(state.contractId());
        if (active == null) return false;

        if (state.progress() >= active.requiredWins()) {
            if (!CampaignProgressStore.claimRegionalContract(player.getUUID())) return false;
            CampaignPersistence.saveIfDirty(player);
            FieldNetwork.showDialogue(player, npcLabel,
                    active.title() + " 완료를 확인했습니다.\n\n보상 · "
                            + String.format(Locale.ROOT, "%,d", active.rewardGold()) + " Gold · 파티 XP " + active.rewardXp()
                            + "\n다음 의뢰가 필요하면 다시 말을 걸어 주세요.");
            return true;
        }

        FieldNetwork.showDialogue(player, npcLabel,
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
            int tier = highestTier(player, partyLevel);
            return line("지역 의뢰", "반복 지역 의뢰 · " + tier + "단계",
                    "지역 의뢰관에게서 현재 파티와 발견 지역에 맞는 반복 의뢰를 받을 수 있습니다.");
        }
        RegionalContractCatalog.Contract active = RegionalContractCatalog.contract(state.contractId());
        if (active == null) return "";
        String progress = state.progress() >= active.requiredWins()
                ? "완료 · 지역 의뢰관에게 보고"
                : "진행 " + state.progress() + "/" + active.requiredWins();
        String reward = String.format(Locale.ROOT, "%,d", active.rewardGold())
                + " Gold / 파티 XP " + active.rewardXp();
        return line(active.title(), "반복 지역 의뢰 · " + active.tier() + "단계 · " + active.regionLabel(),
                active.objective() + " · " + progress + " · 보상 " + reward);
    }

    private static RegionalContractCatalog.Contract next(ServerPlayer player, int rotation, int partyLevel) {
        int tier = highestTier(player, partyLevel);
        List<RegionalContractCatalog.Contract> pool = RegionalContractCatalog.all().stream()
                .filter(contract -> contract.tier() == tier)
                .filter(contract -> available(player, contract, partyLevel))
                .sorted(Comparator.comparing(RegionalContractCatalog.Contract::id))
                .toList();
        if (pool.isEmpty()) return null;
        return pool.get(Math.floorMod(rotation, pool.size()));
    }

    private static int highestTier(ServerPlayer player, int partyLevel) {
        return RegionalContractCatalog.all().stream()
                .filter(contract -> available(player, contract, partyLevel))
                .mapToInt(RegionalContractCatalog.Contract::tier)
                .max().orElse(1);
    }

    private static boolean available(ServerPlayer player, RegionalContractCatalog.Contract contract, int partyLevel) {
        if (contract == null || contract.minPartyLevel() > partyLevel) return false;
        if (contract.requiredFlag().isBlank()) return true;
        if (player == null || player.level().getServer() == null) return false;
        return ExternalWorldSavedData.get(player.level().getServer())
                .onboardingFlag(player.getUUID(), contract.requiredFlag());
    }

    private static String line(String title, String category, String objective) {
        return "Q|" + safe(title) + "|" + safe(category) + "|1|0|" + safe(objective) + "\n";
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/').replace('\n', ' ').replace('\r', ' ').trim();
    }
}

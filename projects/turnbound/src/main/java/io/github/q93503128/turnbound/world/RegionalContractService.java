package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Physical-NPC entry point and journal projection for repeatable regional contracts. */
final class RegionalContractService {
    static final String NPC_LABEL = "지역 의뢰관";
    private static final String AVSAL_BROKER = "turnbound:npc/avsal/contract_broker";

    private RegionalContractService() {}

    static boolean unlocked(ServerPlayer player) {
        if (player == null || player.level().getServer() == null) return false;
        Set<String> flags = ExternalWorldSavedData.get(player.level().getServer()).onboardingFlags(player.getUUID());
        return flags.contains(DrehmalFirstRouteProgress.HUB_REACHED);
    }

    static boolean interactionAvailable(ServerPlayer player) {
        return DrabyelHubServiceRuntime.nearRole(player, "CONTRACT")
                || DrehmalFieldNpcRuntime.nearNpc(player, AVSAL_BROKER);
    }

    static boolean interact(ServerPlayer player) { return interact(player, NPC_LABEL); }

    static boolean interact(ServerPlayer player, String speaker) {
        if (!unlocked(player)) return false;
        String npcLabel = speaker == null || speaker.isBlank() ? NPC_LABEL : speaker.trim();
        CampaignProgressStore.RegionalContractState state = CampaignProgressStore.regionalContractState(player.getUUID());

        if (state.contractId().isBlank()) {
            List<RegionalContractCatalog.Contract> options = selection(player, state.rotation());
            if (options.isEmpty()) return false;
            List<FieldNetwork.DialogueChoice> choices = options.stream()
                    .map(contract -> new FieldNetwork.DialogueChoice(
                            contract.tier() + "단계 · " + contract.title(),
                            "CONTRACT_ACCEPT|" + contract.id()))
                    .toList();
            FieldNetwork.showDialogueChoices(player, npcLabel,
                    "이번에 들어온 지역 의뢰입니다. 하나를 골라 맡을 수 있어요.\n"
                            + "권장 레벨은 참고일 뿐이고, 어려운 의뢰도 원하면 바로 받을 수 있습니다.",
                    choices);
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
                        + state.progress() + "/" + active.requiredWins() + "\n완료하면 여기로 돌아와 주세요.");
        return true;
    }

    static boolean accept(ServerPlayer player, String contractId) {
        if (!unlocked(player) || !interactionAvailable(player) || contractId == null || contractId.isBlank()) return false;
        CampaignProgressStore.RegionalContractState state = CampaignProgressStore.regionalContractState(player.getUUID());
        if (!state.contractId().isBlank()) return false;
        RegionalContractCatalog.Contract selected = selection(player, state.rotation()).stream()
                .filter(contract -> contract.id().equals(contractId)).findFirst().orElse(null);
        if (selected == null) return false;
        if (!CampaignProgressStore.activateRegionalContract(player.getUUID(), selected.id())) return false;
        CampaignPersistence.saveIfDirty(player);
        FieldNetwork.showDialogue(player, NPC_LABEL,
                selected.tier() + "단계 · " + selected.title() + "\n" + selected.objective() + "\n\n보상 · "
                        + String.format(Locale.ROOT, "%,d", selected.rewardGold()) + " Gold · 파티 XP " + selected.rewardXp()
                        + (selected.minPartyLevel() > 1 ? "\n권장 Lv." + selected.minPartyLevel() : ""));
        return true;
    }

    static String encodeMenu(ServerPlayer player) {
        if (!unlocked(player)) return "";
        CampaignProgressStore.RegionalContractState state = CampaignProgressStore.regionalContractState(player.getUUID());
        if (state.contractId().isBlank()) {
            return line("지역 의뢰", "반복 지역 의뢰 · 레벨 제한 없음",
                    "지역 의뢰관에게 가서 현재 제시된 의뢰 중 하나를 선택하십시오.");
        }
        RegionalContractCatalog.Contract active = RegionalContractCatalog.contract(state.contractId());
        if (active == null) return "";
        String progress = state.progress() >= active.requiredWins() ? "완료 · 지역 의뢰관에게 보고" : "진행 " + state.progress() + "/" + active.requiredWins();
        String reward = String.format(Locale.ROOT, "%,d", active.rewardGold()) + " Gold / 파티 XP " + active.rewardXp();
        return line(active.title(), "반복 지역 의뢰 · " + active.tier() + "단계 · " + active.regionLabel(),
                active.objective() + " · " + progress + " · 보상 " + reward);
    }

    private static List<RegionalContractCatalog.Contract> selection(ServerPlayer player, int rotation) {
        List<RegionalContractCatalog.Contract> pool = RegionalContractCatalog.all().stream()
                .filter(contract -> available(player, contract))
                .sorted(Comparator.comparingInt(RegionalContractCatalog.Contract::tier).thenComparing(RegionalContractCatalog.Contract::id))
                .toList();
        if (pool.isEmpty()) return List.of();
        int wanted = Math.min(3, pool.size());
        List<RegionalContractCatalog.Contract> out = new ArrayList<>();
        for (int i = 0; i < wanted; i++) {
            int offset = (int)Math.floor(i * pool.size() / (double)wanted);
            RegionalContractCatalog.Contract contract = pool.get(Math.floorMod(rotation + offset, pool.size()));
            if (!out.contains(contract)) out.add(contract);
        }
        for (int i = 0; out.size() < wanted && i < pool.size(); i++) {
            RegionalContractCatalog.Contract contract = pool.get(Math.floorMod(rotation + i, pool.size()));
            if (!out.contains(contract)) out.add(contract);
        }
        return List.copyOf(out);
    }

    private static boolean available(ServerPlayer player, RegionalContractCatalog.Contract contract) {
        if (contract == null) return false;
        if (contract.requiredFlag().isBlank()) return true;
        if (player == null || player.level().getServer() == null) return false;
        return ExternalWorldSavedData.get(player.level().getServer()).onboardingFlag(player.getUUID(), contract.requiredFlag());
    }

    private static String line(String title, String category, String objective) {
        return "Q|" + safe(title) + "|" + safe(category) + "|1|0|" + safe(objective) + "\n";
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/').replace('\n', ' ').replace('\r', ' ').trim();
    }
}

package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Set;

/** Physical-NPC entry point and journal projection for repeatable regional contracts. */
final class RegionalContractService {
    private RegionalContractService() {}

    static boolean unlocked(ServerPlayer player) {
        if (player == null) return false;
        Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
        return clears.contains(DrabyelOpeningTutorial.ENCOUNTER_ID);
    }

    static boolean interact(ServerPlayer player) {
        if (!unlocked(player)) return false;
        CampaignProgressStore.RegionalContractState state =
                CampaignProgressStore.regionalContractState(player.getUUID());

        if (state.contractId().isBlank()) {
            RegionalContractCatalog.Contract next = next(state.rotation());
            if (next == null) return false;
            if (!CampaignProgressStore.activateRegionalContract(player.getUUID(), next.id())) return false;
            CampaignPersistence.saveIfDirty(player);
            FieldNetwork.showDialogue(player, "기록관 세린",
                    "마을 주변에서 계속 들어오는 의뢰가 하나 있어요. 급한 이야기는 아니니 필요할 때 처리해 주세요.\n\n"
                            + next.title() + "\n" + next.objective() + "\n보상 · "
                            + String.format(Locale.ROOT, "%,d", next.rewardGold()) + " Gold · 파티 XP " + next.rewardXp());
            return true;
        }

        RegionalContractCatalog.Contract active = RegionalContractCatalog.contract(state.contractId());
        if (active == null) return false;

        if (state.progress() >= active.requiredWins()) {
            if (!CampaignProgressStore.claimRegionalContract(player.getUUID())) return false;
            CampaignPersistence.saveIfDirty(player);
            FieldNetwork.showDialogue(player, "기록관 세린",
                    active.title() + " 확인했어요. 필요한 사람들에게 기록을 넘겨 둘게요.\n\n보상 · "
                            + String.format(Locale.ROOT, "%,d", active.rewardGold()) + " Gold · 파티 XP " + active.rewardXp()
                            + "\n다음 지역 의뢰가 필요하면 다시 말을 걸어 주세요.");
            return true;
        }

        FieldNetwork.showDialogue(player, "기록관 세린",
                active.title() + "\n" + active.objective() + "\n진행 "
                        + state.progress() + "/" + active.requiredWins()
                        + "\n완료하면 여기로 돌아와 주세요.");
        return true;
    }

    static String encodeMenu(ServerPlayer player) {
        if (!unlocked(player)) return "";
        CampaignProgressStore.RegionalContractState state =
                CampaignProgressStore.regionalContractState(player.getUUID());
        if (state.contractId().isBlank()) {
            return line("지역 의뢰", "지역 의뢰 · Capital Valley",
                    "기록관 세린에게서 반복 가능한 지역 의뢰를 받을 수 있습니다.");
        }
        RegionalContractCatalog.Contract active = RegionalContractCatalog.contract(state.contractId());
        if (active == null) return "";
        String progress = state.progress() >= active.requiredWins()
                ? "완료 · 기록관 세린에게 보고"
                : "진행 " + state.progress() + "/" + active.requiredWins();
        String reward = String.format(Locale.ROOT, "%,d", active.rewardGold())
                + " Gold / 파티 XP " + active.rewardXp();
        return line(active.title(), "지역 의뢰 · " + active.regionLabel(),
                active.objective() + " · " + progress + " · 보상 " + reward);
    }

    private static RegionalContractCatalog.Contract next(int rotation) {
        var all = RegionalContractCatalog.all();
        if (all.isEmpty()) return null;
        return all.get(Math.floorMod(rotation, all.size()));
    }

    private static String line(String title, String category, String objective) {
        return "Q|" + safe(title) + "|" + safe(category) + "|1|0|" + safe(objective) + "\n";
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/').replace('\n', ' ').replace('\r', ' ').trim();
    }
}

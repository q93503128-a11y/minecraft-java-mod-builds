package io.github.q93503128.turnbound.world;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** One-time, server-owned rewards for authored-world quests and meaningful exploration milestones. */
final class OpenworldQuestRewardService {
    private record Grant(String label, int crystal, int gold, int xp) {}

    private OpenworldQuestRewardService() {}

    static void reconcile(ServerPlayer player) {
        if (player == null || player.level().getServer() == null) return;
        var server = player.level().getServer();
        Set<String> flags = ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());
        Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
        List<Grant> granted = new ArrayList<>();

        for (DrehmalQuestCatalog.Quest quest : DrehmalQuestCatalog.all()) {
            if (!quest.activationFlag().isBlank() && !flags.contains(quest.activationFlag())) continue;
            if (!DrehmalQuestCatalog.completed(quest, flags, clears)) continue;
            grant(player, "quest:" + quest.id(), quest.title(), quest.rewardCrystal(), quest.rewardGold(), quest.rewardXp(), granted);
        }
        for (AvsalQuestCatalog.Quest quest : AvsalQuestCatalog.all()) {
            if (!quest.activationFlag().isBlank() && !flags.contains(quest.activationFlag())) continue;
            if (!AvsalQuestCatalog.completed(quest, flags, clears)) continue;
            grant(player, "quest:" + quest.id(), quest.title(), quest.rewardCrystal(), quest.rewardGold(), quest.rewardXp(), granted);
        }

        discovery(player, flags, DrehmalFirstRouteProgress.TOWER_REACHED, "캐피털 밸리 탑 발견", 80, 500, granted);
        discovery(player, flags, DrehmalFirstRouteProgress.CAMP_REACHED, "탐험가 야영지 발견", 60, 400, granted);
        discovery(player, flags, AvsalExpansionProgress.ROADSIDE_ECHO_SEEN, "가도의 이상 신호 발견", 60, 500, granted);
        discovery(player, flags, AvsalExpansionProgress.WAYSIDE_CACHE_SEEN, "뒤집힌 운송수레 조사", 40, 700, granted);

        if (granted.isEmpty()) return;
        int crystal = granted.stream().mapToInt(Grant::crystal).sum();
        int gold = granted.stream().mapToInt(Grant::gold).sum();
        int xp = granted.stream().mapToInt(Grant::xp).sum();
        String names = granted.size() == 1 ? granted.getFirst().label() : granted.size() + "개 목표/발견";
        player.sendSystemMessage(Component.literal(names + " 보상 · "
                + crystal + " Crystal · " + String.format(Locale.ROOT, "%,d", gold) + " Gold"
                + (xp > 0 ? " · 파티 XP " + xp : "")));
    }

    private static void discovery(ServerPlayer player, Set<String> flags, String flag, String label,
                                  int crystal, int gold, List<Grant> granted) {
        if (flags.contains(flag)) grant(player, "discovery:" + flag, label, crystal, gold, 0, granted);
    }

    private static void grant(ServerPlayer player, String rewardId, String label,
                              int crystal, int gold, int xp, List<Grant> granted) {
        if (CampaignProgressStore.grantOneTimeWorldReward(player.getUUID(), rewardId, crystal, gold, xp)) {
            granted.add(new Grant(label, crystal, gold, xp));
        }
    }
}

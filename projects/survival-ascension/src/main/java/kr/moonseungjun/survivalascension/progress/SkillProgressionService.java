package kr.moonseungjun.survivalascension.progress;

import kr.moonseungjun.survivalascension.network.SkillNetwork;
import kr.moonseungjun.survivalascension.network.SkillSnapshotPayload;
import kr.moonseungjun.survivalascension.network.SkillUpdatePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SkillProgressionService {
    private SkillProgressionService() {}

    public static SkillProgressData.AddXpResult award(ServerPlayer player, SkillType skill, long amount) {
        SkillProgressData data = SkillProgressData.get(player);
        SkillProgressData.AddXpResult result = data.addXp(player, skill, amount);
        if (result.newXp() != result.oldXp()) {
            SkillNetwork.sendUpdate(player, new SkillUpdatePayload(skill.id(), result.oldXp(), result.newXp(), result.oldLevel(), result.newLevel()));
        }
        if (result.leveledUp()) player.sendSystemMessage(Component.literal("§6[" + skill.koreanName() + "] §f레벨 §e" + result.newLevel() + "§f 달성!"));
        return result;
    }

    public static SkillProgressData.AddXpResult setLevel(ServerPlayer player, SkillType skill, int level) {
        SkillProgressData data = SkillProgressData.get(player);
        SkillProgressData.AddXpResult result = data.setLevel(player, skill, level);
        SkillNetwork.sendUpdate(player, new SkillUpdatePayload(skill.id(), result.oldXp(), result.newXp(), result.oldLevel(), result.newLevel()));
        return result;
    }

    public static void syncAll(ServerPlayer player) {
        SkillProgressData data = SkillProgressData.get(player);
        data.ensureProfile(player);
        SkillNetwork.sendSnapshot(player, new SkillSnapshotPayload(data.snapshot(player)));
    }
}

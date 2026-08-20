package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VillageHudSystem {
    private static final int REFRESH_TICKS = 10;
    private static final Map<UUID, String> LAST_TEXT = new HashMap<>();
    private static int ticks;

    private VillageHudSystem() {}

    public static void reset() {
        ticks = 0;
        LAST_TEXT.clear();
    }

    public static void tick(MinecraftServer server) {
        ticks++;
        if (ticks < REFRESH_TICKS) return;
        ticks = 0;
        LAST_TEXT.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String text = buildText(player);
            LAST_TEXT.put(player.getUUID(), text);
            VillageNetwork.sendMainHud(player, text);
            VillageNetwork.sendSkillHud(player, VillageRespawnSystem.isDowned(player)
                    ? "" : buildSkillText(player));
        }
    }

    private static String buildText(ServerPlayer player) {
        if (VillageRespawnSystem.isDowned(player)) {
            return VillageRespawnSystem.hudText(player);
        }
        RpgProgress progress = VillageCouncilState.progressOf(player.getUUID());
        String xp = progress.level() >= RpgProgress.MAX_LEVEL
                ? "MAX"
                : progress.experience() + "/" + progress.experienceToNextLevel();
        String role = VillageCouncilState.roleOf(player.getUUID())
                .map(VillageRole::shortName)
                .orElse("역할 없음");
        return "§6" + VillageCouncilState.currentDay() + "일 "
                + VillageCouncilState.currentPhase().koreanName()
                + " §8│ §bLv." + progress.level() + " §7" + xp
                + " §8│ §f" + role
                + " §8│ §e" + VillageProgressionSystem.coins(player) + "주화"
                + " §8· §6" + VillageProgressionSystem.supplies() + "보급";
    }

    private static String buildSkillText(ServerPlayer player) {
        String base = VillageRoleSkillSystem.hudSlotText(player, 0)
                + " §8│ " + VillageRoleSkillSystem.hudSlotText(player, 1);
        String active = VillageRoleAbilitySystem.activeSkillHud(player);
        return active.isBlank() ? base : base + " §8│ " + active;
    }
}

package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

import java.util.Set;

/** Makes the current prologue interaction actor visually unmistakable instead of hiding among hub props. */
public final class TutorialWaypointService {
    private TutorialWaypointService() {}

    public static void sync(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null || !RadiaHubSessionManager.active(player)) return;
        String target = targetName(player);
        AABB area = new AABB(-126, 55, -110, 126, 94, 124);
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, area)) {
            if (stand.getCustomName() == null) continue;
            String name = stand.getCustomName().getString();
            boolean tutorialActor = name.equals("Director Iven") || name.equals("파티 편성 콘솔") || name.startsWith("전투 훈련 ");
            if (!tutorialActor) continue;
            stand.setGlowingTag(!target.isBlank() && name.equals(target));
            stand.setCustomNameVisible(true);
        }
    }

    private static String targetName(ServerPlayer player) {
        var snapshot = CampaignProgressStore.snapshot(player.getUUID());
        Set<String> completed = snapshot.quests().completed();
        if (!completed.contains("MQ_P00_01_arrival")) return "Director Iven";
        if (!completed.contains("MQ_P00_02_first_party")) return "파티 편성 콘솔";
        if (!completed.contains("MQ_P00_03_south_gate")) {
            Set<String> clears = snapshot.clearedEncounters();
            if (!clears.contains("TUTORIAL_1")) return "전투 훈련 1";
            if (!clears.contains("TUTORIAL_2")) return "전투 훈련 2";
            if (!clears.contains("TUTORIAL_3")) return "전투 훈련 3";
        }
        return "";
    }
}

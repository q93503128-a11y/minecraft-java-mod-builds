package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.presentation.PersonalPresentationIsolation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

import java.util.Set;

/** Makes the current prologue interaction actor visually unmistakable without mutating shared actor state per player. */
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
            // Legacy single-player implementation wrote a player-specific glow bit onto a world-shared entity.
            // Clear that shared bit permanently; the local waypoint pulse below is sent only to this player.
            if (stand.isCurrentlyGlowing()) stand.setGlowingTag(false);
            stand.setCustomNameVisible(true);
            if (!target.isBlank() && name.equals(target)) {
                PersonalPresentationIsolation.particles(level, player, ParticleTypes.END_ROD,
                        stand.getX(), stand.getY() + 1.15, stand.getZ(), 5, 0.45, 0.75, 0.45, 0.008);
            }
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

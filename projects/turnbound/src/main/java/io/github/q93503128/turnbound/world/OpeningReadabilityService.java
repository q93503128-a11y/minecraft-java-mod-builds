package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Corrects opening orientation/readability without changing canonical encounter coordinates or progression. */
public final class OpeningReadabilityService {
    private static final Map<UUID, Boolean> FIELD_WAS_ACTIVE = new ConcurrentHashMap<>();

    private OpeningReadabilityService() {}

    public static void tick(ServerPlayer player) {
        boolean fieldActive = FieldSessionManager.active(player);
        boolean wasActive = FIELD_WAS_ACTIVE.getOrDefault(player.getUUID(), false);
        FIELD_WAS_ACTIVE.put(player.getUUID(), fieldActive);
        if (!fieldActive || wasActive || BattleSessionManager.exists(player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        SouthgateOpeningReadabilityWorld.build(level);

        // Southgate encounters sit deeper at +Z. Legacy entry code faced -Z, literally putting M01 behind the player.
        if (player.getZ() <= 142.0) {
            player.setYRot(0.0F);
            player.setXRot(3.0F);
            player.setDeltaMovement(Vec3.ZERO);
        }
        player.sendSystemMessage(Component.literal("남문 초원 · 금빛 길 표식을 따라 첫 순찰대와 접촉하세요.")
                .withStyle(ChatFormatting.GOLD));
    }

    public static void remove(ServerPlayer player) {
        if (player != null) FIELD_WAS_ACTIVE.remove(player.getUUID());
    }

    public static void clear() { FIELD_WAS_ACTIVE.clear(); }
}

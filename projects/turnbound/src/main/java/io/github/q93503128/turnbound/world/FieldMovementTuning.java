package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Aster March is a large authored RPG field, so traversal is intentionally faster than vanilla survival. */
public final class FieldMovementTuning {
    private static final double VANILLA_PLAYER_BASE = 0.10;
    private static final double FIELD_WALK = 0.135;
    private static final double FIELD_SPRINT = 0.190;

    private FieldMovementTuning() {}

    public static void apply(ServerPlayer player, boolean fieldActive) {
        if (player == null) return;
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        double wanted = fieldActive ? (player.isSprinting() ? FIELD_SPRINT : FIELD_WALK) : VANILLA_PLAYER_BASE;
        if (Math.abs(speed.getBaseValue() - wanted) > 0.00001) speed.setBaseValue(wanted);
    }

    public static void reset(ServerPlayer player) { apply(player, false); }
}

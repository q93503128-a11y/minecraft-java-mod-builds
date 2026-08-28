package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.Titanbreak;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class AugmentedMobilityService {
    private static final Identifier MOVE_ID = id("reflex_drive_move");
    private static final Identifier ATTACK_ID = id("reflex_drive_attack");
    private static final Identifier BREAK_ID = id("reflex_drive_break");
    private static final Identifier SNEAK_ID = id("reflex_drive_sneak");
    private static final Identifier SUBMERGED_BREAK_ID = id("reflex_drive_submerged_break");

    private AugmentedMobilityService() {}

    public static void tick(ServerPlayer player, boolean driveActive, double compensation) {
        if (!driveActive || compensation <= 1.0001D) {
            clear(player);
            return;
        }

        applyScale(player, Attributes.MOVEMENT_SPEED, MOVE_ID, compensation);
        applyScale(player, Attributes.ATTACK_SPEED, ATTACK_ID, compensation);
        applyScale(player, Attributes.BLOCK_BREAK_SPEED, BREAK_ID, compensation);
        applyScale(player, Attributes.SNEAKING_SPEED, SNEAK_ID, compensation);
        applyScale(player, Attributes.SUBMERGED_MINING_SPEED, SUBMERGED_BREAK_ID, compensation);
    }

    public static void clear(ServerPlayer player) {
        remove(player, Attributes.MOVEMENT_SPEED, MOVE_ID);
        remove(player, Attributes.ATTACK_SPEED, ATTACK_ID);
        remove(player, Attributes.BLOCK_BREAK_SPEED, BREAK_ID);
        remove(player, Attributes.SNEAKING_SPEED, SNEAK_ID);
        remove(player, Attributes.SUBMERGED_MINING_SPEED, SUBMERGED_BREAK_ID);
    }

    private static void applyScale(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double scale) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        double amount = Math.max(0.0D, scale - 1.0D);
        AttributeModifier current = instance.getModifier(id);
        if (current != null
                && current.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                && Math.abs(current.amount() - amount) < 1.0E-6D) {
            return;
        }

        instance.addOrUpdateTransientModifier(new AttributeModifier(
                id,
                amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void remove(ServerPlayer player, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.hasModifier(id)) instance.removeModifier(id);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, path);
    }
}

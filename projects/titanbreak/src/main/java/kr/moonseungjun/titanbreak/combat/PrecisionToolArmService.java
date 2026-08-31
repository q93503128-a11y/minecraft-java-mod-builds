package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/** General-hand utility arm: mining assistance, powered field repair, and placement reach. */
public final class PrecisionToolArmService {
    private static final Identifier BREAK_SPEED = Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, "precision_tool_arm_break_speed");
    private static final Identifier BLOCK_RANGE = Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, "precision_tool_arm_block_range");

    private PrecisionToolArmService() {}

    public static void tick(ServerPlayer player, TitanPlayerData.State state) {
        TitanPlayerData.AugmentInstance right = precisionArm(state, AugmentationCatalog.Slot.RIGHT_ARM_MAIN);
        TitanPlayerData.AugmentInstance left = precisionArm(state, AugmentationCatalog.Slot.LEFT_ARM_MAIN);
        TitanPlayerData.AugmentInstance best = stronger(right, left);
        if (best == null) {
            remove(player, Attributes.BLOCK_BREAK_SPEED, BREAK_SPEED);
            remove(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_RANGE);
            return;
        }

        double miningBonus = best.enhancement() >= 5 ? 0.40D : 0.12D;
        set(player, Attributes.BLOCK_BREAK_SPEED, BREAK_SPEED, miningBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (best.enhancement() >= 10) {
            set(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_RANGE, 1.50D,
                    AttributeModifier.Operation.ADD_VALUE);
        } else {
            remove(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_RANGE);
        }

        if (!player.isShiftKeyDown()) return;
        if (right != null) tryFieldRepair(player, state, right, InteractionHand.MAIN_HAND);
        if (left != null) tryFieldRepair(player, state, left, InteractionHand.OFF_HAND);
    }

    private static void tryFieldRepair(ServerPlayer player, TitanPlayerData.State state,
                                       TitanPlayerData.AugmentInstance arm, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isDamageableItem() || stack.getDamageValue() <= 0) return;

        int enhancement = arm.enhancement();
        int interval = enhancement >= 7 ? 20 : 80;
        if (player.tickCount % interval != 0) return;

        AugmentationCatalog.Definition definition = AugmentationCatalog.byId("precision_tool_arm");
        if (definition == null) return;
        double power = Math.max(1.0D, definition.powerLoad())
                * (enhancement >= 7 ? 1.25D : 0.70D)
                * state.powerLoadMultiplier("precision_tool_arm");
        if (!AugmentationResourceService.trySpendBurstPower(player, state, power)) return;

        int divisor = enhancement >= 7 ? 32 : 80;
        int repaired = Math.max(1, stack.getMaxDamage() / divisor);
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - repaired));

        if (player.level() instanceof ServerLevel level) {
            TitanPlayerData data = TitanPlayerData.get(level.getServer());
            if (definition.heatLoad() > 0) {
                double rawHeat = definition.heatLoad() * (enhancement >= 7 ? 0.45D : 0.20D)
                        * state.heatLoadMultiplier("precision_tool_arm");
                data.setHeat(player, state.heat() + AugmentationResourceService.normalizedHeatGain(state, rawHeat));
            }
            data.addMasteryXp(player, "precision_tool_arm", enhancement >= 7 ? 2 : 1);
        }
    }

    private static TitanPlayerData.AugmentInstance precisionArm(TitanPlayerData.State state, AugmentationCatalog.Slot slot) {
        TitanPlayerData.AugmentInstance instance = state.installedInstance(slot);
        return instance != null && "precision_tool_arm".equals(instance.id()) ? instance : null;
    }

    private static TitanPlayerData.AugmentInstance stronger(TitanPlayerData.AugmentInstance a,
                                                             TitanPlayerData.AugmentInstance b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.enhancement() != b.enhancement()) return a.enhancement() > b.enhancement() ? a : b;
        return a.mk() >= b.mk() ? a : b;
    }

    private static void set(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double amount,
                            AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier current = instance.getModifier(id);
        if (current != null && current.operation() == operation && Math.abs(current.amount() - amount) < 1.0E-8D) return;
        instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
    }

    private static void remove(ServerPlayer player, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.hasModifier(id)) instance.removeModifier(id);
    }

    public static void clear(ServerPlayer player) {
        remove(player, Attributes.BLOCK_BREAK_SPEED, BREAK_SPEED);
        remove(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_RANGE);
    }
}

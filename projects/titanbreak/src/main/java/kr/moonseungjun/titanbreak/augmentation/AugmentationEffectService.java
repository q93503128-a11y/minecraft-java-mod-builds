package kr.moonseungjun.titanbreak.augmentation;

import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AugmentationEffectService {
    private record ModifierSpec(Holder<Attribute> attribute, Identifier id, double amount,
                                AttributeModifier.Operation operation) {}

    private static final Identifier SPINE_ATTACK = id("augment_powered_spine_attack");
    private static final Identifier SPINE_MOVE = id("augment_powered_spine_move");
    private static final Identifier SPINE_KB = id("augment_powered_spine_knockback");
    private static final Identifier SKELETON_HEALTH = id("augment_bioalloy_health");
    private static final Identifier SKELETON_ARMOR = id("augment_bioalloy_armor");
    private static final Identifier SKELETON_ATTACK = id("augment_bioalloy_attack");
    private static final Identifier SKIN_ARMOR = id("augment_subdermal_armor");
    private static final Identifier BLADE_DAMAGE = id("augment_blade_arm_damage");
    private static final Identifier LEGS_MOVE = id("augment_reinforced_legs_move");
    private static final Identifier REFLEX_NODE_MOVE = id("augment_reflex_node_move");
    private static final Identifier REFLEX_NODE_ATTACK = id("augment_reflex_node_attack");

    private static final Map<Identifier, ModifierSpec> ALL = new LinkedHashMap<>();

    static {
        add(Attributes.ATTACK_DAMAGE, SPINE_ATTACK, 0.20D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(Attributes.MOVEMENT_SPEED, SPINE_MOVE, 0.08D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(Attributes.KNOCKBACK_RESISTANCE, SPINE_KB, 0.15D, AttributeModifier.Operation.ADD_VALUE);
        add(Attributes.MAX_HEALTH, SKELETON_HEALTH, 8.0D, AttributeModifier.Operation.ADD_VALUE);
        add(Attributes.ARMOR, SKELETON_ARMOR, 4.0D, AttributeModifier.Operation.ADD_VALUE);
        add(Attributes.ATTACK_DAMAGE, SKELETON_ATTACK, 0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(Attributes.ARMOR, SKIN_ARMOR, 6.0D, AttributeModifier.Operation.ADD_VALUE);
        add(Attributes.ATTACK_DAMAGE, BLADE_DAMAGE, 3.0D, AttributeModifier.Operation.ADD_VALUE);
        add(Attributes.MOVEMENT_SPEED, LEGS_MOVE, 0.15D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(Attributes.MOVEMENT_SPEED, REFLEX_NODE_MOVE, 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(Attributes.ATTACK_SPEED, REFLEX_NODE_ATTACK, 0.12D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private AugmentationEffectService() {}

    private static void add(Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
        ALL.put(id, new ModifierSpec(attribute, id, amount, operation));
    }

    public static void tick(ServerPlayer player, TitanPlayerData.State state) {
        set(player, SPINE_ATTACK, state.hasInstalled("powered_spine"));
        set(player, SPINE_MOVE, state.hasInstalled("powered_spine"));
        set(player, SPINE_KB, state.hasInstalled("powered_spine"));

        set(player, SKELETON_HEALTH, state.hasInstalled("bioalloy_skeleton"));
        set(player, SKELETON_ARMOR, state.hasInstalled("bioalloy_skeleton"));
        set(player, SKELETON_ATTACK, state.hasInstalled("bioalloy_skeleton"));
        set(player, SKIN_ARMOR, state.hasInstalled("subdermal_armor"));

        boolean blade = state.installed(AugmentationCatalog.Slot.LEFT_ARM) != null
                && state.installed(AugmentationCatalog.Slot.LEFT_ARM).equals("blade_arm")
                || state.installed(AugmentationCatalog.Slot.RIGHT_ARM) != null
                && state.installed(AugmentationCatalog.Slot.RIGHT_ARM).equals("blade_arm");
        set(player, BLADE_DAMAGE, blade);
        set(player, LEGS_MOVE, state.hasInstalled("reinforced_legs"));
        set(player, REFLEX_NODE_MOVE, state.hasInstalled("reflex_accelerator"));
        set(player, REFLEX_NODE_ATTACK, state.hasInstalled("reflex_accelerator"));

        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    public static void clear(ServerPlayer player) {
        for (ModifierSpec spec : ALL.values()) remove(player, spec);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void set(ServerPlayer player, Identifier id, boolean enabled) {
        ModifierSpec spec = ALL.get(id);
        if (spec == null) return;
        if (!enabled) {
            remove(player, spec);
            return;
        }
        AttributeInstance instance = player.getAttribute(spec.attribute());
        if (instance == null) return;
        AttributeModifier current = instance.getModifier(spec.id());
        if (current != null && current.operation() == spec.operation()
                && Math.abs(current.amount() - spec.amount()) < 1.0E-8D) return;
        instance.addOrUpdateTransientModifier(new AttributeModifier(spec.id(), spec.amount(), spec.operation()));
    }

    private static void remove(ServerPlayer player, ModifierSpec spec) {
        AttributeInstance instance = player.getAttribute(spec.attribute());
        if (instance != null && instance.hasModifier(spec.id())) instance.removeModifier(spec.id());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, path);
    }
}

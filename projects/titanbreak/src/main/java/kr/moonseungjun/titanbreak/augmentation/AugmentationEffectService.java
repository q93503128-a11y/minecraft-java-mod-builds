package kr.moonseungjun.titanbreak.augmentation;

import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class AugmentationEffectService {
    private record ModifierSpec(Holder<Attribute> attribute, Identifier id, double amount,
                                AttributeModifier.Operation operation) {}

    private static final Identifier SPINE_ATTACK = id("augment_powered_spine_attack");
    private static final Identifier SPINE_MOVE = id("augment_powered_spine_move");
    private static final Identifier SPINE_KB = id("augment_powered_spine_knockback");
    private static final Identifier GYRO_KB = id("augment_gyro_spine_knockback");
    private static final Identifier SKELETON_HEALTH = id("augment_bioalloy_health");
    private static final Identifier SKELETON_ARMOR = id("augment_bioalloy_armor");
    private static final Identifier SKELETON_ATTACK = id("augment_bioalloy_attack");
    private static final Identifier SKELETON_KB = id("augment_bioalloy_knockback");
    private static final Identifier IMPACT_TOUGHNESS = id("augment_impact_frame_toughness");
    private static final Identifier IMPACT_KB = id("augment_impact_frame_knockback");
    private static final Identifier SKIN_ARMOR = id("augment_subdermal_armor");
    private static final Identifier BLADE_DAMAGE = id("augment_blade_arm_damage");
    private static final Identifier LEGS_MOVE = id("augment_reinforced_legs_move");
    private static final Identifier REFLEX_NODE_MOVE = id("augment_reflex_node_move");
    private static final Identifier REFLEX_NODE_ATTACK = id("augment_reflex_node_attack");
    private static final Identifier ARTIFICIAL_HEART_HEALTH = id("augment_artificial_heart_health");
    private static final Identifier DUAL_HEART_HEALTH = id("augment_dual_heart_health");
    private static final Identifier ADRENALINE_ATTACK = id("augment_adrenaline_attack");
    private static final Identifier ADRENALINE_MOVE = id("augment_adrenaline_move");
    private static final Identifier ADRENALINE_KB = id("augment_adrenaline_knockback");

    private static final Map<Identifier, ModifierSpec> ALL = new LinkedHashMap<>();
    private static final Map<UUID, Float> LAST_HEALTH = new HashMap<>();
    private static final Map<UUID, Integer> LAST_DAMAGE_TICK = new HashMap<>();
    private static final Map<UUID, Integer> DUAL_HEART_READY_TICK = new HashMap<>();

    static {
        add(Attributes.ATTACK_DAMAGE, SPINE_ATTACK, 0.20D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(Attributes.MOVEMENT_SPEED, SPINE_MOVE, 0.08D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(Attributes.KNOCKBACK_RESISTANCE, SPINE_KB, 0.15D, AttributeModifier.Operation.ADD_VALUE);
        add(Attributes.KNOCKBACK_RESISTANCE, GYRO_KB, 0.35D, AttributeModifier.Operation.ADD_VALUE);
        add(Attributes.MAX_HEALTH, SKELETON_HEALTH, 8.0D, AttributeModifier.Operation.ADD_VALUE);
        add(Attributes.ARMOR, SKELETON_ARMOR, 4.0D, AttributeModifier.Operation.ADD_VALUE);
        add(Attributes.ATTACK_DAMAGE, SKELETON_ATTACK, 0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(Attributes.KNOCKBACK_RESISTANCE, SKELETON_KB, 0.0D, AttributeModifier.Operation.ADD_VALUE);
        add(Attributes.ARMOR_TOUGHNESS, IMPACT_TOUGHNESS, 4.0D, AttributeModifier.Operation.ADD_VALUE);
        add(Attributes.KNOCKBACK_RESISTANCE, IMPACT_KB, 0.25D, AttributeModifier.Operation.ADD_VALUE);
        add(Attributes.ARMOR, SKIN_ARMOR, 6.0D, AttributeModifier.Operation.ADD_VALUE);
        add(Attributes.ATTACK_DAMAGE, BLADE_DAMAGE, 3.0D, AttributeModifier.Operation.ADD_VALUE);
        add(Attributes.MOVEMENT_SPEED, LEGS_MOVE, 0.15D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(Attributes.MOVEMENT_SPEED, REFLEX_NODE_MOVE, 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(Attributes.ATTACK_SPEED, REFLEX_NODE_ATTACK, 0.12D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(Attributes.MAX_HEALTH, ARTIFICIAL_HEART_HEALTH, CombatScale.toInternal(20.0D), AttributeModifier.Operation.ADD_VALUE);
        add(Attributes.MAX_HEALTH, DUAL_HEART_HEALTH, CombatScale.toInternal(40.0D), AttributeModifier.Operation.ADD_VALUE);
        add(Attributes.ATTACK_DAMAGE, ADRENALINE_ATTACK, 0.20D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(Attributes.MOVEMENT_SPEED, ADRENALINE_MOVE, 0.15D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(Attributes.KNOCKBACK_RESISTANCE, ADRENALINE_KB, 0.0D, AttributeModifier.Operation.ADD_VALUE);
    }

    private AugmentationEffectService() {}

    private static void add(Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
        ALL.put(id, new ModifierSpec(attribute, id, amount, operation));
    }

    public static void tick(ServerPlayer player, TitanPlayerData.State state) {
        trackDamage(player);

        int poweredSpine = enhancement(state, "powered_spine");
        set(player, SPINE_ATTACK, state.hasInstalled("powered_spine"), 0.20D + (poweredSpine >= 10 ? 0.15D : 0.0D));
        set(player, SPINE_MOVE, state.hasInstalled("powered_spine"));
        set(player, SPINE_KB, state.hasInstalled("powered_spine"), 0.15D + (poweredSpine >= 7 ? 0.20D : 0.0D));

        int gyro = enhancement(state, "gyro_stabilized_spine");
        set(player, GYRO_KB, state.hasInstalled("gyro_stabilized_spine"), gyro >= 10 ? 0.60D : 0.35D);

        int skeleton = enhancement(state, "bioalloy_skeleton");
        set(player, SKELETON_HEALTH, state.hasInstalled("bioalloy_skeleton"));
        set(player, SKELETON_ARMOR, state.hasInstalled("bioalloy_skeleton"), 4.0D + (skeleton >= 3 ? 2.0D : 0.0D));
        set(player, SKELETON_ATTACK, state.hasInstalled("bioalloy_skeleton"), 0.10D + (skeleton >= 10 ? 0.10D : 0.0D));
        set(player, SKELETON_KB, state.hasInstalled("bioalloy_skeleton") && skeleton >= 5, 0.20D);

        int impact = enhancement(state, "impact_dispersal_frame");
        set(player, IMPACT_TOUGHNESS, state.hasInstalled("impact_dispersal_frame"), 4.0D + (impact >= 5 ? 2.0D : 0.0D));
        set(player, IMPACT_KB, state.hasInstalled("impact_dispersal_frame"), impact >= 10 ? 0.50D : 0.25D);

        int skin = enhancement(state, "subdermal_armor");
        set(player, SKIN_ARMOR, state.hasInstalled("subdermal_armor"), 6.0D + (skin >= 5 ? 3.0D : 0.0D));

        boolean blade = "blade_arm".equals(state.installed(AugmentationCatalog.Slot.LEFT_ARM_MAIN))
                || "blade_arm".equals(state.installed(AugmentationCatalog.Slot.RIGHT_ARM_MAIN));
        set(player, BLADE_DAMAGE, blade);

        int legs = enhancement(state, "reinforced_legs");
        set(player, LEGS_MOVE, state.hasInstalled("reinforced_legs"), 0.15D + (legs >= 5 ? 0.08D : 0.0D));
        set(player, REFLEX_NODE_MOVE, state.hasInstalled("reflex_accelerator"));
        set(player, REFLEX_NODE_ATTACK, state.hasInstalled("reflex_accelerator"));

        tickArtificialHeart(player, state);
        tickDualHeart(player, state);
        tickAdrenaline(player, state);
        tickNanoRepair(player, state);

        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        LAST_HEALTH.put(player.getUUID(), player.getHealth());
    }

    private static void tickArtificialHeart(ServerPlayer player, TitanPlayerData.State state) {
        TitanPlayerData.AugmentInstance heart = state.firstInstalledInstance("artificial_heart");
        if (heart == null) {
            set(player, ARTIFICIAL_HEART_HEALTH, false);
            return;
        }
        double healthBonus = CombatScale.toInternal(20.0D + (heart.enhancement() >= 5 ? 10.0D : 0.0D));
        set(player, ARTIFICIAL_HEART_HEALTH, true, healthBonus);
        int interval = heart.enhancement() >= 7 ? 60 : 100;
        if (player.tickCount % interval == 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal((float) (heart.enhancement() >= 7 ? CombatScale.toInternal(2.5D) : CombatScale.toInternal(1.0D)));
        }
    }

    private static void tickDualHeart(ServerPlayer player, TitanPlayerData.State state) {
        TitanPlayerData.AugmentInstance heart = state.firstInstalledInstance("dual_heart");
        if (heart == null) {
            set(player, DUAL_HEART_HEALTH, false);
            DUAL_HEART_READY_TICK.remove(player.getUUID());
            return;
        }
        set(player, DUAL_HEART_HEALTH, true, CombatScale.toInternal(40.0D));
        int lastDamage = LAST_DAMAGE_TICK.getOrDefault(player.getUUID(), Integer.MIN_VALUE / 2);
        int readyTick = DUAL_HEART_READY_TICK.getOrDefault(player.getUUID(), 0);
        boolean damagedRecently = player.tickCount - lastDamage <= 20;
        if (damagedRecently && player.tickCount >= readyTick && player.getHealth() > 0.0F
                && player.getHealth() <= player.getMaxHealth() * 0.25F) {
            double visibleBurst = heart.enhancement() >= 10 ? 45.0D : heart.enhancement() >= 5 ? 40.0D : 30.0D;
            player.heal((float) CombatScale.toInternal(visibleBurst));
            int cooldown = heart.enhancement() >= 7 ? 500 : 800;
            DUAL_HEART_READY_TICK.put(player.getUUID(), player.tickCount + cooldown);
        }
    }

    private static void tickAdrenaline(ServerPlayer player, TitanPlayerData.State state) {
        TitanPlayerData.AugmentInstance pump = state.firstInstalledInstance("adrenaline_pump");
        if (pump == null) {
            set(player, ADRENALINE_ATTACK, false);
            set(player, ADRENALINE_MOVE, false);
            set(player, ADRENALINE_KB, false);
            return;
        }
        double threshold = pump.enhancement() >= 5 ? 0.40D : 0.30D;
        boolean active = player.getHealth() <= player.getMaxHealth() * threshold;
        set(player, ADRENALINE_ATTACK, active, 0.20D + Math.min(0.10D, pump.enhancement() * 0.01D));
        set(player, ADRENALINE_MOVE, active, 0.15D + Math.min(0.08D, pump.enhancement() * 0.008D));
        set(player, ADRENALINE_KB, active && pump.enhancement() >= 10, 0.50D);
    }

    private static void tickNanoRepair(ServerPlayer player, TitanPlayerData.State state) {
        TitanPlayerData.AugmentInstance nano = state.firstInstalledInstance("nano_repair_organ");
        if (nano == null || player.getHealth() >= player.getMaxHealth()) return;
        int sinceDamage = player.tickCount - LAST_DAMAGE_TICK.getOrDefault(player.getUUID(), Integer.MIN_VALUE / 2);
        boolean combatRepair = nano.enhancement() >= 7 && sinceDamage >= 20;
        boolean safeRepair = sinceDamage >= 100;
        if (!combatRepair && !safeRepair) return;
        int interval = nano.enhancement() >= 5 ? 30 : 40;
        if (player.tickCount % interval != 0) return;
        double visibleHeal = combatRepair && !safeRepair ? 1.0D : 2.5D;
        player.heal((float) CombatScale.toInternal(visibleHeal));
    }

    private static void trackDamage(ServerPlayer player) {
        UUID id = player.getUUID();
        float current = player.getHealth();
        Float previous = LAST_HEALTH.get(id);
        if (previous != null && current + 0.01F < previous) LAST_DAMAGE_TICK.put(id, player.tickCount);
        LAST_HEALTH.put(id, current);
    }

    private static int enhancement(TitanPlayerData.State state, String augmentId) {
        TitanPlayerData.AugmentInstance instance = state.firstInstalledInstance(augmentId);
        return instance == null ? 0 : instance.enhancement();
    }

    public static void clear(ServerPlayer player) {
        for (ModifierSpec spec : ALL.values()) remove(player, spec);
        UUID id = player.getUUID();
        LAST_HEALTH.remove(id);
        LAST_DAMAGE_TICK.remove(id);
        DUAL_HEART_READY_TICK.remove(id);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void set(ServerPlayer player, Identifier id, boolean enabled) {
        ModifierSpec spec = ALL.get(id);
        if (spec == null) return;
        set(player, id, enabled, spec.amount());
    }

    private static void set(ServerPlayer player, Identifier id, boolean enabled, double amount) {
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
                && Math.abs(current.amount() - amount) < 1.0E-8D) return;
        instance.addOrUpdateTransientModifier(new AttributeModifier(spec.id(), amount, spec.operation()));
    }

    private static void remove(ServerPlayer player, ModifierSpec spec) {
        AttributeInstance instance = player.getAttribute(spec.attribute());
        if (instance != null && instance.hasModifier(spec.id())) instance.removeModifier(spec.id());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, path);
    }
}

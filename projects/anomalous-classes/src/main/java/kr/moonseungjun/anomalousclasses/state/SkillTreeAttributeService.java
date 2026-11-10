package kr.moonseungjun.anomalousclasses.state;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Applies only the purchased vanilla-stat nodes for the currently selected class. */
public final class SkillTreeAttributeService {
    private static final ResourceLocation HEALTH = id("skill_tree_max_health");
    private static final ResourceLocation ARMOR = id("skill_tree_armor");
    private static final ResourceLocation MOVEMENT = id("skill_tree_movement_speed");
    private static final ResourceLocation ATTACK = id("skill_tree_attack_damage");

    private SkillTreeAttributeService() {}

    public static void apply(ServerPlayer player, ClassId classId, ClassProgress progress) {
        float previousHealth = player.getHealth();
        float previousMaximum = player.getMaxHealth();
        remove(player, HEALTH, Attributes.MAX_HEALTH);
        remove(player, ARMOR, Attributes.ARMOR);
        remove(player, MOVEMENT, Attributes.MOVEMENT_SPEED);
        remove(player, ATTACK, Attributes.ATTACK_DAMAGE);

        GraphSkillTreeCatalog.StatTotals totals = GraphSkillTreeCatalog.statTotals(classId, progress.purchasedNodes());
        add(player, HEALTH, Attributes.MAX_HEALTH, totals.maxHealth(), AttributeModifier.Operation.ADD_VALUE);
        add(player, ARMOR, Attributes.ARMOR, totals.armor(), AttributeModifier.Operation.ADD_VALUE);
        add(player, MOVEMENT, Attributes.MOVEMENT_SPEED, totals.movementSpeedPercent() / 100.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        add(player, ATTACK, Attributes.ATTACK_DAMAGE, totals.attackDamage(), AttributeModifier.Operation.ADD_VALUE);

        float updatedMaximum = player.getMaxHealth();
        if (previousMaximum > 0.0F && updatedMaximum > 0.0F && player.isAlive()) {
            float ratio = Math.max(0.0F, Math.min(1.0F, previousHealth / previousMaximum));
            player.setHealth(Math.min(updatedMaximum, updatedMaximum * ratio));
        }
    }

    private static void remove(ServerPlayer player, ResourceLocation id,
                                Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }

    private static void add(ServerPlayer player, ResourceLocation id,
            Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            double amount, AttributeModifier.Operation operation) {
        if (amount == 0.0) return;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, operation));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, path);
    }
}

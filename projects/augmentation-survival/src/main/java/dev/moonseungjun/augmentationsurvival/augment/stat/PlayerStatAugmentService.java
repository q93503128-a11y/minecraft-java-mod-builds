package dev.moonseungjun.augmentationsurvival.augment.stat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.Objects;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Applies persistent augment levels to transient vanilla player attributes. */
public final class PlayerStatAugmentService {
    private static final String GLASS_CANNON_ID = AugmentationSurvival.MOD_ID + ":glass_cannon";
    private static final Identifier GLASS_CANNON_HEALTH_MODIFIER = Identifier.fromNamespaceAndPath(
            AugmentationSurvival.MOD_ID,
            "augment_stat/max_health/glass_cannon_total"
    );

    private PlayerStatAugmentService() {
    }

    public static void initialize() {
        ServerPlayerEvents.JOIN.register(PlayerStatAugmentService::reconcile);
        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> reconcile(newPlayer)
        );
    }

    public static void reconcile(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        reconcile(player, AugmentationPlayerStateService.state(player));
    }

    public static void reconcile(ServerPlayer player, AugmentationPlayerState state) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(state, "state");

        for (StatAugmentCatalog.ModifierGroup group : StatAugmentCatalog.current().groups()) {
            double totalAmount = 0.0D;
            for (StatAugmentCatalog.StatModifierDefinition definition : group.definitions()) {
                int level = state.level(definition.augmentId());
                if (level <= 0) {
                    continue;
                }
                int boundedLevel = Math.min(
                        level,
                        AugmentCatalog.require(definition.augmentId()).maxLevel()
                );
                totalAmount += boundedLevel * definition.amountPerLevel();
            }
            applyGroup(player, group, totalAmount);
        }
        applyGlassCannon(player, state);
    }

    private static void applyGlassCannon(ServerPlayer player, AugmentationPlayerState state) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            throw new IllegalStateException("Player is missing max-health attribute");
        }
        int level = state.level(GLASS_CANNON_ID);
        if (level <= 0) {
            health.removeModifier(GLASS_CANNON_HEALTH_MODIFIER);
            return;
        }
        level = Math.min(level, AugmentCatalog.require(GLASS_CANNON_ID).maxLevel());
        double multiplier = ExpansionTuning.current().glassCannonHealthMultiplier(level);
        double amount = multiplier - 1.0D;
        if (Math.abs(amount) < 1.0E-9D) {
            health.removeModifier(GLASS_CANNON_HEALTH_MODIFIER);
        } else {
            health.addOrUpdateTransientModifier(new AttributeModifier(
                    GLASS_CANNON_HEALTH_MODIFIER,
                    amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void applyGroup(
            ServerPlayer player,
            StatAugmentCatalog.ModifierGroup group,
            double amount
    ) {
        AttributeInstance instance = player.getAttribute(group.attribute().attribute());
        if (instance == null) {
            throw new IllegalStateException(
                    "Player is missing required attribute for stat augment: " + group.attribute().id()
            );
        }

        if (amount <= 0.0D) {
            instance.removeModifier(group.modifierId());
            return;
        }

        instance.addOrUpdateTransientModifier(new AttributeModifier(
                group.modifierId(),
                amount,
                group.operation()
        ));
    }
}

package dev.moonseungjun.augmentationsurvival.augment.stat;

import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.Objects;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Applies persistent augment levels to transient vanilla player attributes. */
public final class PlayerStatAugmentService {
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

package dev.moonseungjun.augmentationsurvival.threat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.Objects;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Applies save-wide threat scaling without per-tick hostile scans. */
public final class ThreatScalingService {
    private static final Identifier HEALTH_MODIFIER = id("threat/max_health");
    private static final Identifier SPEED_MODIFIER = id("threat/movement_speed");
    private static final Identifier ARMOR_MODIFIER = id("threat/armor");
    private static final Identifier KNOCKBACK_MODIFIER = id("threat/knockback_resistance");
    private static final EnemyScalingCatalog.Profile COMPATIBILITY_BOSS_PROFILE =
            new EnemyScalingCatalog.Profile(
                    "compatibility_boss",
                    EnemyScalingCatalog.ScaleGroup.BOSS,
                    1.0D,
                    1.0D,
                    1.0D,
                    1.0D,
                    1.0D
            );

    private ThreatScalingService() {
    }

    public static void initialize() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof LivingEntity living) {
                reconcile(living, level);
            }
        });
        ServerPlayerEvents.JOIN.register(player -> observePlayerPower(
                player,
                AugmentationPlayerStateService.state(player)
        ));
        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> observePlayerPower(
                        newPlayer,
                        AugmentationPlayerStateService.state(newPlayer)
                )
        );
    }

    public static void observePlayerPower(ServerPlayer player, AugmentationPlayerState state) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(state, "state");
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        WorldThreatState before = WorldThreatService.state(server);
        WorldThreatState after = WorldThreatService.observePowerScore(
                server,
                AugmentCatalog.powerScore(state.augmentLevels())
        );
        if (after.stage() > before.stage()) {
            refreshLoadedEntities(server, before.stage(), after.stage());
        }
    }

    public static void reconcile(LivingEntity entity, ServerLevel level) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(level, "level");
        EnemyScalingCatalog.Profile profile = resolveProfile(entity);
        if (profile.scaleGroup() == EnemyScalingCatalog.ScaleGroup.EXCLUDED) {
            return;
        }

        int targetStage = WorldThreatService.state(level.getServer()).stage();
        if (profile.scaleGroup() == EnemyScalingCatalog.ScaleGroup.BOSS) {
            ThreatEntityState state = entity.getAttachedOrSet(
                    ThreatEntityAttachments.ENTITY_STATE,
                    ThreatEntityState.initial()
            );
            if (state.bossSnapshotStage() < 0) {
                state = new ThreatEntityState(targetStage);
                entity.setAttached(ThreatEntityAttachments.ENTITY_STATE, state);
            }
            targetStage = state.bossSnapshotStage();
        }
        applyScale(entity, EnemyScalingCatalog.scaleFor(targetStage, profile));
    }

    /** Applies hostile damage scaling once at the shared pre-mitigation damage boundary. */
    public static float modifyHostileOutgoingDamage(DamageSource source, float damage) {
        if (!(damage > 0.0F) || !Float.isFinite(damage)) {
            return damage;
        }
        Entity dealer = source.getEntity();
        if (!(dealer instanceof LivingEntity living) || !(living.level() instanceof ServerLevel level)) {
            return damage;
        }
        EnemyScalingCatalog.Profile profile = resolveProfile(living);
        if (profile.scaleGroup() == EnemyScalingCatalog.ScaleGroup.EXCLUDED) {
            return damage;
        }

        int stage = WorldThreatService.state(level.getServer()).stage();
        if (profile.scaleGroup() == EnemyScalingCatalog.ScaleGroup.BOSS) {
            ThreatEntityState state = living.getAttachedOrSet(
                    ThreatEntityAttachments.ENTITY_STATE,
                    ThreatEntityState.initial()
            );
            if (state.bossSnapshotStage() < 0) {
                state = new ThreatEntityState(stage);
                living.setAttached(ThreatEntityAttachments.ENTITY_STATE, state);
            }
            stage = state.bossSnapshotStage();
        }

        double scaled = damage * EnemyScalingCatalog.scaleFor(stage, profile).damageMultiplier();
        if (!Double.isFinite(scaled)) {
            return Float.MAX_VALUE;
        }
        return (float) Math.min(Float.MAX_VALUE, Math.max(0.0D, scaled));
    }

    private static EnemyScalingCatalog.Profile resolveProfile(LivingEntity entity) {
        if (EntityCompatibilityProfileService.isBoss(entity)) {
            return COMPATIBILITY_BOSS_PROFILE;
        }
        return EnemyScalingCatalog.resolveProfile(entity);
    }

    private static void refreshLoadedEntities(MinecraftServer server, int previousStage, int newStage) {
        int refreshed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity living)) {
                    continue;
                }
                EnemyScalingCatalog.Profile profile = resolveProfile(living);
                if (profile.scaleGroup() == EnemyScalingCatalog.ScaleGroup.EXCLUDED) {
                    continue;
                }
                reconcile(living, level);
                refreshed++;
            }
        }
        AugmentationSurvival.LOGGER.info(
                "Threat stage advanced {} -> {}; refreshed {} loaded scalable entities",
                previousStage,
                newStage,
                refreshed
        );
    }

    private static void applyScale(
            LivingEntity entity,
            EnemyScalingCatalog.EffectiveScale scale
    ) {
        float oldMaxHealth = entity.getMaxHealth();
        float healthRatio = oldMaxHealth > 0.0F
                ? Math.max(0.0F, Math.min(1.0F, entity.getHealth() / oldMaxHealth))
                : 1.0F;

        applyMultiplier(entity.getAttribute(Attributes.MAX_HEALTH), HEALTH_MODIFIER, scale.healthMultiplier());
        applyMultiplier(entity.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_MODIFIER, scale.speedMultiplier());
        applyAdditive(entity.getAttribute(Attributes.ARMOR), ARMOR_MODIFIER, scale.armorAdd());
        applyAdditive(
                entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE),
                KNOCKBACK_MODIFIER,
                scale.knockbackResistanceAdd()
        );

        float newMaxHealth = entity.getMaxHealth();
        if (newMaxHealth > 0.0F) {
            entity.setHealth(Math.max(0.0F, Math.min(newMaxHealth, newMaxHealth * healthRatio)));
        }
    }

    private static void applyMultiplier(
            AttributeInstance instance,
            Identifier modifierId,
            double multiplier
    ) {
        if (instance == null) {
            return;
        }
        double amount = multiplier - 1.0D;
        if (Math.abs(amount) < 1.0E-9D) {
            instance.removeModifier(modifierId);
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(
                modifierId,
                amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }

    private static void applyAdditive(
            AttributeInstance instance,
            Identifier modifierId,
            double amount
    ) {
        if (instance == null) {
            return;
        }
        if (Math.abs(amount) < 1.0E-9D) {
            instance.removeModifier(modifierId);
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(
                modifierId,
                amount,
                AttributeModifier.Operation.ADD_VALUE
        ));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, path);
    }
}

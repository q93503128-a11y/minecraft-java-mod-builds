package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.mixin.SpecialActiveAugmentServiceInvoker;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Dedicated server-authoritative execution path for the mythic percent-health strike. */
public final class DivinePunishmentService {
    public static final String ID = AugmentationSurvival.MOD_ID + ":divine_punishment";
    private static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "divine_punishment")
    );
    private static final List<VisualStrike> VISUAL_STRIKES = new ArrayList<>();
    private static boolean initialized;

    private DivinePunishmentService() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(DivinePunishmentService::tickVisualStrikes);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> VISUAL_STRIKES.clear());
    }

    public static boolean tryUse(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int augmentLevel = boundedLevel(state);
        long now = player.level().getGameTime();
        if (augmentLevel <= 0 || now < state.cooldownReadyTick(ID)) return false;

        SpecialActiveTuning.Snapshot tuning = SpecialActiveTuning.current();
        ServerLevel level = player.level();
        double radius = tuning.divinePunishmentRadius();
        double radiusSquared = radius * radius;
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                candidate -> CompanionAugmentService.isHostileTarget(player, candidate)
                        && player.distanceToSqr(candidate) <= radiusSquared
        );
        if (targets.isEmpty()) return false;

        DamageSource source = player.damageSources().source(DAMAGE, null, player);
        boolean affected = false;
        for (LivingEntity target : targets) {
            boolean targetAffected = false;
            boolean boss = EntityCompatibilityProfileService.isBoss(target);
            if (EntityCompatibilityProfileService.allowPercentHpDamage(target)) {
                float damage = (float) Math.min(
                        Float.MAX_VALUE,
                        target.getMaxHealth() * tuning.divinePunishmentFraction(augmentLevel, boss)
                );
                if (damage > 0.0F && Float.isFinite(damage)) {
                    boolean damaged = CombatProvenanceContext.callAs(
                            CombatProvenance.SPECIAL_PERCENT_HP,
                            () -> target.hurtServer(level, source, damage)
                    );
                    targetAffected |= damaged;
                }
            }
            if (augmentLevel >= 2 && EntityCompatibilityProfileService.allowStun(target)) {
                SpecialActiveAugmentServiceInvoker.augmentationSurvival$applyStun(
                        target,
                        tuning.divinePunishmentStunTicksLevel2()
                );
                targetAffected = true;
            }
            if (targetAffected) {
                scheduleTripleLightning(level, target.position());
                affected = true;
            }
        }
        if (!affected) return false;

        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerStateService.state(player).withCooldownReadyTick(
                        ID,
                        now + tuning.divinePunishmentCooldownTicks()
                )
        );
        return true;
    }

    private static void scheduleTripleLightning(ServerLevel level, Vec3 position) {
        long now = level.getGameTime();
        ResourceKey<Level> dimension = level.dimension();
        Vec3 fixed = new Vec3(position.x, position.y, position.z);
        VISUAL_STRIKES.add(new VisualStrike(dimension, fixed, now));
        VISUAL_STRIKES.add(new VisualStrike(dimension, fixed, now + 4L));
        VISUAL_STRIKES.add(new VisualStrike(dimension, fixed, now + 8L));
    }

    private static void tickVisualStrikes(MinecraftServer server) {
        Iterator<VisualStrike> iterator = VISUAL_STRIKES.iterator();
        while (iterator.hasNext()) {
            VisualStrike strike = iterator.next();
            ServerLevel level = server.getLevel(strike.dimension());
            if (level == null) {
                iterator.remove();
                continue;
            }
            if (level.getGameTime() < strike.dueTick()) continue;
            iterator.remove();
            spawnVisualLightning(level, strike.position());
        }
    }

    private static void spawnVisualLightning(ServerLevel level, Vec3 position) {
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) return;
        bolt.setVisualOnly(true);
        bolt.snapTo(position);
        level.addFreshEntity(bolt);
    }

    private static int boundedLevel(AugmentationPlayerState state) {
        int level = state.level(ID);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(ID).maxLevel());
    }

    private record VisualStrike(ResourceKey<Level> dimension, Vec3 position, long dueTick) {
    }
}

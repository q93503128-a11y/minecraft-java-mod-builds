package dev.moonseungjun.augmentationsurvival.augment;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative runtime for the final non-resource/non-platform augment set. */
public final class FinalAugmentRuntimeService {
    private static final String POTION_DIFFUSION_ID = id("potion_diffusion");
    private static final String ELASTIC_STORAGE_ID = id("elastic_storage");
    private static final String BALLISTIC_BARRIER_ID = id("ballistic_barrier");
    private static final String LIFE_LINK_ID = id("life_link");
    private static final String SHRINK_ID = id("shrink");
    private static final String WALL_SLAM_ID = id("wall_slam");

    private static final Identifier SHRINK_SCALE_MODIFIER = Identifier.fromNamespaceAndPath(
            AugmentationSurvival.MOD_ID,
            "final_runtime/shrink_scale"
    );

    private static final Map<UUID, PlayerRuntime> PLAYER_RUNTIME = new HashMap<>();
    private static final Map<UUID, LifeLinkGroup> LIFE_LINKS = new HashMap<>();
    private static final Map<UUID, WallSlamWindow> WALL_SLAMS = new HashMap<>();

    private FinalAugmentRuntimeService() {
    }

    public static void initialize() {
        for (String augmentId : new String[]{
                POTION_DIFFUSION_ID, ELASTIC_STORAGE_ID, BALLISTIC_BARRIER_ID,
                LIFE_LINK_ID, SHRINK_ID, WALL_SLAM_ID
        }) {
            AugmentCatalog.require(augmentId);
        }
        ServerPlayerEvents.JOIN.register(FinalAugmentRuntimeService::clearPlayerRuntime);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> clearPlayerRuntime(newPlayer));
        ServerPlayerEvents.LEAVE.register(FinalAugmentRuntimeService::clearPlayerRuntime);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(FinalAugmentRuntimeService::afterDamage);
        ServerTickEvents.END_SERVER_TICK.register(FinalAugmentRuntimeService::tickRuntime);
    }

    /** Called only from the direct PotionItem consumption boundary. */
    public static void onPotionConsumed(ServerPlayer player, ItemStack consumedStack) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int augmentLevel = boundedLevel(state, POTION_DIFFUSION_ID);
        if (augmentLevel <= 0) {
            return;
        }
        PotionContents contents = consumedStack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return;
        }
        FinalAugmentTuning.Snapshot tuning = FinalAugmentTuning.current();
        double fraction = tuning.potionDiffusionFraction(augmentLevel);
        double radius = tuning.potionDiffusionRadius();
        double radiusSquared = radius * radius;
        List<LivingEntity> allies = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                candidate -> candidate != player
                        && !candidate.isDeadOrDying()
                        && player.distanceToSqr(candidate) <= radiusSquared
                        && isPotionAlly(player, candidate)
        );
        if (allies.isEmpty()) {
            return;
        }
        ServerLevel level = player.level();
        for (MobEffectInstance sourceEffect : contents.getAllEffects()) {
            if (sourceEffect.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL) {
                continue;
            }
            for (LivingEntity ally : allies) {
                if (sourceEffect.getEffect().value().isInstantaneous()) {
                    sourceEffect.getEffect().value().applyInstantaneousEffect(
                            level,
                            player,
                            player,
                            ally,
                            sourceEffect.getAmplifier(),
                            fraction
                    );
                } else {
                    int duration = Math.max(1, (int) Math.round(sourceEffect.getDuration() * fraction));
                    ally.addEffect(new MobEffectInstance(
                            sourceEffect.getEffect(),
                            duration,
                            sourceEffect.getAmplifier(),
                            sourceEffect.isAmbient(),
                            sourceEffect.isVisible(),
                            sourceEffect.showIcon()
                    ), player);
                }
            }
        }
    }

    /** Consumes a stored Elastic Storage landing on the next ordinary ground jump. */
    public static void afterGroundJump(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int augmentLevel = boundedLevel(state, ELASTIC_STORAGE_ID);
        if (augmentLevel <= 0) {
            return;
        }
        PlayerRuntime runtime = PLAYER_RUNTIME.get(player.getUUID());
        long now = player.level().getGameTime();
        if (runtime == null || !(runtime.storedFallDistance > 0.0D) || runtime.elasticExpiresAtTick < now) {
            return;
        }
        FinalAugmentTuning.Snapshot tuning = FinalAugmentTuning.current();
        double bonus = runtime.storedFallDistance
                * tuning.elasticVerticalVelocityPerFallBlock()
                * tuning.elasticReturnFraction(augmentLevel);
        bonus = Math.min(tuning.elasticMaximumVerticalBonus(), bonus);
        runtime.storedFallDistance = 0.0D;
        runtime.elasticExpiresAtTick = 0L;
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, motion.y + bonus, motion.z);
        player.resetFallDistance();
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private static void afterDamage(
            LivingEntity target,
            DamageSource source,
            float baseDamageTaken,
            float damageTaken,
            boolean blocked
    ) {
        if (blocked || !(damageTaken > 0.0F) || !Float.isFinite(damageTaken)) {
            return;
        }
        CombatProvenance provenance = CombatProvenanceContext.resolve(source);
        if (source.getEntity() instanceof ServerPlayer attacker && provenance.isRootDirect()
                && CompanionAugmentService.isHostileTarget(attacker, target)) {
            AugmentationPlayerState state = AugmentationPlayerStateService.state(attacker);
            if (boundedLevel(state, LIFE_LINK_ID) > 0) {
                buildLifeLink(attacker, target);
            }
            int wallLevel = boundedLevel(state, WALL_SLAM_ID);
            if (wallLevel > 0
                    && provenance == CombatProvenance.ROOT_MELEE
                    && baseDamageTaken > 0.0F
                    && EntityCompatibilityProfileService.allowWallImpact(target)) {
                long now = target.level().getGameTime();
                WALL_SLAMS.put(target.getUUID(), new WallSlamWindow(
                        target,
                        attacker.getUUID(),
                        wallLevel,
                        baseDamageTaken,
                        now,
                        now + FinalAugmentTuning.current().wallSlamCollisionWindowTicks()
                ));
            }
        }

        if (provenance != CombatProvenance.LIFE_LINK
                && provenance != CombatProvenance.SPECIAL_PERCENT_HP) {
            shareLifeLinkDamage(target, damageTaken);
        }
    }

    private static void buildLifeLink(ServerPlayer owner, LivingEntity primary) {
        FinalAugmentTuning.Snapshot tuning = FinalAugmentTuning.current();
        double radius = tuning.lifeLinkRadius();
        double radiusSquared = radius * radius;
        List<LivingEntity> candidates = owner.level().getEntitiesOfClass(
                LivingEntity.class,
                primary.getBoundingBox().inflate(radius),
                candidate -> candidate != primary
                        && CompanionAugmentService.isHostileTarget(owner, candidate)
                        && primary.distanceToSqr(candidate) <= radiusSquared
        );
        candidates.sort(Comparator.comparingDouble(primary::distanceToSqr));
        LinkedHashSet<UUID> targets = new LinkedHashSet<>();
        targets.add(primary.getUUID());
        int additional = Math.max(0, tuning.lifeLinkMaxTargets() - 1);
        for (int i = 0; i < Math.min(additional, candidates.size()); i++) {
            targets.add(candidates.get(i).getUUID());
        }
        LIFE_LINKS.put(owner.getUUID(), new LifeLinkGroup(
                owner.getUUID(),
                owner.level().dimension(),
                Set.copyOf(targets),
                owner.level().getGameTime() + tuning.lifeLinkDurationTicks()
        ));
    }

    private static void shareLifeLinkDamage(LivingEntity damaged, float damageTaken) {
        if (!(damaged.level() instanceof ServerLevel damagedLevel)) {
            return;
        }
        MinecraftServer server = damagedLevel.getServer();
        long now = damaged.level().getGameTime();
        for (LifeLinkGroup group : new ArrayList<>(LIFE_LINKS.values())) {
            if (group.expiresAtTick <= now || !group.targetIds.contains(damaged.getUUID())) {
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(group.ownerId);
            if (owner == null || owner.level().dimension() != group.dimension) {
                continue;
            }
            float shared = (float) (damageTaken * FinalAugmentTuning.current().lifeLinkSharedDamageFraction());
            if (!(shared > 0.0F)) {
                continue;
            }
            for (UUID targetId : group.targetIds) {
                if (targetId.equals(damaged.getUUID())) {
                    continue;
                }
                Entity entity = ((ServerLevel) owner.level()).getEntity(targetId);
                if (!(entity instanceof LivingEntity linked) || linked.isDeadOrDying()) {
                    continue;
                }
                DamageSource linkedSource = owner.damageSources().playerAttack(owner);
                CombatProvenanceContext.runAs(
                        CombatProvenance.LIFE_LINK,
                        () -> linked.hurtServer((ServerLevel) linked.level(), linkedSource, shared)
                );
            }
        }
    }

    private static void tickRuntime(MinecraftServer server) {
        long globalNow = server.overworld().getGameTime();
        LIFE_LINKS.entrySet().removeIf(entry -> entry.getValue().expiresAtTick <= globalNow);
        tickWallSlams(server);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
            PlayerRuntime runtime = PLAYER_RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new PlayerRuntime());
            long now = player.level().getGameTime();
            tickElastic(player, state, runtime, now);
            tickShrink(player, state, runtime);
            tickBallisticBarrier(player, state, runtime, now);
        }
    }

    private static void tickElastic(
            ServerPlayer player,
            AugmentationPlayerState state,
            PlayerRuntime runtime,
            long now
    ) {
        int augmentLevel = boundedLevel(state, ELASTIC_STORAGE_ID);
        if (runtime.elasticExpiresAtTick > 0L && runtime.elasticExpiresAtTick < now) {
            runtime.storedFallDistance = 0.0D;
            runtime.elasticExpiresAtTick = 0L;
        }
        if (augmentLevel <= 0) {
            runtime.maxObservedFallDistance = 0.0D;
            runtime.storedFallDistance = 0.0D;
            runtime.wasGrounded = player.onGround();
            return;
        }

        boolean grounded = player.onGround();
        if (!grounded && !player.isInWater() && !player.onClimbable()) {
            runtime.maxObservedFallDistance = Math.max(runtime.maxObservedFallDistance, player.fallDistance);
        }
        if (grounded && !runtime.wasGrounded) {
            double observed = runtime.maxObservedFallDistance;
            if (observed >= FinalAugmentTuning.current().elasticMinimumFallDistance()) {
                runtime.storedFallDistance = observed;
                runtime.elasticExpiresAtTick = now + FinalAugmentTuning.current().elasticStorageTicks();
            }
            runtime.maxObservedFallDistance = 0.0D;
        } else if (player.isInWater() || player.onClimbable()) {
            runtime.maxObservedFallDistance = 0.0D;
        }
        runtime.wasGrounded = grounded;
    }

    private static void tickShrink(
            ServerPlayer player,
            AugmentationPlayerState state,
            PlayerRuntime runtime
    ) {
        int augmentLevel = boundedLevel(state, SHRINK_ID);
        boolean giant = AdvancedWorldAugmentService.gigantificationKnockbackMultiplier(player) > 1.0D;
        if (augmentLevel <= 0 || giant) {
            runtime.crouchTicks = 0;
            if (runtime.shrunk) {
                removeShrink(player, runtime, giant);
            } else {
                removeShrinkModifier(player);
            }
            return;
        }

        if (player.isShiftKeyDown()) {
            if (runtime.shrunk) {
                return;
            }
            runtime.crouchTicks++;
            if (runtime.crouchTicks >= FinalAugmentTuning.current().shrinkCrouchHoldTicks()) {
                applyShrink(player, runtime);
            }
            return;
        }

        runtime.crouchTicks = 0;
        if (runtime.shrunk) {
            removeShrink(player, runtime, false);
        }
    }

    private static void applyShrink(ServerPlayer player, PlayerRuntime runtime) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale == null) {
            return;
        }
        scale.addOrUpdateTransientModifier(new AttributeModifier(
                SHRINK_SCALE_MODIFIER,
                FinalAugmentTuning.current().shrinkScaleMultiplier() - 1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
        player.refreshDimensions();
        runtime.shrunk = true;
    }

    private static boolean removeShrink(ServerPlayer player, PlayerRuntime runtime, boolean forceForGiant) {
        removeShrinkModifier(player);
        player.refreshDimensions();
        if (!player.level().noCollision(player)) {
            if (!forceForGiant) {
                applyShrink(player, runtime);
                return false;
            }
            correctCollisionUpward(player);
        }
        runtime.shrunk = false;
        return true;
    }

    private static void removeShrinkModifier(ServerPlayer player) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.removeModifier(SHRINK_SCALE_MODIFIER);
        }
    }

    private static void correctCollisionUpward(ServerPlayer player) {
        for (int dy = 0; dy <= 6; dy++) {
            AABB moved = player.getBoundingBox().move(0.0D, dy, 0.0D);
            if (player.level().noCollision(player, moved)) {
                player.teleportTo(player.getX(), player.getY() + dy, player.getZ());
                return;
            }
        }
    }

    private static void tickBallisticBarrier(
            ServerPlayer player,
            AugmentationPlayerState state,
            PlayerRuntime runtime,
            long now
    ) {
        int augmentLevel = boundedLevel(state, BALLISTIC_BARRIER_ID);
        if (augmentLevel <= 0) {
            runtime.nextBarrierPulseTick = 0L;
            return;
        }
        if (now < runtime.nextBarrierPulseTick) {
            return;
        }
        FinalAugmentTuning.Snapshot tuning = FinalAugmentTuning.current();
        runtime.nextBarrierPulseTick = now + tuning.ballisticBarrierIntervalTicks(augmentLevel);
        double radius = tuning.ballisticBarrierRadius();
        double radiusSquared = radius * radius;
        List<Projectile> hostile = player.level().getEntitiesOfClass(
                Projectile.class,
                player.getBoundingBox().inflate(radius),
                projectile -> !projectile.isRemoved()
                        && player.distanceToSqr(projectile) <= radiusSquared
                        && isHostileProjectile(player, projectile)
        );
        hostile.sort(Comparator.comparingDouble(player::distanceToSqr));
        int limit = Math.min(tuning.ballisticBarrierDestroyCount(augmentLevel), hostile.size());
        for (int i = 0; i < limit; i++) {
            Projectile projectile = hostile.get(i);
            player.level().playSound(
                    null,
                    projectile.getX(), projectile.getY(), projectile.getZ(),
                    SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS,
                    0.6F,
                    1.25F
            );
            projectile.discard();
        }
    }

    private static boolean isHostileProjectile(ServerPlayer player, Projectile projectile) {
        Entity owner = projectile.getOwner();
        if (!(owner instanceof LivingEntity livingOwner)) {
            return false;
        }
        return CompanionAugmentService.isHostileTarget(player, livingOwner);
    }

    /**
     * Arms a pending wall-slam only from a real LivingEntity knockback call in the same game tick as
     * the qualifying direct melee hit. This runs after vanilla/compatible combat has applied motion,
     * avoiding the old pre-knockback AFTER_DAMAGE timing race.
     */
    public static void noteWallSlamKnockback(LivingEntity target) {
        WallSlamWindow window = WALL_SLAMS.get(target.getUUID());
        if (window == null
                || window.target != target
                || window.createdAtTick != target.level().getGameTime()
                || target.getDeltaMovement().horizontalDistanceSqr()
                < FinalAugmentTuning.current().wallSlamMinimumHorizontalSpeedSquared()) {
            return;
        }
        window.knockbackObserved = true;
    }

    private static void tickWallSlams(MinecraftServer server) {
        Iterator<Map.Entry<UUID, WallSlamWindow>> iterator = WALL_SLAMS.entrySet().iterator();
        while (iterator.hasNext()) {
            WallSlamWindow window = iterator.next().getValue();
            LivingEntity target = window.target;
            long now = target.level().getGameTime();
            if (target.isRemoved() || target.isDeadOrDying() || now > window.expiresAtTick) {
                iterator.remove();
                continue;
            }
            if (!window.knockbackObserved || !target.horizontalCollision) {
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(window.ownerId);
            iterator.remove();
            if (owner == null || owner.level() != target.level()) {
                continue;
            }
            float damage = (float) (window.referenceDamage
                    * FinalAugmentTuning.current().wallSlamDamageFraction(window.level));
            if (!(damage > 0.0F)) {
                continue;
            }
            DamageSource wallSource = owner.damageSources().playerAttack(owner);
            CombatProvenanceContext.runAs(
                    CombatProvenance.DERIVED_AOE,
                    () -> target.hurtServer((ServerLevel) target.level(), wallSource, damage)
            );
        }
    }

    private static boolean isPotionAlly(ServerPlayer player, LivingEntity candidate) {
        if (player.isAlliedTo(candidate) || CompanionAugmentService.areAllied(player, candidate)) {
            return true;
        }
        if (candidate instanceof TamableAnimal tame && tame.isTame()) {
            LivingEntity owner = tame.getOwner();
            return owner != null && owner.getUUID().equals(player.getUUID());
        }
        return false;
    }

    private static void clearPlayerRuntime(ServerPlayer player) {
        PlayerRuntime runtime = PLAYER_RUNTIME.remove(player.getUUID());
        if (runtime != null && runtime.shrunk) {
            removeShrinkModifier(player);
            player.refreshDimensions();
        } else {
            removeShrinkModifier(player);
        }
        LIFE_LINKS.remove(player.getUUID());
        WALL_SLAMS.entrySet().removeIf(entry -> entry.getValue().ownerId.equals(player.getUUID()));
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private static final class PlayerRuntime {
        private double maxObservedFallDistance;
        private double storedFallDistance;
        private long elasticExpiresAtTick;
        private boolean wasGrounded;
        private int crouchTicks;
        private boolean shrunk;
        private long nextBarrierPulseTick;
    }

    private record LifeLinkGroup(
            UUID ownerId,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
            Set<UUID> targetIds,
            long expiresAtTick
    ) {
    }

    private static final class WallSlamWindow {
        private final LivingEntity target;
        private final UUID ownerId;
        private final int level;
        private final float referenceDamage;
        private final long createdAtTick;
        private final long expiresAtTick;
        private boolean knockbackObserved;

        private WallSlamWindow(
                LivingEntity target,
                UUID ownerId,
                int level,
                float referenceDamage,
                long createdAtTick,
                long expiresAtTick
        ) {
            this.target = target;
            this.ownerId = ownerId;
            this.level = level;
            this.referenceDamage = referenceDamage;
            this.createdAtTick = createdAtTick;
            this.expiresAtTick = expiresAtTick;
        }
    }
}

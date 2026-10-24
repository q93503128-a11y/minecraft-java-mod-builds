package dev.moonseungjun.augmentationsurvival.augment.companion;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldTuning;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

/** Server-owned permanent companion runtime for Domination. */
public final class CompanionAugmentService {
    private static final String DOMINATION_ID = id("domination");

    private CompanionAugmentService() {
    }

    public static void initialize() {
        AugmentCatalog.require(DOMINATION_ID);
        ServerTickEvents.END_SERVER_TICK.register(CompanionAugmentService::tickDominatedCompanions);
    }

    public static boolean tryDominate(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, DOMINATION_ID);
        if (level <= 0) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        DominationWorldState worldState = worldState(server);
        if (worldState.occupied(player.getUUID()) >= AdvancedWorldTuning.current().dominationCap(level)) {
            return false;
        }
        Mob target = findLookTarget(player, AdvancedWorldTuning.current().dominationRange());
        if (target == null) {
            return false;
        }
        target.setAttached(
                CompanionEntityAttachments.ENTITY_STATE,
                CompanionEntityState.dominatedBy(player.getUUID())
        );
        target.setPersistenceRequired();
        target.setTarget(null);
        setWorldState(server, worldState.withDelta(player.getUUID(), 1));
        return true;
    }

    public static boolean isDominated(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        CompanionEntityState state = entity.getAttached(CompanionEntityAttachments.ENTITY_STATE);
        return state != null && state.ownerId().isPresent();
    }

    public static Optional<UUID> ownerId(Entity entity) {
        CompanionEntityState state = entity.getAttached(CompanionEntityAttachments.ENTITY_STATE);
        return state == null ? Optional.empty() : state.ownerId();
    }

    public static boolean isOwnedBy(Entity entity, UUID ownerId) {
        return ownerId(entity).map(ownerId::equals).orElse(false);
    }

    public static boolean areAllied(Entity first, Entity second) {
        if (first == second) {
            return true;
        }
        Optional<UUID> firstOwner = ownerId(first);
        Optional<UUID> secondOwner = ownerId(second);
        if (firstOwner.isPresent() && secondOwner.isPresent() && firstOwner.get().equals(secondOwner.get())) {
            return true;
        }
        if (first instanceof ServerPlayer player && secondOwner.map(player.getUUID()::equals).orElse(false)) {
            return true;
        }
        if (second instanceof ServerPlayer player && firstOwner.map(player.getUUID()::equals).orElse(false)) {
            return true;
        }
        if (firstOwner.isPresent() && second instanceof TamableAnimal tamable && tamable.isTame()) {
            LivingEntity owner = tamable.getOwner();
            return owner != null && owner.getUUID().equals(firstOwner.get());
        }
        if (secondOwner.isPresent() && first instanceof TamableAnimal tamable && tamable.isTame()) {
            LivingEntity owner = tamable.getOwner();
            return owner != null && owner.getUUID().equals(secondOwner.get());
        }
        return false;
    }

    /** Called from the shared LivingEntity death boundary; owner need not be online. */
    public static void onActualDeath(LivingEntity entity) {
        Optional<UUID> owner = ownerId(entity);
        if (owner.isEmpty() || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        entity.setAttached(CompanionEntityAttachments.ENTITY_STATE, CompanionEntityState.initial());
        MinecraftServer server = level.getServer();
        setWorldState(server, worldState(server).withDelta(owner.get(), -1));
    }

    private static void tickDominatedCompanions(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Mob mob) || !isDominated(mob) || mob.isDeadOrDying()) {
                    continue;
                }
                mob.setPersistenceRequired();
                Optional<UUID> owner = ownerId(mob);
                if (owner.isEmpty()) {
                    continue;
                }
                LivingEntity target = mob.getTarget();
                if (target != null && (areAllied(mob, target) || target.isDeadOrDying())) {
                    mob.setTarget(null);
                    target = null;
                }
                if (target == null || !isHostileCandidate(mob, target)) {
                    LivingEntity next = findNearestHostile(mob, owner.get());
                    if (next != null) {
                        mob.setTarget(next);
                    }
                }
            }
        }
    }

    private static LivingEntity findNearestHostile(Mob companion, UUID ownerId) {
        double range = Math.max(8.0D, companion.getAttributeValue(Attributes.FOLLOW_RANGE));
        List<LivingEntity> candidates = companion.level().getEntitiesOfClass(
                LivingEntity.class,
                companion.getBoundingBox().inflate(range),
                candidate -> candidate != companion
                        && !candidate.isDeadOrDying()
                        && !isOwnedBy(candidate, ownerId)
                        && isHostileCandidate(companion, candidate)
        );
        candidates.sort(Comparator.comparingDouble(companion::distanceToSqr));
        return candidates.isEmpty() ? null : candidates.getFirst();
    }

    private static boolean isHostileCandidate(Mob companion, LivingEntity candidate) {
        return !areAllied(companion, candidate)
                && !isDominated(candidate)
                && (candidate instanceof Enemy || EntityCompatibilityProfileService.isBoss(candidate));
    }

    private static Mob findLookTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double rangeSquared = range * range;
        Mob best = null;
        double bestScore = Double.MAX_VALUE;
        List<Mob> nearby = player.level().getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(range),
                candidate -> candidate instanceof Enemy
                        && !candidate.isDeadOrDying()
                        && !EntityCompatibilityProfileService.isBoss(candidate)
                        && EntityCompatibilityProfileService.allowDominate(candidate)
                        && !isDominated(candidate)
                        && !(candidate instanceof TamableAnimal tamable && tamable.isTame())
                        && player.distanceToSqr(candidate) <= rangeSquared
                        && player.hasLineOfSight(candidate)
        );
        for (Mob candidate : nearby) {
            Vec3 toTarget = candidate.getBoundingBox().getCenter().subtract(eye);
            double along = toTarget.dot(look);
            if (!(along > 0.0D) || along > range) {
                continue;
            }
            double perpendicularSq = Math.max(0.0D, toTarget.lengthSqr() - along * along);
            double aimRadius = Math.max(0.75D, candidate.getBbWidth() * 0.75D);
            if (perpendicularSq > aimRadius * aimRadius) {
                continue;
            }
            double score = perpendicularSq + along * 1.0E-4D;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static DominationWorldState worldState(MinecraftServer server) {
        return server.overworld().getAttachedOrSet(
                DominationWorldAttachments.STATE,
                DominationWorldState.initial()
        );
    }

    private static void setWorldState(MinecraftServer server, DominationWorldState state) {
        server.overworld().setAttached(DominationWorldAttachments.STATE, state);
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}

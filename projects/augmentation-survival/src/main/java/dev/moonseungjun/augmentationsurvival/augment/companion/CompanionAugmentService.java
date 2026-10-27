package dev.moonseungjun.augmentationsurvival.augment.companion;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldTuning;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

/** Server-owned permanent companion runtime for Domination and shared hostile classification. */
public final class CompanionAugmentService {
    private static final String DOMINATION_ID = id("domination");
    private static final int WARDEN_COMMAND_ANGER = 150;
    private static final Map<UUID, Mob> LOADED_DOMINATED = new HashMap<>();
    /** Last living entity deliberately attacked by each owner. It remains the command target until invalid. */
    private static final Map<UUID, UUID> OWNER_COMMAND_TARGETS = new HashMap<>();

    private CompanionAugmentService() {
    }

    public static void initialize() {
        AugmentCatalog.require(DOMINATION_ID);
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Mob mob && isDominated(mob)) {
                mob.setPersistenceRequired();
                mob.setGlowingTag(true);
                LOADED_DOMINATED.put(mob.getUUID(), mob);
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> LOADED_DOMINATED.remove(entity.getUUID()));
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (!blocked && damageTaken > 0.0F && source.getEntity() instanceof ServerPlayer owner) {
                recordOwnerAttack(owner, entity);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LOADED_DOMINATED.clear();
            OWNER_COMMAND_TARGETS.clear();
        });
        ServerTickEvents.END_SERVER_TICK.register(CompanionAugmentService::tickDominatedCompanions);
    }

    public static boolean tryDominate(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, DOMINATION_ID);
        if (level <= 0) return false;
        MinecraftServer server = player.level().getServer();
        DominationWorldState worldState = worldState(server);
        if (worldState.occupied(player.getUUID()) >= AdvancedWorldTuning.current().dominationCap(level)) return false;
        Mob target = findLookTarget(player, AdvancedWorldTuning.current().dominationRange());
        if (target == null) return false;
        target.setAttached(CompanionEntityAttachments.ENTITY_STATE, CompanionEntityState.dominatedBy(player.getUUID()));
        target.setPersistenceRequired();
        target.setGlowingTag(true);
        clearCombatTarget(target, target.getTarget());
        if (target instanceof Warden warden) {
            warden.clearAnger(player);
        }
        LOADED_DOMINATED.put(target.getUUID(), target);
        setWorldState(server, worldState.withDelta(player.getUUID(), 1));
        refreshCombatTarget(target, player.getUUID(), server);
        return true;
    }

    public static boolean isDominated(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
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
        if (first == second) return true;
        Optional<UUID> firstOwner = ownerId(first);
        Optional<UUID> secondOwner = ownerId(second);
        if (firstOwner.isPresent() && secondOwner.isPresent() && firstOwner.get().equals(secondOwner.get())) return true;
        if (first instanceof ServerPlayer player && secondOwner.map(player.getUUID()::equals).orElse(false)) return true;
        if (second instanceof ServerPlayer player && firstOwner.map(player.getUUID()::equals).orElse(false)) return true;
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

    public static boolean isHostileEntity(LivingEntity candidate) {
        return !isDominated(candidate) && (candidate instanceof Enemy || EntityCompatibilityProfileService.isBoss(candidate));
    }

    public static boolean isHostileTarget(Entity source, LivingEntity candidate) {
        return candidate != source
                && !candidate.isDeadOrDying()
                && isHostileEntity(candidate)
                && (source == null || (!areAllied(source, candidate) && !source.isAlliedTo(candidate)));
    }

    /** Records an explicit owner attack so dominated companions can obey even against neutral mobs. */
    public static void recordOwnerAttack(ServerPlayer owner, LivingEntity target) {
        if (target == owner || target.isDeadOrDying() || areAllied(owner, target)) return;
        OWNER_COMMAND_TARGETS.put(owner.getUUID(), target.getUUID());
        for (Mob companion : LOADED_DOMINATED.values()) {
            if (!companion.isRemoved() && isOwnedBy(companion, owner.getUUID()) && companion.level() == target.level()) {
                applyCombatTarget(companion, target);
            }
        }
    }

    /** Dominated mobs may damage only true hostiles or the living target their owner deliberately attacked. */
    public static boolean mayDominatedMobDamage(Entity attacker, LivingEntity target) {
        Optional<UUID> owner = ownerId(attacker);
        if (owner.isEmpty()) return true;
        if (target == attacker || target.isDeadOrDying() || areAllied(attacker, target)) return false;
        if (isHostileEntity(target)) return true;
        UUID commanded = OWNER_COMMAND_TARGETS.get(owner.get());
        return commanded != null && commanded.equals(target.getUUID());
    }

    /** Called from the shared LivingEntity death boundary; owner need not be online. */
    public static void onActualDeath(LivingEntity entity) {
        Optional<UUID> owner = ownerId(entity);
        if (owner.isPresent() && entity.level() instanceof ServerLevel level) {
            LOADED_DOMINATED.remove(entity.getUUID());
            MinecraftServer server = level.getServer();
            setWorldState(server, worldState(server).withDelta(owner.get(), -1));
        }
        OWNER_COMMAND_TARGETS.entrySet().removeIf(entry -> entry.getValue().equals(entity.getUUID()));
    }

    /** Reclaims the domination occupancy slot and AI tracking after a successful permanent-companion revive. */
    public static void onRevivedCompanion(Mob mob) {
        Optional<UUID> owner = ownerId(mob);
        if (owner.isEmpty() || !(mob.level() instanceof ServerLevel level)) return;
        mob.setPersistenceRequired();
        mob.setGlowingTag(true);
        clearCombatTarget(mob, mob.getTarget());
        LOADED_DOMINATED.put(mob.getUUID(), mob);
        MinecraftServer server = level.getServer();
        setWorldState(server, worldState(server).withDelta(owner.get(), 1));
        refreshCombatTarget(mob, owner.get(), server);
    }

    private static void tickDominatedCompanions(MinecraftServer server) {
        for (Mob mob : new ArrayList<>(LOADED_DOMINATED.values())) {
            if (mob.isRemoved()) {
                LOADED_DOMINATED.remove(mob.getUUID());
                continue;
            }
            if (!isDominated(mob) || mob.isDeadOrDying()) continue;
            mob.setPersistenceRequired();
            mob.setGlowingTag(true);
            Optional<UUID> owner = ownerId(mob);
            if (owner.isEmpty()) continue;
            refreshCombatTarget(mob, owner.get(), server);
        }
    }

    private static void refreshCombatTarget(Mob mob, UUID ownerId, MinecraftServer server) {
        LivingEntity commanded = commandTarget(mob, ownerId);
        sanitizeWardenAnger(mob, commanded);
        if (commanded != null) {
            applyCombatTarget(mob, commanded);
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target != null && !isHostileTarget(mob, target)) {
            clearCombatTarget(mob, target);
            target = null;
        }
        if (target == null) {
            LivingEntity next = findNearestHostile(mob, ownerId);
            if (next != null) {
                applyCombatTarget(mob, next);
            }
        } else if (mob instanceof Warden) {
            applyCombatTarget(mob, target);
        }
    }

    private static void sanitizeWardenAnger(Mob mob, LivingEntity commanded) {
        if (!(mob instanceof Warden warden)) return;
        warden.getEntityAngryAt().ifPresent(angryAt -> {
            boolean commandTarget = commanded != null && angryAt.getUUID().equals(commanded.getUUID());
            if (!commandTarget && !isHostileTarget(warden, angryAt)) {
                warden.clearAnger(angryAt);
            }
        });
    }

    private static void applyCombatTarget(Mob companion, LivingEntity target) {
        if (companion instanceof Warden warden) {
            warden.increaseAngerAt(target, WARDEN_COMMAND_ANGER, false);
            warden.setAttackTarget(target);
        } else {
            companion.setTarget(target);
        }
        companion.getNavigation().moveTo(target, 1.15D);
    }

    private static void clearCombatTarget(Mob companion, LivingEntity previousTarget) {
        if (companion instanceof Warden warden) {
            if (previousTarget != null) {
                warden.clearAnger(previousTarget);
            }
            warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        } else {
            companion.setTarget(null);
        }
    }

    private static LivingEntity commandTarget(Mob companion, UUID ownerId) {
        UUID targetId = OWNER_COMMAND_TARGETS.get(ownerId);
        if (targetId == null || !(companion.level() instanceof ServerLevel level)) return null;
        Entity found = level.getEntity(targetId);
        if (!(found instanceof LivingEntity living) || living.isDeadOrDying() || living.isRemoved() || areAllied(companion, living)) {
            OWNER_COMMAND_TARGETS.remove(ownerId, targetId);
            return null;
        }
        return living;
    }

    private static LivingEntity findNearestHostile(Mob companion, UUID ownerId) {
        double range = Math.max(8.0D, companion.getAttributeValue(Attributes.FOLLOW_RANGE));
        List<LivingEntity> candidates = companion.level().getEntitiesOfClass(
                LivingEntity.class,
                companion.getBoundingBox().inflate(range),
                candidate -> candidate != companion && !isOwnedBy(candidate, ownerId) && isHostileTarget(companion, candidate)
        );
        candidates.sort(Comparator.comparingDouble(companion::distanceToSqr));
        return candidates.isEmpty() ? null : candidates.getFirst();
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
            if (!(along > 0.0D) || along > range) continue;
            double perpendicularSq = Math.max(0.0D, toTarget.lengthSqr() - along * along);
            double aimRadius = Math.max(0.75D, candidate.getBbWidth() * 0.75D);
            if (perpendicularSq > aimRadius * aimRadius) continue;
            double score = perpendicularSq + along * 1.0E-4D;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static DominationWorldState worldState(MinecraftServer server) {
        return server.overworld().getAttachedOrSet(DominationWorldAttachments.STATE, DominationWorldState.initial());
    }

    private static void setWorldState(MinecraftServer server, DominationWorldState state) {
        server.overworld().setAttached(DominationWorldAttachments.STATE, state);
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) { return AugmentationSurvival.MOD_ID + ":" + path; }
}

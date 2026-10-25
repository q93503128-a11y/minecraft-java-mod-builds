package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Heavy server-owned actives that alter collision, scale and permanent allegiance. */
public final class AdvancedWorldAugmentService {
    private static final String PHASE_SHIFT_ID = id("phase_shift");
    private static final String DOMINATION_ID = id("domination");
    private static final String GIGANTIFICATION_ID = id("gigantification");
    private static final String FORBIDDEN_MAGIC_ID = id("forbidden_magic");
    private static final String ABILITY_HASTE_ID = id("ability_haste");
    private static final String DOUBLE_CHARGE_ID = id("double_charge");
    private static final String COMBAT_CYCLE_ID = id("combat_cycle");
    private static final String OVERTIME_ID = id("overtime");

    private static final Identifier GIANT_SCALE_MODIFIER = modifier("advanced_world/giant_scale");
    private static final Identifier GIANT_HEALTH_MODIFIER = modifier("advanced_world/giant_health");
    private static final Identifier GIANT_REACH_MODIFIER = modifier("advanced_world/giant_reach");
    private static final Identifier GIANT_DAMAGE_MODIFIER = modifier("advanced_world/giant_melee_damage");

    private static final Map<UUID, PhaseState> PHASE = new HashMap<>();
    private static final Map<UUID, GiantState> GIANT = new HashMap<>();
    private static final Map<UUID, FearState> FEARED = new HashMap<>();

    private AdvancedWorldAugmentService() {
    }

    public static void initialize() {
        for (String augmentId : new String[]{
                PHASE_SHIFT_ID, DOMINATION_ID, GIGANTIFICATION_ID, FORBIDDEN_MAGIC_ID,
                ABILITY_HASTE_ID, DOUBLE_CHARGE_ID, COMBAT_CYCLE_ID, OVERTIME_ID
        }) {
            AugmentCatalog.require(augmentId);
        }
        ServerPlayerEvents.JOIN.register(player -> {
            clearPlayerRuntime(player);
            PhaseStateNetworking.send(player, false);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            clearPlayerRuntime(newPlayer);
            PhaseStateNetworking.send(newPlayer, false);
        });
        ServerPlayerEvents.LEAVE.register(AdvancedWorldAugmentService::clearPlayerRuntime);
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(AdvancedWorldAugmentService::afterKill);
        ServerTickEvents.END_SERVER_TICK.register(AdvancedWorldAugmentService::tickRuntime);
    }

    public static boolean tryUse(ServerPlayer player, String abilityId) {
        if (PHASE_SHIFT_ID.equals(abilityId)) {
            return tryPhaseShift(player);
        }
        if (DOMINATION_ID.equals(abilityId)) {
            return CompanionAugmentService.tryDominate(player);
        }
        if (GIGANTIFICATION_ID.equals(abilityId)) {
            return tryGigantification(player);
        }
        return false;
    }

    public static boolean isPhaseShiftActive(ServerPlayer player) {
        PhaseState state = PHASE.get(player.getUUID());
        return state != null && state.endTick > player.level().getGameTime();
    }

    public static double gigantificationKnockbackMultiplier(ServerPlayer player) {
        GiantState state = GIANT.get(player.getUUID());
        return state == null || state.endTick <= player.level().getGameTime()
                ? 1.0D
                : AdvancedWorldTuning.current().gigantificationKnockbackMultiplier();
    }

    public static boolean hasForbiddenMagic(Player player) {
        return player instanceof ServerPlayer serverPlayer
                && boundedLevel(AugmentationPlayerStateService.state(serverPlayer), FORBIDDEN_MAGIC_ID) > 0;
    }

    public static boolean isActionLockedByPhase(ServerPlayer player) {
        return isPhaseShiftActive(player);
    }

    public static void correctPhaseExit(ServerPlayer player) {
        if (player.level().noCollision(player)) {
            return;
        }
        PhaseState state = PHASE.get(player.getUUID());
        Vec3 fallback = state == null ? player.position() : state.startPosition;
        for (int dy = 0; dy <= 4; dy++) {
            for (int radius = 0; radius <= 4; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                            continue;
                        }
                        AABB moved = player.getBoundingBox().move(dx, dy, dz);
                        if (player.level().noCollision(player, moved)) {
                            player.teleportTo(player.getX() + dx, player.getY() + dy, player.getZ() + dz);
                            return;
                        }
                    }
                }
            }
        }
        player.teleportTo(fallback.x, fallback.y, fallback.z);
    }

    private static boolean tryPhaseShift(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, PHASE_SHIFT_ID) <= 0 || isPhaseShiftActive(player)) {
            return false;
        }
        ChargeReservation reservation = calculateReservation(
                player, state, PHASE_SHIFT_ID, AdvancedWorldTuning.current().phaseShiftCooldownTicks()
        );
        if (reservation == null) {
            return false;
        }
        long now = player.level().getGameTime();
        int duration = AdvancedWorldTuning.current().phaseShiftDurationTicks();
        long baseEnd = now + duration;
        long maxEnd = baseEnd + Math.round(
                duration * AdvancedActiveTuning.current().overtimeMaximumExtensionFraction()
        );
        PHASE.put(player.getUUID(), new PhaseState(player.position(), baseEnd, maxEnd));
        PhaseStateNetworking.send(player, true);
        commitReservation(player, reservation);
        return true;
    }

    private static boolean tryGigantification(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, GIGANTIFICATION_ID);
        if (level <= 0 || GIANT.containsKey(player.getUUID())
                || state.cooldownReadyTick(GIGANTIFICATION_ID) > player.level().getGameTime()) {
            return false;
        }
        long now = player.level().getGameTime();
        AdvancedWorldTuning.Snapshot tuning = AdvancedWorldTuning.current();
        GIANT.put(player.getUUID(), new GiantState(level, now + tuning.gigantificationDurationTicks()));
        applyGigantification(player, level);
        AugmentationPlayerStateService.replace(
                player,
                state.withCooldownReadyTick(GIGANTIFICATION_ID, now + tuning.gigantificationCooldownTicks())
        );
        if (level >= 2) {
            applyFear(player);
        }
        return true;
    }

    private static void applyGigantification(ServerPlayer player, int level) {
        float oldMax = player.getMaxHealth();
        float ratio = oldMax > 0.0F ? Math.max(0.0F, Math.min(1.0F, player.getHealth() / oldMax)) : 1.0F;
        AdvancedWorldTuning.Snapshot tuning = AdvancedWorldTuning.current();
        applyMultiplier(player.getAttribute(Attributes.SCALE), GIANT_SCALE_MODIFIER,
                tuning.gigantificationSizeMultiplier(level));
        applyMultiplier(player.getAttribute(Attributes.MAX_HEALTH), GIANT_HEALTH_MODIFIER,
                tuning.gigantificationHealthMultiplier(level));
        applyAdditive(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), GIANT_REACH_MODIFIER,
                tuning.gigantificationReachBonus(level));
        applyAdditive(player.getAttribute(Attributes.ATTACK_DAMAGE), GIANT_DAMAGE_MODIFIER,
                tuning.gigantificationMeleeDamageBonus(level), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        player.setHealth(Math.max(1.0F, Math.min(player.getMaxHealth(), player.getMaxHealth() * ratio)));
    }

    private static void removeGigantification(ServerPlayer player) {
        float giantMax = player.getMaxHealth();
        float ratio = giantMax > 0.0F ? Math.max(0.0F, Math.min(1.0F, player.getHealth() / giantMax)) : 1.0F;
        remove(player.getAttribute(Attributes.SCALE), GIANT_SCALE_MODIFIER);
        remove(player.getAttribute(Attributes.MAX_HEALTH), GIANT_HEALTH_MODIFIER);
        remove(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), GIANT_REACH_MODIFIER);
        remove(player.getAttribute(Attributes.ATTACK_DAMAGE), GIANT_DAMAGE_MODIFIER);
        player.setHealth(Math.max(1.0F, Math.min(player.getMaxHealth(), player.getMaxHealth() * ratio)));
        if (!player.level().noCollision(player)) {
            correctGenericExit(player);
        }
    }

    private static void applyFear(ServerPlayer player) {
        AdvancedWorldTuning.Snapshot tuning = AdvancedWorldTuning.current();
        double radius = tuning.gigantificationFearRadiusLevel2();
        long until = player.level().getGameTime() + tuning.gigantificationFearTicksLevel2();
        for (Mob mob : player.level().getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(radius),
                mob -> mob instanceof Enemy
                        && !mob.isDeadOrDying()
                        && !CompanionAugmentService.isDominated(mob)
                        && !EntityCompatibilityProfileService.isBoss(mob)
                        && player.distanceToSqr(mob) <= radius * radius
        )) {
            FEARED.put(mob.getUUID(), new FearState(mob, player.getUUID(), until));
            mob.setTarget(null);
        }
    }

    private static void afterKill(ServerLevel level, Entity killer, net.minecraft.world.entity.LivingEntity killed,
                                  DamageSource source) {
        if (!(killer instanceof ServerPlayer player)
                || !CombatProvenanceContext.resolve(source).isRootDirect()) {
            return;
        }
        boolean boss = EntityCompatibilityProfileService.isBoss(killed);
        if (!(killed instanceof Enemy) && !boss) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int cycle = boundedLevel(state, COMBAT_CYCLE_ID);
        if (cycle > 0 && boundedLevel(state, PHASE_SHIFT_ID) > 0) {
            reducePhaseCooldown(player, 20 * cycle * (boss ? 5 : 1));
        }
        if (boundedLevel(state, OVERTIME_ID) > 0) {
            extendPhase(player, boss
                    ? AdvancedActiveTuning.current().overtimeBossExtensionTicks()
                    : AdvancedActiveTuning.current().overtimeNormalExtensionTicks());
        }
    }

    private static void reducePhaseCooldown(ServerPlayer player, int ticks) {
        if (ticks <= 0) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        AugmentationPlayerState next = state;
        long now = player.level().getGameTime();
        if (boundedLevel(state, DOUBLE_CHARGE_ID) <= 0) {
            long ready = state.cooldownReadyTick(PHASE_SHIFT_ID);
            if (ready > now) {
                next = state.withCooldownReadyTick(PHASE_SHIFT_ID, Math.max(now, ready - ticks));
            }
        } else {
            String first = chargeKey(PHASE_SHIFT_ID, 0);
            String second = chargeKey(PHASE_SHIFT_ID, 1);
            String earlier = state.cooldownReadyTick(first) <= state.cooldownReadyTick(second) ? first : second;
            String later = earlier.equals(first) ? second : first;
            int remaining = ticks;
            for (String key : new String[]{earlier, later}) {
                if (remaining <= 0) break;
                long ready = next.cooldownReadyTick(key);
                if (ready <= now) continue;
                long applied = Math.min(ready - now, remaining);
                next = next.withCooldownReadyTick(key, ready - applied);
                remaining -= (int) applied;
            }
        }
        if (!next.equals(state)) {
            AugmentationPlayerStateService.replace(player, next);
        }
    }

    private static void extendPhase(ServerPlayer player, int ticks) {
        if (ticks <= 0) return;
        PhaseState phase = PHASE.get(player.getUUID());
        if (phase == null || phase.endTick <= player.level().getGameTime()) return;
        phase.endTick = Math.min(phase.maxEndTick, phase.endTick + ticks);
    }

    private static ChargeReservation calculateReservation(ServerPlayer player, AugmentationPlayerState state,
                                                           String abilityId, int baseCooldownTicks) {
        long now = player.level().getGameTime();
        int cooldown = baseCooldownTicks;
        if (boundedLevel(state, ABILITY_HASTE_ID) > 0) {
            cooldown = Math.max(1, (int) Math.round(baseCooldownTicks
                    * (1.0D - ExpansionTuning.current().abilityHasteReduction())));
        }
        if (boundedLevel(state, DOUBLE_CHARGE_ID) <= 0) {
            return state.cooldownReadyTick(abilityId) > now
                    ? null : new ChargeReservation(abilityId, now + cooldown);
        }
        String first = chargeKey(abilityId, 0);
        String second = chargeKey(abilityId, 1);
        long firstReady = state.cooldownReadyTick(first);
        long secondReady = state.cooldownReadyTick(second);
        String selected;
        if (firstReady <= now) selected = first;
        else if (secondReady <= now) selected = second;
        else return null;
        long queueTail = Math.max(now, Math.max(firstReady, secondReady));
        return new ChargeReservation(selected, queueTail + cooldown);
    }

    private static void commitReservation(ServerPlayer player, ChargeReservation reservation) {
        AugmentationPlayerState latest = AugmentationPlayerStateService.state(player);
        AugmentationPlayerStateService.replace(
                player,
                latest.withCooldownReadyTick(reservation.key, reservation.readyTick)
        );
    }

    private static void tickRuntime(MinecraftServer server) {
        tickPhase(server);
        tickGiants(server);
        tickFear(server);
    }

    private static void tickPhase(MinecraftServer server) {
        Iterator<Map.Entry<UUID, PhaseState>> iterator = PHASE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PhaseState> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (player.isDeadOrDying()) {
                PhaseStateNetworking.send(player, false);
                iterator.remove();
                continue;
            }
            if (player.level().getGameTime() >= entry.getValue().endTick) {
                correctPhaseExit(player);
                PhaseStateNetworking.send(player, false);
                iterator.remove();
            }
        }
    }

    private static void tickGiants(MinecraftServer server) {
        Iterator<Map.Entry<UUID, GiantState>> iterator = GIANT.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, GiantState> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (player.isDeadOrDying() || player.level().getGameTime() >= entry.getValue().endTick) {
                removeGigantification(player);
                iterator.remove();
            }
        }
    }

    private static void tickFear(MinecraftServer server) {
        Iterator<Map.Entry<UUID, FearState>> iterator = FEARED.entrySet().iterator();
        while (iterator.hasNext()) {
            FearState state = iterator.next().getValue();
            Mob mob = state.mob;
            if (mob.isRemoved() || mob.isDeadOrDying() || mob.level().getGameTime() >= state.expiresAtTick) {
                iterator.remove();
                continue;
            }
            Entity source = mob.level() instanceof ServerLevel level
                    ? level.getServer().getPlayerList().getPlayer(state.sourcePlayer)
                    : null;
            if (!(source instanceof ServerPlayer player)) {
                iterator.remove();
                continue;
            }
            Vec3 away = mob.position().subtract(player.position());
            if (away.lengthSqr() < 1.0E-6D) away = new Vec3(1.0D, 0.0D, 0.0D);
            Vec3 destination = mob.position().add(away.normalize().scale(8.0D));
            mob.setTarget(null);
            mob.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.0D);
        }
    }

    private static void clearPlayerRuntime(ServerPlayer player) {
        PHASE.remove(player.getUUID());
        if (GIANT.remove(player.getUUID()) != null) {
            removeGigantification(player);
        } else {
            remove(player.getAttribute(Attributes.SCALE), GIANT_SCALE_MODIFIER);
            remove(player.getAttribute(Attributes.MAX_HEALTH), GIANT_HEALTH_MODIFIER);
            remove(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), GIANT_REACH_MODIFIER);
            remove(player.getAttribute(Attributes.ATTACK_DAMAGE), GIANT_DAMAGE_MODIFIER);
        }
    }

    private static void correctGenericExit(ServerPlayer player) {
        for (int dy = 0; dy <= 4; dy++) {
            AABB moved = player.getBoundingBox().move(0.0D, dy, 0.0D);
            if (player.level().noCollision(player, moved)) {
                player.teleportTo(player.getX(), player.getY() + dy, player.getZ());
                return;
            }
        }
    }

    private static void applyMultiplier(AttributeInstance instance, Identifier id, double multiplier) {
        if (instance != null) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(
                    id, multiplier - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void applyAdditive(AttributeInstance instance, Identifier id, double amount) {
        applyAdditive(instance, id, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void applyAdditive(AttributeInstance instance, Identifier id, double amount,
                                      AttributeModifier.Operation operation) {
        if (instance != null) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void remove(AttributeInstance instance, Identifier id) {
        if (instance != null) instance.removeModifier(id);
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String chargeKey(String abilityId, int index) {
        return abilityId + "#charge" + index;
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private static Identifier modifier(String path) {
        return Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, path);
    }

    private static final class PhaseState {
        final Vec3 startPosition;
        long endTick;
        final long maxEndTick;
        PhaseState(Vec3 startPosition, long endTick, long maxEndTick) {
            this.startPosition = startPosition;
            this.endTick = endTick;
            this.maxEndTick = maxEndTick;
        }
    }

    private record GiantState(int level, long endTick) {}
    private record FearState(Mob mob, UUID sourcePlayer, long expiresAtTick) {}
    private record ChargeReservation(String key, long readyTick) {}
}

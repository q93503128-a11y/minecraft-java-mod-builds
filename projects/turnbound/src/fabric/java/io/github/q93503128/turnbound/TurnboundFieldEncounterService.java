package io.github.q93503128.turnbound;

import io.github.q93503128.turnbound.combat.BattleSessionAuthority;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.FieldEncounterClaims;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Field representatives are opt-in and server-authored, never guessed from all
 * vanilla/Drehmal entities. No encounter is auto-installed in a fresh world:
 * authoring services register a real mob, parameters and a trusted party factory.
 *
 * No drops, dungeon progress, arbitrary spawn, story or vanilla world edits.
 */
public final class TurnboundFieldEncounterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TurnboundFieldEncounterService.class);
    private static final Map<MinecraftServer, Environment> LIVE =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final int MAX_ACTORS = 128;

    public record EncounterPlan(BattleState battle, Map<UUID, Set<String>> owners) {
        public EncounterPlan {
            if (battle == null || owners == null || owners.isEmpty())
                throw new IllegalArgumentException("Missing authored battle and party ownership");
            owners = Map.copyOf(owners);
        }
    }

    @FunctionalInterface
    public interface TrustedBattleFactory {
        EncounterPlan create(ServerPlayerEntity target);
    }

    private static final class Representative {
        final MobEntity entity;
        final FieldEncounterClaims.Tuning tuning;
        final double chaseSpeed;
        final TrustedBattleFactory battleFactory;
        long sessionId;
        Representative(MobEntity entity, FieldEncounterClaims.Tuning tuning,
                       double chaseSpeed, TrustedBattleFactory battleFactory) {
            this.entity = entity;
            this.tuning = tuning;
            this.chaseSpeed = chaseSpeed;
            this.battleFactory = battleFactory;
        }
    }

    private static final class Environment {
        final FieldEncounterClaims claims = new FieldEncounterClaims();
        final Map<UUID, Representative> actors = new HashMap<>();
        final Map<Long, UUID> actorBySession = new HashMap<>();
        long tick;
    }

    private TurnboundFieldEncounterService() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (TurnboundWorldGate.active(server)) LIVE.put(server, new Environment());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            Environment env = LIVE.remove(server);
            if (env != null) {
                env.actors.clear();
                env.actorBySession.clear();
                env.claims.clear();
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(TurnboundFieldEncounterService::tick);
    }

    /** Called from trusted server-side authoring only, never from client networking. */
    public static boolean authorize(MobEntity actor, FieldEncounterClaims.Tuning tuning,
                                    double chaseSpeed, TrustedBattleFactory factory) {
        if (actor == null || !actor.isAlive() || !(actor.getWorld() instanceof ServerWorld world)
                || factory == null || tuning == null || !Double.isFinite(chaseSpeed)
                || chaseSpeed <= 0 || chaseSpeed > 2) return false;
        MinecraftServer server = world.getServer();
        Environment env = LIVE.get(server);
        if (!TurnboundWorldGate.active(server) || env == null || !server.isOnThread()
                || env.actors.size() >= MAX_ACTORS
                || env.actors.containsKey(actor.getUuid())) return false;
        env.claims.register(actor.getUuid(), tuning);
        env.actors.put(actor.getUuid(), new Representative(actor, tuning, chaseSpeed, factory));
        return true;
    }

    public static boolean unregister(MobEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) return false;
        Environment env = LIVE.get(world.getServer());
        if (env == null || !world.getServer().isOnThread()) return false;
        Representative existing = env.actors.remove(actor.getUuid());
        if (existing == null) return false;
        env.claims.remove(actor.getUuid());
        if (existing.sessionId != 0L) env.actorBySession.remove(existing.sessionId);
        return true;
    }

    /** An explicit trusted battle close releases only the matching representative. */
    public static void onBattleClosed(MinecraftServer server, long sessionId) {
        Environment env = LIVE.get(server);
        if (env == null) return;
        UUID actorId = env.actorBySession.remove(sessionId);
        if (actorId == null) return;
        Representative actor = env.actors.get(actorId);
        if (actor == null || actor.sessionId != sessionId) return;
        actor.sessionId = 0L;
        actor.entity.getNavigation().stop();
        env.claims.release(actorId, env.tick);
    }

    private static void tick(MinecraftServer server) {
        Environment env = LIVE.get(server);
        if (env == null || !TurnboundWorldGate.active(server)) return;
        env.tick++;
        // Only *explicitly authorized* representatives; never scan all world entities.
        for (Map.Entry<UUID, Representative> row : new ArrayList<>(env.actors.entrySet())) {
            UUID id = row.getKey();
            Representative representative = row.getValue();
            MobEntity actor = representative.entity;
            if (!actor.isAlive() || actor.isRemoved()) {
                // Do not leave a dead actor or a stale reservation on the field.
                env.actors.remove(id);
                env.claims.remove(id);
                if (representative.sessionId != 0L) env.actorBySession.remove(representative.sessionId);
                continue;
            }
            FieldEncounterClaims.View previous = env.claims.view(id);
            if (previous == null || previous.phase() == FieldEncounterClaims.Phase.CLAIMED) continue;
            ServerPlayerEntity target = previous.target() == null
                    ? nearestAvailable(server, actor, representative.tuning.noticeRadius())
                    : server.getPlayerManager().getPlayer(previous.target());
            boolean eligible = canTarget(server, actor, target);
            double dist = eligible ? actor.squaredDistanceTo(target) : Double.MAX_VALUE;
            boolean visible = eligible && (previous.target() != null || lineOfSight(actor, target));
            FieldEncounterClaims.Step step = env.claims.observe(id,
                    target == null ? null : target.getUuid(), eligible, visible, dist, env.tick);
            if (step.warned()) {
                // Minimal readable cue; final authored entities need dedicated alert art/audio.
                ((ServerWorld) actor.getWorld()).spawnParticles(ParticleTypes.ANGRY_VILLAGER,
                        actor.getX(), actor.getEyeY() + 0.2, actor.getZ(), 3, 0.2, 0.15, 0.2, 0);
            }
            if (step.phase() == FieldEncounterClaims.Phase.PURSUIT && target != null) {
                actor.getNavigation().startMovingTo(target, representative.chaseSpeed);
            } else if (step.phase() == FieldEncounterClaims.Phase.IDLE
                    || step.phase() == FieldEncounterClaims.Phase.COOLDOWN) {
                actor.getNavigation().stop();
            }
            if (step.contactClaim() && target != null) {
                actor.getNavigation().stop();
                try {
                    EncounterPlan plan = representative.battleFactory.create(target);
                    if (plan == null || !plan.owners().containsKey(target.getUuid()))
                        throw new IllegalArgumentException("Encounter plan omits the contacted player");
                    String worldId = target.getServerWorld().getRegistryKey().getValue().toString();
                    BattleSessionAuthority session =
                            TurnboundBattleBridge.open(server, worldId, plan.battle(), plan.owners());
                    representative.sessionId = session.id();
                    env.actorBySession.put(session.id(), id);
                } catch (RuntimeException invalidPlan) {
                    LOGGER.error("TURNBOUND rejected authored field encounter for representative {}",
                            id, invalidPlan);
                    env.claims.release(id, env.tick);
                }
            }
        }
    }

    private static boolean canTarget(MinecraftServer server, MobEntity actor, ServerPlayerEntity target) {
        return target != null && target.isAlive() && !target.isSpectator()
                && !target.isCreative()
                && target.getServerWorld() == actor.getWorld()
                && !TurnboundBattleBridge.inBattle(server, target.getUuid());
    }

    private static ServerPlayerEntity nearestAvailable(MinecraftServer server, MobEntity actor,
                                                       double radius) {
        ServerPlayerEntity closest = null;
        double limit = radius * radius;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!canTarget(server, actor, player)) continue;
            double d = actor.squaredDistanceTo(player);
            if (d <= limit && (closest == null || d < limit)) {
                closest = player;
                limit = d;
            }
        }
        return closest;
    }

    private static boolean lineOfSight(MobEntity actor, PlayerEntity player) {
        return actor.getWorld().raycast(new RaycastContext(
                actor.getEyePos(), player.getEyePos(),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, actor)).getType() == HitResult.Type.MISS;
    }
}

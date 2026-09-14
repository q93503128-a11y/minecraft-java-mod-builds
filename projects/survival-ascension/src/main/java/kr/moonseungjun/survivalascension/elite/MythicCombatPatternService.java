package kr.moonseungjun.survivalascension.elite;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative field-combat patterns for Survival-owned Mythic III elites.
 *
 * The ordinary Mythic entity keeps its vanilla AI and trait reactions. This service only becomes active
 * after real damage has been exchanged, then adds bounded telegraphed positional attacks. Original
 * content-pack field bosses are intentionally excluded so their native animation, attacks and encounter
 * identity remain authoritative. It never scans the world for mobs, force-loads chunks, or keeps an
 * encounter alive after combat has gone quiet.
 */
public final class MythicCombatPatternService {
    private static final int TICK_INTERVAL = 5;
    private static final int TELEGRAPH_TICKS = 40;
    private static final int COMBAT_MEMORY_TICKS = 240;
    private static final int PHASE_TWO_COOLDOWN_TICKS = 120;
    private static final int PHASE_THREE_COOLDOWN_TICKS = 80;
    private static final double PATTERN_PLAYER_RADIUS = 64.0D;
    private static final double ATTACK_VERTICAL_TOLERANCE = 4.0D;

    private static final Map<UUID, Runtime> ACTIVE = new HashMap<>();
    private static int ticker;

    private MythicCombatPatternService() {}

    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getHealthDamage() <= 0.0F) return;

        if (event.getEntity() instanceof Mob defender
                && defender.level() instanceof ServerLevel level
                && EliteMobSystem.rankId(defender) == 3
                && !MythicFieldBossService.isExternalFieldBoss(defender)) {
            engage(level, defender);
        }

        if (event.getSource().getEntity() instanceof Mob attacker
                && attacker.level() instanceof ServerLevel level
                && EliteMobSystem.rankId(attacker) == 3
                && !MythicFieldBossService.isExternalFieldBoss(attacker)) {
            engage(level, attacker);
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Mob mob && EliteMobSystem.rankId(mob) == 3) {
            ACTIVE.remove(mob.getUUID());
        }
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        if (++ticker < TICK_INTERVAL) return;
        ticker = 0;
        if (ACTIVE.isEmpty()) return;

        MinecraftServer server = event.getServer();
        List<UUID> stale = new ArrayList<>();
        for (Map.Entry<UUID, Runtime> entry : new ArrayList<>(ACTIVE.entrySet())) {
            Runtime runtime = entry.getValue();
            if (runtime.level.getServer() != server) continue;
            long now = runtime.level.getGameTime();
            if (now - runtime.lastCombatTick > COMBAT_MEMORY_TICKS) {
                stale.add(entry.getKey());
                continue;
            }
            Entity entity = runtime.level.getEntity(entry.getKey());
            if (!(entity instanceof Mob mob) || !mob.isAlive() || EliteMobSystem.rankId(mob) != 3
                    || MythicFieldBossService.isExternalFieldBoss(mob)) {
                stale.add(entry.getKey());
                continue;
            }
            tickRuntime(runtime, mob, now);
        }
        for (UUID id : stale) ACTIVE.remove(id);
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        ACTIVE.entrySet().removeIf(entry -> entry.getValue().level.getServer() == event.getServer());
        ticker = 0;
    }

    private static void engage(ServerLevel level, Mob mob) {
        Runtime runtime = ACTIVE.computeIfAbsent(mob.getUUID(), ignored -> new Runtime(level, level.getGameTime()));
        if (runtime.level != level) {
            runtime = new Runtime(level, level.getGameTime());
            ACTIVE.put(mob.getUUID(), runtime);
        }
        runtime.lastCombatTick = level.getGameTime();
    }

    private static void tickRuntime(Runtime runtime, Mob mob, long now) {
        int phase = phase(mob);
        if (phase <= 0) {
            clearPending(runtime);
            runtime.nextPatternTick = Math.max(runtime.nextPatternTick, now + 40L);
            return;
        }

        if (runtime.pending != Pattern.NONE) {
            if (now % 10L < TICK_INTERVAL) renderTelegraph(runtime);
            if (now >= runtime.executeTick) {
                executePattern(runtime, mob, phase);
                clearPending(runtime);
                runtime.nextPatternTick = now + (phase >= 2 ? PHASE_THREE_COOLDOWN_TICKS : PHASE_TWO_COOLDOWN_TICKS);
            }
            return;
        }

        if (now < runtime.nextPatternTick) return;
        ServerPlayer target = nearestCombatPlayer(runtime.level, mob);
        if (target == null) {
            runtime.nextPatternTick = now + 40L;
            return;
        }

        Pattern pattern = phase >= 2 && runtime.patternSerial++ % 2 == 0 ? Pattern.RING : Pattern.MARKED;
        schedule(runtime, mob, target, pattern);
    }

    private static int phase(Mob mob) {
        float ratio = mob.getHealth() / Math.max(1.0F, mob.getMaxHealth());
        return ratio <= 0.33F ? 2 : ratio <= 0.66F ? 1 : 0;
    }

    private static ServerPlayer nearestCombatPlayer(ServerLevel level, Mob mob) {
        double radiusSqr = PATTERN_PLAYER_RADIUS * PATTERN_PLAYER_RADIUS;
        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level || !player.isAlive() || player.isSpectator()) continue;
            double distance = player.distanceToSqr(mob);
            if (distance > radiusSqr || distance >= nearestDistance) continue;
            nearest = player;
            nearestDistance = distance;
        }
        return nearest;
    }

    private static void schedule(Runtime runtime, Mob mob, ServerPlayer target, Pattern pattern) {
        runtime.pending = pattern;
        runtime.executeTick = runtime.level.getGameTime() + TELEGRAPH_TICKS;
        runtime.origin = mob.position();
        runtime.markedPoint = target.position();

        Component warning = Component.literal(pattern == Pattern.RING
                ? "§b§l[신화 전조] §r§f공명 원환 — 붙거나 멀어지세요."
                : "§6§l[신화 전조] §r§f지면 붕괴 — 지금 자리에서 벗어나세요.");
        for (ServerPlayer player : nearbyPlayers(runtime.level, mob, PATTERN_PLAYER_RADIUS)) {
            player.sendSystemMessage(warning, true);
        }
        renderTelegraph(runtime);
    }

    private static void renderTelegraph(Runtime runtime) {
        switch (runtime.pending) {
            case MARKED -> {
                for (int i = 0; i < 20; i++) {
                    double angle = Math.PI * 2.0D * i / 20.0D;
                    runtime.level.sendParticles(ParticleTypes.CRIT,
                            runtime.markedPoint.x + Math.cos(angle) * 3.5D,
                            runtime.markedPoint.y + 0.15D,
                            runtime.markedPoint.z + Math.sin(angle) * 3.5D,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
            case RING -> {
                for (int i = 0; i < 28; i++) {
                    double angle = Math.PI * 2.0D * i / 28.0D;
                    runtime.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            runtime.origin.x + Math.cos(angle) * 6.5D,
                            runtime.origin.y + 0.15D,
                            runtime.origin.z + Math.sin(angle) * 6.5D,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
            default -> { }
        }
    }

    private static void executePattern(Runtime runtime, Mob mob, int phase) {
        float damage = phase >= 2 ? 11.0F : 8.0F;
        Vec3 impactOrigin = runtime.pending == Pattern.MARKED ? runtime.markedPoint : runtime.origin;
        for (ServerPlayer player : runtime.level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != runtime.level || !player.isAlive() || player.isSpectator()) continue;
            boolean hit = switch (runtime.pending) {
                case MARKED -> withinVerticalBand(player.position(), runtime.markedPoint)
                        && horizontalDistance(player.position(), runtime.markedPoint) <= 3.75D;
                case RING -> {
                    double distance = horizontalDistance(player.position(), runtime.origin);
                    yield withinVerticalBand(player.position(), runtime.origin) && distance >= 4.25D && distance <= 8.75D;
                }
                default -> false;
            };
            if (!hit) continue;

            player.hurtServer(runtime.level, mob.damageSources().mobAttack(mob), damage);
            Vec3 away = player.position().subtract(impactOrigin).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() > 1.0E-5D) {
                away = away.normalize();
                player.setDeltaMovement(player.getDeltaMovement().add(away.x * 0.55D, 0.18D, away.z * 0.55D));
                player.hurtMarked = true;
            }
        }

        runtime.level.sendParticles(phase >= 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.CRIT,
                impactOrigin.x, impactOrigin.y + 0.35D, impactOrigin.z,
                phase >= 2 ? 36 : 24, 0.9D, 0.35D, 0.9D, 0.04D);
    }

    private static List<ServerPlayer> nearbyPlayers(ServerLevel level, Entity entity, double radius) {
        double radiusSqr = radius * radius;
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level && player.isAlive() && !player.isSpectator()
                    && player.distanceToSqr(entity) <= radiusSqr) {
                players.add(player);
            }
        }
        return players;
    }

    private static boolean withinVerticalBand(Vec3 point, Vec3 origin) {
        return Math.abs(point.y - origin.y) <= ATTACK_VERTICAL_TOLERANCE;
    }

    private static double horizontalDistance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void clearPending(Runtime runtime) {
        runtime.pending = Pattern.NONE;
        runtime.executeTick = 0L;
        runtime.origin = Vec3.ZERO;
        runtime.markedPoint = Vec3.ZERO;
    }

    private enum Pattern { NONE, MARKED, RING }

    private static final class Runtime {
        final ServerLevel level;
        long lastCombatTick;
        long nextPatternTick;
        long executeTick;
        int patternSerial;
        Pattern pending = Pattern.NONE;
        Vec3 origin = Vec3.ZERO;
        Vec3 markedPoint = Vec3.ZERO;

        Runtime(ServerLevel level, long now) {
            this.level = level;
            this.lastCombatTick = now;
            this.nextPatternTick = now + 60L;
        }
    }
}

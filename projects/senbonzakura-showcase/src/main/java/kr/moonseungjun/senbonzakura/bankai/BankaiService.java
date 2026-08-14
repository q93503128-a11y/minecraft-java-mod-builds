package kr.moonseungjun.senbonzakura.bankai;

import kr.moonseungjun.senbonzakura.network.BankaiNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BankaiService {
    public static final int DURATION_TICKS = 260;
    public static final int COOLDOWN_TICKS = 600;
    public static final double ATTACK_RADIUS = 32.0;
    public static final double SAFE_RADIUS = 2.45;

    private static final int BLADE_STORM_START_TICK = 108;
    private static final int BLADE_STORM_END_TICK = 248;
    private static final int HIT_SWEEP_HISTORY_TICKS = 4;
    private static final int MAX_CONTACTS_PER_TICK = 4;
    private static final double[] RENDER_SPEED_SCALES = {0.82, 1.00, 1.10, 1.30};
    private static final double[] RENDER_TUBE_RADII = {5.7, 6.2, 7.1, 8.2};

    private static final Map<UUID, ActiveBankai> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> READY_AT = new HashMap<>();

    private BankaiService() {}

    public static boolean activate(ServerPlayer player) {
        UUID id = player.getUUID();
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        if (ACTIVE.containsKey(id)) return false;

        long ready = READY_AT.getOrDefault(id, 0L);
        if (now < ready) {
            long seconds = Math.max(1L, (ready - now + 19L) / 20L);
            player.sendSystemMessage(Component.literal("§d[천본앵] §f재사용까지 §d" + seconds + "초"));
            return false;
        }

        Vec3 facing = horizontalLook(player);
        Vec3 origin = groundAnchor(level, player);
        ActiveBankai active = new ActiveBankai(id, origin, facing, now);
        ACTIVE.put(id, active);
        READY_AT.put(id, now + COOLDOWN_TICKS);

        BankaiNetwork.broadcastStart(level, id, origin, facing, DURATION_TICKS);
        level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                SoundSource.PLAYERS, 0.55F, 0.56F);
        return true;
    }

    public static void tick(ServerPlayer player) {
        ActiveBankai active = ACTIVE.get(player.getUUID());
        if (active == null) return;
        ServerLevel level = (ServerLevel) player.level();
        int age = (int) (level.getGameTime() - active.startedAt());
        if (age < 0 || age >= DURATION_TICKS) {
            clear(player, true);
            return;
        }

        if (age < 108) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(0.0, motion.y, 0.0);
        }

        if (age == 24) {
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 0.82F, 0.62F);
        }
        if (age == 72) {
            level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.PLAYERS, 0.72F, 0.42F);
        }
        if (age == 108) {
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 1.35F, 1.18F);
        }

        if (age >= BLADE_STORM_START_TICK && age <= BLADE_STORM_END_TICK) {
            bladeStormTick(level, player, active, age);
        }

        // These are audio accents only. Damage is continuous while the visible blade rivers pass.
        if (age == 150) {
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.35F, 0.74F);
        }
        if (age == 180) {
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.35F, 0.66F);
        }
        if (age == 212) {
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.55F, 0.58F);
        }
    }

    public static void clear(ServerPlayer player, boolean broadcast) {
        if (ACTIVE.remove(player.getUUID()) != null && broadcast) {
            BankaiNetwork.broadcastStop(player.getUUID());
        }
    }

    public static void clearAll() {
        ACTIVE.clear();
        READY_AT.clear();
    }

    /**
     * Dense multi-hit storm. The renderer uses four different speed layers for the near/break,
     * dense, mid and far blade masses, so the server samples all four rather than pretending the
     * entire visible storm follows the 1.0-speed center. Every tick a target can be contacted by
     * several independent layers; each contact is a deliberately tiny cut so the Bankai produces
     * a very high hit count instead of three oversized damage events.
     */
    private static void bladeStormTick(
            ServerLevel level,
            ServerPlayer player,
            ActiveBankai active,
            int age) {
        Vec3 origin = active.origin();
        AABB area = new AABB(origin, origin).inflate(ATTACK_RADIUS, 12.0, ATTACK_RADIUS);
        List<Mob> targets = level.getEntitiesOfClass(
                        Mob.class,
                        area,
                        mob -> validTarget(player, mob))
                .stream()
                .filter(mob -> horizontalDistanceSqr(origin, mob.position())
                        >= SAFE_RADIUS * SAFE_RADIUS)
                .sorted(Comparator.comparingDouble(
                        mob -> horizontalDistanceSqr(origin, mob.position())))
                .limit(96)
                .toList();

        boolean finale = age >= 202;
        float damagePerContact = finale ? 0.14F : 0.10F;
        for (Mob mob : targets) {
            Vec3 samplePoint = mob.position().add(0.0, mob.getBbHeight() * 0.5, 0.0);
            int contacts = bladeContactCount(active, age, samplePoint);
            if (contacts <= 0) continue;

            int pulses = Math.min(MAX_CONTACTS_PER_TICK, contacts);
            for (int i = 0; i < pulses && mob.isAlive(); i++) {
                // Senbonzakura is intentionally a rapid multi-hit source. Vanilla's generic hurt
                // invulnerability would collapse dozens of visible blade contacts into one hit.
                mob.invulnerableTime = 0;
                if (mob.hurtServer(
                        level,
                        level.damageSources().playerAttack(player),
                        damagePerContact)) {
                    mob.setTarget(player);
                }
            }
        }
    }

    private static int bladeContactCount(ActiveBankai active, int age, Vec3 point) {
        int contacts = 0;
        int previousAge = Math.max(BLADE_STORM_START_TICK, age - HIT_SWEEP_HISTORY_TICKS);
        double seconds = age / 20.0;
        double progress = age / (double) DURATION_TICKS;
        double previousSeconds = previousAge / 20.0;
        double previousProgress = previousAge / (double) DURATION_TICKS;

        for (int layer = 0; layer < RENDER_SPEED_SCALES.length; layer++) {
            double speedScale = RENDER_SPEED_SCALES[layer];
            double radius = RENDER_TUBE_RADII[layer];
            if (age >= 202) radius += 0.9;
            double radiusSqr = radius * radius;
            boolean layerContact = false;

            for (int cluster = 0; cluster < BankaiFlowMath.CLUSTER_COUNT; cluster++) {
                Vec3 previous = BankaiFlowMath.currentCenter(
                        active.origin(),
                        active.facing(),
                        cluster,
                        previousSeconds,
                        previousProgress,
                        speedScale);
                Vec3 current = BankaiFlowMath.currentCenter(
                        active.origin(),
                        active.facing(),
                        cluster,
                        seconds,
                        progress,
                        speedScale);

                double midpointY = (previous.y + current.y) * 0.5;
                double verticalAllowance = 7.0 + layer * 0.9;
                if (Math.abs(point.y - midpointY) > verticalAllowance) continue;
                if (horizontalPointSegmentDistanceSqr(point, previous, current) <= radiusSqr) {
                    layerContact = true;
                    break;
                }
            }

            if (layerContact) contacts++;
        }
        return contacts;
    }

    private static double horizontalPointSegmentDistanceSqr(Vec3 point, Vec3 a, Vec3 b) {
        double abx = b.x - a.x;
        double abz = b.z - a.z;
        double apx = point.x - a.x;
        double apz = point.z - a.z;
        double lengthSqr = abx * abx + abz * abz;
        if (lengthSqr < 1.0E-8) return apx * apx + apz * apz;

        double t = BankaiFlowMath.clamp(
                (apx * abx + apz * abz) / lengthSqr,
                0.0,
                1.0);
        double dx = point.x - (a.x + abx * t);
        double dz = point.z - (a.z + abz * t);
        return dx * dx + dz * dz;
    }

    private static boolean validTarget(ServerPlayer player, Mob mob) {
        if (!mob.isAlive() || mob.isRemoved()) return false;
        if (mob instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(player)) {
            return false;
        }
        return !player.isAlliedTo(mob);
    }

    private static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    private static Vec3 horizontalLook(ServerPlayer player) {
        return BankaiFlowMath.horizontal(player.getLookAngle());
    }

    private static Vec3 groundAnchor(ServerLevel level, ServerPlayer player) {
        BlockPos start = player.blockPosition();
        for (int down = 0; down <= 12; down++) {
            BlockPos floor = start.below(down);
            if (level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                return Vec3.atCenterOf(floor.above()).add(0.0, -0.5, 0.0);
            }
        }
        return player.position().add(0.0, 0.05, 0.0);
    }

    private record ActiveBankai(
            UUID caster,
            Vec3 origin,
            Vec3 facing,
            long startedAt) {}
}

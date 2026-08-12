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
    public static final double ATTACK_RADIUS = 14.5;
    public static final double SAFE_RADIUS = 2.45;

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
        ACTIVE.put(id, new ActiveBankai(id, origin, facing, now));
        READY_AT.put(id, now + COOLDOWN_TICKS);

        BankaiNetwork.broadcastStart(id, origin, facing, DURATION_TICKS);
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

        if (age == 150) {
            slashWave(level, player, 7.5F, 36);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.35F, 0.74F);
        }
        if (age == 180) {
            slashWave(level, player, 7.5F, 36);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.35F, 0.66F);
        }
        if (age == 212) {
            slashWave(level, player, 10.0F, 42);
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

    private static void slashWave(ServerLevel level, ServerPlayer player, float damage, int limit) {
        Vec3 center = player.position();
        AABB area = new AABB(center, center).inflate(ATTACK_RADIUS, 8.0, ATTACK_RADIUS);
        List<Mob> targets = level.getEntitiesOfClass(Mob.class, area, mob -> validTarget(player, mob)).stream()
                .filter(mob -> horizontalDistanceSqr(center, mob.position()) >= SAFE_RADIUS * SAFE_RADIUS)
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .limit(limit)
                .toList();
        for (Mob mob : targets) {
            boolean damaged = mob.hurtServer(level, level.damageSources().playerAttack(player), damage);
            if (damaged) mob.setTarget(player);
        }
    }

    private static boolean validTarget(ServerPlayer player, Mob mob) {
        if (!mob.isAlive() || mob.isRemoved()) return false;
        if (mob instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(player)) return false;
        return !player.isAlliedTo(mob);
    }

    private static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    private static Vec3 horizontalLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        return flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
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

    private record ActiveBankai(UUID caster, Vec3 origin, Vec3 facing, long startedAt) {}
}

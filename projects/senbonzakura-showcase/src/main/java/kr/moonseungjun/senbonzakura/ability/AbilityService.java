package kr.moonseungjun.senbonzakura.ability;

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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-side timing and hit logic for the standalone showcase abilities. */
public final class AbilityService {
    private static final Map<UUID, ActiveAbility> ACTIVE = new HashMap<>();
    private static final Map<UUID, EnumMap<ShowcaseAbility, Long>> READY_AT = new HashMap<>();

    private AbilityService() {}

    public static boolean activate(ServerPlayer player, ShowcaseAbility ability) {
        if (ability == null) return false;
        UUID id = player.getUUID();
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();

        if (ACTIVE.containsKey(id)) {
            player.sendSystemMessage(Component.literal("§7[Showcase] §f현재 기술 연출이 끝난 뒤 사용할 수 있습니다."));
            return false;
        }

        long ready = READY_AT.computeIfAbsent(id, ignored -> new EnumMap<>(ShowcaseAbility.class))
                .getOrDefault(ability, 0L);
        if (now < ready) {
            long seconds = Math.max(1L, (ready - now + 19L) / 20L);
            player.sendSystemMessage(Component.literal("§7[" + ability.displayName() + "] §f재사용까지 §b" + seconds + "초"));
            return false;
        }

        Vec3 facing = horizontalLook(player);
        Vec3 origin = groundAnchor(level, player);
        ACTIVE.put(id, new ActiveAbility(ability, origin, facing, now));
        READY_AT.get(id).put(ability, now + ability.cooldownTicks());
        BankaiNetwork.broadcastAbilityStart(level, id, ability, origin, facing);
        cue(level, player, 0.72F, 0.62F);
        return true;
    }

    public static void tick(ServerPlayer player) {
        ActiveAbility active = ACTIVE.get(player.getUUID());
        if (active == null) return;
        ServerLevel level = (ServerLevel) player.level();
        int age = (int) (level.getGameTime() - active.startedAt());
        if (age < 0 || age >= active.ability().durationTicks()) {
            clear(player, true);
            return;
        }

        switch (active.ability()) {
            case SKYFALL -> tickSkyfall(level, player, active, age);
            case WORLD_DIVIDE -> tickWorldDivide(level, player, active, age);
            case BLACK_SUN -> tickBlackSun(level, player, active, age);
            case SWORD_GRAVE -> tickSwordGrave(level, player, active, age);
            case GRAVITY_REVERSAL -> tickGravity(level, player, active, age);
            case LAST_SECOND -> tickLastSecond(level, player, active, age);
            case HEAVEN_JUDGMENT -> tickJudgment(level, player, active, age);
            case STELLAR_LANCE -> tickStellarLance(level, player, active, age);
        }
    }

    public static void clear(ServerPlayer player, boolean broadcast) {
        ActiveAbility removed = ACTIVE.remove(player.getUUID());
        if (removed != null && broadcast) {
            BankaiNetwork.broadcastAbilityStop(player.getUUID(), removed.ability());
        }
    }

    public static void clearAll() {
        ACTIVE.clear();
        READY_AT.clear();
    }

    private static void tickSkyfall(ServerLevel level, ServerPlayer player, ActiveAbility active, int age) {
        if (age == 34) resonance(level, player, 0.9F, 0.48F);
        if (age == 94) {
            Vec3 impact = active.origin().add(active.facing().scale(9.0));
            damageRadial(level, player, impact, 9.5, 7.0, 22.0F, 64, 1.4);
            cue(level, player, 1.7F, 0.45F);
        }
    }

    private static void tickWorldDivide(ServerLevel level, ServerPlayer player, ActiveAbility active, int age) {
        if (age == 25) resonance(level, player, 0.8F, 1.32F);
        if (age == 53) {
            damageCorridor(level, player, active.origin(), active.facing(), 1.0, 29.0, 3.3, 5.5, 24.0F, 48);
            cue(level, player, 1.35F, 0.92F);
        }
    }

    private static void tickBlackSun(ServerLevel level, ServerPlayer player, ActiveAbility active, int age) {
        Vec3 center = active.origin().add(active.facing().scale(10.0)).add(0.0, 5.5, 0.0);
        if (age >= 38 && age <= 108) {
            List<Mob> mobs = targets(level, player, center, 13.0, 10.0, 72);
            for (Mob mob : mobs) {
                Vec3 delta = center.subtract(mob.position());
                if (delta.lengthSqr() > 0.25) {
                    Vec3 pull = delta.normalize().scale(0.075 + Math.min(0.085, delta.length() * 0.003));
                    Vec3 motion = mob.getDeltaMovement().scale(0.72).add(pull);
                    mob.setDeltaMovement(motion);
                    mob.getNavigation().stop();
                }
            }
        }
        if (age == 112) {
            damageRadial(level, player, center, 11.0, 9.0, 23.0F, 72, 0.9);
            cue(level, player, 1.8F, 0.34F);
        }
    }

    private static void tickSwordGrave(ServerLevel level, ServerPlayer player, ActiveAbility active, int age) {
        if (age == 48) resonance(level, player, 1.0F, 0.74F);
        if (age == 103) {
            Vec3 center = active.origin().add(active.facing().scale(9.0));
            damageRadial(level, player, center, 14.5, 7.5, 19.0F, 80, 0.55);
            cue(level, player, 1.55F, 0.58F);
        }
    }

    private static void tickGravity(ServerLevel level, ServerPlayer player, ActiveAbility active, int age) {
        Vec3 center = active.origin().add(active.facing().scale(6.0));
        if (age >= 25 && age <= 72) {
            for (Mob mob : targets(level, player, center, 12.0, 8.0, 64)) {
                Vec3 motion = mob.getDeltaMovement();
                double up = 0.12 + Math.min(0.18, Math.max(0.0, (72 - age) * 0.0025));
                mob.setDeltaMovement(motion.x * 0.58, Math.max(motion.y, up), motion.z * 0.58);
                mob.getNavigation().stop();
            }
        }
        if (age == 76) {
            for (Mob mob : targets(level, player, center, 12.0, 12.0, 64)) {
                Vec3 motion = mob.getDeltaMovement();
                mob.setDeltaMovement(motion.x * 0.35, -1.45, motion.z * 0.35);
            }
            cue(level, player, 1.2F, 0.54F);
        }
        if (age == 88) damageRadial(level, player, center, 12.0, 5.5, 18.0F, 64, 0.25);
    }

    private static void tickLastSecond(ServerLevel level, ServerPlayer player, ActiveAbility active, int age) {
        Vec3 center = active.origin().add(active.facing().scale(5.0));
        if (age >= 24 && age <= 92) {
            for (Mob mob : targets(level, player, center, 12.0, 7.0, 64)) {
                mob.setDeltaMovement(Vec3.ZERO);
                mob.getNavigation().stop();
            }
        }
        if (age == 96) {
            damageRadial(level, player, center, 12.0, 7.0, 25.0F, 64, 0.35);
            cue(level, player, 1.45F, 1.05F);
        }
    }

    private static void tickJudgment(ServerLevel level, ServerPlayer player, ActiveAbility active, int age) {
        if (age == 36) resonance(level, player, 0.95F, 1.45F);
        if (age == 79) {
            Vec3 center = active.origin().add(active.facing().scale(8.0));
            damageRadial(level, player, center, 9.0, 16.0, 24.0F, 64, 1.1);
            cue(level, player, 1.9F, 0.76F);
        }
    }

    private static void tickStellarLance(ServerLevel level, ServerPlayer player, ActiveAbility active, int age) {
        if (age == 42) resonance(level, player, 1.1F, 1.18F);
        if (age == 66) {
            damageCorridor(level, player, active.origin(), active.facing(), 0.0, 34.0, 2.7, 4.5, 22.0F, 64);
            for (int i = 0; i < 4; i++) {
                Vec3 center = active.origin().add(active.facing().scale(10.0 + i * 6.0));
                damageRadial(level, player, center, 3.6, 4.0, 7.0F, 24, 0.45);
            }
            cue(level, player, 1.65F, 1.28F);
        }
    }

    private static List<Mob> targets(ServerLevel level, ServerPlayer player, Vec3 center,
                                     double horizontalRadius, double verticalRadius, int limit) {
        AABB area = new AABB(center, center).inflate(horizontalRadius, verticalRadius, horizontalRadius);
        return level.getEntitiesOfClass(Mob.class, area, mob -> validTarget(player, mob)).stream()
                .sorted(Comparator.comparingDouble(mob -> mob.distanceToSqr(center)))
                .limit(limit)
                .toList();
    }

    private static void damageRadial(ServerLevel level, ServerPlayer player, Vec3 center,
                                     double horizontalRadius, double verticalRadius,
                                     float damage, int limit, double knockback) {
        for (Mob mob : targets(level, player, center, horizontalRadius, verticalRadius, limit)) {
            double dx = mob.getX() - center.x;
            double dz = mob.getZ() - center.z;
            if (dx * dx + dz * dz > horizontalRadius * horizontalRadius) continue;
            if (mob.hurtServer(level, level.damageSources().playerAttack(player), damage) && knockback > 0.0) {
                Vec3 away = new Vec3(dx, 0.15, dz);
                if (away.lengthSqr() > 1.0E-5) {
                    Vec3 impulse = away.normalize().scale(knockback);
                    mob.setDeltaMovement(mob.getDeltaMovement().add(impulse.x, Math.max(0.12, impulse.y), impulse.z));
                }
                mob.setTarget(player);
            }
        }
    }

    private static void damageCorridor(ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 forward,
                                       double minForward, double maxForward, double halfWidth, double vertical,
                                       float damage, int limit) {
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 center = origin.add(forward.scale((minForward + maxForward) * 0.5));
        AABB area = new AABB(center, center).inflate(maxForward * 0.6 + halfWidth, vertical, maxForward * 0.6 + halfWidth);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, area, mob -> validTarget(player, mob)).stream()
                .sorted(Comparator.comparingDouble(player::distanceToSqr)).limit(limit).toList();
        for (Mob mob : mobs) {
            Vec3 d = mob.position().subtract(origin);
            double f = d.dot(forward);
            double s = Math.abs(d.dot(right));
            if (f < minForward || f > maxForward || s > halfWidth || Math.abs(d.y) > vertical) continue;
            if (mob.hurtServer(level, level.damageSources().playerAttack(player), damage)) mob.setTarget(player);
        }
    }

    private static boolean validTarget(ServerPlayer player, Mob mob) {
        if (!mob.isAlive() || mob.isRemoved()) return false;
        if (mob instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(player)) return false;
        return !player.isAlliedTo(mob);
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

    private static void cue(ServerLevel level, ServerPlayer player, float volume, float pitch) {
        level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                SoundSource.PLAYERS, volume, pitch);
    }

    private static void resonance(ServerLevel level, ServerPlayer player, float volume, float pitch) {
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, volume, pitch);
    }

    private record ActiveAbility(ShowcaseAbility ability, Vec3 origin, Vec3 facing, long startedAt) {}
}

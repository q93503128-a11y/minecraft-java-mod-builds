
package kr.moonseungjun.arcanecircle.magic;

import kr.moonseungjun.arcanecircle.world.ArcaneEconomyService;
import kr.moonseungjun.arcanecircle.world.MagicWorldService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class HighCircleSpellEffects {
    private HighCircleSpellEffects() {}

    public static boolean execute(ServerPlayer player, String id, double range, double power) {
        return switch (id) {
            case "disintegrate" -> beam(player, range, power, ParticleTypes.ELECTRIC_SPARK, true);
            case "globe_of_invulnerability" -> selfWard(player, power, 520, 4);
            case "mass_suggestion" -> massControl(player, range, 420, 4, 0.30);
            case "move_earth" -> quake(player, range * 0.55, power, ParticleTypes.DUST_PLUME, false);
            case "sunbeam" -> beam(player, range, power, ParticleTypes.END_ROD, false);
            case "true_seeing" -> trueSeeing(player, range);
            case "freezing_sphere" -> blast(player, range, power, 7.0, ParticleTypes.SNOWFLAKE, 0, true);
            case "eyebite" -> curseTarget(player, range, power, 360, true);
            case "flesh_to_stone" -> petrify(player, range, power, 420);
            case "circle_of_death" -> blast(player, range, power, 10.0, ParticleTypes.SOUL, 0, false);

            case "delayed_blast_fireball" -> blast(player, range, power, 10.0, ParticleTypes.FLAME, 280, false);
            case "etherealness" -> ethereal(player, power);
            case "finger_of_death" -> deathRay(player, range, power);
            case "fire_storm" -> multiBlast(player, range, power, ParticleTypes.FLAME, 6, 5.0);
            case "forcecage" -> forceCage(player, range, power);
            case "plane_shift" -> ExpandedSpellEffects.safeTeleport(player, Math.max(range, 36.0), power, 4);
            case "prismatic_spray" -> prismaticSpray(player, range, power);
            case "reverse_gravity" -> reverseGravity(player, range, power);
            case "simulacrum" -> simulacrum(player, power);
            case "teleport" -> ExpandedSpellEffects.safeTeleport(player, range, power, 4);

            case "antimagic_field" -> antimagic(player, range);
            case "clone" -> cloneWard(player, power);
            case "control_weather" -> weatherStorm(player, range, power);
            case "demiplane" -> ExpandedSpellEffects.safeTeleport(player, Math.max(range, 48.0), power, 5);
            case "dominate_monster" -> massControl(player, range, 700, 6, 0.12);
            case "earthquake" -> quake(player, range * 0.65, power, ParticleTypes.CAMPFIRE_COSY_SMOKE, true);
            case "feeblemind" -> curseTarget(player, range, power, 700, false);
            case "incendiary_cloud" -> sustainedField(player, range, power, ParticleTypes.FLAME, true);
            case "maze" -> maze(player, range, power);
            case "sunburst" -> blast(player, range, power, 14.0, ParticleTypes.END_ROD, 160, false);

            case "meteor_swarm" -> meteorSwarm(player, range, power);
            case "power_word_kill" -> powerWordKill(player, range, power);
            case "prismatic_wall" -> prismaticWall(player, range, power);
            case "shapechange" -> shapechange(player, power);
            case "time_stop" -> timeStop(player, range);
            case "true_polymorph" -> truePolymorph(player, range, power);
            case "weird" -> weird(player, range, power);
            case "wish" -> wish(player);
            case "gate" -> ExpandedSpellEffects.safeTeleport(player, Math.max(range, 64.0), power, 6);
            case "foresight" -> foresight(player, power);
            default -> ExpandedSpellEffects.execute(player, id, range, power);
        };
    }

    private static ServerLevel level(ServerPlayer player) { return (ServerLevel) player.level(); }

    private static List<Mob> enemies(ServerPlayer player, Vec3 center, double radius) {
        return level(player).getEntitiesOfClass(Mob.class, new AABB(center, center).inflate(radius, radius * 0.65, radius),
                mob -> mob.isAlive() && !player.isAlliedTo(mob)
                        && (!(mob instanceof TamableAnimal tame) || !tame.isTame() || !tame.isOwnedBy(player)));
    }

    private static Optional<Mob> target(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.5);
        return player.level().getEntitiesOfClass(Mob.class, box, mob -> mob.isAlive()
                        && !player.isAlliedTo(mob)
                        && (!(mob instanceof TamableAnimal tame) || !tame.isTame() || !tame.isOwnedBy(player))).stream()
                .filter(mob -> {
                    Vec3 to = mob.getEyePosition().subtract(eye);
                    double projection = to.dot(look);
                    return projection >= 0 && projection <= range
                            && to.subtract(look.scale(projection)).length() <= Math.max(1.4, mob.getBbWidth() + 0.9);
                }).min(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)));
    }

    private static Vec3 aim(ServerPlayer player, double range) {
        return target(player, range).<Vec3>map(Mob::position)
                .orElse(player.getEyePosition().add(player.getLookAngle().normalize().scale(range)));
    }

    private static boolean beam(ServerPlayer player, double range, double power, ParticleOptions particle, boolean lethal) {
        ServerLevel level = level(player);
        Vec3 start = player.getEyePosition().add(player.getLookAngle().normalize().scale(1.6));
        Vec3 end = aim(player, range);
        line(level, start, end, particle, Math.max(48, (int) range * 3));
        List<Mob> hits = lineTargets(player, start, end, 1.6);
        for (Mob mob : hits) {
            ArcaneDamage.hurt(level, player, mob, (float) (power * (lethal ? 1.25 : 1.0)));
            if (!lethal) mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 180, 1));
        }
        burst(level, end, particle, 80, 1.0);
        return true;
    }

    private static boolean selfWard(ServerPlayer player, double power, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration,
                amplifier + Math.max(0, (int) (power / 40.0))));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, Math.min(4, amplifier - 1)));
        ring(level(player), player.position().add(0, 1.0, 0), 4.0 + power / 80.0, ParticleTypes.END_ROD, 96);
        return true;
    }

    private static boolean massControl(ServerPlayer player, double range, int duration, int amplifier, double damageScale) {
        Vec3 center = aim(player, range);
        List<Mob> mobs = enemies(player, center, Math.max(7.0, range * 0.26));
        for (Mob mob : mobs) {
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, amplifier));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, amplifier));
            if (damageScale > 0) mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) (range * damageScale));
        }
        ring(level(player), center.add(0, 0.2, 0), Math.max(7.0, range * 0.26), ParticleTypes.WITCH, 120);
        return true;
    }

    private static boolean quake(ServerPlayer player, double radius, double power, ParticleOptions particle, boolean huge) {
        ServerLevel level = level(player);
        Vec3 center = aim(player, Math.max(8.0, radius * 1.4));
        double r = Math.max(8.0, radius);
        for (Mob mob : enemies(player, center, r)) {
            ArcaneDamage.hurt(level, player, mob, (float) power);
            Vec3 away = mob.position().subtract(center).normalize();
            mob.push(away.x * (huge ? 2.2 : 1.4), huge ? 1.8 : 1.1, away.z * (huge ? 2.2 : 1.4));
        }
        for (double rr = 2; rr <= r; rr += 2.0) ring(level, center.add(0, 0.15, 0), rr, particle, 48);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.55F);
        return true;
    }

    private static boolean trueSeeing(ServerPlayer player, double range) {
        int duration = 1200;
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0));
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0));
        for (Mob mob : enemies(player, player.position(), Math.max(20.0, range)))
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0));
        return true;
    }

    private static boolean blast(ServerPlayer player, double range, double power, double radius,
                                 ParticleOptions particle, int fireTicks, boolean freeze) {
        ServerLevel level = level(player);
        Vec3 center = aim(player, range);
        double scaled = radius * Math.max(1.0, Math.sqrt(range / 25.0));
        for (Mob mob : enemies(player, center, scaled)) {
            ArcaneDamage.hurt(level, player, mob, (float) power);
            if (fireTicks > 0) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), fireTicks));
            if (freeze) mob.setTicksFrozen(mob.getTicksRequiredToFreeze() + 500);
        }
        for (double rr = 1.5; rr <= scaled; rr += 1.5) ring(level, center.add(0, 0.2, 0), rr, particle, 52);
        burst(level, center.add(0, 1, 0), particle, 160, scaled * 0.35);
        return true;
    }

    private static boolean curseTarget(ServerPlayer player, double range, double power, int duration, boolean fear) {
        Optional<Mob> target = target(player, range);
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) power);
        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 5));
        mob.addEffect(new MobEffectInstance(fear ? MobEffects.SLOWNESS : MobEffects.BLINDNESS, duration, 5));
        burst(level(player), mob.getEyePosition(), ParticleTypes.WITCH, 90, 0.8);
        return true;
    }

    private static boolean petrify(ServerPlayer player, double range, double power, int duration) {
        Optional<Mob> target = target(player, range);
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) power);
        mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 255));
        mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 4));
        burst(level(player), mob.position().add(0, 1, 0), ParticleTypes.ASH, 120, 0.7);
        return true;
    }

    private static boolean ethereal(ServerPlayer player, double power) {
        int duration = 360 + (int) power;
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 4));
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0));
        return true;
    }

    private static boolean deathRay(ServerPlayer player, double range, double power) {
        Optional<Mob> target = target(player, range);
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        line(level(player), player.getEyePosition(), mob.getEyePosition(), ParticleTypes.SOUL_FIRE_FLAME, 90);
        mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) (power * 1.45));
        mob.addEffect(new MobEffectInstance(MobEffects.WITHER, 300, 4));
        return true;
    }

    private static boolean multiBlast(ServerPlayer player, double range, double power,
                                      ParticleOptions particle, int count, double radius) {
        Vec3 center = aim(player, range);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count;
            Vec3 at = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            blastAt(player, at, power, radius * 0.75, particle, particle == ParticleTypes.FLAME);
        }
        return true;
    }

    private static boolean forceCage(ServerPlayer player, double range, double power) {
        Optional<Mob> target = target(player, range);
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 700, 255));
        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 700, 6));
        for (int y = 0; y <= 4; y++) ring(level(player), mob.position().add(0, y * 0.65, 0),
                1.5 + power / 150.0, ParticleTypes.END_ROD, 40);
        return true;
    }

    private static boolean prismaticSpray(ServerPlayer player, double range, double power) {
        ServerLevel level = level(player);
        Vec3 start = player.getEyePosition().add(player.getLookAngle().normalize().scale(1.6));
        List<ParticleOptions> colors = List.of(ParticleTypes.FLAME, ParticleTypes.SNOWFLAKE,
                ParticleTypes.ELECTRIC_SPARK, ParticleTypes.WITCH, ParticleTypes.END_ROD);
        for (int i = -3; i <= 3; i++) {
            Vec3 direction = player.getLookAngle().add(i * 0.08, (i % 2) * 0.04, -i * 0.06).normalize();
            Vec3 end = start.add(direction.scale(range));
            line(level, start, end, colors.get(Math.floorMod(i, colors.size())), 70);
        }
        for (Mob mob : enemies(player, start.add(player.getLookAngle().scale(range * 0.55)), range * 0.45)) {
            ArcaneDamage.hurt(level, player, mob, (float) power);
            int roll = level.getRandom().nextInt(4);
            mob.addEffect(new MobEffectInstance(roll == 0 ? MobEffects.BLINDNESS
                    : roll == 1 ? MobEffects.WITHER : roll == 2 ? MobEffects.SLOWNESS : MobEffects.WEAKNESS, 260, 3));
        }
        return true;
    }

    private static boolean reverseGravity(ServerPlayer player, double range, double power) {
        Vec3 center = aim(player, range);
        double radius = Math.max(10.0, range * 0.32);
        for (Mob mob : enemies(player, center, radius)) {
            mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) (power * 0.45));
            mob.push(0, 3.2 + power / 80.0, 0);
            mob.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 160, 5));
        }
        ring(level(player), center.add(0, 0.2, 0), radius, ParticleTypes.REVERSE_PORTAL, 150);
        return true;
    }

    private static boolean simulacrum(ServerPlayer player, double power) {
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 8 + (int) (power / 40.0)));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 2));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 2));
        burst(level(player), player.position().add(0, 1, 0), ParticleTypes.SNOWFLAKE, 140, 1.2);
        return true;
    }

    private static boolean antimagic(ServerPlayer player, double range) {
        double radius = Math.max(9.0, range * 0.50);
        for (Mob mob : enemies(player, player.position(), radius)) mob.removeAllEffects();
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WEAKNESS);
        ring(level(player), player.position().add(0, 0.2, 0), radius, ParticleTypes.END_ROD, 170);
        return true;
    }

    private static boolean cloneWard(ServerPlayer player, double power) {
        player.setHealth(player.getMaxHealth());
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1800, 12 + (int) (power / 35.0)));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1800, 3));
        return true;
    }

    private static boolean weatherStorm(ServerPlayer player, double range, double power) {
        ServerLevel level = level(player);
        Vec3 center = player.position();
        double radius = Math.max(18.0, range * 0.55);
        for (Mob mob : enemies(player, center, radius)) {
            ArcaneDamage.hurt(level, player, mob, (float) (power * 0.70));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 360, 3));

        }
        for (double r = 4; r <= radius; r += 4) ring(level, center.add(0, 1, 0), r, ParticleTypes.CLOUD, 70);
        return true;
    }

    private static boolean sustainedField(ServerPlayer player, double range, double power,
                                          ParticleOptions particle, boolean fire) {
        Vec3 center = aim(player, range);
        double radius = Math.max(10.0, range * 0.30);
        for (Mob mob : enemies(player, center, radius)) {
            mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) power);
            if (fire) mob.setRemainingFireTicks(400);
        }
        burst(level(player), center.add(0, 2, 0), particle, 260, radius * 0.55);
        ring(level(player), center.add(0, 0.2, 0), radius, particle, 180);
        return true;
    }

    private static boolean maze(ServerPlayer player, double range, double power) {
        Optional<Mob> target = target(player, range);
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) (power * 0.35));
        mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 800, 5));
        mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 800, 8));
        mob.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 800, 2));
        burst(level(player), mob.position().add(0, 1, 0), ParticleTypes.PORTAL, 160, 1.4);
        return true;
    }

    private static boolean meteorSwarm(ServerPlayer player, double range, double power) {
        Vec3 center = aim(player, range);
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI * 2.0 * i / 4.0 + Math.PI / 4.0;
            Vec3 impact = center.add(Math.cos(angle) * 10.0, 0, Math.sin(angle) * 10.0);
            Vec3 sky = impact.add(0, 42, 0);
            line(level(player), sky, impact, ParticleTypes.FLAME, 100);
            blastAt(player, impact, power, 11.0, ParticleTypes.FLAME, true);
        }
        return true;
    }

    private static boolean powerWordKill(ServerPlayer player, double range, double power) {
        Optional<Mob> target = target(player, range);
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        double threshold = Math.max(60.0, power * 0.85);
        float damage = mob.getHealth() <= threshold ? mob.getHealth() + mob.getMaxHealth() + 10.0F : (float) (power * 0.65);
        mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), damage);
        burst(level(player), mob.getEyePosition(), ParticleTypes.SOUL, 200, 1.4);
        return true;
    }

    private static boolean prismaticWall(ServerPlayer player, double range, double power) {
        Vec3 center = aim(player, range);
        double half = Math.max(12.0, Math.min(36.0, range * 0.25));
        for (Mob mob : enemies(player, center, half + 3.0)) {
            mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) power);
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 500, 7));
        }
        return true;
    }

    private static boolean shapechange(ServerPlayer player, double power) {
        int duration = 1800;
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, 5));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 4));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 3));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, duration, 3));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 2));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 12));
        return true;
    }

    private static boolean timeStop(ServerPlayer player, double range) {
        double radius = Math.max(20.0, range * 0.55);
        for (Mob mob : enemies(player, player.position(), radius)) {
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 240, 255));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 240, 255));
        }
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 240, 5));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 240, 4));
        ring(level(player), player.position().add(0, 1, 0), radius, ParticleTypes.END_ROD, 240);
        return true;
    }

    private static boolean truePolymorph(ServerPlayer player, double range, double power) {
        Optional<Mob> target = target(player, range);
        if (target.isEmpty()) return shapechange(player, power);
        Mob mob = target.get();
        mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) power);
        mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 1200, 10));
        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 10));
        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 0));
        burst(level(player), mob.position().add(0, 1, 0), ParticleTypes.WITCH, 220, 1.5);
        return true;
    }

    private static boolean weird(ServerPlayer player, double range, double power) {
        Vec3 center = aim(player, range);
        double radius = Math.max(14.0, range * 0.35);
        for (Mob mob : enemies(player, center, radius)) {
            mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) power);
            mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 500, 4));
            mob.addEffect(new MobEffectInstance(MobEffects.WITHER, 500, 5));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 500, 5));
        }
        burst(level(player), center.add(0, 1, 0), ParticleTypes.WITCH, 300, radius * 0.45);
        return true;
    }

    private static boolean wish(ServerPlayer player) {
        player.setHealth(player.getMaxHealth());
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 5));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 12));
        long balance = ArcaneEconomyService.balance(player);
        player.sendSystemMessage(Component.literal("§6[소원] §f생명·마력·주문 회로가 복구되었습니다. §7아르카나 " + balance));
        burst(level(player), player.position().add(0, 1, 0), ParticleTypes.END_ROD, 320, 2.2);
        return true;
    }

    private static boolean foresight(ServerPlayer player, double power) {
        int duration = 2400;
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 4));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 3));
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, duration, 4));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 8));
        return true;
    }

    private static List<Mob> lineTargets(ServerPlayer player, Vec3 start, Vec3 end, double width) {
        Vec3 direction = end.subtract(start);
        double length = Math.max(0.001, direction.length());
        Vec3 unit = direction.scale(1.0 / length);
        AABB box = new AABB(start, end).inflate(width + 1.0);
        return player.level().getEntitiesOfClass(Mob.class, box, Mob::isAlive).stream().filter(mob -> {
            Vec3 to = mob.getEyePosition().subtract(start);
            double projection = to.dot(unit);
            return projection >= 0 && projection <= length
                    && to.subtract(unit.scale(projection)).length() <= width + mob.getBbWidth();
        }).toList();
    }

    private static void blastAt(ServerPlayer player, Vec3 center, double power, double radius,
                                ParticleOptions particle, boolean fire) {
        for (Mob mob : enemies(player, center, radius)) {
            mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) power);
            if (fire) mob.setRemainingFireTicks(400);
        }
        burst(level(player), center.add(0, 1, 0), particle, 180, radius * 0.4);
        ring(level(player), center.add(0, 0.2, 0), radius, particle, 120);
    }

    private static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions particle, int points) {
        // Client WorldMagicTracker owns spell rendering.
    }

    private static void line(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int points) {
        // Client WorldMagicTracker owns spell rendering.
    }

    private static void burst(ServerLevel level, Vec3 center, ParticleOptions particle, int count, double spread) {
        // Client WorldMagicTracker owns spell rendering.
    }
}

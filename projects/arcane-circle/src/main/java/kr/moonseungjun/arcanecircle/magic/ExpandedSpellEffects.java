package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Minecraft adaptations of established D&D SRD and Pathfinder spell archetypes. */
public final class ExpandedSpellEffects {
    private ExpandedSpellEffects() {}

    public static boolean execute(ServerPlayer player, String id, double range, double power) {
        return switch (id) {
            case "magic_missile" -> missile(player, range, power, ParticleTypes.ENCHANT, 0, 0);
            case "fire_bolt" -> missile(player, range, power, ParticleTypes.FLAME, 120, 0);
            case "ray_of_frost" -> missile(player, range, power, ParticleTypes.SNOWFLAKE, 0, 120);
            case "shield" -> ward(player, power, 170, 1);
            case "feather_fall" -> featherFall(player, 120);
            case "light" -> light(player);
            case "grease" -> hinderingField(player, range, SpellMetrics.effectRadius("grease", range, 1), 180, 4, ParticleTypes.WITCH);
            case "sleep" -> sleep(player, range);
            case "thunderwave" -> conePush(player, "thunderwave", range, power, ParticleTypes.CLOUD, false);
            case "mage_armor" -> armor(player, power, 720, 1);

            case "scorching_ray" -> multiRay(player, range, power, ParticleTypes.FLAME, true);
            case "misty_step" -> teleport(player, range, power, 0);
            case "web" -> web(player, range);
            case "mirror_image" -> mirrorImage(player, power);
            case "invisibility" -> invisibility(player, 420, false);
            case "gust_of_wind" -> linePush(player, range, power);
            case "hold_person" -> hold(player, range, 190, 6, power);
            case "shatter" -> areaDamage(player, aimGround(player, range), SpellMetrics.effectRadius("shatter", range, 2), power,
                    ParticleTypes.CRIT, false, false, false);
            case "blur" -> blur(player, power);
            case "levitate" -> levitate(player, power, 260);

            case "lightning_bolt" -> piercingLine(player, range, power, ParticleTypes.ELECTRIC_SPARK, true);
            case "fly" -> fly(player, power);
            case "haste" -> haste(player);
            case "dispel_magic" -> dispel(player, range, power);
            case "vampiric_touch" -> drain(player, range, power);
            case "slow" -> hinderingField(player, range, SpellMetrics.effectRadius("slow", range, 3), 260, 5, ParticleTypes.ENCHANT);
            case "protection_from_energy" -> energyProtection(player, power);
            case "sleet_storm" -> areaDamage(player, aimGround(player, range), SpellMetrics.effectRadius("sleet_storm", range, 3), power,
                    ParticleTypes.SNOWFLAKE, false, true, true);

            case "wall_of_fire" -> wall(player, "wall_of_fire", range, power, ParticleTypes.FLAME, true, false, false);
            case "ice_storm" -> storm(player, "ice_storm", range, power, ParticleTypes.SNOWFLAKE, true);
            case "greater_invisibility" -> invisibility(player, 780, true);
            case "resilient_sphere" -> sphere(player, power);
            case "dimension_door" -> teleport(player, range, power, 2);
            case "stoneskin" -> armor(player, power, 760, 3);
            case "confusion" -> confusion(player, range);
            case "blight" -> singleTarget(player, range, power * 1.35, ParticleTypes.WITCH, true);
            case "freedom_of_movement" -> freedom(player);
            case "phantasmal_killer" -> phantasmalKiller(player, range, power);

            case "cone_of_cold" -> coneCold(player, range, power);
            case "wall_of_force" -> wall(player, "wall_of_force", range, power, ParticleTypes.END_ROD, false, false, true);
            case "cloudkill" -> areaDamage(player, aimGround(player, range), SpellMetrics.effectRadius("cloudkill", range, 5), power,
                    ParticleTypes.WITCH, false, false, true);
            case "telekinesis" -> telekinesis(player, range, power);
            case "flame_strike" -> flameStrike(player, range, power);
            case "hold_monster" -> hold(player, range, 340, 8, power * 1.2);
            case "mass_cure_wounds" -> massHeal(player, range, power);
            case "passwall" -> teleport(player, range, power, 3);
            case "dominate_person" -> dominate(player, range, power);
            case "insect_plague" -> areaDamage(player, aimGround(player, range), SpellMetrics.effectRadius("insect_plague", range, 5), power,
                    ParticleTypes.CRIT, false, false, true);

            case "burning_hands" -> conePush(player, "burning_hands", range, power, ParticleTypes.FLAME, true);
            case "ice_knife" -> iceKnife(player, range, power);
            case "chromatic_orb" -> chromaticOrb(player, range, power);
            case "wind_wall" -> wall(player, "wind_wall", range, power, ParticleTypes.CLOUD, false, true, false);
            case "counterspell" -> dispel(player, range, power * 1.2);
            case "fire_shield" -> fireShield(player, power);
            case "wall_of_ice" -> wall(player, "wall_of_ice", range, power, ParticleTypes.SNOWFLAKE, false, false, false);
            case "chain_lightning" -> chainLightning(player, range, power);
            case "arcane_hand" -> telekinesis(player, range, power * 1.25);
            case "teleportation_circle" -> teleport(player, range, power, 4);
            default -> false;
        };
    }

    public static boolean safeTeleport(ServerPlayer player, double range, double power, int tier) {
        return teleport(player, Math.max(2.0, range), power, Math.max(0, tier));
    }

    private static boolean missile(ServerPlayer player, double range, double power, ParticleOptions particle,
                                   int fireTicks, int freezeTicks) {
        ServerLevel level = level(player);
        Optional<Mob> target = lookTarget(player, range);
        Vec3 start = front(player, 1.45);
        Vec3 end = target.map(Mob::getEyePosition).orElse(start.add(player.getLookAngle().normalize().scale(range)));
        beam(level, start, end, particle, Math.max(28, (int) Math.round(range * 2.5)));
        target.ifPresent(mob -> {
            ArcaneDamage.hurt(level, player, mob, (float) power);
            if (fireTicks > 0) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), fireTicks));
            if (freezeTicks > 0) {
                mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + freezeTicks));
                mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, freezeTicks, 2));
            }
            burst(level, mob.getEyePosition(), particle, 20, 0.4);
        });
        return true;
    }

    private static boolean multiRay(ServerPlayer player, double range, double power, ParticleOptions particle,
                                    boolean ignite) {
        ServerLevel level = level(player);
        Optional<Mob> target = lookTarget(player, range);
        Vec3 start = front(player, 1.45);
        Vec3 end = target.map(Mob::getEyePosition).orElse(start.add(player.getLookAngle().normalize().scale(range)));
        Vec3 right = right(player);
        for (int lane = -1; lane <= 1; lane++) beam(level, start.add(right.scale(lane * 0.17)), end,
                particle, Math.max(30, (int) Math.round(range * 2.5)));
        target.ifPresent(mob -> {
            ArcaneDamage.hurt(level, player, mob, (float) (power * 1.15));
            if (ignite) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 180));
        });
        return true;
    }

    private static boolean singleTarget(ServerPlayer player, double range, double power, ParticleOptions particle,
                                        boolean weakness) {
        Optional<Mob> target = lookTarget(player, range);
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        ServerLevel level = level(player);
        beam(level, front(player, 1.4), mob.getEyePosition(), particle, Math.max(30, (int) (range * 2.4)));
        ArcaneDamage.hurt(level, player, mob, (float) power);
        if (weakness) mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 220, 3));
        return true;
    }

    private static boolean areaDamage(ServerPlayer player, Vec3 center, double radius, double power,
                                      ParticleOptions particle, boolean fire, boolean freeze, boolean lingering) {
        ServerLevel level = level(player);
        for (Mob mob : nearby(player, center, radius, Math.max(4.0, radius * 0.7))) {
            ArcaneDamage.hurt(level, player, mob, (float) power);
            if (fire) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 260));
            if (freeze) {
                mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 300));
                mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 220, 4));
            }
            if (lingering) {
                mob.addEffect(new MobEffectInstance(MobEffects.POISON, 220, 2));
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 180, 2));
            }
        }
        int rings = Math.max(4, Math.min(9, (int) Math.ceil(radius)));
        for (int i = 1; i <= rings; i++) ring(level, center.add(0.0, 0.1 + i * 0.03, 0.0),
                radius * i / rings, particle, 24 + i * 5);
        burst(level, center.add(0.0, 0.8, 0.0), particle, Math.min(120, 28 + (int) (radius * 7)),
                Math.max(0.7, radius * 0.28));
        return true;
    }

    private static boolean ward(ServerPlayer player, double power, int duration, int baseAmplifier) {
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration,
                Math.max(baseAmplifier, (int) Math.floor(power / 9.0))));
        shell(player, 1.4 + Math.min(1.6, power * 0.06), ParticleTypes.END_ROD);
        return true;
    }

    private static boolean armor(ServerPlayer player, double power, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration,
                Math.max(amplifier, (int) Math.floor(power / 12.0))));
        shell(player, 1.2 + Math.min(1.5, power * 0.05), ParticleTypes.ENCHANT);
        return true;
    }

    private static boolean featherFall(ServerPlayer player, int duration) {
        MageGearService.grantStableDescent(player, duration);
        return true;
    }

    private static boolean light(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1800, 0));
        ArcaneLightService.illuminate(player,1800);
        shell(player, 1.0, ParticleTypes.END_ROD);
        return true;
    }

    private static boolean hinderingField(ServerPlayer player, double range, double radius, int duration,
                                          int amplifier, ParticleOptions particle) {
        Vec3 center = aimGround(player, range);
        for (Mob mob : nearby(player, center, radius, 3.0)) {
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, amplifier));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, Math.max(0, amplifier / 2)));
        }
        for (int i = 1; i <= 4; i++) ring(level(player), center.add(0.0, 0.09, 0.0),
                radius * i / 4.0, particle, 22 + i * 5);
        return true;
    }

    private static boolean sleep(ServerPlayer player, double range) {
        Vec3 center = aimGround(player, range);
        double radius = SpellMetrics.effectRadius("sleep", range, 1);
        for (Mob mob : nearby(player, center, radius, Math.max(3.0, radius * 0.65))) {
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 170, 8));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 170, 4));
            mob.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
        }
        burst(level(player), center.add(0.0, 1.0, 0.0), ParticleTypes.ENCHANT, 55, Math.max(2.2, radius * 0.48));
        return true;
    }

    private static boolean conePush(ServerPlayer player, String id, double range, double power, ParticleOptions particle,
                                    boolean fire) {
        ServerLevel level = level(player);
        Vec3 origin = player.position(); Vec3 look = horizontalLook(player);
        double length = SpellMetrics.waveLength(range); double endRadius = SpellMetrics.waveEndRadius(id, range, id.equals("burning_hands") ? 2 : 1);
        for (Mob mob : nearby(player, origin.add(look.scale(length * 0.5)), length * 0.55 + endRadius, Math.max(4.5, endRadius))) {
            if (!insideWave(origin, look, mob.position(), length, endRadius)) continue;
            Vec3 direction = horizontalDirection(origin, mob.position()); ArcaneDamage.hurt(level, player, mob, (float) power);
            mob.push(direction.x * 1.55, 0.34, direction.z * 1.55); if (fire) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 180));
        } return true;
    }

    private static boolean web(ServerPlayer player, double range) {
        Vec3 center = aimGround(player, range);
        double radius = SpellMetrics.effectRadius("web", range, 2);
        for (Mob mob : nearby(player, center, radius, Math.max(3.2, radius * 0.64))) {
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 300, 7));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 220, 2));
        }
        ServerLevel level = level(player);
        for (int spoke = 0; spoke < 12; spoke++) {
            double angle = Math.PI * 2.0 * spoke / 12.0;
            beam(level, center.add(0.0, 0.1, 0.0),
                    center.add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius), ParticleTypes.END_ROD, 18);
        }
        for (int i = 1; i <= 4; i++) ring(level, center.add(0.0, 0.1, 0.0), radius * i / 4.0, ParticleTypes.END_ROD, 30);
        return true;
    }

    private static boolean mirrorImage(ServerPlayer player, double power) {
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 260, Math.max(1, (int) power / 8)));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 160, 0));
        ServerLevel level = level(player);
        for (int copy = 0; copy < 6; copy++) {
            double angle = Math.PI * 2.0 * copy / 6.0;
            Vec3 base = player.position().add(Math.cos(angle) * 1.15, 0.1, Math.sin(angle) * 1.15);
            beam(level, base, base.add(0.0, 1.8, 0.0), ParticleTypes.ENCHANT, 13);
        }
        return true;
    }

    private static boolean invisibility(ServerPlayer player, int duration, boolean greater) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0));
        if (greater) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 1));
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration / 2, 0));
        }
        burst(level(player), player.position().add(0.0, 1.0, 0.0), ParticleTypes.WITCH,
                greater ? 60 : 32, 0.7);
        return true;
    }

    private static boolean linePush(ServerPlayer player, double range, double power) {
        ServerLevel level = level(player);
        Vec3 start = front(player, 1.2);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(range));
        for (int lane = -2; lane <= 2; lane++) beam(level, start.add(right(player).scale(lane * 0.27)),
                end.add(right(player).scale(lane * 0.27)), ParticleTypes.CLOUD, Math.max(28, (int) (range * 2.0)));
        for (Mob mob : lineTargets(player, range, 2.1)) {
            ArcaneDamage.hurt(level, player, mob, (float) (power * 0.55));
            mob.push(look.x * 2.0, 0.3, look.z * 2.0);
        }
        return true;
    }

    private static boolean hold(ServerPlayer player, double range, int duration, int amplifier, double power) {
        Optional<Mob> target = lookTarget(player, range);
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, amplifier));
        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, Math.max(2, amplifier / 2)));
        mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) (power * 0.35));
        for (double y = 0.2; y < mob.getBbHeight() + 0.3; y += 0.42) {
            ring(level(player), mob.position().add(0.0, y, 0.0), Math.max(0.7, mob.getBbWidth()),
                    ParticleTypes.END_ROD, 22);
        }
        return true;
    }

    private static boolean blur(ServerPlayer player, double power) {
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 360, Math.max(0, (int) power / 10)));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 360, 1));
        burst(level(player), player.position().add(0.0, 1.0, 0.0), ParticleTypes.ENCHANT, 44, 0.65);
        return true;
    }

    private static boolean levitate(ServerPlayer player, double power, int duration) {
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 42 + (int) Math.round(power * 2.0), 1));
        MageGearService.grantStableDescent(player, duration);
        return true;
    }

    private static boolean piercingLine(ServerPlayer player, double range, double power, ParticleOptions particle,
                                         boolean shock) {
        ServerLevel level = level(player);
        Vec3 start = front(player, 1.45);
        Vec3 end = start.add(player.getLookAngle().normalize().scale(range));
        for (int lane = -1; lane <= 1; lane++) beam(level, start.add(right(player).scale(lane * 0.11)),
                end.add(right(player).scale(lane * 0.11)), particle, Math.max(45, (int) (range * 3.0)));
        for (Mob mob : lineTargets(player, range, 1.55)) {
            ArcaneDamage.hurt(level, player, mob, (float) power);
            if (shock) mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70, 2));
        }
        return true;
    }

    private static boolean fly(ServerPlayer player, double power) {
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 55 + (int) Math.round(power * 2.0), 1));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 650, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 520, 1));
        burst(level(player), player.position().add(0.0, 0.3, 0.0), ParticleTypes.CLOUD, 52, 0.9);
        return true;
    }

    private static boolean haste(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 560, 2));
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, 560, 2));
        burst(level(player), player.position().add(0.0, 1.0, 0.0), ParticleTypes.ELECTRIC_SPARK, 48, 0.75);
        return true;
    }

    private static boolean dispel(ServerPlayer player, double range, double power) {
        Optional<Mob> target = lookTarget(player, range);
        if (target.isPresent()) {
            Mob mob = target.get();
            mob.removeEffect(MobEffects.SPEED);
            mob.removeEffect(MobEffects.RESISTANCE);
            mob.removeEffect(MobEffects.ABSORPTION);
            mob.removeEffect(MobEffects.REGENERATION);
            mob.removeEffect(MobEffects.INVISIBILITY);
            mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) Math.max(2.0, power * 0.35));
            burst(level(player), mob.getEyePosition(), ParticleTypes.END_ROD, 36, 0.6);
        } else {
            freedom(player);
        }
        return true;
    }

    private static boolean drain(ServerPlayer player, double range, double power) {
        Optional<Mob> target = lookTarget(player, Math.min(range, 9.0));
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        beam(level(player), mob.getEyePosition(), player.position().add(0.0, 1.0, 0.0), ParticleTypes.WITCH, 36);
        mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) power);
        player.heal((float) Math.max(2.0, power * 0.58));
        return true;
    }

    private static boolean energyProtection(ServerPlayer player, double power) {
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 620, Math.max(1, (int) power / 10)));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 620, 0));
        shell(player, 2.0, ParticleTypes.END_ROD);
        return true;
    }

    private static boolean storm(ServerPlayer player, String id, double range, double power, ParticleOptions particle,
                                 boolean freeze) {
        Vec3 center = aimGround(player, range);
        double radius = SpellMetrics.effectRadius(id, range, 4);
        areaDamage(player, center, radius, power, particle, false, freeze, false);
        ServerLevel level = level(player);
        for (int i = 0; i < 72; i++) {
            double angle = i * 2.399963229728653;
            double r = radius * Math.sqrt((i + 1) / 72.0);
            Vec3 top = center.add(Math.cos(angle) * r, 5.0 + i % 4, Math.sin(angle) * r);
            beam(level, top, top.add(0.0, -5.0, 0.0), particle, 9);
        }
        return true;
    }

    private static boolean sphere(ServerPlayer player, double power) {
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 560, Math.max(3, (int) power / 7)));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 500, 2));
        shell(player, 2.8, ParticleTypes.END_ROD);
        return true;
    }

    private static boolean confusion(ServerPlayer player, double range) {
        Vec3 center = aimGround(player, range);
        double radius = SpellMetrics.effectRadius("confusion", range, 4);
        for (Mob mob : nearby(player, center, radius, Math.max(4.0, radius * 0.7))) {
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 280, 3));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 280, 4));
            mob.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 180, 0));
        }
        burst(level(player), center.add(0.0, 1.2, 0.0), ParticleTypes.WITCH, 70, 3.0);
        return true;
    }

    private static boolean freedom(ServerPlayer player) {
        player.removeEffect(MobEffects.SLOWNESS);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.DARKNESS);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 520, 1));
        burst(level(player), player.position().add(0.0, 1.0, 0.0), ParticleTypes.END_ROD, 40, 0.65);
        return true;
    }

    private static boolean phantasmalKiller(ServerPlayer player, double range, double power) {
        Optional<Mob> target = lookTarget(player, range);
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) power);
        mob.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 220, 0));
        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 220, 5));
        burst(level(player), mob.getEyePosition(), ParticleTypes.WITCH, 65, 0.9);
        return true;
    }

    private static boolean coneCold(ServerPlayer player, double range, double power) {
        ServerLevel level = level(player); Vec3 origin = player.position(); Vec3 look = horizontalLook(player);
        double length = SpellMetrics.waveLength(range); double endRadius = SpellMetrics.waveEndRadius("cone_of_cold", range, 5);
        for (Mob mob : nearby(player, origin.add(look.scale(length * 0.5)), length * 0.55 + endRadius, Math.max(5.5, endRadius))) {
            if (!insideWave(origin, look, mob.position(), length, endRadius)) continue;
            ArcaneDamage.hurt(level, player, mob, (float) power); mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 560));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 320, 5));
        } return true;
    }

    private static boolean telekinesis(ServerPlayer player, double range, double power) {
        Optional<Mob> target = lookTarget(player, range);
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        Vec3 look = player.getLookAngle().normalize();
        mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) (power * 0.7));
        mob.push(look.x * 2.8, 1.15, look.z * 2.8);
        mob.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 55, 2));
        beam(level(player), front(player, 1.4), mob.getEyePosition(), ParticleTypes.ENCHANT, 44);
        return true;
    }

    private static boolean flameStrike(ServerPlayer player, double range, double power) {
        Vec3 center = aimGround(player, range);
        ServerLevel level = level(player);
        Vec3 top = center.add(0.0, 13.0, 0.0);
        for (int lane = 0; lane < 7; lane++) {
            double angle = Math.PI * 2.0 * lane / 7.0;
            Vec3 offset = new Vec3(Math.cos(angle) * 0.55, 0.0, Math.sin(angle) * 0.55);
            beam(level, top.add(offset), center.add(0.0, 0.3, 0.0).add(offset), ParticleTypes.FLAME, 58);
        }
        areaDamage(player, center, Math.max(5.0, range * 0.25), power, ParticleTypes.FLAME, true, false, false);
        return true;
    }

    private static boolean massHeal(ServerPlayer player, double range, double power) {
        ServerLevel level = level(player);
        AABB box = player.getBoundingBox().inflate(range, 5.0, range);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && (entity == player || isAlly(player, entity)))) {
            entity.heal((float) power);
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 180, 2));
            for (int layer = 0; layer < 3; layer++) ring(level, entity.position().add(0.0, 0.3 + layer * 0.5, 0.0),
                    0.65 + layer * 0.12, ParticleTypes.HAPPY_VILLAGER, 20);
        }
        return true;
    }

    private static boolean dominate(ServerPlayer player, double range, double power) {
        Optional<Mob> target = lookTarget(player, range);
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 520, 7));
        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 520, 6));
        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 520, 0));
        mob.hurtServer(level(player), level(player).damageSources().playerAttack(player), (float) (power * 0.4));
        shell(level(player), mob.position().add(0.0, mob.getBbHeight() * 0.5, 0.0),
                Math.max(1.0, mob.getBbWidth() * 1.3), ParticleTypes.ENCHANT);
        return true;
    }

    private static boolean iceKnife(ServerPlayer player, double range, double power) {
        Optional<Mob> target = lookTarget(player, range);
        Vec3 center = target.map(Mob::position).orElse(aimGround(player, range));
        missile(player, range, power * 0.65, ParticleTypes.SNOWFLAKE, 0, 160);
        return areaDamage(player, center, SpellMetrics.effectRadius("ice_knife", range, 2), power * 0.65, ParticleTypes.SNOWFLAKE, false, true, false);
    }

    private static boolean chromaticOrb(ServerPlayer player, double range, double power) {
        ServerLevel level = level(player);
        Optional<Mob> target = lookTarget(player, range);
        Vec3 start = front(player, 1.45);
        Vec3 end = target.map(Mob::getEyePosition).orElse(start.add(player.getLookAngle().normalize().scale(range)));
        beam(level, start, end, ParticleTypes.FLAME, 32);
        beam(level, start.add(0.0, 0.08, 0.0), end, ParticleTypes.SNOWFLAKE, 32);
        beam(level, start.add(0.0, -0.08, 0.0), end, ParticleTypes.ENCHANT, 32);
        target.ifPresent(mob -> {
            ArcaneDamage.hurt(level, player, mob, (float) (power * 1.2));
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 120));
            mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 120));
        });
        return true;
    }

    private static boolean fireShield(ServerPlayer player, double power) {
        ward(player, power, 620, 3);
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 620, 0));
        shell(player, 2.2, ParticleTypes.FLAME);
        return true;
    }

    private static boolean chainLightning(ServerPlayer player, double range, double power) {
        List<Mob> targets = chainedTargets(player, range, 7);
        if (targets.isEmpty()) return piercingLine(player, range, power * 0.6, ParticleTypes.ELECTRIC_SPARK, true);
        ServerLevel level = level(player);
        Vec3 from = front(player, 1.45);
        double scale = 1.0;
        for (Mob target : targets) {
            beam(level, from, target.getEyePosition(), ParticleTypes.ELECTRIC_SPARK, 32);
            ArcaneDamage.hurt(level, player, target, (float) (power * scale));
            from = target.getEyePosition();
            scale *= 0.84;
        }
        return true;
    }

    private static boolean wall(ServerPlayer player, String id, double range, double power, ParticleOptions particle,
                                boolean fire, boolean push, boolean force) {
        ServerLevel level = level(player);
        Vec3 center = aimGround(player, range);
        Vec3 right = right(player);
        int circle = id.equals("wall_of_force") ? 5 : id.equals("wind_wall") ? 3 : 4;
        double halfWidth = SpellMetrics.wallWidth(id, range, circle) * 0.5;
        int segments = Math.max(15, (int) Math.round(halfWidth * 3.0));
        for (int step = -segments; step <= segments; step++) {
            Vec3 base = center.add(right.scale(step * halfWidth / segments));
            beam(level, base, base.add(0.0, Math.max(3.5, halfWidth * 0.55), 0.0), particle, 20);
        }
        AABB box = new AABB(center.add(right.scale(-halfWidth)), center.add(right.scale(halfWidth)))
                .inflate(1.3, Math.max(4.0, halfWidth * 0.6), 1.3);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, candidate -> validTarget(player, candidate))) {
            ArcaneDamage.hurt(level, player, mob, (float) power);
            if (fire) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 320));
            if (particle == ParticleTypes.SNOWFLAKE) {
                mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 360));
                mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 260, 4));
            }
            if (push || force) {
                Vec3 away = horizontalDirection(center, mob.position());
                mob.push(away.x * (force ? 2.2 : 1.5), force ? 0.5 : 0.25, away.z * (force ? 2.2 : 1.5));
            }
        }
        return true;
    }

    private static boolean teleport(ServerPlayer player, double range, double power, int tier) {
        Optional<BlockPos> destination = findSafeDestination(player, range);
        if (destination.isEmpty()) return false;
        ServerLevel level = level(player);

        BlockPos pos = destination.get();
        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        if (tier >= 2) player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                80 + tier * 30 + (int) power * 4, Math.min(3, tier - 1)));
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 1.1F - tier * 0.08F);
        return true;
    }

    private static void shell(ServerPlayer player, double radius, ParticleOptions particle) {
        shell(level(player), player.position().add(0.0, 1.0, 0.0), radius, particle);
    }

    private static void shell(ServerLevel level, Vec3 center, double radius, ParticleOptions particle) {
        for (int layer = -3; layer <= 3; layer++) {
            double y = layer / 3.0;
            double ringRadius = Math.sqrt(Math.max(0.0, 1.0 - y * y)) * radius;
            ring(level, center.add(0.0, y * radius, 0.0), ringRadius, particle, 24);
        }
    }

    private static Optional<Mob> lookTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(range));
        AABB box = new AABB(start, end).inflate(2.4);
        return level(player).getEntitiesOfClass(Mob.class, box, mob -> validTarget(player, mob) && player.hasLineOfSight(mob))
                .stream().filter(mob -> projection(start, look, mob.getEyePosition()) >= 0.0)
                .filter(mob -> projection(start, look, mob.getEyePosition()) <= range + 1.0)
                .min(Comparator.<Mob>comparingDouble(mob -> rayDistanceSquared(start, look, mob.getEyePosition()))
                        .thenComparingDouble(player::distanceToSqr));
    }

    private static List<Mob> lineTargets(ServerPlayer player, double range, double width) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(range));
        AABB box = new AABB(start, end).inflate(width + 0.8);
        return level(player).getEntitiesOfClass(Mob.class, box, mob -> validTarget(player, mob) && player.hasLineOfSight(mob))
                .stream().filter(mob -> projection(start, look, mob.getEyePosition()) >= 0.0)
                .filter(mob -> projection(start, look, mob.getEyePosition()) <= range + 1.0)
                .filter(mob -> rayDistanceSquared(start, look, mob.getEyePosition()) <= width * width)
                .sorted(Comparator.comparingDouble(mob -> projection(start, look, mob.getEyePosition()))).toList();
    }

    private static List<Mob> nearby(ServerPlayer player, Vec3 center, double radius, double vertical) {
        return level(player).getEntitiesOfClass(Mob.class, new AABB(center, center).inflate(radius, vertical, radius),
                mob -> validTarget(player, mob));
    }

    private static List<Mob> chainedTargets(ServerPlayer player, double range, int limit) {
        List<Mob> result = new ArrayList<>();
        Optional<Mob> first = lookTarget(player, range);
        if (first.isEmpty()) return result;
        result.add(first.get());
        while (result.size() < limit) {
            Mob last = result.get(result.size() - 1);
            Optional<Mob> next = level(player).getEntitiesOfClass(Mob.class, last.getBoundingBox().inflate(6.0),
                            mob -> validTarget(player, mob) && !result.contains(mob) && last.hasLineOfSight(mob))
                    .stream().min(Comparator.comparingDouble(last::distanceToSqr));
            if (next.isEmpty()) break;
            result.add(next.get());
        }
        return result;
    }

    private static boolean validTarget(ServerPlayer player, Mob mob) {
        if (!mob.isAlive() || mob.isRemoved()) return false;
        if (mob instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(player)) return false;
        return player.getTeam() == null || mob.getTeam() == null || !player.isAlliedTo(mob);
    }

    private static boolean isAlly(ServerPlayer player, LivingEntity entity) {
        if (entity == player) return true;
        if (entity instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(player)) return true;
        if (entity instanceof ServerPlayer other) return player.isAlliedTo(other);
        return player.isAlliedTo(entity);
    }

    private static Optional<BlockPos> findSafeDestination(ServerPlayer player, double range) {
        ServerLevel level = level(player);
        Vec3 look = player.getLookAngle().normalize();
        for (int step = (int) Math.floor(range); step >= 2; step--) {
            BlockPos candidate = BlockPos.containing(player.position().add(look.scale(step)));
            for (int down = 0; down <= 7; down++) {
                BlockPos feet = candidate.below(down);
                if (safe(level, feet)) return Optional.of(feet);
            }
        }
        return Optional.empty();
    }

    private static boolean safe(ServerLevel level, BlockPos feet) {
        if (!level.getWorldBorder().isWithinBounds(feet)) return false;
        BlockState below = level.getBlockState(feet.below());
        if (!below.isFaceSturdy(level, feet.below(), Direction.UP)) return false;
        if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) return false;
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    private static Vec3 aimGround(ServerPlayer player, double range) {
        ServerLevel level = level(player);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.getEyePosition();
        for (int step = (int) Math.max(2, Math.floor(range)); step >= 2; step--) {
            BlockPos candidate = BlockPos.containing(origin.add(look.scale(step)));
            for (int down = 0; down <= 9; down++) {
                BlockPos floor = candidate.below(down);
                if (level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                    return Vec3.atCenterOf(floor.above()).add(0.0, -0.48, 0.0);
                }
            }
        }
        return player.position().add(horizontalLook(player).scale(Math.min(6.0, range))).add(0.0, 0.08, 0.0);
    }

    private static Vec3 front(ServerPlayer player, double distance) {
        return player.getEyePosition().add(player.getLookAngle().normalize().scale(distance));
    }

    private static Vec3 horizontalLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        return flat.lengthSqr() < 0.0001 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static Vec3 horizontalDirection(Vec3 from, Vec3 to) {
        Vec3 flat = new Vec3(to.x - from.x, 0.0, to.z - from.z);
        return flat.lengthSqr() < 0.0001 ? Vec3.ZERO : flat.normalize();
    }

    private static Vec3 right(ServerPlayer player) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 candidate = new Vec3(-look.z, 0.0, look.x);
        return candidate.lengthSqr() < 0.0001 ? new Vec3(1.0, 0.0, 0.0) : candidate.normalize();
    }

    private static boolean insideWave(Vec3 origin, Vec3 look, Vec3 point, double length, double endRadius) {
        Vec3 delta = point.subtract(origin); Vec3 flat = new Vec3(delta.x, 0.0, delta.z); double forward = flat.dot(look);
        if (forward < 0.0 || forward > length) return false; double lateralSq = Math.max(0.0, flat.lengthSqr() - forward * forward);
        double t = length <= 0.0001 ? 1.0 : forward / length; double allowed = endRadius * (0.16 + 0.84 * t) + 0.65;
        return lateralSq <= allowed * allowed;
    }

    private static double projection(Vec3 start, Vec3 direction, Vec3 point) {
        return point.subtract(start).dot(direction);
    }

    private static double rayDistanceSquared(Vec3 start, Vec3 direction, Vec3 point) {
        double projected = Math.max(0.0, point.subtract(start).dot(direction));
        return start.add(direction.scale(projected)).distanceToSqr(point);
    }

    private static void beam(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int points) {
        // Client WorldMagicTracker owns spell rendering.
    }

    private static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions particle, int points) {
        // Client WorldMagicTracker owns spell rendering.
    }

    private static void burst(ServerLevel level, Vec3 center, ParticleOptions particle, int count, double spread) {
        // Client WorldMagicTracker owns spell rendering.
    }

    private static ServerLevel level(ServerPlayer player) { return (ServerLevel) player.level(); }
}

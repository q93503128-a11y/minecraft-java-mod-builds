package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Functional effects for extended fusion formulae. Visual identity stays in the world renderer. */
public final class FusionSpellEffects {
    private static final Set<String> IDS = Set.of(
            "steam_burst", "frost_step", "thunder_cage", "solar_guard",
            "void_lance", "winter_domain", "astral_prison", "phoenix_requiem", "world_sunder");

    private FusionSpellEffects() {}

    public static boolean supports(String id) { return IDS.contains(id); }

    public static boolean execute(ServerPlayer player, String id, double range, double power) {
        return switch (id) {
            case "steam_burst" -> steamBurst(player, range, power);
            case "frost_step" -> frostStep(player, range, power);
            case "thunder_cage" -> thunderCage(player, range, power);
            case "solar_guard" -> solarGuard(player, range, power);
            case "void_lance" -> voidLance(player, range, power);
            case "winter_domain" -> winterDomain(player, range, power);
            case "astral_prison" -> astralPrison(player, range, power);
            case "phoenix_requiem" -> phoenixRequiem(player, range, power);
            case "world_sunder" -> worldSunder(player, range, power);
            default -> false;
        };
    }

    private static boolean steamBurst(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 origin = player.position();
        Vec3 look = horizontal(CastTargetSnapshot.launchDirectionOr(player, player.getLookAngle()));
        double length = SpellMetrics.waveLength(range);
        double endRadius = SpellMetrics.waveEndRadius("steam_burst", range, 2);
        for (Mob mob : hostiles(player, length + endRadius + 1.0)) {
            Vec3 delta = mob.position().subtract(origin);
            Vec3 flat = new Vec3(delta.x, 0.0, delta.z);
            double forward = flat.dot(look);
            if (forward < 0.0 || forward > length) continue;
            double lateralSq = Math.max(0.0, flat.lengthSqr() - forward * forward);
            double t = length <= 0.0001 ? 1.0 : forward / length;
            double allowed = endRadius * (0.16 + 0.84 * t) + Math.max(0.35, mob.getBbWidth() * 0.5);
            if (lateralSq > allowed * allowed) continue;
            ArcaneDamage.hurt(level, player, mob, (float) power);
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 75, 2));
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 45));
            Vec3 push = horizontal(delta).scale(0.35);
            mob.push(push.x, 0.12, push.z);
        }
        sound(level, player, SoundEvents.FIRE_EXTINGUISH, 0.9F, 0.85F);
        return true;
    }

    private static boolean frostStep(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = horizontal(CastTargetSnapshot.launchDirectionOr(player, player.getLookAngle()));
        player.push(look.x * Math.min(2.2, 0.8 + range * 0.08), 0.18,
                look.z * Math.min(2.2, 0.8 + range * 0.08));
        for (Mob mob : hostiles(player, 4.5)) {
            ArcaneDamage.hurt(level, player, mob, (float) (power * 0.65));
            mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 120));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 90, 2));
        }
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 50, 1));
        sound(level, player, SoundEvents.GLASS_BREAK, 0.65F, 1.55F);
        return true;
    }

    private static boolean thunderCage(ServerPlayer player, double range, double power) {
        Optional<Mob> target = sightTarget(player, range);
        if (target.isEmpty()) return false;
        ServerLevel level = (ServerLevel) player.level();
        Mob mob = target.get();
        ArcaneDamage.hurt(level, player, mob, (float) power);
        mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 130, 5));
        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 130, 2));
        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 130, 0));
        sound(level, player, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.45F, 1.65F);
        return true;
    }

    private static boolean solarGuard(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        int amplifier = Math.max(1, (int) Math.floor(power / 22.0));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 220, amplifier));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 300, 0));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 160, Math.min(2, amplifier / 2)));
        for (Mob mob : hostiles(player, Math.min(8.0, range))) {
            ArcaneDamage.hurt(level, player, mob, (float) (power * 0.42));
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 100));
        }
        sound(level, player, SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.3F);
        return true;
    }

    private static boolean voidLance(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = CastTargetSnapshot.launchOriginOr(player, player.getEyePosition());
        Vec3 fallbackDirection = CastTargetSnapshot.launchDirectionOr(player, player.getLookAngle());
        Vec3 fallbackEnd = start.add(fallbackDirection.scale(Math.max(8.0, range)));
        Vec3 end = CastTargetSnapshot.targetOr(player, fallbackEnd);
        Vec3 delta = end.subtract(start);
        if (delta.lengthSqr() < 1.0E-8) delta = fallbackDirection.scale(Math.max(8.0, range));
        double length = Math.max(0.001, delta.length());
        Vec3 direction = delta.scale(1.0 / length);
        end = start.add(delta);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(start, end).inflate(1.5),
                value -> value.isAlive() && value instanceof Enemy)) {
            Vec3 relative = mob.getEyePosition().subtract(start);
            double projected = relative.dot(direction);
            if (projected < 0.0 || projected > length) continue;
            Vec3 closest = start.add(direction.scale(projected));
            if (closest.distanceToSqr(mob.getEyePosition()) > 2.25) continue;
            ArcaneDamage.hurt(level, player, mob, (float) power);
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 90, 0));
        }
        sound(level, player, SoundEvents.ENDERMAN_TELEPORT, 0.85F, 0.65F);
        return true;
    }

    private static boolean winterDomain(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        double radius = Math.max(6.0, range * 0.45);
        for (Mob mob : hostiles(player, radius)) {
            ArcaneDamage.hurt(level, player, mob, (float) (power * 0.72));
            mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 260));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 180, 4));
            mob.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 180, 2));
        }
        sound(level, player, SoundEvents.GLASS_BREAK, 1.0F, 0.55F);
        return true;
    }

    private static boolean astralPrison(ServerPlayer player, double range, double power) {
        Optional<Mob> target = sightTarget(player, range);
        if (target.isEmpty()) return false;
        ServerLevel level = (ServerLevel) player.level();
        Mob mob = target.get();
        ArcaneDamage.hurt(level, player, mob, (float) (power * 0.58));
        mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 260, 6));
        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 260, 4));
        mob.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 35, 0));
        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 260, 0));
        sound(level, player, SoundEvents.BEACON_DEACTIVATE, 0.85F, 0.7F);
        return true;
    }

    private static boolean phoenixRequiem(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        player.heal((float) Math.max(8.0, power * 0.35));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 180, 2));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 360, 0));
        double radius = Math.max(8.0, range * 0.42);
        for (ServerPlayer ally : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(radius),
                value -> value.isAlive() && !value.isSpectator())) {
            ally.heal((float) Math.max(4.0, power * 0.16));
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
        }
        for (Mob mob : hostiles(player, radius)) {
            ArcaneDamage.hurt(level, player, mob, (float) (power * 0.62));
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 180));
        }
        sound(level, player, SoundEvents.TOTEM_USE, 1.0F, 1.05F);
        return true;
    }

    private static boolean worldSunder(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 fallback = player.position().add(horizontal(player.getLookAngle()).scale(Math.max(8.0, range * .70)));
        Vec3 center = CastTargetSnapshot.targetOr(player, fallback);
        double radius = Math.max(12.0, range * 0.38);
        for (Mob mob : hostilesAt(player, center, radius)) {
            double distance = Math.sqrt(mob.position().distanceToSqr(center));
            double distanceScale = Math.max(0.35, 1.0 - distance / radius);
            ArcaneDamage.hurt(level, player, mob, (float) (power * distanceScale));
            Vec3 away = horizontal(mob.position().subtract(center));
            mob.push(away.x * (.45 + distanceScale * .75), 0.65 + distanceScale * 0.9,
                    away.z * (.45 + distanceScale * .75));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 180, 3));
        }
        DestructiveMagicService.impact(player, "world_sunder", center, radius, power);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 1.0F, 0.52F);
        return true;
    }

    private static List<Mob> hostiles(ServerPlayer player, double radius) {
        ServerLevel level = (ServerLevel) player.level();
        return level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius),
                value -> value.isAlive() && value instanceof Enemy);
    }

    private static List<Mob> hostilesAt(ServerPlayer player, Vec3 center, double radius) {
        ServerLevel level = (ServerLevel) player.level();
        return level.getEntitiesOfClass(Mob.class, new AABB(center, center).inflate(radius),
                value -> value.isAlive() && value instanceof Enemy);
    }

    private static Optional<Mob> sightTarget(ServerPlayer player, double range) {
        Optional<CastTargetSnapshot> active = CastTargetSnapshot.active(player);
        if (active.isPresent()) {
            Optional<Mob> locked = active.get().targetEntity(player)
                    .filter(Mob.class::isInstance).map(Mob.class::cast)
                    .filter(mob -> mob instanceof Enemy);
            if (locked.isPresent()) return locked;
        }
        Vec3 eye = CastTargetSnapshot.launchOriginOr(player, player.getEyePosition());
        Vec3 look = CastTargetSnapshot.launchDirectionOr(player, player.getLookAngle());
        double distance = Math.max(5.0, range);
        Vec3 end = eye.add(look.scale(distance));
        ServerLevel level = (ServerLevel) player.level();
        return level.getEntitiesOfClass(Mob.class, new AABB(eye, end).inflate(2.5),
                        mob -> mob.isAlive() && mob instanceof Enemy).stream()
                .filter(mob -> {
                    Vec3 delta = mob.getEyePosition().subtract(eye);
                    double projection = delta.dot(look);
                    return projection >= 0.0 && projection <= distance
                            && delta.subtract(look.scale(projection)).length()
                            <= Math.max(1.4, mob.getBbWidth() + .9);
                })
                .min(Comparator.comparingDouble(mob -> mob.getEyePosition().distanceToSqr(eye)));
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 result = new Vec3(value.x, 0.0, value.z);
        return result.lengthSqr() < 0.00001 ? new Vec3(0, 0, 1) : result.normalize();
    }

    private static void sound(ServerLevel level, ServerPlayer player,
                              net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}

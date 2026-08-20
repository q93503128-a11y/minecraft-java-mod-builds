package kr.moonseungjun.arcanecircle.world;

import kr.moonseungjun.arcanecircle.magic.ArcaneDamage;
import kr.moonseungjun.arcanecircle.magic.ArcaneFieldService;
import kr.moonseungjun.arcanecircle.magic.CastTargetSnapshot;
import kr.moonseungjun.arcanecircle.magic.FirstCircleSpellService;
import kr.moonseungjun.arcanecircle.magic.HighWardSpellService;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellMetrics;
import kr.moonseungjun.arcanecircle.magic.SpellPresentationProfile;
import kr.moonseungjun.arcanecircle.magic.WorldMagicService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class NpcSpellResolver {
    private NpcSpellResolver() {}

    static int impactDelay(Mob caster, LivingEntity target, SpellDefinition spell) {
        if ("meteor_swarm".equals(spell.id())) return 1;
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        double distance = switch (profile.motion()) {
            case DART, BOLT, HEAVY_ORB, MISSILE_SWARM, LANCE ->
                    caster.getEyePosition().distanceTo(target.getEyePosition());
            case SKY_DROP -> profile.skyHeight();
            default -> 0.0;
        };
        return SpellPresentationProfile.impactDelayTicks(spell, distance);
    }

    static boolean execute(ServerLevel level, Mob caster, LivingEntity target,
                           SpellDefinition spell, double range, double power) {
        if (ArcaneFieldService.blocksCasting(caster)) {
            WorldMagicService.stop(caster);
            return false;
        }
        if (target == null || !target.isAlive() || caster.isAlliedTo(target)) return false;
        CastTargetSnapshot snapshot = WorldMagicService.consumeNpcSnapshot(caster, spell.id())
                .orElseGet(() -> WorldMagicService.captureSnapshot(caster, target, spell, range));
        if (!snapshot.validFor(caster)) return false;
        if (HighWardSpellService.intercepts(caster, spell, snapshot, range)) return false;
        if (FirstCircleSpellService.handles(spell.id())) {
            return FirstCircleSpellService.executeNpc(level, caster, target, spell, range, power, snapshot);
        }
        if ("meteor_swarm".equals(spell.id())) {
            return NpcMeteorBarrageService.schedule(level, caster, snapshot, range, power);
        }

        Vec3 lockedTarget = snapshot.target();
        return switch (SpellPresentationProfile.profile(spell).motion()) {
            case SKY_DROP, STORM, FIELD -> area(level, caster, lockedTarget, spell, range, power);
            case WAVE -> wave(level, caster, snapshot, spell, range, power);
            case WALL -> wall(level, caster, lockedTarget, snapshot.launchDirection(), spell, range, power);
            case BEAM, LANCE -> line(level, caster, snapshot.launchOrigin(), lockedTarget, power);
            case DART, BOLT, HEAVY_ORB, MISSILE_SWARM, TARGET_BURST, PRISON ->
                    directAt(level, caster, lockedTarget, power);
            default -> direct(level, caster, target, power);
        };
    }

    private static boolean direct(ServerLevel level, Mob caster, LivingEntity target, double power) {
        ArcaneDamage.hurt(level, caster, target, (float) power);
        return true;
    }

    private static boolean directAt(ServerLevel level, Mob caster, Vec3 impact, double power) {
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(impact, impact).inflate(1.75), value -> valid(caster, value)).stream()
                .min(java.util.Comparator.comparingDouble(value -> value.getEyePosition().distanceToSqr(impact)))
                .orElse(null);
        if (target == null) return false;
        return direct(level, caster, target, power);
    }

    private static boolean line(ServerLevel level, Mob caster, Vec3 start, Vec3 end, double power) {
        Vec3 delta = end.subtract(start);
        double length = Math.max(.001, delta.length());
        Vec3 unit = delta.scale(1.0 / length);
        boolean hit = false;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(start, end).inflate(1.25), value -> valid(caster, value))) {
            Vec3 relative = entity.getEyePosition().subtract(start);
            double projection = relative.dot(unit);
            if (projection < 0 || projection > length) continue;
            double width = Math.max(.8, entity.getBbWidth() * .65 + .55);
            if (relative.subtract(unit.scale(projection)).lengthSqr() > width * width) continue;
            ArcaneDamage.hurt(level, caster, entity, (float) power);
            hit = true;
        }
        return hit;
    }

    private static boolean wave(ServerLevel level, Mob caster, CastTargetSnapshot snapshot,
                                SpellDefinition spell, double range, double power) {
        Vec3 origin = snapshot.launchOrigin();
        Vec3 direction = snapshot.target().subtract(origin);
        if (direction.lengthSqr() < 1e-8) direction = snapshot.launchDirection();
        direction = direction.normalize();
        double length = Math.max(4, range);
        double endRadius = SpellMetrics.waveEndRadius(spell.id(), range, spell.circle());
        boolean hit = false;
        AABB box = new AABB(origin, origin.add(direction.scale(length))).inflate(endRadius + 1.5);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> valid(caster, value))) {
            Vec3 relative = entity.position().add(0, entity.getBbHeight() * .45, 0).subtract(origin);
            double projection = relative.dot(direction);
            if (projection < 0 || projection > length) continue;
            double allowed = Math.max(.8, endRadius * (projection / length)) + entity.getBbWidth() * .5;
            if (relative.subtract(direction.scale(projection)).lengthSqr() > allowed * allowed) continue;
            ArcaneDamage.hurt(level, caster, entity, (float) power);
            hit = true;
        }
        return hit;
    }

    private static boolean area(ServerLevel level, Mob caster, Vec3 center,
                                SpellDefinition spell, double range, double power) {
        double radius = Math.min(24, Math.max(3, SpellMetrics.effectRadius(spell.id(), range, spell.circle())));
        boolean hit = false;
        AABB box = new AABB(center, center).inflate(radius, Math.max(4, radius * .70), radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> valid(caster, value))) {
            ArcaneDamage.hurt(level, caster, entity, (float) power);
            hit = true;
        }
        return hit;
    }

    private static boolean wall(ServerLevel level, Mob caster, Vec3 center, Vec3 lockedDirection,
                                SpellDefinition spell, double range, double power) {
        double halfWidth = Math.min(20, Math.max(4, SpellMetrics.effectRadius(spell.id(), range, spell.circle())));
        Vec3 forward = new Vec3(lockedDirection.x, 0, lockedDirection.z);
        if (forward.lengthSqr() < 1e-8) forward = new Vec3(0, 0, 1);
        Vec3 forwardUnit = forward.normalize();
        Vec3 right = new Vec3(-forwardUnit.z, 0, forwardUnit.x);
        boolean hit = false;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(halfWidth + 1.5, 5, halfWidth + 1.5),
                value -> valid(caster, value))) {
            Vec3 delta = entity.position().subtract(center);
            double lateral = Math.abs(delta.dot(right));
            double depth = Math.abs(delta.dot(forwardUnit));
            if (lateral > halfWidth + entity.getBbWidth() || depth > 1.8 + entity.getBbWidth()) continue;
            ArcaneDamage.hurt(level, caster, entity, (float) power);
            hit = true;
        }
        return hit;
    }

    private static boolean valid(Mob caster, LivingEntity target) {
        return target != caster && target.isAlive() && !target.isRemoved() && !caster.isAlliedTo(target);
    }
}

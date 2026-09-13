package kr.moonseungjun.riftfrontier.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/** Applies the provisional Region 01 local-pressure displacement without owning attack timing or target authority. */
public final class Region01BossFieldImpulseResolver {
    public int applyActiveEntry(ServerLevel level, LivingEntity attacker, kr.moonseungjun.riftfrontier.content.ContentId patternId) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(patternId, "patternId");

        Region01BossFieldImpactProfile.Profile profile = Region01BossFieldImpactProfile.find(patternId).orElse(null);
        if (profile == null || profile.activeEntryRadialImpulse() <= 0.0D) {
            return 0;
        }

        AABB search = attacker.getBoundingBox().inflate(profile.reach(), profile.verticalRadius(), profile.reach());
        Vec3 origin = attacker.getBoundingBox().getCenter();
        int displaced = 0;
        for (LivingEntity target : level.getEntitiesOfClass(
            LivingEntity.class,
            search,
            candidate -> MinecraftCombatAuthority.isEligibleTarget(level, attacker, candidate)
                && Region01BossFieldImpactResolver.insideProfile(attacker, candidate, profile, 0.0D, 0.0D)
        )) {
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            var impulse = Region01BossFieldImpulsePolicy.radial(
                targetCenter.x - origin.x,
                targetCenter.z - origin.z,
                profile.activeEntryRadialImpulse()
            );
            if (impulse.isEmpty()) {
                continue;
            }
            var horizontal = impulse.orElseThrow();
            target.push(horizontal.x(), 0.0D, horizontal.z());
            displaced++;
        }
        return displaced;
    }
}

package kr.moonseungjun.arcanecircle.magic;

import kr.moonseungjun.arcanecircle.network.WorldMagicPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Server-authoritative visual event broadcaster for both players and NPC mages. */
public final class WorldMagicService {
    private WorldMagicService() {}

    public static void charge(ServerPlayer player, SpellDefinition spell, boolean fusion,
                              List<String> ingredients, double range, double progress) {
        Vec3 direction = safeDirection(player.getLookAngle());
        Vec3 target = targetPoint(player, spell, range, direction);
        Vec3 center = presentationCenter(player, spell, target, direction);
        send(player, encode("charge", player, spell, fusion, ingredients.size(), center, target,
                direction, range, spell.power(), clamp01(progress), 8, 0));
    }

    public static void release(ServerPlayer player, MagicPlayerData.CastPreparation cast) {
        SpellDefinition spell = cast.spell();
        Vec3 direction = safeDirection(player.getLookAngle());
        Vec3 target = targetPoint(player, spell, cast.range(), direction);
        Vec3 center = presentationCenter(player, spell, target, direction);
        double travelDistance = Math.max(0.0, target.distanceTo(center));
        int impactTicks = SpellPresentationProfile.impactDelayTicks(spell, kineticDistanceForVisual(player, spell, cast.range(), center, target));
        int duration = SpellPresentationProfile.releaseDurationTicks(spell, travelDistance);
        send(player, encode("release", player, spell, cast.fusion(), cast.ingredients().size(), center, target,
                direction, cast.range(), cast.power(), 1.0, duration, impactTicks));
    }

    public static void charge(LivingEntity caster, LivingEntity targetEntity, SpellDefinition spell,
                              double progress, double range, double power) {
        if (!(caster.level() instanceof ServerLevel)) return;
        Vec3 direction = targetEntity == null ? safeDirection(caster.getLookAngle())
                : safeDirection(targetEntity.getEyePosition().subtract(caster.getEyePosition()));
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        Vec3 target = targetEntity == null ? caster.getEyePosition().add(direction.scale(Math.max(3.0, range)))
                : profile.motion() == SpellPresentationProfile.MotionStyle.PRISON
                ? targetEntity.position() : targetEntity.getEyePosition();
        Vec3 center = presentationCenter(caster, spell, target, direction);
        send(caster, encode("charge", caster, spell, false, 0, center, target, direction,
                range, power, clamp01(progress), 8, 0));
    }

    public static void release(LivingEntity caster, LivingEntity targetEntity, SpellDefinition spell,
                               double range, double power) {
        if (!(caster.level() instanceof ServerLevel)) return;
        Vec3 direction = targetEntity == null ? safeDirection(caster.getLookAngle())
                : safeDirection(targetEntity.getEyePosition().subtract(caster.getEyePosition()));
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        Vec3 target = targetEntity == null ? caster.getEyePosition().add(direction.scale(Math.max(3.0, range)))
                : profile.motion() == SpellPresentationProfile.MotionStyle.PRISON
                ? targetEntity.position() : targetEntity.getEyePosition();
        Vec3 center = presentationCenter(caster, spell, target, direction);
        double distance = target.distanceTo(center);
        int impact = SpellPresentationProfile.impactDelayTicks(spell, Math.max(0.0, distance));
        int duration = SpellPresentationProfile.releaseDurationTicks(spell, Math.max(0.0, distance));
        send(caster, encode("release", caster, spell, false, 0, center, target, direction,
                range, power, 1.0, duration, impact));
    }

    public static void stop(ServerPlayer player) { stop((LivingEntity) player); }
    public static void stop(LivingEntity caster) { send(caster, "kind=stop;caster=" + caster.getUUID()); }

    private static void send(LivingEntity caster, String state) {
        if (!(caster.level() instanceof ServerLevel level)) return;
        PacketDistributor.sendToPlayersNear(level, null, caster.getX(), caster.getY(), caster.getZ(),
                160.0, new WorldMagicPayload(state));
    }

    private static String encode(String kind, LivingEntity caster, SpellDefinition spell, boolean fusion,
                                 int ingredientCount, Vec3 center, Vec3 target, Vec3 direction, double range,
                                 double power, double progress, int duration, int impactTicks) {
        return String.format(Locale.ROOT,
                "kind=%s;caster=%s;spell=%s;fusion=%d;ingredients=%d;x=%.5f;y=%.5f;z=%.5f;tx=%.5f;ty=%.5f;tz=%.5f;dx=%.5f;dy=%.5f;dz=%.5f;range=%.4f;power=%.4f;progress=%.4f;duration=%d;impact=%d",
                kind, caster.getUUID(), spell.id(), fusion ? 1 : 0, ingredientCount,
                center.x, center.y, center.z, target.x, target.y, target.z,
                direction.x, direction.y, direction.z, range, power, progress, duration, impactTicks);
    }

    private static Vec3 presentationCenter(LivingEntity caster, SpellDefinition spell, Vec3 target, Vec3 look) {
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        return switch (profile.sigil()) {
            case SKY_RITUAL -> target.add(0.0, profile.skyHeight(), 0.0);
            case TARGET_SEAL -> profile.motion() == SpellPresentationProfile.MotionStyle.PRISON
                    ? target.add(0.0, 0.055, 0.0) : target;
            case GROUND_SEAL, QUAD_ARRAY -> target.add(0.0, 0.055, 0.0);
            case WALL_MATRIX -> target.add(0.0, Math.max(1.2, profile.radius() * 0.34), 0.0);
            case PORTAL_GATE -> target.add(0.0, Math.max(1.1, profile.radius() * 0.52), 0.0);
            case BODY_HALO -> caster.position().add(0.0, 1.0, 0.0);
            case FEET_RUNE -> caster.position().add(0.0, 0.055, 0.0);
            case FRONT_COMPACT, FRONT_LANCE -> caster instanceof ServerPlayer player
                    ? visibleFrontAnchor(player, profile, look)
                    : caster.getEyePosition().add(safeDirection(look).scale(profile.sigil() == SpellPresentationProfile.SigilStyle.FRONT_LANCE ? 1.15 : 1.00));
        };
    }

    private static Vec3 targetPoint(ServerPlayer player, SpellDefinition spell, double range, Vec3 look) {
        Optional<Mob> target = aimedMob(player, range);
        return switch (spell.sigilAnchor()) {
            case GROUND_TARGET -> target.map(mob -> groundUnder(player, mob.position())).orElseGet(() -> aimGround(player, Math.max(4.0, range)));
            case TARGET -> SpellPresentationProfile.profile(spell).motion() == SpellPresentationProfile.MotionStyle.PRISON
                    ? target.map(mob -> groundUnder(player, mob.position()))
                    .orElseGet(() -> aimGround(player, Math.max(4.0, range)))
                    : target.<Vec3>map(Mob::getEyePosition)
                    .orElseGet(() -> visiblePoint(player, look, Math.max(3.0, range)));
            case FRONT -> target.<Vec3>map(Mob::getEyePosition).orElseGet(() -> visiblePoint(player, look, Math.max(3.0, range)));
            case FEET, GROUND_SELF -> player.position().add(0.0, 0.055, 0.0);
            case BODY -> player.position().add(0.0, 1.0, 0.0);
        };
    }

    private static Optional<Mob> aimedMob(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = safeDirection(player.getLookAngle());
        double max = Math.max(2.0, range);
        AABB box = player.getBoundingBox().expandTowards(look.scale(max)).inflate(2.4);
        return player.level().getEntitiesOfClass(Mob.class, box, mob -> mob.isAlive()
                        && !player.isAlliedTo(mob)
                        && (!(mob instanceof TamableAnimal tame) || !tame.isTame() || !tame.isOwnedBy(player))).stream()
                .filter(mob -> {
                    Vec3 to = mob.getEyePosition().subtract(eye);
                    double projection = to.dot(look);
                    return projection >= 0.0 && projection <= max
                            && to.subtract(look.scale(projection)).length() <= Math.max(1.25, mob.getBbWidth() + 0.8);
                })
                .min(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)));
    }

    private static Vec3 groundUnder(ServerPlayer player, Vec3 around) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos base = BlockPos.containing(around);
        for (int down = 0; down <= 8; down++) {
            BlockPos floor = base.below(down);
            BlockState state = level.getBlockState(floor);
            if (state.isFaceSturdy(level, floor, Direction.UP))
                return Vec3.atCenterOf(floor.above()).add(0.0, -0.48, 0.0);
        }
        return around;
    }

    private static Vec3 visibleFrontAnchor(ServerPlayer player, SpellPresentationProfile.Profile profile, Vec3 look) {
        double desired = profile.sigil() == SpellPresentationProfile.SigilStyle.FRONT_LANCE ? 1.18 : 0.96;
        return visiblePoint(player, look, desired);
    }

    private static Vec3 visiblePoint(ServerPlayer player, Vec3 look, double desiredDistance) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 origin = player.getEyePosition();
        Vec3 direction = safeDirection(look);
        double minDistance = 0.44;
        Vec3 lastVisible = origin.add(direction.scale(minDistance));
        double max = Math.max(minDistance, desiredDistance);
        for (double distance = minDistance; distance <= max + 1.0E-6; distance += 0.08) {
            Vec3 point = origin.add(direction.scale(distance));
            BlockPos pos = BlockPos.containing(point);
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty()) break;
            lastVisible = point;
        }
        return lastVisible;
    }

    private static Vec3 aimGround(ServerPlayer player, double range) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = safeDirection(player.getLookAngle());
        Vec3 origin = player.getEyePosition();
        Vec3 bestVisibleFloor = null;
        double max = Math.max(2.0, range);
        for (double distance = 0.70; distance <= max; distance += 0.32) {
            Vec3 sample = origin.add(look.scale(distance));
            BlockPos samplePos = BlockPos.containing(sample);
            BlockState sampleState = level.getBlockState(samplePos);
            if (!sampleState.getCollisionShape(level, samplePos).isEmpty()) {
                if (sampleState.isFaceSturdy(level, samplePos, Direction.UP))
                    bestVisibleFloor = Vec3.atCenterOf(samplePos.above()).add(0.0, -0.48, 0.0);
                break;
            }
            for (int down = 0; down <= 12; down++) {
                BlockPos floor = samplePos.below(down);
                BlockState state = level.getBlockState(floor);
                if (state.isFaceSturdy(level, floor, Direction.UP)) {
                    bestVisibleFloor = Vec3.atCenterOf(floor.above()).add(0.0, -0.48, 0.0);
                    break;
                }
            }
        }
        if (bestVisibleFloor != null) return bestVisibleFloor;
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0E-8) flat = new Vec3(0.0, 0.0, 1.0);
        return player.position().add(flat.normalize().scale(Math.min(3.0, range))).add(0.0, 0.055, 0.0);
    }

    static Vec3 lockedTarget(ServerPlayer player, SpellDefinition spell, double range) {
        Vec3 direction = safeDirection(player.getLookAngle());
        return targetPoint(player, spell, range, direction);
    }

    public static double kineticDistance(ServerPlayer player, SpellDefinition spell, double range) {
        Vec3 direction = safeDirection(player.getLookAngle());
        Vec3 target = targetPoint(player, spell, range, direction);
        Vec3 center = presentationCenter(player, spell, target, direction);
        return kineticDistanceForVisual(player, spell, range, center, target);
    }

    private static double kineticDistanceForVisual(ServerPlayer player, SpellDefinition spell, double range,
                                                   Vec3 center, Vec3 target) {
        if (SpellPresentationProfile.profile(spell).motion() == SpellPresentationProfile.MotionStyle.SKY_DROP)
            return Math.max(0.0, target.distanceTo(center));
        return aimedMob(player, range).map(mob -> mob.getEyePosition().distanceTo(center))
                .orElse(Math.max(0.0, target.distanceTo(center)));
    }

    private static Vec3 safeDirection(Vec3 value) {
        return value == null || value.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : value.normalize();
    }
    private static double clamp01(double value) { return Math.max(0.0, Math.min(1.0, value)); }
}

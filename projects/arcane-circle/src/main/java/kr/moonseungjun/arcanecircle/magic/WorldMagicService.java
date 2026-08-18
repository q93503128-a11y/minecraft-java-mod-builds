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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Server-authoritative visual event broadcaster for both players and NPC mages. */
public final class WorldMagicService {
    private static final Map<UUID, CastTargetSnapshot> NPC_RELEASES = new HashMap<>();
    private static final Map<UUID, ChargeSeed> PLAYER_CHARGE_SEEDS = new HashMap<>();
    private static final Map<UUID, ChargeSeed> NPC_CHARGE_SEEDS = new HashMap<>();
    private static final Set<String> HOMING_SPELLS = Set.of();
    private record ChargeSeed(String spellId, long seed) {}

    private WorldMagicService() {}

    public static void charge(ServerPlayer player, SpellDefinition spell, boolean fusion,
                              List<String> ingredients, double range, double progress) {
        Vec3 direction = safeDirection(player.getLookAngle());
        Optional<Mob> aimed = aimedMob(player, range, direction);
        Vec3 target = targetPoint(player, spell, range, direction, aimed);
        Vec3 center = presentationCenter(player, spell, target, direction);
        long seed = chargeSeed(player, spell);
        send(player, encode("charge", player, spell, fusion, ingredients.size(), center, target,
                direction, range, spell.power(), clamp01(progress), 8, 0, seed));
    }

    public static CastTargetSnapshot captureSnapshot(ServerPlayer player, SpellDefinition spell, double range) {
        Vec3 direction = safeDirection(player.getLookAngle());
        Optional<Mob> aimed = aimedMob(player, range, direction);
        Vec3 target = targetPoint(player, spell, range, direction, aimed);
        UUID entityId = switch (spell.sigilAnchor()) {
            case FRONT, TARGET, GROUND_TARGET -> aimed.map(Mob::getUUID).orElse(null);
            default -> null;
        };
        long seed = releaseSeed(player, spell);
        return new CastTargetSnapshot(spell.id(), player.getUUID(), player.level().dimension(),
                player.getEyePosition(), target, direction, entityId, impactSurface(spell, target),
                HOMING_SPELLS.contains(spell.id()), seed);
    }

    public static CastTargetSnapshot captureSnapshot(LivingEntity caster, LivingEntity targetEntity,
                                                     SpellDefinition spell, double range) {
        long seed = "meteor_swarm".equals(spell.id())
                ? MeteorBarragePattern.castSeed(caster.getUUID(), ((ServerLevel) caster.level()).getGameTime())
                : 0L;
        return captureSnapshot(caster, targetEntity, spell, range, seed);
    }

    private static CastTargetSnapshot captureSnapshot(LivingEntity caster, LivingEntity targetEntity,
                                                       SpellDefinition spell, double range, long barrageSeed) {
        Vec3 direction = targetEntity == null ? safeDirection(caster.getLookAngle())
                : safeDirection(targetEntity.getEyePosition().subtract(caster.getEyePosition()));
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        Vec3 target;
        if (targetEntity == null) {
            target = caster.getEyePosition().add(direction.scale(Math.max(3.0, range)));
        } else {
            target = switch (spell.sigilAnchor()) {
                case GROUND_TARGET -> groundUnder(caster, targetEntity.position());
                case GROUND_SELF, FEET -> caster.position().add(0.0, 0.055, 0.0);
                case BODY -> caster.position().add(0.0, 1.0, 0.0);
                case TARGET -> profile.motion() == SpellPresentationProfile.MotionStyle.PRISON
                        ? groundUnder(caster, targetEntity.position()) : targetEntity.getEyePosition();
                case FRONT -> targetEntity.getEyePosition();
            };
        }
        UUID entityId = targetEntity == null ? null : targetEntity.getUUID();
        return new CastTargetSnapshot(spell.id(), caster.getUUID(), caster.level().dimension(),
                caster.getEyePosition(), target, direction, entityId, impactSurface(spell, target),
                false, barrageSeed);
    }

    public static void release(ServerPlayer player, MagicPlayerData.CastPreparation cast) {
        release(player, cast, captureSnapshot(player, cast.spell(), cast.range()));
    }

    public static void release(ServerPlayer player, MagicPlayerData.CastPreparation cast,
                               CastTargetSnapshot snapshot) {
        SpellDefinition spell = cast.spell();
        Vec3 direction = snapshot.launchDirection();
        Vec3 target = snapshot.target();
        Vec3 center = presentationCenter(player, spell, target, direction);
        double travelDistance = Math.max(0.0, target.distanceTo(center));
        double kineticDistance = kineticDistanceForVisual(spell, cast.range(), center, target);
        int impactTicks = "meteor_swarm".equals(spell.id())
                ? MeteorBarragePattern.firstImpactTick(snapshot.barrageSeed())
                : SpellPresentationProfile.impactDelayTicks(spell, kineticDistance);
        int duration = "meteor_swarm".equals(spell.id())
                ? MeteorBarragePattern.durationTicks(snapshot.barrageSeed())
                : SpellPresentationProfile.releaseDurationTicks(spell, travelDistance);
        // Etherealness is a true maintained 7C self-state. Its server duration scales with power,
        // so the release geometry must live for that same authored duration instead of AURA's 28 ticks.
        if ("etherealness".equals(spell.id())) duration = Math.max(duration, 360 + (int) cast.power());
        send(player, encode("release", player, spell, cast.fusion(), cast.ingredients().size(), center, target,
                direction, cast.range(), cast.power(), 1.0, duration, impactTicks, snapshot.barrageSeed()));
    }

    public static void charge(LivingEntity caster, LivingEntity targetEntity, SpellDefinition spell,
                              double progress, double range, double power) {
        if (!(caster.level() instanceof ServerLevel)) return;
        long seed = npcChargeSeed(caster, spell);
        CastTargetSnapshot snapshot = captureSnapshot(caster, targetEntity, spell, range, seed);
        Vec3 center = presentationCenter(caster, spell, snapshot.target(), snapshot.launchDirection());
        send(caster, encode("charge", caster, spell, false, 0, center, snapshot.target(),
                snapshot.launchDirection(), range, power, clamp01(progress), 8, 0, seed));
    }

    public static void release(LivingEntity caster, LivingEntity targetEntity, SpellDefinition spell,
                               double range, double power) {
        if (!(caster.level() instanceof ServerLevel)) return;
        long seed = npcReleaseSeed(caster, spell);
        CastTargetSnapshot snapshot = captureSnapshot(caster, targetEntity, spell, range, seed);
        NPC_RELEASES.put(caster.getUUID(), snapshot);
        Vec3 center = presentationCenter(caster, spell, snapshot.target(), snapshot.launchDirection());
        double distance = snapshot.target().distanceTo(center);
        int impact = "meteor_swarm".equals(spell.id())
                ? MeteorBarragePattern.firstImpactTick(snapshot.barrageSeed())
                : SpellPresentationProfile.impactDelayTicks(spell, Math.max(0.0, distance));
        int duration = "meteor_swarm".equals(spell.id())
                ? MeteorBarragePattern.durationTicks(snapshot.barrageSeed())
                : SpellPresentationProfile.releaseDurationTicks(spell, Math.max(0.0, distance));
        send(caster, encode("release", caster, spell, false, 0, center, snapshot.target(),
                snapshot.launchDirection(), range, power, 1.0, duration, impact, snapshot.barrageSeed()));
    }

    public static Optional<CastTargetSnapshot> consumeNpcSnapshot(LivingEntity caster, String spellId) {
        CastTargetSnapshot snapshot = NPC_RELEASES.remove(caster.getUUID());
        if (snapshot == null || !snapshot.validFor(caster) || !snapshot.spellId().equals(spellId))
            return Optional.empty();
        return Optional.of(snapshot);
    }

    public static void stop(ServerPlayer player) { stop((LivingEntity) player); }

    public static void stop(LivingEntity caster) {
        NPC_RELEASES.remove(caster.getUUID());
        PLAYER_CHARGE_SEEDS.remove(caster.getUUID());
        NPC_CHARGE_SEEDS.remove(caster.getUUID());
        send(caster, "kind=stop;caster=" + caster.getUUID());
    }

    /** Cancels one already-released spell without killing unrelated maintained magic. */
    public static void cancelRelease(LivingEntity caster, String spellId) {
        if (caster == null || spellId == null || spellId.isBlank()) return;
        send(caster, "kind=cancel;caster=" + caster.getUUID() + ";spell=" + spellId);
    }

    /** Hard lifecycle boundary used on logout/respawn/dimension change. */
    public static void clearVisuals(LivingEntity caster) {
        if (caster == null) return;
        send(caster, "kind=clear;caster=" + caster.getUUID());
    }

    public static void clearAll() {
        NPC_RELEASES.clear();
        PLAYER_CHARGE_SEEDS.clear();
        NPC_CHARGE_SEEDS.clear();
    }

    private static long chargeSeed(ServerPlayer player, SpellDefinition spell) {
        if (!"meteor_swarm".equals(spell.id())) {
            PLAYER_CHARGE_SEEDS.remove(player.getUUID());
            return 0L;
        }
        ChargeSeed existing = PLAYER_CHARGE_SEEDS.get(player.getUUID());
        if (existing != null && existing.spellId().equals(spell.id())) return existing.seed();
        long seed = ThreadLocalRandom.current().nextLong();
        if (seed == 0L) seed = MeteorBarragePattern.castSeed(player.getUUID(),
                ((ServerLevel) player.level()).getGameTime());
        PLAYER_CHARGE_SEEDS.put(player.getUUID(), new ChargeSeed(spell.id(), seed));
        return seed;
    }

    private static long releaseSeed(ServerPlayer player, SpellDefinition spell) {
        ChargeSeed existing = PLAYER_CHARGE_SEEDS.remove(player.getUUID());
        if (existing != null && existing.spellId().equals(spell.id())) return existing.seed();
        long seed = ThreadLocalRandom.current().nextLong();
        return seed == 0L ? MeteorBarragePattern.castSeed(player.getUUID(),
                ((ServerLevel) player.level()).getGameTime()) : seed;
    }

    private static long npcChargeSeed(LivingEntity caster, SpellDefinition spell) {
        if (!"meteor_swarm".equals(spell.id())) {
            NPC_CHARGE_SEEDS.remove(caster.getUUID());
            return 0L;
        }
        ChargeSeed existing = NPC_CHARGE_SEEDS.get(caster.getUUID());
        if (existing != null && existing.spellId().equals(spell.id())) return existing.seed();
        long seed = MeteorBarragePattern.castSeed(caster.getUUID(), ((ServerLevel) caster.level()).getGameTime());
        NPC_CHARGE_SEEDS.put(caster.getUUID(), new ChargeSeed(spell.id(), seed));
        return seed;
    }

    private static long npcReleaseSeed(LivingEntity caster, SpellDefinition spell) {
        ChargeSeed existing = NPC_CHARGE_SEEDS.remove(caster.getUUID());
        if (existing != null && existing.spellId().equals(spell.id())) return existing.seed();
        return "meteor_swarm".equals(spell.id())
                ? MeteorBarragePattern.castSeed(caster.getUUID(), ((ServerLevel) caster.level()).getGameTime())
                : 0L;
    }

    private static void send(LivingEntity caster, String state) {
        if (!(caster.level() instanceof ServerLevel level)) return;
        PacketDistributor.sendToPlayersNear(level, null, caster.getX(), caster.getY(), caster.getZ(),
                160.0, new WorldMagicPayload(state));
    }

    private static String encode(String kind, LivingEntity caster, SpellDefinition spell, boolean fusion,
                                 int ingredientCount, Vec3 center, Vec3 target, Vec3 direction, double range,
                                 double power, double progress, int duration, int impactTicks, long seed) {
        return String.format(Locale.ROOT,
                "kind=%s;caster=%s;spell=%s;fusion=%d;ingredients=%d;x=%.5f;y=%.5f;z=%.5f;tx=%.5f;ty=%.5f;tz=%.5f;dx=%.5f;dy=%.5f;dz=%.5f;range=%.4f;power=%.4f;progress=%.4f;duration=%d;impact=%d;seed=%d",
                kind, caster.getUUID(), spell.id(), fusion ? 1 : 0, ingredientCount,
                center.x, center.y, center.z, target.x, target.y, target.z,
                direction.x, direction.y, direction.z, range, power, progress, duration, impactTicks, seed);
    }

    private static Vec3 presentationCenter(LivingEntity caster, SpellDefinition spell, Vec3 target, Vec3 look) {
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        return switch (profile.sigil()) {
            case SKY_RITUAL -> target.add(0.0, profile.skyHeight(), 0.0);
            case TARGET_SEAL -> profile.motion() == SpellPresentationProfile.MotionStyle.PRISON
                    ? target.add(0.0, 0.055, 0.0) : target;
            case GROUND_SEAL, QUAD_ARRAY -> target.add(0.0, 0.055, 0.0);
            case WALL_MATRIX -> target.add(0.0, Math.max(1.2, profile.radius() * 0.34), 0.0);
            case PORTAL_GATE -> caster.position().add(0.0, 0.055, 0.0);
            case BODY_HALO -> caster.position().add(0.0, 1.0, 0.0);
            case FEET_RUNE -> caster.position().add(0.0, 0.055, 0.0);
            case FRONT_COMPACT, FRONT_LANCE -> caster instanceof ServerPlayer player
                    ? visibleFrontAnchor(player, profile, look)
                    : caster.getEyePosition().add(safeDirection(look).scale(
                    profile.sigil() == SpellPresentationProfile.SigilStyle.FRONT_LANCE ? 1.15 : 1.00));
        };
    }

    private static Vec3 targetPoint(ServerPlayer player, SpellDefinition spell, double range, Vec3 look,
                                    Optional<Mob> target) {
        return switch (spell.sigilAnchor()) {
            case GROUND_TARGET -> target.map(mob -> groundUnder(player, mob.position()))
                    .orElseGet(() -> aimGround(player, Math.max(4.0, range), look));
            case TARGET -> SpellPresentationProfile.profile(spell).motion() == SpellPresentationProfile.MotionStyle.PRISON
                    ? target.map(mob -> groundUnder(player, mob.position()))
                    .orElseGet(() -> aimGround(player, Math.max(4.0, range), look))
                    : target.<Vec3>map(Mob::getEyePosition)
                    .orElseGet(() -> visiblePoint(player, look, Math.max(3.0, range)));
            case FRONT -> target.<Vec3>map(Mob::getEyePosition)
                    .orElseGet(() -> visiblePoint(player, look, Math.max(3.0, range)));
            case FEET, GROUND_SELF -> player.position().add(0.0, 0.055, 0.0);
            case BODY -> player.position().add(0.0, 1.0, 0.0);
        };
    }

    private static Optional<Mob> aimedMob(ServerPlayer player, double range, Vec3 look) {
        Vec3 eye = player.getEyePosition();
        Vec3 direction = safeDirection(look);
        double max = Math.max(2.0, range);
        AABB box = player.getBoundingBox().expandTowards(direction.scale(max)).inflate(2.4);
        return player.level().getEntitiesOfClass(Mob.class, box, mob -> mob.isAlive()
                        && !player.isAlliedTo(mob)
                        && (!(mob instanceof TamableAnimal tame) || !tame.isTame() || !tame.isOwnedBy(player))).stream()
                .filter(mob -> {
                    Vec3 to = mob.getEyePosition().subtract(eye);
                    double projection = to.dot(direction);
                    return projection >= 0.0 && projection <= max
                            && to.subtract(direction.scale(projection)).length()
                            <= Math.max(1.25, mob.getBbWidth() + 0.8);
                })
                .min(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)));
    }

    private static Vec3 groundUnder(LivingEntity caster, Vec3 around) {
        ServerLevel level = (ServerLevel) caster.level();
        BlockPos base = BlockPos.containing(around);
        for (int down = 0; down <= 8; down++) {
            BlockPos floor = base.below(down);
            BlockState state = level.getBlockState(floor);
            if (state.isFaceSturdy(level, floor, Direction.UP))
                return Vec3.atCenterOf(floor.above()).add(0.0, -0.48, 0.0);
        }
        return around;
    }

    private static BlockPos impactSurface(SpellDefinition spell, Vec3 target) {
        return switch (spell.sigilAnchor()) {
            case FEET, GROUND_SELF, GROUND_TARGET -> BlockPos.containing(target.add(0.0, -0.08, 0.0));
            default -> null;
        };
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

    private static Vec3 aimGround(ServerPlayer player, double range, Vec3 look) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 direction = safeDirection(look);
        Vec3 origin = player.getEyePosition();
        Vec3 bestVisibleFloor = null;
        double max = Math.max(2.0, range);
        for (double distance = 0.70; distance <= max; distance += 0.32) {
            Vec3 sample = origin.add(direction.scale(distance));
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
        Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
        if (flat.lengthSqr() < 1.0E-8) flat = new Vec3(0.0, 0.0, 1.0);
        return player.position().add(flat.normalize().scale(Math.min(3.0, range))).add(0.0, 0.055, 0.0);
    }

    static Vec3 lockedTarget(ServerPlayer player, SpellDefinition spell, double range) {
        return captureSnapshot(player, spell, range).target();
    }

    public static double kineticDistance(ServerPlayer player, SpellDefinition spell, double range) {
        return kineticDistance(player, spell, range, captureSnapshot(player, spell, range));
    }

    public static double kineticDistance(ServerPlayer player, SpellDefinition spell, double range,
                                         CastTargetSnapshot snapshot) {
        Vec3 center = presentationCenter(player, spell, snapshot.target(), snapshot.launchDirection());
        return kineticDistanceForVisual(spell, range, center, snapshot.target());
    }

    private static double kineticDistanceForVisual(SpellDefinition spell, double range, Vec3 center, Vec3 target) {
        return Math.max(0.0, target.distanceTo(center));
    }

    private static Vec3 safeDirection(Vec3 value) {
        return value == null || value.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : value.normalize();
    }

    private static double clamp01(double value) { return Math.max(0.0, Math.min(1.0, value)); }
}

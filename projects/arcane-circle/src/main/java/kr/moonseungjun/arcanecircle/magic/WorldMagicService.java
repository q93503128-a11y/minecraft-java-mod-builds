package kr.moonseungjun.arcanecircle.magic;

import kr.moonseungjun.arcanecircle.network.WorldMagicPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;

/** Server-authoritative visual event broadcaster for both players and NPC mages. */
public final class WorldMagicService {
    private WorldMagicService() {}

    public static void charge(ServerPlayer player, SpellDefinition spell, boolean fusion,
                              List<String> ingredients, double range, double progress) {
        Vec3 direction = safeDirection(player.getLookAngle());
        Vec3 center = anchorCenter(player, spell, range, direction);
        send(player, encode("charge", player, spell, fusion, ingredients.size(), center, direction,
                range, spell.power(), clamp01(progress), 8));
    }

    public static void release(ServerPlayer player, MagicPlayerData.CastPreparation cast) {
        SpellDefinition spell = cast.spell();
        Vec3 direction = safeDirection(player.getLookAngle());
        Vec3 center = anchorCenter(player, spell, cast.range(), direction);
        int duration = 10 + spell.circle() * 5 + (cast.fusion() ? 8 : 0);
        send(player, encode("release", player, spell, cast.fusion(), cast.ingredients().size(), center, direction,
                cast.range(), cast.power(), 1.0, duration));
    }

    public static void charge(LivingEntity caster, LivingEntity target, SpellDefinition spell,
                              double progress, double range, double power) {
        if (!(caster.level() instanceof ServerLevel)) return;
        Vec3 direction = target == null ? safeDirection(caster.getLookAngle())
                : safeDirection(target.getEyePosition().subtract(caster.getEyePosition()));
        Vec3 center = caster.getEyePosition().add(direction.scale(1.15 + spell.circle() * 0.035));
        send(caster, encode("charge", caster, spell, false, 0, center, direction,
                range, power, clamp01(progress), 8));
    }

    public static void release(LivingEntity caster, LivingEntity target, SpellDefinition spell,
                               double range, double power) {
        if (!(caster.level() instanceof ServerLevel)) return;
        Vec3 direction = target == null ? safeDirection(caster.getLookAngle())
                : safeDirection(target.getEyePosition().subtract(caster.getEyePosition()));
        Vec3 center = caster.getEyePosition().add(direction.scale(1.15 + spell.circle() * 0.035));
        send(caster, encode("release", caster, spell, false, 0, center, direction,
                range, power, 1.0, 10 + spell.circle() * 4));
    }

    public static void stop(ServerPlayer player) {
        stop((LivingEntity) player);
    }

    public static void stop(LivingEntity caster) {
        send(caster, "kind=stop;caster=" + caster.getUUID());
    }

    private static void send(LivingEntity caster, String state) {
        if (!(caster.level() instanceof ServerLevel level)) return;
        PacketDistributor.sendToPlayersNear(level, null, caster.getX(), caster.getY(), caster.getZ(),
                160.0, new WorldMagicPayload(state));
    }

    private static String encode(String kind, LivingEntity caster, SpellDefinition spell, boolean fusion,
                                 int ingredientCount, Vec3 center, Vec3 direction, double range,
                                 double power, double progress, int duration) {
        return String.format(Locale.ROOT,
                "kind=%s;caster=%s;spell=%s;fusion=%d;ingredients=%d;x=%.5f;y=%.5f;z=%.5f;dx=%.5f;dy=%.5f;dz=%.5f;range=%.4f;power=%.4f;progress=%.4f;duration=%d",
                kind, caster.getUUID(), spell.id(), fusion ? 1 : 0, ingredientCount,
                center.x, center.y, center.z, direction.x, direction.y, direction.z,
                range, power, progress, duration);
    }

    private static Vec3 anchorCenter(ServerPlayer player, SpellDefinition spell, double range, Vec3 look) {
        return switch (spell.sigilAnchor()) {
            case FRONT -> visibleFrontAnchor(player, spell, look);
            case FEET, GROUND_SELF -> player.position().add(0.0, 0.055, 0.0);
            case BODY -> player.position().add(0.0, 1.0, 0.0);
            case GROUND_TARGET -> aimGround(player, Math.max(4.0, range));
            case TARGET -> visiblePoint(player, look, Math.min(Math.max(3.0, range * 0.72), 18.0));
        };
    }

    /**
     * Keeps a front-facing charge sigil on the caster side of nearby collision geometry. The old
     * fixed 1.6-1.9 block offset could put the entire plate behind a wall when casting indoors.
     */
    private static Vec3 visibleFrontAnchor(ServerPlayer player, SpellDefinition spell, Vec3 look) {
        double desired = 1.02 + Math.min(0.58, spell.circle() * 0.065);
        return visiblePoint(player, look, desired);
    }

    private static Vec3 visiblePoint(ServerPlayer player, Vec3 look, double desiredDistance) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 origin = player.getEyePosition();
        Vec3 direction = safeDirection(look);
        double minDistance = 0.48;
        Vec3 lastVisible = origin.add(direction.scale(minDistance));
        double max = Math.max(minDistance, desiredDistance);
        for (double distance = minDistance; distance <= max + 1.0E-6; distance += 0.07) {
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
        double max = Math.min(Math.max(2.0, range), 28.0);

        // March from the caster outward and stop at the first collision. This prevents a target
        // glyph from being selected on a floor hidden behind a wall or closed door.
        for (double distance = 0.70; distance <= max; distance += 0.32) {
            Vec3 sample = origin.add(look.scale(distance));
            BlockPos samplePos = BlockPos.containing(sample);
            BlockState sampleState = level.getBlockState(samplePos);
            if (!sampleState.getCollisionShape(level, samplePos).isEmpty()) {
                if (sampleState.isFaceSturdy(level, samplePos, Direction.UP)) {
                    bestVisibleFloor = Vec3.atCenterOf(samplePos.above()).add(0.0, -0.48, 0.0);
                }
                break;
            }
            for (int down = 0; down <= 10; down++) {
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

    private static Vec3 safeDirection(Vec3 value) {
        return value == null || value.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : value.normalize();
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}

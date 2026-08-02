package kr.moonseungjun.arcanecircle.magic;

import kr.moonseungjun.arcanecircle.network.WorldMagicPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;

public final class WorldMagicService {
    private WorldMagicService() {}

    public static void charge(ServerPlayer player, SpellDefinition spell, boolean fusion,
                              List<String> ingredients, double range, double progress) {
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 center = anchorCenter(player, spell, range, direction);
        send(player, encode("charge", player, spell, fusion, ingredients, center, direction,
                range, spell.power(), Math.max(0.0, Math.min(1.0, progress)), 8));
    }

    public static void release(ServerPlayer player, MagicPlayerData.CastPreparation cast) {
        SpellDefinition spell = cast.spell();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 center = anchorCenter(player, spell, cast.range(), direction);
        int duration = 10 + spell.circle() * 5 + (cast.fusion() ? 8 : 0);
        send(player, encode("release", player, spell, cast.fusion(), cast.ingredients(), center, direction,
                cast.range(), cast.power(), 1.0, duration));
    }

    public static void stop(ServerPlayer player) {
        send(player, "kind=stop;caster=" + player.getUUID());
    }

    public static void noParticles() {
        // Deliberate no-op. Core spell visuals are submitted as world geometry on clients.
    }

    private static void send(ServerPlayer player, String state) {
        ServerLevel level = (ServerLevel) player.level();
        PacketDistributor.sendToPlayersNear(level, null, player.getX(), player.getY(), player.getZ(),
                128.0, new WorldMagicPayload(state));
    }

    private static String encode(String kind, ServerPlayer player, SpellDefinition spell, boolean fusion,
                                 List<String> ingredients, Vec3 center, Vec3 direction, double range,
                                 double power, double progress, int duration) {
        return String.format(Locale.ROOT,
                "kind=%s;caster=%s;spell=%s;fusion=%d;ingredients=%d;x=%.5f;y=%.5f;z=%.5f;dx=%.5f;dy=%.5f;dz=%.5f;range=%.4f;power=%.4f;progress=%.4f;duration=%d",
                kind, player.getUUID(), spell.id(), fusion ? 1 : 0, ingredients.size(),
                center.x, center.y, center.z, direction.x, direction.y, direction.z,
                range, power, progress, duration);
    }

    private static Vec3 anchorCenter(ServerPlayer player, SpellDefinition spell, double range, Vec3 look) {
        return switch (spell.sigilAnchor()) {
            case FRONT -> player.getEyePosition().add(look.scale(1.55 + spell.circle() * 0.035));
            case FEET, GROUND_SELF -> player.position().add(0.0, 0.055, 0.0);
            case BODY -> player.position().add(0.0, 1.0, 0.0);
            case GROUND_TARGET -> aimGround(player, Math.max(4.0, range));
            case TARGET -> player.getEyePosition().add(look.scale(Math.min(Math.max(3.0, range * 0.72), 18.0)));
        };
    }

    private static Vec3 aimGround(ServerPlayer player, double range) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.getEyePosition();
        for (int step = (int) Math.max(2, Math.floor(Math.min(range, 28.0))); step >= 2; step--) {
            BlockPos candidate = BlockPos.containing(origin.add(look.scale(step)));
            for (int down = 0; down <= 10; down++) {
                BlockPos floor = candidate.below(down);
                BlockState state = level.getBlockState(floor);
                if (state.isFaceSturdy(level, floor, Direction.UP)) {
                    return Vec3.atCenterOf(floor.above()).add(0.0, -0.48, 0.0);
                }
            }
        }
        return player.position().add(new Vec3(look.x, 0.0, look.z).normalize().scale(Math.min(6.0, range)))
                .add(0.0, 0.055, 0.0);
    }
}

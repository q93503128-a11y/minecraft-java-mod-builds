package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.presentation.PersonalPresentationIsolation;
import io.github.q93503128.turnbound.progression.GachaService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Player-private in-world summon reveal. RNG and ownership are already committed before this class runs;
 * it owns only temporary presentation actors and never mutates progression.
 */
public final class GachaPresentationActorService {
    private static final int REVEAL_TICKS = 30;
    private static final Map<UUID, Active> ACTIVE = new LinkedHashMap<>();

    private static final class Active {
        final ServerLevel level;
        final List<String> revealIds;
        int index;
        int ticksToNext = REVEAL_TICKS;
        int ttl;
        UUID actorId;

        Active(ServerLevel level, List<String> revealIds) {
            this.level = level;
            this.revealIds = revealIds;
            this.ttl = Math.max(100, revealIds.size() * REVEAL_TICKS + 100);
        }
    }

    private GachaPresentationActorService() {}

    public static void begin(ServerPlayer player, GachaService.BatchResult result) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        finish(player);
        List<String> revealIds = GachaPresentationPlan.revealCharacterIds(result);
        if (revealIds.isEmpty()) return;
        Active active = new Active(level, revealIds);
        ACTIVE.put(player.getUUID(), active);
        spawnCurrent(player, active);
    }

    public static void tick(ServerPlayer player) {
        if (player == null) return;
        Active active = ACTIVE.get(player.getUUID());
        if (active == null) return;
        if (player.level() != active.level || --active.ttl <= 0) {
            finish(player);
            return;
        }
        if (active.index + 1 < active.revealIds.size() && --active.ticksToNext <= 0) {
            removeActor(active);
            active.index++;
            active.ticksToNext = REVEAL_TICKS;
            spawnCurrent(player, active);
        }
    }

    public static void finish(ServerPlayer player) {
        if (player == null) return;
        Active active = ACTIVE.remove(player.getUUID());
        if (active != null) removeActor(active);
    }

    public static void clearAll() {
        for (Active active : List.copyOf(ACTIVE.values())) removeActor(active);
        ACTIVE.clear();
    }

    private static void spawnCurrent(ServerPlayer player, Active active) {
        if (active.index < 0 || active.index >= active.revealIds.size()) return;
        String characterId = active.revealIds.get(active.index);
        Vec3 position = safeRevealPosition(active.level, player);
        if (position == null) return;
        float yaw = faceYaw(position, player.position());
        BattleActorEntity actor = PersonalPresentationIsolation.spawnPrivateActor(
                active.level, characterId, position, yaw, player.getUUID());
        if (actor == null) return;
        actor.setCustomName(Component.literal(CanonicalData.definition(characterId).name()));
        actor.setCustomNameVisible(false);
        actor.playVictory();
        active.actorId = actor.getUUID();
    }

    private static void removeActor(Active active) {
        if (active.actorId == null) return;
        Entity entity = active.level.getEntity(active.actorId);
        if (entity != null) entity.discard();
        active.actorId = null;
    }

    private static Vec3 safeRevealPosition(ServerLevel level, ServerPlayer player) {
        double radians = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        double[] distances = {3.4, 4.3, 2.7, 5.2, 6.0};
        double[] lateral = {0.0, 1.25, -1.25, 2.4, -2.4};

        for (double distance : distances) {
            for (double side : lateral) {
                Vec3 raw = player.position().add(forward.scale(distance)).add(right.scale(side));
                int x = (int)Math.floor(raw.x);
                int z = (int)Math.floor(raw.z);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                Vec3 candidate = new Vec3(x + 0.5, y, z + 0.5);
                if (Math.abs(candidate.y - player.getY()) <= 3.0 && open(level, candidate)) return candidate;
            }
        }
        return null;
    }

    private static boolean open(ServerLevel level, Vec3 point) {
        BlockPos feet = BlockPos.containing(point.x, point.y + 0.05, point.z);
        BlockPos body = feet.above();
        BlockPos head = body.above();
        BlockPos below = feet.below();
        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) return false;
        if (!level.getBlockState(body).getCollisionShape(level, body).isEmpty()) return false;
        if (!level.getBlockState(head).getCollisionShape(level, head).isEmpty()) return false;
        if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(body).isEmpty()) return false;
        return !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }

    private static float faceYaw(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float)Math.toDegrees(Math.atan2(-dx, dz));
    }
}

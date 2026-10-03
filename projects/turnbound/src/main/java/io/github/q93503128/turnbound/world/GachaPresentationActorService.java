package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.presentation.PersonalPresentationIsolation;
import io.github.q93503128.turnbound.progression.GachaService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
    public record Stage(double x, double y, double z, float cameraYaw) {}

    private static final Map<UUID, Active> ACTIVE = new LinkedHashMap<>();

    private static final class Active {
        final ServerLevel level;
        final List<GachaPresentationPlan.Reveal> reveals;
        int index;
        int slotTick;
        int ttl;
        UUID actorId;
        final Vec3 stage;
        final float actorYaw;

        Active(ServerLevel level, List<GachaPresentationPlan.Reveal> reveals, int pullCount, Vec3 stage, float actorYaw) {
            this.level = level;
            this.reveals = reveals;
            this.stage = stage;
            this.actorYaw = actorYaw;
            this.ttl = GachaPresentationTimeline.totalTicks(reveals, pullCount) + 80;
        }

        GachaPresentationPlan.Reveal current() {
            return index >= 0 && index < reveals.size() ? reveals.get(index) : null;
        }
    }

    private GachaPresentationActorService() {}

    public static Stage begin(ServerPlayer player, GachaService.BatchResult result) {
        if (player == null || result == null || !(player.level() instanceof ServerLevel level)) return null;
        finish(player);
        List<GachaPresentationPlan.Reveal> reveals = GachaPresentationPlan.reveals(result);
        if (reveals.isEmpty()) return null;
        DrabyelHubServiceRuntime.SummonStage fixedStage = DrabyelHubServiceRuntime.summonStage(player);
        Vec3 stage = fixedStage == null ? safeStagePosition(level, player) : fixedStage.position();
        if (stage == null) return null;
        float actorYaw = fixedStage == null ? faceYaw(stage, player.position()) : fixedStage.actorYaw();
        ACTIVE.put(player.getUUID(), new Active(level, reveals, result.pulls().size(), stage, actorYaw));
        float cameraYaw = wrapDegrees(actorYaw + 180.0F);
        return new Stage(stage.x, stage.y, stage.z, cameraYaw);
    }

    public static void tick(ServerPlayer player) {
        if (player == null) return;
        Active active = ACTIVE.get(player.getUUID());
        if (active == null) return;
        if (player.level() != active.level || --active.ttl <= 0) {
            finish(player);
            return;
        }

        GachaPresentationPlan.Reveal reveal = active.current();
        if (reveal == null) return;
        GachaPresentationTimeline.Timing timing = GachaPresentationTimeline.timing(reveal.stars(), reveal.newlyOwned());

        active.slotTick++;
        if (active.slotTick == 1) stageSignal(player, active, reveal, false);
        if (active.slotTick == timing.signalTicks()) stageSignal(player, active, reveal, true);
        if (active.slotTick == timing.revealTick()) {
            spawnCurrent(player, active);
        }
        if (active.slotTick == timing.nameTick()) {
            playRevealPose(player, active);
        }
        if (active.slotTick >= timing.slotTicks()) {
            removeActor(active);
            active.index++;
            active.slotTick = 0;
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
        GachaPresentationPlan.Reveal reveal = active.current();
        if (reveal == null) return;
        Vec3 position = active.stage;
        BattleActorEntity actor = PersonalPresentationIsolation.spawnPrivateActor(
                active.level, reveal.characterId(), position, active.actorYaw, player.getUUID());
        if (actor == null) return;
        actor.setCustomName(Component.literal(CanonicalData.definition(reveal.characterId()).name()));
        actor.setCustomNameVisible(false);
        actor.playReady();
        active.actorId = actor.getUUID();
        revealBurst(player, position, reveal, false);
    }

    private static void playRevealPose(ServerPlayer player, Active active) {
        GachaPresentationPlan.Reveal reveal = active.current();
        if (reveal == null || active.actorId == null) return;
        Entity entity = active.level.getEntity(active.actorId);
        if (!(entity instanceof BattleActorEntity actor)) return;
        actor.playVictory();
        revealBurst(player, actor.position().add(0.0D, Math.max(0.8D, actor.getBbHeight() * 0.5D), 0.0D), reveal, true);
    }

    private static void revealBurst(
            ServerPlayer player, Vec3 position, GachaPresentationPlan.Reveal reveal, boolean payoff) {
        int intensity = GachaPresentationTimeline.intensity(reveal.stars());
        int enchant = (payoff ? 4 : 3) + intensity * (payoff ? 2 : 1);
        PersonalPresentationIsolation.particles(
                (ServerLevel)player.level(), player, ParticleTypes.ENCHANT,
                position.x, position.y + 0.7D, position.z,
                enchant, 0.48D + intensity * 0.07D, 0.72D, 0.48D + intensity * 0.07D, 0.035D);
        if (reveal.stars() >= 4) {
            int rods = 2 + intensity + (payoff ? 2 : 0);
            PersonalPresentationIsolation.particles(
                    (ServerLevel)player.level(), player, ParticleTypes.END_ROD,
                    position.x, position.y + 0.85D, position.z,
                    rods, 0.36D, 0.58D, 0.36D, 0.025D);
        }
    }

    private static void stageSignal(
            ServerPlayer player, Active active, GachaPresentationPlan.Reveal reveal, boolean charged) {
        if (player == null || active == null || reveal == null) return;
        int intensity = GachaPresentationTimeline.intensity(reveal.stars());
        int points = (charged ? 7 : 5) + intensity;
        double radius = charged ? 1.95D : 1.55D;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            double x = active.stage.x + Math.cos(angle) * radius;
            double z = active.stage.z + Math.sin(angle) * radius;
            PersonalPresentationIsolation.particles(active.level, player, ParticleTypes.ENCHANT,
                    x, active.stage.y + 0.08D, z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
        if (charged && reveal.stars() >= 4) {
            PersonalPresentationIsolation.particles(active.level, player, ParticleTypes.END_ROD,
                    active.stage.x, active.stage.y + 0.18D, active.stage.z,
                    2 + intensity, 0.62D, 0.08D, 0.62D, 0.018D);
        }
    }

    private static void removeActor(Active active) {
        if (active.actorId == null) return;
        Entity entity = active.level.getEntity(active.actorId);
        if (entity != null) entity.discard();
        active.actorId = null;
    }

    private static Vec3 safeStagePosition(ServerLevel level, ServerPlayer player) {
        double radians = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        double[] distances = {9.0, 10.5, 8.0, 12.0, 13.5};
        double[] lateral = {0.0, 2.0, -2.0, 3.5, -3.5};

        for (double distance : distances) {
            for (double side : lateral) {
                Vec3 raw = player.position().add(forward.scale(distance)).add(right.scale(side));
                int x = (int)Math.floor(raw.x);
                int z = (int)Math.floor(raw.z);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                Vec3 candidate = new Vec3(x + 0.5, y, z + 0.5);
                if (Math.abs(candidate.y - player.getY()) > 4.0 || !open(level, candidate)) continue;
                if (!DrehmalAdaptiveRoutePlacement.sourceContentClear(level, x, y, z, 4.5D)) continue;
                return candidate;
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

    private static float wrapDegrees(float value) {
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) wrapped -= 360.0F;
        if (wrapped < -180.0F) wrapped += 360.0F;
        return wrapped;
    }
}

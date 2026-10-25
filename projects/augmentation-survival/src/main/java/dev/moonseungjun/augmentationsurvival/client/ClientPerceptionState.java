package dev.moonseungjun.augmentationsurvival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Client-only presentation cache for server-authoritative perception snapshots plus bounded ore scanning. */
public final class ClientPerceptionState {
    private static final int CYAN = 0xFF39E6D0;
    private static final int GOLD = 0xFFFFC857;
    private static final int RED = 0xFFFF4A4A;
    private static final int REVIVE = 0xFFE6F2FF;
    private static final AABB UNIT_BOX = new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    private static int oreSenseLevel;
    private static boolean enchantRefreshOwned;
    private static List<BlockPos> chestPositions = List.of();
    private static Set<Integer> dangerEntityIds = Set.of();
    private static List<BlockPos> reviveMarkerPositions = List.of();
    private static List<BlockPos> exposedOrePositions = List.of();
    private static int oreScanTicks;

    private ClientPerceptionState() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++oreScanTicks % 10 != 0) return;
            scanExposedOres(client);
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(ClientPerceptionState::renderOutlines);
    }

    public static void apply(
            int oreLevel,
            boolean enchantRefresh,
            List<Long> chests,
            List<Integer> dangerIds,
            List<Long> reviveMarkers
    ) {
        oreSenseLevel = Math.max(0, Math.min(3, oreLevel));
        enchantRefreshOwned = enchantRefresh;
        chestPositions = chests.stream().map(BlockPos::of).toList();
        dangerEntityIds = Set.copyOf(dangerIds);
        reviveMarkerPositions = reviveMarkers.stream().map(BlockPos::of).toList();
        if (oreSenseLevel <= 0) exposedOrePositions = List.of();
    }

    public static boolean isDangerEntity(int entityId) { return dangerEntityIds.contains(entityId); }
    public static int dangerOutlineColor() { return RED; }
    public static boolean enchantRefreshOwned() { return enchantRefreshOwned; }

    public static void clear() {
        oreSenseLevel = 0;
        enchantRefreshOwned = false;
        chestPositions = List.of();
        dangerEntityIds = Set.of();
        reviveMarkerPositions = List.of();
        exposedOrePositions = List.of();
    }

    private static void scanExposedOres(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null || client.player == null || oreSenseLevel <= 0) {
            exposedOrePositions = List.of();
            return;
        }
        int radius = 4 + oreSenseLevel * 2;
        BlockPos center = client.player.blockPosition();
        double radiusSquared = (double) radius * radius;
        List<BlockPos> found = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - radius; y <= center.getY() + radius; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    cursor.set(x, y, z);
                    if (center.distSqr(cursor) > radiusSquared) continue;
                    BlockState state = level.getBlockState(cursor);
                    if (!state.is(ConventionalBlockTags.ORES) || !isExposed(level, cursor)) continue;
                    found.add(cursor.immutable());
                }
            }
        }
        exposedOrePositions = List.copyOf(found);
    }

    private static boolean isExposed(ClientLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (level.getBlockState(neighbor).getCollisionShape(level, neighbor).isEmpty()) return true;
        }
        return false;
    }

    private static void renderOutlines(LevelRenderContext context) {
        Vec3 camera = context.levelState().cameraRenderState.pos;
        renderPositions(context, exposedOrePositions, camera, CYAN);
        renderPositions(context, chestPositions, camera, GOLD);
        renderPositions(context, reviveMarkerPositions, camera, REVIVE);
    }

    private static void renderPositions(LevelRenderContext context, List<BlockPos> positions, Vec3 camera, int color) {
        PoseStack stack = context.poseStack();
        for (BlockPos pos : positions) {
            stack.pushPose();
            stack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            context.submitNodeCollector().submitCustomGeometry(
                    stack,
                    RenderTypes.linesTranslucent(),
                    (pose, consumer) -> ShapeRenderer.renderLineBox(
                            pose,
                            consumer,
                            UNIT_BOX,
                            ((color >> 16) & 0xFF) / 255.0F,
                            ((color >> 8) & 0xFF) / 255.0F,
                            (color & 0xFF) / 255.0F,
                            ((color >>> 24) & 0xFF) / 255.0F
                    )
            );
            stack.popPose();
        }
    }
}

package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.presentation.DrabyelServiceActors;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Physical fast-travel keepers for discovered-world waystations outside New Drabyel.
 *
 * <p>The world map never performs travel. A player must be standing at a physical waystation keeper, or at
 * New Drabyel's existing stables, before the server accepts a travel command.</p>
 */
final class DrehmalWaystationRuntime {
    private static final String TAG = "turnbound_waystation_keeper";
    private static final String PREFIX = TAG + ":";
    private static final String VISUAL = "DRABYEL_STABLEMASTER";
    private static final double MATERIALIZE_RADIUS = 96.0D;
    private static final double INTERACTION_RADIUS = 5.5D;
    private static final Map<String, UUID> ACTORS = new HashMap<>();
    private static final Map<ServerLevel, Map<String, Vec3>> POSITIONS = new IdentityHashMap<>();
    private static ServerLevel boundLevel;
    private static long lastTick = Long.MIN_VALUE;

    private DrehmalWaystationRuntime() {}

    static void tick(ServerPlayer caller) {
        if (caller == null || !(caller.level() instanceof ServerLevel level)) return;
        bind(level);
        DrehmalFastTravelService.recordDiscovery(caller);

        long gameTime = level.getGameTime();
        if (lastTick == gameTime) return;
        lastTick = gameTime;

        for (var node : DrehmalFastTravelCatalog.nodes()) {
            if (isNewDrabyel(node)) continue;
            if (!demanded(level, node)) {
                discard(level, node.id());
                continue;
            }
            Vec3 pos = resolve(level, node);
            if (pos == null) {
                discard(level, node.id());
                continue;
            }
            ensureStructure(level, node, pos);
            BattleActorEntity actor = ensure(level, node, pos);
            if (actor != null) present(level, node, actor);
        }
    }

    static boolean interact(ServerPlayer player, Entity target) {
        if (player == null || target == null) return false;
        String nodeId = nodeId(target);
        if (nodeId == null) return false;
        var node = DrehmalFastTravelCatalog.node(nodeId);
        if (node == null) return false;
        Vec3 pos = position(player.level() instanceof ServerLevel level ? level : null, node);
        if (pos == null || player.position().distanceToSqr(pos) > INTERACTION_RADIUS * INTERACTION_RADIUS) return false;

        DrehmalFastTravelService.recordDiscovery(player);
        if (target instanceof BattleActorEntity actor) actor.playServiceGreeting();
        MetaNetwork.open(player, "TRAVEL");
        return true;
    }

    static boolean nearWaystation(ServerPlayer player) {
        if (player == null) return false;
        if (DrabyelHubServiceRuntime.nearFacility(player, "TRAVEL")) return true;
        if (!(player.level() instanceof ServerLevel level)) return false;
        for (var node : DrehmalFastTravelCatalog.nodes()) {
            if (isNewDrabyel(node)) continue;
            UUID actorId = ACTORS.get(node.id());
            Entity entity = actorId == null ? null : level.getEntity(actorId);
            if (!(entity instanceof BattleActorEntity actor) || !node.id().equals(nodeId(actor))) continue;
            if (player.position().distanceToSqr(actor.position()) <= INTERACTION_RADIUS * INTERACTION_RADIUS) return true;
        }
        return false;
    }

    static DrabyelInteractionPromptRules.Prompt prompt(ServerPlayer player) {
        if (player == null || BattleSessionManager.exists(player) || player.isSpectator()
                || !(player.level() instanceof ServerLevel level)) {
            return DrabyelInteractionPromptRules.none();
        }
        BattleActorEntity best = null;
        String bestNode = "";
        double bestDistance = Double.MAX_VALUE;
        double radiusSq = INTERACTION_RADIUS * INTERACTION_RADIUS;
        for (var node : DrehmalFastTravelCatalog.nodes()) {
            if (isNewDrabyel(node)) continue;
            UUID actorId = ACTORS.get(node.id());
            Entity entity = actorId == null ? null : level.getEntity(actorId);
            if (!(entity instanceof BattleActorEntity actor) || !node.id().equals(nodeId(actor))) continue;
            double distance = player.distanceToSqr(actor);
            if (distance <= radiusSq && distance < bestDistance) {
                best = actor;
                bestNode = node.id();
                bestDistance = distance;
            }
        }
        if (best == null) return DrabyelInteractionPromptRules.none();
        var node = DrehmalFastTravelCatalog.node(bestNode);
        return node == null
                ? DrabyelInteractionPromptRules.none()
                : new DrabyelInteractionPromptRules.Prompt(bestNode, "역참지기 · " + node.label(), "이동 지도 보기");
    }

    static boolean isKeeper(Entity entity) {
        return nodeId(entity) != null;
    }

    static void clear() {
        if (boundLevel != null) for (String id : List.copyOf(ACTORS.keySet())) discard(boundLevel, id);
        ACTORS.clear();
        POSITIONS.clear();
        boundLevel = null;
        lastTick = Long.MIN_VALUE;
    }

    private static void bind(ServerLevel level) {
        if (boundLevel == level) return;
        clear();
        boundLevel = level;
    }

    private static boolean isNewDrabyel(DrehmalFastTravelCatalog.Node node) {
        return node != null && "turnbound:travel/new_drabyel".equals(node.id());
    }

    private static boolean demanded(ServerLevel level, DrehmalFastTravelCatalog.Node node) {
        double radiusSq = MATERIALIZE_RADIUS * MATERIALIZE_RADIUS;
        for (ServerPlayer player : level.players()) {
            if (!ExternalWorldBootstrap.active(player) || BattleSessionManager.exists(player) || player.isSpectator()) continue;
            double dx = player.getX() - (node.mapX() + 0.5D);
            double dz = player.getZ() - (node.mapZ() + 0.5D);
            if (dx * dx + dz * dz <= radiusSq) return true;
        }
        return false;
    }

    private static Vec3 position(ServerLevel level, DrehmalFastTravelCatalog.Node node) {
        if (level == null || node == null) return null;
        return POSITIONS.getOrDefault(level, Map.of()).get(node.id());
    }

    private static Vec3 resolve(ServerLevel level, DrehmalFastTravelCatalog.Node node) {
        Map<String, Vec3> byNode = POSITIONS.computeIfAbsent(level, ignored -> new HashMap<>());
        if (byNode.containsKey(node.id())) return byNode.get(node.id());

        Candidate best = null;
        for (int[] offset : offsets(10)) {
            int x = node.mapX() + offset[0];
            int z = node.mapZ() + offset[1];
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - node.preferredY()) > 18) continue;
            if (!safeStanding(level, x, y, z) || !sourceClear(level, x, y, z)) continue;
            int distanceSq = offset[0] * offset[0] + offset[1] * offset[1];
            int roadBias = surfaceBias(level, new BlockPos(x, y - 1, z));
            Candidate candidate = new Candidate(new Vec3(x + 0.5D, y, z + 0.5D), roadBias * 10_000 + distanceSq);
            if (best == null || candidate.score() < best.score()) best = candidate;
        }
        if (best == null) return null;
        byNode.put(node.id(), best.pos());
        return best.pos();
    }

    private record Candidate(Vec3 pos, int score) {}

    private static BattleActorEntity ensure(ServerLevel level, DrehmalFastTravelCatalog.Node node, Vec3 pos) {
        UUID cached = ACTORS.get(node.id());
        Entity raw = cached == null ? null : level.getEntity(cached);
        if (raw instanceof BattleActorEntity actor && node.id().equals(nodeId(actor))) {
            configure(actor, node, pos);
            return actor;
        }
        if (cached != null) ACTORS.remove(node.id());

        AABB area = new AABB(pos.x - 3, pos.y - 2, pos.z - 3, pos.x + 3, pos.y + 4, pos.z + 3);
        for (BattleActorEntity actor : level.getEntitiesOfClass(BattleActorEntity.class, area)) {
            if (!node.id().equals(nodeId(actor))) continue;
            configure(actor, node, pos);
            ACTORS.put(node.id(), actor.getUUID());
            return actor;
        }

        BattleActorEntity actor = DrabyelServiceActors.spawn(level, VISUAL, pos, node.yaw());
        if (actor == null) return null;
        actor.addTag(TAG);
        actor.addTag(PREFIX + node.id());
        configure(actor, node, pos);
        ACTORS.put(node.id(), actor.getUUID());
        return actor;
    }

    private static void ensureStructure(ServerLevel level, DrehmalFastTravelCatalog.Node node, Vec3 pos) {
        int x = (int)Math.floor(pos.x);
        int y = (int)Math.floor(pos.y);
        int z = (int)Math.floor(pos.z);
        BlockPos origin = new BlockPos(x, y, z);
        int quarter = Math.floorMod(Math.round(node.yaw() / 90.0F), 4);

        for (int[] raw : new int[][]{{-2,1},{2,1},{-2,3},{2,3}}) {
            int[] o = rotate(raw[0], raw[1], quarter);
            for (int dy = 0; dy <= 2; dy++) {
                placeIfAir(level, origin.offset(o[0], dy, o[1]), Blocks.SPRUCE_FENCE);
            }
        }
        for (int dx = -2; dx <= 2; dx++) for (int dz = 1; dz <= 3; dz++) {
            int[] o = rotate(dx, dz, quarter);
            placeIfAir(level, origin.offset(o[0], 3, o[1]), Blocks.SPRUCE_SLAB);
        }
        int[] hay = rotate(1, 2, quarter);
        placeIfAir(level, origin.offset(hay[0], 0, hay[1]), Blocks.HAY_BLOCK);
        int[] hitch = rotate(-1, 2, quarter);
        placeIfAir(level, origin.offset(hitch[0], 0, hitch[1]), Blocks.OAK_FENCE);
        placeIfAir(level, origin.offset(hitch[0], 1, hitch[1]), Blocks.OAK_FENCE);
    }

    private static int[] rotate(int dx, int dz, int quarter) {
        return switch (quarter) {
            case 1 -> new int[]{-dz, dx};
            case 2 -> new int[]{-dx, -dz};
            case 3 -> new int[]{dz, -dx};
            default -> new int[]{dx, dz};
        };
    }

    private static void placeIfAir(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block block) {
        if (level.getBlockState(pos).isAir()) level.setBlockAndUpdate(pos, block.defaultBlockState());
    }

    private static void configure(BattleActorEntity actor, DrehmalFastTravelCatalog.Node node, Vec3 pos) {
        actor.setPos(pos.x, pos.y, pos.z);
        actor.setYRot(node.yaw());
        actor.setYHeadRot(node.yaw());
        actor.setYBodyRot(node.yaw());
        actor.setInvulnerable(true);
        actor.setFieldWalking(false);
        actor.setCustomName(Component.literal("역참지기 · " + node.label()).withStyle(ChatFormatting.AQUA));
        actor.setCustomNameVisible(false);
        actor.setGlowingTag(false);
        actor.addTag(TAG);
        actor.addTag(PREFIX + node.id());
    }

    private static void present(ServerLevel level, DrehmalFastTravelCatalog.Node node, BattleActorEntity actor) {
        ServerPlayer nearest = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            if (!ExternalWorldBootstrap.active(player) || BattleSessionManager.exists(player) || player.isSpectator()) continue;
            double d = actor.distanceToSqr(player);
            if (d < best) { best = d; nearest = player; }
        }
        actor.setCustomNameVisible(nearest != null && best <= 36.0D);
        if (nearest != null && best <= 64.0D) face(actor, nearest);
    }

    private static void face(BattleActorEntity actor, ServerPlayer player) {
        double dx = player.getX() - actor.getX();
        double dz = player.getZ() - actor.getZ();
        if (dx * dx + dz * dz <= 0.000001D) return;
        float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
        actor.setYRot(yaw);
        actor.setYHeadRot(yaw);
        actor.setYBodyRot(yaw);
    }

    private static String nodeId(Entity entity) {
        if (entity == null) return null;
        for (String tag : entity.entityTags()) if (tag.startsWith(PREFIX)) return tag.substring(PREFIX.length());
        return null;
    }

    private static void discard(ServerLevel level, String nodeId) {
        UUID id = ACTORS.remove(nodeId);
        if (id == null) return;
        Entity entity = level.getEntity(id);
        if (entity != null && nodeId.equals(nodeId(entity))) entity.discard();
    }

    private static boolean safeStanding(ServerLevel level, int x, int y, int z) {
        BlockPos feet = new BlockPos(x, y, z);
        BlockPos below = feet.below();
        if (level.getBlockState(below).getCollisionShape(level, below).isEmpty()) return false;
        if (!level.getFluidState(below).isEmpty()) return false;
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos p = feet.above(dy);
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty() || !level.getFluidState(p).isEmpty()) return false;
        }
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        return max - min <= 2;
    }

    private static boolean sourceClear(ServerLevel level, int x, int y, int z) {
        AABB box = new AABB(x - 2.5D, y - 1.0D, z - 2.5D, x + 3.5D, y + 3.5D, z + 3.5D);
        if (!level.getEntitiesOfClass(AbstractVillager.class, box).isEmpty()) return false;
        if (!level.getEntitiesOfClass(ItemFrame.class, box).isEmpty()) return false;
        if (!level.getEntitiesOfClass(ArmorStand.class, box).isEmpty()) return false;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = -1; dy <= 2; dy++) {
            if (level.getBlockEntity(new BlockPos(x + dx, y + dy, z + dz)) != null) return false;
        }
        return true;
    }

    private static int surfaceBias(ServerLevel level, BlockPos ground) {
        var state = level.getBlockState(ground);
        if (state.is(Blocks.DIRT_PATH)) return 0;
        if (state.is(Blocks.GRAVEL) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.ANDESITE) || state.is(Blocks.POLISHED_ANDESITE)) return 1;
        return 2;
    }

    private static List<int[]> offsets(int radius) {
        List<int[]> out = new ArrayList<>();
        for (int dz = -radius; dz <= radius; dz++) for (int dx = -radius; dx <= radius; dx++) {
            if (dx * dx + dz * dz <= radius * radius) out.add(new int[]{dx, dz});
        }
        out.sort(Comparator.comparingInt(v -> v[0] * v[0] + v[1] * v[1]));
        return out;
    }
}

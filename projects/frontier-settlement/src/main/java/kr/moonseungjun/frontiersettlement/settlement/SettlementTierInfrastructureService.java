package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.List;

/**
 * Visible tier infrastructure that stays behind the existing compact interaction model.
 *
 * Alpha.29 deliberately does not own transport navigation. Alpha.27's tagged road logistics remain
 * the single authority for outpost transport; tier growth is expressed through safe public works
 * and stronger loaded guard-post garrisons instead of a second UUID/name-based logistics backend.
 */
public final class SettlementTierInfrastructureService {
    private static final int PUBLIC_WORKS_INTERVAL_TICKS = 100;
    private static final int GARRISON_INTERVAL_TICKS = 200;
    private static final int FRONTIER_TOWN_LAMP_SPACING = 16;
    private static final int DOMAIN_LAMP_SPACING = 10;
    private static final int PUBLIC_WORKS_UPDATE = 3;

    private SettlementTierInfrastructureService() {}

    public static void tick(MinecraftServer server, SettlementData data) {
        SettlementTier tier = SettlementTier.current(data);
        if (tier.ordinal() < SettlementTier.FRONTIER_TOWN.ordinal()) return;

        ServerLevel level = server.overworld();
        int tick = server.getTickCount();
        if (tick % PUBLIC_WORKS_INTERVAL_TICKS == 0) maintainRoadPublicWorks(level, data, tier);
        if (tick % GARRISON_INTERVAL_TICKS == 0
                && data.buildingCount(BuildingType.BLACKSMITH) > 0) {
            maintainTierGarrison(level, data, tier);
        }
    }

    private static void maintainRoadPublicWorks(ServerLevel level, SettlementData data, SettlementTier tier) {
        int spacing = tier == SettlementTier.DOMAIN ? DOMAIN_LAMP_SPACING : FRONTIER_TOWN_LAMP_SPACING;
        int changed = 0;
        for (int roadIndex = 0; roadIndex < data.roads().size() && changed < 2; roadIndex++) {
            List<BlockPos> centers = data.roads().get(roadIndex).centers();
            if (centers.size() < 5) continue;
            int offset = Math.max(2, spacing / 2);
            for (int index = offset; index < centers.size() - 2 && changed < 2; index += spacing) {
                LampSite site = lampSite(level, data, centers, roadIndex, index);
                if (site == null) continue;
                BlockState post = level.getBlockState(site.post());
                if (!post.is(Blocks.OAK_FENCE)) {
                    if (!canPlacePublicWork(level, site.post())) continue;
                    level.setBlock(site.post(), Blocks.OAK_FENCE.defaultBlockState(), PUBLIC_WORKS_UPDATE);
                    changed++;
                    continue;
                }
                BlockState light = level.getBlockState(site.light());
                if (!light.is(Blocks.LANTERN) && canPlacePublicWork(level, site.light())) {
                    level.setBlock(site.light(), Blocks.LANTERN.defaultBlockState(), PUBLIC_WORKS_UPDATE);
                    changed++;
                }
            }
        }
    }

    private static LampSite lampSite(ServerLevel level, SettlementData data, List<BlockPos> centers,
                                     int roadIndex, int index) {
        BlockPos center = centers.get(index);
        int[] direction = directionAt(centers, index);
        if (direction[0] == 0 && direction[1] == 0) return null;
        int side = ((roadIndex + index) & 1) == 0 ? 2 : -2;
        LampSite preferred = candidate(level, data, center, direction, side);
        if (preferred != null) return preferred;
        return candidate(level, data, center, direction, -side);
    }

    private static LampSite candidate(ServerLevel level, SettlementData data, BlockPos center,
                                      int[] direction, int side) {
        BlockPos ground = center.offset(-direction[1] * side, 0, direction[0] * side);
        if (!level.hasChunkAt(ground)) return null;
        if (protectedXZ(data, ground)) return null;
        if (level.getBlockEntity(ground) != null) return null;
        BlockState groundState = level.getBlockState(ground);
        if (!groundState.getFluidState().isEmpty() || !isShoulderGround(groundState)) return null;
        BlockPos post = ground.above();
        BlockPos light = ground.above(2);
        if (!level.hasChunkAt(post) || !level.hasChunkAt(light)) return null;
        if ((!level.getBlockState(post).is(Blocks.OAK_FENCE) && !canPlacePublicWork(level, post))
                || (!level.getBlockState(light).is(Blocks.LANTERN) && !canPlacePublicWork(level, light))) {
            return null;
        }
        return new LampSite(post, light);
    }

    private static boolean canPlacePublicWork(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) != null) return false;
        BlockState current = level.getBlockState(pos);
        if (!current.getFluidState().isEmpty()) return false;
        return current.isAir() || current.canBeReplaced();
    }

    private static boolean isShoulderGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.ANDESITE) || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE)
                || state.is(Blocks.TUFF) || state.is(Blocks.GRAVEL) || state.is(Blocks.DIRT_PATH);
    }

    private static boolean protectedXZ(SettlementData data, BlockPos pos) {
        if (Math.abs(pos.getX() - data.centerPos().getX()) <= 7
                && Math.abs(pos.getZ() - data.centerPos().getZ()) <= 7) return true;
        for (BuildingRecord building : data.buildings()) if (building.protectsXZ(pos, 1)) return true;
        for (OutpostRecord outpost : data.outposts()) if (outpost.protectsXZ(pos, 1)) return true;
        return false;
    }

    private static int[] directionAt(List<BlockPos> centers, int index) {
        BlockPos from = centers.get(Math.max(0, index - 1));
        BlockPos to = centers.get(Math.min(centers.size() - 1, index + 1));
        int dx = Integer.signum(to.getX() - from.getX());
        int dz = Integer.signum(to.getZ() - from.getZ());
        if (Math.abs(dx) + Math.abs(dz) == 1) return new int[] {dx, dz};
        BlockPos current = centers.get(index);
        if (index + 1 < centers.size()) {
            BlockPos next = centers.get(index + 1);
            dx = Integer.signum(next.getX() - current.getX());
            dz = Integer.signum(next.getZ() - current.getZ());
            if (Math.abs(dx) + Math.abs(dz) == 1) return new int[] {dx, dz};
        }
        return new int[] {0, 0};
    }

    private static void maintainTierGarrison(ServerLevel level, SettlementData data, SettlementTier tier) {
        int reinforcementsPerPost = tier == SettlementTier.DOMAIN ? 2 : 1;
        for (BuildingRecord post : data.buildings()) {
            if (post.buildingType() != BuildingType.GUARD_POST) continue;
            BlockPos center = post.workCenter();
            if (!level.hasChunkAt(center)) continue;
            for (int index = 1; index <= reinforcementsPerPost; index++) {
                maintainReinforcement(level, post, index);
            }
        }
    }

    private static void maintainReinforcement(ServerLevel level, BuildingRecord post, int index) {
        BlockPos center = post.workCenter();
        String identity = reinforcementIdentity(post, index);
        AABB search = new AABB(center).inflate(24.0D, 10.0D, 24.0D);
        List<IronGolem> existing = level.getEntitiesOfClass(IronGolem.class, search,
                guard -> guard.getCustomName() != null && identity.equals(guard.getCustomName().getString()));
        if (!existing.isEmpty()) return;

        IronGolem guard = new IronGolem(EntityTypes.IRON_GOLEM, level);
        guard.setPos(center.getX() + 0.5D + index, center.getY(), center.getZ() + 0.5D);
        guard.setCustomName(Component.literal(identity));
        guard.setCustomNameVisible(false);
        guard.setPersistenceRequired();
        guard.setPlayerCreated(true);
        level.addFreshEntity(guard);
    }

    private static String reinforcementIdentity(BuildingRecord post, int index) {
        return "개척 수비대 [" + post.originX() + "," + post.originZ() + "] #" + index;
    }

    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (level != server.overworld()) return;
        SettlementData data = SettlementData.get(server);
        SettlementTier tier = SettlementTier.current(data);
        if (tier.ordinal() < SettlementTier.FRONTIER_TOWN.ordinal()) return;

        BlockPos pos = event.getPos();
        Block block = level.getBlockState(pos).getBlock();
        if (block != Blocks.OAK_FENCE && block != Blocks.LANTERN) return;
        int spacing = tier == SettlementTier.DOMAIN ? DOMAIN_LAMP_SPACING : FRONTIER_TOWN_LAMP_SPACING;
        for (int roadIndex = 0; roadIndex < data.roads().size(); roadIndex++) {
            List<BlockPos> centers = data.roads().get(roadIndex).centers();
            int offset = Math.max(2, spacing / 2);
            for (int index = offset; index < centers.size() - 2; index += spacing) {
                LampSite site = lampSite(level, data, centers, roadIndex, index);
                if (site == null) continue;
                if ((pos.equals(site.post()) && block == Blocks.OAK_FENCE)
                        || (pos.equals(site.light()) && block == Blocks.LANTERN)) {
                    event.setCanceled(true);
                    event.setNotifyClient(true);
                    return;
                }
            }
        }
    }

    private record LampSite(BlockPos post, BlockPos light) {}
}

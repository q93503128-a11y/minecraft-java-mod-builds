package kr.moonseungjun.survivalascension.harvesting;

import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import kr.moonseungjun.survivalascension.progress.SkillProgressData;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public final class IrrigationReplantService {
    private static final int REPLANT_BUDGET_PER_TICK = 64;
    private static final Deque<ReplantJob> JOBS = new ArrayDeque<>();

    private IrrigationReplantService() {}

    public static void scheduleIfEligible(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState harvestedState) {
        if (player.isCreative() || player.isSpectator()) return;
        if (SkillProgressData.get(player).level(player, SkillType.HARVESTING) < 30) return;
        if (!InfrastructureData.get(player).isComplete(InfrastructureProject.IRRIGATION_WORKS)) return;
        ReplantKind kind = ReplantKind.from(harvestedState);
        if (kind == null) return;
        JOBS.addLast(new ReplantJob(player.getUUID(), level.dimension(), pos.immutable(), kind));
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        int budget = REPLANT_BUDGET_PER_TICK;
        while (budget-- > 0 && !JOBS.isEmpty()) {
            ReplantJob job = JOBS.removeFirst();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(job.playerId);
            ServerLevel level = event.getServer().getLevel(job.dimension);
            if (player == null || level == null || player.isSpectator()) continue;
            if (!InfrastructureData.get(player).isComplete(InfrastructureProject.IRRIGATION_WORKS)) continue;
            tryReplant(player, level, job.pos, job.kind);
        }
    }

    private static void tryReplant(ServerPlayer player, ServerLevel level, BlockPos pos, ReplantKind kind) {
        if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)) return;
        BlockState current = level.getBlockState(pos);
        if (!current.canBeReplaced()) return;
        BlockState young = kind.youngState();
        if (!young.canSurvive(level, pos)) return;
        if (!hasSeed(player, kind.seed())) return;
        if (EventHooks.onBlockPlace(player, BlockSnapshot.create(level.dimension(), level, pos), Direction.UP)) return;
        if (!level.setBlockAndUpdate(pos, young)) return;
        consumeOne(player, kind.seed());
    }

    private static boolean hasSeed(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(item)) return true;
        }
        return false;
    }

    private static void consumeOne(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) continue;
            stack.shrink(1);
            player.getInventory().setChanged();
            return;
        }
    }

    private enum ReplantKind {
        WHEAT(Items.WHEAT_SEEDS, Blocks.WHEAT.defaultBlockState()),
        CARROT(Items.CARROT, Blocks.CARROTS.defaultBlockState()),
        POTATO(Items.POTATO, Blocks.POTATOES.defaultBlockState()),
        BEETROOT(Items.BEETROOT_SEEDS, Blocks.BEETROOTS.defaultBlockState()),
        NETHER_WART(Items.NETHER_WART, Blocks.NETHER_WART.defaultBlockState());

        private final Item seed;
        private final BlockState youngState;

        ReplantKind(Item seed, BlockState youngState) {
            this.seed = seed;
            this.youngState = youngState;
        }

        Item seed() { return seed; }
        BlockState youngState() { return youngState; }

        static ReplantKind from(BlockState state) {
            if (state.is(Blocks.WHEAT)) return WHEAT;
            if (state.is(Blocks.CARROTS)) return CARROT;
            if (state.is(Blocks.POTATOES)) return POTATO;
            if (state.is(Blocks.BEETROOTS)) return BEETROOT;
            if (state.is(Blocks.NETHER_WART)) return NETHER_WART;
            return null;
        }
    }

    private record ReplantJob(UUID playerId, ResourceKey<Level> dimension, BlockPos pos, ReplantKind kind) {}
}

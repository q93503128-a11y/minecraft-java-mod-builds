package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import kr.moonseungjun.titanbreak.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controlled terrain breach rules for explicit augmentation attacks.
 * Movement-only destruction remains disabled until Breach Mode is implemented.
 */
public final class BreachService {
    private static final int HARD_MAX_BLOCKS_PER_ACTION = 32;
    private static final int MAX_BREACH_POWER = 5;

    private BreachService() {}

    public static boolean canBreach(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
        return state.hasInstalled("blade_arm")
                || state.hasInstalled("high_frequency_blade_arm")
                || state.hasInstalled("power_arm")
                || state.hasInstalled("photon_emitter_arm");
    }

    /**
     * Adds body-wide Breach milestones to an attack's own Breach rating.
     * Powered Spine +5 and Bioalloy Skeleton +7 are global body-output upgrades,
     * while arm-specific milestones remain in their own attack implementations.
     */
    public static int effectiveBreachPower(ServerPlayer player, int attackBreachPower) {
        if (!(player.level() instanceof ServerLevel level) || attackBreachPower <= 0) return 0;
        TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
        int bonus = 0;

        TitanPlayerData.AugmentInstance poweredSpine = state.firstInstalledInstance("powered_spine");
        if (poweredSpine != null && poweredSpine.enhancement() >= 5) bonus++;

        TitanPlayerData.AugmentInstance skeleton = state.firstInstalledInstance("bioalloy_skeleton");
        if (skeleton != null && skeleton.enhancement() >= 7) bonus++;

        return Math.min(MAX_BREACH_POWER, Math.max(0, attackBreachPower + bonus));
    }

    public static int breachAtLook(ServerPlayer player, double range, int breachPower, double radius, int maxBlocks,
                                   boolean dropBlocks) {
        HitResult hit = player.pick(range, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) return 0;
        return breachArea(player, blockHit.getBlockPos(), breachPower, radius, maxBlocks, dropBlocks);
    }

    public static int breachLine(ServerPlayer player, double range, int breachPower, double radius, int maxBlocks) {
        if (!(player.level() instanceof ServerLevel level) || range <= 0.0D) return 0;
        int effectivePower = effectiveBreachPower(player, breachPower);
        if (effectivePower <= 0) return 0;
        int limit = Math.max(0, Math.min(HARD_MAX_BLOCKS_PER_ACTION, maxBlocks));
        if (limit == 0) return 0;

        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Set<BlockPos> visited = new HashSet<>();
        int broken = 0;
        for (double distance = 1.0D; distance <= range && broken < limit; distance += 0.65D) {
            BlockPos center = BlockPos.containing(start.add(direction.scale(distance)));
            int r = Math.max(0, (int) Math.ceil(radius));
            for (BlockPos candidate : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
                if (broken >= limit) break;
                BlockPos pos = candidate.immutable();
                if (!visited.add(pos)) continue;
                if (Vec3.atCenterOf(pos).distanceTo(Vec3.atCenterOf(center)) > radius + 0.75D) continue;
                if (tryBreak(level, player, pos, effectivePower, false)) broken++;
            }
        }
        return broken;
    }

    public static int breachArea(ServerPlayer player, BlockPos center, int breachPower, double radius, int maxBlocks,
                                 boolean dropBlocks) {
        if (!(player.level() instanceof ServerLevel level)) return 0;
        int effectivePower = effectiveBreachPower(player, breachPower);
        if (effectivePower <= 0) return 0;
        int limit = Math.max(0, Math.min(HARD_MAX_BLOCKS_PER_ACTION, maxBlocks));
        if (limit == 0) return 0;

        int r = Math.max(0, (int) Math.ceil(radius));
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            BlockPos immutable = pos.immutable();
            if (immutable.distSqr(center) <= radius * radius + 0.75D) candidates.add(immutable);
        }
        candidates.sort(Comparator.comparingDouble(center::distSqr));

        int broken = 0;
        for (BlockPos pos : candidates) {
            if (broken >= limit) break;
            if (tryBreak(level, player, pos, effectivePower, dropBlocks)) broken++;
        }
        return broken;
    }

    public static int requiredPower(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || isProtected(level, pos, state)) return Integer.MAX_VALUE;
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F) return Integer.MAX_VALUE;

        if (state.is(BlockTags.LEAVES) || hardness <= 0.5F) return 1;
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.MINEABLE_WITH_AXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return hardness > 4.0F ? 3 : 2;
        }
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            if (hardness >= 8.0F) return 5;
            if (hardness >= 4.0F) return 4;
            return 3;
        }
        if (hardness <= 1.0F) return 1;
        if (hardness <= 2.5F) return 2;
        if (hardness <= 5.0F) return 3;
        if (hardness <= 12.0F) return 4;
        return 5;
    }

    private static boolean tryBreak(ServerLevel level, ServerPlayer player, BlockPos pos, int breachPower,
                                    boolean dropBlocks) {
        BlockState state = level.getBlockState(pos);
        if (requiredPower(level, pos, state) > breachPower) return false;
        return level.destroyBlock(pos, dropBlocks, player);
    }

    private static boolean isProtected(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) != null) return true;
        Block block = state.getBlock();
        return block == ModBlocks.FABRICATOR_I.get()
                || block == ModBlocks.FABRICATOR_II.get()
                || block == ModBlocks.FABRICATOR_III.get()
                || block == ModBlocks.SURGICAL_BAY.get()
                || block == ModBlocks.IMPLANT_VAULT.get();
    }
}

package kr.moonseungjun.survivalascension.production;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * 0.40 physical breach pressure for bastion defense.
 *
 * Only the unique fourth bastion wave may damage the same real fortification blocks that qualified
 * the structure. The service never attacks arbitrary terrain, never force-loads chunks and obeys
 * mobGriefing, NeoForge entity-destroy hooks and the owning player's mayInteract protection gate.
 */
public final class OutpostSiegeBreachService {
    private static final String SIEGE_OWNER_KEY = "survivalascension_outpost_siege_owner";
    private static final String SIEGE_WAVE_KEY = "survivalascension_outpost_siege_wave";
    private static final String BREAK_READY_KEY = "survivalascension_outpost_breach_ready";
    private static final String OWNER_WARNING_READY_KEY = "survivalascension_outpost_breach_warning_ready";

    private static final int BASTION_ONLY_WAVE = 4;
    private static final int OWNER_SCAN_RADIUS = 96;
    private static final int BREAK_SEARCH_HORIZONTAL = 2;
    private static final int BREAK_SEARCH_DOWN = 1;
    private static final int BREAK_SEARCH_UP = 2;
    private static final int ANCHOR_APPROACH_RADIUS = OutpostFortificationService.OUTER_RADIUS + 5;
    private static final int RAVAGER_BREAK_COOLDOWN = 30;
    private static final int VINDICATOR_BREAK_COOLDOWN = 60;
    private static int ticker;

    private OutpostSiegeBreachService() {}

    public static void onServerTick(ServerTickEvent.Pre event) {
        if (++ticker < 5) return;
        ticker = 0;

        for (ServerPlayer owner : event.getServer().getPlayerList().getPlayers()) {
            if (!owner.isAlive() || owner.isCreative() || owner.isSpectator() || !OutpostSiegeSystem.isActive(owner)) continue;
            if (!(owner.level() instanceof ServerLevel level)) continue;

            String ownerId = owner.getUUID().toString();
            List<Mob> breakers = level.getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(OWNER_SCAN_RADIUS), mob -> {
                if (!mob.isAlive() || !isBreaker(mob)) return false;
                var data = mob.getPersistentData();
                return ownerId.equals(data.getStringOr(SIEGE_OWNER_KEY, ""))
                        && data.getIntOr(SIEGE_WAVE_KEY, 0) >= BASTION_ONLY_WAVE;
            });
            for (Mob mob : breakers) tryBreak(owner, level, mob);
        }
    }

    private static void tryBreak(ServerPlayer owner, ServerLevel level, Mob mob) {
        long now = level.getGameTime();
        var data = mob.getPersistentData();
        if (now < data.getLongOr(BREAK_READY_KEY, 0L)) return;

        if (!EventHooks.canEntityGrief(level, mob)) {
            data.putLong(BREAK_READY_KEY, now + 20L);
            return;
        }

        BreachTarget target = findTarget(owner, level, mob);
        if (target == null) {
            data.putLong(BREAK_READY_KEY, now + 10L);
            return;
        }

        BlockState state = level.getBlockState(target.pos());
        if (!isFortificationBlock(state)
                || level.getBlockEntity(target.pos()) != null
                || !level.mayInteract(owner, target.pos())
                || !state.canEntityDestroy(level, target.pos(), mob)
                || !EventHooks.onEntityDestroyBlock(mob, target.pos(), state)) {
            data.putLong(BREAK_READY_KEY, now + 20L);
            return;
        }

        if (!level.destroyBlock(target.pos(), true, mob)) {
            data.putLong(BREAK_READY_KEY, now + 20L);
            return;
        }

        data.putLong(BREAK_READY_KEY, now + breakCooldown(mob));
        var ownerData = owner.getPersistentData();
        if (now >= ownerData.getLongOr(OWNER_WARNING_READY_KEY, 0L)) {
            ownerData.putLong(OWNER_WARNING_READY_KEY, now + 40L);
            owner.sendSystemMessage(Component.literal("§4[공성 파괴] §f최종 공세의 파괴자가 실제 방어진을 부수고 있습니다. §7드롭된 자재로 틈을 메우세요."), true);
        }
    }

    private static BreachTarget findTarget(ServerPlayer owner, ServerLevel level, Mob mob) {
        BreachTarget best = null;
        for (OutpostData.OutpostEntry outpost : OutpostData.get(owner).outposts(owner)) {
            if (!outpost.dimension().equals(level.dimension().toString())) continue;
            BlockPos anchor = outpost.pos();
            if (anchor.distSqr(owner.blockPosition()) > OutpostSiegeSystem.DEFENSE_RADIUS * OutpostSiegeSystem.DEFENSE_RADIUS) continue;
            if (horizontalDistanceSqr(mob.blockPosition(), anchor) > ANCHOR_APPROACH_RADIUS * ANCHOR_APPROACH_RADIUS) continue;
            if (!OutpostService.isRecoveryOperational(owner, level, outpost.dimension(), anchor)) continue;

            BreachTarget candidate = findTargetNearMob(level, mob, anchor);
            if (candidate != null && (best == null || candidate.distanceSqr() < best.distanceSqr())) best = candidate;
        }
        return best;
    }

    private static BreachTarget findTargetNearMob(ServerLevel level, Mob mob, BlockPos anchor) {
        BlockPos center = mob.blockPosition();
        BreachTarget best = null;
        double towardAnchorX = anchor.getX() + 0.5D - mob.getX();
        double towardAnchorZ = anchor.getZ() + 0.5D - mob.getZ();

        for (int dx = -BREAK_SEARCH_HORIZONTAL; dx <= BREAK_SEARCH_HORIZONTAL; dx++) {
            for (int dz = -BREAK_SEARCH_HORIZONTAL; dz <= BREAK_SEARCH_HORIZONTAL; dz++) {
                for (int dy = -BREAK_SEARCH_DOWN; dy <= BREAK_SEARCH_UP; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!level.hasChunkAt(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (!isFortificationBlock(state) || level.getBlockEntity(pos) != null) continue;

                    int anchorDx = pos.getX() - anchor.getX();
                    int anchorDz = pos.getZ() - anchor.getZ();
                    int anchorHorizontalSq = anchorDx * anchorDx + anchorDz * anchorDz;
                    int innerSq = OutpostFortificationService.INNER_RADIUS * OutpostFortificationService.INNER_RADIUS;
                    int outerSq = OutpostFortificationService.OUTER_RADIUS * OutpostFortificationService.OUTER_RADIUS;
                    if (anchorHorizontalSq < innerSq || anchorHorizontalSq > outerSq) continue;

                    double toBlockX = pos.getX() + 0.5D - mob.getX();
                    double toBlockZ = pos.getZ() + 0.5D - mob.getZ();
                    if (toBlockX * towardAnchorX + toBlockZ * towardAnchorZ <= 0.0D) continue;

                    double distanceSqr = mob.distanceToSqr(pos.getCenter());
                    if (best == null || distanceSqr < best.distanceSqr()) best = new BreachTarget(pos.immutable(), distanceSqr);
                }
            }
        }
        return best;
    }

    private static int horizontalDistanceSqr(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private static boolean isBreaker(Mob mob) {
        return mob.getType() == EntityType.RAVAGER || mob.getType() == EntityType.VINDICATOR;
    }

    private static int breakCooldown(Mob mob) {
        return mob.getType() == EntityType.RAVAGER ? RAVAGER_BREAK_COOLDOWN : VINDICATOR_BREAK_COOLDOWN;
    }

    private static boolean isFortificationBlock(BlockState state) {
        return state.is(BlockTags.WALLS) || state.is(Blocks.IRON_BARS) || state.is(Blocks.NETHER_BRICK_FENCE);
    }

    private record BreachTarget(BlockPos pos, double distanceSqr) {}
}

package kr.moonseungjun.anomalousclasses.state;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Preserves vanilla inventory, tools, harvest logic and container interactions
 * while possessing one original Mob. The vanilla network handlers still use
 * the riding player's position for their early reach checks, so grant a small
 * transient buffer, but authorize every actual hit again from the host eye.
 */
public final class ParasiteHostReachService {
    private static final ResourceLocation BLOCK_BUFFER =
            ResourceLocation.fromNamespaceAndPath("anomalousclasses", "host_block_reach_buffer");
    private static final ResourceLocation ENTITY_BUFFER =
            ResourceLocation.fromNamespaceAndPath("anomalousclasses", "host_entity_reach_buffer");
    private static final double BUFFER = 2.75;
    private ParasiteHostReachService() {}

    /** Only the current server-owned original host may confer interaction range. */
    public static void refresh(ServerPlayer player, Mob host) {
        boolean possessed = player != null && host != null && host.isAlive()
                && ParasiteService.activeHost(player) == host && !player.isSpectator();
        change(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_BUFFER, possessed);
        change(player, Attributes.ENTITY_INTERACTION_RANGE, ENTITY_BUFFER, possessed);
    }

    public static void reset(ServerPlayer player) {
        refresh(player, null);
    }

    private static void change(ServerPlayer player,
                               net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                               ResourceLocation id, boolean add) {
        if (player == null) return;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (add) {
            if (instance.getModifier(id) == null)
                instance.addOrUpdateTransientModifier(new AttributeModifier(
                        id, BUFFER, AttributeModifier.Operation.ADD_VALUE));
        } else {
            instance.removeModifier(id);
        }
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Mob host = ParasiteService.activeHost(player);
        if (host != null && !canReachBlock(player, host, event.getPos()))
            event.setCanceled(true);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Mob host = ParasiteService.activeHost(player);
        if (host != null && !canReachBlock(player, host, event.getPos()))
            event.setCanceled(true);
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Mob host = ParasiteService.activeHost(player);
        if (host != null && !canReachEntity(player, host, event.getTarget()))
            event.setCanceled(true);
    }

    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Mob host = ParasiteService.activeHost(player);
        if (host != null && !canReachEntity(player, host, event.getTarget()))
            event.setCanceled(true);
    }

    /** Also apply at block-harvest commit; never trust an earlier dig-start. */
    public static boolean canReachBlock(ServerPlayer player, Mob host, BlockPos target) {
        if (player == null || host == null || target == null
                || !player.isAlive() || host.level() != player.level()
                || ParasiteService.activeHost(player) != host) return false;
        Vec3 eye = host.getEyePosition();
        Vec3 center = Vec3.atCenterOf(target);
        double max = blockLimit(player.blockInteractionRange() - BUFFER);
        if (eye.distanceToSqr(center) > (max + .8) * (max + .8)
                || !aimedToward(eye, center, ParasiteService.viewDirection(player))) return false;
        BlockHitResult hit = player.serverLevel().clip(new ClipContext(
                eye, center, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, host));
        // Ray casting to the center allows partial-height blocks to be used
        // without letting the player mine through the first obstructing block.
        if (hit.getType() == HitResult.Type.BLOCK)
            return hit.getBlockPos().equals(target);
        // Empty collision/outline shapes (e.g. replaceable plants) are still
        // subject to a solid-block obstruction check and the same aim/range.
        BlockHitResult obstruction = player.serverLevel().clip(new ClipContext(
                eye, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host));
        return obstruction.getType() == HitResult.Type.MISS
                || obstruction.getBlockPos().equals(target);
    }

    public static boolean canReachEntity(ServerPlayer player, Mob host, Entity target) {
        if (player == null || host == null || target == null || target == host
                || target == player || !player.isAlive() || !target.isAlive()
                || host.level() != target.level() || player.level() != host.level()
                || ParasiteService.activeHost(player) != host) return false;
        Vec3 eye = host.getEyePosition();
        Vec3 center = target.getBoundingBox().getCenter();
        double max = entityLimit(player.entityInteractionRange() - BUFFER);
        Vec3 closest = new Vec3(
                Math.max(target.getBoundingBox().minX, Math.min(eye.x, target.getBoundingBox().maxX)),
                Math.max(target.getBoundingBox().minY, Math.min(eye.y, target.getBoundingBox().maxY)),
                Math.max(target.getBoundingBox().minZ, Math.min(eye.z, target.getBoundingBox().maxZ)));
        if (eye.distanceToSqr(closest) > max * max
                || !aimedToward(eye, center, ParasiteService.viewDirection(player))) return false;
        BlockHitResult wall = player.serverLevel().clip(new ClipContext(
                eye, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host));
        return wall.getType() == HitResult.Type.MISS
                || eye.distanceToSqr(wall.getLocation()) >= eye.distanceToSqr(closest);
    }

    /** Bounded game reach before the server-side mount offset safety buffer. */
    public static double blockLimit(double base) {
        return Math.max(3.0, Math.min(5.5, base));
    }

    public static double entityLimit(double base) {
        return Math.max(2.5, Math.min(5.0, base));
    }

    /** Allow small network-angle lag, never a side/backward reach. */
    public static boolean aimedToward(Vec3 start, Vec3 target, Vec3 look) {
        Vec3 delta = target.subtract(start);
        if (delta.lengthSqr() < .00001) return true;
        return look.lengthSqr() > .00001
                && delta.normalize().dot(look.normalize()) >= .86;
    }
}

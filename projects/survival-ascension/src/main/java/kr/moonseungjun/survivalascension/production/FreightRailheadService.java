package kr.moonseungjun.survivalascension.production;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 0.43 physical freight railhead validation.
 *
 * Freight still moves only through a real Chest Minecart. This service merely verifies that the
 * exact active outpost endpoint has a small, already-loaded physical loading yard around its real
 * Barrel anchor. It stores no route, does not drive carts and never loads chunks.
 */
public final class FreightRailheadService {
    public static final int RAILHEAD_RADIUS = 6;
    public static final int MIN_RAIL_BLOCKS = 6;
    public static final int MIN_POWERED_RAILS = 1;
    public static final int MIN_HOPPERS = 1;
    public static final int MIN_CONTROLS = 1;

    private static final int POWERED_NEAR_CART_SQ = 9;
    private static final int HOPPER_NEAR_CART_SQ = 9;
    private static final int CONTROL_NEAR_CART_SQ = 16;

    private FreightRailheadService() {}

    public static boolean validate(ServerPlayer player, OutpostData.OutpostEntry outpost, MinecartChest cart) {
        Inspection inspection = inspect(player, outpost, cart);
        if (inspection.complete()) return true;

        player.sendSystemMessage(Component.literal("§6[화물 하역장] §f이 전초의 실제 하역장이 아직 준비되지 않았습니다."));
        sendInspection(player, inspection);
        player.sendSystemMessage(Component.literal("§7전초 통 앵커 반경6에 레일6+ · 동력레일1+ · 호퍼1+ · 레버/레드스톤 블록1+을 두고, 수레를 그 하역장 레일에 올리세요."));
        return false;
    }

    public static void sendStatus(ServerPlayer player, OutpostData.OutpostEntry outpost, MinecartChest cart) {
        Inspection inspection = inspect(player, outpost, cart);
        player.sendSystemMessage(Component.literal("§6[화물 하역장] " + (inspection.complete() ? "§a가동 가능" : "§e미완성")
                + " §7· 전초 통 반경" + RAILHEAD_RADIUS));
        sendInspection(player, inspection);
    }

    private static void sendInspection(ServerPlayer player, Inspection inspection) {
        player.sendSystemMessage(Component.literal("  §7- 레일 §f" + inspection.rails() + "§7/§f" + MIN_RAIL_BLOCKS
                + " · 동력레일 §f" + inspection.poweredRails() + "§7/§f" + MIN_POWERED_RAILS
                + " · 호퍼 §f" + inspection.hoppers() + "§7/§f" + MIN_HOPPERS
                + " · 제어 §f" + inspection.controls() + "§7/§f" + MIN_CONTROLS));
        if (!inspection.cartRailInside()) {
            player.sendSystemMessage(Component.literal("  §7- §c수레가 전초 하역장 반경 안의 로딩된 레일 위에 있지 않습니다."));
        } else {
            player.sendSystemMessage(Component.literal("  §7- 수레 주변 연결: 동력레일 " + yesNo(inspection.poweredNearCart())
                    + " §7· 호퍼 " + yesNo(inspection.hopperNearCart())
                    + " §7· 제어 " + yesNo(inspection.controlNearCart())));
        }
    }

    private static String yesNo(boolean value) {
        return value ? "§aOK" : "§e필요";
    }

    private static Inspection inspect(ServerPlayer player, OutpostData.OutpostEntry outpost, MinecartChest cart) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos anchor = outpost.pos();
        BlockPos cartRail = cartRailPosition(level, cart);
        boolean cartRailInside = cartRail != null && withinRadius(anchor, cartRail, RAILHEAD_RADIUS);
        int rails = 0;
        int poweredRails = 0;
        int hoppers = 0;
        int controls = 0;
        boolean poweredNearCart = false;
        boolean hopperNearCart = false;
        boolean controlNearCart = false;

        int radiusSq = RAILHEAD_RADIUS * RAILHEAD_RADIUS;
        for (int dx = -RAILHEAD_RADIUS; dx <= RAILHEAD_RADIUS; dx++) {
            for (int dy = -RAILHEAD_RADIUS; dy <= RAILHEAD_RADIUS; dy++) {
                for (int dz = -RAILHEAD_RADIUS; dz <= RAILHEAD_RADIUS; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSq) continue;
                    BlockPos pos = anchor.offset(dx, dy, dz);
                    if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (state.is(BlockTags.RAILS)) rails++;
                    if (state.is(Blocks.POWERED_RAIL)) {
                        poweredRails++;
                        if (cartRailInside && distanceSq(pos, cartRail) <= POWERED_NEAR_CART_SQ) poweredNearCart = true;
                    }
                    if (state.is(Blocks.HOPPER)) {
                        hoppers++;
                        if (cartRailInside && distanceSq(pos, cartRail) <= HOPPER_NEAR_CART_SQ) hopperNearCart = true;
                    }
                    if (state.is(Blocks.LEVER) || state.is(Blocks.REDSTONE_BLOCK)) {
                        controls++;
                        if (cartRailInside && distanceSq(pos, cartRail) <= CONTROL_NEAR_CART_SQ) controlNearCart = true;
                    }
                }
            }
        }
        return new Inspection(rails, poweredRails, hoppers, controls, cartRailInside,
                poweredNearCart, hopperNearCart, controlNearCart);
    }

    private static BlockPos cartRailPosition(ServerLevel level, MinecartChest cart) {
        BlockPos pos = cart.blockPosition();
        if (level.hasChunkAt(pos) && level.getBlockState(pos).is(BlockTags.RAILS)) return pos.immutable();
        BlockPos below = pos.below();
        if (level.hasChunkAt(below) && level.getBlockState(below).is(BlockTags.RAILS)) return below.immutable();
        return null;
    }

    private static boolean withinRadius(BlockPos a, BlockPos b, int radius) {
        return distanceSq(a, b) <= radius * radius;
    }

    private static int distanceSq(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        int dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private record Inspection(int rails, int poweredRails, int hoppers, int controls,
                              boolean cartRailInside, boolean poweredNearCart,
                              boolean hopperNearCart, boolean controlNearCart) {
        boolean complete() {
            return cartRailInside
                    && rails >= MIN_RAIL_BLOCKS
                    && poweredRails >= MIN_POWERED_RAILS
                    && hoppers >= MIN_HOPPERS
                    && controls >= MIN_CONTROLS
                    && poweredNearCart
                    && hopperNearCart
                    && controlNearCart;
        }
    }
}

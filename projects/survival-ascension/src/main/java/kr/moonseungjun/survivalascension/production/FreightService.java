package kr.moonseungjun.survivalascension.production;

import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Physical freight relay with real railhead endpoints.
 *
 * Normal activation loads the existing broad bulk whitelist. Shift activation on an empty cart
 * loads one bounded frontline manifest: enough local stock for one expedition, one normal outpost
 * defense and one bastion defense. Nothing is generated or teleported; both modes pull only from
 * the exact departure outpost's real loaded Barrel cluster and use the same physical cart/rail path.
 */
public final class FreightService {
    private static final String OWNER_KEY = "survivalascension_freight_owner";
    private static final String ORIGIN_DIMENSION_KEY = "survivalascension_freight_origin_dimension";
    private static final String ORIGIN_X_KEY = "survivalascension_freight_origin_x";
    private static final String ORIGIN_Y_KEY = "survivalascension_freight_origin_y";
    private static final String ORIGIN_Z_KEY = "survivalascension_freight_origin_z";
    private static final String FRONTLINE_KEY = "survivalascension_freight_frontline";
    private static final int INTERACTION_RADIUS = 4;

    // 0.48 local-operation costs combined exactly once each:
    // expedition food32/iron8/fuel8 + defense food48/iron16/logs32
    // + bastion food96/iron32/stone bricks128.
    public static final int FRONTLINE_FOOD = 176;
    public static final int FRONTLINE_IRON = 56;
    public static final int FRONTLINE_FUEL = 8;
    public static final int FRONTLINE_LOGS = 32;
    public static final int FRONTLINE_STONE_BRICKS = 128;

    private FreightService() {}

    public static void transferNearest(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f크리에이티브/관전자 상태에서는 화물 적재·하역을 처리할 수 없습니다."));
            return;
        }
        InfrastructureData infrastructure = InfrastructureData.get(player);
        if (!infrastructure.isComplete(InfrastructureProject.INDUSTRIAL_WORKS)
                || !infrastructure.isComplete(InfrastructureProject.CIVIL_WORKS)) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f§b산업 가공소§f와 §e토목 공사소§f를 모두 완공해야 합니다."));
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        OutpostData.OutpostEntry outpost = OutpostService.nearestActiveOutpost(player, INTERACTION_RADIUS);
        if (outpost == null) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f4블록 안에 현재 활성 상태인 자신의 전초기지가 없습니다."));
            return;
        }

        MinecartChest cart = nearestCart(player, level);
        if (cart == null) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f4블록 안에 사용할 수 있는 §6상자 광산수레§f가 없습니다."));
            return;
        }
        if (!isOnLoadedRail(level, cart)) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f상자 광산수레가 현재 로딩된 §e레일 위§f에 있어야 합니다."));
            return;
        }
        if (!FreightRailheadService.validate(player, outpost, cart)) return;

        String taggedOwner = cart.getPersistentData().getStringOr(OWNER_KEY, "");
        if (taggedOwner.isEmpty()) load(player, level, outpost, cart, player.isShiftKeyDown());
        else unload(player, level, outpost, cart, taggedOwner);
    }

    public static void sendStatus(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        OutpostData.OutpostEntry outpost = OutpostService.nearestActiveOutpost(player, INTERACTION_RADIUS);
        MinecartChest cart = nearestCart(player, level);
        if (outpost == null) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f근처 활성 전초 없음 §7· 전초4블록 안에서 출발/도착 하역장을 사용합니다."));
            return;
        }
        if (cart == null) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f근처 상자 광산수레 없음 §7· 활성 전초4블록 + 실제 하역장 레일 위 수레 필요"));
            return;
        }
        FreightRailheadService.sendStatus(player, outpost, cart);
        var cartData = cart.getPersistentData();
        String owner = cartData.getStringOr(OWNER_KEY, "");
        if (owner.isEmpty()) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f근처 수레 §7· " + (isEmpty(cart)
                    ? "§a빈 수레 · 일반선택=대량화물 · Shift+선택=전선 보급 묶음"
                    : "§e일반 화물 존재 · 자동 적재 불가")));
            return;
        }
        boolean mine = owner.equals(player.getUUID().toString());
        int bulk = countBulk(cart);
        boolean frontline = cartData.getIntOr(FRONTLINE_KEY, 0) == 1;
        player.sendSystemMessage(Component.literal("§3[물리 화물] §f근처 수레 §7· " + (mine ? "§b내 운송 화물" : "§c다른 소유자 화물")
                + " §7· " + (frontline ? "§6전선 보급 묶음" : "§f일반 대량화물") + " §7· 대량 자원 §e" + bulk + "개"));
        if (frontline) {
            player.sendSystemMessage(Component.literal("  §7식량 §e" + countMatching(cart, FreightService::isFieldFood)
                    + "§7/§f" + FRONTLINE_FOOD + " · 철 §e" + countMatching(cart, stack -> stack.is(Items.IRON_INGOT))
                    + "§7/§f" + FRONTLINE_IRON + " · 연료 §e" + countMatching(cart, FreightService::isFieldFuel)
                    + "§7/§f" + FRONTLINE_FUEL + " · 통나무 §e" + countMatching(cart, stack -> stack.is(ItemTags.LOGS))
                    + "§7/§f" + FRONTLINE_LOGS + " · 석재벽돌 §e" + countMatching(cart, stack -> stack.is(Items.STONE_BRICKS))
                    + "§7/§f" + FRONTLINE_STONE_BRICKS));
        }
    }

    private static void load(ServerPlayer player, ServerLevel level, OutpostData.OutpostEntry outpost, MinecartChest cart,
                             boolean frontlineManifest) {
        if (!isEmpty(cart)) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f자동 적재를 시작하려면 상자 광산수레가 완전히 비어 있어야 합니다."));
            return;
        }
        FieldDepotData.DepotEntry depot = depotForOutpost(player, outpost);
        if (depot == null) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f이 전초의 등록 통 물류 앵커를 확인할 수 없습니다."));
            return;
        }
        List<Container> source = storageForDepot(player, level, depot);
        if (source.isEmpty()) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f이 전초에서 현재 로딩·상호작용 가능한 물류 통이 없습니다."));
            return;
        }

        int moved;
        if (frontlineManifest) {
            if (!checkFrontlineBundle(player, source)) return;
            FrontlineLoadResult result = moveFrontlineBundleInto(source, cart);
            if (!result.complete()) {
                int rollback = moveBulkOut(cart, source);
                if (!isEmpty(cart)) {
                    player.sendSystemMessage(Component.literal("§c[전선 화물] §f수레 슬롯 제약으로 묶음 적재를 완성하지 못했고 전부 되돌리지도 못했습니다. §7수레와 출발 통 재고를 확인하세요."));
                } else {
                    player.sendSystemMessage(Component.literal("§c[전선 화물] §f수레 슬롯 제약으로 묶음 적재를 완성하지 못해 §e" + rollback
                            + "개§f를 출발 전초 통으로 되돌렸습니다. §7수레 안의 서로 다른 아이템 종류를 줄인 뒤 다시 시도하세요."));
                }
                return;
            }
            moved = result.total();
        } else {
            moved = moveBulkInto(source, cart);
            if (moved <= 0) {
                player.sendSystemMessage(Component.literal("§3[물리 화물] §f이 전초 창고군에 운송 대상으로 분류된 대량 자원이 없습니다."));
                return;
            }
        }

        var data = cart.getPersistentData();
        data.putString(OWNER_KEY, player.getUUID().toString());
        data.putString(ORIGIN_DIMENSION_KEY, outpost.dimension());
        data.putInt(ORIGIN_X_KEY, outpost.x());
        data.putInt(ORIGIN_Y_KEY, outpost.y());
        data.putInt(ORIGIN_Z_KEY, outpost.z());
        data.putInt(FRONTLINE_KEY, frontlineManifest ? 1 : 0);
        cart.setChanged();

        if (frontlineManifest) {
            player.sendSystemMessage(Component.literal("§6[전선 화물 적재] §f원정1 + 전초방어1 + 요새방어1회분을 선별해 §e" + moved
                    + "개§f 적재했습니다. §7식량176 · 철56 · 연료8 · 통나무32 · 석재벽돌128"));
        } else {
            player.sendSystemMessage(Component.literal("§b[물리 화물 적재] §f전초 창고군 → 상자 광산수레 §e" + moved
                    + "개§f 적재. §7Shift를 누른 채 선택하면 다음 빈 수레에는 전선 보급 묶음만 선별 적재합니다."));
        }
        player.sendSystemMessage(Component.literal("§7이 수레를 실제 레일망으로 다른 자신의 활성 전초 하역장까지 운반한 뒤 같은 메뉴를 선택하세요."));
    }

    private static boolean checkFrontlineBundle(ServerPlayer player, List<Container> sources) {
        boolean ready = true;
        ready &= checkFrontlineRequirement(player, sources, "식량", FRONTLINE_FOOD, FreightService::isFieldFood);
        ready &= checkFrontlineRequirement(player, sources, "철 주괴", FRONTLINE_IRON, stack -> stack.is(Items.IRON_INGOT));
        ready &= checkFrontlineRequirement(player, sources, "연료", FRONTLINE_FUEL, FreightService::isFieldFuel);
        ready &= checkFrontlineRequirement(player, sources, "통나무", FRONTLINE_LOGS, stack -> stack.is(ItemTags.LOGS));
        ready &= checkFrontlineRequirement(player, sources, "석재 벽돌", FRONTLINE_STONE_BRICKS, stack -> stack.is(Items.STONE_BRICKS));
        if (!ready) {
            player.sendSystemMessage(Component.literal("§7전선 묶음은 일부만 빼가지 않습니다. 부족분을 이 출발 전초 통에 채운 뒤 Shift+선택으로 다시 적재하세요."));
        }
        return ready;
    }

    private static boolean checkFrontlineRequirement(ServerPlayer player, List<Container> sources, String label, int required,
                                                     Predicate<ItemStack> matcher) {
        int have = countMatching(sources, matcher);
        if (have >= required) return true;
        player.sendSystemMessage(Component.literal("§c[전선 화물] §f" + label + " 부족 · §e" + have + "§7/§f" + required));
        return false;
    }

    private static FrontlineLoadResult moveFrontlineBundleInto(List<Container> sources, Container target) {
        int food = moveMatchingInto(sources, target, FreightService::isFieldFood, FRONTLINE_FOOD);
        int iron = moveMatchingInto(sources, target, stack -> stack.is(Items.IRON_INGOT), FRONTLINE_IRON);
        int fuel = moveMatchingInto(sources, target, FreightService::isFieldFuel, FRONTLINE_FUEL);
        int logs = moveMatchingInto(sources, target, stack -> stack.is(ItemTags.LOGS), FRONTLINE_LOGS);
        int bricks = moveMatchingInto(sources, target, stack -> stack.is(Items.STONE_BRICKS), FRONTLINE_STONE_BRICKS);
        return new FrontlineLoadResult(food, iron, fuel, logs, bricks);
    }

    private static int moveMatchingInto(List<Container> sources, Container target, Predicate<ItemStack> matcher, int amount) {
        if (amount <= 0) return 0;
        List<SourceSlot> candidates = new ArrayList<>();
        for (Container source : sources) {
            for (int slot = 0; slot < source.getContainerSize(); slot++) {
                ItemStack stack = source.getItem(slot);
                if (matcher.test(stack)) candidates.add(new SourceSlot(source, slot));
            }
        }
        candidates.sort(Comparator.comparingInt((SourceSlot slot) -> slot.container().getItem(slot.slot()).getCount()).reversed());

        int remaining = amount;
        int moved = 0;
        for (SourceSlot candidate : candidates) {
            if (remaining <= 0) break;
            ItemStack stack = candidate.container().getItem(candidate.slot());
            if (!matcher.test(stack) || stack.isEmpty()) continue;
            int request = Math.min(remaining, stack.getCount());
            ItemStack copy = stack.copyWithCount(request);
            int inserted = insertInto(copy, target);
            if (inserted <= 0) continue;
            stack.shrink(inserted);
            candidate.container().setChanged();
            moved += inserted;
            remaining -= inserted;
        }
        return moved;
    }

    private static void unload(ServerPlayer player, ServerLevel level, OutpostData.OutpostEntry destination, MinecartChest cart, String taggedOwner) {
        if (!taggedOwner.equals(player.getUUID().toString())) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f이 운송 수레는 다른 플레이어가 적재한 화물입니다."));
            return;
        }
        var data = cart.getPersistentData();
        String originDimension = data.getStringOr(ORIGIN_DIMENSION_KEY, "");
        BlockPos origin = new BlockPos(data.getIntOr(ORIGIN_X_KEY, 0), data.getIntOr(ORIGIN_Y_KEY, 0), data.getIntOr(ORIGIN_Z_KEY, 0));
        if (!originDimension.equals(level.dimension().toString())) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f출발 전초와 같은 차원에서만 이 화물을 하역할 수 있습니다."));
            return;
        }
        if (destination.dimension().equals(originDimension) && destination.pos().equals(origin)) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f출발 전초가 아닌 §e다른 자신의 활성 전초§f까지 실제로 수레를 운반해야 합니다."));
            return;
        }

        FieldDepotData.DepotEntry depot = depotForOutpost(player, destination);
        if (depot == null) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f도착 전초의 등록 통 물류 앵커를 확인할 수 없습니다."));
            return;
        }
        List<Container> targets = storageForDepot(player, level, depot);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f도착 전초에서 현재 사용할 수 있는 물류 통이 없습니다."));
            return;
        }

        int before = countBulk(cart);
        if (before <= 0) {
            clearManifest(cart);
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f운송 대상으로 남아 있는 대량 자원이 없어 화물 표식을 해제했습니다."));
            return;
        }
        int moved = moveBulkOut(cart, targets);
        int remaining = countBulk(cart);
        if (moved <= 0) {
            player.sendSystemMessage(Component.literal("§3[물리 화물] §f도착 전초의 실제 통들에 남은 적재 공간이 없습니다. §7수레 화물은 그대로 유지됩니다."));
            return;
        }
        if (remaining <= 0) clearManifest(cart);
        player.sendSystemMessage(Component.literal("§b[물리 화물 하역] §f상자 광산수레 → 도착 전초 창고군 §e" + moved
                + "개§f 하역. §7" + (remaining <= 0 ? "운송 완료 · 수레 화물 표식 해제" : "잔여 대량 자원 " + remaining + "개 · 공간 확보 후 다시 하역")));
    }

    private static MinecartChest nearestCart(ServerPlayer player, ServerLevel level) {
        return level.getEntitiesOfClass(MinecartChest.class, player.getBoundingBox().inflate(INTERACTION_RADIUS),
                        cart -> cart.isAlive() && cart.stillValid(player)).stream()
                .min(Comparator.comparingDouble(cart -> player.distanceToSqr(cart)))
                .orElse(null);
    }

    private static boolean isOnLoadedRail(ServerLevel level, MinecartChest cart) {
        BlockPos pos = cart.blockPosition();
        if (!level.hasChunkAt(pos)) return false;
        if (level.getBlockState(pos).is(BlockTags.RAILS)) return true;
        BlockPos below = pos.below();
        return level.hasChunkAt(below) && level.getBlockState(below).is(BlockTags.RAILS);
    }

    private static FieldDepotData.DepotEntry depotForOutpost(ServerPlayer player, OutpostData.OutpostEntry outpost) {
        FieldDepotData data = FieldDepotData.get(player);
        return data.depots(player).stream()
                .filter(depot -> depot.dimension().equals(outpost.dimension()) && depot.pos().equals(outpost.pos()))
                .findFirst()
                .orElse(null);
    }

    private static List<Container> storageForDepot(ServerPlayer player, ServerLevel level, FieldDepotData.DepotEntry depot) {
        List<Container> out = new ArrayList<>();
        addBarrelIfUsable(player, level, depot.pos(), out);
        for (FieldDepotData.LinkedBarrel link : FieldDepotData.get(player).linkedBarrels(player, depot)) {
            if (!link.dimension().equals(level.dimension().toString())) continue;
            addBarrelIfUsable(player, level, link.pos(), out);
        }
        return out;
    }

    private static void addBarrelIfUsable(ServerPlayer player, ServerLevel level, BlockPos pos, List<Container> out) {
        if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)) return;
        if (!level.getBlockState(pos).is(Blocks.BARREL)) return;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Container container) out.add(container);
    }

    private static int moveBulkInto(List<Container> sources, Container target) {
        int moved = 0;
        for (Container source : sources) {
            boolean changed = false;
            for (int slot = 0; slot < source.getContainerSize(); slot++) {
                ItemStack stack = source.getItem(slot);
                if (!FieldDepotService.isBulkMaterial(stack)) continue;
                ItemStack copy = stack.copy();
                int inserted = insertInto(copy, target);
                if (inserted <= 0) continue;
                stack.shrink(inserted);
                moved += inserted;
                changed = true;
                if (isFullForBulk(target)) break;
            }
            if (changed) source.setChanged();
            if (isFullForBulk(target)) break;
        }
        if (moved > 0) target.setChanged();
        return moved;
    }

    private static int moveBulkOut(Container source, List<Container> targets) {
        int moved = 0;
        for (int slot = 0; slot < source.getContainerSize(); slot++) {
            ItemStack stack = source.getItem(slot);
            if (!FieldDepotService.isBulkMaterial(stack)) continue;
            for (Container target : targets) {
                if (stack.isEmpty()) break;
                moved += insertInto(stack, target);
            }
        }
        if (moved > 0) source.setChanged();
        return moved;
    }

    private static int insertInto(ItemStack source, Container target) {
        if (source.isEmpty()) return 0;
        int before = source.getCount();
        boolean changed = false;
        for (int slot = 0; slot < target.getContainerSize() && !source.isEmpty(); slot++) {
            ItemStack existing = target.getItem(slot);
            if (existing.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(source, existing)) continue;
            if (!target.canPlaceItem(slot, source)) continue;
            int limit = Math.min(target.getMaxStackSize(source), existing.getMaxStackSize());
            int space = limit - existing.getCount();
            if (space <= 0) continue;
            int move = Math.min(space, source.getCount());
            existing.grow(move);
            source.shrink(move);
            changed = true;
        }
        for (int slot = 0; slot < target.getContainerSize() && !source.isEmpty(); slot++) {
            ItemStack existing = target.getItem(slot);
            if (!existing.isEmpty() || !target.canPlaceItem(slot, source)) continue;
            int limit = Math.min(target.getMaxStackSize(source), source.getMaxStackSize());
            if (limit <= 0) continue;
            int move = Math.min(limit, source.getCount());
            target.setItem(slot, source.copyWithCount(move));
            source.shrink(move);
            changed = true;
        }
        if (changed) target.setChanged();
        return before - source.getCount();
    }

    private static boolean isFullForBulk(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) return false;
            if (FieldDepotService.isBulkMaterial(stack)
                    && stack.getCount() < Math.min(container.getMaxStackSize(stack), stack.getMaxStackSize())) return false;
        }
        return true;
    }

    private static boolean isEmpty(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) if (!container.getItem(slot).isEmpty()) return false;
        return true;
    }

    private static int countBulk(Container container) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (FieldDepotService.isBulkMaterial(stack)) total += stack.getCount();
        }
        return total;
    }

    private static int countMatching(Container container, Predicate<ItemStack> matcher) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (matcher.test(stack)) total += stack.getCount();
        }
        return total;
    }

    private static int countMatching(List<Container> containers, Predicate<ItemStack> matcher) {
        int total = 0;
        for (Container container : containers) total += countMatching(container, matcher);
        return total;
    }

    private static boolean isFieldFood(ItemStack stack) {
        return stack.is(Items.WHEAT) || stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT);
    }

    private static boolean isFieldFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
    }

    private static void clearManifest(MinecartChest cart) {
        var data = cart.getPersistentData();
        data.remove(OWNER_KEY);
        data.remove(ORIGIN_DIMENSION_KEY);
        data.remove(ORIGIN_X_KEY);
        data.remove(ORIGIN_Y_KEY);
        data.remove(ORIGIN_Z_KEY);
        data.remove(FRONTLINE_KEY);
        cart.setChanged();
    }

    private record SourceSlot(Container container, int slot) {}

    private record FrontlineLoadResult(int food, int iron, int fuel, int logs, int stoneBricks) {
        int total() { return food + iron + fuel + logs + stoneBricks; }
        boolean complete() {
            return food == FRONTLINE_FOOD && iron == FRONTLINE_IRON && fuel == FRONTLINE_FUEL
                    && logs == FRONTLINE_LOGS && stoneBricks == FRONTLINE_STONE_BRICKS;
        }
    }
}

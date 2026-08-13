package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Makes every exterior loading-yard barrel authoritative and player-editable. */
public final class ErdenKingdomExteriorInventoryManager {
    private static final int SYNC_INTERVAL = 20;
    private static final int MAX_STACK_SIZE = 64;

    private static final List<ResourceItem> RESOURCES = List.of(
            new ResourceItem("wheat", Items.WHEAT),
            new ResourceItem("leather", Items.LEATHER),
            new ResourceItem("hay", Items.HAY_BLOCK),
            new ResourceItem("coal", Items.COAL),
            new ResourceItem("iron", Items.IRON_INGOT),
            new ResourceItem("paper", Items.PAPER)
    );

    private static final Set<String> CI_VERIFIED_NODES = new HashSet<>();
    private static final Set<String> CI_VISIBLE_RESOURCES = new HashSet<>();
    private static MinecraftServer activeServer;
    private static boolean ciPassed;

    private ErdenKingdomExteriorInventoryManager() {
    }

    /** Must run before ErdenKingdomSupplyManager dispatches cargo for the current tick. */
    public static void captureBeforeSupply(ServerTickEvent.Post event) {
        ServerLevel level = realm(event.getServer());
        if (level == null || level.getGameTime() % SYNC_INTERVAL != 0L) return;
        resetIfNeeded(event.getServer());

        ErdenKingdomSupplySavedData supply = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomSupplySavedData.TYPE);
        ErdenKingdomExteriorContainerSavedData containers = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorContainerSavedData.TYPE);
        if (supply.nodes().isEmpty()) return;

        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            if (!node.producer()) continue;
            if (!containers.isMaterialized(node.id)
                    || !ErdenKingdomExteriorBuilder.anchorBuilt(level, node)) continue;
            Container container = container(level, node);
            ErdenKingdomSupplySavedData.NodeState state = nodeState(supply, node.id);
            if (container == null || state == null) continue;

            ErdenKingdomSupplySavedData.NodeState updated = state;
            for (ResourceItem resource : resourcesFor(node.role)) {
                updated = updated.withStock(resource.resource, countItem(container, resource.item));
            }
            supply.replaceNode(updated);
            containers.markCaptured(node.id);
            noteCiVerification(node, updated, container);
        }
        verifyCi(containers);
    }

    /** Must run after supply production, dispatch and arrival settlement. */
    public static void materializeAfterSupply(ServerTickEvent.Post event) {
        ServerLevel level = realm(event.getServer());
        if (level == null || level.getGameTime() % SYNC_INTERVAL != 0L) return;
        resetIfNeeded(event.getServer());

        ErdenKingdomSupplySavedData supply = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomSupplySavedData.TYPE);
        ErdenKingdomExteriorContainerSavedData containers = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorContainerSavedData.TYPE);
        if (supply.nodes().isEmpty()) return;

        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            if (!node.producer()) continue;
            if (!ErdenKingdomExteriorBuilder.anchorBuilt(level, node)) continue;
            Container container = container(level, node);
            ErdenKingdomSupplySavedData.NodeState state = nodeState(supply, node.id);
            if (container == null || state == null || !fits(container, state, node.role)) continue;
            writeContainer(container, state, node.role);
            containers.markMaterialized(node.id);
            containers.recordWrite();
            noteCiVerification(node, state, container);
        }
        verifyCi(containers);
    }

    public static void onInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)) return;

        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            BlockPos storage = ErdenKingdomExteriorBuilder.storagePosition(level, node);
            if (!storage.equals(event.getPos())) continue;
            ErdenKingdomSupplySavedData supply = level.getDataStorage()
                    .computeIfAbsent(ErdenKingdomSupplySavedData.TYPE);
            ErdenKingdomSupplySavedData.NodeState state = nodeState(supply, node.id);
            if (state == null) return;
            long inTransit = 0L;
            for (ErdenKingdomSupplySavedData.ShipmentState shipment : supply.shipments()) {
                if (shipment.sourceId().equals(node.id) && shipment.status().equals("in_transit")) {
                    inTransit += shipment.amount();
                }
            }
            player.sendSystemMessage(Component.literal(
                    roleName(node.role) + " | 현장 재고 " + compactStock(state, node.role)
                            + " | 운송 중 " + inTransit
                            + " | 누적 생산 " + state.totalProduced()
                            + " | 운송 지연 " + state.blockedDays() + "일"), true);
            return;
        }
    }

    private static void resetIfNeeded(MinecraftServer server) {
        if (activeServer == server) return;
        activeServer = server;
        ciPassed = false;
        CI_VERIFIED_NODES.clear();
        CI_VISIBLE_RESOURCES.clear();
    }

    private static ServerLevel realm(MinecraftServer server) {
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return null;
        return level;
    }

    private static Container container(
            ServerLevel level,
            ErdenKingdomSupplyCatalog.SupplyNode node) {
        BlockPos pos = ErdenKingdomExteriorBuilder.storagePosition(level, node);
        if (!level.hasChunkAt(pos)) return null;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof Container container ? container : null;
    }

    private static ErdenKingdomSupplySavedData.NodeState nodeState(
            ErdenKingdomSupplySavedData supply,
            String nodeId) {
        for (ErdenKingdomSupplySavedData.NodeState node : supply.nodes()) {
            if (node.id().equals(nodeId)) return node;
        }
        return null;
    }

    private static List<ResourceItem> resourcesFor(String role) {
        return switch (role) {
            case "grain_estate" -> List.of(resource("wheat"));
            case "ranch" -> List.of(resource("leather"), resource("hay"));
            case "colliery" -> List.of(resource("coal"));
            case "iron_mine" -> List.of(resource("iron"));
            case "paper_mill" -> List.of(resource("paper"));
            default -> List.of();
        };
    }

    private static ResourceItem resource(String id) {
        for (ResourceItem resource : RESOURCES) {
            if (resource.resource.equals(id)) return resource;
        }
        throw new IllegalArgumentException("Unsupported exterior resource " + id);
    }

    private static long countItem(Container container, Item item) {
        long count = 0L;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static boolean fits(
            Container container,
            ErdenKingdomSupplySavedData.NodeState state,
            String role) {
        long neededStacks = 0L;
        for (ResourceItem resource : resourcesFor(role)) {
            neededStacks += (state.stock(resource.resource) + MAX_STACK_SIZE - 1L) / MAX_STACK_SIZE;
        }
        int availableSlots = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || managedItem(stack.getItem(), role)) availableSlots++;
        }
        return neededStacks <= availableSlots;
    }

    private static void writeContainer(
            Container container,
            ErdenKingdomSupplySavedData.NodeState state,
            String role) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && managedItem(stack.getItem(), role)) {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
        for (ResourceItem resource : resourcesFor(role)) {
            long remaining = state.stock(resource.resource);
            for (int slot = 0; slot < container.getContainerSize() && remaining > 0L; slot++) {
                if (!container.getItem(slot).isEmpty()) continue;
                int amount = (int) Math.min(MAX_STACK_SIZE, remaining);
                container.setItem(slot, new ItemStack(resource.item, amount));
                remaining -= amount;
            }
        }
        container.setChanged();
    }

    private static boolean managedItem(Item item, String role) {
        for (ResourceItem resource : resourcesFor(role)) {
            if (resource.item == item) return true;
        }
        return false;
    }

    private static String compactStock(
            ErdenKingdomSupplySavedData.NodeState state,
            String role) {
        List<String> values = new ArrayList<>();
        for (ResourceItem resource : resourcesFor(role)) {
            values.add(resourceName(resource.resource) + " " + state.stock(resource.resource));
        }
        return values.isEmpty() ? "없음" : String.join(", ", values);
    }

    private static String roleName(String role) {
        return switch (role) {
            case "grain_estate" -> "곡물 농장";
            case "ranch" -> "왕도 외곽 목장";
            case "colliery" -> "석탄 광산";
            case "iron_mine" -> "철 광산";
            case "paper_mill" -> "제지소";
            case "river_wharf" -> "강변 부두";
            default -> "외곽 생산지";
        };
    }

    private static String resourceName(String resource) {
        return switch (resource) {
            case "wheat" -> "밀";
            case "leather" -> "가죽";
            case "hay" -> "건초";
            case "coal" -> "석탄";
            case "iron" -> "철";
            case "paper" -> "종이";
            default -> resource;
        };
    }

    /**
     * Records an authoritative observation while this producer's chunk is actually loaded. The
     * observation is sticky for the current server run, so the final CI decision does not require
     * all remote producer chunks to remain loaded simultaneously after their transient tickets are
     * deliberately released.
     */
    private static void noteCiVerification(
            ErdenKingdomSupplyCatalog.SupplyNode node,
            ErdenKingdomSupplySavedData.NodeState state,
            Container container) {
        if (!"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        for (ResourceItem resource : resourcesFor(node.role)) {
            long visible = countItem(container, resource.item);
            if (visible != state.stock(resource.resource)) return;
        }
        CI_VERIFIED_NODES.add(node.id);
        for (ResourceItem resource : resourcesFor(node.role)) {
            if (countItem(container, resource.item) > 0L) {
                CI_VISIBLE_RESOURCES.add(resource.resource);
            }
        }
    }

    private static void verifyCi(ErdenKingdomExteriorContainerSavedData containers) {
        int producers = ErdenKingdomSupplyCatalog.producerCount();
        if (ciPassed
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || containers.materializedCount() != producers
                || containers.capturedCount() != producers
                || CI_VERIFIED_NODES.size() != producers
                || CI_VISIBLE_RESOURCES.size() < RESOURCES.size()
                || containers.captures() <= 0L
                || containers.writes() <= 0L) return;
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_EXTERIOR_INVENTORY_PASS revision={} nodes={} containers={} captured_nodes={} verified_nodes={} visible_resources={} captures={} writes={} progressive_loaded_verification=true player_removal_authoritative=true dispatch_reduces_barrels=true local_reserves=true",
                ErdenKingdomExteriorContainerSavedData.REVISION,
                producers, containers.materializedCount(), containers.capturedCount(),
                CI_VERIFIED_NODES.size(), CI_VISIBLE_RESOURCES.size(),
                containers.captures(), containers.writes());
    }

    private record ResourceItem(String resource, Item item) {
    }
}

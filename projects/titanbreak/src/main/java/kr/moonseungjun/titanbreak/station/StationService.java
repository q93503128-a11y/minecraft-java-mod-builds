package kr.moonseungjun.titanbreak.station;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.network.TitanbreakNetwork;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import kr.moonseungjun.titanbreak.registry.ModBlocks;
import kr.moonseungjun.titanbreak.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StationService {
    private static final int INSTALL_TICKS = 100;
    private static final int REMOVE_TICKS = 80;
    private static final Map<UUID, SurgeryProcess> SURGERIES = new ConcurrentHashMap<>();

    private StationService() {}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.FABRICATOR_I.get())) {
            open(player, "fabricator", pos);
            consumeInteraction(event);
            return;
        }
        if (state.is(ModBlocks.SURGICAL_BAY.get())) {
            open(player, "surgery", pos);
            consumeInteraction(event);
            return;
        }

        if (state.is(Blocks.CRAFTING_TABLE) && player.isShiftKeyDown()) {
            if (assembleFabricator(player, level, pos)) consumeInteraction(event);
        }
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
        AugmentationCatalog.Slot slot = event.getHand() == InteractionHand.MAIN_HAND
                ? AugmentationCatalog.Slot.RIGHT_ARM_MAIN : AugmentationCatalog.Slot.LEFT_ARM_MAIN;
        String installed = state.installed(slot);
        if ("blade_arm".equals(installed)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    private static void consumeInteraction(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static void open(ServerPlayer player, String station, BlockPos pos) {
        TitanbreakNetwork.openStation(player, station + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ());
    }

    private static boolean assembleFabricator(ServerPlayer player, ServerLevel level, BlockPos pos) {
        Requirement[] requirements = {
                new Requirement(Items.IRON_INGOT, 8),
                new Requirement(Items.COPPER_INGOT, 8),
                new Requirement(Items.REDSTONE, 6),
                new Requirement(Items.QUARTZ, 2)
        };
        if (!hasAll(player, requirements)) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.fabricator_requirements"));
            return false;
        }
        consumeAll(player, requirements);
        level.setBlockAndUpdate(pos, ModBlocks.FABRICATOR_I.get().defaultBlockState());
        player.sendSystemMessage(Component.translatable("message.titanbreak.fabricator_assembled"));
        return true;
    }

    public static void handleAction(ServerPlayer player, String raw) {
        String[] parts = raw.split("\\|", 6);
        if (parts.length < 5) return;
        String station = parts[0];
        BlockPos pos;
        try {
            pos = new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException ignored) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || player.blockPosition().distSqr(pos) > 64.0D) return;
        BlockState block = level.getBlockState(pos);
        if (station.equals("fabricator") && !block.is(ModBlocks.FABRICATOR_I.get())) return;
        if (station.equals("surgery") && !block.is(ModBlocks.SURGICAL_BAY.get())) return;

        String action = parts[4];
        String argument = parts.length >= 6 ? parts[5] : "";
        if (station.equals("fabricator")) {
            if (action.equals("fabricate")) fabricate(player, argument);
            else if (action.equals("assemble_surgery")) assembleSurgicalBay(player);
        } else if (station.equals("surgery")) {
            if (action.equals("install")) beginInstall(player, pos, argument);
            else if (action.equals("remove")) beginRemove(player, pos, argument);
        }
    }

    private static void fabricate(ServerPlayer player, String augmentId) {
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(augmentId);
        if (definition == null || !definition.fabricatorOne()) return;
        Item output = ModItems.augmentationByPath(definition.itemId());
        if (output == null) return;
        Requirement[] requirements = definition.recipe().entrySet().stream()
                .map(entry -> new Requirement(ModItems.byPath(entry.getKey()), entry.getValue()))
                .toArray(Requirement[]::new);
        for (Requirement requirement : requirements) if (requirement.item() == null) return;
        if (!hasAll(player, requirements)) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.materials_missing"));
            return;
        }
        consumeAll(player, requirements);
        give(player, new ItemStack(output));
        player.sendSystemMessage(Component.translatable("message.titanbreak.fabricated", Component.translatable(definition.nameKey())));
    }

    private static void assembleSurgicalBay(ServerPlayer player) {
        Requirement[] requirements = {
                new Requirement(Items.IRON_INGOT, 10),
                new Requirement(Items.COPPER_INGOT, 6),
                new Requirement(Items.REDSTONE, 4),
                new Requirement(Items.GLASS, 4)
        };
        if (!hasAll(player, requirements) || countBeds(player) < 1) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.surgery_requirements"));
            return;
        }
        consumeAll(player, requirements);
        consumeBed(player);
        give(player, new ItemStack(ModItems.SURGICAL_BAY.get()));
        player.sendSystemMessage(Component.translatable("message.titanbreak.surgery_assembled"));
    }

    private static void beginInstall(ServerPlayer player, BlockPos pos, String argument) {
        if (SURGERIES.containsKey(player.getUUID())) return;
        String[] args = argument.split(":", 2);
        if (args.length < 2) return;

        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(args[0]);
        if (definition == null) return;
        AugmentationCatalog.Slot anchor;
        try {
            anchor = AugmentationCatalog.Slot.valueOf(args[1]);
        } catch (IllegalArgumentException ignored) {
            return;
        }

        AugmentationCatalog.Placement placement = definition.placementFor(anchor);
        if (placement == null) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.slot_incompatible"));
            return;
        }

        TitanPlayerData.State state = TitanPlayerData.get(((ServerLevel) player.level()).getServer()).state(player);
        for (AugmentationCatalog.Slot slot : placement.slots()) {
            if (state.installed(slot) != null) {
                player.sendSystemMessage(Component.translatable("message.titanbreak.slot_occupied"));
                return;
            }
        }

        Item module = ModItems.augmentationByPath(definition.itemId());
        if (module == null || count(player, module) < 1) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.module_missing"));
            return;
        }
        long finish = ((ServerLevel) player.level()).getGameTime() + INSTALL_TICKS;
        SURGERIES.put(player.getUUID(), new SurgeryProcess(pos, true, definition.id(), anchor, finish));
        player.sendSystemMessage(Component.translatable("message.titanbreak.surgery_install_started"));
        TitanbreakNetwork.sync(player);
    }

    private static void beginRemove(ServerPlayer player, BlockPos pos, String argument) {
        if (SURGERIES.containsKey(player.getUUID())) return;
        AugmentationCatalog.Slot slot;
        try {
            slot = AugmentationCatalog.Slot.valueOf(argument);
        } catch (IllegalArgumentException ignored) {
            return;
        }
        TitanPlayerData.State state = TitanPlayerData.get(((ServerLevel) player.level()).getServer()).state(player);
        String augment = state.installed(slot);
        if (augment == null) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.slot_empty"));
            return;
        }
        long finish = ((ServerLevel) player.level()).getGameTime() + REMOVE_TICKS;
        SURGERIES.put(player.getUUID(), new SurgeryProcess(pos, false, augment, slot, finish));
        player.sendSystemMessage(Component.translatable("message.titanbreak.surgery_remove_started"));
        TitanbreakNetwork.sync(player);
    }

    public static void tick(ServerPlayer player) {
        SurgeryProcess process = SURGERIES.get(player.getUUID());
        if (process == null) return;
        if (!(player.level() instanceof ServerLevel level)
                || player.blockPosition().distSqr(process.pos()) > 64.0D
                || !level.getBlockState(process.pos()).is(ModBlocks.SURGICAL_BAY.get())) {
            SURGERIES.remove(player.getUUID());
            player.sendSystemMessage(Component.translatable("message.titanbreak.surgery_cancelled"));
            TitanbreakNetwork.sync(player);
            return;
        }
        if (level.getGameTime() < process.finishTick()) return;

        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        if (process.install()) {
            AugmentationCatalog.Definition definition = AugmentationCatalog.byId(process.augmentId());
            Item module = definition == null ? null : ModItems.augmentationByPath(definition.itemId());
            if (definition == null || module == null || count(player, module) < 1
                    || !data.install(player, process.slot(), process.augmentId())) {
                SURGERIES.remove(player.getUUID());
                player.sendSystemMessage(Component.translatable("message.titanbreak.surgery_cancelled"));
                TitanbreakNetwork.sync(player);
                return;
            }
            consume(player, module, 1);
            player.sendSystemMessage(Component.translatable("message.titanbreak.surgery_install_complete",
                    Component.translatable(definition.nameKey())));
        } else {
            String removed = data.remove(player, process.slot());
            AugmentationCatalog.Definition definition = AugmentationCatalog.byId(removed);
            Item module = definition == null ? null : ModItems.augmentationByPath(definition.itemId());
            if (module != null) give(player, new ItemStack(module));
            player.sendSystemMessage(Component.translatable("message.titanbreak.surgery_remove_complete"));
        }
        SURGERIES.remove(player.getUUID());
        TitanbreakNetwork.sync(player);
    }

    public static int remainingTicks(ServerPlayer player) {
        SurgeryProcess process = SURGERIES.get(player.getUUID());
        if (process == null || !(player.level() instanceof ServerLevel level)) return 0;
        return (int) Math.max(0L, process.finishTick() - level.getGameTime());
    }

    public static void clear(UUID playerId) {
        SURGERIES.remove(playerId);
    }

    public static void useHook(ServerPlayer player) {
        TitanPlayerData.State state = TitanPlayerData.get(((ServerLevel) player.level()).getServer()).state(player);
        if (!state.hasInstalled("wire_hook_arm")) return;
        var hit = player.pick(24.0D, 1.0F, false);
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return;
        var direction = hit.getLocation().subtract(player.position()).normalize();
        player.setDeltaMovement(direction.scale(1.55D).add(0.0D, 0.18D, 0.0D));
        player.hurtMarked = true;
    }

    private static boolean hasAll(ServerPlayer player, Requirement[] requirements) {
        for (Requirement requirement : requirements) {
            if (requirement.item() == null || count(player, requirement.item()) < requirement.count()) return false;
        }
        return true;
    }

    private static void consumeAll(ServerPlayer player, Requirement[] requirements) {
        for (Requirement requirement : requirements) consume(player, requirement.item(), requirement.count());
    }

    private static int count(ServerPlayer player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static void consume(ServerPlayer player, Item item, int count) {
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(item)) continue;
            int remove = Math.min(remaining, stack.getCount());
            player.getInventory().removeItem(i, remove);
            remaining -= remove;
        }
    }

    private static int countBeds(ServerPlayer player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof BedItem) total += stack.getCount();
        }
        return total;
    }

    private static void consumeBed(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof BedItem) {
                player.getInventory().removeItem(i, 1);
                return;
            }
        }
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) player.drop(stack, false);
    }

    private record Requirement(Item item, int count) {}
    private record SurgeryProcess(BlockPos pos, boolean install, String augmentId,
                                  AugmentationCatalog.Slot slot, long finishTick) {}
}

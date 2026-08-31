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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StationService {
    private static final Map<UUID, SurgeryProcess> SURGERIES = new ConcurrentHashMap<>();
    private static final Set<String> FULL_REPLACEMENT_ARMS = Set.of(
            "blade_arm", "high_frequency_blade_arm", "rail_projector_arm", "photon_emitter_arm");

    private StationService() {}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        int tier = fabricatorTier(state);
        if (tier > 0) {
            open(player, "fabricator_" + tier, pos);
            consumeInteraction(event);
            return;
        }
        if (state.is(ModBlocks.SURGICAL_BAY.get())) {
            open(player, "surgery", pos);
            consumeInteraction(event);
            return;
        }
        if (state.is(ModBlocks.IMPLANT_VAULT.get())) {
            TitanbreakNetwork.sync(player);
            player.sendSystemMessage(Component.translatable("message.titanbreak.vault_ready",
                    TitanPlayerData.get(level.getServer()).state(player).vaultView().size()), true);
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
        if (FULL_REPLACEMENT_ARMS.contains(installed)) {
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
                new Requirement(Items.IRON_INGOT, 8), new Requirement(Items.COPPER_INGOT, 8),
                new Requirement(Items.REDSTONE, 6), new Requirement(Items.QUARTZ, 2)
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
        int tier = station.startsWith("fabricator_") ? parseFabricatorTier(station) : 0;
        if (tier > 0 && fabricatorTier(block) != tier) return;
        if (station.equals("surgery") && !block.is(ModBlocks.SURGICAL_BAY.get())) return;

        String action = parts[4];
        String argument = parts.length >= 6 ? parts[5] : "";
        if (tier > 0) {
            if (action.equals("fabricate")) fabricate(player, argument, tier);
            else if (action.equals("assemble_surgery")) assembleSurgicalBay(player);
            else if (action.equals("assemble_vault")) assembleImplantVault(player);
            else if (action.equals("upgrade_fabricator")) upgradeFabricator(player, level, pos, tier);
            else if (action.equals("enhance")) enhanceAugment(player, argument, tier);
            else if (action.equals("upgrade_mk")) upgradeAugmentMk(player, argument, tier);
        } else if (station.equals("surgery")) {
            if (action.equals("install")) beginInstall(player, pos, argument);
            else if (action.equals("remove")) beginRemove(player, pos, argument);
        }
    }

    private static int parseFabricatorTier(String station) {
        try {
            return Math.max(1, Math.min(3, Integer.parseInt(station.substring("fabricator_".length()))));
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static int fabricatorTier(BlockState state) {
        if (state.is(ModBlocks.FABRICATOR_I.get())) return 1;
        if (state.is(ModBlocks.FABRICATOR_II.get())) return 2;
        if (state.is(ModBlocks.FABRICATOR_III.get())) return 3;
        return 0;
    }

    private static void fabricate(ServerPlayer player, String augmentId, int fabricatorTier) {
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(augmentId);
        if (definition == null || definition.fabricatorTier() > fabricatorTier) return;
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

    private static void enhanceAugment(ServerPlayer player, String augmentId, int fabricatorTier) {
        TitanPlayerData data = TitanPlayerData.get(((ServerLevel) player.level()).getServer());
        TitanPlayerData.AugmentInstance instance = data.firstInstance(player, augmentId);
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(augmentId);
        if (instance == null || definition == null || definition.fabricatorTier() > fabricatorTier
                || instance.enhancement() >= TitanPlayerData.MAX_ENHANCEMENT) return;

        int next = instance.enhancement() + 1;
        Item familyMaterial = firstRecipeMaterial(definition);
        Requirement[] requirements = {
                new Requirement(ModItems.SERVO_BUNDLE.get(), 1 + next / 4),
                new Requirement(familyMaterial, 1 + next / 3)
        };
        if (!hasAll(player, requirements)) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.enhance_requirements"));
            return;
        }
        consumeAll(player, requirements);
        if (data.enhance(player, augmentId)) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.enhance_complete",
                    Component.translatable(definition.nameKey()), next));
            TitanbreakNetwork.sync(player);
        }
    }

    private static void upgradeAugmentMk(ServerPlayer player, String augmentId, int fabricatorTier) {
        TitanPlayerData data = TitanPlayerData.get(((ServerLevel) player.level()).getServer());
        TitanPlayerData.AugmentInstance instance = data.firstInstance(player, augmentId);
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(augmentId);
        if (instance == null || definition == null || definition.fabricatorTier() > fabricatorTier
                || instance.mk() >= TitanPlayerData.MAX_AUGMENT_MK) return;

        int nextMk = instance.mk() + 1;
        int requiredFabricator = nextMk <= 2 ? 1 : nextMk <= 4 ? 2 : 3;
        if (fabricatorTier < requiredFabricator) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.mk_fabricator_locked", requiredFabricator));
            return;
        }
        Item familyMaterial = firstRecipeMaterial(definition);
        Item advanced = advancedMaterial(definition);
        Requirement[] requirements = {
                new Requirement(familyMaterial, nextMk),
                new Requirement(advanced, Math.max(1, nextMk - 2))
        };
        if (!hasAll(player, requirements)) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.mk_requirements"));
            return;
        }
        consumeAll(player, requirements);
        if (data.upgradeMk(player, augmentId)) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.mk_complete",
                    Component.translatable(definition.nameKey()), nextMk));
            TitanbreakNetwork.sync(player);
        }
    }

    private static Item firstRecipeMaterial(AugmentationCatalog.Definition definition) {
        for (String key : definition.recipe().keySet()) {
            Item item = ModItems.byPath(key);
            if (item != null) return item;
        }
        return ModItems.SERVO_BUNDLE.get();
    }

    private static Item advancedMaterial(AugmentationCatalog.Definition definition) {
        AugmentationCatalog.Region region = definition.placements().getFirst().anchor().region();
        return switch (region) {
            case EYE -> ModItems.PREDICTIVE_OPTIC_CORE.get();
            case BRAIN, NERVES, SPINE -> ModItems.TEMPORAL_NEURAL_BUNDLE.get();
            case HEART, AUX_ORGAN -> ModItems.CIRCULATION_CORE.get();
            case SKELETON, SKIN -> ModItems.IMPACT_CORE.get();
            case LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG -> ModItems.CAPACITOR_STACK.get();
        };
    }

    private static void upgradeFabricator(ServerPlayer player, ServerLevel level, BlockPos pos, int tier) {
        if (tier == 1) {
            Requirement[] requirements = {
                    new Requirement(ModItems.CALCULATION_CORE.get(), 4),
                    new Requirement(ModItems.SERVO_BUNDLE.get(), 4),
                    new Requirement(ModItems.TEMPORAL_NEURAL_BUNDLE.get(), 1),
                    new Requirement(ModItems.THERMAL_OPTIC_CLUSTER.get(), 1)
            };
            if (!hasAll(player, requirements)) {
                player.sendSystemMessage(Component.translatable("message.titanbreak.fabricator_ii_requirements"));
                return;
            }
            consumeAll(player, requirements);
            level.setBlockAndUpdate(pos, ModBlocks.FABRICATOR_II.get().defaultBlockState());
            player.sendSystemMessage(Component.translatable("message.titanbreak.fabricator_upgraded", 2));
            return;
        }
        if (tier == 2) {
            Requirement[] fixed = {
                    new Requirement(ModItems.CAPACITOR_STACK.get(), 6),
                    new Requirement(ModItems.HEAT_SINK.get(), 4)
            };
            Item[] bossCores = {
                    ModItems.PURSUER_REACTION_ORGAN.get(), ModItems.GRAVEMARCH_IMPACT_HEART.get(),
                    ModItems.BASTION_ARMOR_CORE.get(), ModItems.REGNANT_REGENERATION_CORE.get(),
                    ModItems.WATCHER_PREDICTIVE_BRAIN.get(), ModItems.CHRONOPHAGE_TEMPORAL_ORGAN.get(),
                    ModItems.LEVIATHAN_STORM_ORGAN.get(), ModItems.ASH_RADIANT_HEART.get(),
                    ModItems.NULL_SUPPRESSION_CORE.get(), ModItems.WORLDBREAKER_CORE.get()
            };
            Item[] advanced = {
                    ModItems.PHASE_COIL.get(), ModItems.TEMPORAL_ORGAN.get(), ModItems.REGENERATIVE_TISSUE.get(),
                    ModItems.CIRCULATION_CORE.get(), ModItems.NANO_MEDIUM.get()
            };
            if (!hasAll(player, fixed) || countAny(player, bossCores) < 1 || countAny(player, advanced) < 2) {
                player.sendSystemMessage(Component.translatable("message.titanbreak.fabricator_iii_requirements"));
                return;
            }
            consumeAll(player, fixed);
            consumeAny(player, bossCores, 1);
            consumeAny(player, advanced, 2);
            level.setBlockAndUpdate(pos, ModBlocks.FABRICATOR_III.get().defaultBlockState());
            player.sendSystemMessage(Component.translatable("message.titanbreak.fabricator_upgraded", 3));
        }
    }

    private static void assembleSurgicalBay(ServerPlayer player) {
        Requirement[] requirements = {
                new Requirement(Items.IRON_INGOT, 10), new Requirement(Items.COPPER_INGOT, 6),
                new Requirement(Items.REDSTONE, 4), new Requirement(Items.GLASS, 4)
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

    private static void assembleImplantVault(ServerPlayer player) {
        Requirement[] requirements = {
                new Requirement(Items.IRON_INGOT, 6), new Requirement(Items.COPPER_INGOT, 4),
                new Requirement(Items.REDSTONE, 2), new Requirement(Items.CHEST, 1)
        };
        if (!hasAll(player, requirements)) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.vault_requirements"));
            return;
        }
        consumeAll(player, requirements);
        give(player, new ItemStack(ModItems.IMPLANT_VAULT.get()));
        player.sendSystemMessage(Component.translatable("message.titanbreak.vault_assembled"));
    }

    private static void beginInstall(ServerPlayer player, BlockPos pos, String argument) {
        if (SURGERIES.containsKey(player.getUUID())) return;
        String[] args = argument.split(":", 2);
        if (args.length < 2) return;
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(args[0]);
        if (definition == null) return;
        AugmentationCatalog.Slot anchor;
        try { anchor = AugmentationCatalog.Slot.valueOf(args[1]); }
        catch (IllegalArgumentException ignored) { return; }

        AugmentationCatalog.Placement placement = definition.placementFor(anchor);
        if (placement == null) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.slot_incompatible"));
            return;
        }
        TitanPlayerData data = TitanPlayerData.get(((ServerLevel) player.level()).getServer());
        TitanPlayerData.State state = data.state(player);
        for (AugmentationCatalog.Slot slot : placement.slots()) {
            if (state.installed(slot) != null) {
                player.sendSystemMessage(Component.translatable("message.titanbreak.slot_occupied"));
                return;
            }
        }
        Item module = ModItems.augmentationByPath(definition.itemId());
        boolean availableInVault = state.vaultView().stream().anyMatch(instance -> instance.id().equals(definition.id()));
        if (module == null || (!availableInVault && count(player, module) < 1)) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.module_missing"));
            return;
        }
        int ticks = surgeryTicks(definition.tier(), true);
        long finish = ((ServerLevel) player.level()).getGameTime() + ticks;
        SURGERIES.put(player.getUUID(), new SurgeryProcess(pos, true, definition.id(), anchor, finish));
        player.sendSystemMessage(Component.translatable("message.titanbreak.surgery_install_started"));
        TitanbreakNetwork.sync(player);
    }

    private static void beginRemove(ServerPlayer player, BlockPos pos, String argument) {
        if (SURGERIES.containsKey(player.getUUID())) return;
        AugmentationCatalog.Slot slot;
        try { slot = AugmentationCatalog.Slot.valueOf(argument); }
        catch (IllegalArgumentException ignored) { return; }
        TitanPlayerData.State state = TitanPlayerData.get(((ServerLevel) player.level()).getServer()).state(player);
        String augment = state.installed(slot);
        if (augment == null) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.slot_empty"));
            return;
        }
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(augment);
        int ticks = surgeryTicks(definition == null ? 1 : definition.tier(), false);
        long finish = ((ServerLevel) player.level()).getGameTime() + ticks;
        SURGERIES.put(player.getUUID(), new SurgeryProcess(pos, false, augment, slot, finish));
        player.sendSystemMessage(Component.translatable("message.titanbreak.surgery_remove_started"));
        TitanbreakNetwork.sync(player);
    }

    private static int surgeryTicks(int tier, boolean install) {
        if (install) return switch (tier) {
            case 0 -> 80; case 1 -> 120; case 2 -> 200; case 3 -> 300; default -> 420;
        };
        return switch (tier) {
            case 0 -> 60; case 1 -> 90; case 2 -> 160; case 3 -> 200; default -> 260;
        };
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
            if (definition == null || module == null) {
                cancel(player);
                return;
            }

            TitanPlayerData.AugmentInstance instance = data.takeVault(player, process.augmentId());
            boolean fromVault = instance != null;
            if (instance == null) {
                if (count(player, module) < 1) {
                    cancel(player);
                    return;
                }
                instance = TitanPlayerData.AugmentInstance.fresh(process.augmentId());
            }
            if (!data.installInstance(player, process.slot(), instance)) {
                if (fromVault) data.storeVault(player, instance);
                cancel(player);
                return;
            }
            if (!fromVault) consume(player, module, 1);
            player.sendSystemMessage(Component.translatable("message.titanbreak.surgery_install_complete",
                    Component.translatable(definition.nameKey()), instance.mk(), instance.enhancement()));
        } else {
            TitanPlayerData.AugmentInstance removed = data.removeInstance(player, process.slot());
            if (removed != null) data.storeVault(player, removed);
            player.sendSystemMessage(Component.translatable("message.titanbreak.surgery_remove_complete"));
        }
        SURGERIES.remove(player.getUUID());
        TitanbreakNetwork.sync(player);
    }

    private static void cancel(ServerPlayer player) {
        SURGERIES.remove(player.getUUID());
        player.sendSystemMessage(Component.translatable("message.titanbreak.surgery_cancelled"));
        TitanbreakNetwork.sync(player);
    }

    public static int remainingTicks(ServerPlayer player) {
        SurgeryProcess process = SURGERIES.get(player.getUUID());
        if (process == null || !(player.level() instanceof ServerLevel level)) return 0;
        return (int) Math.max(0L, process.finishTick() - level.getGameTime());
    }

    public static void clear(UUID playerId) { SURGERIES.remove(playerId); }

    public static void useHook(ServerPlayer player) {
        TitanPlayerData.State state = TitanPlayerData.get(((ServerLevel) player.level()).getServer()).state(player);
        if (!state.hasInstalled("wire_hook_arm")) return;
        var hit = player.pick(24.0D, 1.0F, false);
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return;
        var direction = hit.getLocation().subtract(player.position()).normalize();
        player.setDeltaMovement(direction.scale(1.55D).add(0.0D, 0.18D, 0.0D));
        player.hurtMarked = true;
        TitanPlayerData.get(((ServerLevel) player.level()).getServer()).addMasteryXp(player, "wire_hook_arm", 2);
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

    private static int countAny(ServerPlayer player, Item[] items) {
        int total = 0;
        for (Item item : items) total += count(player, item);
        return total;
    }

    private static void consumeAny(ServerPlayer player, Item[] items, int count) {
        int remaining = count;
        for (Item item : items) {
            if (remaining <= 0) return;
            int available = count(player, item);
            int consume = Math.min(remaining, available);
            if (consume > 0) {
                consume(player, item, consume);
                remaining -= consume;
            }
        }
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

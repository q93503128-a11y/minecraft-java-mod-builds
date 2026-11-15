package kr.moonseungjun.anomalousclasses.state;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.block.PuppetBlocks;
import kr.moonseungjun.anomalousclasses.entity.PuppetEntities;
import kr.moonseungjun.anomalousclasses.entity.PuppetEntity;
import kr.moonseungjun.anomalousclasses.item.PuppetItem;
import kr.moonseungjun.anomalousclasses.item.PuppetPartItem;
import kr.moonseungjun.anomalousclasses.network.ClassNetwork;
import kr.moonseungjun.anomalousclasses.network.payload.PuppetWorkshopStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.PuppetWorkshopActionPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Server-only craft/deploy/recall/break lifecycle. No client-supplied health,
 * ownership, item count, or puppet UUID can override the world registry.
 */
public final class PuppetService {
    private PuppetService() {}

    public static boolean isPuppeteer(ServerPlayer player) {
        return player != null && player.getServer() != null
                && ClassProfileSavedData.get(player.getServer()).profile(player.getUUID())
                    .currentClass().orElse(null) == ClassId.PUPPETEER;
    }

    /** R: select a hostile creature as the priority target for every loaded owned puppet. */
    public static void designateTarget(ServerPlayer player) {
        if (!isPuppeteer(player) || !player.isAlive()) return;
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        double range = 20.0 + .25 * PuppeteerRules.rank(profile, "connection_strength");
        Vec3 eye = player.getEyePosition();
        Vec3 ray = player.getLookAngle().scale(range);
        Vec3 end = eye.add(ray);
        var hit = ProjectileUtil.getEntityHitResult(player, eye, end,
                player.getBoundingBox().expandTowards(ray).inflate(1.0),
                entity -> entity instanceof Monster monster && monster.isAlive()
                        && !monster.isAlliedTo(player), range * range);
        if (hit == null || !(hit.getEntity() instanceof Monster target)) return;
        BlockHitResult wall = player.level().clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS
                && wall.getLocation().distanceToSqr(eye) < hit.getLocation().distanceToSqr(eye)) return;
        int count = 0;
        for (var entry : PuppetRegistrySavedData.get(player.getServer()).deployedBy(player.getUUID())) {
            Entity entity = player.serverLevel().getEntity(entry.blueprint().puppetId());
            if (entity instanceof PuppetEntity puppet && puppet.revision() == entry.revision()) {
                puppet.designateTarget(target.getUUID());
                count++;
            }
        }
        if (count > 0) message(player, "message.anomalousclasses.puppet.target");
    }

    /** Sneak + right click: server-toggle follow versus one persisted guard position. */
    public static boolean toggleHold(ServerPlayer owner, PuppetEntity puppet) {
        if (!isPuppeteer(owner) || !owner.getUUID().equals(puppet.puppetOwner())
                || !puppet.isAlive() || owner.level() != puppet.level()
                || owner.distanceToSqr(puppet) > 64.0 || CombatTracker.isLocked(owner))
            return false;
        PuppetRegistrySavedData ledger = PuppetRegistrySavedData.get(owner.getServer());
        var held = ledger.hold(puppet.puppetId());
        var location = new PuppetRegistrySavedData.HoldPosition(
                owner.serverLevel().dimension().location().toString(),
                puppet.getX(), puppet.getY(), puppet.getZ());
        if (!ledger.setHold(owner.getUUID(), puppet.puppetId(), puppet.revision(),
                held.isPresent() ? null : location)) return false;
        owner.displayClientMessage(Component.translatable(held.isPresent()
                ? "message.anomalousclasses.puppet.follow"
                : "message.anomalousclasses.puppet.stay"), true);
        return true;
    }

    /** A class change stores every puppet, including safely snapshotted unloaded puppets. */
    public static boolean prepareClassChange(ServerPlayer player, ClassId previous) {
        if (previous != ClassId.PUPPETEER) return true;
        PuppetRegistrySavedData registry = PuppetRegistrySavedData.get(player.getServer());
        List<PuppetRegistrySavedData.Entry> deployed = registry.deployedBy(player.getUUID());
        int empty = 0;
        for (int slot=0;slot<36;slot++) if(player.getInventory().getItem(slot).isEmpty()) empty++;
        if (empty < deployed.size()) {
            message(player, "message.anomalousclasses.puppet.no_space_class_change");
            return false;
        }
        // Capture live HP/attack recovery before transferring exclusive ownership to items.
        for (var entry : deployed) {
            PuppetEntity entity = loadedPuppet(player, entry.blueprint().puppetId());
            if (entity != null && entity.revision() == entry.revision()) entity.syncSnapshot();
        }
        for (var entry : registry.deployedBy(player.getUUID())) {
            PuppetEntity entity = loadedPuppet(player, entry.blueprint().puppetId());
            PuppetBlueprint snapshot = entity != null && entity.revision() == entry.revision()
                    ? entity.snapshot() : entry.blueprint();
            var stored = registry.recall(player.getUUID(), snapshot.puppetId(), entry.revision(), snapshot);
            if (stored.isEmpty()) throw new IllegalStateException("Puppet class-change state changed mid-tick");
            if (entity != null) entity.discard();
            ItemStack item = PuppetItem.make(snapshot, stored.get().revision(), false);
            if (!player.getInventory().add(item))
                throw new IllegalStateException("Puppet inventory reservation failed");
        }
        return true;
    }

    private static PuppetEntity loadedPuppet(ServerPlayer owner, UUID id) {
        for (ServerLevel world : owner.getServer().getAllLevels()) {
            Entity entity = world.getEntity(id);
            if (entity instanceof PuppetEntity puppet) return puppet;
        }
        return null;
    }

    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof PuppetEntity puppet) {
            puppet.syncSnapshot();
            PuppetAbilityService.stopPull(puppet.puppetId());
        }
    }

    private static boolean validWorkbench(ServerPlayer player, BlockPos station) {
        return player != null && player.getServer() != null
                && player.distanceToSqr(station.getX()+.5, station.getY()+.5, station.getZ()+.5) <= 64.0
                && player.serverLevel().getBlockState(station).is(PuppetBlocks.WORKSHOP.get());
    }

    public static void openWorkshop(ServerPlayer player, BlockPos station) {
        if (!validWorkbench(player, station)) return;
        sendWorkshopState(player, station, PuppetWorkshopStatePayload.OPEN);
    }

    public static void workshopAction(ServerPlayer player, PuppetWorkshopActionPayload request) {
        BlockPos station = request.station();
        if (!validWorkbench(player, station)) return;
        int outcome = PuppetWorkshopStatePayload.REJECTED;
        if (isPuppeteer(player) && !CombatTracker.isLocked(player)) {
            if (request.operation() == PuppetWorkshopActionPayload.CRAFT) {
                outcome = assembleStarter(player, station, request.craftDevice())
                        ? PuppetWorkshopStatePayload.CRAFTED : PuppetWorkshopStatePayload.REJECTED;
            } else {
                outcome = modifyStored(player, request);
            }
        }
        sendWorkshopState(player, station, outcome);
    }

    private record Held(PuppetItem.Identity item, PuppetRegistrySavedData.Entry entry) {}

    /** World ledger and the actual inventory slot must agree byte-for-byte. */
    private static Optional<Held> authenticHeld(ServerPlayer player, int slot, UUID claimed,
                                                 int revision) {
        if (slot < 0 || slot >= 36 || claimed == null || revision < 1) return Optional.empty();
        var item = PuppetItem.read(player.getInventory().getItem(slot)).orElse(null);
        if (item == null || item.revision() != revision
                || !item.blueprint().puppetId().equals(claimed)
                || !item.blueprint().ownerId().equals(player.getUUID())) return Optional.empty();
        var entry = PuppetRegistrySavedData.get(player.getServer()).find(claimed).orElse(null);
        if (entry == null || entry.revision() != revision
                || !entry.blueprint().equals(item.blueprint())
                || entry.location() != (item.broken() ?
                    PuppetRegistrySavedData.Location.BROKEN : PuppetRegistrySavedData.Location.STORED))
            return Optional.empty();
        return Optional.of(new Held(item, entry));
    }

    /** Stable 0..9 choices: three frames, three weapons, four drive cores. */
    private static PuppetPartItem.Kind refitKind(int choice) {
        return choice < 3 ? PuppetPartItem.Kind.FRAME :
                choice < 6 ? PuppetPartItem.Kind.DEVICE : PuppetPartItem.Kind.CORE;
    }

    private static int refitVariant(int choice) {
        return choice < 3 ? choice : choice < 6 ? choice - 3 : choice - 6;
    }

    private static int installedVariant(PuppetBlueprint blueprint, PuppetPartItem.Kind kind) {
        return switch (kind) {
            case FRAME -> blueprint.frame().ordinal();
            case DEVICE -> blueprint.device().ordinal();
            case CORE -> blueprint.core().ordinal();
        };
    }

    private static int matchingModuleSlot(Inventory inventory, PuppetPartItem.Kind kind,
                                           int variant, PuppetBlueprint.Size size) {
        for (int slot = 0; slot < 36; slot++) {
            var module = PuppetPartItem.read(inventory.getItem(slot)).orElse(null);
            if (module != null && module.kind() == kind && module.variant() == variant
                    && module.size() == (kind == PuppetPartItem.Kind.FRAME
                            ? size : PuppetBlueprint.Size.NORMAL))
                return slot;
        }
        return -1;
    }

    private static int modifyStored(ServerPlayer player, PuppetWorkshopActionPayload request) {
        Optional<Held> held = authenticHeld(player, request.inventorySlot(),
                request.puppetId(), request.expectedRevision());
        if (held.isEmpty()) return PuppetWorkshopStatePayload.REJECTED;
        PuppetItem.Identity item = held.get().item();
        PuppetBlueprint blueprint = item.blueprint();
        PuppetRegistrySavedData registry = PuppetRegistrySavedData.get(player.getServer());
        Inventory inventory = player.getInventory();
        var profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        int materialRanks = PuppeteerRules.rank(profile, "material_use");
        if (request.operation() == PuppetWorkshopActionPayload.REPAIR) {
            if (blueprint.healthRatio() >= 0.999999) return PuppetWorkshopStatePayload.REJECTED;
            Map<String, Integer> bill = PuppetEconomy.repairMaterials(blueprint, materialRanks);
            if (bill.isEmpty() || !hasMaterials(inventory, bill)) return PuppetWorkshopStatePayload.REJECTED;
            PuppetBlueprint repaired = blueprint.withCondition(1.0, blueprint.nextAttackTick());
            var revised = registry.reviseStored(player.getUUID(), blueprint.puppetId(), item.revision(),
                    blueprint, repaired, true);
            if (revised.isEmpty()) return PuppetWorkshopStatePayload.REJECTED;
            consume(inventory, bill);
            inventory.setItem(request.inventorySlot(), PuppetItem.make(repaired, revised.get().revision(), false));
            message(player, "message.anomalousclasses.puppet.repaired");
            return PuppetWorkshopStatePayload.REPAIRED;
        }
        if (request.operation() == PuppetWorkshopActionPayload.EXPAND) {
            if (item.broken() || blueprint.size() == PuppetBlueprint.Size.GIANT)
                return PuppetWorkshopStatePayload.REJECTED;
            PuppetBlueprint.Size next = PuppetBlueprint.Size.values()[blueprint.size().ordinal() + 1];
            Map<String,Integer> bill = PuppetEconomy.extraFrameMaterials(blueprint.frame(), blueprint.size(), next);
            if (bill.isEmpty() || !hasMaterials(inventory, bill)) return PuppetWorkshopStatePayload.REJECTED;
            PuppetBlueprint bigger = blueprint.withParts(blueprint.frame(), blueprint.device(),
                    blueprint.core(), next, PuppetEconomy.expandLedger(blueprint, next));
            var revised = registry.reviseStored(player.getUUID(), blueprint.puppetId(), item.revision(),
                    blueprint, bigger, false);
            if (revised.isEmpty()) return PuppetWorkshopStatePayload.REJECTED;
            consume(inventory, bill);
            inventory.setItem(request.inventorySlot(), PuppetItem.make(bigger, revised.get().revision(), false));
            message(player, "message.anomalousclasses.puppet.expanded");
            return PuppetWorkshopStatePayload.EXPANDED;
        }
        if (request.operation() == PuppetWorkshopActionPayload.REFIT) {
            int selected = request.craftDevice();
            if (item.broken() || selected < 0 || selected >= 10) return PuppetWorkshopStatePayload.REJECTED;
            PuppetPartItem.Kind kind = refitKind(selected);
            int variant = refitVariant(selected);
            int previous = installedVariant(blueprint, kind);
            if (previous == variant) return PuppetWorkshopStatePayload.REJECTED;
            PuppetBlueprint.Size partSize = kind == PuppetPartItem.Kind.FRAME
                    ? blueprint.size() : PuppetBlueprint.Size.NORMAL;
            Map<String,Integer> replacementLedger;
            try {
                replacementLedger = PuppetEconomy.partReplacementLedger(blueprint, kind, variant);
            } catch (IllegalArgumentException ex) {
                return PuppetWorkshopStatePayload.REJECTED;
            }
            Map<String,Integer> rawBill = PuppetEconomy.partMaterials(kind, variant, partSize);
            int reusable = matchingModuleSlot(inventory, kind, variant, blueprint.size());
            if (reusable < 0 && (inventory.getFreeSlot() < 0 || !hasMaterials(inventory, rawBill)))
                return PuppetWorkshopStatePayload.REJECTED;
            double condition = blueprint.healthRatio();
            if (reusable >= 0) {
                var module = PuppetPartItem.read(inventory.getItem(reusable)).orElseThrow();
                // A salvaged worn module may never heal the puppet when re-installed.
                condition = Math.min(condition, module.condition());
            }
            PuppetBlueprint changed = blueprint.withParts(
                    kind == PuppetPartItem.Kind.FRAME
                            ? PuppetBlueprint.Frame.values()[variant] : blueprint.frame(),
                    kind == PuppetPartItem.Kind.DEVICE
                            ? PuppetBlueprint.Device.values()[variant] : blueprint.device(),
                    kind == PuppetPartItem.Kind.CORE
                            ? PuppetBlueprint.Core.values()[variant] : blueprint.core(),
                    blueprint.size(), replacementLedger).withCondition(condition, blueprint.nextAttackTick());
            var updated = registry.reviseStored(player.getUUID(), blueprint.puppetId(), item.revision(),
                    blueprint, changed, false);
            if (updated.isEmpty()) return PuppetWorkshopStatePayload.REJECTED;
            if (reusable >= 0) inventory.getItem(reusable).shrink(1);
            else consume(inventory, rawBill);
            inventory.setItem(request.inventorySlot(), PuppetItem.make(changed, updated.get().revision(), false));
            // Old module is a reusable physical item. It is NOT refunded as raw ingredients.
            ItemStack oldModule = PuppetPartItem.create(kind, previous, partSize, blueprint.healthRatio());
            inventory.add(oldModule);
            if (!oldModule.isEmpty()) player.drop(oldModule, false);
            inventory.setChanged();
            message(player, "message.anomalousclasses.puppet.refitted");
            return PuppetWorkshopStatePayload.REFITTED;
        }
        if (request.operation() == PuppetWorkshopActionPayload.DISMANTLE) {
            Map<String,Integer> refund = PuppetEconomy.dismantleReturn(blueprint,
                    item.broken(), 0.006 * Math.min(25, materialRanks));
            if (!PuppetEconomy.refundNeverExceedsInput(blueprint, refund))
                return PuppetWorkshopStatePayload.REJECTED;
            var retired = registry.dismantle(player.getUUID(), blueprint.puppetId(), item.revision());
            if (retired.isEmpty()) return PuppetWorkshopStatePayload.REJECTED;
            inventory.setItem(request.inventorySlot(), ItemStack.EMPTY);
            // Every output is material from the historical ledger, never a new puppet.
            for (var entry : refund.entrySet()) {
                int remaining = entry.getValue();
                Item material = item(entry.getKey());
                while (remaining > 0) {
                    int amount = Math.min(material.getDefaultMaxStackSize(), remaining);
                    ItemStack stack = new ItemStack(material, amount);
                    inventory.add(stack);
                    if (!stack.isEmpty()) player.drop(stack, false);
                    remaining -= amount;
                }
            }
            message(player, "message.anomalousclasses.puppet.dismantled");
            return PuppetWorkshopStatePayload.DISMANTLED;
        }
        return PuppetWorkshopStatePayload.REJECTED;
    }

    private static void sendWorkshopState(ServerPlayer player, BlockPos station, int result) {
        Inventory inventory = player.getInventory();
        boolean ownerClass = isPuppeteer(player);
        boolean safe = ownerClass && !CombatTracker.isLocked(player);
        List<PuppetWorkshopStatePayload.CraftChoice> crafts = new ArrayList<>();
        for (PuppetBlueprint.Device device : PuppetBlueprint.Device.values()) {
            Map<String, Integer> bill = PuppetEconomy.starterBlueprintLedger(
                    PuppetBlueprint.Size.NORMAL, device);
            crafts.add(new PuppetWorkshopStatePayload.CraftChoice(device.ordinal(),
                    safe && inventory.getFreeSlot() >= 0 && hasMaterials(inventory, bill), bill));
        }
        boolean craftable = crafts.get(0).craftable();
        int materialRanks = ownerClass ? PuppeteerRules.rank(
                ClassProfileSavedData.get(player.getServer()).profile(player.getUUID()), "material_use") : 0;
        List<PuppetWorkshopStatePayload.PuppetView> views = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getItem(slot);
            var identity = PuppetItem.read(stack).orElse(null);
            if (identity == null) continue;
            var blueprint = identity.blueprint();
            if (authenticHeld(player, slot, blueprint.puppetId(), identity.revision()).isEmpty()) continue;
            Map<String, Integer> repair = PuppetEconomy.repairMaterials(blueprint, materialRanks);
            Map<String, Integer> expansion = !identity.broken()
                    && blueprint.size() != PuppetBlueprint.Size.GIANT
                    ? PuppetEconomy.extraFrameMaterials(blueprint.frame(), blueprint.size(),
                        PuppetBlueprint.Size.values()[blueprint.size().ordinal() + 1]) : Map.of();
            Map<String, Integer> refund = PuppetEconomy.dismantleReturn(blueprint,
                    identity.broken(), 0.006 * Math.min(25, materialRanks));
            List<PuppetWorkshopStatePayload.RefitChoice> refits = new ArrayList<>();
            for (int choice = 0; choice < 10; choice++) {
                PuppetPartItem.Kind kind = refitKind(choice);
                int variant = refitVariant(choice);
                PuppetBlueprint.Size moduleSize = kind == PuppetPartItem.Kind.FRAME
                        ? blueprint.size() : PuppetBlueprint.Size.NORMAL;
                Map<String,Integer> bill = PuppetEconomy.partMaterials(kind, variant, moduleSize);
                boolean installed = installedVariant(blueprint, kind) == variant;
                int reusable = matchingModuleSlot(inventory, kind, variant, blueprint.size());
                boolean possible = safe && !identity.broken() && !installed
                        && (reusable >= 0 || inventory.getFreeSlot() >= 0 && hasMaterials(inventory, bill));
                refits.add(new PuppetWorkshopStatePayload.RefitChoice(kind.ordinal(), variant,
                        possible, reusable >= 0, reusable >= 0 ? Map.of() : bill));
            }
            views.add(new PuppetWorkshopStatePayload.PuppetView(slot, blueprint.puppetId(),
                    identity.revision(), blueprint.size().ordinal(),
                    (int) Math.round(blueprint.healthRatio() * 100.0), identity.broken(),
                    safe && !repair.isEmpty() && hasMaterials(inventory, repair),
                    safe && !expansion.isEmpty() && hasMaterials(inventory, expansion),
                    safe, repair, expansion, refund, refits));
        }
        ClassNetwork.sendToPlayer(player, new PuppetWorkshopStatePayload(station, craftable, result, views, crafts));
    }

    public static boolean assembleStarter(ServerPlayer player, BlockPos station) {
        return assembleStarter(player, station, PuppetBlueprint.Device.BLADE.ordinal());
    }

    public static boolean assembleStarter(ServerPlayer player, BlockPos station, int deviceIndex) {
        if (!isPuppeteer(player) || !validWorkbench(player, station)
                || deviceIndex < 0 || deviceIndex >= PuppetBlueprint.Device.values().length) return false;
        PuppetBlueprint.Device device = PuppetBlueprint.Device.values()[deviceIndex];
        Inventory inventory = player.getInventory();
        if (inventory.getFreeSlot() < 0) {
            message(player, "message.anomalousclasses.puppet.no_space");
            return false;
        }
        Map<String,Integer> bill = PuppetEconomy.starterBlueprintLedger(PuppetBlueprint.Size.NORMAL, device);
        if (!hasMaterials(inventory, bill)) {
            message(player, "message.anomalousclasses.puppet.materials");
            return false;
        }
        // No visible item leaves the craft station until all material counts are proven.
        consume(inventory, bill);
        PuppetBlueprint puppet = new PuppetBlueprint(UUID.randomUUID(), player.getUUID(),
                PuppetBlueprint.Frame.LIGHT, device,
                PuppetBlueprint.Core.BASIC, PuppetBlueprint.Size.NORMAL, 1.0, 0L, bill);
        PuppetRegistrySavedData registry = PuppetRegistrySavedData.get(player.getServer());
        if (!registry.registerCrafted(puppet)) throw new IllegalStateException("Duplicate puppet UUID");
        ItemStack result = PuppetItem.make(puppet, 1, false);
        if (!inventory.add(result)) player.drop(result, false);
        message(player, "message.anomalousclasses.puppet.assembled");
        return true;
    }

    public static boolean deploy(ServerPlayer player, UseOnContext context) {
        if (!isPuppeteer(player)) return false;
        ItemStack item = context.getItemInHand();
        Optional<PuppetItem.Identity> identity = PuppetItem.read(item);
        if (identity.isEmpty() || identity.get().broken()) return false;
        PuppetBlueprint blueprint = identity.get().blueprint();
        if (!blueprint.ownerId().equals(player.getUUID())) return false;
        PuppetRegistrySavedData ledger = PuppetRegistrySavedData.get(player.getServer());
        var entry = ledger.find(blueprint.puppetId()).orElse(null);
        if (entry == null || entry.location() != PuppetRegistrySavedData.Location.STORED
                || entry.revision() != identity.get().revision()
                || !entry.blueprint().equals(blueprint)) return false;
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        if (ledger.deployedCount(player.getUUID()) >= PuppeteerRules.maxActive(
                profile.progress(ClassId.PUPPETEER).level(), profile)) {
            message(player, "message.anomalousclasses.puppet.slots");
            return false;
        }
        ServerLevel world = player.serverLevel();
        BlockPos location = context.getClickedPos().relative(context.getClickedFace());
        if (!world.getBlockState(location).canBeReplaced()) return false;
        PuppetEntity entity = PuppetEntities.MARIONETTE.get().create(world);
        if (entity == null) return false;
        entity.setUUID(blueprint.puppetId());
        entity.moveTo(location.getX()+.5, location.getY(), location.getZ()+.5,
                player.getYRot(), 0);
        entity.initialize(blueprint, entry.revision()+1, profile);
        if (!world.noCollision(entity) || !world.getWorldBorder().isWithinBounds(entity.getBoundingBox()))
            return false;
        if (!world.addFreshEntity(entity)) return false;
        var moved = ledger.deploy(player.getUUID(), blueprint.puppetId(), entry.revision());
        if (moved.isEmpty()) {
            entity.discard();
            return false;
        }
        item.shrink(1);
        message(player, "message.anomalousclasses.puppet.deployed");
        return true;
    }

    public static boolean recall(ServerPlayer player, PuppetEntity entity) {
        if (!isPuppeteer(player) || entity.puppetOwner() == null ||
                !entity.puppetOwner().equals(player.getUUID()) || !entity.isAlive()
                || entity.distanceToSqr(player) > 64.0 || CombatTracker.isLocked(player)
                || player.getInventory().getFreeSlot() < 0) return false;
        PuppetRegistrySavedData ledger = PuppetRegistrySavedData.get(player.getServer());
        PuppetBlueprint snapshot = entity.snapshot();
        var recorded = ledger.recall(player.getUUID(), snapshot.puppetId(), entity.revision(), snapshot);
        if (recorded.isEmpty()) return false;
        entity.discard();
        ItemStack item = PuppetItem.make(snapshot, recorded.get().revision(), false);
        if (!player.getInventory().add(item)) player.drop(item, false);
        message(player, "message.anomalousclasses.puppet.recalled");
        return true;
    }

    public static void breakEntity(PuppetEntity entity) {
        if (entity.puppetId() == null || !(entity.level() instanceof ServerLevel world)) return;
        PuppetBlueprint destroyed = entity.snapshot().withCondition(0, entity.snapshot().nextAttackTick());
        var changed = PuppetRegistrySavedData.get(world.getServer())
                .breakPuppet(destroyed.ownerId(), destroyed.puppetId(), entity.revision(), destroyed);
        changed.ifPresent(e -> entity.spawnAtLocation(PuppetItem.make(destroyed, e.revision(), true)));
    }

    static boolean hasMaterials(Inventory inventory, Map<String,Integer> bill) {
        for (var req : bill.entrySet()) {
            Item material = item(req.getKey());
            int owned = 0;
            for (int slot=0;slot<36;slot++) if(inventory.getItem(slot).is(material))
                owned += inventory.getItem(slot).getCount();
            if (owned < req.getValue()) return false;
        }
        return true;
    }

    static void consume(Inventory inventory, Map<String,Integer> bill) {
        for (var req : bill.entrySet()) {
            Item material = item(req.getKey());
            int left = req.getValue();
            for (int slot=0;slot<36 && left>0;slot++) {
                ItemStack stack=inventory.getItem(slot);
                if (!stack.is(material)) continue;
                int consumed=Math.min(stack.getCount(),left);
                stack.shrink(consumed);
                left-=consumed;
            }
            if (left != 0) throw new IllegalStateException("Material count changed within server tick");
        }
        inventory.setChanged();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}

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
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof PuppetEntity puppet)
            puppet.syncSnapshot();
    }

    public static boolean assembleStarter(ServerPlayer player, BlockPos station) {
        if (!isPuppeteer(player) || player.distanceToSqr(station.getX()+.5,
                station.getY()+.5, station.getZ()+.5) > 64
                || !player.serverLevel().getBlockState(station).is(PuppetBlocks.WORKSHOP.get())) return false;
        Inventory inventory = player.getInventory();
        if (inventory.getFreeSlot() < 0) {
            message(player, "message.anomalousclasses.puppet.no_space");
            return false;
        }
        Map<String,Integer> bill = PuppetEconomy.starterBlueprintLedger(PuppetBlueprint.Size.NORMAL);
        if (!hasMaterials(inventory, bill)) {
            message(player, "message.anomalousclasses.puppet.materials");
            return false;
        }
        // No visible item leaves the craft station until all material counts are proven.
        consume(inventory, bill);
        PuppetBlueprint puppet = new PuppetBlueprint(UUID.randomUUID(), player.getUUID(),
                PuppetBlueprint.Frame.LIGHT, PuppetBlueprint.Device.BLADE,
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

    private static boolean hasMaterials(Inventory inventory, Map<String,Integer> bill) {
        for (var req : bill.entrySet()) {
            Item material = item(req.getKey());
            int owned = 0;
            for (int slot=0;slot<36;slot++) if(inventory.getItem(slot).is(material))
                owned += inventory.getItem(slot).getCount();
            if (owned < req.getValue()) return false;
        }
        return true;
    }

    private static void consume(Inventory inventory, Map<String,Integer> bill) {
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

package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.block.PactAltarBlock;
import kr.moonseungjun.anomalousclasses.block.PactBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Real one-time fights at third- and fourth-tier altars; completed proof is permanently server saved. */
public final class PactTrialService {
    private static final String MARKER = "anomalousclasses_trial_guardian";
    private static final String OWNER = "anomalousclasses_trial_owner";
    private static final long MAX_TICKS = 20L * 150;
    private static MinecraftServer currentServer;
    private record Trial(String patron, UUID guardian, ServerLevel world, BlockPos altar, long expires) {}
    private static final Map<UUID, Trial> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> RECENT_HITS = new HashMap<>();
    private PactTrialService() {}

    private static void world(MinecraftServer server) {
        if (currentServer != server) {
            ACTIVE.clear();
            RECENT_HITS.clear();
            currentServer = server;
        }
    }
    private static boolean validAltar(ServerPlayer player, BlockPos pos, String patron) {
        return player.serverLevel().hasChunkAt(pos)
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 10.0 * 10.0
                && player.serverLevel().getBlockState(pos).is(PactBlocks.ALTAR_CORE.get())
                && PactAltarBlock.patronId(player.serverLevel().getBlockState(pos)).equals(patron);
    }
    public static boolean proved(ServerPlayer player, String patron) {
        return PactSavedData.get(player.getServer()).player(player.getUUID()).hasProof(patron);
    }
    public static void challenge(ServerPlayer player) {
        if (!PactService.isPact(player) || CombatTracker.isLocked(player)) {
            hint(player, "message.anomalousclasses.pact.unavailable");
            return;
        }
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(8.0));
        BlockHitResult block = player.serverLevel().clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.BLOCK) {
            hint(player, "message.anomalousclasses.pact.trial_aim"); return;
        }
        BlockPos pos = block.getBlockPos();
        if (!player.serverLevel().getBlockState(pos).is(PactBlocks.ALTAR_CORE.get())) {
            hint(player, "message.anomalousclasses.pact.trial_aim"); return;
        }
        String patron = PactAltarBlock.patronId(player.serverLevel().getBlockState(pos));
        PactCatalog.Patron config = PactCatalog.get(patron);
        if (config == null || (config.tier() != 3 && config.tier() != 4) || !config.enabled()
                || !validAltar(player, pos, patron)) {
            hint(player, "message.anomalousclasses.pact.trial_aim"); return;
        }
        if (proved(player, patron)) {
            hint(player, "message.anomalousclasses.pact.trial_completed"); return;
        }
        world(player.getServer());
        Trial existing = ACTIVE.get(player.getUUID());
        if (existing != null) {
            hint(player, "message.anomalousclasses.pact.trial_busy"); return;
        }
        EntityType<? extends Mob> type = switch (patron) {
            case "sevenwing" -> EntityType.VINDICATOR;
            case "merzad" -> EntityType.WITHER_SKELETON;
            case "taron", "ormas" -> EntityType.RAVAGER;
            case "nahar" -> EntityType.ENDERMAN;
            case "rakiel" -> EntityType.VINDICATOR;
            case "viar" -> EntityType.HUSK;
            default -> EntityType.PILLAGER;
        };
        Mob guardian = type.create(player.serverLevel());
        if (guardian == null) {
            hint(player, "message.anomalousclasses.pact.trial_invalid"); return;
        }
        double health = switch (patron) {
            case "sevenwing" -> 60.0;
            case "merzad" -> 66.0;
            case "taron" -> 95.0;
            case "nahar" -> 130.0;
            case "rakiel" -> 145.0;
            case "ormas" -> 180.0;
            case "viar" -> 160.0;
            default -> 52.0;
        };
        var hp = guardian.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(health);
        guardian.setHealth((float) health);
        // Raw EntityType#create does not guarantee ordinary natural-spawn equipment.
        // The guardians must actually be able to attack, especially pillager crossbows.
        switch (patron) {
            case "sevenwing", "rakiel" -> guardian.setItemSlot(
                    EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            case "merzad", "viar" -> guardian.setItemSlot(
                    EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
            case "arken" -> guardian.setItemSlot(
                    EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            default -> { } // Ravager already attacks without equipment.
        }
        guardian.setCanPickUpLoot(false);
        guardian.setCustomName(Component.translatable(
                "message.anomalousclasses.pact.guardian." + patron));
        guardian.setCustomNameVisible(true);
        guardian.getPersistentData().putBoolean(MARKER, true);
        guardian.getPersistentData().putUUID(OWNER, player.getUUID());
        guardian.setPersistenceRequired();
        boolean found = false;
        for (BlockPos offset : new BlockPos[]{
                pos.offset(5, 0, 0), pos.offset(-5, 0, 0),
                pos.offset(0, 0, 5), pos.offset(0, 0, -5),
                pos.offset(4, 0, 4), pos.offset(-4, 0, -4)}) {
            for (int dy = 0; dy <= 2; dy++) {
                BlockPos ground = offset.above(dy).below();
                BlockPos feet = ground.above();
                if (!player.serverLevel().hasChunkAt(feet)
                        || !player.serverLevel().getWorldBorder().isWithinBounds(feet)
                        || !player.serverLevel().getBlockState(ground).isSolid()) continue;
                guardian.moveTo(feet.getX() + .5, feet.getY(), feet.getZ() + .5, player.getYRot(), 0);
                if (player.serverLevel().noCollision(guardian, guardian.getBoundingBox())) {
                    found = true; break;
                }
            }
            if (found) break;
        }
        if (!found) {
            hint(player, "message.anomalousclasses.pact.trial_invalid"); return;
        }
        long expires = player.getServer().getTickCount() + (config.tier()==4 ? 20L*240 : MAX_TICKS);
        ACTIVE.put(player.getUUID(), new Trial(patron, guardian.getUUID(),
                player.serverLevel(), pos.immutable(), expires));
        if (!player.serverLevel().addFreshEntity(guardian)) {
            ACTIVE.remove(player.getUUID());
            hint(player, "message.anomalousclasses.pact.trial_invalid"); return;
        }
        guardian.setTarget(player);
        hint(player, "message.anomalousclasses.pact.trial_started");
    }
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Mob guardian)
                || !guardian.getPersistentData().getBoolean(MARKER)
                || event.getNewDamage() <= 0.0F
                || !(event.getSource().getEntity() instanceof ServerPlayer owner)) return;
        Trial trial=ACTIVE.get(owner.getUUID());
        if (trial != null && trial.guardian().equals(guardian.getUUID())) {
            RECENT_HITS.put(guardian.getUUID(), (long) owner.getServer().getTickCount());
        }
    }
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob guardian)
                || !guardian.getPersistentData().getBoolean(MARKER)
                || guardian.level().getServer() == null) return;
        MinecraftServer server = guardian.level().getServer();
        world(server);
        UUID ownerId = guardian.getPersistentData().getUUID(OWNER);
        Trial trial = ACTIVE.get(ownerId);
        if (trial == null || !trial.guardian().equals(guardian.getUUID())) return;
        ACTIVE.remove(ownerId);
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        long recent = RECENT_HITS.getOrDefault(guardian.getUUID(), -10_000L);
        RECENT_HITS.remove(guardian.getUUID());
        if (owner == null || !PactService.isPact(owner)
                || !owner.level().equals(trial.world())
                || owner.distanceToSqr(Vec3.atCenterOf(trial.altar())) > 48 * 48
                || !trial.world().hasChunkAt(trial.altar())
                || !trial.world().getBlockState(trial.altar()).is(PactBlocks.ALTAR_CORE.get())
                || !PactAltarBlock.patronId(trial.world().getBlockState(trial.altar()))
                        .equals(trial.patron())
                || (event.getSource().getEntity() instanceof ServerPlayer killer
                        ? !killer.getUUID().equals(ownerId)
                        : server.getTickCount() - recent > 100L)) return;
        if (PactSavedData.get(server).completeTrial(ownerId, trial.patron())) {
            RpgExperienceService.awardFirst(owner, "pact_trial:" + trial.patron(), 1600L);
            hint(owner, "message.anomalousclasses.pact.trial_completed");
        }
    }
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity().getPersistentData().getBoolean(MARKER))
            event.getDrops().clear();
    }
    public static void onExperience(LivingExperienceDropEvent event) {
        if (event.getEntity().getPersistentData().getBoolean(MARKER))
            event.setDroppedExperience(0);
    }
    public static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide() || !entity.getPersistentData().getBoolean(MARKER)
                || entity.level().getServer() == null) return;
        MinecraftServer server = entity.level().getServer();
        world(server);
        UUID ownerId = entity.getPersistentData().getUUID(OWNER);
        Trial trial = ACTIVE.get(ownerId);
        if (trial == null || !trial.guardian().equals(entity.getUUID()))
            entity.discard(); // Remove orphaned trial mobs after a server restart or chunk reload.
    }
    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        world(server);
        if (server.getTickCount() % 20 != 0) return;
        Iterator<Map.Entry<UUID, Trial>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            var row = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(row.getKey());
            Trial trial = row.getValue();
            Entity entity = trial.world().getEntity(trial.guardian());
            if (player == null || !PactService.isPact(player)
                    || player.level() != trial.world()
                    || player.distanceToSqr(Vec3.atCenterOf(trial.altar())) > 48 * 48
                    || server.getTickCount() >= trial.expires()
                    || !(entity instanceof Mob guardian) || !guardian.isAlive()) {
                if (entity != null) entity.discard();
                RECENT_HITS.remove(trial.guardian());
                it.remove();
                continue;
            }
            guardian.setTarget(player);
        }
    }
    public static void clear(ServerPlayer player) {
        world(player.getServer());
        Trial trial = ACTIVE.remove(player.getUUID());
        if (trial != null) {
            RECENT_HITS.remove(trial.guardian());
            Entity entity = trial.world().getEntity(trial.guardian());
            if (entity != null) entity.discard();
        }
    }
    private static void hint(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), false);
    }
}

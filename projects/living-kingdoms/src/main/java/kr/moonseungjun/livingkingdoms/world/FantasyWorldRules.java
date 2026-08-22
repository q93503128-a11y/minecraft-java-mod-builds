package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.economy.RealmEconomyManager;
import kr.moonseungjun.livingkingdoms.foundation.FoundationCatalog;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Realm-wide gameplay rules for the Erden-only playable slice.
 *
 * <p>The authored kingdom is a civic fantasy simulation, not a vanilla survival sandbox. Civic
 * production, institutional recovery, settlement safety and shared world time remain authoritative.
 * Creative and spectator players are left untouched so builders and diagnostics remain usable.</p>
 */
public final class FantasyWorldRules {
    public static final int RULES_REVISION = 1;
    public static final long RECOVERY_FEE_SILVER = 8L;
    private static final int CAPITAL_SAFETY_BUFFER = 32;

    private static final Set<Block> VANILLA_WORKSTATIONS = Set.of(
            Blocks.CRAFTING_TABLE,
            Blocks.FURNACE,
            Blocks.BLAST_FURNACE,
            Blocks.SMOKER,
            Blocks.SMITHING_TABLE,
            Blocks.ANVIL,
            Blocks.CHIPPED_ANVIL,
            Blocks.DAMAGED_ANVIL,
            Blocks.GRINDSTONE,
            Blocks.STONECUTTER,
            Blocks.BREWING_STAND,
            Blocks.ENCHANTING_TABLE,
            Blocks.LOOM,
            Blocks.CARTOGRAPHY_TABLE,
            Blocks.CRAFTER
    );

    private static final Set<String> VANILLA_PRODUCTION_MENUS = Set.of(
            "CraftingMenu",
            "FurnaceMenu",
            "BlastFurnaceMenu",
            "SmokerMenu",
            "SmithingMenu",
            "AnvilMenu",
            "GrindstoneMenu",
            "StonecutterMenu",
            "BrewingStandMenu",
            "EnchantmentMenu",
            "LoomMenu",
            "CartographyTableMenu",
            "CrafterMenu"
    );

    private static final Set<String> AMBIENT_HOSTILE_SPAWN_REASONS = Set.of(
            "NATURAL", "CHUNK_GENERATION", "PATROL"
    );

    private static final Map<UUID, Long> LAST_RULE_MESSAGE = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN = 40L;

    private FantasyWorldRules() {
    }

    /** Fails fast if an unfinished origin is accidentally re-exposed in the playable slice. */
    public static void audit(MinecraftServer server) {
        FoundationCatalog.bootstrap();
        if (FoundationCatalog.species().size() != 1
                || FoundationCatalog.homelands().size() != 1
                || FoundationCatalog.backgrounds().size() != 1
                || PlayableOriginCatalog.residences().size() != 1
                || !FoundationCatalog.species().containsKey(PlayableOriginCatalog.DEFAULT_SPECIES)
                || !FoundationCatalog.homelands().containsKey(PlayableOriginCatalog.DEFAULT_HOMELAND)
                || !FoundationCatalog.backgrounds().containsKey(PlayableOriginCatalog.DEFAULT_BACKGROUND)
                || !PlayableOriginCatalog.residences().containsKey(PlayableOriginCatalog.DEFAULT_RESIDENCE)) {
            throw new IllegalStateException("Erden gameplay rules lock was violated by an unfinished playable origin");
        }
        if (ErdenRegionalSettlementCatalog.settlements().size() != ErdenRegionalSettlementCatalog.SETTLEMENT_COUNT) {
            throw new IllegalStateException("Erden settlement safety catalog drifted");
        }
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_GAMEPLAY_RULES_LOCK revision={} erden_only=true playable_species=1 playable_homelands=1 playable_backgrounds=1 playable_residences=1 sleep_skip=false bed_respawn=false shared_clock=true institutional_defeat=true recovery_fee={} item_loss=false personal_crafting=false vanilla_workstations=false capital_safe=true settlement_safe=true settlements={} road_danger=true wild_danger=true scripted_spawns_preserved=true initial_placement_teleport_only=true multiplayer_world_shared=true",
                RULES_REVISION, RECOVERY_FEE_SILVER, ErdenRegionalSettlementCatalog.SETTLEMENT_COUNT
        );
    }

    public static void tick(ServerPlayer player) {
        if (!insideRealm(player)) return;

        // Hunger is not a combat timer here. Meals, inns and feasts are social/economic content.
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);

        // Personal 2x2 crafting is server-authoritatively disabled. Inputs are not consumed.
        if (!player.inventoryMenu.slots.isEmpty()) {
            var result = player.inventoryMenu.getSlot(0);
            if (result.hasItem()) {
                result.set(ItemStack.EMPTY);
                player.inventoryMenu.broadcastChanges();
                notifyRule(player, "물건은 손바닥 위에서 조합하지 않습니다. 허가된 공방이나 장인을 이용하십시오.");
            }
        }

        // Defense in depth if another mod or command opened a vanilla production menu.
        if (player.containerMenu != player.inventoryMenu
                && VANILLA_PRODUCTION_MENUS.contains(player.containerMenu.getClass().getSimpleName())) {
            player.closeContainer();
            notifyRule(player, "이 설비의 원래 조합법은 폐지되었습니다. 지역 공방의 작업 주문을 이용하십시오.");
        }
    }

    public static void handleWorkstation(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !insideRealm(player)) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!VANILLA_WORKSTATIONS.contains(event.getLevel().getBlockState(event.getPos()).getBlock())) return;

        event.setCanceled(true);
        notifyRule(player, "개인 제작은 허용되지 않습니다. 길드·공방·연금술원에 작업을 의뢰하십시오.");
    }

    /**
     * Beds remain physical furniture, but cannot erase the kingdom clock. This also avoids one
     * multiplayer client advancing every resident, market and shipment for all other players.
     */
    public static void handleSleep(CanPlayerSleepEvent event) {
        ServerPlayer player = event.getEntity();
        if (!insideRealm(player) || player.isCreative() || player.isSpectator()) return;
        event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
        notifyRule(player, "에르덴의 시간은 모든 주민과 플레이어가 공유합니다. 침대로 밤을 건너뛸 수 없습니다.");
    }

    /** Recovery is institutional, so a vanilla bed/command spawn must not become a free checkpoint. */
    public static void handleSpawnPoint(PlayerSetSpawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !insideRealm(player)) return;
        if (player.isCreative() || player.isSpectator()) return;
        event.setCanceled(true);
        notifyRule(player, "에르덴에서는 개인 부활 지점을 지정하지 않습니다. 구조와 치료는 지역 제도가 담당합니다.");
    }

    /**
     * Prevents ambient hostile generation inside the walled capital and the six authored regional
     * settlements. Roads stay dangerous (but guarded), and wilderness remains fully dangerous.
     * Scripted/command/spawner encounters are intentionally not blocked.
     */
    public static void handleMobSpawn(MobSpawnEvent.SpawnPlacementCheck event) {
        ServerLevel level = event.getLevel().getLevel();
        if (!level.dimension().equals(StarterRealmManager.REALM_KEY)
                || event.getEntityType().getCategory() != MobCategory.MONSTER
                || !AMBIENT_HOSTILE_SPAWN_REASONS.contains(event.getSpawnType().name())
                || !insideCivilianSafetyZone(event.getPos())) {
            return;
        }
        event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
    }

    /** Capital and regional settlement fabric is protected; surveyed wilderness gathering remains possible. */
    public static void handleBlockBreak(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !insideRealm(player)) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!(player.level() instanceof ServerLevel) || !insideCivilianSafetyZone(event.getPos())) return;
        event.setCanceled(true);
        event.setNotifyClient(true);
        notifyRule(player, "도시·마을 건축물과 공공 기반시설은 시민 재산입니다. 채집은 성외 허가 구역에서 하십시오.");
    }

    /**
     * Converts lethal damage into institutional recovery. World time is never jumped: in multiplayer
     * the recovery cost is personal silver plus temporary weakness/slowness while the shared kingdom
     * simulation continues normally.
     */
    public static boolean handleDefeat(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !insideRealm(player)) return false;
        if (player.isCreative() || player.isSpectator()) return false;

        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        OriginProfile profile = OriginProfileManager.profile(player.getUUID()).orElse(null);
        if (realm == null || profile == null) return false;

        event.setCanceled(true);
        BlockPos recovery = RealmSitePlanner.residencePosition(
                realm, profile.homelandId(), profile.residenceId()
        );
        long balance = RealmEconomyManager.account(player).silver();
        long charged = Math.min(balance, RECOVERY_FEE_SILVER);
        if (charged > 0L) RealmEconomyManager.spend(player, charged);

        player.removeAllEffects();
        player.setHealth(Math.max(1.0F, player.getMaxHealth() * 0.30F));
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        player.teleportTo(
                realm,
                recovery.getX() + 0.5D,
                recovery.getY() + 0.2D,
                recovery.getZ() + 0.5D,
                Set.<Relative>of(),
                player.getYRot(),
                player.getXRot(),
                true
        );
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 45, 1, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 20, 0, false, false, true));
        player.sendSystemMessage(Component.literal(
                "전투불능 상태에서 구조되어 거주지로 후송되었습니다. 치료·구조비 은화 " + charged
                        + "을 지불했고 소지품은 보존됩니다."
        ));
        return true;
    }

    public static boolean insideCivilianSafetyZone(BlockPos position) {
        int x = position.getX();
        int z = position.getZ();
        boolean capital = x >= ErdenCapitalStreamingBuilder.WEST_WALL_X - CAPITAL_SAFETY_BUFFER
                && x <= ErdenCapitalStreamingBuilder.EAST_WALL_X + CAPITAL_SAFETY_BUFFER
                && z >= ErdenCapitalStreamingBuilder.NORTH_WALL_Z - CAPITAL_SAFETY_BUFFER
                && z <= ErdenCapitalStreamingBuilder.SOUTH_WALL_Z + CAPITAL_SAFETY_BUFFER;
        return capital || ErdenRegionalSettlementCatalog.settlementAt(x, z) != null;
    }

    private static boolean insideRealm(ServerPlayer player) {
        return player.level().dimension().equals(StarterRealmManager.REALM_KEY)
                && OriginProfileManager.profile(player.getUUID()).isPresent();
    }

    private static void notifyRule(ServerPlayer player, String message) {
        long now = player.level().getGameTime();
        long previous = LAST_RULE_MESSAGE.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2L);
        if (now - previous < MESSAGE_COOLDOWN) return;
        LAST_RULE_MESSAGE.put(player.getUUID(), now);
        player.sendSystemMessage(Component.literal(message));
    }
}

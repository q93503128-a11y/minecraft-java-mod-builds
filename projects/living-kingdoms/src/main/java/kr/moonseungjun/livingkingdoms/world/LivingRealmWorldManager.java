package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.economy.RealmEconomyManager;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.network.RealmBuildProgressPayload;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Entry point for queued Erden preparation and final verified player placement. */
public final class LivingRealmWorldManager {
    private static final int PLACEMENT_RETRY_INTERVAL = 5;
    private static final Map<UUID, OriginProfile> PENDING_PLACEMENTS = new ConcurrentHashMap<>();

    private LivingRealmWorldManager() {
    }

    public static void requestPlacement(ServerPlayer player, OriginProfile profile) {
        RealmBuildCoordinator.requestPlayer(player, profile);
    }

    static boolean finishPlacement(ServerPlayer player, OriginProfile profile) {
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) {
            LivingKingdoms.LOGGER.error("Living Kingdoms realm is not loaded");
            return false;
        }
        if (!PlayableOriginCatalog.DEFAULT_HOMELAND.equals(profile.homelandId())) {
            LivingKingdoms.LOGGER.error("Rejected inactive homeland placement {} for {}",
                    profile.homelandId(), player.getUUID());
            return false;
        }
        PlayableOriginCatalog.ResidenceOption residence = PlayableOriginCatalog.residences().get(profile.residenceId());
        if (residence == null) return false;
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(realm, profile.homelandId());
        if (site == null || !site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) return false;

        ConstructionDebrisCleaner.schedule(realm, profile.homelandId(), site);
        BlockPos feet = SafeResidenceLocator.residenceIfReady(
                realm, profile.homelandId(), profile.residenceId());
        if (feet == null) {
            queuePending(player, profile);
            return false;
        }
        if (!SafeResidenceLocator.isWalkable(realm, feet)) {
            LivingKingdoms.LOGGER.error("Rejected unsafe authored residence spawn {} for {}", feet, player.getUUID());
            queuePending(player, profile);
            return false;
        }
        float yaw = SafeResidenceLocator.yaw(profile.homelandId(), profile.residenceId());
        boolean moved = player.teleportTo(realm,
                feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D,
                Set.<Relative>of(), yaw, 0.0F, true);
        if (moved) {
            PENDING_PLACEMENTS.remove(player.getUUID());
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            player.fallDistance = 0.0F;
            SelectionStagingManager.cleanupLegacyPlatform(realm);
            player.sendSystemMessage(Component.literal(
                    "§6[살아있는 왕국] §f" + residence.displayName() + "에서 에르덴 왕국 시민으로 삶을 시작합니다."
            ));
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_PLAYER_ENTRY_PASS player={} residence={} position={} actual_tenement=true authored_home_target=true synthetic_floor=false arbitrary_roof_scan=false staging_platform=false",
                    player.getUUID(), profile.residenceId(), feet);
        } else {
            queuePending(player, profile);
        }
        return moved;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING_PLACEMENTS.isEmpty()
                || event.getServer().getTickCount() % PLACEMENT_RETRY_INTERVAL != 0) return;
        for (Map.Entry<UUID, OriginProfile> entry : Map.copyOf(PENDING_PLACEMENTS).entrySet()) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) continue;
            OriginProfile current = OriginProfileManager.profile(entry.getKey()).orElse(null);
            if (current == null) {
                PENDING_PLACEMENTS.remove(entry.getKey());
                continue;
            }
            ServerLevel realm = event.getServer().getLevel(StarterRealmManager.REALM_KEY);
            if (realm == null || !RealmSitePlanner.isBuilt(realm, current.homelandId())) continue;
            if (!finishPlacement(player, current)) continue;

            StarterNpcManager.ensureForPlayer(player, current);
            RealmEconomyManager.sync(player);
            PacketDistributor.sendToPlayer(player, new RealmBuildProgressPayload(
                    current.homelandId(), "complete", 100,
                    "실제 시민구 거주지 확인이 끝났습니다. 왕도에서 삶을 시작합니다.", true, false
            ));
        }
    }

    static boolean isPlacementPending(UUID playerId) {
        return PENDING_PLACEMENTS.containsKey(playerId);
    }

    private static void queuePending(ServerPlayer player, OriginProfile profile) {
        OriginProfile previous = PENDING_PLACEMENTS.put(player.getUUID(), profile);
        if (previous != null) return;
        PacketDistributor.sendToPlayer(player, new RealmBuildProgressPayload(
                profile.homelandId(), "residence", 99,
                "실제 시민 주택 내부와 출입 동선을 확인하고 있습니다.", false, false
        ));
        LivingKingdoms.LOGGER.info(
                "Queued authored Erden residence placement player={} synthetic_staging=false",
                player.getUUID());
    }

    public static BlockPos homePosition(ServerLevel realm, OriginProfile profile) {
        return SafeResidenceLocator.residence(realm, profile.homelandId(), profile.residenceId());
    }
}

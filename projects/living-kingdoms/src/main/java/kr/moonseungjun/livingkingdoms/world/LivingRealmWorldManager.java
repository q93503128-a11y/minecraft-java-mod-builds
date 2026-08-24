package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.economy.RealmEconomyManager;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Entry point for queued Erden preparation and final verified player placement. */
public final class LivingRealmWorldManager {
    private static final Map<UUID, OriginProfile> PENDING_RESIDENCE = new ConcurrentHashMap<>();

    private LivingRealmWorldManager() {
    }

    public static void requestPlacement(ServerPlayer player, OriginProfile profile) {
        RealmBuildCoordinator.requestPlayer(player, profile);
    }

    /** Called from the normal player tick until an authored apartment interior becomes available. */
    public static void retryPendingPlacement(ServerPlayer player) {
        OriginProfile pending = PENDING_RESIDENCE.get(player.getUUID());
        if (pending == null || player.level().getGameTime() % 10L != 0L) return;
        OriginProfile current = OriginProfileManager.profile(player.getUUID()).orElse(null);
        if (current == null || !current.equals(pending)) {
            PENDING_RESIDENCE.remove(player.getUUID());
            return;
        }
        if (finishPlacement(player, current)) {
            StarterNpcManager.ensureForPlayer(player, current);
            RealmEconomyManager.sync(player);
        }
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
        BlockPos feet = SafeResidenceLocator.residence(realm, profile.homelandId(), profile.residenceId());
        if (!SafeResidenceLocator.isWalkable(realm, feet)) {
            if (PENDING_RESIDENCE.putIfAbsent(player.getUUID(), profile) == null) {
                LivingKingdoms.LOGGER.info(
                        "Deferred player placement until authored residence is ready player={} residence={} synthetic_fallback=false",
                        player.getUUID(), profile.residenceId());
            }
            return false;
        }
        float yaw = SafeResidenceLocator.yaw(profile.homelandId(), profile.residenceId());
        boolean moved = player.teleportTo(realm,
                feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D,
                Set.<Relative>of(), yaw, 0.0F, true);
        if (moved) {
            PENDING_RESIDENCE.remove(player.getUUID());
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            player.fallDistance = 0.0F;
            LivingKingdoms.LOGGER.info(
                    "LK_PLAYER_RESIDENCE_PLACEMENT player={} target={} authored_interior=true synthetic_fallback=false",
                    player.getUUID(), feet);
            player.sendSystemMessage(Component.literal(
                    "§6[살아있는 왕국] §f" + residence.displayName() + "에서 에르덴 왕국 시민으로 삶을 시작합니다."
            ));
        }
        return moved;
    }

    public static BlockPos homePosition(ServerLevel realm, OriginProfile profile) {
        return SafeResidenceLocator.residence(realm, profile.homelandId(), profile.residenceId());
    }
}

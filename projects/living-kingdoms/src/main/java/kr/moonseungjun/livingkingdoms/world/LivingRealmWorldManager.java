package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;

import java.util.Set;

/** Entry point for queued Erden preparation and final verified player placement. */
public final class LivingRealmWorldManager {
    private LivingRealmWorldManager() {
    }

    public static void requestPlacement(ServerPlayer player, OriginProfile profile) {
        RealmBuildCoordinator.requestPlayer(player, profile);
    }

    static void prepareResidence(ServerLevel realm, OriginProfile profile) {
        SafeResidenceLocator.prepareResidence(realm, profile.homelandId(), profile.residenceId());
    }

    /**
     * Attempts final placement only into the same authored tenement topology used by capital
     * households. Returning false means construction is still pending; callers must never announce
     * completion or invent a synthetic fallback.
     */
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
        BlockPos feet = SafeResidenceLocator.tryResidence(realm, profile.homelandId(), profile.residenceId());
        if (feet == null) return false;
        if (!SafeResidenceLocator.isWalkable(realm, feet)) {
            LivingKingdoms.LOGGER.error("Rejected unsafe authored residence spawn {} for {}", feet, player.getUUID());
            return false;
        }

        float yaw = SafeResidenceLocator.yaw(profile.homelandId(), profile.residenceId());
        boolean moved = player.teleportTo(realm,
                feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D,
                Set.<Relative>of(), yaw, 0.0F, true);
        if (moved) {
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            player.fallDistance = 0.0F;
            ExternalUrbanFabricBuilder.UrbanEntrance entrance =
                    SafeResidenceLocator.residenceEntrance(realm, profile.homelandId(), profile.residenceId());
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_PLAYER_RESIDENCE player={} role={} entrance={},{} feet={} authored_tenement=true upper_route=true synthetic_fallback=false staging_exit=true",
                    player.getUUID(), entrance.role(), entrance.x(), entrance.z(), feet);
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

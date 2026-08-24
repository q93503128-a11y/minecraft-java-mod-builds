package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import kr.moonseungjun.livingkingdoms.profile.ResidenceAssignment;
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

    /**
     * Compatibility entry used by the realm build coordinator. A false physical attempt is converted
     * into a residence-barrier request, so old coordinator completion packets cannot release the
     * client before the actual room is available.
     */
    static boolean finishPlacement(ServerPlayer player, OriginProfile profile) {
        if (tryFinishPlacement(player, profile)) return true;
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm != null && RealmSitePlanner.isBuilt(realm, profile.homelandId())
                && !PlayerResidencePlacementManager.pending(player.getUUID())) {
            PlayerResidencePlacementManager.queue(player, profile);
        }
        return false;
    }

    /** Makes one physical attempt. Never creates a fallback and never recursively queues itself. */
    static boolean tryFinishPlacement(ServerPlayer player, OriginProfile profile) {
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
        if (feet == null) return false;

        ExternalUrbanFabricBuilder.UrbanEntrance entrance = SafeResidenceLocator.starterResidenceEntrance(
                realm, profile.homelandId(), profile.residenceId());
        float yaw = SafeResidenceLocator.yaw(profile.homelandId(), profile.residenceId());
        boolean moved = player.teleportTo(realm,
                feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D,
                Set.<Relative>of(), yaw, 0.0F, true);
        if (moved) {
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            player.fallDistance = 0.0F;
            OriginProfileManager.putResidenceAssignment(player.getUUID(), new ResidenceAssignment(
                    ResidenceAssignment.CURRENT_REVISION,
                    feet.getX(), feet.getY(), feet.getZ(), yaw,
                    entrance.x(), entrance.z()
            ));
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_PLAYER_RESIDENCE_PLACEMENT player={} target={},{},{} building={},{} role=tenement verified_authored=true roof_fallback=false staging=false synthetic_floor=false revision={}",
                    player.getUUID(), feet.getX(), feet.getY(), feet.getZ(),
                    entrance.x(), entrance.z(), ResidenceAssignment.CURRENT_REVISION
            );
            player.sendSystemMessage(Component.literal(
                    "§6[살아있는 왕국] §f" + residence.displayName() + "에서 에르덴 왕국 시민으로 삶을 시작합니다."
            ));
        }
        return moved;
    }

    /** Restores an already-proven home without scanning roofs, walls or terrain again. */
    public static boolean restoreAssignedResidence(ServerPlayer player, OriginProfile profile) {
        ResidenceAssignment assignment = OriginProfileManager.residenceAssignment(player.getUUID())
                .filter(ResidenceAssignment::current)
                .orElse(null);
        if (assignment == null) return false;
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) return false;
        boolean moved = player.teleportTo(realm,
                assignment.x() + 0.5D, assignment.y(), assignment.z() + 0.5D,
                Set.<Relative>of(), assignment.yaw(), 0.0F, true);
        if (moved) {
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            player.fallDistance = 0.0F;
        }
        return moved;
    }

    public static BlockPos homePosition(ServerLevel realm, ServerPlayer player, OriginProfile profile) {
        ResidenceAssignment assignment = OriginProfileManager.residenceAssignment(player.getUUID())
                .filter(ResidenceAssignment::current)
                .orElse(null);
        if (assignment != null) return assignment.position();
        return SafeResidenceLocator.residence(realm, profile.homelandId(), profile.residenceId());
    }

    /** Legacy helper retained for non-player callers; it now returns only a verified authored room. */
    public static BlockPos homePosition(ServerLevel realm, OriginProfile profile) {
        return SafeResidenceLocator.residence(realm, profile.homelandId(), profile.residenceId());
    }
}

package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.network.RealmBuildProgressPayload;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;

/** Entry point for queued Erden preparation and final verified player placement. */
public final class LivingRealmWorldManager {
    private LivingRealmWorldManager() {
    }

    public static void requestPlacement(ServerPlayer player, OriginProfile profile) {
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm != null && RealmSitePlanner.isBuilt(realm, profile.homelandId())) {
            if (finishPlacement(player, profile)) {
                StarterNpcManager.ensureForPlayer(player, profile);
                PacketDistributor.sendToPlayer(player, new RealmBuildProgressPayload(
                        profile.homelandId(), "complete", 100,
                        "실제 시민구 거주지 확인을 마쳤습니다. 왕국 생활을 시작합니다.", true, false));
            } else {
                SelectionStagingManager.ensure(player);
                PacketDistributor.sendToPlayer(player, new RealmBuildProgressPayload(
                        profile.homelandId(), "residence", 99,
                        "시민구의 실제 주거 내부와 진입 동선을 확인하고 있습니다.", false, false));
            }
            return;
        }
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
        BlockPos feet = SafeResidenceLocator.residence(realm, profile.homelandId(), profile.residenceId());
        if (feet == null) return false;
        if (!SafeResidenceLocator.isWalkable(realm, feet)) {
            LivingKingdoms.LOGGER.error("Rejected unsafe authored final residence spawn {} for {}", feet, player.getUUID());
            return false;
        }
        float yaw = SafeResidenceLocator.yaw(profile.homelandId(), profile.residenceId());
        boolean moved = player.teleportTo(realm,
                feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D,
                Set.<Relative>of(), yaw, 0.0F, true);
        if (moved) {
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            player.fallDistance = 0.0F;
            player.sendSystemMessage(Component.literal(
                    "§6[살아있는 왕국] §f" + residence.displayName() + "의 실제 주거 내부에서 에르덴 왕국 시민으로 삶을 시작합니다."
            ));
        }
        return moved;
    }

    public static BlockPos homePosition(ServerLevel realm, OriginProfile profile) {
        return SafeResidenceLocator.residence(realm, profile.homelandId(), profile.residenceId());
    }
}

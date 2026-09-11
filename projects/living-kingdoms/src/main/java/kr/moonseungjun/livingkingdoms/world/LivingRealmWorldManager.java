package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;

import java.util.Set;
import java.util.function.Consumer;

/** Entry point for queued Erden preparation and final verified player placement. */
public final class LivingRealmWorldManager {
    private LivingRealmWorldManager() {
    }

    public static void requestPlacement(ServerPlayer player, OriginProfile profile) {
        RealmBuildCoordinator.requestPlayer(player, profile);
    }

    static void finishPlacementWhenReady(ServerPlayer player, OriginProfile profile, Consumer<Boolean> completion) {
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) {
            LivingKingdoms.LOGGER.error("Living Kingdoms realm is not loaded");
            completion.accept(false);
            return;
        }
        if (!PlayableOriginCatalog.DEFAULT_HOMELAND.equals(profile.homelandId())) {
            LivingKingdoms.LOGGER.error("Rejected inactive homeland placement {} for {}",
                    profile.homelandId(), player.getUUID());
            completion.accept(false);
            return;
        }
        PlayableOriginCatalog.ResidenceOption residence = PlayableOriginCatalog.residences().get(profile.residenceId());
        if (residence == null || !PlayableOriginCatalog.DEFAULT_RESIDENCE.equals(profile.residenceId())) {
            LivingKingdoms.LOGGER.error("Rejected inactive residence placement {} for {}",
                    profile.residenceId(), player.getUUID());
            completion.accept(false);
            return;
        }
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(realm, profile.homelandId());
        if (site == null || !site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) {
            completion.accept(false);
            return;
        }

        BlockPos preferred;
        try {
            preferred = SafeResidenceLocator.preferredResidence(realm, profile.homelandId(), profile.residenceId());
        } catch (Throwable throwable) {
            LivingKingdoms.LOGGER.error("Unable to resolve preferred Erden residence for {}", player.getUUID(), throwable);
            completion.accept(false);
            return;
        }
        ChunkPos chunk = new ChunkPos(preferred.getX() >> 4, preferred.getZ() >> 4);

        try {
            realm.getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, chunk, 1)
                    .whenComplete((ignored, failure) -> realm.getServer().execute(() -> {
                        boolean moved = false;
                        try {
                            if (failure != null) {
                                LivingKingdoms.LOGGER.error(
                                        "Failed to load final Erden residence chunk {},{} for {}",
                                        chunk.x(), chunk.z(), player.getUUID(), failure);
                                return;
                            }
                            ServerPlayer current = realm.getServer().getPlayerList().getPlayer(player.getUUID());
                            if (current == null) return;
                            if (!realm.hasChunk(chunk.x(), chunk.z())) {
                                LivingKingdoms.LOGGER.error(
                                        "Residence ticket completed without loaded chunk {},{} for {}",
                                        chunk.x(), chunk.z(), current.getUUID());
                                return;
                            }
                            moved = finishPlacementLoaded(current, profile, realm, residence);
                        } catch (Throwable throwable) {
                            LivingKingdoms.LOGGER.error("Final Erden residence placement failed for {}",
                                    player.getUUID(), throwable);
                        } finally {
                            realm.getChunkSource().removeTicketWithRadius(TicketType.PORTAL, chunk, 1);
                            completion.accept(moved);
                        }
                    }));
        } catch (Throwable throwable) {
            realm.getChunkSource().removeTicketWithRadius(TicketType.PORTAL, chunk, 1);
            LivingKingdoms.LOGGER.error("Unable to request final Erden residence chunk {},{} for {}",
                    chunk.x(), chunk.z(), player.getUUID(), throwable);
            completion.accept(false);
        }
    }

    private static boolean finishPlacementLoaded(ServerPlayer player, OriginProfile profile,
                                                 ServerLevel realm,
                                                 PlayableOriginCatalog.ResidenceOption residence) {
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(realm, profile.homelandId());
        if (site == null || !site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) return false;

        ConstructionDebrisCleaner.schedule(realm, profile.homelandId(), site);
        BlockPos feet = SafeResidenceLocator.residence(realm, profile.homelandId(), profile.residenceId());
        if (!SafeResidenceLocator.isWalkable(realm, feet)) {
            LivingKingdoms.LOGGER.error("Rejected unsafe final residence spawn {} for {}", feet, player.getUUID());
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
                    "§6[살아있는 왕국] §f" + residence.displayName() + "에서 에르덴 왕국 시민으로 삶을 시작합니다."
            ));
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_RESIDENCE_PLACEMENT_PASS chunk={},{} walkable=true transient_ticket=true ticket_radius=1",
                    feet.getX() >> 4, feet.getZ() >> 4);
        }
        return moved;
    }

    public static BlockPos homePosition(ServerLevel realm, OriginProfile profile) {
        return SafeResidenceLocator.residence(realm, profile.homelandId(), profile.residenceId());
    }
}

package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

/** Entry point for queued noise-realm preparation and final player placement. */
public final class LivingRealmWorldManager {
    private static final int SAFE_PLACEMENT_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;

    private LivingRealmWorldManager() {
    }

    public static void requestPlacement(ServerPlayer player, OriginProfile profile) {
        RealmBuildCoordinator.requestPlayer(player, profile);
    }

    static boolean finishPlacement(ServerPlayer player, OriginProfile profile) {
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) {
            LivingKingdoms.LOGGER.error("Living Kingdoms noise realm is not loaded");
            return false;
        }
        PlayableOriginCatalog.ResidenceOption residence = PlayableOriginCatalog.residences().get(profile.residenceId());
        if (residence == null) return false;
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(realm, profile.homelandId());
        if (site == null || !site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) return false;

        // The first placement occurs immediately after capital completion. This removes the exact
        // pathological plant debris seen in visual tests while leaving ordinary player drops alone.
        ConstructionDebrisCleaner.cleanIfPathological(realm, profile.homelandId(), site);

        BlockPos feet = RealmSitePlanner.residencePosition(realm, profile.homelandId(), profile.residenceId());
        makeSafe(realm, feet);
        boolean moved = player.teleportTo(realm, feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5,
                Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
        if (moved) {
            player.sendSystemMessage(Component.literal(
                    "§6[살아있는 왕국] §f" + residence.displayName() + "에서 "
                            + affiliation(profile.homelandId()) + " 소속으로 삶을 시작합니다."
            ));
        }
        return moved;
    }

    public static BlockPos homePosition(ServerLevel realm, OriginProfile profile) {
        return RealmSitePlanner.residencePosition(realm, profile.homelandId(), profile.residenceId());
    }

    private static void makeSafe(ServerLevel level, BlockPos feet) {
        BlockPos floor = feet.below();
        if (level.getBlockState(floor).isAir()) {
            level.setBlock(floor, Blocks.STONE_BRICKS.defaultBlockState(), SAFE_PLACEMENT_FLAGS);
        }
        level.setBlock(feet, Blocks.AIR.defaultBlockState(), SAFE_PLACEMENT_FLAGS);
        level.setBlock(feet.above(), Blocks.AIR.defaultBlockState(), SAFE_PLACEMENT_FLAGS);
        level.setBlock(feet.above(2), Blocks.AIR.defaultBlockState(), SAFE_PLACEMENT_FLAGS);
    }

    private static String affiliation(String homelandId) {
        return switch (homelandId) {
            case "silvana_forest" -> "실바나 수림 의회";
            case "kardum_league" -> "카르둠 산악 연맹";
            default -> "에르덴 왕국 로엔 변경백령";
        };
    }
}

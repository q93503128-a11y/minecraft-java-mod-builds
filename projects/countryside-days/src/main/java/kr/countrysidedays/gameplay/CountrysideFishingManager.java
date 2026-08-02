package kr.countrysidedays.gameplay;

import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.PlayerEstateLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Allows countryside fishing only in the long public river west of the village. */
public final class CountrysideFishingManager {
    private CountrysideFishingManager() {
    }

    public static boolean isAllowed(ServerLevel level, BlockPos hookPos) {
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        for (CountrysideWorldData.PlayerEstate estate : data.estates()) {
            if (PlayerEstateLayout.contains(estate.originPos(), hookPos)) return false;
        }
        BlockPos village = data.homesteadOrigin().orElse(null);
        return village != null && isPublicRiver(village, hookPos) && isWaterNear(level, hookPos);
    }

    public static boolean isPublicRiver(BlockPos villageOrigin, BlockPos pos) {
        int dx = pos.getX() - villageOrigin.getX();
        int dz = pos.getZ() - villageOrigin.getZ();
        return dx >= -67 && dx <= -55 && dz >= -13 && dz <= 45;
    }

    /** Compatibility alias used by old tests and saves. */
    public static boolean isPublicFishingPond(BlockPos villageOrigin, BlockPos pos) {
        return isPublicRiver(villageOrigin, pos);
    }

    public static BlockPos riverLandmark(BlockPos villageOrigin) {
        return villageOrigin.offset(-57, 1, 34);
    }

    private static boolean isWaterNear(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.WATER)
                || level.getBlockState(pos.below()).is(Blocks.WATER)
                || level.getBlockState(pos.above()).is(Blocks.WATER);
    }
}

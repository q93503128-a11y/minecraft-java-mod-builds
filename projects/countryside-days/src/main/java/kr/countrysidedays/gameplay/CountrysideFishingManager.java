package kr.countrysidedays.gameplay;

import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.PlayerEstateLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Allows countryside fishing only at the public fishing pond. */
public final class CountrysideFishingManager {
    private CountrysideFishingManager() {
    }

    public static boolean isAllowed(ServerLevel level, BlockPos hookPos) {
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());

        for (CountrysideWorldData.PlayerEstate estate : data.estates()) {
            if (PlayerEstateLayout.contains(estate.originPos(), hookPos)) return false;
        }

        BlockPos village = data.homesteadOrigin().orElse(null);
        if (village == null || !isPublicFishingPond(village, hookPos)) return false;
        return isWaterNear(level, hookPos);
    }

    public static boolean isPublicFishingPond(BlockPos villageOrigin, BlockPos pos) {
        int dx = pos.getX() - villageOrigin.getX();
        int dz = pos.getZ() - villageOrigin.getZ();
        return dx >= -50 && dx <= -34 && dz >= 30 && dz <= 44;
    }

    private static boolean isWaterNear(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.WATER)
                || level.getBlockState(pos.below()).is(Blocks.WATER)
                || level.getBlockState(pos.above()).is(Blocks.WATER);
    }
}

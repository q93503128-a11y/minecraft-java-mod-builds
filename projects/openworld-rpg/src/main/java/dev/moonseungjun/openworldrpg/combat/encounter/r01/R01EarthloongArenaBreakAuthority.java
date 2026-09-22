package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;

public final class R01EarthloongArenaBreakAuthority {
    public static final TagKey<Block> BREAKABLE_PROPS = TagKey.create(
            Registries.BLOCK,
            Identifier.parse("openworld_rpg:r01_earthloong_breakable_prop")
    );

    private R01EarthloongArenaBreakAuthority() {
    }

    public static int breakTaggedProps(
            ServerLevel level,
            LivingEntity earthloong,
            double horizontalRadius
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(earthloong, "earthloong");
        if (!Double.isFinite(horizontalRadius) || horizontalRadius < 0.0) {
            throw new IllegalArgumentException("horizontalRadius must be finite and non-negative.");
        }

        BlockPos center = earthloong.blockPosition();
        int ceil = (int) Math.ceil(horizontalRadius);
        int broken = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-ceil, -2, -ceil),
                center.offset(ceil, 3, ceil)
        )) {
            double dx = pos.getX() + 0.5 - earthloong.getX();
            double dz = pos.getZ() + 0.5 - earthloong.getZ();
            if (dx * dx + dz * dz > horizontalRadius * horizontalRadius) {
                continue;
            }
            if (level.getBlockState(pos).is(BREAKABLE_PROPS)
                    && level.destroyBlock(pos, false, earthloong)) {
                broken++;
            }
        }
        return broken;
    }
}

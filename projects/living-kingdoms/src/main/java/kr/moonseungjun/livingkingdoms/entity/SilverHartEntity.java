package kr.moonseungjun.livingkingdoms.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.level.Level;

/** Northern Erden herd animal. Uses goat locomotion while keeping its own registered species id. */
public final class SilverHartEntity extends Goat {
    public SilverHartEntity(EntityType<? extends Goat> type, Level level) {
        super(type, level);
    }
}

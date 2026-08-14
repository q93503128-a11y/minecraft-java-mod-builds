package kr.moonseungjun.livingkingdoms.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.Level;

/** Ambient Silver River spirit with its own entity identity and allay-like flight locomotion. */
public final class RiverWispEntity extends Allay {
    public RiverWispEntity(EntityType<? extends Allay> type, Level level) {
        super(type, level);
    }
}

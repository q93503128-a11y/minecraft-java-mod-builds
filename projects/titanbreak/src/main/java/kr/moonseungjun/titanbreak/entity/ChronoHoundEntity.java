package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.TemporalRated;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

public final class ChronoHoundEntity extends Zombie implements TemporalRated, TitanGeoEntity {
    public ChronoHoundEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    public int temporalRating() {
        return 55;
    }
}

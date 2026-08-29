package kr.moonseungjun.titanbreak.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class BulwarkEntity extends Zombie {
    public BulwarkEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker != null) {
            Vec3 toAttacker = attacker.position().subtract(position());
            if (toAttacker.horizontalDistanceSqr() > 1.0E-6D) {
                Vec3 facing = getLookAngle();
                Vec3 direction = toAttacker.normalize();
                double dot = facing.x * direction.x + facing.z * direction.z;
                if (dot > 0.35D) amount *= 0.28F;
            }
        }
        return super.hurtServer(level, source, amount);
    }
}

package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Routes magical damage through an attacker-bearing source so vanilla retaliation works. */
public final class ArcaneDamage {
    private ArcaneDamage() {}

    public static boolean hurt(ServerLevel level, ServerPlayer caster, LivingEntity target, float amount) {
        if (target == null || !target.isAlive() || amount <= 0.0F) return false;
        boolean damaged = target.hurtServer(level, level.damageSources().playerAttack(caster), amount);
        if (damaged && target instanceof Mob mob && target != caster) {
            mob.setTarget(caster);
        }
        return damaged;
    }

    public static boolean hurt(ServerLevel level, LivingEntity caster, LivingEntity target, float amount) {
        if (target == null || !target.isAlive() || amount <= 0.0F) return false;
        boolean damaged = target.hurtServer(level, level.damageSources().mobAttack(caster), amount);
        if (damaged && target instanceof Mob mob && target != caster) {
            mob.setTarget(caster);
        }
        return damaged;
    }
}

package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Routes magical damage through an attacker-bearing source so vanilla retaliation works. */
public final class ArcaneDamage {
    private static final ThreadLocal<Integer> RESOLVING = ThreadLocal.withInitial(() -> 0);

    private ArcaneDamage() {}

    /** True only while an ArcaneDamage hurtServer call is synchronously resolving its damage event. */
    public static boolean isResolving() {
        return RESOLVING.get() > 0;
    }

    public static boolean hurt(ServerLevel level, ServerPlayer caster, LivingEntity target, float amount) {
        if (target == null || !target.isAlive() || amount <= 0.0F) return false;
        int depth = RESOLVING.get();
        RESOLVING.set(depth + 1);
        boolean damaged;
        try {
            damaged = target.hurtServer(level, level.damageSources().playerAttack(caster), amount);
        } finally {
            if (depth == 0) RESOLVING.remove(); else RESOLVING.set(depth);
        }
        if (damaged && target instanceof Mob mob && target != caster) mob.setTarget(caster);
        return damaged;
    }

    public static boolean hurt(ServerLevel level, LivingEntity caster, LivingEntity target, float amount) {
        if (caster instanceof ServerPlayer player) return hurt(level, player, target, amount);
        if (target == null || !target.isAlive() || amount <= 0.0F) return false;
        int depth = RESOLVING.get();
        RESOLVING.set(depth + 1);
        boolean damaged;
        try {
            damaged = target.hurtServer(level, level.damageSources().mobAttack(caster), amount);
        } finally {
            if (depth == 0) RESOLVING.remove(); else RESOLVING.set(depth);
        }
        if (damaged && target instanceof Mob mob && target != caster) mob.setTarget(caster);
        return damaged;
    }

    /** Records only server-authoritative post-release damage for an already-open deferred spell ledger. */
    public static boolean hurtAttributed(ServerLevel level, LivingEntity caster, LivingEntity target,
                                         float amount, String spellId) {
        ServerPlayer player = caster instanceof ServerPlayer value ? value : null;
        Mob mob = target instanceof Mob value ? value : null;
        boolean track = player != null && mob != null && CombatGrowthService.attributableTarget(player, mob);
        float before = target == null ? 0.0F : target.getHealth() + target.getAbsorptionAmount();
        boolean damaged = hurt(level, caster, target, amount);
        if (track && damaged) {
            float after = target.isAlive() && !target.isRemoved()
                    ? Math.max(0.0F, target.getHealth() + target.getAbsorptionAmount()) : 0.0F;
            double actual = Math.max(0.0, before - after);
            if (actual > .001) CombatGrowthService.recordAttributed(player, spellId, mob, actual,
                    !target.isAlive() || target.isRemoved() || after <= .001F);
        }
        return damaged;
    }
}

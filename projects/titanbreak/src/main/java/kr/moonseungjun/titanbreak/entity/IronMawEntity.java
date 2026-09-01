package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Heavy elite that converts repeated contact into armor-independent internal shock and a short grab. */
public final class IronMawEntity extends Zombie implements TemporalRated, TitanGeoEntity {
    private UUID impactVictim;
    private int impactStacks;
    private int impactDecay;
    private int grabTicks;
    private int grabbedEntityId = -1;

    public IronMawEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        xpReward = 28;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public int temporalRating() {
        return 0;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (grabTicks > 0) return false;
        boolean hit = super.doHurtTarget(level, target);
        if (!hit || !(target instanceof ServerPlayer player)) return hit;

        if (!player.getUUID().equals(impactVictim) || impactDecay <= 0) {
            impactVictim = player.getUUID();
            impactStacks = 0;
        }
        impactStacks = Math.min(5, impactStacks + 1);
        impactDecay = 90;

        // Supplemental internal shock is deliberately non-lethal. The actual melee hit remains the lethal source,
        // while this portion bypasses ordinary armor and grows with repeated contact.
        float internalShock = (float) CombatScale.toInternal(2.0D + impactStacks * 1.6D);
        player.setHealth(Math.max(1.0F, player.getHealth() - internalShock));

        if (impactStacks >= 3) {
            grabTicks = 14;
            grabbedEntityId = player.getId();
            impactStacks = 0;
            swing(InteractionHand.MAIN_HAND);
        }
        return true;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (impactDecay > 0 && --impactDecay == 0) {
            impactStacks = 0;
            impactVictim = null;
        }
        if (grabTicks <= 0) return;

        Entity entity = level.getEntity(grabbedEntityId);
        if (!(entity instanceof ServerPlayer player) || !player.isAlive() || distanceToSqr(player) > 7.0D * 7.0D) {
            grabTicks = 0;
            grabbedEntityId = -1;
            return;
        }

        grabTicks--;
        Vec3 anchor = position().add(getLookAngle().scale(1.1D)).add(0.0D, 1.0D, 0.0D);
        Vec3 pull = anchor.subtract(player.position());
        if (pull.lengthSqr() > 0.01D) {
            pull = pull.normalize().scale(0.46D);
            player.setDeltaMovement(pull.x, Math.max(-0.12D, Math.min(0.22D, pull.y)), pull.z);
            player.hurtMarked = true;
        }

        if (grabTicks == 0) {
            player.hurtServer(level, damageSources().mobAttack(this), (float) CombatScale.toInternal(12.0D));
            Vec3 away = player.position().subtract(position());
            if (away.lengthSqr() > 1.0E-6D) {
                away = away.normalize();
                player.push(away.x * 0.9D, 0.28D, away.z * 0.9D);
                player.hurtMarked = true;
            }
            grabbedEntityId = -1;
        }
    }
}

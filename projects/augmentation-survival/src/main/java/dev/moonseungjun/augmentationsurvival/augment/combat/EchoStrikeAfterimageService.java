package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
import dev.moonseungjun.augmentationsurvival.mixin.CombatExpansionServiceInvoker;
import dev.moonseungjun.augmentationsurvival.mixin.MannequinEchoAccessor;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;

/** Immediate server-owned Echo Strike hit with a short-lived copy of the attacking player. */
public final class EchoStrikeAfterimageService {
    private static final String ECHO_STRIKE_ID = AugmentationSurvival.MOD_ID + ":echo_strike";
    private static final int CLONE_LIFETIME_TICKS = 5;
    private static final ThreadLocal<Boolean> APPLYING_ECHO = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final List<ActiveClone> ACTIVE_CLONES = new ArrayList<>();
    private static boolean initialized;

    private EchoStrikeAfterimageService() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(EchoStrikeAfterimageService::tick);
        ServerPlayerEvents.JOIN.register(EchoStrikeAfterimageService::clearPlayerClones);
        ServerPlayerEvents.LEAVE.register(EchoStrikeAfterimageService::clearPlayerClones);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> clearPlayerClones(newPlayer));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ACTIVE_CLONES.clear();
            APPLYING_ECHO.remove();
        });
    }

    /**
     * Mirrors one successful root melee hit. The echo owns its damage and hit-count interactions,
     * but never executes Player.attack again, so durability, vanilla enchant callbacks and Better
     * Combat combo state are not consumed twice.
     */
    public static void onRootMelee(
            ServerPlayer attacker,
            LivingEntity target,
            AugmentationPlayerState state,
            float baseDamageTaken,
            long now
    ) {
        int level = Math.max(0, Math.min(3, state.level(ECHO_STRIKE_ID)));
        if (level <= 0 || Boolean.TRUE.equals(APPLYING_ECHO.get())
                || target == null || target.isDeadOrDying()
                || !(baseDamageTaken > 0.0F) || !Float.isFinite(baseDamageTaken)) {
            return;
        }

        float echoDamage = (float) (baseDamageTaken * ExpansionTuning.current().echoStrikeDamageFraction(level));
        if (!(echoDamage > 0.0F) || !Float.isFinite(echoDamage)) return;

        ServerLevel levelWorld = (ServerLevel) attacker.level();
        spawnClone(levelWorld, attacker, target, now);

        int previousInvulnerableTime = target.invulnerableTime;
        target.invulnerableTime = 0;
        boolean applied;
        APPLYING_ECHO.set(Boolean.TRUE);
        try {
            applied = CombatProvenanceContext.callAs(
                    CombatProvenance.ECHO,
                    () -> target.hurtOrSimulate(
                            attacker.damageSources().playerAttack(attacker),
                            echoDamage
                    )
            );
        } finally {
            APPLYING_ECHO.remove();
        }
        if (!applied) {
            target.invulnerableTime = previousInvulnerableTime;
            return;
        }

        // Echo is a second melee contact for hit-count augments, but not a second raw Player.attack.
        LightningDirectHitService.onDirectHit(attacker, target, state, echoDamage);
        CombatExpansionServiceInvoker.augmentationSurvival$updateWeaknessMark(attacker, target, state, now);
        MovementAugmentService.onSuccessfulRootMelee(attacker, target);
        cueImpact(levelWorld, target);
    }

    private static void spawnClone(ServerLevel level, ServerPlayer attacker, LivingEntity target, long now) {
        Mannequin clone = Mannequin.create(EntityTypes.MANNEQUIN, level);
        if (clone == null) return;

        MannequinEchoAccessor bridge = (MannequinEchoAccessor) (Object) clone;
        bridge.augmentationSurvival$setProfile(ResolvableProfile.createResolved(attacker.getGameProfile()));
        bridge.augmentationSurvival$setImmovable(true);
        bridge.augmentationSurvival$setHideDescription(true);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            clone.setItemSlot(slot, attacker.getItemBySlot(slot).copy());
        }

        Vec3 look = attacker.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-6D) horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        else horizontal = horizontal.normalize();
        Vec3 side = new Vec3(-horizontal.z, 0.0D, horizontal.x).scale(0.72D);
        Vec3 behind = horizontal.scale(-0.22D);
        Vec3 position = attacker.position().add(side).add(behind);

        clone.setPos(position.x, position.y, position.z);
        clone.setYRot(attacker.getYRot());
        clone.setXRot(attacker.getXRot());
        clone.setYHeadRot(attacker.getYHeadRot());
        clone.setNoGravity(true);
        clone.setInvulnerable(true);
        clone.setSilent(true);
        clone.noPhysics = true;
        level.addFreshEntity(clone);
        clone.swing(InteractionHand.MAIN_HAND);
        ACTIVE_CLONES.add(new ActiveClone(attacker.getUUID(), clone.getUUID(), level, now + CLONE_LIFETIME_TICKS));

        Vec3 flash = clone.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                flash.x, flash.y, flash.z,
                7, 0.24D, 0.42D, 0.24D, 0.035D);
    }

    private static void cueImpact(ServerLevel level, LivingEntity target) {
        Vec3 hit = target.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                hit.x, hit.y, hit.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                hit.x, hit.y, hit.z,
                8, 0.24D, 0.30D, 0.24D, 0.055D);
        level.playSound(null,
                hit.x, hit.y, hit.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.62F, 1.38F);
        level.playSound(null,
                hit.x, hit.y, hit.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.28F, 1.72F);
    }

    private static void tick(MinecraftServer server) {
        Iterator<ActiveClone> iterator = ACTIVE_CLONES.iterator();
        while (iterator.hasNext()) {
            ActiveClone active = iterator.next();
            if (active.level().getGameTime() < active.removeTick()) continue;
            Entity entity = active.level().getEntity(active.cloneId());
            if (entity != null) entity.discard();
            iterator.remove();
        }
    }

    private static void clearPlayerClones(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Iterator<ActiveClone> iterator = ACTIVE_CLONES.iterator();
        while (iterator.hasNext()) {
            ActiveClone active = iterator.next();
            if (!active.ownerId().equals(playerId)) continue;
            Entity entity = active.level().getEntity(active.cloneId());
            if (entity != null) entity.discard();
            iterator.remove();
        }
    }

    private record ActiveClone(UUID ownerId, UUID cloneId, ServerLevel level, long removeTick) {
    }
}

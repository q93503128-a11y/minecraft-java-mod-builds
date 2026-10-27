package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.presentation.PassivePresentationService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reports reactive passive procs after the underlying server runtime has actually accepted them. */
@Mixin(value = ReactiveCombatAugmentService.class, remap = false)
abstract class ReactivePassivePresentationMixin {
    private static final String RETURN_TO_SENDER = PassivePresentationService.id("return_to_sender");
    private static final String STATUS_REFLECT = PassivePresentationService.id("status_reflect");
    private static final String STRONG_MIND = PassivePresentationService.id("strong_mind");
    private static final String SHIELD_BURST = PassivePresentationService.id("shield_burst");
    private static final String RETRACE = PassivePresentationService.id("retrace");
    private static final String INVINCIBILITY = PassivePresentationService.id("invincibility");
    private static final ThreadLocal<Long> RETRACE_READY_BEFORE = new ThreadLocal<>();
    private static final ThreadLocal<Vec3> RETRACE_POSITION_BEFORE = new ThreadLocal<>();

    @Inject(method = "interceptIncomingEffect", at = @At("RETURN"))
    private static void augmentationSurvival$presentEffectPassives(
            ServerPlayer victim,
            MobEffectInstance incoming,
            Entity source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (incoming == null
                || incoming.isInfiniteDuration()
                || incoming.getDuration() <= 0
                || incoming.getEffect().value().getCategory() != MobEffectCategory.HARMFUL
                || ReactiveCombatAugmentService.isInvulnerable(victim)) {
            return;
        }
        if (owns(victim, STATUS_REFLECT) && isEnemySource(victim, source)) {
            PassivePresentationService.proc(victim, STATUS_REFLECT, source);
        }
        if (owns(victim, STRONG_MIND) && cir.getReturnValue() != null) {
            PassivePresentationService.proc(victim, STRONG_MIND);
        }
    }

    @Inject(method = "tryReturnProjectile", at = @At("RETURN"))
    private static void augmentationSurvival$presentReturnToSender(
            ServerPlayer player,
            DamageSource source,
            CallbackInfo ci
    ) {
        if (source.getDirectEntity() instanceof Projectile projectile
                && ReactiveCombatAugmentService.isReflectedProjectile(projectile)) {
            PassivePresentationService.proc(player, RETURN_TO_SENDER, projectile);
        }
    }

    @Inject(method = "onProtectionPoolEnded", at = @At("RETURN"))
    private static void augmentationSurvival$presentShieldBurst(
            ServerPlayer player,
            float effectivePoolAmount,
            CallbackInfo ci
    ) {
        if (effectivePoolAmount > 0.0F && owns(player, SHIELD_BURST)) {
            PassivePresentationService.proc(player, SHIELD_BURST);
        }
    }

    @Inject(method = "triggerRetrace", at = @At("HEAD"))
    private static void augmentationSurvival$captureRetraceState(
            ServerPlayer attacker,
            float baseDamageTaken,
            CallbackInfo ci
    ) {
        RETRACE_READY_BEFORE.set(
                AugmentationPlayerStateService.state(attacker).cooldownReadyTick(RETRACE)
        );
        RETRACE_POSITION_BEFORE.set(attacker.position());
    }

    @Inject(method = "triggerRetrace", at = @At("RETURN"))
    private static void augmentationSurvival$presentRetrace(
            ServerPlayer attacker,
            float baseDamageTaken,
            CallbackInfo ci
    ) {
        Long readyBefore = RETRACE_READY_BEFORE.get();
        Vec3 positionBefore = RETRACE_POSITION_BEFORE.get();
        RETRACE_READY_BEFORE.remove();
        RETRACE_POSITION_BEFORE.remove();
        if (readyBefore != null
                && positionBefore != null
                && AugmentationPlayerStateService.state(attacker).cooldownReadyTick(RETRACE) > readyBefore) {
            PassivePresentationService.procPath(attacker, RETRACE, positionBefore, attacker.position());
        }
    }

    @Inject(method = "tickRuntime", at = @At("RETURN"))
    private static void augmentationSurvival$observeInvincibility(
            MinecraftServer server,
            CallbackInfo ci
    ) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PassivePresentationService.observeState(
                    player,
                    INVINCIBILITY,
                    ReactiveCombatAugmentService.isInvulnerable(player)
            );
        }
    }

    private static boolean isEnemySource(ServerPlayer victim, Entity source) {
        if (!(source instanceof LivingEntity living) || source == victim || victim.isAlliedTo(source)) return false;
        return source instanceof ServerPlayer || CompanionAugmentService.isHostileTarget(victim, living);
    }

    private static boolean owns(ServerPlayer player, String augmentId) {
        return AugmentationPlayerStateService.state(player).level(augmentId) > 0;
    }
}

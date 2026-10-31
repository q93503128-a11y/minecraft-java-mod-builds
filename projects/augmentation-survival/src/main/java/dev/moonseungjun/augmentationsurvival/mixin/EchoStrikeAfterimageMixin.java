package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.EchoStrikeAfterimageService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Converts Echo Strike's same-tick derived damage into a distinct delayed second melee hit. */
@Mixin(value = CombatExpansionService.class, remap = false)
abstract class EchoStrikeAfterimageMixin {
    private static volatile boolean betterCombatReflectionChecked;
    private static volatile Method betterCombatGetComboCount;
    private static volatile Method betterCombatGetCurrentAttack;

    @Inject(method = "initialize", at = @At("TAIL"))
    private static void augmentationSurvival$initializeEchoAfterimages(CallbackInfo ci) {
        EchoStrikeAfterimageService.initialize();
    }

    @Inject(method = "triggerEchoStrike", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$routeEchoAfterimage(
            ServerPlayer attacker,
            LivingEntity target,
            AugmentationPlayerState state,
            float baseDamageTaken,
            long now,
            CallbackInfo ci
    ) {
        ci.cancel();
        EchoStrikeAfterimageService.onChargedMelee(attacker, target, state, baseDamageTaken, now);
    }

    /**
     * Better Combat deliberately executes valid combo hits before vanilla's 0.90 charged-hit gate.
     * CombatExpansionService therefore skips those hits even though Better Combat already accepted
     * the swing and routed it through Player.attack. Bridge only that missing sub-0.90 path; vanilla
     * fully charged melee keeps using triggerEchoStrike above and cannot be double-counted here.
     */
    @Inject(method = "afterDamage", at = @At("TAIL"))
    private static void augmentationSurvival$bridgeBetterCombatEchoHit(
            LivingEntity target,
            DamageSource source,
            float baseDamageTaken,
            float damageTaken,
            boolean blocked,
            CallbackInfo ci
    ) {
        if (blocked || !(damageTaken > 0.0F) || !(source.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        if (CombatProvenanceContext.resolve(source) != CombatProvenance.ROOT_MELEE) {
            return;
        }
        if (attacker.getAttackStrengthScale(0.5F) >= 0.90F || !augmentationSurvival$isBetterCombatAttack(attacker)) {
            return;
        }
        EchoStrikeAfterimageService.onChargedMelee(
                attacker,
                target,
                AugmentationPlayerStateService.state(attacker),
                baseDamageTaken,
                attacker.level().getGameTime()
        );
    }

    private static boolean augmentationSurvival$isBetterCombatAttack(ServerPlayer player) {
        augmentationSurvival$ensureBetterCombatReflection();
        if (betterCombatGetComboCount == null || betterCombatGetCurrentAttack == null) {
            return false;
        }
        try {
            int comboCount = ((Number) betterCombatGetComboCount.invoke(player)).intValue();
            if (comboCount < 0) {
                return false;
            }
            return betterCombatGetCurrentAttack.invoke(null, (Player) player, comboCount) != null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    private static void augmentationSurvival$ensureBetterCombatReflection() {
        if (betterCombatReflectionChecked) {
            return;
        }
        synchronized (EchoStrikeAfterimageMixin.class) {
            if (betterCombatReflectionChecked) {
                return;
            }
            betterCombatReflectionChecked = true;
            try {
                Class<?> properties = Class.forName("net.bettercombat.logic.PlayerAttackProperties");
                Class<?> helper = Class.forName("net.bettercombat.logic.PlayerAttackHelper");
                betterCombatGetComboCount = properties.getMethod("getComboCount");
                betterCombatGetCurrentAttack = helper.getMethod("getCurrentAttack", Player.class, int.class);
            } catch (ReflectiveOperationException | LinkageError exception) {
                betterCombatGetComboCount = null;
                betterCombatGetCurrentAttack = null;
            }
        }
    }
}

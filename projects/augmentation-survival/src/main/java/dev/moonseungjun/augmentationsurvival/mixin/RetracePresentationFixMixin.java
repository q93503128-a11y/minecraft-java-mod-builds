package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps Retrace player-facing text readable instead of exposing/removing the internal ROOT term. */
@Mixin(value = AugmentPresentationCatalog.class, remap = false)
abstract class RetracePresentationFixMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$retracePlayerText(
            AugmentDefinition definition,
            CallbackInfoReturnable<AugmentPresentationCatalog.Entry> cir
    ) {
        if (definition == null || !definition.id().endsWith(":retrace")) {
            return;
        }
        cir.setReturnValue(new AugmentPresentationCatalog.Entry(
                "되밟기",
                AugmentPresentationCatalog.Kind.PASSIVE,
                "완충 근접 공격 적중 시 지난 2초 이동 경로를 잔상이 역주행해 경로의 적을 공격합니다.",
                "완충 근접 공격이 적중하면 지난 2초 동안 이동한 경로를 잔상이 역주행합니다. 경로에 닿은 적은 기준 공격 피해의 50% / 75% / 100%를 한 번 받습니다. 내부 쿨다운은 5초입니다."
        ));
    }
}

package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Final player-facing wording for the mythic rules changed by live-play Bundle I. */
@Mixin(value = AugmentPresentationCatalog.class, remap = false)
abstract class BundleIMythicPresentationMixin {
    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private static AugmentPresentationCatalog.Entry augmentationSurvival$bundleIMythicText(
            AugmentPresentationCatalog.Entry original,
            AugmentDefinition definition
    ) {
        if (definition == null) return original;
        String id = definition.id();
        if (id.endsWith(":gigantification")) {
            return new AugmentPresentationCatalog.Entry(
                    "거대화",
                    AugmentPresentationCatalog.Kind.ACTIVE,
                    "20초 동안 몸집과 체력, 근접 전투 능력이 크게 증가합니다.",
                    "20초 동안 크기·최대/현재 체력·휩쓸기 범위가 1.75배 / 2.5배로 커집니다. 근접 사거리는 +3 / +6블록, 근접 피해는 +35% / +70%, 넉백은 +100% 증가합니다. 재사용 대기시간은 60초입니다."
            );
        }
        if (id.endsWith(":time_stop")) {
            return new AugmentPresentationCatalog.Entry(
                    "시간 정지",
                    AugmentPresentationCatalog.Kind.ACTIVE,
                    "주변 32블록의 구체 안에서 자신을 제외한 모든 움직이는 대상을 9초 동안 멈춥니다.",
                    "반경 32블록의 시간 정지 구체를 9초 동안 펼칩니다. 시전자만 정상적으로 움직이며, 구체 안의 적·아군·중립 생물·보스·투사체·드롭·탈것 같은 동적 대상은 정지합니다. 재사용 대기시간은 60초입니다."
            );
        }
        return original;
    }
}

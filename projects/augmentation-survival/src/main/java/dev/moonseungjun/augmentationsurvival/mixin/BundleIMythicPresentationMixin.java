package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Final player-facing wording for mythic/live-play rules that differ from the original catalog. */
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
        if (id.endsWith(":echo_strike")) {
            return new AugmentPresentationCatalog.Entry(
                    "메아리 타격",
                    AugmentPresentationCatalog.Kind.PASSIVE,
                    "근접 공격이 적중하면 자신의 메아리가 동시에 같은 적을 한 번 더 공격합니다.",
                    "근접 공격 적중 시 자신과 같은 모습의 메아리가 함께 공격해 원래 공격 피해의 20% / 40% / 60%를 줍니다. 메아리의 공격은 벼락처럼 적중 횟수를 세는 증강을 한 번 더 진행시키지만, 또 다른 메아리를 만들지는 않습니다."
            );
        }
        return original;
    }
}

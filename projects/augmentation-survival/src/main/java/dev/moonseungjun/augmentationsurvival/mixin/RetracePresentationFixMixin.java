package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps repaired player-facing text aligned with the live runtime instead of stale design rows. */
@Mixin(value = AugmentPresentationCatalog.class, remap = false)
abstract class RetracePresentationFixMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$repairedPlayerText(
            AugmentDefinition definition,
            CallbackInfoReturnable<AugmentPresentationCatalog.Entry> cir
    ) {
        if (definition == null) {
            return;
        }
        String id = definition.id();
        if (id.endsWith(":retrace")) {
            cir.setReturnValue(new AugmentPresentationCatalog.Entry(
                    "되밟기",
                    AugmentPresentationCatalog.Kind.PASSIVE,
                    "완충 근접 공격 적중 시 지난 2초 이동 경로를 잔상이 역주행해 경로의 적을 공격합니다.",
                    "완충 근접 공격이 적중하면 지난 2초 동안 이동한 경로를 잔상이 역주행합니다. 경로에 닿은 적은 기준 공격 피해의 50% / 75% / 100%를 한 번 받습니다. 내부 쿨다운은 5초입니다."
            ));
            return;
        }
        if (id.endsWith(":return_to_sender")) {
            cir.setReturnValue(new AugmentPresentationCatalog.Entry(
                    "돌려보내기",
                    AugmentPresentationCatalog.Kind.PASSIVE,
                    "방패로 공격을 막아 내구도가 닳을 때 방어 횟수를 쌓고, 일정 횟수마다 전방으로 반사합니다.",
                    "방패 내구도가 실제로 닳은 방어만 셉니다. Lv.1 / 2 / 3은 5 / 4 / 3회 방어마다 공격자 방향 7블록, 전체 45° 원뿔 충격파를 방출해 그동안 막은 공격 피해 합계의 60% / 80% / 100%를 돌려줍니다."
            ));
            return;
        }
        if (id.endsWith(":portable_riptide")) {
            cir.setReturnValue(new AugmentPresentationCatalog.Entry(
                    "휴대 수류",
                    AugmentPresentationCatalog.Kind.PASSIVE,
                    "급류 삼지창과 물 양동이를 함께 들면 마른 곳에서도 정상적인 급류를 사용할 수 있습니다.",
                    "주손에 급류 삼지창을, 보조손에 물 양동이를 들면 비나 물이 없는 곳에서도 급류를 사용할 수 있습니다. 물 양동이는 소모되지 않습니다. 이동·소리·몸 회전은 일반 급류와 같은 바닐라 동작을 사용합니다."
            ));
            return;
        }
        if (id.endsWith(":water_run")) {
            cir.setReturnValue(new AugmentPresentationCatalog.Entry(
                    "수상 보행",
                    AugmentPresentationCatalog.Kind.PASSIVE,
                    "물 표면을 땅처럼 걷고 뛸 수 있습니다. 웅크리거나 높은 곳에서 뛰어들면 물속으로 들어갑니다.",
                    "물의 실제 수면 높이에 발을 디디고 서거나 걷고 뛸 수 있습니다. 웅크리면 수면을 통과하며, 5블록 이상 높이에서 떨어져 들어가면 그대로 다이빙합니다. 물속에서는 수면을 막지 않아 정상적으로 위로 빠져나올 수 있습니다. 수면 착지로 낙하 피해를 받지 않으며 용암에는 적용되지 않습니다."
            ));
            return;
        }
        if (id.endsWith(":last_stand")) {
            cir.setReturnValue(new AugmentPresentationCatalog.Entry(
                    "한 번 버티기",
                    AugmentPresentationCatalog.Kind.PASSIVE,
                    "치명 피해를 한 번 버티고 1 HP로 생존한 뒤 3초 동안 피해를 막습니다.",
                    "치명 피해를 받으면 1 HP로 생존하고 3초 동안 일반 피해를 받지 않습니다. 발동 순간 남아 있던 피해 지연 예약은 모두 사라지고, 보호 시간 동안 새 피해 지연도 쌓이지 않습니다. 재사용 대기시간은 180초입니다."
            ));
            return;
        }
        if (id.endsWith(":delayed_damage")) {
            cir.setReturnValue(new AugmentPresentationCatalog.Entry(
                    "피해 지연",
                    AugmentPresentationCatalog.Kind.PASSIVE,
                    "받을 피해 일부를 여러 번으로 나누어 뒤늦게 받습니다.",
                    "즉시 받을 피해 일부를 예약해 여러 차례에 걸쳐 나누어 받습니다. 한 번 버티기가 발동하면 남은 예약 피해는 모두 폐기되며, 그 3초 보호 시간 동안에는 새 예약 피해도 생성되지 않습니다."
            ));
        }
    }
}

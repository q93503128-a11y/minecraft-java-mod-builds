package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hand-authored player text for high-rarity augments; no mechanical summary extraction. */
@Mixin(value = AugmentPresentationCatalog.class, remap = false)
abstract class LegendaryPresentationFixMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$legendaryPlayerText(
            AugmentDefinition definition,
            CallbackInfoReturnable<AugmentPresentationCatalog.Entry> cir
    ) {
        if (definition == null) return;
        AugmentPresentationCatalog.Entry entry = switch (definition.canonicalNameKo()) {
            case "원격 창고" -> passive("원격 창고",
                    "어디서든 열 수 있는 27칸 개인 창고를 얻습니다.",
                    "개인 원격 창고 27칸을 사용할 수 있습니다. 공유 그룹에 원격 창고 보유자가 여러 명이면 각자의 27칸이 이어져 더 큰 공동 보관공간처럼 사용할 수 있습니다.");
            case "사망 지점 귀환 (액티브)" -> active("사망 지점 귀환",
                    "마지막으로 실제 사망한 장소로 한 번 돌아갑니다.",
                    "실제로 사망했을 때 기록된 마지막 사망 지점으로 이동합니다. 한 번의 사망 기록마다 한 번 사용할 수 있으며, 재림으로 살아난 경우에는 새 사망 지점으로 기록되지 않습니다.");
            case "위상 이동 (액티브)" -> active("위상 이동",
                    "5초 동안 벽과 천장을 통과하며 이동합니다.",
                    "5초 동안 수평 벽과 위쪽 고체 블록을 통과할 수 있습니다. 바닥 아래로 빠질 수는 없고, 지속 중에는 공격·채굴·설치·상호작용을 할 수 없습니다. 종료 시 안전한 위치로 보정됩니다. 재사용 대기시간은 20초입니다.");
            case "위협 억제 (액티브 · 토글)" -> toggle("위협 억제",
                    "켜 두면 주변 24블록에서 일반 적대몹의 자연 생성을 막습니다.",
                    "활성화 중 반경 24블록에서 일반 적대몹의 자연 생성을 막습니다. 스포너, 보스, 이벤트로 생성되는 적은 막지 않습니다.");
            case "마법 공명" -> passive("마법 공명",
                    "장비의 레벨형 마법부여 효과를 +1 / +2 / +3레벨 높여 적용합니다.",
                    "레벨이 있는 마법부여의 실제 효과를 Lv.1 / 2 / 3에서 각각 +1 / +2 / +3레벨 높여 계산합니다. 일반 최대 레벨을 넘어선 효과도 적용되며, 지원되는 외부 마법부여에도 적용됩니다.");
            case "되감기 (액티브)" -> active("되감기",
                    "자신의 위치와 생존 상태를 8초 전으로 되돌립니다.",
                    "8초 전의 위치, 이동 속도, 체력, 흡수 체력, 허기, 포화도, 산소, 화염 상태와 상태효과를 복원합니다. 인벤토리, 경험치, 장비 내구도, 월드와 몹 상태, 드롭, 재사용 대기시간은 되돌리지 않습니다. 재사용 대기시간은 60초입니다.");
            case "시간 가속 (액티브)" -> active("시간 가속",
                    "집중 후 20초 동안 이동·공격·채굴과 아이템 사용을 크게 가속합니다.",
                    "3초 동안 움직이거나 아이템을 사용하지 않고 집중하면 20초 동안 이동·공격·채굴 속도가 60%, 아이템 사용 속도가 40%, 자신이 발사한 투사체 속도가 40% 증가합니다. 재사용 대기시간은 45초입니다.");
            case "분신 (액티브)" -> active("분신",
                    "20초 동안 스스로 싸우는 전투 분신을 소환합니다.",
                    "20초 동안 주변 적을 스스로 찾아 싸우는 분신을 소환합니다. 분신의 최대 체력은 발동 시 자신의 50%, 공격 피해는 60%를 기준으로 합니다. 분신의 처치는 직접 처치 보상이나 진행으로 계산되지 않습니다. 재사용 대기시간은 60초입니다.");
            case "생명 연결" -> passive("생명 연결",
                    "직접 공격한 적과 주변 적들을 연결해 받은 피해 일부를 서로 나눕니다.",
                    "직접 공격이 적중하면 그 적과 반경 8블록의 가까운 적을 최대 4체까지 6초 동안 연결합니다. 연결된 적 하나가 피해를 받으면 실제 피해의 40%가 다른 살아 있는 연결 대상에게 나뉘어 전달됩니다. 전달된 피해가 다시 연쇄되지는 않습니다.");
            case "능력 가속" -> passive("능력 가속",
                    "재사용 대기시간이 있는 비신화 액티브의 쿨다운이 25% 짧아집니다.",
                    "일반 액티브의 재사용 대기시간을 25% 줄입니다. 재사용 대기시간이 없는 능력, 토글, 사망당 한 번만 쓰는 능력, 신화 액티브에는 적용되지 않습니다.");
            case "이중 충전" -> passive("이중 충전",
                    "재사용 대기시간이 있는 비신화 액티브를 최대 2회분까지 저장합니다.",
                    "일반 액티브가 최대 2회의 충전을 저장할 수 있습니다. 두 번 모두 사용하면 충전이 하나씩 순서대로 회복되며, 능력 가속과 전투 순환의 재사용 대기시간 효과도 정상 적용됩니다. 신화 액티브에는 적용되지 않습니다.");
            case "소생 (액티브)" -> active("소생",
                    "최근 죽은 아군이나 영구 동료를 최대 체력 50%로 되살립니다.",
                    "20블록 안의 아군 플레이어나 길들인·지배 영구 동료가 죽으면 20초 동안 소생 표식이 남습니다. 표식 가까이에서 3초 집중하면 최대 체력 50%로 부활시킵니다. 자신과 임시 분신은 대상이 아닙니다. 성공 후 재사용 대기시간은 120초입니다.");
            case "재림" -> passive("재림",
                    "죽을 상황에서 3초 뒤 최대 체력으로 다시 일어납니다.",
                    "다른 사망 방지 효과가 모두 실패한 치명 상황에서 자동 발동합니다. 3초 동안 피해를 받지 못하고 행동할 수 없는 상태가 된 뒤 최대 체력 100%로 안전하게 복귀합니다. 소지품과 경험치를 떨어뜨리지 않으며 실제 사망으로 기록되지 않습니다. 재사용 대기시간은 180초입니다.");
            case "무적" -> passive("무적",
                    "10초마다 자동으로 3초 동안 피해와 해로운 효과를 무시합니다.",
                    "10초마다 자동으로 3초 동안 무적이 됩니다. 그동안 일반·환경 피해와 넉백, 새로 들어오는 제거 가능한 해로운 상태효과를 무시하며 행동은 정상적으로 할 수 있습니다.");
            case "유리 대포" -> passive("유리 대포",
                    "최대 체력을 희생해 자신이 주는 전투 피해를 크게 높입니다.",
                    "Lv.1 / 2 / 3에서 최종 최대 체력이 원래의 60% / 80% / 100%가 되고, 일반 전투에서 자신이 주는 피해는 60% / 80% / 100% 증가합니다. 정해진 비율로 체력을 깎는 특수 피해에는 공격 증가가 적용되지 않습니다.");
            case "사냥 기록" -> passive("사냥 기록",
                    "처음 직접 처치한 적 종류마다 영구 스택을 얻어 체력과 공격력이 오릅니다.",
                    "처음으로 직접 처치한 적대적 생물 종류마다 영구 스택을 1 얻습니다. 스택당 최대 체력 +1 HP, 직접 공격 피해 +0.5%를 얻으며 최대 30스택까지 쌓입니다. 서로 다른 외부 모드 생물도 각각 별도 종류로 기록됩니다.");
            case "연장전" -> passive("연장전",
                    "직접 처치할 때 현재 사용 중인 지속형 액티브의 남은 시간이 늘어납니다.",
                    "적을 직접 처치하면 현재 실행 중인 비신화 지속형 액티브의 남은 시간이 1초 늘어납니다. 보스를 직접 처치하면 4초 늘어납니다. 한 번의 사용에서 원래 지속시간의 최대 50%까지만 연장할 수 있습니다.");
            case "영역 가속 (액티브)" -> active("영역 가속",
                    "60초 동안 주변 24블록의 성장과 생산을 크게 가속합니다.",
                    "60초 동안 반경 24블록의 작물, 묘목, 가축 성장과 호환되는 생산·처리 속도를 Lv.1 / 2에서 15배 / 30배로 가속합니다. 화로, 훈연기, 용광로, 양조도 포함됩니다. 적과 플레이어의 행동이나 월드 시간은 빨라지지 않습니다. 재사용 대기시간은 120초입니다.");
            case "금단의 마법" -> passive("금단의 마법",
                    "모루에서 원래 함께 붙일 수 없는 호환 마법부여도 조합할 수 있습니다.",
                    "모루에서 서로 배타적인 호환 마법부여를 같은 장비에 함께 부여할 수 있습니다. 'Too Expensive' 제한과 반복 작업에 따른 추가 경험치 상승은 사라지지만, 실제 마법부여·수리의 기본 비용은 그대로 지불합니다.");
            case "재연" -> passive("재연",
                    "비신화 전투 액티브를 사용하면 1.5초 뒤 50% 효율로 한 번 더 발동합니다.",
                    "호환되는 비신화 전투 액티브를 직접 사용하면 1.5초 뒤 같은 기술이 50% 효율로 자동 재발동합니다. 추가 충전이나 재사용 대기시간을 소비하지 않으며, 재연으로 발동한 기술은 다시 재연되지 않습니다.");
            case "지배 (액티브)" -> active("지배",
                    "바라보는 일반 적대몹을 영구 아군으로 만들고 곁에서 싸우게 합니다.",
                    "24블록 안의 일반 적대몹을 영구 아군으로 만듭니다. 최대 3 / 5 / 10마리까지 보유할 수 있고 재사용 대기시간은 없습니다. 지배 개체는 주인 주변에서 적을 상대하며, 자기 지배 개체를 바라보고 다시 사용하면 그 개체의 지배를 해제합니다. 보스는 지배할 수 없습니다.");
            case "거대화 (액티브)" -> active("거대화",
                    "20초 동안 몸집과 체력, 근접 전투 능력이 크게 증가합니다.",
                    "크기와 최대·현재 체력이 1.75배 / 2.5배가 되고 근접 사거리가 +3 / +6블록, 근접 피해가 +35% / +70%, 넉백이 +100% 증가합니다. 휩쓸기 범위도 커진 몸집에 맞춰 넓어집니다. 재사용 대기시간은 60초이며 이중 충전의 대상이 아닙니다.");
            case "천벌 (액티브)" -> active("천벌",
                    "주변 32블록의 모든 적에게 강력한 번개를 내립니다.",
                    "일반 적에게 최대 체력의 60% / 90%, 보스에게 10% / 25% 피해를 한 번 적용합니다. 번개는 세 차례 연달아 내리치며, Lv.2에서는 적중한 적을 2초 기절시킵니다. 재사용 대기시간은 60초입니다.");
            case "시간 정지 (액티브)" -> active("시간 정지",
                    "회색으로 드러나는 주변 32블록의 정지 영역에서 적과 적대 투사체를 9초 동안 멈춥니다.",
                    "회색 정지 영역 안의 적과 적대 투사체를 9초 동안 완전히 멈춥니다. 플레이어와 아군은 정상적으로 움직일 수 있습니다. 재사용 대기시간은 60초입니다.");
            case "비행 (액티브 · 토글)" -> toggle("비행",
                    "전용키로 자유 비행을 켜고 끕니다.",
                    "기본 V키로 켜고 끕니다. 시선 방향으로 가속하며 호버와 순항, 고속 비행을 오갑니다. 빠르게 비행하며 아군이 아닌 생물과 몸으로 충돌하면 비행 충돌 피해를 줄 수 있습니다.");
            default -> null;
        };
        if (entry != null) cir.setReturnValue(entry);
    }

    private static AugmentPresentationCatalog.Entry passive(String name, String summary, String detail) {
        return new AugmentPresentationCatalog.Entry(name, AugmentPresentationCatalog.Kind.PASSIVE, summary, detail);
    }

    private static AugmentPresentationCatalog.Entry active(String name, String summary, String detail) {
        return new AugmentPresentationCatalog.Entry(name, AugmentPresentationCatalog.Kind.ACTIVE, summary, detail);
    }

    private static AugmentPresentationCatalog.Entry toggle(String name, String summary, String detail) {
        return new AugmentPresentationCatalog.Entry(name, AugmentPresentationCatalog.Kind.TOGGLE, summary, detail);
    }
}

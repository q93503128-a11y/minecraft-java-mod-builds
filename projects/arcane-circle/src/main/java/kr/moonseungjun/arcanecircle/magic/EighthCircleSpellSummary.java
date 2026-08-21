package kr.moonseungjun.arcanecircle.magic;

/** Detailed effect-compendium text for the alpha.60 eighth-circle deep pass. */
public final class EighthCircleSpellSummary {
    private EighthCircleSpellSummary() {}

    public static String summary(String id) {
        return switch (id) {
            case "antimagic_field" -> "16초 동안 시전자 주변에 실제 반마법장을 유지합니다. 범위 안의 Arcane 시전과 유지형 마법을 계속 억제·해제하며, 이미 강한 반마법장 권능을 그대로 보존합니다.";
            case "clone" -> "조준한 비플레이어 생명체의 실제 생명체 복제본을 생성합니다. 장비와 주요 전투 능력을 복제하는 기존 클론 권능을 그대로 사용하며 단순 치명상 방지 버프로 퇴행하지 않습니다.";
            case "control_weather" -> "45초간 실제 폭우·뇌우를 지배하고 주변 적에게 지속적인 폭풍 압력을 가합니다. 유지 중 G키로 바라본 지점에 12연속 낙뢰 명령을 내리는 기존 천후 지배 권능을 보존합니다.";
            case "demiplane" -> "개인 주머니 공간을 실제로 열고 그 안의 블록과 물품을 다음 시전에도 보존합니다. 웅크린 동행자와 함께 들어갈 수 있고 G키 또는 재시전으로 기억된 귀환점으로 돌아옵니다.";
            case "dominate_monster" -> "강력한 비플레이어 생명체의 실제 전투 진영과 목표를 약 24초 동안 뒤집습니다. 주변 위협과 싸우거나 시전자를 따르며 Arcane 시전도 지배 효과 동안 봉쇄됩니다.";
            case "earthquake" -> "고정된 조준 지점을 중심으로 약 9초간 반복 지진을 일으켜 넓은 범위를 계속 흔들고 적을 띄우며 피해를 줍니다. 플레이어 시전은 최초 충격에서 실제 지형도 파괴합니다.";
            case "feeblemind" -> "대상의 사고·마법 회로를 약 35초 동안 붕괴시킵니다. 대상은 몸을 움직일 수 있지만 극심한 전투 약화와 Arcane 시전 봉쇄가 지속됩니다.";
            case "incendiary_cloud" -> "조준 지점에서 약 12초간 이동하는 소이 구름을 유지합니다. 구름은 시전 당시 진행 방향으로 천천히 흘러가며 안에 남은 적을 반복해서 태우고 지속 피해를 줍니다.";
            case "maze" -> "조준한 비플레이어 생명체를 약 18초간 전투에서 추방하는 기존 미궁 권능을 보존합니다. 대상은 전장과 상호작용할 수 없으며 시간이 끝나면 원래 전장으로 복귀합니다.";
            case "sunburst" -> "고정된 조준 지점을 거대한 광역 태양광으로 폭발시켜 넓은 범위에 강한 생명 피해를 주고 화상·실명·발광을 함께 남깁니다.";
            default -> "";
        };
    }
}

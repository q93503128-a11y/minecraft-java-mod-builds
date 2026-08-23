package kr.moonseungjun.arcanecircle.magic;

/** Detailed effect-compendium text for the eighth-circle authority pass. */
public final class EighthCircleSpellSummary {
    private EighthCircleSpellSummary() {}

    public static String summary(String id) {
        return switch (id) {
            case "antimagic_field" -> "16초 동안 시전자 주변에 실제 반마법장을 유지합니다. 범위 안의 Arcane 시전과 유지형 마법을 계속 억제·해제하며, 이미 강한 반마법장 권능을 그대로 보존합니다.";
            case "clone" -> "조준한 비플레이어 생명체의 전투 육체를 90초간 실제 복제합니다. 체력·공격·방어·이동 능력을 복제하고 한 번에 1체만 유지되며, 시전자를 따라다니면서 시전자나 자신을 공격하는 적을 자동 반격합니다. 장비 아이템은 복제하지 않아 전리품 복제는 불가능합니다.";
            case "control_weather" -> "45초간 실제 폭우·뇌우를 지배하고 주변 적에게 지속적인 폭풍 압력을 가합니다. 유지 중 G키로 바라본 지점에 12연속 낙뢰 명령을 내리는 기존 천후 지배 권능을 보존합니다.";
            case "demiplane" -> "개인 주머니 공간을 실제로 열고 그 안의 블록과 물품을 다음 시전에도 보존합니다. 웅크린 동행자와 함께 들어갈 수 있고 G키 또는 재시전으로 기억된 귀환점으로 돌아옵니다.";
            case "dominate_monster" -> "강력한 비플레이어 생명체의 실제 전투 진영을 60초 동안 탈취합니다. 대상은 시전자를 공격할 수 없고 시전자나 자신을 위협하는 적과 싸우며, 전투가 없으면 시전자를 추종합니다.";
            case "earthquake" -> "고정된 조준 지점을 중심으로 약 9초간 반복 지진을 일으켜 넓은 범위를 계속 흔들고 적을 띄우며 피해를 줍니다. 플레이어 시전은 최초 충격에서 실제 지형도 파괴합니다.";
            case "feeblemind" -> "대상의 사고·마법 회로를 90초 동안 붕괴시킵니다. 대상은 움직일 수 있지만 Arcane 시전은 완전히 봉쇄되고 공격력·행동속도·시야가 극단적으로 약화됩니다. 짧은 군중제어가 아니라 장기적인 마법사 무력화 권능입니다.";
            case "incendiary_cloud" -> "조준 지점에서 약 12초간 이동하는 소이 구름을 유지합니다. 구름은 시전 당시 진행 방향으로 천천히 흘러가며 안에 남은 적을 반복해서 태우고 지속 피해를 줍니다.";
            case "maze" -> "조준한 비플레이어 생명체를 24초간 전장에서 완전히 추방합니다. 대상은 그동안 공격·시전·피격·이동 등 전장과 상호작용할 수 없으며, 귀환 뒤 6초 동안 방향감각·이동·공격력이 크게 흔들리는 미궁 후유증을 받습니다.";
            case "sunburst" -> "고정된 조준 지점을 거대한 광역 태양광으로 폭발시켜 넓은 범위에 강한 생명 피해를 주고 화상·실명·발광을 함께 남깁니다.";
            default -> "";
        };
    }
}

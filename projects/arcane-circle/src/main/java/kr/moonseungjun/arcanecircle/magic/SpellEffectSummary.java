package kr.moonseungjun.arcanecircle.magic;

/** Short, mechanical spell text used by the grimoire detail reader. */
public final class SpellEffectSummary {
    private SpellEffectSummary() {}

    public static String summary(SpellDefinition spell) {
        if (spell == null) return "";
        return switch (spell.id()) {
            // 1C
            case "magic_missile" -> "조준 지점에 비전 피해를 주는 다중 탄환";
            case "fire_bolt" -> "단일 화염 피해 · 적중 대상 화상";
            case "ray_of_frost" -> "단일 냉기 피해 · 동결/둔화";
            case "shield" -> "약 8.5초 동안 피해 흡수 보호막";
            case "feather_fall" -> "6초 동안 낙하를 안정화해 추락 피해 억제";
            case "light" -> "90초 야간 시야 · 주변에 실제 임시 광원 생성";
            case "grease" -> "8초 지속 미끄럼 영역 · 반복 둔화·밀림";
            case "sleep" -> "조준 범위 적의 AI·이동을 7초간 정지 + 암흑";
            case "thunderwave" -> "전방 충격파 피해·넉백 · 취약 블록 파손";
            case "mage_armor" -> "장시간 피해 감소 효과";

            // 2C
            case "scorching_ray" -> "세 갈래 화염 광선 피해 · 화상";
            case "misty_step" -> "가까운 안전 지점으로 순간이동";
            case "web" -> "11초 지속 거미줄 영역 · 반복 강한 둔화·약화";
            case "mirror_image" -> "13초 내 다음 3회의 공격을 환영이 대신 받음";
            case "invisibility" -> "일정 시간 투명화";
            case "gust_of_wind" -> "직선 강풍으로 적 밀치기 · 매우 약한 블록 파손";
            case "hold_person" -> "조준 대상 AI·이동·Arcane 시전을 9초간 완전 속박";
            case "shatter" -> "목표 지점 진동 피해 · 약한 블록 파괴 및 제한 드롭";
            case "blur" -> "18초 동안 이동 강화 + 들어오는 피해 32% 감소";
            case "levitate" -> "상승 후 느린 낙하 상태 부여";

            // 3C
            case "fireball" -> "착탄 범위 화염 피해·화상 · 주변 지형 일부 파괴";
            case "lightning_bolt" -> "직선 관통 번개 피해 · 경로의 약한 블록만 파손";
            case "fly" -> "30초 동안 자유 비행 허용 · 종료 시 안전하게 비행 해제";
            case "haste" -> "이동·행동 속도 강화";
            case "dispel_magic" -> "조준 대상의 마법성 강화 상태 또는 자신의 해로운 효과 해제";
            case "vampiric_touch" -> "단일 피해 · 준 피해 일부를 체력으로 흡수";
            case "slow" -> "9초 지속 영역 · 적 이동·전투 능력을 반복 감소";
            case "protection_from_energy" -> "원소 피해에 버티는 장기 방호 효과";
            case "sleet_storm" -> "9초 지속 냉기 폭풍 · 반복 피해·동결·둔화, 중독 없음";
            case "blink" -> "조준 방향의 안전 지점으로 공간 도약";

            // 4C
            case "wall_of_fire" -> "10초 지속 화염 장벽 · 접촉 적 반복 피해·화상";
            case "ice_storm" -> "고정한 목표 지점 냉기 폭격 · 광역 피해·동결";
            case "greater_invisibility" -> "전투 중에도 오래 유지되는 투명화";
            case "resilient_sphere" -> "20초 구형 역장 · 흡수 + 들어오는 피해 82% 감소";
            case "dimension_door" -> "더 먼 안전 지점으로 즉시 공간 이동";
            case "stoneskin" -> "장시간 강한 피해 감소";
            case "confusion" -> "범위 적의 행동 능력을 둔화·약화시키는 정신 교란";
            case "blight" -> "강한 단일 생명 피해 · 약화";
            case "freedom_of_movement" -> "둔화·속박·동결·부양을 즉시 해제 + 이동 강화";
            case "phantasmal_killer" -> "단일 대상 정신 피해 · 공포성 약화 상태";

            // 5C
            case "cone_of_cold" -> "넓은 전방 냉기 피해 · 강한 동결";
            case "wall_of_force" -> "12초 지속 역장벽 · 접근/통과 시 지속적으로 밀어냄";
            case "cloudkill" -> "11초 지속 독성 지대 · 반복 피해·중독·약화";
            case "telekinesis" -> "대상 피해 후 들어 올리고 강하게 밀침";
            case "flame_strike" -> "고정 목표에 화염 기둥 낙하 · 광역 피해/화상/지형 파괴";
            case "hold_monster" -> "조준 대상을 15초간 AI·이동·Arcane 시전 완전 봉쇄";
            case "mass_cure_wounds" -> "주변 플레이어 다수의 체력 회복";
            case "passwall" -> "조준 방향의 먼 안전 지점으로 공간 통과";
            case "dominate_person" -> "조준 대상의 AI·이동·Arcane 시전을 13초간 봉쇄";
            case "insect_plague" -> "11초 지속 벌레 떼 · 반복 피해·약화";

            // 6C
            case "disintegrate" -> "직선 분해 피해 · 광선 경로 실제 블록 파괴";
            case "globe_of_invulnerability" -> "26초 흡수·저항 + 들어오는 피해 70% 감소";
            case "mass_suggestion" -> "넓은 범위 적의 AI·이동·Arcane 시전을 8초간 일괄 봉쇄";
            case "move_earth" -> "고정 목표 지면 광역 피해·띄우기 · 조건부 지형 파괴";
            case "sunbeam" -> "직선 태양광 피해 · 실명";
            case "true_seeing" -> "60초 야간 시야 · 주변 투명화 제거 + 생명체 발광 표시";
            case "freezing_sphere" -> "목표 폭발 냉기 피해 · 강한 동결";
            case "eyebite" -> "단일 피해 · 약화와 공포성 둔화";
            case "flesh_to_stone" -> "단일 피해 · 18초 AI·이동·Arcane 시전 완전 석화";
            case "circle_of_death" -> "고정 목표 지점 대형 광역 피해";

            // 7C
            case "delayed_blast_fireball" -> "지연 후 고정 목표 대형 화염 폭발 · 화상·강한 지형 파괴";
            case "etherealness" -> "저항·투명화·느린 낙하로 생존력 대폭 강화";
            case "finger_of_death" -> "강한 단일 피해 · 장시간 위더";
            case "fire_storm" -> "고정 목표 주변 7지점 화염 폭격 · 화상·지형 파괴";
            case "forcecage" -> "조준 대상을 15초간 AI·이동·Arcane 시전 완전 봉쇄";
            case "plane_shift" -> "장거리 안전 지점으로 고등 공간 이동";
            case "prismatic_spray" -> "전방 7색 광역 피해 · 무작위 강한 상태 이상";
            case "reverse_gravity" -> "고정 목표 범위 피해 · 강한 상승/공중 부양";
            case "simulacrum" -> "60초 내 다음 치명상을 대리체가 대신 받고 생존";
            case "teleport" -> "매우 먼 안전 지점으로 순간이동";

            // 8C
            case "antimagic_field" -> "12초 이동형 반마법장 · 버프/지속마법 제거 + Arcane 시전 차단";
            case "clone" -> "체력 완전 회복 · 90초 내 다음 치명상을 클론이 대신 받음";
            case "control_weather" -> "20초 실제 폭우·뇌우 · 주변 적 주기적 번개 피해/둔화";
            case "demiplane" -> "고등 공간 회로로 매우 먼 안전 지점 이동";
            case "dominate_monster" -> "조준 대상 AI·이동·Arcane 시전을 18초간 완전 봉쇄";
            case "earthquake" -> "고정 목표 지면 대규모 피해·띄우기 · 강한 실제 지형 파괴";
            case "feeblemind" -> "단일 피해 · 장시간 약화·실명";
            case "incendiary_cloud" -> "12초 지속 소이 구름 · 반복 화염 피해·화상";
            case "maze" -> "조준 대상을 12초간 완전 격리 상태 + 실명·혼란";
            case "sunburst" -> "고정 목표 초대형 태양광 폭발 · 광역 피해·화상·실명";

            // 9C
            case "meteor_swarm" -> "16발 시드형 연속 폭격 · 개별 피해·화상·지형 파괴";
            case "power_word_kill" -> "약해진 대상 즉사급 처형 · 그 외에는 대형 단일 피해";
            case "prismatic_wall" -> "30초 지속 7색 장벽 · 반복 피해·상태이상·통과 저지";
            case "shapechange" -> "90초 대폭 강화 + 들어오는 피해 35% 추가 감소";
            case "time_stop" -> "6초 고정 시간장 · 주변 비아군 AI·이동·Arcane 시전 정지";
            case "true_polymorph" -> "대상 피해 · 15초 축소 변형 + AI·이동·Arcane 시전 봉쇄";
            case "weird" -> "고정 목표 대형 정신 피해 · 실명·위더·둔화";
            case "wish" -> "체력·마력 완전 회복 · 해로운 상태 제거 · 기존 이로운 버프 보존 · 소원 외 주문 쿨 초기화";
            case "gate" -> "최상급 장거리 공간 이동";
            case "foresight" -> "120초 강화 + 들어오는 피해 38% 추가 감소";

            // Fusion
            case "burning_hands" -> "전방 화염 충격파 피해·넉백·화상";
            case "ice_knife" -> "냉기 투사체/파열 피해·동결 · 지형은 파괴하지 않음";
            case "chromatic_orb" -> "원소 구체 피해 · 원소성 추가 상태";
            case "wind_wall" -> "9초 지속 강풍 장벽 · 접근 적 반복 밀치기";
            case "counterspell" -> "대상 강화 해제 + 현재 주문 차단 · 1.8초 재시전 봉쇄";
            case "fire_shield" -> "31초 화염 저항·흡수 + 근접 공격자 자동 화염 반격";
            case "wall_of_ice" -> "11초 지속 냉기 장벽 · 반복 피해·동결·감속";
            case "chain_lightning" -> "여러 적 사이를 연쇄하는 번개 피해";
            case "arcane_hand" -> "대상 피해 후 강한 염동 밀치기";
            case "teleportation_circle" -> "안정된 장거리 공간 이동";
            case "steam_burst" -> "전방 증기 피해·둔화·화상·넉백";
            case "frost_step" -> "전방 도약 · 주변 적 냉기 피해·동결";
            case "thunder_cage" -> "단일 번개 피해 · 8초 AI·이동·Arcane 시전 완전 봉쇄";
            case "solar_guard" -> "흡수·저항·화염 면역 + 주변 적 화염 피해";
            case "void_lance" -> "긴 직선 관통 피해 · 대상 표식";
            case "winter_domain" -> "12초 지속 겨울 영역 · 반복 냉기 피해·동결·둔화";
            case "astral_prison" -> "단일 피해 · 11초 AI·이동·Arcane 시전 완전 봉쇄";
            case "phoenix_requiem" -> "자신/동맹만 치유·재생 + 주변 비아군 화염 피해";
            case "world_sunder" -> "목표 지면 초대형 피해·띄우기 · 방향성 실제 세계 균열";
            default -> fallback(spell);
        };
    }

    private static String fallback(SpellDefinition spell) {
        if (SpellCatalog.isDamaging(spell.id())) {
            return switch (spell.sigilAnchor()) {
                case FRONT -> "전방에 피해를 주는 " + spell.school().displayName() + " 공격";
                case TARGET -> "조준 대상에 피해와 상태 효과";
                case GROUND_TARGET -> "조준 지점에 범위 피해와 상태 효과";
                case GROUND_SELF -> "시전자 주변 범위 피해/효과";
                case BODY, FEET -> "시전자에게 전투 강화 효과";
            };
        }
        return switch (spell.sigilAnchor()) {
            case BODY, FEET, GROUND_SELF -> "시전자 중심의 " + spell.school().displayName() + " 보조 효과";
            case TARGET -> "조준 대상에 " + spell.school().displayName() + " 제어 효과";
            case GROUND_TARGET -> "조준 지점에 " + spell.school().displayName() + " 영역 효과";
            case FRONT -> "전방에 " + spell.school().displayName() + " 효과";
        };
    }
}

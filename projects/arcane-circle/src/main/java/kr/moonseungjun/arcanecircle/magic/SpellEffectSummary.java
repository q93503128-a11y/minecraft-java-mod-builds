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
            case "grease" -> "조준 지면 범위 둔화·약화";
            case "sleep" -> "조준 범위 적을 강하게 둔화·약화·암흑";
            case "thunderwave" -> "전방 충격파 피해·넉백 · 취약 블록 파손";
            case "mage_armor" -> "장시간 피해 감소 효과";

            // 2C
            case "scorching_ray" -> "세 갈래 화염 광선 피해 · 화상";
            case "misty_step" -> "가까운 안전 지점으로 순간이동";
            case "web" -> "조준 범위 적을 장시간 강한 둔화·약화";
            case "mirror_image" -> "환영 방호로 생존력을 높임";
            case "invisibility" -> "일정 시간 투명화";
            case "gust_of_wind" -> "직선 강풍으로 적 밀치기 · 매우 약한 블록 파손";
            case "hold_person" -> "조준 대상을 강하게 속박·약화";
            case "shatter" -> "목표 지점 진동 피해 · 약한 블록 파괴 및 제한 드롭";
            case "blur" -> "윤곽 왜곡으로 방어/회피 성능 강화";
            case "levitate" -> "상승 후 느린 낙하 상태 부여";

            // 3C
            case "fireball" -> "착탄 범위 화염 피해·화상 · 주변 지형 일부 파괴";
            case "lightning_bolt" -> "직선 관통 번개 피해 · 경로의 약한 블록만 파손";
            case "fly" -> "상승·활공 가능한 비행 보조 효과";
            case "haste" -> "이동·행동 속도 강화";
            case "dispel_magic" -> "조준 대상의 마법성 강화 상태 또는 자신의 해로운 효과 해제";
            case "vampiric_touch" -> "단일 피해 · 준 피해 일부를 체력으로 흡수";
            case "slow" -> "조준 범위 적의 이동·전투 능력 크게 감소";
            case "protection_from_energy" -> "원소 피해에 버티는 장기 방호 효과";
            case "sleet_storm" -> "조준 범위 지속 냉기 피해·동결·약화";
            case "blink" -> "조준 방향의 안전 지점으로 공간 도약";

            // 4C
            case "wall_of_fire" -> "화염 장벽 범위 피해·화상";
            case "ice_storm" -> "목표 지역 냉기 폭격 · 피해와 동결";
            case "greater_invisibility" -> "전투 중에도 오래 유지되는 투명화";
            case "resilient_sphere" -> "강한 흡수·저항을 주는 구형 방호";
            case "dimension_door" -> "더 먼 안전 지점으로 즉시 공간 이동";
            case "stoneskin" -> "장시간 강한 피해 감소";
            case "confusion" -> "범위 적의 행동 능력을 둔화·약화시키는 정신 교란";
            case "blight" -> "강한 단일 생명 피해 · 약화";
            case "freedom_of_movement" -> "둔화·속박·동결 계열 상태를 해제하고 이동 보호";
            case "phantasmal_killer" -> "단일 대상 정신 피해 · 공포성 약화 상태";

            // 5C
            case "cone_of_cold" -> "넓은 전방 냉기 피해 · 강한 동결";
            case "wall_of_force" -> "목표 전선에 강한 역장 장벽 효과";
            case "cloudkill" -> "목표 지역 독성 피해 · 중독·약화";
            case "telekinesis" -> "대상 피해 후 들어 올리고 강하게 밀침";
            case "flame_strike" -> "하늘에서 화염 기둥 낙하 · 광역 피해/화상/지형 파괴";
            case "hold_monster" -> "강한 대상도 장시간 완전 속박";
            case "mass_cure_wounds" -> "주변 플레이어 다수의 체력 회복";
            case "passwall" -> "조준 방향의 먼 안전 지점으로 공간 통과";
            case "dominate_person" -> "대상의 이동·전투 능력을 장시간 봉쇄";
            case "insect_plague" -> "목표 지역 지속 피해 · 중독·약화";

            // 6C
            case "disintegrate" -> "직선 분해 피해 · 광선 경로 실제 블록 파괴";
            case "globe_of_invulnerability" -> "강한 흡수 보호막과 피해 저항";
            case "mass_suggestion" -> "넓은 범위 적 피해·강한 둔화·약화";
            case "move_earth" -> "목표 지면 광역 피해·띄우기 · 조건부 지형 파괴";
            case "sunbeam" -> "직선 태양광 피해 · 실명";
            case "true_seeing" -> "60초 야간 시야 · 주변 적을 발광 표시";
            case "freezing_sphere" -> "목표 폭발 냉기 피해 · 강한 동결";
            case "eyebite" -> "단일 피해 · 약화와 공포성 둔화";
            case "flesh_to_stone" -> "단일 피해 · 이동을 거의 완전히 봉쇄";
            case "circle_of_death" -> "목표 지점 대형 광역 피해";

            // 7C
            case "delayed_blast_fireball" -> "지연 후 대형 화염 폭발 · 화상·강한 지형 파괴";
            case "etherealness" -> "저항·투명화·느린 낙하로 생존력 대폭 강화";
            case "finger_of_death" -> "강한 단일 피해 · 장시간 위더";
            case "fire_storm" -> "목표 주변 연속 화염 폭격 · 화상·지형 파괴";
            case "forcecage" -> "단일 대상을 장시간 거의 완전히 속박·약화";
            case "plane_shift" -> "장거리 안전 지점으로 고등 공간 이동";
            case "prismatic_spray" -> "전방 7색 광역 피해 · 무작위 강한 상태 이상";
            case "reverse_gravity" -> "목표 범위 피해 · 강한 상승/공중 부양";
            case "simulacrum" -> "장시간 흡수·재생·저항으로 대리체형 생존 버프";
            case "teleport" -> "매우 먼 안전 지점으로 순간이동";

            // 8C
            case "antimagic_field" -> "12초 이동형 반마법장 · 상태효과 억제·Arcane 시전/진행 차단";
            case "clone" -> "체력 완전 회복 · 장시간 대형 흡수·재생 보호";
            case "control_weather" -> "시전자 주변 광역 피해·강한 둔화의 폭풍 영역";
            case "demiplane" -> "고등 공간 회로로 매우 먼 안전 지점 이동";
            case "dominate_monster" -> "넓은 범위 적 피해·극심한 둔화·약화";
            case "earthquake" -> "목표 지면 대규모 피해·띄우기 · 강한 실제 지형 파괴";
            case "feeblemind" -> "단일 피해 · 장시간 약화·실명";
            case "incendiary_cloud" -> "목표 지역 지속 화염 피해·화상";
            case "maze" -> "단일 대상 피해 · 장시간 실명·둔화·혼란";
            case "sunburst" -> "초대형 태양광 폭발 · 광역 피해·화상";

            // 9C
            case "meteor_swarm" -> "16발 시드형 연속 폭격 · 개별 피해·화상·지형 파괴";
            case "power_word_kill" -> "약해진 대상 즉사급 처형 · 그 외에는 대형 단일 피해";
            case "prismatic_wall" -> "목표 전선 7색 장벽 · 범위 적 피해·강한 감속";
            case "shapechange" -> "90초 힘·저항·속도·점프·재생·흡수 대폭 강화";
            case "time_stop" -> "6초 고정 시간장 · 주변 비아군 AI·이동·Arcane 시전 정지";
            case "true_polymorph" -> "대상 피해 · 장시간 둔화·약화·표식";
            case "weird" -> "대형 범위 정신 피해 · 실명·위더·둔화";
            case "wish" -> "체력·마력 완전 회복 · 해로운 상태 제거 · 소원 외 주문 쿨 초기화";
            case "gate" -> "최상급 장거리 공간 이동";
            case "foresight" -> "120초 시야·속도·저항·행운·흡수 강화";

            // Fusion
            case "burning_hands" -> "전방 화염 충격파 피해·넉백·화상";
            case "ice_knife" -> "냉기 투사체 피해 · 적중 지점 파열";
            case "chromatic_orb" -> "원소 구체 피해 · 원소성 추가 상태";
            case "wind_wall" -> "강풍 장벽으로 접근 적 밀치기";
            case "counterspell" -> "대상의 강화 상태를 더 강하게 해제";
            case "fire_shield" -> "흡수·화염 저항과 근접 화염 반격형 방호";
            case "wall_of_ice" -> "냉기 장벽 피해·동결";
            case "chain_lightning" -> "여러 적 사이를 연쇄하는 번개 피해";
            case "arcane_hand" -> "대상 피해 후 강한 염동 밀치기";
            case "teleportation_circle" -> "안정된 장거리 공간 이동";
            case "steam_burst" -> "전방 증기 피해·둔화·화상·넉백";
            case "frost_step" -> "전방 도약 · 주변 적 냉기 피해·동결";
            case "thunder_cage" -> "단일 번개 피해 · 강한 둔화·약화·표식";
            case "solar_guard" -> "흡수·저항·화염 면역 + 주변 적 화염 피해";
            case "void_lance" -> "긴 직선 관통 피해 · 대상 표식";
            case "winter_domain" -> "시전자 주변 광역 냉기 피해·동결·둔화";
            case "astral_prison" -> "단일 피해 · 장시간 둔화·약화·부양";
            case "phoenix_requiem" -> "자신/아군 치유·재생 + 주변 적 화염 피해";
            case "world_sunder" -> "목표 지면 초대형 피해·띄우기 · 방향성 실제 세계 균열";
            default -> fallback(spell);
        };
    }

    private static String fallback(SpellDefinition spell) {
        if (SpellCatalog.isDamaging(spell.id())) {
            return switch (spell.sigilAnchor()) {
                case FRONT -> "전방에 피해를 주는 " + spell.school().displayName() + " 공격";
                case TARGET -> "조준 대상에 피해와 상태 효과";
                case GROUND_TARGET -> "조준 지점 범위 피해와 상태 효과";
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

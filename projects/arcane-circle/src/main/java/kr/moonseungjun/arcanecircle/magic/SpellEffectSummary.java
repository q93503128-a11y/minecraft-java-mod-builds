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
            case "shield" -> "약 8.5초 · 반응 방벽 2장으로 다음 충격의 고정 피해를 직접 흡수";
            case "feather_fall" -> "6초 동안 낙하를 안정화해 추락 피해 억제";
            case "light" -> "90초 야간 시야 · 주변에 실제 임시 광원 생성";
            case "grease" -> "8초 지속 미끄럼 영역 · 반복 둔화·밀림";
            case "sleep" -> "조준 범위 적의 AI·이동을 7초간 정지 + 암흑";
            case "thunderwave" -> "전방 충격파 피해·넉백 · 취약 블록 파손";
            case "mage_armor" -> "36초 · 4장 재생형 아케인 플레이트가 소모·재충전되며 피해 분산";
            // 2C
            case "scorching_ray" -> "0.5초 간격으로 이어지는 실제 3연속 화염 광선 · 각 타격 화상";
            case "misty_step" -> "조준 방향 12m 이내에서 실제 안전 착지점을 찾아 단거리 순간이동";
            case "web" -> "11초 지속 거미줄 영역 · 수평 이동을 강하게 붙잡고 반복 둔화·약화";
            case "mirror_image" -> "13초 · 환영 3체가 적대 직접 공격 3회를 대신 받음 · 환경 피해는 그대로 받음";
            case "invisibility" -> "21초 투명화 · 주변 적대 추적 해제 + 첫 적대 직접 공격 1회 회피 후 은신 해제";
            case "gust_of_wind" -> "전방 직선 강풍으로 적을 실제 밀어냄 · 거미줄/불/횃불 같은 취약 오브젝트 제거";
            case "hold_person" -> "일반 체급 대상만 9초 이동·공격·Arcane 시전 속박 · 보스급/대형 대상은 면역";
            case "shatter" -> "조준 지점 하나를 기준으로 진동 피해와 유리·얼음 등 취약 블록 파괴를 동시 적용";
            case "blur" -> "18초 · 적대 직접 공격이 35% 확률로 빗나감 · 환경 피해에는 적용되지 않음";
            case "levitate" -> "약 3초 실제 상승 후 부드러운 하강 · 종료 시 추가 안전 낙하";
            // 3C
            case "fireball" -> "고정 착탄점 화염 폭발 · 중심부 강한 피해/거리 감쇠 + 화상 + 주변 지형 일부 파괴";
            case "lightning_bolt" -> "고정 직선 관통 번개 · 경로의 복수 대상 타격 + 약한 지형 파손";
            case "fly" -> "30초 실제 자유 비행 · 종료 시 기존 비행 권한 복원 + 안전 낙하";
            case "haste" -> "30초 · 마법진 전개 28% 단축 + 주문 재사용 대기 15% 단축 + 이동 가속";
            case "dispel_magic" -> "조준 대상의 1~3써클 유지형 강화·제어 마법 제거 · 대상이 없으면 자신의 해로운 상태 정화";
            case "vampiric_touch" -> "근거리 생명력 흡수 · 실제로 잃게 한 체력/흡수량의 60%만큼 회복";
            case "slow" -> "9초 지속 시간왜곡 영역 · 적 이동·공격 행동을 반복적으로 둔화";
            case "protection_from_energy" -> "30초 · 5중 공명막이 Arcane/화염/투사체성 충격만 45% 경감 · 3.5초마다 재충전";
            case "sleet_storm" -> "9초 진눈깨비 영역 · 반복 냉기 피해·동결·암흑·미끄럼 + 내부 적대 Arcane 시전 방해";
            case "blink" -> "최대 약 20m 안전 공간 도약 · 착지 후 2초 위상 저항";
            // 4C
            case "wall_of_fire" -> "12초 실제 화염 장벽 · 벽을 스치거나 통과하는 적에게 반복 피해·연소";
            case "ice_storm" -> "고정 목표에 5회 연속 우박 폭격 · 반복 냉기 피해·동결·이동 압박";
            case "greater_invisibility" -> "39초 전투 투명화 · 공격해도 유지 + 적대 추적 지속 해제 + 직접 공격 45% 빗나감";
            case "resilient_sphere" -> "20초 완전 격리막 · 안팎의 피해 모두 차단 · 내부 Arcane 시전 불가";
            case "dimension_door" -> "최대 약 36m 안전 공간 이동 · 3m 내 웅크린 플레이어 1명 동행 가능";
            case "stoneskin" -> "38초 · 적이 가하는 비마법 물리 공격만 50% 경감 · 화염/Arcane/환경 피해는 통과";
            case "confusion" -> "12초 범위 정신 교란 · 매초 행동이 정지/배회/오인공격/비틀림 중 하나로 바뀌며 Arcane 시전도 간헐 방해";
            case "blight" -> "단일 생명 쇠퇴 · 8초 추가 흡수 피해 + 받는 치유량 80% 감소";
            case "freedom_of_movement" -> "26초 · 둔화/속박/동결/강제부양 지속 정화 + 하위 이동 제어의 Arcane 시전 봉쇄 무시";
            case "phantasmal_killer" -> "11초 단일 공포 환상 · 대상이 시전자에게서 실제로 도주하며 주기적 정신 피해";
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
            case "globe_of_invulnerability" -> "26초 · 반경 약 6m · 외부에서 들어오는 적대 1~5써클 Arcane 주문을 경계면에서 소거 · 6써클 이상/물리 공격은 통과";
            case "mass_suggestion" -> "8초 · 범위 적에게 전투 이탈 명령 · 공격을 끊고 조준 지점에서 실제로 멀어지며 Arcane 시전 억제";
            case "move_earth" -> "고정 목표 지면 광역 피해·띄우기 · 조건부 지형 파괴";
            case "sunbeam" -> "직선 태양광 피해 · 실명";
            case "true_seeing" -> "60초 · 주변 은신을 주기적으로 벗기고 생명체를 계속 추적 표시";
            case "freezing_sphere" -> "목표 폭발 냉기 피해 · 강한 동결";
            case "eyebite" -> "단일 피해 · 약화와 공포성 둔화";
            case "flesh_to_stone" -> "단일 피해 · 18초 AI·이동·Arcane 시전 완전 석화";
            case "circle_of_death" -> "고정 목표 지점 대형 광역 피해";
            // 7C
            case "delayed_blast_fireball" -> "지연 후 초대형 화염 폭발 · 증폭 피해·장기 화상·강한 지형 파괴";
            case "etherealness" -> "물질 충돌 위상화 + 자유 비행 · 일반 피해 88% 경감 · 종료 시 안전 낙하";
            case "finger_of_death" -> "강한 단일 피해 · 장시간 위더";
            case "fire_storm" -> "대범위 7지점 연쇄 화염 폭격 · 증폭 피해·장기 화상·지형 파괴";
            case "forcecage" -> "20초 · 대상 AI/공격/시전은 유지하지만 약 6m 고정 역장 경계를 실제로 넘지 못하게 감금";
            case "plane_shift" -> "실제 차원 이동 · 시선 위=엔드/아래=네더/수평=오버월드 · 5.5m 내 웅크린 플레이어 최대 8명 동행";
            case "prismatic_spray" -> "전방 7색 광역 피해 · 무작위 강한 상태 이상";
            case "reverse_gravity" -> "대범위 피해 · 11초간 초강제 상승/공중 부양";
            case "simulacrum" -> "조준 생명체의 반실체 복제체 생성 · 체력 50%/전투력 약 72% · 웅크린 채 G로 추종·수호·집중공격 명령";
            case "teleport" -> "매우 먼 안전 지점으로 순간이동";
            // 8C
            case "antimagic_field" -> "16초 대형 이동 반마법장 · 버프/지속마법 제거 + Arcane 시전 완전 차단";
            case "clone" -> "조준한 비플레이어 생명체의 실제 복제본 생성 · 장비/기초 전투체급 복제 · 시전자 소유 아님";
            case "control_weather" -> "45초 실제 폭우·뇌우 지배 · G키로 바라본 지점 12연속 낙뢰 명령 · 재사용 2.5초";
            case "demiplane" -> "보존되는 개인 주머니방 생성·재접속 · 내부 블록/물품 유지 · G 또는 재시전으로 기억된 귀환점 복귀";
            case "dominate_monster" -> "24초 · 조준 괴물을 임시 전투 대리체로 지배 · 시전자 공격 중지·주변 위협 공격·비전투 시 추종 · 적대 Arcane 시전 봉쇄";
            case "earthquake" -> "초대형 지면 피해·강제 띄우기 · 8써클급 광역 실제 지형 붕괴";
            case "feeblemind" -> "단일 정신 피해 + 35초 Arcane 시전 봉쇄·공격/행동 약화 · 몸과 AI 자체는 계속 움직임";
            case "incendiary_cloud" -> "12초 지속 소이 구름 · 반복 화염 피해·화상";
            case "maze" -> "조준 생명체를 18초간 전장에서 실제 추방 · 종료 시 원래 전장으로 귀환";
            case "sunburst" -> "초대형 태양광 폭발 · 증폭 광역 피해·장기 화상·강한 실명";
            // 9C
            case "meteor_swarm" -> "16발 시드형 연속 폭격 · 개별 피해·화상·대형 충돌구 지형 파괴";
            case "power_word_kill" -> "높은 처형 임계치 이하 대상 즉사급 명령 · 실패해도 9써클 대형 단일 피해";
            case "prismatic_wall" -> "20초 지속 7색 장벽 · 반복 피해·상태이상·강제 통과 저지";
            case "shapechange" -> "90초 초월 육체 · 피해 50% 경감 + 강한 자체 재생·근력·속도·도약 강화";
            case "time_stop" -> "8초 초대형 고정 시간장 · 주변 비아군 AI·이동·투사체·드롭 이동·Arcane 시전 정지";
            case "true_polymorph" -> "조준 생명체의 실제 몸체를 24초간 다른 생물로 교체 · 변신체가 쓰러지면 원형이 부상 상태로 복귀";
            case "weird" -> "초대형 정신 붕괴 · 35초 증폭 피해·실명·고단계 위더·둔화";
            case "wish" -> "체력·마력 완전 회복 · 해로운 상태 제거 · 기존 이로운 버프 보존 · 소원 외 주문 쿨 초기화";
            case "gate" -> "최상급 장거리 공간 이동";
            case "foresight" -> "120초 예지 · 2초마다 다음 피격 완전 회피 + 사이 피해 25% 경감 + 예지 시야";
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
            case "solar_guard" -> "30초 태양 방패 4장 · 피해 흡수·재충전 + 공격자 점화·넉백";
            case "void_lance" -> "긴 직선 관통 피해 · 대상 표식";
            case "winter_domain" -> "12초 지속 겨울 영역 · 반복 냉기 피해·동결·둔화";
            case "astral_prison" -> "단일 피해 · 11초 AI·이동·Arcane 시전 완전 봉쇄";
            case "phoenix_requiem" -> "자신/동맹만 치유·재생 + 주변 비아군 화염 피해";
            case "world_sunder" -> "목표 지면 초대형 피해·띄우기 · 장거리·심층 실제 세계 균열";
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

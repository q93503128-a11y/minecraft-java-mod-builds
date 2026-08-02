package kr.moonseungjun.arcanecircle.magic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.Acquisition.BOOK;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.Acquisition.FUSION;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.Acquisition.PRIMER;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.ARCANE;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.FIRE;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.FROST;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.LIFE;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.SPACE;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.WARD;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.WIND;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.SigilAnchor.BODY;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.SigilAnchor.FEET;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.SigilAnchor.FRONT;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.SigilAnchor.GROUND_SELF;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.SigilAnchor.GROUND_TARGET;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.SigilAnchor.TARGET;

/**
 * Rank 1-5 spell catalogue adapted from long-established D&D SRD and Pathfinder spell
 * archetypes. Names and encounter roles are intentionally recognizable; numerical values,
 * anchors, Minecraft effects and fusion formulae are balanced specifically for this mod.
 */
public final class SpellCatalog {
    public static final int IMPLEMENTED_MAX_CIRCLE = 9;
    public static final int WORLD_MAX_CIRCLE = 9;

    private static final Map<String, SpellDefinition> SPELLS = new LinkedHashMap<>();
    private static final List<FusionFormula> FUSIONS = new ArrayList<>();
    private static boolean bootstrapped;

    private SpellCatalog() {}

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;

        // 1st Circle — apprentice formulae and classic low-rank utility magic.
        add("magic_missile", "매직 미사일", 1, 10, 28, 14.0, 4.0, ARCANE, PRIMER, FRONT,
                "피하지 못하는 비전 탄환을 전방 회로에서 발사한다.");
        add("fire_bolt", "파이어 볼트", 1, 12, 42, 13.0, 4.5, FIRE, PRIMER, FRONT,
                "응축한 불꽃을 직선으로 쏘아 대상을 태운다.");
        add("ray_of_frost", "프로스트 레이", 1, 13, 48, 13.0, 3.8, FROST, PRIMER, FRONT,
                "차가운 광선으로 피해를 주고 움직임을 둔화한다.");
        add("shield", "아케인 실드", 1, 18, 110, 0.0, 4.0, WARD, PRIMER, BODY,
                "짧은 시간 충격을 흡수하는 즉응형 역장을 펼친다.");
        add("feather_fall", "페더 폴", 1, 14, 120, 0.0, 1.0, WIND, PRIMER, FEET,
                "낙하 속도를 크게 줄여 추락 피해를 막는다.");
        add("light", "라이트", 1, 8, 80, 0.0, 1.0, ARCANE, BOOK, BODY,
                "지속되는 마법광을 만들어 어둠 속 시야를 확보한다.");
        add("grease", "그리스", 1, 16, 100, 12.0, 1.0, ARCANE, BOOK, GROUND_TARGET,
                "조준 지면을 미끄럽게 만들어 적의 움직임을 방해한다.");
        add("sleep", "슬립", 1, 20, 160, 12.0, 1.0, ARCANE, BOOK, GROUND_TARGET,
                "약한 적의 의식을 흐리게 해 거의 움직이지 못하게 한다.");
        add("thunderwave", "썬더 웨이브", 1, 22, 105, 7.0, 5.0, WIND, BOOK, FRONT,
                "전방에 굉음을 동반한 충격파를 방출해 적을 밀어낸다.");
        add("mage_armor", "메이지 아머", 1, 20, 220, 0.0, 4.0, WARD, BOOK, BODY,
                "몸을 감싸는 장기 지속형 방호장을 형성한다.");

        // 2nd Circle — established adventuring and control magic.
        add("scorching_ray", "스코칭 레이", 2, 34, 115, 17.0, 8.0, FIRE, BOOK, FRONT,
                "여러 줄기의 화염 광선을 한 대상에 집중한다.");
        add("misty_step", "미스티 스텝", 2, 32, 170, 11.0, 1.0, SPACE, BOOK, GROUND_TARGET,
                "안개처럼 사라져 가까운 안전 지점으로 순간이동한다.");
        add("web", "웹", 2, 38, 190, 14.0, 1.0, WARD, BOOK, GROUND_TARGET,
                "넓은 지역에 끈끈한 마법 거미줄을 펼쳐 적을 묶는다.");
        add("mirror_image", "미러 이미지", 2, 42, 230, 0.0, 6.0, ARCANE, BOOK, BODY,
                "여러 환영을 만들어 공격을 분산시킨다.");
        add("invisibility", "인비저빌리티", 2, 44, 280, 0.0, 1.0, ARCANE, BOOK, BODY,
                "일정 시간 자신의 모습을 감춘다.");
        add("gust_of_wind", "거스트", 2, 40, 130, 16.0, 6.0, WIND, BOOK, FRONT,
                "강한 직선 바람으로 적을 멀리 밀어낸다.");
        add("hold_person", "홀드", 2, 46, 240, 15.0, 3.0, WARD, BOOK, TARGET,
                "대상의 움직임을 강제로 봉쇄한다.");
        add("shatter", "섀터", 2, 48, 150, 15.0, 8.0, ARCANE, BOOK, GROUND_TARGET,
                "고주파 진동을 폭발시켜 범위 안 적을 타격한다.");
        add("blur", "블러", 2, 40, 260, 0.0, 4.0, ARCANE, BOOK, BODY,
                "윤곽을 흔들어 공격이 정확히 닿기 어렵게 만든다.");
        add("levitate", "레비테이트", 2, 38, 210, 0.0, 1.0, WIND, BOOK, FEET,
                "중력을 약화해 짧게 상승하고 천천히 내려온다.");

        // 3rd Circle — mature combat and mobility formulae.
        add("fireball", "파이어볼", 3, 68, 190, 19.0, 13.0, FIRE, BOOK, FRONT,
                "폭발성 화염핵을 발사해 착탄 지점을 태운다.");
        add("lightning_bolt", "라이트닝 볼트", 3, 70, 175, 21.0, 14.0, ARCANE, BOOK, FRONT,
                "직선상 적들을 관통하는 강력한 번개를 방출한다.");
        add("fly", "플라이", 3, 60, 320, 0.0, 1.0, WIND, BOOK, FEET,
                "공중으로 상승하고 한동안 안전하게 활공한다.");
        add("haste", "헤이스트", 3, 64, 330, 0.0, 1.0, ARCANE, BOOK, BODY,
                "신체와 사고를 가속해 이동과 행동 속도를 높인다.");
        add("dispel_magic", "디스펠", 3, 62, 190, 18.0, 5.0, ARCANE, BOOK, TARGET,
                "대상의 강화 마법 또는 자신의 해로운 마법을 제거한다.");
        add("vampiric_touch", "뱀파릭 터치", 3, 66, 175, 8.0, 12.0, LIFE, BOOK, TARGET,
                "대상의 생명력을 빼앗아 자신의 상처를 회복한다.");
        add("slow", "슬로우", 3, 60, 220, 17.0, 2.0, ARCANE, BOOK, GROUND_TARGET,
                "범위 안 적의 행동과 이동 속도를 크게 떨어뜨린다.");
        add("protection_from_energy", "에너지 보호", 3, 68, 360, 0.0, 9.0, WARD, BOOK, BODY,
                "원소 피해를 견디는 다층 보호장을 펼친다.");
        add("sleet_storm", "진눈깨비 폭풍", 3, 72, 250, 17.0, 8.0, FROST, BOOK, GROUND_TARGET,
                "차가운 진눈깨비로 넓은 지역의 시야와 움직임을 방해한다.");
        add("blink", "점멸", 3, 58, 250, 18.0, 1.0, SPACE, BOOK, GROUND_TARGET,
                "불안정한 차원 경계를 통과해 먼 지점으로 도약한다.");

        // 4th Circle — strategic wall, defence and disabling magic.
        add("wall_of_fire", "화염벽", 4, 100, 390, 22.0, 17.0, FIRE, BOOK, GROUND_TARGET,
                "긴 화염 장벽을 세워 접근하는 적을 태운다.");
        add("ice_storm", "얼음 폭풍", 4, 104, 410, 21.0, 16.0, FROST, BOOK, GROUND_TARGET,
                "우박과 얼음 파편을 쏟아 넓은 지역을 타격한다.");
        add("greater_invisibility", "상급 투명화", 4, 105, 520, 0.0, 1.0, ARCANE, BOOK, BODY,
                "전투 중에도 안정적으로 유지되는 강력한 투명화를 건다.");
        add("resilient_sphere", "탄성 구체", 4, 108, 480, 0.0, 16.0, WARD, BOOK, BODY,
                "시전자를 완전히 감싸는 고강도 구형 역장을 만든다.");
        add("dimension_door", "차원문", 4, 112, 430, 31.0, 1.0, SPACE, BOOK, GROUND_TARGET,
                "두 지점 사이를 접어 장거리를 즉시 통과한다.");
        add("stoneskin", "돌가죽", 4, 96, 500, 0.0, 14.0, WARD, BOOK, BODY,
                "피부를 돌처럼 강화해 오랫동안 큰 피해를 줄인다.");
        add("confusion", "혼란", 4, 104, 360, 19.0, 3.0, ARCANE, BOOK, GROUND_TARGET,
                "여러 적의 정신을 뒤섞어 행동 능력을 크게 떨어뜨린다.");
        add("blight", "황폐", 4, 110, 310, 17.0, 18.0, LIFE, BOOK, TARGET,
                "생명력을 급격히 말려 강한 단일 피해를 준다.");
        add("freedom_of_movement", "이동의 자유", 4, 92, 480, 0.0, 1.0, LIFE, BOOK, BODY,
                "속박·둔화·동결을 벗어나 자유롭게 움직이게 한다.");
        add("phantasmal_killer", "환영 살해자", 4, 118, 400, 18.0, 20.0, ARCANE, BOOK, TARGET,
                "대상이 가장 두려워하는 환영으로 정신과 육체를 공격한다.");

        // 5th Circle — high-level battlefield magic.
        add("cone_of_cold", "냉기 원뿔", 5, 165, 620, 20.0, 28.0, FROST, BOOK, FRONT,
                "거대한 원뿔형 냉기를 방출해 전방을 얼린다.");
        add("wall_of_force", "역장벽", 5, 170, 760, 24.0, 24.0, WARD, BOOK, GROUND_TARGET,
                "거의 파괴되지 않는 순수 역장 장벽을 세운다.");
        add("cloudkill", "독구름", 5, 178, 680, 22.0, 25.0, LIFE, BOOK, GROUND_TARGET,
                "치명적인 독성 안개로 넓은 지역을 뒤덮는다.");
        add("telekinesis", "염동력", 5, 160, 520, 25.0, 22.0, ARCANE, BOOK, TARGET,
                "강한 정신력으로 대상을 들어 올리고 멀리 내던진다.");
        add("flame_strike", "화염 기둥", 5, 185, 650, 24.0, 32.0, FIRE, BOOK, GROUND_TARGET,
                "하늘에서 거대한 화염 기둥을 내려찍는다.");
        add("hold_monster", "괴물 속박", 5, 172, 620, 23.0, 10.0, WARD, BOOK, TARGET,
                "강력한 괴물조차 장시간 움직이지 못하게 봉쇄한다.");
        add("mass_cure_wounds", "광역 치유", 5, 180, 720, 13.0, 26.0, LIFE, BOOK, GROUND_SELF,
                "주변 아군 다수의 상처를 동시에 크게 회복한다.");
        add("passwall", "통과문", 5, 155, 620, 28.0, 1.0, SPACE, BOOK, GROUND_TARGET,
                "견고한 공간 경계를 잠시 열어 멀리 통과한다.");
        add("dominate_person", "인간형 지배", 5, 190, 820, 24.0, 12.0, ARCANE, BOOK, TARGET,
                "대상의 의지를 압도해 오랫동안 전투 능력을 봉쇄한다.");
        add("insect_plague", "곤충 떼", 5, 176, 660, 22.0, 24.0, LIFE, BOOK, GROUND_TARGET,
                "굶주린 곤충 떼를 불러 지속적인 범위 피해를 준다.");


        // 6th Circle — battlefield domination and major transformation.
        add("disintegrate", "분해", 6, 330, 900, 34.0, 56.0, ARCANE, BOOK, FRONT,
                "가느다란 녹색 광선으로 물질과 생명체의 결합을 붕괴시킨다.");
        add("globe_of_invulnerability", "무적의 구체", 6, 360, 1200, 0.0, 52.0, WARD, BOOK, BODY,
                "하위 주문을 밀어내는 거대한 구형 역장을 전개한다.");
        add("mass_suggestion", "대규모 제안", 6, 310, 1100, 38.0, 18.0, ARCANE, BOOK, GROUND_TARGET,
                "여러 적의 의식을 동시에 지배해 전투 의지를 꺾는다.");
        add("move_earth", "대지 이동", 6, 350, 1000, 34.0, 42.0, LIFE, BOOK, GROUND_TARGET,
                "대지를 뒤집는 충격으로 넓은 범위의 적을 띄우고 무너뜨린다.");
        add("sunbeam", "태양 광선", 6, 345, 820, 38.0, 52.0, LIFE, BOOK, FRONT,
                "눈부신 태양광을 직선으로 방출해 적을 태우고 실명시킨다.");
        add("true_seeing", "진실의 시야", 6, 280, 1300, 40.0, 1.0, ARCANE, BOOK, BODY,
                "투명화와 환영을 꿰뚫고 주변 존재를 감지한다.");
        add("freezing_sphere", "냉동 구체", 6, 350, 900, 36.0, 50.0, FROST, BOOK, FRONT,
                "폭발하는 극저온 구체로 넓은 지역을 얼린다.");
        add("eyebite", "사악한 눈", 6, 320, 900, 32.0, 36.0, ARCANE, BOOK, TARGET,
                "저주의 시선으로 대상을 공포와 쇠약에 빠뜨린다.");
        add("flesh_to_stone", "육신 석화", 6, 355, 1100, 30.0, 40.0, WARD, BOOK, TARGET,
                "생명체의 육신을 돌처럼 굳혀 움직임을 봉쇄한다.");
        add("circle_of_death", "죽음의 원", 6, 390, 1250, 34.0, 58.0, LIFE, BOOK, GROUND_TARGET,
                "넓은 죽음의 파동을 펼쳐 약한 적을 쓸어버린다.");

        // 7th Circle — fortress-breaking and planar magic.
        add("delayed_blast_fireball", "딜레이드 파이어볼", 7, 590, 1500, 44.0, 82.0, FIRE, BOOK, GROUND_TARGET,
                "고밀도 화염핵을 축적한 뒤 거대한 폭발을 일으킨다.");
        add("etherealness", "에테르화", 7, 520, 1800, 0.0, 1.0, SPACE, BOOK, BODY,
                "짧은 시간 물질계의 간섭에서 벗어나 피해를 무시한다.");
        add("finger_of_death", "죽음의 손가락", 7, 630, 1700, 38.0, 95.0, LIFE, BOOK, TARGET,
                "단일 대상의 생명력을 직접 붕괴시키는 죽음의 광선을 쏜다.");
        add("fire_storm", "화염 폭풍", 7, 650, 1650, 46.0, 84.0, FIRE, BOOK, GROUND_TARGET,
                "여러 화염 기둥을 연쇄적으로 떨어뜨려 전장을 불태운다.");
        add("forcecage", "역장 감옥", 7, 610, 1900, 40.0, 55.0, WARD, BOOK, TARGET,
                "도망칠 수 없는 다층 역장 감옥으로 강적을 봉쇄한다.");
        add("plane_shift", "차원 이동", 7, 560, 2200, 0.0, 1.0, SPACE, BOOK, BODY,
                "공간층을 넘어 마법학원 귀환 회로로 즉시 이동한다.");
        add("prismatic_spray", "무지개 분사", 7, 620, 1450, 38.0, 76.0, ARCANE, BOOK, FRONT,
                "일곱 빛깔 광선을 흩뿌려 서로 다른 원소와 상태 효과를 준다.");
        add("reverse_gravity", "중력 반전", 7, 600, 1600, 40.0, 48.0, SPACE, BOOK, GROUND_TARGET,
                "광범위한 중력을 뒤집어 적들을 하늘로 솟구치게 한다.");
        add("simulacrum", "시뮬라크럼", 7, 680, 2400, 0.0, 64.0, FROST, BOOK, BODY,
                "얼음과 비전력으로 자신의 대리체를 만들어 치명상을 대신 받게 한다.");
        add("teleport", "순간이동", 7, 570, 1500, 72.0, 1.0, SPACE, BOOK, GROUND_TARGET,
                "시야 너머의 안전한 지점까지 공간을 접어 이동한다.");

        // 8th Circle — regional and reality-warping magic.
        add("antimagic_field", "반마법장", 8, 920, 2400, 16.0, 72.0, WARD, BOOK, GROUND_SELF,
                "주변의 마법 효과를 지우고 적의 강화 상태를 소거한다.");
        add("clone", "클론", 8, 980, 3200, 0.0, 95.0, LIFE, BOOK, BODY,
                "치명상을 대신 받아내는 생명 복제 회로를 자신에게 각인한다.");
        add("control_weather", "기후 조종", 8, 900, 2600, 64.0, 70.0, WIND, BOOK, GROUND_SELF,
                "하늘과 대기의 흐름을 장악해 번개와 폭풍을 불러온다.");
        add("demiplane", "반차원", 8, 950, 3000, 0.0, 1.0, SPACE, BOOK, BODY,
                "안전한 학원 성소와 연결된 임시 반차원을 연다.");
        add("dominate_monster", "괴물 지배", 8, 1040, 2800, 42.0, 72.0, ARCANE, BOOK, TARGET,
                "강력한 괴물의 의지를 완전히 압도해 전투 능력을 봉쇄한다.");
        add("earthquake", "지진", 8, 1100, 3000, 50.0, 120.0, LIFE, BOOK, GROUND_TARGET,
                "대규모 지진으로 전장을 뒤흔들고 적을 공중으로 던진다.");
        add("feeblemind", "정신 붕괴", 8, 990, 2600, 40.0, 88.0, ARCANE, BOOK, TARGET,
                "대상의 사고와 마법 저항을 파괴해 극심한 쇠약을 준다.");
        add("incendiary_cloud", "소이 구름", 8, 1060, 2700, 48.0, 110.0, FIRE, BOOK, GROUND_TARGET,
                "계속 타오르는 거대한 화염 구름으로 지역을 봉쇄한다.");
        add("maze", "미궁", 8, 960, 2500, 42.0, 80.0, SPACE, BOOK, TARGET,
                "대상의 공간 인식을 뒤틀어 한동안 전투에서 이탈시킨다.");
        add("sunburst", "태양 폭발", 8, 1120, 2800, 52.0, 125.0, LIFE, BOOK, GROUND_TARGET,
                "전장을 백색 태양광으로 뒤덮어 적들을 태우고 실명시킨다.");

        // 9th Circle — archmage miracles and world-scale battle magic.
        add("meteor_swarm", "메테오 스트라이크", 9, 2200, 4200, 72.0, 230.0, FIRE, BOOK, GROUND_TARGET,
                "네 개의 거대 유성을 광범위한 전장에 떨어뜨린다.");
        add("power_word_kill", "죽음의 권능어", 9, 1900, 5000, 52.0, 260.0, LIFE, BOOK, TARGET,
                "약해진 존재의 생명을 명령 한마디로 끊는다.");
        add("prismatic_wall", "무지개 장벽", 9, 2050, 4600, 56.0, 180.0, WARD, BOOK, GROUND_TARGET,
                "일곱 겹 원소 장벽으로 거대한 전선을 봉쇄한다.");
        add("shapechange", "형상변환", 9, 1800, 5200, 0.0, 150.0, LIFE, BOOK, BODY,
                "대마수의 육체 능력을 재현해 압도적인 전투 상태가 된다.");
        add("time_stop", "타임 스톱", 9, 2400, 6000, 48.0, 1.0, SPACE, BOOK, GROUND_SELF,
                "자신을 제외한 주변 적의 시간을 일시적으로 멈춘다.");
        add("true_polymorph", "진정한 변신", 9, 2100, 5600, 40.0, 170.0, LIFE, BOOK, TARGET,
                "생명체의 형상과 전투 능력을 근본적으로 다시 쓴다.");
        add("weird", "기괴한 환영", 9, 2150, 4800, 55.0, 190.0, ARCANE, BOOK, GROUND_TARGET,
                "다수의 적에게 최악의 공포를 실체화해 정신을 파괴한다.");
        add("wish", "소원", 9, 3000, 7200, 0.0, 1.0, ARCANE, BOOK, BODY,
                "현실을 제한적으로 다시 써 생명·마력·주문 회로를 복구한다.");
        add("gate", "월드 게이트", 9, 2300, 5200, 80.0, 1.0, SPACE, BOOK, GROUND_TARGET,
                "거대한 차원문을 열어 학원과 먼 전장을 즉시 연결한다.");
        add("foresight", "예지", 9, 1750, 5000, 0.0, 100.0, ARCANE, BOOK, BODY,
                "다가올 위험을 미리 읽어 회피·방어·시야를 극한까지 높인다.");

        // Fusion results also use established fantasy spell names.
        addFusion("burning_hands", "플레임 버스트", 2, 30, 105, 8.0, 8.0, FIRE, FRONT,
                "손앞에서 부채꼴 화염을 뿜는다.", "fire_bolt", "thunderwave");
        addFusion("ice_knife", "프로스트 랜스", 2, 32, 115, 15.0, 8.0, FROST, FRONT,
                "얼음 칼날을 던져 적중 지점에서 파열시킨다.", "magic_missile", "ray_of_frost");
        addFusion("chromatic_orb", "엘리멘탈 오브", 2, 36, 130, 16.0, 10.0, ARCANE, FRONT,
                "여러 원소를 담은 구체를 발사한다.", "fire_bolt", "magic_missile", "ray_of_frost");
        addFusion("wind_wall", "스톰 배리어", 3, 58, 230, 17.0, 8.0, WIND, GROUND_TARGET,
                "강한 바람 장벽을 세워 적을 밀어낸다.", "gust_of_wind", "shield");
        addFusion("counterspell", "스펠 브레이커", 3, 62, 250, 16.0, 10.0, ARCANE, TARGET,
                "해제와 방패 회로를 겹쳐 적의 강화 마법을 깨뜨린다.", "dispel_magic", "shield");
        addFusion("fire_shield", "블레이징 이지스", 4, 96, 370, 0.0, 16.0, FIRE, BODY,
                "불꽃을 두른 방패로 자신을 보호한다.", "wall_of_fire", "shield");
        addFusion("wall_of_ice", "글레이셜 월", 4, 100, 390, 21.0, 17.0, FROST, GROUND_TARGET,
                "두꺼운 얼음 장벽을 세워 적을 얼린다.", "ice_storm", "shield");
        addFusion("chain_lightning", "체인 라이트닝", 5, 176, 570, 24.0, 27.0, ARCANE, FRONT,
                "번개가 여러 적 사이를 빠르게 연쇄한다.", "haste", "lightning_bolt", "magic_missile");
        addFusion("arcane_hand", "아스트랄 핸드", 5, 165, 560, 22.0, 24.0, ARCANE, TARGET,
                "거대한 비전 손으로 대상을 붙잡고 밀쳐낸다.", "shield", "telekinesis");
        addFusion("teleportation_circle", "아케인 게이트", 5, 184, 720, 34.0, 1.0, SPACE, GROUND_TARGET,
                "안정된 원형 전이 회로로 먼 거리를 건넌다.", "blink", "dimension_door", "misty_step");
    }

    private static void add(String id, String name, int circle, int mana, int cooldown, double range,
                            double power, SpellDefinition.School school, SpellDefinition.Acquisition acquisition,
                            SpellDefinition.SigilAnchor anchor, String description) {
        SPELLS.put(id, new SpellDefinition(id, name, circle, mana, cooldown, range, power,
                school, acquisition, anchor, description, List.of()));
    }

    private static void addFusion(String id, String name, int circle, int mana, int cooldown, double range,
                                  double power, SpellDefinition.School school, SpellDefinition.SigilAnchor anchor,
                                  String description, String... ingredients) {
        List<String> sources = List.of(ingredients);
        SPELLS.put(id, new SpellDefinition(id, name, circle, mana, cooldown, range, power,
                school, FUSION, anchor, description, sources));
        FUSIONS.add(new FusionFormula(id, sources));
    }

    public static Map<String, SpellDefinition> spells() {
        bootstrap();
        return Collections.unmodifiableMap(SPELLS);
    }

    public static List<SpellDefinition> spellsInCircle(int circle) {
        bootstrap();
        return SPELLS.values().stream().filter(spell -> spell.circle() == circle).toList();
    }

    public static List<FusionFormula> fusions() {
        bootstrap();
        return List.copyOf(FUSIONS);
    }

    public static Optional<SpellDefinition> spell(String id) {
        bootstrap();
        return Optional.ofNullable(SPELLS.get(id));
    }

    public static List<SpellDefinition> primerSpells() {
        bootstrap();
        return SPELLS.values().stream().filter(spell -> spell.acquisition() == PRIMER).toList();
    }

    public static List<SpellDefinition> bookSpells() {
        bootstrap();
        return SPELLS.values().stream().filter(spell -> spell.acquisition() == BOOK).toList();
    }

    public static String bookItemId(String spellId) {
        return "spellbook_" + spellId;
    }

    public static Optional<FusionFormula> fusionFor(List<String> ingredients) {
        bootstrap();
        List<String> normalized = normalized(ingredients);
        return FUSIONS.stream().filter(formula -> formula.normalizedIngredients().equals(normalized)).findFirst();
    }

    public static Optional<FusionFormula> fusionFor(String... ingredients) {
        return fusionFor(List.of(ingredients));
    }

    public static List<FusionFormula> candidatesFor(List<String> ingredients) {
        bootstrap();
        List<String> offered = normalized(ingredients);
        if (offered.isEmpty() || offered.size() > 3) return List.of();
        return FUSIONS.stream()
                .filter(formula -> offered.size() <= formula.ingredients().size())
                .filter(formula -> isSortedMultisetSubset(offered, formula.normalizedIngredients()))
                .toList();
    }

    public static boolean canExtend(List<String> ingredients) {
        int size = normalized(ingredients).size();
        return candidatesFor(ingredients).stream().anyMatch(formula -> formula.ingredients().size() > size);
    }

    public static boolean isFusionResult(String spellId) {
        return spell(spellId).map(spell -> spell.acquisition() == FUSION).orElse(false);
    }

    public static int masteryRequired(String resultId) {
        SpellDefinition spell = spell(resultId).orElse(null);
        if (spell == null) return 12;
        int ingredients = Math.max(2, spell.fusionSources().size());
        return Math.max(8, spell.circle() * 6 + (ingredients - 2) * 5);
    }

    public static int masteryTier(int points) {
        if (points <= 0) return 0;
        return Math.min(10, points / 20);
    }

    public static int masteryToNextTier(int points) {
        int tier = masteryTier(points);
        return tier >= 10 ? 0 : (tier + 1) * 20;
    }

    /** New profiles know no spells until the beginner grimoire is read. */
    public static List<String> starterKnownSpells() { return List.of(); }
    public static List<String> starterSlots() { return List.of("", "", "", "", ""); }

    public static int circleInsightThreshold(int circle) {
        return switch (circle) {
            case 2 -> 70;
            case 3 -> 260;
            case 4 -> 720;
            case 5 -> 1600;
            case 6 -> 3400;
            case 7 -> 7200;
            case 8 -> 15000;
            case 9 -> 32000;
            default -> 0;
        };
    }

    public static int emeraldEquivalentPrice(int circle) {
        return arcaneMarkPrice(circle);
    }

    public static int arcaneMarkPrice(int circle) {
        return switch (circle) {
            case 1 -> 40;
            case 2 -> 120;
            case 3 -> 450;
            case 4 -> 1800;
            case 5 -> 8000;
            case 6 -> 32000;
            case 7 -> 140000;
            case 8 -> 650000;
            case 9 -> 3000000;
            default -> 40;
        };
    }

    private static List<String> normalized(List<String> ingredients) {
        if (ingredients == null) return List.of();
        return ingredients.stream().filter(value -> value != null && !value.isBlank()).sorted().toList();
    }

    private static boolean isSortedMultisetSubset(List<String> offered, List<String> formula) {
        int offeredIndex = 0;
        int formulaIndex = 0;
        while (offeredIndex < offered.size() && formulaIndex < formula.size()) {
            int compare = offered.get(offeredIndex).compareTo(formula.get(formulaIndex));
            if (compare == 0) {
                offeredIndex++;
                formulaIndex++;
            } else if (compare > 0) {
                formulaIndex++;
            } else return false;
        }
        return offeredIndex == offered.size();
    }

    public record FusionFormula(String result, List<String> ingredients) {
        public FusionFormula { ingredients = List.copyOf(ingredients); }
        public List<String> normalizedIngredients() { return normalized(ingredients); }
        public boolean matches(List<String> offered) { return normalizedIngredients().equals(normalized(offered)); }
    }
}

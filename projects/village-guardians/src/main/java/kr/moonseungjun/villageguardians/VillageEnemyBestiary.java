package kr.moonseungjun.villageguardians;

import java.util.Optional;

/** Read-only combat dossiers used by the next-wave briefing. */
public final class VillageEnemyBestiary {
    private VillageEnemyBestiary() {}

    public static Optional<VillageEnemyArchetypeSystem.Archetype> find(String displayName) {
        if (displayName == null) return Optional.empty();
        String normalized = displayName.trim();
        for (VillageEnemyArchetypeSystem.Archetype archetype : VillageEnemyArchetypeSystem.Archetype.values()) {
            if (archetype.displayName().equals(normalized)) return Optional.of(archetype);
        }
        return Optional.empty();
    }

    public static Dossier dossier(VillageEnemyArchetypeSystem.Archetype archetype) {
        if (archetype == null) {
            return new Dossier("정보 없음", "정찰 자료 없음", "위협도 미상", "직접 관찰 후 대응하세요.");
        }
        return switch (archetype) {
            case GRUNT -> new Dossier(
                    "전열을 채우는 기본 근접병. 특별한 강화 능력은 없지만 다수가 길목을 막습니다.",
                    "근접 추격과 기본 공격에 집중합니다.",
                    threat(archetype, "보통 · 성벽 압박 1.00배"),
                    "한곳에 모아 광역기와 밀치기로 빠르게 정리하세요.");
            case RUSHER -> new Dossier(
                    "체력을 낮춘 대신 이동 속도를 높인 고기동 척후병입니다.",
                    "빠르게 빈틈을 파고들며 저체력·저공격력 구조라 장기전에는 약합니다.",
                    threat(archetype, "보통 · 침투 위험 높음"),
                    "원거리 선제 공격이나 둔화로 접근 전에 끊어내세요.");
            case BULWARK -> new Dossier(
                    "방패와 철갑을 두른 느린 전열병입니다.",
                    "장시간 피해 저항 II를 얻는 대신 둔화가 걸려 이동이 느립니다.",
                    threat(archetype, "높음 · 전선 고착 위험"),
                    "정면 평타 교환보다 기술·원거리 화력으로 우회해 집중 공격하세요.");
            case SAPPER -> new Dossier(
                    "플레이어보다 시설 파괴를 우선하는 고속 폭파병입니다.",
                    "이동 속도 II를 받고, 시설 공격 피해가 기본의 2.30배입니다.",
                    threat(archetype, "매우 높음 · 시설 특화 2.30배"),
                    "성문에 닿기 전에 최우선 표적으로 지정해 집중 사격하세요.");
            case MARKSMAN -> new Dossier(
                    "후방에서 화살을 쏘며 전열 뒤에 머무는 원거리 병과입니다.",
                    "근접 압박보다 사거리 유지와 지속 사격이 핵심입니다.",
                    threat(archetype, "높음 · 후방 지속 화력"),
                    "엄폐를 이용해 접근하거나 돌진·원거리 기술로 먼저 제거하세요.");
            case SHIELDBREAKER -> new Dossier(
                    "성벽과 방패를 깨는 데 특화된 중근접 파쇄병입니다.",
                    "장시간 힘 II를 받고 시설 피해가 기본의 1.72배입니다.",
                    threat(archetype, "매우 높음 · 시설 특화 1.72배"),
                    "수비형 적보다 먼저 끊어 시설 접촉 시간을 최소화하세요.");
            case HEXER -> new Dossier(
                    "플레이어의 전투 능력을 떨어뜨리는 원거리 약화 주술사입니다.",
                    "약 9블록 안의 플레이어에게 주기적으로 나약함과 둔화를 부여합니다. 주술 웨이브에서는 발동 간격이 더 짧아집니다.",
                    threat(archetype, "높음 · 약화/둔화 제어"),
                    "효과 범위 밖에서 먼저 저격하고, 접근했다면 빠르게 시야를 끊으세요.");
            case WAR_CHANTER -> new Dossier(
                    "주변 적을 강화해 한 무리 전체의 압박을 키우는 지원병입니다.",
                    "약 7초마다 주변 적에게 힘과 속도 강화를 부여합니다.",
                    threat(archetype, "매우 높음 · 적군 광역 버프"),
                    "본대보다 전쟁 고수를 먼저 처치해 강화 순환을 끊으세요.");
            case NECROMANCER -> new Dossier(
                    "적 병력을 회복시키고 보호막을 덧씌우는 후방 지원병입니다.",
                    "약 9초마다 주변 적을 회복하고 흡수 보호막을 부여합니다.",
                    threat(archetype, "매우 높음 · 회복/보호막 지원"),
                    "장기전을 만들지 말고 최우선으로 후방을 파고들어 제거하세요.");
            case TOWER_HUNTER -> new Dossier(
                    "플레이어보다 방어탑 무력화를 노리는 특수 원거리 병과입니다.",
                    "약 9초마다 설치된 방어탑 하나를 7초 동안 정지시키며 이동 속도 II를 받습니다.",
                    threat(archetype, "최우선 · 방어탑 교란"),
                    "탑 사거리 바깥에서 접근하는 순간 직접 화력으로 우선 제거하세요.");
            case SIEGE_BEAST -> new Dossier(
                    "높은 체급으로 성문과 밀집 수비대를 동시에 압박하는 공성 보스입니다.",
                    "약 5초마다 9.5블록 충격파로 피해와 둔화를 주며 시설 피해가 기본의 2.15배입니다.",
                    threat(archetype, "보스 · 공성 2.15배/광역 충격"),
                    "수비대가 뭉치지 말고 거리를 벌린 채 집중 화력으로 성벽 접촉 전에 녹이세요.");
            case IRON_WARLORD -> new Dossier(
                    "중장갑으로 버티며 주변 병력을 직접 지휘하는 전쟁군주 보스입니다.",
                    "약 6초마다 주변 적에게 힘 II와 피해 저항을 부여하고 시설 피해도 1.75배입니다.",
                    threat(archetype, "보스 · 지휘 버프/공성 1.75배"),
                    "호위병과 분리한 뒤 군주를 먼저 집중 공격해 강화 루프를 끊으세요.");
            case PLAGUE_ARCHON -> new Dossier(
                    "독과 회복을 동시에 사용하는 지속전 특화 보스이며 방어탑을 선호합니다.",
                    "약 5.5초마다 11블록 범위에 독 피해를 주고 주변 적을 회복합니다.",
                    threat(archetype, "보스 · 독 광역/회복/탑 우선"),
                    "밀집을 피하고 장기전을 금지하세요. 화력을 한 번에 몰아 회복 전에 처치해야 합니다.");
            case DREAD_KNIGHT -> new Dossier(
                    "암흑과 흡혈로 근접 전선을 무너뜨리는 고기동 보스입니다.",
                    "약 4.5초마다 10블록 안에 암흑·나약함·마법 피해를 주고, 적중 수에 따라 자신의 체력을 회복합니다.",
                    threat(archetype, "보스 · 광역 약화/흡혈/공성 1.58배"),
                    "여럿이 동시에 근접하지 말고 거리 유지와 교대 집중 공격으로 흡혈량을 제한하세요.");
        };
    }

    private static String threat(VillageEnemyArchetypeSystem.Archetype archetype, String base) {
        float multiplier = VillageEnemyArchetypeSystem.structureDamageMultiplier(archetype);
        if (multiplier <= 1.001f || base.contains("배")) return base;
        return base + " · 시설 피해 " + String.format(java.util.Locale.ROOT, "%.2f", multiplier) + "배";
    }

    public record Dossier(String overview, String ability, String threat, String counter) {}
}

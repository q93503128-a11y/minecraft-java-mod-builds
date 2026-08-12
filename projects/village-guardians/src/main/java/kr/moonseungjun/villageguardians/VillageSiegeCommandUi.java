package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Town-hall command surface for wall segments and player-placed turrets. */
public final class VillageSiegeCommandUi {
    private static final String SEP = "\u001F";
    private VillageSiegeCommandUi() {}

    public static void open(ServerPlayer player) {
        if (!nearTownHall(player)) return;
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageSiegeSegmentSystem.Segment segment : VillageSiegeSegmentSystem.Segment.values()) {
            actions.add("siege_segment_open:" + segment.id());
            labels.add(segment.displayName() + "|" + VillageSiegeSegmentSystem.statusLine(segment));
        }
        actions.add("siege_turret_catalog");
        labels.add("직접 배치 포탑|설치 " + VillagePlacedTurretSystem.count() + " / "
                + VillagePlacedTurretSystem.capacity() + " · 10개 계열 배치·수리·강화·철거");
        actions.add("siege_turret_repair_all");
        labels.add("손상 포탑 일괄 수리|파괴·손상 포탑을 보유 주화 범위에서 순차 복구");
        actions.add("open_wave_intel");
        labels.add("다음 밤 정찰|주공·별동대·병과·수량·공성 병력·보스·전장 상황 확인");
        actions.add("open_dashboard");
        labels.add("회관 전체 지휘|건물·직업·다른 시설 관리로 돌아가기");
        send(player, "management", "공성 방어 지휘", "성벽은 구역별 HP를 가지며 0이 된 위치에만 실제 돌파구가 생깁니다.\n"
                + "배치 포탑 " + VillagePlacedTurretSystem.activeCount() + "기 가동 · 설치 "
                + VillagePlacedTurretSystem.count() + "/" + VillagePlacedTurretSystem.capacity()
                + " · 기존 성루는 관측 구조물이며 실전 화력은 직접 배치 포탑이 담당합니다.", actions, labels);
    }

    public static void openSegment(ServerPlayer player, VillageSiegeSegmentSystem.Segment segment) {
        if (!nearTownHall(player) || segment == null) return;
        int current = VillageSiegeSegmentSystem.currentHp(segment);
        int maximum = VillageSiegeSegmentSystem.maxHp(segment);
        int missing = Math.max(0, maximum - current);
        int repairCost = segment == VillageSiegeSegmentSystem.Segment.NORTH_GATE
                ? (missing <= 0 ? 0 : Math.max(20, (missing + 7) / 8))
                : (missing <= 0 ? 0 : Math.max(35, (missing + 8) / 9));
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        actions.add("siege_segment_repair:" + segment.id());
        labels.add("국소 손상 수리|" + (missing <= 0 ? "현재 완전함" : "예상 비용 " + repairCost
                + " · 손상된 위치만 복원하며 블록 아이템은 드롭하지 않음"));
        actions.add("siege_segment_upgrade:" + segment.id());
        labels.add("방어 구역 강화|현재 강화 " + VillageSiegeSegmentSystem.upgradeLevel(segment)
                + " · 최대 HP와 피해 경감 증가");
        actions.add("siege_command");
        labels.add("성벽·포탑 목록|공성 방어 지휘로 돌아가기");
        send(player, "management", segment.displayName(), VillageSiegeSegmentSystem.statusLine(segment)
                + "\nHP 70% 이하 균열 · 40% 이하 부분 손상 · 0이면 공격 지점 중심 5블록 폭 실제 돌파구 발생", actions, labels);
    }

    public static void openTurretCatalog(ServerPlayer player) {
        if (!nearTownHall(player)) return;
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillagePlacedTurretSystem.TurretType type : VillagePlacedTurretSystem.TurretType.values()) {
            actions.add("siege_turret_select:" + type.id());
            labels.add(type.displayName() + " · 주화 " + type.installCost()
                    + "|" + type.role() + " · 피해 " + type.damage() + " · 사거리 " + type.range()
                    + " · 기본 HP " + type.baseHp());
        }
        for (VillagePlacedTurretSystem.TurretState state : VillagePlacedTurretSystem.states()) {
            actions.add("siege_turret_open:" + state.id());
            labels.add(state.summary() + "|위치 " + state.pos().getX() + ", " + state.pos().getY() + ", " + state.pos().getZ());
        }
        actions.add("siege_command"); labels.add("성벽·포탑 지휘|이전 화면으로 돌아가기");
        send(player, "tower_control", "직접 배치 포탑", "포탑 선택 → 월드 바닥 우클릭 미리보기 → 같은 위치 재클릭 확정.\n"
                + "통행로·건물 출입구·북문 전면·8블록 이내 중복 설치는 서버가 거부합니다.", actions, labels);
    }

    public static void openTurret(ServerPlayer player, int id) {
        if (!nearTownHall(player)) return;
        VillagePlacedTurretSystem.TurretState state = VillagePlacedTurretSystem.states().stream()
                .filter(value -> value.id() == id).findFirst().orElse(null);
        if (state == null) { openTurretCatalog(player); return; }
        List<String> actions = List.of("siege_turret_repair:" + id, "siege_turret_upgrade:" + id,
                "siege_turret_dismantle:" + id, "siege_turret_catalog");
        List<String> labels = List.of(
                "수리|HP 0의 잔해도 다시 가동 상태로 복구",
                "강화|Lv.5까지 HP·피해·사거리·공격 주기 강화",
                "철거|블록 드롭 없이 철거하고 일부 주화 환급",
                "포탑 목록|다른 설치 포탑과 신규 배치 계열 보기");
        send(player, "tower_detail", state.type().displayName() + " #" + id,
                state.summary() + "\n역할: " + state.type().role() + " · 피해 " + state.type().damage()
                        + " · 기본 사거리 " + state.type().range() + " · 공격 주기 " + state.type().interval() + "틱",
                actions, labels);
    }

    private static boolean nearTownHall(ServerPlayer player) {
        if (VillageLocationRules.isNearTownHall(player)) return true;
        player.sendSystemMessage(Component.literal("§c성벽·포탑 지휘는 마을 회관 지휘대 근처에서만 가능합니다."));
        return false;
    }

    private static void send(ServerPlayer player, String screenId, String title, String body,
                             List<String> actions, List<String> labels) {
        VillageNetwork.open(player, new VillageNetwork.OpenVillageUiPayload(
                screenId, title, body, String.join(SEP, actions), String.join(SEP, labels)));
    }
}

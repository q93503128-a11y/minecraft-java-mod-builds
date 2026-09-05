package io.github.q93503128.turnbound.client;

import java.util.List;

/** Stable schematic map metadata derived from the authored Aster March coordinates. */
final class AsterMarchMapData {
    enum Kind { FACILITY, HUNT, BOSS, RELAY }
    record Marker(String label, String info, double x, double z, Kind kind) {}
    record Region(String label, int minX, int maxX, int minZ, int maxZ, String info) {}

    static final int MIN = -512;
    static final int MAX = 512;

    static final List<Region> REGIONS = List.of(
            new Region("라디아", -128, 128, -112, 128, "중앙 거점 · 시설 / 퀘스트 / 계전소"),
            new Region("남문 초원", -80, 430, 120, 360, "권장 Lv.1~6 · B01 들이받는 왕 그라울"),
            new Region("그늘숲", -220, 160, -500, -120, "권장 Lv.5~10 · B02 가시어미 베르나"),
            new Region("붕괴 수로", -500, -130, -170, 210, "권장 Lv.8~13 · B03 수문관리기 ORO-7"),
            new Region("잿불 채석장", -160, 210, 300, 500, "권장 Lv.11~16 · B04 재의 거상 콜바크"),
            new Region("구 중계소", 250, 500, -450, -170, "권장 Lv.15~20 · B05 균열감시자 세라크")
    );

    static final List<Marker> MARKERS = List.of(
            new Marker("Director Iven", "메인 진행", 0.5, 6.5, Kind.FACILITY),
            new Marker("라디아 계전소", "빠른 이동", 0, 24, Kind.RELAY),
            new Marker("Echo Archive", "소환", -56, 22, Kind.FACILITY),
            new Marker("Forge Annex", "장비 강화", 56, 22, Kind.FACILITY),
            new Marker("Market Row", "장비 상점", -57, 55, Kind.FACILITY),
            new Marker("Training Yard", "전투 훈련", 57, 38, Kind.FACILITY),
            new Marker("Rift Gate", "후반 도전", -82, -54, Kind.FACILITY),
            new Marker("Memorial Steps", "캐릭터 퀘스트", -28, -47, Kind.FACILITY),
            new Marker("Clock Tower", "캐릭터 사건", 22, -49, Kind.FACILITY),
            new Marker("Barracks", "수비대 기록", 72, -11, Kind.FACILITY),
            new Marker("South Gate", "남문 초원 출구", 0, 104, Kind.FACILITY),
            new Marker("남문 초원", "권장 Lv.1~6", 190, 230, Kind.HUNT),
            new Marker("그늘숲", "권장 Lv.5~10", -40, -300, Kind.HUNT),
            new Marker("붕괴 수로", "권장 Lv.8~13", -320, 20, Kind.HUNT),
            new Marker("잿불 채석장", "권장 Lv.11~16", 20, 405, Kind.HUNT),
            new Marker("구 중계소", "권장 Lv.15~20", 365, -305, Kind.HUNT),
            new Marker("그라울", "B01", 355, 245, Kind.BOSS),
            new Marker("베르나", "B02", -35, -440, Kind.BOSS),
            new Marker("ORO-7", "B03", -430, 35, Kind.BOSS),
            new Marker("콜바크", "B04", 65, 455, Kind.BOSS),
            new Marker("세라크", "B05", 430, -350, Kind.BOSS)
    );

    private AsterMarchMapData() {}

    static Marker nearest(double x, double z) {
        Marker best = null; double bestDistance = Double.MAX_VALUE;
        for (Marker marker : MARKERS) {
            double dx = marker.x - x, dz = marker.z - z, distance = dx * dx + dz * dz;
            if (distance < bestDistance) { bestDistance = distance; best = marker; }
        }
        return best;
    }
}

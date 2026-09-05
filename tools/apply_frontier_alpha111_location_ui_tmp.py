#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
P = ROOT / "projects/frontier-settlement"
JAVA = P / "src/main/java/kr/moonseungjun/frontiersettlement"
CLIENT = JAVA / "client"


def read(path):
    return path.read_text(encoding="utf-8")


def write(path, text):
    path.write_text(text, encoding="utf-8")


def replace_once(path, old, new):
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one match, found {count}: {old[:100]!r}")
    write(path, text.replace(old, new, 1))

# version
props = P / "gradle.properties"
replace_once(props, "mod_version=0.1.0-alpha.110", "mod_version=0.1.0-alpha.111")
with props.open("a", encoding="utf-8") as f:
    f.write("\n# Alpha.111 settlement location UX: M > infrastructure has an explicit base-location button and dedicated saved-coordinate/distance/direction screen for the main settlement and completed outposts.\n")

# Dedicated location screen.
location_screen = CLIENT / "SettlementLocationScreen.java"
write(location_screen, r'''package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.SettlementContextTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Explicit, checkpoint-independent navigation view for the saved main settlement and outposts. */
public final class SettlementLocationScreen extends Screen {
    private static final int PANEL_BG = 0xF0121418;
    private static final int PANEL_EDGE = 0xFFD0A45C;
    private static final int CARD_BG = 0xB01A1D21;
    private static final int MAIN_EDGE = 0xFFFFD58A;
    private static final int OUTPOST_EDGE = 0xFF65B8C8;
    private static final int TEXT_PRIMARY = 0xFFF4F1EA;
    private static final int TEXT_SECONDARY = 0xFFBEB7AA;
    private static final int TEXT_MUTED = 0xFF918B82;

    private final Screen parent;
    private int panelX, panelY, panelWidth, panelHeight;

    public SettlementLocationScreen(Screen parent) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("거점 위치"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(590, Math.max(310, this.width - 16));
        panelHeight = Math.min(340, Math.max(220, this.height - 16));
        panelX = (this.width - panelWidth) / 2;
        panelY = Math.max(8, (this.height - panelHeight) / 2);
        addRenderableWidget(Button.builder(Component.literal("돌아가기"), b -> this.minecraft.gui.setScreen(parent))
                .bounds(panelX + panelWidth - 82, panelY + panelHeight - 30, 68, 20).build());
    }

    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float p) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float p) {
        g.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BG);
        g.fill(panelX, panelY, panelX + 4, panelY + panelHeight, PANEL_EDGE);
        int x = panelX + 16;
        g.text(this.font, Component.literal("거점 위치"), x, panelY + 14, TEXT_PRIMARY, true);
        g.text(this.font, Component.literal("체크포인트와 무관한 월드 저장 좌표 · 본진과 완공 전초기지"),
                x, panelY + 31, TEXT_SECONDARY, false);

        List<SettlementContextTarget> bases = bases();
        if (bases.isEmpty()) {
            g.fill(x - 4, panelY + 53, panelX + panelWidth - 14, panelY + 94, CARD_BG);
            g.text(this.font, Component.literal("위치 정보 동기화 대기 중…"), x + 6, panelY + 67, TEXT_MUTED, false);
            g.text(this.font, Component.literal("월드에 다시 들어오거나 M 메뉴를 다시 열어 주세요."), x + 6, panelY + 80, TEXT_MUTED, false);
            super.extractRenderState(g, mx, my, p);
            return;
        }

        var mc = Minecraft.getInstance();
        var player = mc.player;
        boolean overworld = mc.level != null && mc.level.dimension().equals(Level.OVERWORLD);
        bases.sort((a, b) -> {
            boolean mainA = "settlement".equals(a.kind());
            boolean mainB = "settlement".equals(b.kind());
            if (mainA != mainB) return mainA ? -1 : 1;
            if (player != null && overworld) {
                return Long.compare(distanceSq(player.getX(), player.getZ(), a), distanceSq(player.getX(), player.getZ(), b));
            }
            return Integer.compare(a.markerX(), b.markerX());
        });

        int top = panelY + 53;
        int bottom = panelY + panelHeight - 40;
        int rowHeight = 36;
        int maxRows = Math.max(1, (bottom - top) / rowHeight);
        int visible = Math.min(maxRows, bases.size());
        for (int i = 0; i < visible; i++) {
            SettlementContextTarget target = bases.get(i);
            boolean main = "settlement".equals(target.kind());
            int y = top + i * rowHeight;
            g.fill(x - 4, y, panelX + panelWidth - 14, y + 30, CARD_BG);
            g.fill(x - 4, y, x - 1, y + 30, main ? MAIN_EDGE : OUTPOST_EDGE);

            String label = main ? "본진" : target.title();
            g.text(this.font, Component.literal(label), x + 6, y + 5, main ? MAIN_EDGE : OUTPOST_EDGE, true);
            String coords = "X " + target.markerX() + "   Y " + target.markerY() + "   Z " + target.markerZ();
            if (player != null && overworld) {
                long dx = Math.round(target.markerX() + 0.5D - player.getX());
                long dz = Math.round(target.markerZ() + 0.5D - player.getZ());
                long distance = Math.round(Math.sqrt((double) dx * dx + (double) dz * dz));
                coords += "   ·   " + distance + "블록 " + directionName(dx, dz);
            } else {
                coords += "   ·   오버월드";
            }
            g.text(this.font, Component.literal(trim(coords, panelWidth - 48)), x + 6, y + 18, TEXT_SECONDARY, false);
        }
        if (bases.size() > visible) {
            g.text(this.font, Component.literal("외 " + (bases.size() - visible) + "개 거점 · 화면이 넓으면 더 표시됩니다."),
                    x, bottom + 2, TEXT_MUTED, false);
        }
        super.extractRenderState(g, mx, my, p);
    }

    private List<SettlementContextTarget> bases() {
        List<SettlementContextTarget> result = new ArrayList<>();
        for (SettlementContextTarget target : ClientSettlementState.context().targets()) {
            if ("settlement".equals(target.kind()) || "outpost".equals(target.kind())) result.add(target);
        }
        return result;
    }

    private static long distanceSq(double x, double z, SettlementContextTarget target) {
        long dx = Math.round(target.markerX() + 0.5D - x);
        long dz = Math.round(target.markerZ() + 0.5D - z);
        return dx * dx + dz * dz;
    }

    private static String directionName(long dx, long dz) {
        if (dx == 0L && dz == 0L) return "현재 위치";
        String[] names = {"북", "북동", "동", "남동", "남", "남서", "서", "북서"};
        double angle = Math.atan2((double) dx, (double) -dz);
        return names[Math.floorMod((int) Math.round(angle / (Math.PI / 4.0D)), 8)];
    }

    private String trim(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) return text;
        String out = text;
        while (!out.isEmpty() && this.font.width(out + "…") > maxWidth) out = out.substring(0, out.length() - 1);
        return out + "…";
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
''')

# Add explicit button and move old inline helper below the fourth button.
palette = CLIENT / "BuildingPaletteScreen.java"
replace_once(palette,
'''        addRenderableWidget(Button.builder(Component.literal("토목 평탄화   · 절토/성토"),
                b -> { CivilWorkPlacementClient.beginPlacement(); this.minecraft.gui.setScreen(null); })
                .bounds(x, y + 62, width, 23).build());''',
'''        addRenderableWidget(Button.builder(Component.literal("토목 평탄화   · 절토/성토"),
                b -> { CivilWorkPlacementClient.beginPlacement(); this.minecraft.gui.setScreen(null); })
                .bounds(x, y + 62, width, 23).build());
        addRenderableWidget(Button.builder(Component.literal("거점 위치   · 본진/전초 좌표·방향"),
                b -> this.minecraft.gui.setScreen(new SettlementLocationScreen(this)))
                .bounds(x, y + 93, width, 23).build());''')
replace_once(palette, "int infoY = contentY + 102;", "int infoY = contentY + 133;")
replace_once(palette,
'''            g.text(this.font, Component.literal("인프라는 건물보다 월드의 연결 관계가 중요합니다."),
                    contentX, infoY + 9, TEXT_SECONDARY, false);
            g.text(this.font, Component.literal("메뉴에서 작업을 고른 뒤 월드 프리뷰로 위치와 범위를 확인하세요."),
                    contentX, infoY + 22, TEXT_MUTED, false);
            drawSettlementLocations(g, infoY + 39);''',
'''            g.text(this.font, Component.literal("본진을 잃어버렸다면 ‘거점 위치’에서 저장 좌표와 방향을 확인하세요."),
                    contentX, infoY + 9, TEXT_SECONDARY, false);
            g.text(this.font, Component.literal("체크포인트를 바꿔도 본진·전초기지 좌표는 마을 세이브에 남습니다."),
                    contentX, infoY + 22, TEXT_MUTED, false);''')

# Guide discoverability.
guide = CLIENT / "SettlementGuideScreen.java"
replace_once(guide,
'''            case 3 -> draw(g, x, y, "4. 영토와 물류",
                    "M → 도로 계획으로 마을 밖까지 길을 연결합니다.",
                    "도로 끝에 전초기지를 세워 영토·생산 거점을 넓힙니다.",
                    "수레 정거장·시장·방어·제작 시설이 차례로 열립니다.",
                    "언로드 지역은 강제로 로드하지 않으며 운송도 멈춥니다.");''',
'''            case 3 -> draw(g, x, y, "4. 영토와 물류",
                    "M → 인프라 → 거점 위치에서 본진·전초 좌표와 방향을 확인합니다.",
                    "도로 끝에 전초기지를 세워 영토·생산 거점을 넓힙니다.",
                    "체크포인트를 바꿔도 거점 저장 좌표는 사라지지 않습니다.",
                    "언로드 지역은 강제로 로드하지 않으며 운송도 멈춥니다.");''')

# Lock metadata.
lock_path = P / "COMPANION_LOCK.json"
lock = json.loads(read(lock_path))
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.110":
    raise RuntimeError("COMPANION_LOCK frontier target drift")
lock["target"]["frontier_settlement"] = "0.1.0-alpha.111"
lock.setdefault("notes", []).append(
    "Alpha.111 turns the saved main-settlement/outpost context data into an explicit M > infrastructure > base-location screen with coordinates, Overworld distance and 8-way direction; checkpoint/waypoint state is not an authority for settlement positions."
)
write(lock_path, json.dumps(lock, ensure_ascii=False, indent=2) + "\n")

# Verifier.
verifier = P / "tools/test_current_source.py"
replace_once(verifier,
        'require("mod_version=0.1.0-alpha.110" in gradle, "current verifier/version drift")',
        'require("mod_version=0.1.0-alpha.111" in gradle, "current verifier/version drift")')
replace_once(verifier,
'''require("drawSettlementLocations" in palette and "directionName" in palette and '\"outpost\".equals(target.kind())' in palette,
        "settlement/outpost coordinate navigation UI missing")''',
'''location_screen = text(JAVA / "client/SettlementLocationScreen.java")
require("거점 위치   · 본진/전초 좌표·방향" in palette and "new SettlementLocationScreen(this)" in palette,
        "explicit settlement-location button missing from infrastructure menu")
require('"settlement".equals(target.kind())' in location_screen and '"outpost".equals(target.kind())' in location_screen,
        "dedicated location screen does not enumerate main settlement and outposts")
require("markerX()" in location_screen and "markerY()" in location_screen and "markerZ()" in location_screen,
        "dedicated location screen does not expose saved coordinates")
require("distanceSq" in location_screen and "directionName" in location_screen and "오버월드" in location_screen,
        "dedicated location screen distance/direction/dimension behavior missing")''')
replace_once(verifier,
        'print("CURRENT SOURCE CHECK PASS: alpha110 scalable parallel construction crews + alpha109 navigation + prior authority invariants")',
        'print("CURRENT SOURCE CHECK PASS: alpha111 explicit settlement location UX + alpha110 scalable parallel construction crews + prior authority invariants")')

print("PATCH APPLIED: Frontier alpha111 explicit settlement location UI")

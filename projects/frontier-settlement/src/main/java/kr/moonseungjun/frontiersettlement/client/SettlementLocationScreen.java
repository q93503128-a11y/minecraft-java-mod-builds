package kr.moonseungjun.frontiersettlement.client;

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

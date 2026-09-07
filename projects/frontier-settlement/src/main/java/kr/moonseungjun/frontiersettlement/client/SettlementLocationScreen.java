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
    private final Screen parent;
    private int panelX, panelY, panelWidth, panelHeight;

    public SettlementLocationScreen(Screen parent) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("거점 위치"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(620, Math.max(310, this.width - FrontierUiTheme.L));
        panelHeight = Math.min(350, Math.max(220, this.height - FrontierUiTheme.L));
        panelX = (this.width - panelWidth) / 2;
        panelY = Math.max(FrontierUiTheme.S, (this.height - panelHeight) / 2);
        addRenderableWidget(Button.builder(Component.literal("돌아가기"), b -> this.minecraft.gui.setScreen(parent))
                .bounds(panelX + panelWidth - 82, panelY + panelHeight - 30, 68, 20).build());
    }

    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float p) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float p) {
        FrontierUiTheme.panel(g, panelX, panelY, panelWidth, panelHeight);
        int x = panelX + FrontierUiTheme.M;
        int innerWidth = panelWidth - FrontierUiTheme.M * 2;
        g.text(this.font, Component.literal("거점 위치"), x, panelY + FrontierUiTheme.M,
                FrontierUiTheme.TEXT_PRIMARY, true);
        g.text(this.font, Component.literal("본진과 완공 전초기지 · 체크포인트와 무관한 저장 좌표"),
                x, panelY + 31, FrontierUiTheme.TEXT_SECONDARY, false);
        FrontierUiTheme.divider(g, x, panelY + 46, innerWidth);

        List<SettlementContextTarget> bases = bases();
        if (bases.isEmpty()) {
            int emptyY = panelY + 58;
            FrontierUiTheme.surface(g, x, emptyY, innerWidth, 54);
            g.text(this.font, Component.literal("동기화 대기 중"), x + FrontierUiTheme.M, emptyY + 11,
                    FrontierUiTheme.WARNING, true);
            g.text(this.font, Component.literal("월드에 다시 들어오거나 M 메뉴를 다시 열어 주세요."),
                    x + FrontierUiTheme.M, emptyY + 29, FrontierUiTheme.TEXT_SECONDARY, false);
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

        int top = panelY + 58;
        int bottom = panelY + panelHeight - 40;
        int rowHeight = 35;
        int maxRows = Math.max(1, (bottom - top) / rowHeight);
        int visible = Math.min(maxRows, bases.size());
        for (int i = 0; i < visible; i++) {
            SettlementContextTarget target = bases.get(i);
            boolean main = "settlement".equals(target.kind());
            int y = top + i * rowHeight;
            int stateColor = main ? FrontierUiTheme.PRIMARY : FrontierUiTheme.SECONDARY;
            g.fill(x, y, x + innerWidth, y + 29, FrontierUiTheme.SURFACE_SOFT);
            g.fill(x, y, x + 3, y + 29, stateColor);

            String label = main ? "본진" : target.title();
            g.text(this.font, Component.literal(trim(label, Math.max(40, innerWidth / 3))),
                    x + FrontierUiTheme.S, y + 5, main ? FrontierUiTheme.ACCENT : FrontierUiTheme.TEXT_PRIMARY, true);
            String coords = "X " + target.markerX() + "   Y " + target.markerY() + "   Z " + target.markerZ();
            if (player != null && overworld) {
                long dx = Math.round(target.markerX() + 0.5D - player.getX());
                long dz = Math.round(target.markerZ() + 0.5D - player.getZ());
                long distance = Math.round(Math.sqrt((double) dx * dx + (double) dz * dz));
                coords += "   ·   " + distance + "블록 " + directionName(dx, dz);
            } else {
                coords += "   ·   오버월드";
            }
            g.text(this.font, Component.literal(trim(coords, innerWidth - FrontierUiTheme.L)),
                    x + FrontierUiTheme.S, y + 18, FrontierUiTheme.TEXT_SECONDARY, false);
        }
        if (bases.size() > visible) {
            g.text(this.font, Component.literal("외 " + (bases.size() - visible) + "개 거점 · 창 크기를 키우면 더 표시됩니다."),
                    x, bottom + 2, FrontierUiTheme.TEXT_MUTED, false);
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

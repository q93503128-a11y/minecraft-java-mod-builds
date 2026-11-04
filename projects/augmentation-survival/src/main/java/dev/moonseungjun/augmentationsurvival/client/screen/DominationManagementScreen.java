package dev.moonseungjun.augmentationsurvival.client.screen;

import dev.moonseungjun.augmentationsurvival.client.ClientDominationState;
import dev.moonseungjun.augmentationsurvival.network.DominationManageRequestPayload;
import dev.moonseungjun.augmentationsurvival.network.DominationRosterPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Compact owner-only roster: loaded, distant and deceased companions all remain releasable. */
public final class DominationManagementScreen extends Screen {
    private static final int BACKGROUND = 0xEA090D10;
    private static final int PANEL = 0xFF172127;
    private static final int ROW = 0xFF202D33;
    private static final int TEXT = 0xFFEAF0EF;
    private static final int SUBTLE = 0xFF9AAEB4;
    private static final int ACCENT = 0xFF9ED2C9;
    private static final int BORDER = 0xFF44555A;
    private static final int DANGER = 0xFFE4A09B;
    private final boolean returnToLoadout;
    private int page;
    private boolean confirmReleaseAll;

    public DominationManagementScreen(boolean returnToLoadout) {
        super(Component.literal("지배 관리"));
        this.returnToLoadout = returnToLoadout;
    }

    @Override protected void init() {
        ClientPlayNetworking.send(new DominationManageRequestPayload(DominationManageRequestPayload.OPEN, ""));
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int w = Math.max(1, Math.min(470, width - 12));
        int h = Math.max(1, Math.min(336, height - 12));
        int x = (width - w) / 2, y = (height - h) / 2;
        g.fill(0, 0, width, height, BACKGROUND);
        g.fill(x, y, x + w, y + h, PANEL);
        g.outline(x, y, w, h, BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + 4, ACCENT);
        g.text(font, "지배 관리", x + 14, y + 13, TEXT, false);
        DominationRosterPayload roster = ClientDominationState.roster();
        String count = roster.occupied() + " / " + roster.maximum();
        g.text(font, count, x + w - 13 - font.width(count), y + 13, ACCENT, false);
        g.text(font, "동료를 선택해 원격으로 지배를 해제할 수 있습니다.", x + 14, y + 34, SUBTLE, false);
        int rows = pageSize(h);
        int pages = Math.max(1, (roster.members().size() + rows - 1) / rows);
        page = Math.max(0, Math.min(page, pages - 1));
        int start = page * rows;
        int listTop = y + 58;
        for (int j = 0; j < rows && start + j < roster.members().size(); j++) {
            DominationRosterPayload.Member member = roster.members().get(start + j);
            int ry = listTop + j * 38;
            g.fill(x + 12, ry, x + w - 12, ry + 34, ROW);
            String name = displayName(member.typeId());
            String label = name.length() > 21 ? name.substring(0, 20) + "…" : name;
            g.text(font, label, x + 21, ry + 5, TEXT, false);
            String status = member.reserved()
                    ? "소생 대기 " + member.reviveSeconds() + "초"
                    : member.loaded() ? "동행 중" : "멀리 있음 · " + place(member.dimension());
            g.text(font, status, x + 21, ry + 20, member.reserved() ? ACCENT : SUBTLE, false);
            boolean hover = inside(mouseX, mouseY, x + w - 82, ry + 7, 61, 20);
            g.fill(x + w - 82, ry + 7, x + w - 21, ry + 27, hover ? 0xFF6E3B40 : 0xFF493037);
            g.outline(x + w - 82, ry + 7, 61, 20, DANGER);
            g.text(font, "해제", x + w - 82 + 30 - font.width("해제") / 2, ry + 13, TEXT, false);
        }
        if (pages > 1) {
            g.text(font, "◀", x + 15, y + h - 43, page > 0 ? TEXT : SUBTLE, false);
            String pagination = (page + 1) + " / " + pages;
            g.text(font, pagination, x + w / 2 - font.width(pagination) / 2, y + h - 43, SUBTLE, false);
            g.text(font, "▶", x + w - 26, y + h - 43, page + 1 < pages ? TEXT : SUBTLE, false);
        }
        int by = y + h - 27;
        g.fill(x + 13, by, x + 119, by + 19, confirmReleaseAll ? 0xFF854349 : ROW);
        g.outline(x + 13, by, 106, 19, BORDER);
        String allText = confirmReleaseAll ? "정말 모두 해제?" : "모두 해제";
        g.text(font, allText, x + 66 - font.width(allText) / 2, by + 6, TEXT, false);
        g.fill(x + w - 93, by, x + w - 13, by + 19, ROW);
        g.outline(x + w - 93, by, 80, 19, BORDER);
        String close = returnToLoadout ? "뒤로" : "닫기";
        g.text(font, close, x + w - 53 - font.width(close) / 2, by + 6, TEXT, false);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int w = Math.max(1, Math.min(470, width - 12)), h = Math.max(1, Math.min(336, height - 12));
        int x = (width - w) / 2, y = (height - h) / 2;
        int by = y + h - 27;
        if (inside(event.x(), event.y(), x + w - 93, by, 80, 19)) {
            onClose();
            return true;
        }
        DominationRosterPayload roster = ClientDominationState.roster();
        if (inside(event.x(), event.y(), x + 13, by, 106, 19) && !roster.members().isEmpty()) {
            if (!confirmReleaseAll) {
                confirmReleaseAll = true;
            } else {
                ClientPlayNetworking.send(new DominationManageRequestPayload(
                        DominationManageRequestPayload.RELEASE_ALL, ""));
                confirmReleaseAll = false;
            }
            return true;
        }
        int rows = pageSize(h);
        int pages = Math.max(1, (roster.members().size() + rows - 1) / rows);
        if (pages > 1 && inside(event.x(), event.y(), x + 9, y + h - 48, 31, 22) && page > 0) {
            page--;
            return true;
        }
        if (pages > 1 && inside(event.x(), event.y(), x + w - 36, y + h - 48, 31, 22) && page + 1 < pages) {
            page++;
            return true;
        }
        for (int j = 0; j < rows && page * rows + j < roster.members().size(); j++) {
            if (inside(event.x(), event.y(), x + w - 82, y + 58 + j * 38 + 7, 61, 20)) {
                ClientPlayNetworking.send(new DominationManageRequestPayload(
                        DominationManageRequestPayload.RELEASE,
                        roster.members().get(page * rows + j).entityId()));
                confirmReleaseAll = false;
                return true;
            }
        }
        confirmReleaseAll = false;
        return super.mouseClicked(event, doubleClick);
    }

    @Override public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(
                returnToLoadout ? new ActiveLoadoutScreen() : null);
    }

    @Override public boolean isPauseScreen() { return false; }

    private static int pageSize(int height) {
        return Math.max(1, Math.min(6, (height - 110) / 38));
    }
    private static boolean inside(double px, double py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }
    private static String displayName(String type) {
        if (!type.contains(":")) return type;
        return Component.translatable("entity." + type.replace(':', '.')).getString();
    }
    private static String place(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> "오버월드";
            case "minecraft:the_nether" -> "네더";
            case "minecraft:the_end" -> "엔드";
            default -> "다른 차원";
        };
    }
}

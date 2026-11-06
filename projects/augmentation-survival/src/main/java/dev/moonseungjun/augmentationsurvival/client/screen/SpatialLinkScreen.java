package dev.moonseungjun.augmentationsurvival.client.screen;

import dev.moonseungjun.augmentationsurvival.network.SpatialLinkActionPayload;
import dev.moonseungjun.augmentationsurvival.network.SpatialLinkSnapshotPayload;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Search-first, responsive controls for personal and shared spatial links. */
public final class SpatialLinkScreen extends Screen {
    private static final int SHADE = 0xA8000000, STONE = 0xF022292B, SURFACE = 0xFF303A3B;
    private static final int ALT = 0xFF394442, SELECTED = 0xFF514237, TEXT = 0xFFF1E9D8;
    private static final int MUTED = 0xFFB2B6A8, COPPER = 0xFFBE8058, SHARED = 0xFFAFC3E8;
    private static final int ROWS_WIDE = 10;

    private enum View { LIST, DETAIL, REGISTER, MANAGE }
    private enum ManagePage { FAVORITES, RENAME, SHARE, DELETE }

    private List<SpatialLinkSnapshotPayload.GateView> gates;
    private String query = "", registerName = "", renameName = "", sharePlayer = "", selectedId = "";
    private int scroll;
    private View view = View.LIST;
    private ManagePage managePage = ManagePage.FAVORITES;
    private boolean deleteArmed;
    private long deleteArmedAt;
    private Layout layout;
    private EditBox searchBox, registerBox, renameBox, shareBox;
    private Button registerButton, detailButton, moveButton, defaultMoveButton, manageButton;
    private Button favoriteButton, defaultButton, renameButton, shareButton, unshareButton, deleteButton;

    public SpatialLinkScreen(List<SpatialLinkSnapshotPayload.GateView> gates) {
        super(Component.translatable("screen.augmentation_survival.spatial_link"));
        this.gates = List.copyOf(gates);
        ensureSelection();
        if (this.gates.size() == 1) view = View.DETAIL;
    }

    public void applySnapshot(List<SpatialLinkSnapshotPayload.GateView> next) {
        int previousCount = gates.size();
        boolean hadDefault = hasDefault();
        boolean hadSelection = selected() != null;
        View previousView = view;
        boolean editingRename = renameBox != null && renameBox.isFocused();
        gates = List.copyOf(next);
        ensureSelection();
        if (gates.isEmpty() && (view == View.DETAIL || view == View.MANAGE)) view = View.LIST;
        else if (previousCount != 1 && gates.size() == 1) view = View.DETAIL;
        if (!editingRename) syncSelectedName();
        boolean hasSelection = selected() != null;
        if (layout != null && (panelClass(previousCount) != panelClass(gates.size())
                || previousView != view
                || (hadSelection != hasSelection && !layout.compact)
                || (hadDefault != hasDefault() && layout.compact && view == View.DETAIL))) {
            rebuildWidgets();
        }
        updateControls();
    }

    @Override protected void init() {
        super.init();
        searchBox = registerBox = renameBox = shareBox = null;
        registerButton = detailButton = moveButton = defaultMoveButton = manageButton = null;
        favoriteButton = defaultButton = renameButton = shareButton = unshareButton = deleteButton = null;
        layout = layoutFor(width, height);
        if (layout.compact) initCompact(); else initWide();
        updateControls();
    }

    private void initWide() {
        Layout l = layout;
        int labelsY = l.y + 28, fieldsY = l.y + 40;
        int searchW = Math.min(150, l.w * 23 / 100);
        int registerW = Math.min(150, l.w * 23 / 100);
        int searchX = l.x + 12;
        int registerX = searchX + searchW + 28;
        searchBox = makeBox(searchX, fieldsY, searchW, 18, "이름·차원 검색", 48, query, value -> { query = value; scroll = 0; ensureSelection(); updateControls(); });
        registerBox = makeBox(registerX, fieldsY, registerW, 18, "새 공간문 이름", 32, registerName, value -> registerName = value);
        registerButton = addRenderableWidget(Button.builder(Component.literal("위치 등록"), b -> registerCurrent())
                .bounds(registerX + registerW + 5, fieldsY, 68, 18).build());
        defaultMoveButton = addRenderableWidget(Button.builder(Component.literal("기본 위치로 이동"), b -> send(SpatialLinkActionPayload.Action.TELEPORT_DEFAULT, "", ""))
                .bounds(l.x + l.w - 116, fieldsY, 104, 18).build());
        SpatialLinkSnapshotPayload.GateView selected = selected();
        if (selected == null) return;
        int dx = l.detailX, contentY = l.contentY;
        int moveW = Math.min(180, l.detailW - 24);
        moveButton = addRenderableWidget(Button.builder(Component.literal("이동"), b -> sendSelected(SpatialLinkActionPayload.Action.TELEPORT))
                .bounds(dx + 12, contentY + 155, moveW, 22).build());
        int half = Math.min(104, (l.detailW - 28) / 2);
        favoriteButton = actionButton(selected.favorite() ? "즐겨찾기 해제" : "즐겨찾기", dx + 12, contentY + 184, half, SpatialLinkActionPayload.Action.FAVORITE);
        defaultButton = actionButton(selected.defaultGate() ? "기본 해제" : "기본 지정", dx + 20 + half, contentY + 184, half, SpatialLinkActionPayload.Action.DEFAULT);
        renameBox = makeBox(dx + 12, contentY + 231, Math.max(70, l.detailW - 112), 18, "새 이름", 32, renameName, value -> renameName = value);
        renameButton = actionButton("이름 변경", dx + l.detailW - 92, contentY + 231, 80, SpatialLinkActionPayload.Action.RENAME);
        shareBox = makeBox(dx + 12, contentY + 274, Math.max(70, l.detailW - 178), 18, "플레이어 이름", 32, sharePlayer, value -> sharePlayer = value);
        shareButton = actionButton("공유", dx + l.detailW - 158, contentY + 274, 68, SpatialLinkActionPayload.Action.SHARE);
        unshareButton = actionButton("회수", dx + l.detailW - 82, contentY + 274, 70, SpatialLinkActionPayload.Action.UNSHARE);
        deleteButton = addRenderableWidget(Button.builder(Component.literal(deleteArmed ? "삭제 확인" : "삭제"), b -> deleteOrConfirm(selected()))
                .bounds(dx + 12, contentY + 302, 76, 18).build());
    }

    private void initCompact() {
        Layout l = layout;
        switch (view) {
            case LIST -> {
                int y = l.y + 30;
                int searchW = Math.max(48, l.w - 16 - 78);
                searchBox = makeBox(l.x + 8, y, searchW, 17, "이름·차원 검색", 48, query, value -> { query = value; scroll = 0; ensureSelection(); updateControls(); });
                registerButton = addRenderableWidget(Button.builder(Component.literal("등록"), b -> switchView(View.REGISTER))
                        .bounds(l.x + l.w - 61, y, 53, 17).build());
                detailButton = addRenderableWidget(Button.builder(Component.literal("상세 ›"), b -> { if (selected() != null) switchView(View.DETAIL); })
                        .bounds(l.x + l.w - 61, l.y + l.h - 20, 53, 16).build());
            }
            case DETAIL -> {
                addRenderableWidget(Button.builder(Component.literal("‹ 목록"), b -> switchView(View.LIST))
                        .bounds(l.x + 8, l.y + 28, 54, 16).build());
                SpatialLinkSnapshotPayload.GateView selected = selected();
                if (selected == null) return;
                int buttonY = l.y + l.h - 22;
                moveButton = addRenderableWidget(Button.builder(Component.literal("이동"), b -> sendSelected(SpatialLinkActionPayload.Action.TELEPORT))
                        .bounds(l.x + 8, buttonY, 55, 18).build());
                defaultMoveButton = addRenderableWidget(Button.builder(Component.literal("기본 이동"), b -> send(SpatialLinkActionPayload.Action.TELEPORT_DEFAULT, "", ""))
                        .bounds(l.x + 67, buttonY, 68, 18).build());
                manageButton = addRenderableWidget(Button.builder(Component.literal("관리 ›"), b -> { managePage = ManagePage.FAVORITES; switchView(View.MANAGE); })
                        .bounds(l.x + l.w - 63, buttonY, 55, 18).build());
            }
            case REGISTER -> {
                addRenderableWidget(Button.builder(Component.literal("‹ 목록"), b -> switchView(View.LIST)).bounds(l.x + 8, l.y + 28, 54, 16).build());
                registerBox = makeBox(l.x + 8, l.y + 57, l.w - 16, 18, "새 공간문 이름", 32, registerName, value -> registerName = value);
                registerButton = addRenderableWidget(Button.builder(Component.literal("현재 위치 등록"), b -> registerCurrent())
                        .bounds(l.x + 8, l.y + l.h - 23, l.w - 16, 18).build());
            }
            case MANAGE -> initCompactManage(l);
        }
    }

    private void initCompactManage(Layout l) {
        addRenderableWidget(Button.builder(Component.literal("‹ 위치"), b -> switchView(View.DETAIL)).bounds(l.x + 8, l.y + 28, 54, 16).build());
        SpatialLinkSnapshotPayload.GateView selected = selected();
        if (selected == null || !selected.owner()) return;
        int y = l.y + 58;
        if (managePage != ManagePage.DELETE) {
            addRenderableWidget(Button.builder(Component.literal("다음 ›"), b -> {
                managePage = ManagePage.values()[managePage.ordinal() + 1]; rebuildWidgets();
            }).bounds(l.x + l.w - 62, l.y + 28, 54, 16).build());
        }
        switch (managePage) {
            case FAVORITES -> {
                favoriteButton = actionButton(selected.favorite() ? "즐겨찾기 해제" : "즐겨찾기", l.x + 8, y, l.w - 16, SpatialLinkActionPayload.Action.FAVORITE);
                defaultButton = actionButton(selected.defaultGate() ? "기본 지정 해제" : "기본 지정", l.x + 8, y + 20, l.w - 16, SpatialLinkActionPayload.Action.DEFAULT);
            }
            case RENAME -> {
                renameBox = makeBox(l.x + 8, y, l.w - 74, 18, "새 이름", 32, renameName, value -> renameName = value);
                renameButton = actionButton("저장", l.x + l.w - 60, y, 52, SpatialLinkActionPayload.Action.RENAME);
            }
            case SHARE -> {
                shareBox = makeBox(l.x + 8, y, l.w - 16, 18, "플레이어 이름", 32, sharePlayer, value -> sharePlayer = value);
                shareButton = actionButton("공유", l.x + 8, y + 21, (l.w - 22) / 2, SpatialLinkActionPayload.Action.SHARE);
                unshareButton = actionButton("공유 회수", l.x + 14 + (l.w - 22) / 2, y + 21, (l.w - 22) / 2, SpatialLinkActionPayload.Action.UNSHARE);
            }
            case DELETE -> {
                deleteButton = addRenderableWidget(Button.builder(Component.literal(deleteArmed ? "삭제를 확인" : "공간문 삭제"), b -> deleteOrConfirm(selected))
                        .bounds(l.x + 8, y, l.w - 16, 20).build());
                addRenderableWidget(Button.builder(Component.literal("취소"), b -> { deleteArmed = false; managePage = ManagePage.FAVORITES; rebuildWidgets(); })
                        .bounds(l.x + 8, y + 26, l.w - 16, 18).build());
            }
        }
    }

    private EditBox makeBox(int x, int y, int w, int h, String hint, int max, String initial,
                            java.util.function.Consumer<String> responder) {
        EditBox box = new EditBox(font, x, y, Math.max(1, w), h, Component.literal(hint));
        box.setMaxLength(max);
        box.setValue(initial);
        box.setResponder(responder);
        return addRenderableWidget(box);
    }

    private Button actionButton(String label, int x, int y, int w, SpatialLinkActionPayload.Action action) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> doAction(action))
                .bounds(x, y, Math.max(1, w), 18).build());
    }

    private void doAction(SpatialLinkActionPayload.Action action) {
        SpatialLinkSnapshotPayload.GateView gate = selected();
        if (gate == null) return;
        String value = switch (action) {
            case RENAME -> renameBox == null ? renameName : renameBox.getValue();
            case SHARE, UNSHARE -> shareBox == null ? sharePlayer : shareBox.getValue();
            default -> "";
        };
        send(action, gate.id(), value);
    }

    private void registerCurrent() {
        String name = registerBox == null ? registerName : registerBox.getValue();
        send(SpatialLinkActionPayload.Action.REGISTER, "", name);
        registerName = name;
    }

    private void deleteOrConfirm(SpatialLinkSnapshotPayload.GateView gate) {
        long now = System.currentTimeMillis();
        if (deleteArmed && now - deleteArmedAt < 5000L) {
            send(SpatialLinkActionPayload.Action.DELETE, gate.id(), "");
            deleteArmed = false;
        } else {
            deleteArmed = true;
            deleteArmedAt = now;
            rebuildWidgets();
        }
    }

    private void switchView(View next) {
        view = next;
        deleteArmed = false;
        rebuildWidgets();
    }

    @Override public void tick() { super.tick(); updateControls(); }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(g);
        Layout l = layout == null ? layoutFor(width, height) : layout;
        g.fill(0, 0, width, height, SHADE);
        g.fill(l.x, l.y, l.x + l.w, l.y + l.h, STONE);
        g.outline(l.x, l.y, l.w, l.h, COPPER);
        g.fill(l.x + 2, l.y + 2, l.x + l.w - 2, l.y + 25, SURFACE);
        if (!l.compact) {
            g.fill(l.x + 7, l.contentY - 4, l.x + l.listW + 5, l.y + l.h - 20, SURFACE);
            if (selected() != null) g.fill(l.detailX, l.contentY - 4, l.x + l.w - 8, l.y + l.h - 20, 0xFF293334);
        }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(g, mouseX, mouseY, partialTicks);
        Layout l = layout == null ? layoutFor(width, height) : layout;
        g.text(font, title, l.x + 12, l.y + 8, TEXT, false);
        String count = gates.size() + " 공간문";
        g.text(font, count, l.x + l.w - font.width(count) - 12, l.y + 8, COPPER, false);
        if (l.compact) drawCompact(g, l, mouseX, mouseY); else drawWide(g, l, mouseX, mouseY);
    }

    private void drawWide(GuiGraphicsExtractor g, Layout l, int mx, int my) {
        g.text(font, "공간문 찾기", l.x + 12, l.y + 28, MUTED, false);
        int registerX = l.x + 12 + Math.min(150, l.w * 23 / 100) + 28;
        g.text(font, "현재 위치 등록", registerX, l.y + 28, MUTED, false);
        List<SpatialLinkSnapshotPayload.GateView> filtered = filtered();
        int rows = l.rows, rowY = l.contentY;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, filtered.size() - rows)));
        for (int row = 0; row < rows; row++) {
            int index = scroll + row;
            if (index >= filtered.size()) break;
            var gate = filtered.get(index); int y = rowY + row * l.rowH;
            boolean active = gate.id().equals(selectedId);
            boolean hover = inside(mx, my, l.x + 12, y, l.listW - 12, l.rowH - 2);
            drawGateRow(g, gate, l.x + 12, y, l.listW - 12, l.rowH - 2, active, hover);
        }
        if (filtered.isEmpty()) {
            String text = gates.isEmpty() ? "현재 위치를 등록해 이동 경로를 만드세요." : "검색 결과가 없습니다.";
            g.text(font, fit(text, l.detailW - 24), l.detailX + 12, l.contentY + 24, MUTED, false);
        }
        drawPageHint(g, l.x + 12, l.y + l.h - 34, l.listW - 12, filtered.size(), rows);
        SpatialLinkSnapshotPayload.GateView gate = selected();
        if (gate == null) return;
        drawSelected(g, gate, l.detailX + 12, l.contentY + 12, l.detailW - 24, mx, my, false);
        g.text(font, gate.owner() ? "내 공간문" : "공유 공간문", l.detailX + 12, l.contentY + 7,
                gate.owner() ? COPPER : SHARED, false);
        if (gate.owner()) {
            g.text(font, "이름 변경", l.detailX + 12, l.contentY + 217, MUTED, false);
            g.text(font, "플레이어 공유", l.detailX + 12, l.contentY + 260, MUTED, false);
            if (deleteArmed && System.currentTimeMillis() - deleteArmedAt >= 5000L) deleteArmed = false;
        }
    }

    private void drawSelected(GuiGraphicsExtractor g, SpatialLinkSnapshotPayload.GateView gate,
                              int x, int y, int w, int mx, int my, boolean compact) {
        if (compact) {
            String destination = dimensionName(gate.dimension()) + " · " + floor(gate.x()) + ", " + floor(gate.y()) + ", " + floor(gate.z());
            g.text(font, fit(gate.name(), w), x, y, TEXT, false);
            g.text(font, fit(destination, w), x, y + 14, MUTED, false);
            return;
        }
        int nameY = y + 20;
        g.text(font, fit(gate.name(), w), x, nameY, TEXT, false);
        g.text(font, "차원", x, nameY + 19, MUTED, false);
        String dimension = dimensionName(gate.dimension());
        g.text(font, fit(dimension, w), x, nameY + 31, TEXT, false);
        g.text(font, "좌표", x, nameY + 49, MUTED, false);
        String coords = floor(gate.x()) + ", " + floor(gate.y()) + ", " + floor(gate.z());
        g.text(font, fit(coords, w), x, nameY + 61, TEXT, false);
        g.text(font, (gate.favorite() ? "★ 즐겨찾기" : "즐겨찾기 아님") + "  ·  "
                + (gate.defaultGate() ? "기본 위치" : "기본 위치 아님"), x, nameY + 80, COPPER, false);
        if (gate.owner()) g.text(font, fit("공유 대상의 이름을 입력해 공유하거나 회수할 수 있습니다.", w), x, nameY + 105, MUTED, false);
    }

    private void drawCompact(GuiGraphicsExtractor g, Layout l, int mx, int my) {
        if (view == View.LIST) {
            g.text(font, "검색 · 이름 또는 차원", l.x + 8, l.y + 27, MUTED, false);
            List<SpatialLinkSnapshotPayload.GateView> filtered = filtered();
            int rows = Math.max(1, l.listRows);
            scroll = Math.max(0, Math.min(scroll, Math.max(0, filtered.size() - rows)));
            int rowY = l.y + 51;
            for (int row = 0; row < rows; row++) {
                int index = scroll + row; if (index >= filtered.size()) break;
                var gate = filtered.get(index); int y = rowY + row * l.rowH;
                drawGateRow(g, gate, l.x + 8, y, l.w - 16, l.rowH - 1, gate.id().equals(selectedId),
                        inside(mx, my, l.x + 8, y, l.w - 16, l.rowH - 1));
            }
            if (filtered.isEmpty()) {
                String empty = gates.isEmpty() ? "위치를 등록해 시작하세요." : "검색 결과가 없습니다.";
                g.text(font, fit(empty, l.w - 16), l.x + 8, l.y + 53, MUTED, false);
            }
        } else if (view == View.REGISTER) {
            g.text(font, "현재 서 있는 위치를 등록합니다.", l.x + 8, l.y + 48, MUTED, false);
            g.text(font, "이름", l.x + 8, l.y + 78, COPPER, false);
        } else if (view == View.DETAIL) {
            var gate = selected();
            if (gate == null) return;
            String destination = dimensionName(gate.dimension()) + " · " + floor(gate.x()) + ", " + floor(gate.y()) + ", " + floor(gate.z());
            g.text(font, fit(gate.name(), l.w - 18), l.x + 9, l.y + 51, TEXT, false);
            g.text(font, fit(destination, l.w - 18), l.x + 9, l.y + 64, MUTED, false);
            g.text(font, gate.owner() ? "내 공간문" : "공유 공간문", l.x + 75, l.y + 29, gate.owner() ? COPPER : SHARED, false);
        } else if (view == View.MANAGE) {
            String title = switch (managePage) {
                case FAVORITES -> "즐겨찾기와 기본 위치";
                case RENAME -> "이름 변경";
                case SHARE -> "플레이어 공유";
                case DELETE -> "공간문 삭제";
            };
            g.text(font, title, l.x + 8, l.y + 49, COPPER, false);
            if (managePage == ManagePage.DELETE) g.text(font, "삭제하면 되돌릴 수 없습니다.", l.x + 8, l.y + 78, MUTED, false);
        }
    }

    private void drawGateRow(GuiGraphicsExtractor g, SpatialLinkSnapshotPayload.GateView gate,
                             int x, int y, int w, int h, boolean selected, boolean hover) {
        g.fill(x, y, x + w, y + h, selected ? SELECTED : hover ? ALT : SURFACE);
        g.fill(x, y, x + 3, y + h, gate.owner() ? COPPER : SHARED);
        String prefix = (gate.defaultGate() ? "◆ " : "") + (gate.favorite() ? "★ " : "");
        String dim = dimensionName(gate.dimension());
        String shownDim = fit(dim, Math.max(1, w - 12));
        int dimW = font.width(shownDim), left = x + 9;
        g.text(font, fit(prefix + gate.name(), w - dimW - 25), left, y + Math.max(2, (h - 8) / 2), gate.owner() ? TEXT : SHARED, false);
        g.text(font, shownDim, x + w - dimW - 7, y + Math.max(2, (h - 8) / 2), MUTED, false);
    }

    private void drawPageHint(GuiGraphicsExtractor g, int x, int y, int w, int total, int rows) {
        if (total <= rows) return;
        String hint = "마우스 휠로 탐색 · " + (scroll + 1) + "–" + Math.min(total, scroll + rows) + " / " + total;
        g.text(font, fit(hint, w), x, y, MUTED, false);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x(), my = event.y(); Layout l = layout == null ? layoutFor(width, height) : layout;
            if (l.compact && view == View.LIST) {
                List<SpatialLinkSnapshotPayload.GateView> filtered = filtered();
                int rowY = l.y + 51, row = (int) ((my - rowY) / l.rowH), index = scroll + row;
                if (inside(mx, my, l.x + 8, rowY, l.w - 16, l.listRows * l.rowH)) {
                if (index >= 0 && index < filtered.size()) { selectedId = filtered.get(index).id(); syncSelectedName(true); updateControls(); }
                    return true;
                }
            }
            if (!l.compact) {
                List<SpatialLinkSnapshotPayload.GateView> filtered = filtered();
                for (int row = 0; row < l.rows; row++) {
                    int index = scroll + row, y = l.contentY + row * l.rowH;
                    if (index < filtered.size() && inside(mx, my, l.x + 12, y, l.listW - 12, l.rowH - 2)) {
                        selectedId = filtered.get(index).id(); syncSelectedName(true); updateControls(); return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Layout l = layout == null ? layoutFor(width, height) : layout;
        List<SpatialLinkSnapshotPayload.GateView> filtered = filtered();
        int visibleRows = l.compact ? l.listRows : l.rows;
        int max = Math.max(0, filtered.size() - visibleRows);
        scroll = Math.max(0, Math.min(max, scroll + (vertical < 0 ? 1 : -1)));
        return true;
    }

    private void updateControls() {
        SpatialLinkSnapshotPayload.GateView selected = selected();
        boolean has = selected != null, owner = has && selected.owner();
        if (detailButton != null) detailButton.active = has;
        if (moveButton != null) moveButton.active = has;
        if (defaultMoveButton != null) {
            defaultMoveButton.visible = hasDefault();
            defaultMoveButton.active = hasDefault();
        }
        if (favoriteButton != null) {
            favoriteButton.visible = has;
            favoriteButton.active = has;
            favoriteButton.setMessage(Component.literal(has && selected.favorite() ? "즐겨찾기 해제" : "즐겨찾기"));
        }
        if (defaultButton != null) {
            defaultButton.visible = has;
            defaultButton.active = has;
            defaultButton.setMessage(Component.literal(has && selected.defaultGate() ? "기본 해제" : "기본 지정"));
        }
        if (manageButton != null) {
            manageButton.visible = owner;
            manageButton.active = owner;
        }
        if (renameBox != null) renameBox.visible = owner;
        if (renameButton != null) renameButton.visible = owner;
        if (shareBox != null) shareBox.visible = owner;
        if (shareButton != null) shareButton.visible = owner;
        if (unshareButton != null) unshareButton.visible = owner;
        if (deleteButton != null) {
            deleteButton.visible = owner;
            String label = layout != null && layout.compact
                    ? (deleteArmed ? "삭제를 확인" : "공간문 삭제")
                    : (deleteArmed ? "삭제 확인" : "삭제");
            deleteButton.setMessage(Component.literal(label));
        }
        if (renameButton != null) renameButton.active = owner && !current(renameBox, renameName).isBlank();
        if (shareButton != null) shareButton.active = owner && !current(shareBox, sharePlayer).isBlank();
        if (unshareButton != null) unshareButton.active = owner && !current(shareBox, sharePlayer).isBlank();
        if (deleteButton != null) deleteButton.active = owner;
    }

    private String current(EditBox box, String value) { return box == null ? value : box.getValue(); }
    private void syncSelectedName() { syncSelectedName(false); }
    private void syncSelectedName(boolean force) {
        SpatialLinkSnapshotPayload.GateView gate = selected();
        boolean owner = gate != null && gate.owner();
        if (force || renameBox == null || !renameBox.isFocused()) {
            renameName = owner ? gate.name() : "";
            if (renameBox != null) renameBox.setValue(renameName);
        }
        if (favoriteButton != null) favoriteButton.setMessage(Component.literal(gate != null && gate.favorite() ? "즐겨찾기 해제" : "즐겨찾기"));
        if (defaultButton != null) defaultButton.setMessage(Component.literal(gate != null && gate.defaultGate() ? "기본 해제" : "기본 지정"));
    }
    private void sendSelected(SpatialLinkActionPayload.Action action) {
        SpatialLinkSnapshotPayload.GateView gate = selected();
        if (gate != null) send(action, gate.id(), "");
    }
    private boolean hasDefault() { return gates.stream().anyMatch(SpatialLinkSnapshotPayload.GateView::defaultGate); }
    private SpatialLinkSnapshotPayload.GateView selected() { return gates.stream().filter(g -> g.id().equals(selectedId)).findFirst().orElse(null); }
    private List<SpatialLinkSnapshotPayload.GateView> filtered() {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) return gates;
        return gates.stream().filter(g -> g.name().toLowerCase(Locale.ROOT).contains(needle)
                || dimensionName(g.dimension()).toLowerCase(Locale.ROOT).contains(needle)
                || g.dimension().toLowerCase(Locale.ROOT).contains(needle)).toList();
    }
    private void ensureSelection() {
        List<SpatialLinkSnapshotPayload.GateView> list = filtered();
        if (list.isEmpty()) { selectedId = ""; scroll = 0; return; }
        if (list.stream().noneMatch(g -> g.id().equals(selectedId))) selectedId = list.getFirst().id();
    }
    private void send(SpatialLinkActionPayload.Action action, String id, String value) {
        ClientPlayNetworking.send(new SpatialLinkActionPayload(action, id, value == null ? "" : value));
    }
    private String fit(String value, int maxWidth) {
        if (maxWidth <= 0) return ""; String text = value;
        while (!text.isEmpty() && font.width(text + "…") > maxWidth) text = text.substring(0, text.length() - 1);
        return text.length() == value.length() ? value : text + "…";
    }
    private static String floor(double n) { return Long.toString((long) Math.floor(n)); }
    private static String dimensionName(String id) {
        return switch (id) {
            case "minecraft:overworld" -> "오버월드";
            case "minecraft:the_nether" -> "네더";
            case "minecraft:the_end" -> "엔드";
            default -> { int i = id.indexOf(':'); yield (i >= 0 ? id.substring(i + 1) : id).replace('_', ' '); }
        };
    }
    private static boolean inside(double x, double y, int left, int top, int w, int h) { return x >= left && x < left + w && y >= top && y < top + h; }
    private Layout layoutFor(int screenW, int screenH) {
        int wantedHeight = gates.isEmpty() ? 190 : gates.size() == 1 ? 250 : 500;
        int w = Math.max(1, Math.min(760, screenW - 12)), h = Math.max(1, Math.min(wantedHeight, screenH - 12));
        int x = (screenW - w) / 2, y = (screenH - h) / 2;
        boolean compact = w < 520 || h < 390;
        int contentY = y + 66;
        int rowH = compact ? 19 : 27;
        int rows = Math.max(1, Math.min(ROWS_WIDE, (h - 102) / rowH));
        int listW = Math.max(1, (w - 34) * 42 / 100), detailX = x + listW + 22, detailW = w - (detailX - x) - 12;
        int listRows = Math.max(1, Math.min(8, (h - 78) / rowH));
        return new Layout(x, y, w, h, compact, contentY, rowH, rows, listW, detailX, detailW, listRows);
    }
    private static int panelClass(int count) { return count == 0 ? 0 : count == 1 ? 1 : 2; }
    @Override public boolean isPauseScreen() { return false; }
    private record Layout(int x, int y, int w, int h, boolean compact, int contentY, int rowH,
                          int rows, int listW, int detailX, int detailW, int listRows) { }
}

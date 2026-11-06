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

/** Search-first control surface for permanent personal/shared spatial gates. */
public final class SpatialLinkScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 252;
    private static final int ROWS_PER_PAGE = 6;
    private static final int BACKDROP = 0xFF111820;
    private static final int SURFACE = 0xFF18242C;
    private static final int SURFACE_ALT = 0xFF1F3039;
    private static final int ROW_SELECTED = 0xFF24434B;
    private static final int ACCENT = 0xFF63D7D1;
    private static final int TEXT = 0xFFEAF7F6;
    private static final int MUTED = 0xFF91AAA9;
    private static final int SHARED = 0xFF9FB7E9;

    private List<SpatialLinkSnapshotPayload.GateView> gates;
    private String query = "";
    private String selectedId = "";
    private int page;
    private int leftPos;
    private int topPos;
    private EditBox searchBox;
    private EditBox registerNameBox;
    private EditBox nameBox;
    private EditBox shareBox;
    private Button teleportButton;
    private Button favoriteButton;
    private Button defaultButton;
    private Button renameButton;
    private Button deleteButton;
    private Button shareButton;
    private Button unshareButton;
    private Button previousButton;
    private Button nextButton;

    public SpatialLinkScreen(List<SpatialLinkSnapshotPayload.GateView> gates) {
        super(Component.translatable("screen.augmentation_survival.spatial_link"));
        this.gates = List.copyOf(gates);
        ensureSelection();
    }

    public void applySnapshot(List<SpatialLinkSnapshotPayload.GateView> next) {
        this.gates = List.copyOf(next);
        ensureSelection();
        syncSelectedNameField();
        updateControls();
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - PANEL_WIDTH) / 2;
        topPos = (height - PANEL_HEIGHT) / 2;

        searchBox = new EditBox(font, leftPos + 16, topPos + 38, 132, 20, Component.literal("검색"));
        searchBox.setMaxLength(40);
        searchBox.setResponder(value -> {
            query = value;
            page = 0;
            ensureSelection();
            syncSelectedNameField();
            updateControls();
        });
        addRenderableWidget(searchBox);

        registerNameBox = new EditBox(font, leftPos + 154, topPos + 38, 94, 20, Component.literal("새 공간문 이름"));
        registerNameBox.setMaxLength(32);
        addRenderableWidget(registerNameBox);

        addRenderableWidget(Button.builder(Component.literal("위치 등록"), button -> {
            String name = registerNameBox.getValue();
            send(SpatialLinkActionPayload.Action.REGISTER, "", name);
            registerNameBox.setValue("");
        }).bounds(leftPos + 252, topPos + 38, 72, 20).build());

        addRenderableWidget(Button.builder(Component.literal("기본 이동"), button ->
                send(SpatialLinkActionPayload.Action.TELEPORT_DEFAULT, "", ""))
                .bounds(leftPos + 328, topPos + 38, 76, 20).build());

        nameBox = new EditBox(font, leftPos + 280, topPos + 96, 124, 20, Component.literal("선택한 공간문 이름"));
        nameBox.setMaxLength(32);
        addRenderableWidget(nameBox);

        shareBox = new EditBox(font, leftPos + 280, topPos + 184, 124, 20, Component.literal("플레이어 이름"));
        shareBox.setMaxLength(32);
        addRenderableWidget(shareBox);

        teleportButton = addRenderableWidget(button("이동", 280, 122, 58, SpatialLinkActionPayload.Action.TELEPORT));
        favoriteButton = addRenderableWidget(button("즐겨찾기", 344, 122, 60, SpatialLinkActionPayload.Action.FAVORITE));
        defaultButton = addRenderableWidget(button("기본 지정", 280, 146, 58, SpatialLinkActionPayload.Action.DEFAULT));
        renameButton = addRenderableWidget(button("이름 변경", 344, 146, 60, SpatialLinkActionPayload.Action.RENAME));
        deleteButton = addRenderableWidget(button("삭제", 280, 208, 58, SpatialLinkActionPayload.Action.DELETE));
        shareButton = addRenderableWidget(button("공유", 344, 208, 28, SpatialLinkActionPayload.Action.SHARE));
        unshareButton = addRenderableWidget(button("회수", 376, 208, 28, SpatialLinkActionPayload.Action.UNSHARE));

        previousButton = addRenderableWidget(Button.builder(Component.literal("‹"), button -> {
            if (page > 0) page--;
            ensureSelection();
            syncSelectedNameField();
            updateControls();
        }).bounds(leftPos + 16, topPos + 220, 24, 20).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal("›"), button -> {
            if (page + 1 < pageCount()) page++;
            ensureSelection();
            syncSelectedNameField();
            updateControls();
        }).bounds(leftPos + 244, topPos + 220, 24, 20).build());

        ensureSelection();
        syncSelectedNameField();
        updateControls();
    }

    private Button button(String label, int x, int y, int width, SpatialLinkActionPayload.Action action) {
        return Button.builder(Component.literal(label), button -> {
            SpatialLinkSnapshotPayload.GateView selected = selected();
            if (selected == null) return;
            String value = switch (action) {
                case RENAME -> nameBox.getValue();
                case SHARE, UNSHARE -> shareBox.getValue();
                default -> "";
            };
            send(action, selected.id(), value);
        }).bounds(leftPos + x, topPos + y, width, 20).build();
    }

    @Override
    public void tick() {
        super.tick();
        updateControls();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
        graphics.fill(leftPos, topPos, leftPos + PANEL_WIDTH, topPos + PANEL_HEIGHT, BACKDROP);
        graphics.outline(leftPos, topPos, PANEL_WIDTH, PANEL_HEIGHT, 0xFF4B6A70);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + PANEL_WIDTH - 4, topPos + 28, SURFACE_ALT);
        graphics.fill(leftPos + 4, topPos + 28, leftPos + 272, topPos + PANEL_HEIGHT - 4, SURFACE);
        graphics.fill(leftPos + 274, topPos + 28, leftPos + PANEL_WIDTH - 4, topPos + PANEL_HEIGHT - 4, 0xFF152128);
        graphics.fill(leftPos + 4, topPos + 28, leftPos + 8, topPos + PANEL_HEIGHT - 4, ACCENT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        graphics.text(font, title, leftPos + 16, topPos + 11, TEXT, false);
        graphics.text(font, "영구 공간 네트워크", leftPos + 280, topPos + 12, MUTED, false);

        List<SpatialLinkSnapshotPayload.GateView> filtered = filtered();
        int start = page * ROWS_PER_PAGE;
        for (int row = 0; row < ROWS_PER_PAGE; row++) {
            int index = start + row;
            int y = topPos + 68 + row * 24;
            if (index >= filtered.size()) {
                graphics.fill(leftPos + 16, y, leftPos + 268, y + 20, 0xFF152027);
                continue;
            }
            SpatialLinkSnapshotPayload.GateView gate = filtered.get(index);
            boolean selected = gate.id().equals(selectedId);
            graphics.fill(leftPos + 16, y, leftPos + 268, y + 20, selected ? ROW_SELECTED : SURFACE_ALT);
            graphics.outline(leftPos + 16, y, 252, 20, selected ? ACCENT : 0xFF35464E);
            String prefix = (gate.defaultGate() ? "◆ " : "") + (gate.favorite() ? "★ " : "");
            graphics.text(font, prefix + gate.name(), leftPos + 23, y + 6, gate.owner() ? TEXT : SHARED, false);
            String dim = shortDimension(gate.dimension());
            graphics.text(font, dim, leftPos + 260 - font.width(dim), y + 6, MUTED, false);
        }

        int pages = pageCount();
        String pageText = pages == 0 ? "0 / 0" : (page + 1) + " / " + pages;
        graphics.text(font, pageText, leftPos + 142 - font.width(pageText) / 2, topPos + 226, MUTED, false);

        SpatialLinkSnapshotPayload.GateView selected = selected();
        if (selected == null) {
            graphics.text(font, "등록된 공간문이 없습니다.", leftPos + 280, topPos + 72, MUTED, false);
            return;
        }
        graphics.text(font, selected.owner() ? "내 공간문" : "공유 공간문", leftPos + 280, topPos + 72,
                selected.owner() ? ACCENT : SHARED, false);
        String coords = (int) Math.floor(selected.x()) + ", " + (int) Math.floor(selected.y()) + ", " + (int) Math.floor(selected.z());
        graphics.text(font, coords, leftPos + 280, topPos + 84, MUTED, false);
        if (!selected.owner()) graphics.text(font, selected.name(), leftPos + 280, topPos + 101, TEXT, false);
        graphics.text(font, "공유 대상", leftPos + 280, topPos + 173, MUTED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();
            List<SpatialLinkSnapshotPayload.GateView> filtered = filtered();
            int start = page * ROWS_PER_PAGE;
            for (int row = 0; row < ROWS_PER_PAGE; row++) {
                int index = start + row;
                if (index >= filtered.size()) break;
                int y = topPos + 68 + row * 24;
                if (inside(mx, my, leftPos + 16, y, 252, 20)) {
                    selectedId = filtered.get(index).id();
                    syncSelectedNameField();
                    updateControls();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void send(SpatialLinkActionPayload.Action action, String gateId, String value) {
        ClientPlayNetworking.send(new SpatialLinkActionPayload(action, gateId, value));
    }

    private void ensureSelection() {
        List<SpatialLinkSnapshotPayload.GateView> filtered = filtered();
        int pages = pageCount(filtered.size());
        if (pages == 0) {
            page = 0;
            selectedId = "";
            return;
        }
        page = Math.max(0, Math.min(page, pages - 1));
        boolean visible = filtered.stream().anyMatch(gate -> gate.id().equals(selectedId));
        if (!visible) selectedId = filtered.get(page * ROWS_PER_PAGE).id();
    }

    private void syncSelectedNameField() {
        if (nameBox == null) return;
        SpatialLinkSnapshotPayload.GateView selected = selected();
        nameBox.setValue(selected != null && selected.owner() ? selected.name() : "");
    }

    private void updateControls() {
        SpatialLinkSnapshotPayload.GateView selected = selected();
        boolean has = selected != null;
        boolean owner = has && selected.owner();
        if (teleportButton != null) teleportButton.active = has;
        if (favoriteButton != null) favoriteButton.active = has;
        if (defaultButton != null) defaultButton.active = has;
        if (renameButton != null) renameButton.active = owner && nameBox != null && !nameBox.getValue().isBlank();
        if (deleteButton != null) deleteButton.active = owner;
        if (shareButton != null) shareButton.active = owner && shareBox != null && !shareBox.getValue().isBlank();
        if (unshareButton != null) unshareButton.active = owner && shareBox != null && !shareBox.getValue().isBlank();
        if (nameBox != null) nameBox.active = owner;
        if (shareBox != null) shareBox.active = owner;
        if (previousButton != null) previousButton.active = page > 0;
        if (nextButton != null) nextButton.active = page + 1 < pageCount();
    }

    private SpatialLinkSnapshotPayload.GateView selected() {
        for (SpatialLinkSnapshotPayload.GateView gate : gates) {
            if (gate.id().equals(selectedId)) return gate;
        }
        return null;
    }

    private List<SpatialLinkSnapshotPayload.GateView> filtered() {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) return gates;
        return gates.stream().filter(gate ->
                gate.name().toLowerCase(Locale.ROOT).contains(needle)
                        || gate.dimension().toLowerCase(Locale.ROOT).contains(needle)
        ).toList();
    }

    private int pageCount() {
        return pageCount(filtered().size());
    }

    private static int pageCount(int count) {
        return count <= 0 ? 0 : (count + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
    }

    private static String shortDimension(String dimension) {
        int separator = dimension.indexOf(':');
        return separator >= 0 && separator + 1 < dimension.length() ? dimension.substring(separator + 1) : dimension;
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}

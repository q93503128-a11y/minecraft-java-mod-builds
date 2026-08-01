package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class VillageSkillTreeScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int OVERLAY = 0xC9080B10;
    private static final int PANEL = 0xFF0E151D;
    private static final int PANEL_SOFT = 0xFF17232E;
    private static final int PANEL_RAISED = 0xFF22313F;
    private static final int BORDER = 0xFF536A7D;
    private static final int ACCENT = 0xFF42D8BC;
    private static final int GOLD = 0xFFFFC85A;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFB2C0CC;
    private static final int DANGER = 0xFFE36F79;
    private static final int ATTACK = 0xFFE77777;
    private static final int DEFENCE = 0xFF83A9EE;
    private static final int SUPPORT = 0xFF55D9B7;

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final String[] actions;
    private final String[] labels;
    private int selectedIndex = -1;
    private Button purchaseButton;

    public VillageSkillTreeScreen(VillageNetwork.OpenVillageUiPayload payload) {
        super(Component.literal(payload.title()));
        this.payload = payload;
        this.actions = payload.actions().isBlank() ? new String[0] : payload.actions().split(SEP, -1);
        this.labels = payload.labels().isBlank() ? new String[0] : payload.labels().split(SEP, -1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        clearWidgets();
        Layout layout = layout();
        Graph graph = graph(layout);
        int count = Math.min(9, Math.min(actions.length, labels.length));
        for (int index = 0; index < count; index++) {
            int branch = index / 3;
            int tier = index % 3;
            int x = graph.nodeX(tier);
            int y = graph.nodeY(branch);
            String[] parts = nodeParts(index);
            String title = compact(parts[0], Math.max(7, graph.nodeWidth() / 7));
            String status = parts[2];
            String prefix = switch (status) {
                case "습득" -> "✓ ";
                case "습득 가능" -> "◆ ";
                default -> "· ";
            };
            final int selected = index;
            addRenderableWidget(Button.builder(
                            Component.literal(prefix + title),
                            button -> selectNode(selected))
                    .bounds(x, y, graph.nodeWidth(), graph.nodeHeight())
                    .build());
        }

        int buttonY = layout.top() + layout.height() - 31;
        int buttonWidth = Math.min(150, Math.max(100, layout.width() / 3));
        purchaseButton = Button.builder(
                        Component.literal("노드를 선택하세요"),
                        button -> confirmPurchase())
                .bounds(layout.left() + layout.width() - buttonWidth - 14,
                        buttonY, buttonWidth, 21)
                .build();
        purchaseButton.active = false;
        addRenderableWidget(purchaseButton);

        addRenderableWidget(Button.builder(
                        Component.literal("닫기"),
                        button -> onClose())
                .bounds(layout.left() + 14, buttonY, 70, 21)
                .build());
        refreshPurchaseButton();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        Graph graph = graph(layout);
        int left = layout.left();
        int top = layout.top();
        int right = left + layout.width();
        int bottom = top + layout.height();

        graphics.fill(left - 2, top - 2, right + 2, bottom + 2, BORDER);
        graphics.fill(left, top, right, bottom, PANEL);
        graphics.fill(left, top, left + 5, bottom, ACCENT);
        graphics.text(font, "전술 발전 트리", left + 17, top + 13, TEXT, false);
        graphics.text(font, compact(payload.body(), Math.max(38, layout.width() / 7)),
                left + 17, top + 29, MUTED, false);

        graphics.fill(graph.left(), graph.top(), graph.right(), graph.bottom(), PANEL_SOFT);
        String[] branchNames = {"공격", "방어", "지원"};
        int[] branchColors = {ATTACK, DEFENCE, SUPPORT};
        for (int branch = 0; branch < 3; branch++) {
            int centerY = graph.nodeY(branch) + graph.nodeHeight() / 2;
            graphics.text(font, branchNames[branch], graph.left() + 10,
                    centerY - 4, branchColors[branch], false);
            for (int tier = 0; tier < 2; tier++) {
                int x0 = graph.nodeX(tier) + graph.nodeWidth();
                int x1 = graph.nodeX(tier + 1);
                int lineColor = connectionColor(branch * 3 + tier, branchColors[branch]);
                graphics.fill(x0, centerY - 2, x1, centerY + 2, lineColor);
            }
        }

        for (int tier = 0; tier < 3; tier++) {
            graphics.centeredText(font, "단계 " + (tier + 1),
                    graph.nodeX(tier) + graph.nodeWidth() / 2,
                    graph.top() + 7, MUTED);
        }

        renderSelectedDetail(graphics, layout, graph);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void selectNode(int index) {
        selectedIndex = index;
        refreshPurchaseButton();
    }

    private void refreshPurchaseButton() {
        if (purchaseButton == null) {
            return;
        }
        if (selectedIndex < 0 || selectedIndex >= Math.min(actions.length, labels.length)) {
            purchaseButton.active = false;
            purchaseButton.setMessage(Component.literal("노드를 선택하세요"));
            return;
        }
        String status = nodeParts(selectedIndex)[2];
        purchaseButton.active = "습득 가능".equals(status);
        purchaseButton.setMessage(Component.literal(
                purchaseButton.active ? "습득 내용 확인" : status));
    }

    private void confirmPurchase() {
        if (selectedIndex < 0 || selectedIndex >= Math.min(actions.length, labels.length)) {
            return;
        }
        String[] parts = nodeParts(selectedIndex);
        if (!"습득 가능".equals(parts[2]) || minecraft == null) {
            return;
        }
        String detail = parts[0] + "\n" + parts[1]
                + "\n\n스킬 포인트 1개를 사용합니다. 이 노드를 습득하시겠습니까?";
        minecraft.gui.setScreen(new VillageConfirmScreen(
                this, actions[selectedIndex], parts[0], detail));
    }

    private void renderSelectedDetail(GuiGraphicsExtractor graphics, Layout layout, Graph graph) {
        int detailLeft = graph.left();
        int detailRight = graph.right();
        int detailTop = graph.bottom() + 7;
        int detailBottom = layout.top() + layout.height() - 39;
        graphics.fill(detailLeft, detailTop, detailRight, detailBottom, PANEL_RAISED);

        String title = "노드를 선택해 상세 효과를 확인하세요.";
        String description = "노드 버튼을 눌러도 즉시 습득되지 않습니다. 아래 확인 버튼을 한 번 더 눌러야 합니다.";
        String status = "";
        int color = MUTED;
        if (selectedIndex >= 0 && selectedIndex < Math.min(actions.length, labels.length)) {
            String[] parts = nodeParts(selectedIndex);
            title = parts[0];
            description = parts[1];
            status = parts[2];
            color = statusColor(status);
        }
        graphics.text(font, title, detailLeft + 12, detailTop + 8, TEXT, false);
        if (!status.isBlank()) {
            graphics.text(font, status, detailRight - 12 - font.width(status), detailTop + 8, color, false);
        }
        List<FormattedCharSequence> lines = font.split(
                Component.literal(description), Math.max(100, detailRight - detailLeft - 24));
        int y = detailTop + 23;
        for (FormattedCharSequence line : lines) {
            if (y > detailBottom - 10) {
                break;
            }
            graphics.text(font, line, detailLeft + 12, y, MUTED, false);
            y += 11;
        }
    }

    private int connectionColor(int prerequisiteIndex, int branchColor) {
        if (prerequisiteIndex < 0 || prerequisiteIndex >= Math.min(actions.length, labels.length)) {
            return BORDER;
        }
        return "습득".equals(nodeParts(prerequisiteIndex)[2]) ? branchColor : BORDER;
    }

    private String[] nodeParts(int index) {
        String[] raw = labels[index].split("\\|", 3);
        return new String[]{
                raw.length > 0 ? raw[0] : labels[index],
                raw.length > 1 ? raw[1] : "상세 설명 없음",
                raw.length > 2 ? raw[2] : "잠김"
        };
    }

    private int statusColor(String status) {
        return switch (status) {
            case "습득" -> ACCENT;
            case "습득 가능" -> GOLD;
            case "데이터 잠금" -> DANGER;
            default -> MUTED;
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(null);
        }
    }

    private String compact(String value, int maxCharacters) {
        if (value.length() <= maxCharacters) {
            return value;
        }
        return value.substring(0, Math.max(1, maxCharacters - 1)) + "…";
    }

    private Layout layout() {
        int panelWidth = Math.max(270, Math.min(680, width - 12));
        int panelHeight = Math.max(220, Math.min(450, height - 10));
        return new Layout((width - panelWidth) / 2, (height - panelHeight) / 2,
                panelWidth, panelHeight);
    }

    private Graph graph(Layout layout) {
        int left = layout.left() + 12;
        int right = layout.left() + layout.width() - 12;
        int top = layout.top() + 48;
        int bottom = layout.top() + layout.height() - 116;
        int labelWidth = 55;
        int nodeAreaLeft = left + labelWidth;
        int nodeAreaWidth = Math.max(180, right - nodeAreaLeft - 10);
        int gap = 8;
        int nodeWidth = Math.max(54, (nodeAreaWidth - gap * 2) / 3);
        int nodeHeight = Math.max(28, Math.min(40, (bottom - top - 38) / 3));
        int rowSpace = Math.max(nodeHeight + 5, (bottom - top - 23) / 3);
        return new Graph(left, top, right, bottom, nodeAreaLeft, nodeWidth, nodeHeight, gap, rowSpace);
    }

    private record Layout(int left, int top, int width, int height) {
    }

    private record Graph(
            int left,
            int top,
            int right,
            int bottom,
            int nodeAreaLeft,
            int nodeWidth,
            int nodeHeight,
            int gap,
            int rowSpace) {
        int nodeX(int tier) {
            return nodeAreaLeft + tier * (nodeWidth + gap);
        }

        int nodeY(int branch) {
            return top + 20 + branch * rowSpace;
        }
    }
}

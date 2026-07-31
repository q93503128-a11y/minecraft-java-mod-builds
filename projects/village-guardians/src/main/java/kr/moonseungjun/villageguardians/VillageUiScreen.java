package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class VillageUiScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int[] BUTTON_COLORS = {
            0xFF7E2A35, 0xFF176B68, 0xFF8C5A16,
            0xFF274C77, 0xFF4D6A34, 0xFF6B3E75
    };

    private final VillageNetwork.OpenVillageUiPayload payload;
    private final String[] actions;
    private final String[] labels;
    private final List<ClickRegion> clickRegions = new ArrayList<>();

    public VillageUiScreen(VillageNetwork.OpenVillageUiPayload payload) {
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xD40A0908);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(620, Math.max(300, width - 44));
        int panelHeight = Math.min(390, Math.max(250, height - 44));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        graphics.fill(left - 3, top - 3, left + panelWidth + 3, top + panelHeight + 3, 0xFFB6863A);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFF201713);
        graphics.fill(left + 8, top + 8, left + panelWidth - 8, top + 42, 0xFF3A211D);
        graphics.centeredText(font, payload.title(), width / 2, top + 20, 0xFFFFD98A);

        int closeLeft = left + panelWidth - 34;
        int closeTop = top + 12;
        graphics.fill(closeLeft, closeTop, closeLeft + 20, closeTop + 20,
                isInside(mouseX, mouseY, closeLeft, closeTop, 20, 20) ? 0xFFB84444 : 0xFF722A2A);
        graphics.centeredText(font, "×", closeLeft + 10, closeTop + 6, 0xFFFFFFFF);

        int buttonCount = Math.min(actions.length, labels.length);
        int buttonRows = (buttonCount + 1) / 2;
        int bodyBottom = top + panelHeight - 30 - buttonRows * 34;
        int bodyLeft = left + 22;
        int y = top + 58;
        for (String paragraph : payload.body().split("\n", -1)) {
            if (paragraph.isBlank()) {
                y += 7;
                continue;
            }
            for (FormattedCharSequence line : font.split(Component.literal(paragraph), panelWidth - 44)) {
                if (y + 10 > bodyBottom) break;
                graphics.text(font, line, bodyLeft, y, 0xFFEADCC6, false);
                y += 12;
            }
            if (y + 10 > bodyBottom) break;
            y += 3;
        }

        clickRegions.clear();
        int buttonWidth = (panelWidth - 54) / 2;
        int startY = top + panelHeight - 20 - buttonRows * 34;
        for (int index = 0; index < buttonCount; index++) {
            int x = left + 18 + (index % 2) * (buttonWidth + 18);
            int by = startY + (index / 2) * 34;
            int base = BUTTON_COLORS[index % BUTTON_COLORS.length];
            int color = isInside(mouseX, mouseY, x, by, buttonWidth, 26) ? brighten(base, 30) : base;
            graphics.fill(x - 1, by - 1, x + buttonWidth + 1, by + 27, 0xFFC89C54);
            graphics.fill(x, by, x + buttonWidth, by + 26, color);
            graphics.centeredText(font, labels[index], x + buttonWidth / 2, by + 9, 0xFFFFFFFF);
            clickRegions.add(new ClickRegion(x, by, buttonWidth, 26, actions[index]));
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int panelWidth = Math.min(620, Math.max(300, width - 44));
        int panelHeight = Math.min(390, Math.max(250, height - 44));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        if (click.button() == 0 && isInside(mouseX, mouseY, left + panelWidth - 34, top + 12, 20, 20)) {
            onClose();
            return true;
        }
        if (click.button() == 0) {
            for (ClickRegion region : clickRegions) {
                if (region.contains(mouseX, mouseY)) {
                    ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(region.action()));
                    onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(null);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int brighten(int argb, int amount) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = Math.min(255, ((argb >>> 16) & 0xFF) + amount);
        int green = Math.min(255, ((argb >>> 8) & 0xFF) + amount);
        int blue = Math.min(255, (argb & 0xFF) + amount);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private record ClickRegion(int x, int y, int width, int height, String action) {
        boolean contains(double mouseX, double mouseY) {
            return isInside(mouseX, mouseY, x, y, width, height);
        }
    }
}

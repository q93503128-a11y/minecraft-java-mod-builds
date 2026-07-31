package kr.moonseungjun.villageguardians;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class VillageUiScreen extends Screen {
    private static final String SEP = "\u001F";
    private static final int[] BUTTON_COLORS = {
            0xFF7E2A35,
            0xFF176B68,
            0xFF8C5A16,
            0xFF274C77,
            0xFF4D6A34,
            0xFF6B3E75
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xD40A0908);

        int panelWidth = Math.min(620, Math.max(300, width - 44));
        int panelHeight = Math.min(390, Math.max(250, height - 44));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        graphics.fill(left - 3, top - 3, left + panelWidth + 3, top + panelHeight + 3, 0xFFB6863A);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFF201713);
        graphics.fill(left + 8, top + 8, left + panelWidth - 8, top + 42, 0xFF3A211D);
        graphics.drawCenteredString(font, payload.title(), width / 2, top + 20, 0xFFFFD98A);

        int closeLeft = left + panelWidth - 34;
        int closeTop = top + 12;
        int closeColor = isInside(mouseX, mouseY, closeLeft, closeTop, 20, 20) ? 0xFFB84444 : 0xFF722A2A;
        graphics.fill(closeLeft, closeTop, closeLeft + 20, closeTop + 20, closeColor);
        graphics.drawCenteredString(font, "×", closeLeft + 10, closeTop + 6, 0xFFFFFFFF);

        int bodyLeft = left + 22;
        int bodyTop = top + 58;
        int buttonCount = Math.min(actions.length, labels.length);
        int buttonRows = (buttonCount + 1) / 2;
        int buttonAreaHeight = buttonCount == 0 ? 0 : buttonRows * 34 + 10;
        int bodyBottom = top + panelHeight - 20 - buttonAreaHeight;
        int bodyWidth = panelWidth - 44;
        int y = bodyTop;

        for (String paragraph : payload.body().split("\n", -1)) {
            if (paragraph.isBlank()) {
                y += 7;
                continue;
            }
            List<FormattedCharSequence> lines = font.split(Component.literal(paragraph), bodyWidth);
            for (FormattedCharSequence line : lines) {
                if (y + 10 > bodyBottom) {
                    break;
                }
                graphics.drawString(font, line, bodyLeft, y, 0xFFEADCC6, false);
                y += 12;
            }
            if (y + 10 > bodyBottom) {
                break;
            }
            y += 3;
        }

        clickRegions.clear();
        int buttonWidth = (panelWidth - 54) / 2;
        int startY = top + panelHeight - 20 - buttonRows * 34;
        for (int index = 0; index < buttonCount; index++) {
            int column = index % 2;
            int row = index / 2;
            int x = left + 18 + column * (buttonWidth + 18);
            int by = startY + row * 34;
            boolean hovered = isInside(mouseX, mouseY, x, by, buttonWidth, 26);
            int baseColor = BUTTON_COLORS[index % BUTTON_COLORS.length];
            int color = hovered ? brighten(baseColor, 30) : baseColor;
            graphics.fill(x - 1, by - 1, x + buttonWidth + 1, by + 27, 0xFFC89C54);
            graphics.fill(x, by, x + buttonWidth, by + 26, color);
            graphics.drawCenteredString(font, labels[index], x + buttonWidth / 2, by + 9, 0xFFFFFFFF);
            clickRegions.add(new ClickRegion(x, by, buttonWidth, 26, actions[index]));
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelWidth = Math.min(620, Math.max(300, width - 44));
        int panelHeight = Math.min(390, Math.max(250, height - 44));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int closeLeft = left + panelWidth - 34;
        int closeTop = top + 12;

        if (button == 0 && isInside(mouseX, mouseY, closeLeft, closeTop, 20, 20)) {
            onClose();
            return true;
        }

        if (button == 0) {
            for (ClickRegion region : clickRegions) {
                if (region.contains(mouseX, mouseY)) {
                    ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(region.action()));
                    onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
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

package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Defense-game style ability cards anchored safely above the vanilla hotbar. */
@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageSkillHudOverlay {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(
            VillageGuardians.MOD_ID, "skill_status_hud");
    private static String text = "";
    private static long expiresAt;

    private VillageSkillHudOverlay() {}

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.OVERLAY_MESSAGE, LAYER_ID, VillageSkillHudOverlay::render);
    }

    public static void accept(VillageNetwork.SkillHudPayload payload) {
        text = payload == null || payload.text() == null ? ""
                : VillageClientKeys.resolveTokens(payload.text());
        expiresAt = System.currentTimeMillis() + 1_500L;
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null || text.isBlank()
                || System.currentTimeMillis() > expiresAt) return;

        Font font = minecraft.font;
        String[] sections = text.split(" §8│ ", -1);
        int cardCount = Math.min(2, sections.length);
        if (cardCount <= 0) return;

        int centerX = graphics.guiWidth() / 2;
        int cardHeight = 34;
        int gap = graphics.guiWidth() < 420 ? 5 : 8;
        int cardWidth = Math.min(138, Math.max(92, (graphics.guiWidth() - 52) / 2));
        int totalWidth = cardCount * cardWidth + Math.max(0, cardCount - 1) * gap;
        int startX = centerX - totalWidth / 2;
        int y = Math.max(48, graphics.guiHeight() - 112);

        if (sections.length > 2) {
            StringBuilder active = new StringBuilder();
            for (int index = 2; index < sections.length; index++) {
                String part = plain(sections[index]);
                if (part.isBlank()) continue;
                if (!active.isEmpty()) active.append(" · ");
                active.append(part);
            }
            if (!active.isEmpty()) {
                String chip = fit(font, active.toString(), Math.max(80, totalWidth - 18));
                int chipWidth = Math.min(totalWidth, font.width(chip) + 22);
                int chipLeft = centerX - chipWidth / 2;
                graphics.fill(chipLeft, y - 19, chipLeft + chipWidth, y - 5, 0xD70E1A20);
                graphics.fill(chipLeft, y - 19, chipLeft + 3, y - 5, VillageDefenseUiTheme.CYAN);
                graphics.centeredText(font, chip, centerX, y - 15, VillageDefenseUiTheme.CYAN);
            }
        }

        for (int index = 0; index < cardCount; index++) {
            abilityCard(graphics, font, startX + index * (cardWidth + gap), y,
                    cardWidth, cardHeight, sections[index], index);
        }
    }

    private static void abilityCard(
            GuiGraphicsExtractor graphics,
            Font font,
            int left,
            int top,
            int width,
            int height,
            String formatted,
            int slot) {
        String value = plain(formatted);
        boolean empty = value.contains("비어 있음");
        boolean ready = !empty && value.contains("준비");
        int seconds = cooldownSeconds(value);
        int accent = empty ? VillageDefenseUiTheme.MUTED
                : ready ? VillageDefenseUiTheme.GREEN
                : seconds <= 3 ? VillageDefenseUiTheme.AMBER
                : slot == 0 ? VillageDefenseUiTheme.CYAN : VillageDefenseUiTheme.BLUE;

        VillageDefenseUiTheme.card(graphics, left, top, left + width, top + height, accent, ready);
        String slotLabel = slot == 0 ? "능력 1" : "능력 2";
        graphics.text(font, slotLabel, left + 9, top + 5, accent, true);

        String name = skillName(value);
        graphics.text(font, fit(font, name, width - 18), left + 9, top + 17,
                empty ? VillageDefenseUiTheme.MUTED : VillageDefenseUiTheme.TEXT, false);

        String state = empty ? "EMPTY" : ready ? "READY" : seconds > 0 ? seconds + "s" : "WAIT";
        graphics.text(font, state, left + width - font.width(state) - 7, top + 5, accent, true);

        graphics.fill(left + 8, top + height - 5, left + width - 7, top + height - 2,
                VillageDefenseUiTheme.TRACK);
        if (ready) {
            graphics.fill(left + 8, top + height - 5, left + width - 7, top + height - 2, accent);
        } else if (!empty && seconds > 0) {
            int fillWidth = Math.max(4, (width - 15) * Math.max(0, 10 - Math.min(10, seconds)) / 10);
            graphics.fill(left + 8, top + height - 5, left + 8 + fillWidth, top + height - 2, accent);
        }
    }

    private static String skillName(String value) {
        if (value == null || value.isBlank()) return "비어 있음";
        String cleaned = value.replace("준비", "").replace("비어 있음", "비어 있음").trim();
        int firstSpace = cleaned.indexOf(' ');
        if (firstSpace >= 0 && firstSpace + 1 < cleaned.length()) cleaned = cleaned.substring(firstSpace + 1).trim();
        int seconds = cleaned.indexOf("초");
        if (seconds > 0) {
            int start = seconds - 1;
            while (start >= 0 && Character.isDigit(cleaned.charAt(start))) start--;
            cleaned = (cleaned.substring(0, start + 1) + cleaned.substring(seconds + 1)).trim();
        }
        int bar = cleaned.indexOf('■');
        if (bar >= 0) cleaned = cleaned.substring(0, bar).trim();
        return cleaned.isBlank() ? "비어 있음" : cleaned;
    }

    private static int cooldownSeconds(String value) {
        if (value == null) return 0;
        int marker = value.indexOf("초");
        if (marker <= 0) return 0;
        int start = marker - 1;
        while (start >= 0 && Character.isDigit(value.charAt(start))) start--;
        if (start + 1 >= marker) return 0;
        try { return Integer.parseInt(value.substring(start + 1, marker)); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private static String fit(Font font, String value, int maximumWidth) {
        if (maximumWidth <= 0 || value == null) return "";
        if (font.width(value) <= maximumWidth) return value;
        String suffix = "…";
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + suffix) > maximumWidth) end--;
        return value.substring(0, end) + suffix;
    }
}

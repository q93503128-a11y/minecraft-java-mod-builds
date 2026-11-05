package io.github.q93503128.turnbound.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.NotNull;

/** Short in-game action feedback that remains visible while management screens stay open. */
public final class ClientUiFeedbackLayer implements GuiLayer {
    private static volatile String message = "";
    private static volatile boolean error;
    private static volatile long expiresAt;

    public static void show(String text) {
        if (text == null || text.isBlank()) return;
        message = TurnboundUiText.playerFacingLabel(Component.literal(text)).getString();
        String lower = message.toLowerCase(java.util.Locale.ROOT);
        error = message.contains("실패") || message.contains("부족") || message.contains("잠김")
                || message.contains("불가") || lower.contains("not enough") || lower.contains("requires");
        expiresAt = System.currentTimeMillis() + 4500L;
    }

    public static String current() {
        if (System.currentTimeMillis() > expiresAt) return "";
        return message;
    }

    public static boolean error() { return error; }

    @Override
    public void render(@NotNull GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        String text = current();
        if (text.isBlank()) return;

        String symbol = error ? "!" : "✓";
        int accent = error ? TurnboundUiTokens.DANGER : TurnboundUiTokens.SUCCESS;
        int symbolW = minecraft.font.width(symbol);
        int maxW = Math.min(340, Math.max(170, graphics.guiWidth() - 32));
        String fitted = UiTextLayout.fit(text, maxW - 34 - symbolW);
        int w = Math.min(maxW, minecraft.font.width(fitted) + symbolW + 32);
        int h = 27;
        int x = (graphics.guiWidth() - w) / 2;
        int y = minecraft.gui.screen() == null ? graphics.guiHeight() - h - 42 : 12;

        TurnboundFrameStyle.frame(graphics, x, y, w, h, accent);
        graphics.text(minecraft.font, Component.literal(symbol), x + 13, y + 9, accent, true);
        graphics.text(minecraft.font, Component.literal(fitted), x + 20 + symbolW, y + 9,
                TurnboundUiTokens.TEXT_PRIMARY, true);
    }
}

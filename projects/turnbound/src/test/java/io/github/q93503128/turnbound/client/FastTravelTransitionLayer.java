package io.github.q93503128.turnbound.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.NotNull;

/** Brief world-visible fade around server-authoritative waystation teleport. */
public final class FastTravelTransitionLayer implements GuiLayer {
    private enum Phase { NONE, OUT, HOLD, IN }

    private static final int FADE_OUT_TICKS = 12;
    private static final int FADE_IN_TICKS = 14;
    private static final int HOLD_FAILSAFE_TICKS = 80;
    private static Phase phase = Phase.NONE;
    private static int ticks;
    private static String label = "";

    public static void accept(String encoded) {
        String[] parts = encoded == null ? new String[0] : encoded.split("\n", 2);
        String command = parts.length == 0 ? "" : parts[0].trim();
        label = parts.length < 2 ? "" : parts[1].trim();
        switch (command) {
            case "START" -> { phase = Phase.OUT; ticks = 0; playTravelSound(SoundEvents.HORSE_GALLOP, 0.72F, 0.92F); }
            case "ARRIVE" -> { phase = Phase.IN; ticks = 0; playTravelSound(SoundEvents.HORSE_LAND, 0.78F, 1.02F); }
            case "CANCEL" -> { phase = Phase.IN; ticks = 0; }
            default -> { phase = Phase.NONE; ticks = 0; label = ""; }
        }
    }

    private static void playTravelSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!TurnboundClientSettings.sfxEnabled()
                || minecraft.level == null || minecraft.player == null || sound == null) return;
        minecraft.level.playLocalSound(
                minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(),
                sound, SoundSource.PLAYERS,
                volume * TurnboundClientSettings.sfxGain(), pitch, false);
    }

    public static void onTick(ClientTickEvent.Post event) {
        if (phase == Phase.NONE) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            phase = Phase.NONE; ticks = 0; label = ""; return;
        }
        ticks++;
        if (phase == Phase.OUT && ticks >= FADE_OUT_TICKS) {
            phase = Phase.HOLD; ticks = 0;
        } else if (phase == Phase.HOLD && ticks >= HOLD_FAILSAFE_TICKS) {
            phase = Phase.IN; ticks = 0;
        } else if (phase == Phase.IN && ticks >= FADE_IN_TICKS) {
            phase = Phase.NONE; ticks = 0; label = "";
        }
    }

    @Override
    public void render(@NotNull GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        if (phase == Phase.NONE) return;
        float alpha = switch (phase) {
            case OUT -> Math.min(1.0F, ticks / (float) FADE_OUT_TICKS);
            case HOLD -> 1.0F;
            case IN -> Math.max(0.0F, 1.0F - ticks / (float) FADE_IN_TICKS);
            default -> 0.0F;
        };
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), a << 24);
        if (alpha < 0.62F || label.isBlank()) return;

        Minecraft minecraft = Minecraft.getInstance();
        String text = UiTextLayout.fit(label + "로 이동 중", Math.min(240, graphics.guiWidth() - 32));
        int x = (graphics.guiWidth() - minecraft.font.width(text)) / 2;
        int y = graphics.guiHeight() / 2 + 18;
        int textAlpha = Math.max(0x55, a);
        graphics.text(minecraft.font, Component.literal(text), x, y, (textAlpha << 24) | 0x00F6F0E4, true);
    }
}

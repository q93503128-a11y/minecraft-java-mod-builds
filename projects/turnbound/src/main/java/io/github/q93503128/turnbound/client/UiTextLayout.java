package io.github.q93503128.turnbound.client;

import net.minecraft.client.Minecraft;

/** Pixel-width fitting used by every dense HUD surface so labels never escape their box. */
final class UiTextLayout {
    private UiTextLayout() {}

    static String fit(String value, int maxWidth) {
        if (value == null || value.isEmpty()) return "";
        var font = Minecraft.getInstance().font;
        if (font.width(value) <= Math.max(1, maxWidth)) return value;
        String suffix = "…";
        int end = value.length();
        while (end > 1 && font.width(value.substring(0, end) + suffix) > maxWidth) end--;
        return value.substring(0, Math.max(1, end)) + suffix;
    }
}
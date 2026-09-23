package io.github.q93503128.turnbound.client;

import net.minecraft.client.Minecraft;

/** Pixel-width fitting used by every dense HUD surface so labels never escape their box. */
final class UiTextLayout {
    private UiTextLayout() {}

    static java.util.List<String> wrap(String value, int maxWidth, int maxLines) {
        if (value == null || value.isBlank() || maxLines <= 0) return java.util.List.of();
        var font = Minecraft.getInstance().font;
        int width = Math.max(1, maxWidth);
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();

        for (String word : value.trim().split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.width(candidate) <= width) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            if (!line.isEmpty()) {
                out.add(line.toString());
                if (out.size() >= maxLines) break;
                line.setLength(0);
            }
            if (font.width(word) <= width) {
                line.append(word);
            } else {
                String rest = word;
                while (!rest.isEmpty() && out.size() < maxLines) {
                    String fitted = fit(rest, width);
                    if (fitted.endsWith("…")) {
                        int take = Math.max(1, fitted.length() - 1);
                        out.add(rest.substring(0, Math.min(take, rest.length())));
                        rest = rest.substring(Math.min(take, rest.length()));
                    } else {
                        line.append(rest);
                        rest = "";
                    }
                }
            }
        }
        if (!line.isEmpty() && out.size() < maxLines) out.add(line.toString());
        if (out.isEmpty()) out.add(fit(value, width));
        if (out.size() == maxLines) {
            String joined = String.join(" ", out);
            if (joined.length() < value.trim().length()) {
                int last = out.size() - 1;
                out.set(last, fit(out.get(last) + "…", width));
            }
        }
        return java.util.List.copyOf(out);
    }

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
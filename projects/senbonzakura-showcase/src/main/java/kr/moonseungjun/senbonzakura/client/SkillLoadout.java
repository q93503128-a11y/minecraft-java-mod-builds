package kr.moonseungjun.senbonzakura.client;

import kr.moonseungjun.senbonzakura.ability.ShowcaseAbility;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Client-side persistent 10-slot loadout.
 *
 * The showcase currently has no unlock/progression layer, so every registered showcase ability
 * is visible in the library. The loadout is persisted per Minecraft instance and only stores ids.
 */
public final class SkillLoadout {
    public static final int SLOT_COUNT = 10;
    private static final ShowcaseAbility[] SLOTS = new ShowcaseAbility[SLOT_COUNT];
    private static boolean loaded;

    private SkillLoadout() {}

    public static ShowcaseAbility get(int slot) {
        ensureLoaded();
        return slot >= 0 && slot < SLOT_COUNT ? SLOTS[slot] : null;
    }

    public static ShowcaseAbility[] snapshot() {
        ensureLoaded();
        return Arrays.copyOf(SLOTS, SLOT_COUNT);
    }

    public static void set(int slot, ShowcaseAbility ability) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        ensureLoaded();
        if (ability != null) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                if (i != slot && SLOTS[i] == ability) SLOTS[i] = null;
            }
        }
        SLOTS[slot] = ability;
        save();
    }

    public static void clear(int slot) {
        set(slot, null);
    }

    public static int firstEmpty() {
        ensureLoaded();
        for (int i = 0; i < SLOT_COUNT; i++) if (SLOTS[i] == null) return i;
        return -1;
    }

    public static int slotOf(ShowcaseAbility ability) {
        ensureLoaded();
        if (ability == null) return -1;
        for (int i = 0; i < SLOT_COUNT; i++) if (SLOTS[i] == ability) return i;
        return -1;
    }

    public static void resetDefaults() {
        Arrays.fill(SLOTS, null);
        ShowcaseAbility[] abilities = ShowcaseAbility.values();
        for (int i = 0; i < Math.min(abilities.length, SLOT_COUNT); i++) SLOTS[i] = abilities[i];
        loaded = true;
        save();
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Arrays.fill(SLOTS, null);

        Path file = configFile();
        if (Files.isRegularFile(file)) {
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (line == null || line.isBlank() || line.startsWith("#")) continue;
                    int split = line.indexOf('=');
                    if (split <= 4) continue;
                    String left = line.substring(0, split).trim();
                    if (!left.startsWith("slot")) continue;
                    int slot;
                    try { slot = Integer.parseInt(left.substring(4)); }
                    catch (NumberFormatException ignored) { continue; }
                    if (slot < 0 || slot >= SLOT_COUNT) continue;
                    String id = line.substring(split + 1).trim();
                    SLOTS[slot] = id.isEmpty() ? null : ShowcaseAbility.byId(id);
                }
                return;
            } catch (IOException ignored) {
                // Fall through to safe defaults and rewrite on the next edit.
            }
        }

        ShowcaseAbility[] abilities = ShowcaseAbility.values();
        for (int i = 0; i < Math.min(abilities.length, SLOT_COUNT); i++) SLOTS[i] = abilities[i];
        save();
    }

    private static void save() {
        Path file = configFile();
        try {
            Files.createDirectories(file.getParent());
            List<String> lines = new ArrayList<>();
            lines.add("# Senbonzakura Showcase skill loadout");
            lines.add("# slot0..slot9 correspond to Shift+1..Shift+0");
            for (int i = 0; i < SLOT_COUNT; i++) {
                lines.add("slot" + i + "=" + (SLOTS[i] == null ? "" : SLOTS[i].id()));
            }
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // A read-only instance should not prevent the player from using the in-memory loadout.
        }
    }

    private static Path configFile() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("senbonzakura-showcase-skills.txt");
    }
}

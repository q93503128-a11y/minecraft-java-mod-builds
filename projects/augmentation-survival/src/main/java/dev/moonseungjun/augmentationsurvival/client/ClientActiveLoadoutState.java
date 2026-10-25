package dev.moonseungjun.augmentationsurvival.client;

import java.util.ArrayList;
import java.util.List;

/** Client presentation cache; server payload remains authoritative. */
public final class ClientActiveLoadoutState {
    private static List<String> slots = emptySlots();
    private static List<Long> cooldownReadyMillis = emptyCooldowns();
    private static List<String> available = List.of();

    private ClientActiveLoadoutState() { }

    public static void apply(List<String> slotIds, List<Integer> cooldownTicks, List<String> availableIds) {
        ArrayList<String> nextSlots = new ArrayList<>(10);
        ArrayList<Long> nextCooldowns = new ArrayList<>(10);
        long now = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            nextSlots.add(i < slotIds.size() ? slotIds.get(i) : "");
            int ticks = i < cooldownTicks.size() ? Math.max(0, cooldownTicks.get(i)) : 0;
            nextCooldowns.add(now + ticks * 50L);
        }
        slots = List.copyOf(nextSlots);
        cooldownReadyMillis = List.copyOf(nextCooldowns);
        available = List.copyOf(availableIds);
    }

    public static void clear() {
        slots = emptySlots();
        cooldownReadyMillis = emptyCooldowns();
        available = List.of();
    }

    public static List<String> slots() { return slots; }
    public static List<String> available() { return available; }
    public static String slot(int index) { return index >= 0 && index < slots.size() ? slots.get(index) : ""; }
    public static int cooldownTicks(int index) {
        if (index < 0 || index >= cooldownReadyMillis.size()) return 0;
        long remaining = Math.max(0L, cooldownReadyMillis.get(index) - System.currentTimeMillis());
        return (int) Math.ceil(remaining / 50.0);
    }

    private static List<String> emptySlots() {
        ArrayList<String> values = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) values.add("");
        return List.copyOf(values);
    }
    private static List<Long> emptyCooldowns() {
        ArrayList<Long> values = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) values.add(0L);
        return List.copyOf(values);
    }
}

package dev.moonseungjun.augmentationsurvival.client;

import java.util.ArrayList;
import java.util.List;

/** Client presentation cache; every timer starts from a server-authoritative snapshot. */
public final class ClientActiveLoadoutState {
    private static List<String> slots = emptySlots();
    private static List<Long> cooldownReadyMillis = emptyLongs();
    private static List<Integer> maxCharges = emptyInts();
    private static List<Long> charge0ReadyMillis = emptyLongs();
    private static List<Long> charge1ReadyMillis = emptyLongs();
    private static List<String> available = List.of();

    private ClientActiveLoadoutState() { }

    public static void apply(
            List<String> slotIds,
            List<Integer> cooldownTicks,
            List<Integer> maxChargeValues,
            List<Integer> charge0Ticks,
            List<Integer> charge1Ticks,
            List<String> availableIds
    ) {
        ArrayList<String> nextSlots = new ArrayList<>(10);
        ArrayList<Long> nextCooldowns = new ArrayList<>(10);
        ArrayList<Integer> nextMaxCharges = new ArrayList<>(10);
        ArrayList<Long> nextCharge0 = new ArrayList<>(10);
        ArrayList<Long> nextCharge1 = new ArrayList<>(10);
        long now = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            nextSlots.add(i < slotIds.size() ? slotIds.get(i) : "");
            int cooldown = intAt(cooldownTicks, i);
            int max = Math.max(0, Math.min(2, intAt(maxChargeValues, i)));
            int first = intAt(charge0Ticks, i);
            int second = intAt(charge1Ticks, i);
            nextCooldowns.add(now + cooldown * 50L);
            nextMaxCharges.add(max);
            nextCharge0.add(now + first * 50L);
            nextCharge1.add(now + second * 50L);
        }
        slots = List.copyOf(nextSlots);
        cooldownReadyMillis = List.copyOf(nextCooldowns);
        maxCharges = List.copyOf(nextMaxCharges);
        charge0ReadyMillis = List.copyOf(nextCharge0);
        charge1ReadyMillis = List.copyOf(nextCharge1);
        available = List.copyOf(availableIds);
    }

    public static void clear() {
        slots = emptySlots();
        cooldownReadyMillis = emptyLongs();
        maxCharges = emptyInts();
        charge0ReadyMillis = emptyLongs();
        charge1ReadyMillis = emptyLongs();
        available = List.of();
    }

    public static List<String> slots() { return slots; }
    public static List<String> available() { return available; }
    public static String slot(int index) { return index >= 0 && index < slots.size() ? slots.get(index) : ""; }

    public static int cooldownTicks(int index) {
        if (index < 0 || index >= cooldownReadyMillis.size()) return 0;
        return remainingTicks(cooldownReadyMillis.get(index));
    }

    public static int maxCharges(int index) {
        return index >= 0 && index < maxCharges.size() ? maxCharges.get(index) : 0;
    }

    public static int readyCharges(int index) {
        int max = maxCharges(index);
        if (max <= 0 || index < 0 || index >= charge0ReadyMillis.size()) return 0;
        long now = System.currentTimeMillis();
        int ready = charge0ReadyMillis.get(index) <= now ? 1 : 0;
        if (max > 1 && index < charge1ReadyMillis.size() && charge1ReadyMillis.get(index) <= now) ready++;
        return ready;
    }

    public static int nextRechargeTicks(int index) {
        int max = maxCharges(index);
        if (max <= 0 || index < 0 || index >= charge0ReadyMillis.size()) return 0;
        long now = System.currentTimeMillis();
        long next = Long.MAX_VALUE;
        long first = charge0ReadyMillis.get(index);
        if (first > now) next = Math.min(next, first);
        if (max > 1 && index < charge1ReadyMillis.size()) {
            long second = charge1ReadyMillis.get(index);
            if (second > now) next = Math.min(next, second);
        }
        return next == Long.MAX_VALUE ? 0 : (int) Math.ceil((next - now) / 50.0D);
    }

    private static int remainingTicks(long readyMillis) {
        long remaining = Math.max(0L, readyMillis - System.currentTimeMillis());
        return (int) Math.ceil(remaining / 50.0D);
    }

    private static int intAt(List<Integer> values, int index) {
        return index < values.size() ? Math.max(0, values.get(index)) : 0;
    }

    private static List<String> emptySlots() {
        ArrayList<String> values = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) values.add("");
        return List.copyOf(values);
    }

    private static List<Long> emptyLongs() {
        ArrayList<Long> values = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) values.add(0L);
        return List.copyOf(values);
    }

    private static List<Integer> emptyInts() {
        ArrayList<Integer> values = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) values.add(0);
        return List.copyOf(values);
    }
}

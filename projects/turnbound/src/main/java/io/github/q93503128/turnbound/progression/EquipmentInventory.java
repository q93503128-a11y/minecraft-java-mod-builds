package io.github.q93503128.turnbound.progression;

import io.github.q93503128.turnbound.content.V04Catalogs;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Server-authoritative v0.4 equipment inventory, enhancement and equip rules. */
public final class EquipmentInventory {
    public static final int MAX_INSTANCES = 300;

    public enum Slot { WEAPON, ARMOR, ACCESSORY, SIGNATURE }

    public record Item(String instanceId, String itemId, int enhancementLevel) {
        public Item {
            if (instanceId == null || instanceId.isBlank() || itemId == null || itemId.isBlank()) {
                throw new IllegalArgumentException("Blank equipment identity");
            }
            if (enhancementLevel < 0 || enhancementLevel > 20) throw new IllegalArgumentException("Enhancement must be 0..20");
        }
    }

    public record Loadout(String weapon, String armor, String accessory, String signature) {
        public Loadout {
            weapon = safe(weapon); armor = safe(armor); accessory = safe(accessory); signature = safe(signature);
        }
        public static Loadout empty() { return new Loadout("", "", "", ""); }
        public String get(Slot slot) {
            return switch (slot) {
                case WEAPON -> weapon; case ARMOR -> armor; case ACCESSORY -> accessory; case SIGNATURE -> signature;
            };
        }
        public Loadout with(Slot slot, String instanceId) {
            String value = safe(instanceId);
            return switch (slot) {
                case WEAPON -> new Loadout(value, armor, accessory, signature);
                case ARMOR -> new Loadout(weapon, value, accessory, signature);
                case ACCESSORY -> new Loadout(weapon, armor, value, signature);
                case SIGNATURE -> new Loadout(weapon, armor, accessory, value);
            };
        }
        private static String safe(String value) { return value == null ? "" : value; }
    }

    public record Snapshot(
            long nextSerial,
            Map<String, Item> items,
            Map<String, Loadout> loadouts,
            Map<String, Integer> choiceTokens) {
        public Snapshot {
            if (nextSerial < 1) throw new IllegalArgumentException("nextSerial must be positive");
            items = Map.copyOf(items);
            loadouts = Map.copyOf(loadouts);
            choiceTokens = Map.copyOf(choiceTokens);
            if (items.size() > MAX_INSTANCES) throw new IllegalArgumentException("Equipment inventory exceeds " + MAX_INSTANCES + " instances");
            choiceTokens.forEach((tier, count) -> {
                if (count < 0) throw new IllegalArgumentException("Negative equipment choice token");
            });
        }
        public static Snapshot empty() { return new Snapshot(1, Map.of(), Map.of(), Map.of()); }
    }

    public record StatTotals(Map<String, Double> values) {
        public StatTotals { values = Map.copyOf(values); }
        public double value(String type) { return values.getOrDefault(type, 0.0); }
    }

    private long nextSerial = 1;
    private final Map<String, Item> items = new LinkedHashMap<>();
    private final Map<String, Loadout> loadouts = new LinkedHashMap<>();
    private final Map<String, Integer> choiceTokens = new LinkedHashMap<>();

    private EquipmentInventory() {}

    public static EquipmentInventory empty() { return new EquipmentInventory(); }

    public static EquipmentInventory restore(Snapshot snapshot) {
        EquipmentInventory inventory = new EquipmentInventory();
        inventory.nextSerial = snapshot.nextSerial();
        inventory.items.putAll(snapshot.items());
        inventory.loadouts.putAll(snapshot.loadouts());
        inventory.choiceTokens.putAll(snapshot.choiceTokens());
        inventory.validateReferences();
        return inventory;
    }

    public Snapshot snapshot() {
        return new Snapshot(nextSerial, items, loadouts, choiceTokens);
    }

    public int size() { return items.size(); }
    public int freeSlots() { return MAX_INSTANCES - items.size(); }
    public boolean hasFreeSlot() { return items.size() < MAX_INSTANCES; }

    public Set<String> unknownItemIds() {
        Set<String> out = new LinkedHashSet<>();
        for (Item item : items.values()) if (!isKnownItemId(item.itemId())) out.add(item.itemId());
        return Set.copyOf(out);
    }

    public Item grant(String itemId) {
        requireKnown(itemId);
        requireFreeSlot();
        String instanceId = "eq_" + String.format("%08d", nextSerial++);
        Item item = new Item(instanceId, itemId, 0);
        items.put(instanceId, item);
        return item;
    }

    public void grantChoiceToken(String tier, int amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative choice token grant");
        if (!List.of("T1", "T2", "T3", "T4").contains(tier)) throw new IllegalArgumentException("Invalid equipment tier " + tier);
        choiceTokens.merge(tier, amount, Integer::sum);
    }

    public int choiceTokens(String tier) { return choiceTokens.getOrDefault(tier, 0); }

    public Item claimChoice(String tier, String itemId) {
        int count = choiceTokens(tier);
        if (count <= 0) throw new IllegalStateException("No " + tier + " equipment choice token");
        ItemSpec spec = spec(itemId);
        if (spec.signature || !spec.tier.equals(tier)) throw new IllegalArgumentException("Choice token tier mismatch");
        requireFreeSlot();
        if (count == 1) choiceTokens.remove(tier); else choiceTokens.put(tier, count - 1);
        return grant(itemId);
    }

    public Item enhance(String instanceId, PlayerProfile profile) {
        Item item = item(instanceId);
        if (item.enhancementLevel() >= 20) throw new IllegalStateException("Equipment is already +20");
        ItemSpec spec = spec(item.itemId());
        int cost = V04Catalogs.enhanceCost(spec.signature ? "SIGNATURE" : spec.tier, item.enhancementLevel());
        if (!profile.spend(PlayerProfile.Currency.GOLD, cost)) throw new IllegalStateException("Not enough Gold");
        Item upgraded = new Item(item.instanceId(), item.itemId(), item.enhancementLevel() + 1);
        items.put(instanceId, upgraded);
        return upgraded;
    }

    /**
     * Sells one unequipped normal equipment instance at the exact v0.4 §99 tier value.
     * Canon does not define a Signature sale price or equipped-item auto-unequip behavior,
     * so both remain explicitly blocked instead of inventing rules.
     */
    public int sell(String instanceId, PlayerProfile profile) {
        Item item = item(instanceId);
        ItemSpec spec = spec(item.itemId());
        if (spec.signature) throw new IllegalStateException("Signature equipment has no canonical sale price");
        if (isEquipped(instanceId)) throw new IllegalStateException("Unequip equipment before selling it");
        int price = EquipmentRules.salePrice(spec.tier);
        profile.grant(PlayerProfile.Currency.GOLD, price);
        items.remove(instanceId);
        return price;
    }

    public void equip(String characterId, String instanceId, int currentStar) {
        Item item = item(instanceId);
        ItemSpec spec = spec(item.itemId());
        if (spec.signature) {
            if (!spec.owner.equals(characterId)) throw new IllegalArgumentException("Signature equipment owner mismatch");
            if (currentStar < 6) throw new IllegalStateException("Signature slot requires currentStar >= 6");
        }
        removeFromAllLoadouts(instanceId);
        Loadout current = loadouts.getOrDefault(characterId, Loadout.empty());
        loadouts.put(characterId, current.with(spec.slot, instanceId));
    }

    public void unequip(String characterId, Slot slot) {
        Loadout current = loadouts.getOrDefault(characterId, Loadout.empty());
        loadouts.put(characterId, current.with(slot, ""));
    }

    public Loadout loadout(String characterId) { return loadouts.getOrDefault(characterId, Loadout.empty()); }
    public Item item(String instanceId) {
        Item item = items.get(instanceId);
        if (item == null) throw new IllegalArgumentException("Unknown equipment instance " + instanceId);
        return item;
    }
    public Map<String, Item> items() { return Map.copyOf(items); }

    public StatTotals statTotals(String characterId) {
        Map<String, Double> values = new LinkedHashMap<>();
        Loadout loadout = loadout(characterId);
        for (Slot slot : Slot.values()) {
            String instanceId = loadout.get(slot);
            if (instanceId.isBlank()) continue;
            Item item = item(instanceId);
            ItemSpec spec = spec(item.itemId());
            merge(values, spec.main.type(), scaledMain(spec.main.value(), item.enhancementLevel()));
            merge(values, spec.sub.type(), scaledSub(spec.sub.value(), item.enhancementLevel()));
        }
        return new StatTotals(values);
    }

    public List<String> fixedRules(String characterId) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        Loadout loadout = loadout(characterId);
        for (Slot slot : Slot.values()) {
            String instanceId = loadout.get(slot);
            if (instanceId.isBlank()) continue;
            Item item = item(instanceId);
            ItemSpec spec = spec(item.itemId());
            if (!spec.fixedRule.isBlank()) out.add(spec.fixedRule);
            if (spec.signature) {
                if (item.enhancementLevel() >= 10 && !spec.m10.isBlank()) out.add(spec.m10);
                if (item.enhancementLevel() >= 20 && !spec.m20.isBlank()) out.add(spec.m20);
            }
        }
        return List.copyOf(out);
    }

    public static double scaledMain(double base, int enhancementLevel) {
        return base * (1.0 + 0.04 * enhancementLevel);
    }

    public static double scaledSub(double base, int enhancementLevel) {
        double factor = enhancementLevel >= 20 ? 2.00
                : enhancementLevel >= 15 ? 1.75
                : enhancementLevel >= 10 ? 1.50
                : enhancementLevel >= 5 ? 1.25 : 1.00;
        return base * factor;
    }

    private void validateReferences() {
        Set<String> equipped = new LinkedHashSet<>();
        for (var entry : loadouts.entrySet()) {
            for (Slot slot : Slot.values()) {
                String instanceId = entry.getValue().get(slot);
                if (instanceId.isBlank()) continue;
                if (!items.containsKey(instanceId)) throw new IllegalStateException("Loadout references missing item " + instanceId);
                if (!equipped.add(instanceId)) throw new IllegalStateException("Equipment instance is equipped twice " + instanceId);
            }
        }
    }

    private boolean isEquipped(String instanceId) {
        for (Loadout loadout : loadouts.values()) {
            for (Slot slot : Slot.values()) if (instanceId.equals(loadout.get(slot))) return true;
        }
        return false;
    }

    private void removeFromAllLoadouts(String instanceId) {
        for (var entry : List.copyOf(loadouts.entrySet())) {
            Loadout loadout = entry.getValue();
            for (Slot slot : Slot.values()) if (instanceId.equals(loadout.get(slot))) loadout = loadout.with(slot, "");
            loadouts.put(entry.getKey(), loadout);
        }
    }

    private void requireFreeSlot() {
        if (!hasFreeSlot()) throw new IllegalStateException("Equipment inventory is full (" + MAX_INSTANCES + "/" + MAX_INSTANCES + ")");
    }

    private static void merge(Map<String, Double> values, String type, double amount) {
        values.merge(type, amount, Double::sum);
    }

    private static boolean isKnownItemId(String itemId) {
        try { spec(itemId); return true; } catch (RuntimeException ex) { return false; }
    }

    private static void requireKnown(String itemId) { spec(itemId); }

    private static ItemSpec spec(String itemId) {
        try {
            V04Catalogs.EquipmentSpec normal = V04Catalogs.equipment(itemId);
            return new ItemSpec(Slot.valueOf(normal.slot()), normal.tier(), false, "", normal.main(), normal.sub(), normal.fixedEffect(), "", "");
        } catch (RuntimeException ignored) {
            V04Catalogs.SignatureSpec signature = V04Catalogs.signature(itemId);
            return new ItemSpec(Slot.SIGNATURE, "SIGNATURE", true, signature.owner(), signature.main(), signature.sub(), signature.baseRule(), signature.milestone10(), signature.milestone20());
        }
    }

    private record ItemSpec(
            Slot slot, String tier, boolean signature, String owner,
            V04Catalogs.Stat main, V04Catalogs.Stat sub,
            String fixedRule, String m10, String m20) {}
}

package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Short 2/3-piece universal-power sets. Conditional siege damage is deliberately not the main power budget. */
public final class VillageEquipmentSetSystem {
    private VillageEquipmentSetSystem() {}

    public static EquipmentSet setOf(ItemStack stack) {
        if (stack == null || stack.isEmpty() || VillageEquipmentRaritySystem.rarityOf(stack) == null) return null;
        Item item = stack.getItem();
        if (item == Items.SHIELD || isChest(item) || isLegs(item)) return EquipmentSet.WALL_GUARDIAN;
        if (item == Items.BOW || item == Items.CROSSBOW || isHelmet(item) || isBoots(item)) return EquipmentSet.NIGHT_HUNTER;
        return null;
    }

    public static int countEquipped(Player player, EquipmentSet set) {
        if (player == null || set == null) return 0;
        int count = 0;
        if (setOf(player.getMainHandItem()) == set) count++;
        if (setOf(player.getOffhandItem()) == set) count++;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (setOf(player.getItemBySlot(slot)) == set) count++;
        }
        return Math.min(3, count);
    }

    public static float outgoingMultiplier(ServerPlayer player, boolean projectile) {
        int wall = countEquipped(player, EquipmentSet.WALL_GUARDIAN);
        int hunter = countEquipped(player, EquipmentSet.NIGHT_HUNTER);
        float value = 1.0f;
        if (wall >= 2) value *= 1.06f;
        if (wall >= 3) value *= 1.05f;
        if (hunter >= 2 && projectile) value *= 1.10f;
        if (hunter >= 3) value *= projectile ? 1.08f : 1.03f;
        return value;
    }

    public static float incomingMultiplier(ServerPlayer player) {
        int wall = countEquipped(player, EquipmentSet.WALL_GUARDIAN);
        int hunter = countEquipped(player, EquipmentSet.NIGHT_HUNTER);
        float value = 1.0f;
        if (wall >= 2) value *= 0.92f;
        if (wall >= 3) value *= 0.94f;
        if (hunter >= 3) value *= 0.96f;
        return Math.max(0.78f, value);
    }

    public static String inventorySummary(Player player) {
        int wall = countEquipped(player, EquipmentSet.WALL_GUARDIAN);
        int hunter = countEquipped(player, EquipmentSet.NIGHT_HUNTER);
        return "성벽 수호자 " + wall + "/3 " + (wall >= 2 ? "◆2" : "◇2") + " " + (wall >= 3 ? "◆3" : "◇3")
                + " · 밤사냥꾼 " + hunter + "/3 " + (hunter >= 2 ? "◆2" : "◇2") + " " + (hunter >= 3 ? "◆3" : "◇3");
    }

    public static String tooltipSummary(Player player, ItemStack stack) {
        EquipmentSet set = setOf(stack);
        if (set == null) return "";
        int count = player == null ? 0 : countEquipped(player, set);
        return set.displayName() + " " + count + "/3 · " + set.effectText();
    }

    private static boolean isHelmet(Item item) {
        return item == Items.IRON_HELMET || item == Items.DIAMOND_HELMET || item == Items.NETHERITE_HELMET;
    }
    private static boolean isChest(Item item) {
        return item == Items.IRON_CHESTPLATE || item == Items.DIAMOND_CHESTPLATE || item == Items.NETHERITE_CHESTPLATE;
    }
    private static boolean isLegs(Item item) {
        return item == Items.IRON_LEGGINGS || item == Items.DIAMOND_LEGGINGS || item == Items.NETHERITE_LEGGINGS;
    }
    private static boolean isBoots(Item item) {
        return item == Items.IRON_BOOTS || item == Items.DIAMOND_BOOTS || item == Items.NETHERITE_BOOTS;
    }

    public enum EquipmentSet {
        WALL_GUARDIAN("성벽 수호자", "2세트: 범용 피해 +6%, 받는 피해 -8% · 3세트: 범용 피해 추가 +5%, 받는 피해 추가 -6%"),
        NIGHT_HUNTER("밤사냥꾼", "2세트: 원거리 피해 +10% · 3세트: 원거리 추가 +8%, 근접 +3%, 받는 피해 -4%");
        private final String displayName, effectText;
        EquipmentSet(String displayName, String effectText) { this.displayName = displayName; this.effectText = effectText; }
        public String displayName() { return displayName; }
        public String effectText() { return effectText; }
    }
}

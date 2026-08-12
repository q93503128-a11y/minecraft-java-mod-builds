package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Weapon families keep rarity power universal while changing the feel of the base weapon choice. */
public final class VillageWeaponStyleSystem {
    private VillageWeaponStyleSystem() {}

    public static float outgoingMultiplier(ServerPlayer player, boolean projectile) {
        if (player == null) return 1.0f;
        ItemStack stack = projectile
                ? (isProjectileWeapon(player.getMainHandItem().getItem()) ? player.getMainHandItem() : player.getOffhandItem())
                : player.getMainHandItem();
        Style style = styleOf(stack);
        if (style == null) return 1.0f;
        return projectile ? style.projectileMultiplier() : style.meleeMultiplier();
    }

    public static Style styleOf(ItemStack stack) {
        if (stack == null || stack.isEmpty() || VillageEquipmentRaritySystem.rarityOf(stack) == null) return null;
        Item item = stack.getItem();
        if (item == Items.IRON_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD) return Style.LONGSWORD;
        if (item == Items.IRON_AXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE) return Style.GREAT_AXE;
        if (item == Items.TRIDENT) return Style.SPEAR;
        if (item == Items.MACE) return Style.WAR_HAMMER;
        if (item == Items.BOW) return Style.LONGBOW;
        if (item == Items.CROSSBOW) return Style.CROSSBOW;
        return null;
    }

    public static String tooltip(ItemStack stack) {
        Style style = styleOf(stack);
        return style == null ? "" : style.displayName() + " · " + style.description();
    }

    private static boolean isProjectileWeapon(Item item) {
        return item == Items.BOW || item == Items.CROSSBOW || item == Items.TRIDENT;
    }

    public enum Style {
        LONGSWORD("장검", 1.03f, 1.00f, "균형형 · 근접 피해 +3%"),
        GREAT_AXE("대형 도끼", 1.10f, 1.00f, "중량형 · 근접 피해 +10%"),
        SPEAR("장창", 1.06f, 1.06f, "범용 찌르기 · 근접/투척 피해 +6%"),
        WAR_HAMMER("전투 망치", 1.12f, 1.00f, "강타형 · 근접 피해 +12%"),
        LONGBOW("장궁", 1.00f, 1.05f, "정밀 사격 · 원거리 피해 +5%"),
        CROSSBOW("석궁", 1.00f, 1.08f, "관통 사격 · 원거리 피해 +8%");
        private final String displayName, description;
        private final float meleeMultiplier, projectileMultiplier;
        Style(String displayName, float meleeMultiplier, float projectileMultiplier, String description) {
            this.displayName = displayName; this.meleeMultiplier = meleeMultiplier;
            this.projectileMultiplier = projectileMultiplier; this.description = description;
        }
        public String displayName() { return displayName; }
        public float meleeMultiplier() { return meleeMultiplier; }
        public float projectileMultiplier() { return projectileMultiplier; }
        public String description() { return description; }
    }
}

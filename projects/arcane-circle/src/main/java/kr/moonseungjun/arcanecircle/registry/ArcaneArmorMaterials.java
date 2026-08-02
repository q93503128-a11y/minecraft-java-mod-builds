package kr.moonseungjun.arcanecircle.registry;

import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/** Custom equipment assets stop Arcane gear from inheriting vanilla leather/diamond visuals. */
public final class ArcaneArmorMaterials {
    public static final ResourceKey<EquipmentAsset> MAGE_ASSET = asset("mage");
    public static final ResourceKey<EquipmentAsset> SAGE_ASSET = asset("sage");
    public static final ResourceKey<EquipmentAsset> ARCHMAGE_ASSET = asset("archmage");

    public static final ArmorMaterial MAGE = new ArmorMaterial(
            22, defense(2, 4, 5, 2), 22, SoundEvents.ARMOR_EQUIP_LEATHER,
            0.5F, 0.0F, ItemTags.REPAIRS_LEATHER_ARMOR, MAGE_ASSET);
    public static final ArmorMaterial SAGE = new ArmorMaterial(
            38, defense(3, 6, 8, 3), 28, SoundEvents.ARMOR_EQUIP_LEATHER,
            2.0F, 0.02F, ItemTags.REPAIRS_LEATHER_ARMOR, SAGE_ASSET);
    public static final ArmorMaterial ARCHMAGE = new ArmorMaterial(
            58, defense(4, 8, 11, 4), 34, SoundEvents.ARMOR_EQUIP_DIAMOND,
            4.5F, 0.08F, ItemTags.REPAIRS_DIAMOND_ARMOR, ARCHMAGE_ASSET);

    private ArcaneArmorMaterials() {}

    private static ResourceKey<EquipmentAsset> asset(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID,
                Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, name));
    }

    private static Map<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet) {
        return Map.of(
                ArmorType.BOOTS, boots,
                ArmorType.LEGGINGS, legs,
                ArmorType.CHESTPLATE, chest,
                ArmorType.HELMET, helmet,
                ArmorType.BODY, 0);
    }
}

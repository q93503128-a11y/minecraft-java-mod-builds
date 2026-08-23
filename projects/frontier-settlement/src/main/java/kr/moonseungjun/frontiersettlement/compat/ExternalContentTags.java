package kr.moonseungjun.frontiersettlement.compat;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Soft compatibility seam for companion mods and datapacks.
 *
 * Frontier never links against companion code here. A mod/datapack can add its items to the
 * frontier_settlement tags below, while the conventional c: tags cover many well-behaved modded
 * materials automatically. Missing tags are simply false at runtime.
 */
public final class ExternalContentTags {
    public static final TagKey<Item> SETTLEMENT_WOOD = frontier("settlement_wood");
    public static final TagKey<Item> SETTLEMENT_STONE = frontier("settlement_stone");
    public static final TagKey<Item> SETTLEMENT_METAL = frontier("settlement_metal");
    public static final TagKey<Item> SETTLEMENT_FOOD = frontier("settlement_food");
    public static final TagKey<Item> EXPEDITION_RELICS = frontier("expedition_relics");

    public static final TagKey<Item> C_INGOTS = common("ingots");
    public static final TagKey<Item> C_RAW_MATERIALS = common("raw_materials");
    public static final TagKey<Item> C_STONES = common("stones");
    public static final TagKey<Item> C_COBBLESTONES = common("cobblestones");
    public static final TagKey<Item> C_FOODS = common("foods");

    private ExternalContentTags() {}

    private static TagKey<Item> frontier(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, path));
    }

    private static TagKey<Item> common(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }
}

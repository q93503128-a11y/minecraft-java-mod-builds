package dev.moonseungjun.augmentationsurvival.core;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.device.AugmentDeviceBlocks;
import dev.moonseungjun.augmentationsurvival.guide.GuidebookItems;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Player-facing creative inventory group for progression cores and workstations. */
public final class AugmentCreativeTabs {
    private static final ResourceKey<CreativeModeTab> MAIN_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "main")
    );

    private static final CreativeModeTab MAIN_TAB = FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.augmentation_survival"))
            .icon(() -> new ItemStack(AugmentCoreItems.RARE_CORE))
            .displayItems((parameters, output) -> {
                output.accept(AugmentCoreItems.COMMON_CORE);
                output.accept(AugmentCoreItems.UNCOMMON_CORE);
                output.accept(AugmentCoreItems.RARE_CORE);
                output.accept(AugmentCoreItems.EPIC_CORE);
                output.accept(AugmentCoreItems.LEGENDARY_CORE);
                output.accept(AugmentCoreItems.MYTHIC_CORE);
                output.accept(AugmentDeviceBlocks.AUGMENT_DEVICE);
                output.accept(AugmentDeviceBlocks.ADVANCED_AUGMENT_DEVICE);
                output.accept(GuidebookItems.GUIDEBOOK);
            })
            .build();

    private AugmentCreativeTabs() {
    }

    public static void initialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN_TAB_KEY, MAIN_TAB);
    }
}

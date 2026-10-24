package dev.moonseungjun.augmentationsurvival.menu;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Menu type registry for the two workstation tiers. */
public final class AugmentDeviceMenus {
    public static final MenuType<AugmentDeviceMenu> BASIC = register(
            "augment_device",
            AugmentDeviceMenu::clientBasic
    );
    public static final MenuType<AugmentDeviceMenu> ADVANCED = register(
            "advanced_augment_device",
            AugmentDeviceMenu::clientAdvanced
    );

    private AugmentDeviceMenus() {
    }

    public static void initialize() {
        // Static initialization performs registration.
    }

    private static <T extends AbstractContainerMenu> MenuType<T> register(
            String path,
            MenuType.MenuSupplier<T> supplier
    ) {
        return Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, path),
                new MenuType<>(supplier, FeatureFlagSet.of())
        );
    }
}

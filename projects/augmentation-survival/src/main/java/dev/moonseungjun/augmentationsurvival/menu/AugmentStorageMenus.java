package dev.moonseungjun.augmentationsurvival.menu;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Menu registry for the portable Pockets / Remote Storage surface. */
public final class AugmentStorageMenus {
    public static final MenuType<AugmentStorageMenu> STORAGE = register(
            "augment_storage",
            AugmentStorageMenu::client
    );

    private AugmentStorageMenus() { }

    public static void initialize() { }

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

package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registered stack-level combat identity components. */
public final class RiftfrontierCombatDataComponents {
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(
        Registries.DATA_COMPONENT_TYPE,
        Riftfrontier.MOD_ID
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PlayerWeaponLoadoutComponent>> PLAYER_WEAPON_LOADOUT =
        COMPONENTS.registerComponentType(
            "player_weapon_loadout",
            builder -> builder.persistent(PlayerWeaponLoadoutComponent.CODEC)
        );

    private RiftfrontierCombatDataComponents() {}

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}

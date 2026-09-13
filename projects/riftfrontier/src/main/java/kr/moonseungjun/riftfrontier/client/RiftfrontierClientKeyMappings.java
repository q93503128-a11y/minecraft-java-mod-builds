package kr.moonseungjun.riftfrontier.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.riftfrontier.Riftfrontier;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/** Client-only configurable controls for authored weapon action slots. */
@EventBusSubscriber(value = Dist.CLIENT, modid = Riftfrontier.MOD_ID)
public final class RiftfrontierClientKeyMappings {
    static final KeyMapping.Category COMBAT_CATEGORY = new KeyMapping.Category(
        Identifier.fromNamespaceAndPath(Riftfrontier.MOD_ID, "combat")
    );

    static final KeyMapping WEAPON_ACTION_1 = unbound("key.riftfrontier.weapon_action_1");
    static final KeyMapping WEAPON_ACTION_2 = unbound("key.riftfrontier.weapon_action_2");

    private RiftfrontierClientKeyMappings() {}

    private static KeyMapping unbound(String translationKey) {
        return new KeyMapping(
            translationKey,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            COMBAT_CATEGORY
        );
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(COMBAT_CATEGORY);
        event.register(WEAPON_ACTION_1);
        event.register(WEAPON_ACTION_2);
    }
}

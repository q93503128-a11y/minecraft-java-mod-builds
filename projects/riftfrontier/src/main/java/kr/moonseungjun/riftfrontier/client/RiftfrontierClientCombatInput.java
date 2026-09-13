package kr.moonseungjun.riftfrontier.client;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.combat.CombatRuntimeCatalog;
import kr.moonseungjun.riftfrontier.combat.PlayerWeaponLoadoutComponent;
import kr.moonseungjun.riftfrontier.combat.PlayerWeaponMoveSlotResolver;
import kr.moonseungjun.riftfrontier.combat.RiftfrontierCombatDataComponents;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.network.PlayerWeaponMoveIntentPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ClientTickEvent;
import net.neoforged.neoforge.network.ClientPacketDistributor;

/**
 * Client input bridge for production weapon intents.
 *
 * <p>Controls are deliberately unbound by default until the final control layout is approved. When
 * the player assigns them in Minecraft Controls, each click resolves only an authored move id from
 * the locally published content graph. The server still re-resolves player identity, main-hand
 * loadout, content generation and attack authority before execution.</p>
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = Riftfrontier.MOD_ID)
public final class RiftfrontierClientCombatInput {
    private RiftfrontierClientCombatInput() {}

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        while (RiftfrontierClientKeyMappings.WEAPON_ACTION_1.consumeClick()) sendActionSlot(0);
        while (RiftfrontierClientKeyMappings.WEAPON_ACTION_2.consumeClick()) sendActionSlot(1);
    }

    private static void sendActionSlot(int slot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return;

        var stack = minecraft.player.getMainHandItem();
        PlayerWeaponLoadoutComponent loadout = stack.get(
            RiftfrontierCombatDataComponents.PLAYER_WEAPON_LOADOUT.value()
        );
        if (loadout == null) return;

        try {
            ContentId familyId = ContentId.parse(loadout.familyId());
            var catalog = new CombatRuntimeCatalog(ContentRuntime.requireCurrent());
            var family = catalog.requireWeaponFamily(familyId);
            PlayerWeaponMoveSlotResolver.resolve(family, slot)
                .ifPresent(moveId -> ClientPacketDistributor.sendToServer(new PlayerWeaponMoveIntentPayload(moveId)));
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            // A stale/malformed client-visible loadout is not authority. The server would reject it as well.
        }
    }
}

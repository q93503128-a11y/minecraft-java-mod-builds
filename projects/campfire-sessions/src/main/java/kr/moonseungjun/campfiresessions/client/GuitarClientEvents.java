package kr.moonseungjun.campfiresessions.client;

import kr.moonseungjun.campfiresessions.CampfireSessions;
import kr.moonseungjun.campfiresessions.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = CampfireSessions.MOD_ID)
public final class GuitarClientEvents {
    private GuitarClientEvents() {}

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getItemStack().is(ModItems.ACOUSTIC_GUITAR.get())) return;
        openScreen();
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(ModItems.ACOUSTIC_GUITAR.get())) return;
        openScreen();
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void openScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.gui.screen() instanceof MusicPlayerScreen)) {
            minecraft.gui.setScreen(new MusicPlayerScreen());
        }
    }
}

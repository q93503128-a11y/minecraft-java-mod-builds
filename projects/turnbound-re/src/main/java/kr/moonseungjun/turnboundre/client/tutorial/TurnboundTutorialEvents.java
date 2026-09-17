package kr.moonseungjun.turnboundre.client.tutorial;

import kr.moonseungjun.turnboundre.TurnboundRe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Lightweight onboarding cue using Minecraft 26.2's own tutorial-toast presentation. */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class TurnboundTutorialEvents {
    private static boolean shownThisSession;
    private static int ticksInWorld;

    private TurnboundTutorialEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (shownThisSession) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            ticksInWorld = 0;
            return;
        }
        if (++ticksInWorld < 30) return;

        shownThisSession = true;
        minecraft.gui.toastManager().addToast(new TutorialToast(
                minecraft.font,
                TutorialToast.Icons.RECIPE_BOOK,
                Component.translatable("tutorial.turnbound_re.start.title"),
                Component.translatable("tutorial.turnbound_re.start.body"),
                false,
                9000));
    }
}

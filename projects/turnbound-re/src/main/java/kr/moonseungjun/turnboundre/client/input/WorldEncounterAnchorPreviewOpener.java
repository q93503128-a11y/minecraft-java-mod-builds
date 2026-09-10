package kr.moonseungjun.turnboundre.client.input;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.client.WorldEncounterAnchorClientState;
import kr.moonseungjun.turnboundre.client.ui.WorldEncounterAnchorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opens a newly received authored Encounter preview without stealing focus from unrelated screens. */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class WorldEncounterAnchorPreviewOpener {
    private static long observedGeneration = -1L;

    private WorldEncounterAnchorPreviewOpener() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        long generation = WorldEncounterAnchorClientState.generation();
        if (generation == observedGeneration) return;
        observedGeneration = generation;

        if (WorldEncounterAnchorClientState.view().isEmpty()) return;
        if (BattleClientState.latestSnapshot().isPresent()) return;

        Screen current = minecraft.gui.screen();
        if (current == null) {
            minecraft.gui.setScreen(new WorldEncounterAnchorScreen());
        }
        // An already-open anchor screen consumes the new generation itself. Other screens keep focus;
        // the preview is not delayed and popped unexpectedly after they close.
    }
}

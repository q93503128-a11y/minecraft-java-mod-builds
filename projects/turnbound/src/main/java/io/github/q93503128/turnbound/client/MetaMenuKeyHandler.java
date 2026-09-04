package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.MetaCommandPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/** v0.4 management menu and authored map shortcuts. */
public final class MetaMenuKeyHandler {
    private MetaMenuKeyHandler() {}

    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        if (event.getKey() == GLFW.GLFW_KEY_M) {
            if (minecraft.gui.screen() instanceof AsterMarchMapScreen screen) {
                screen.onClose();
                return;
            }
            if (minecraft.gui.screen() != null || ClientBattleState.snapshot().active() || !ClientFieldState.snapshot().active()) return;
            minecraft.gui.setScreen(new AsterMarchMapScreen());
            return;
        }

        if (event.getKey() != GLFW.GLFW_KEY_E) return;
        if (minecraft.gui.screen() instanceof MetaMenuScreen screen) {
            FacilityUiAccess.clear();
            screen.onClose();
            return;
        }
        if (minecraft.gui.screen() != null || ClientBattleState.snapshot().active()) return;
        FacilityUiAccess.clear();
        ClientPacketDistributor.sendToServer(new MetaCommandPayload("OPEN"));
    }
}

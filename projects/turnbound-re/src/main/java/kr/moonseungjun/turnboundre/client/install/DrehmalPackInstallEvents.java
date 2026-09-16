package kr.moonseungjun.turnboundre.client.install;

import kr.moonseungjun.turnboundre.TurnboundRe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.nio.file.Path;

/** Opens the first-run world preparation surface only inside the official Modrinth distribution. */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class DrehmalPackInstallEvents {
    private static boolean presentedThisSession;

    private DrehmalPackInstallEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (presentedThisSession) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.gui.screen() instanceof TitleScreen titleScreen)) return;

        Path gameDirectory = minecraft.gameDirectory.toPath();
        if (!DrehmalPackInstaller.distributionEnabled(gameDirectory)) return;
        if (DrehmalPackInstaller.ready(gameDirectory)) return;

        presentedThisSession = true;
        DrehmalPackInstaller.start(gameDirectory);
        minecraft.gui.setScreen(new DrehmalPackInstallScreen(titleScreen, gameDirectory));
    }
}

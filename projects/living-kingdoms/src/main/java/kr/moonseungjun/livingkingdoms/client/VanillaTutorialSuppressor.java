package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.tutorial.TutorialSteps;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Living Kingdoms supplies its own onboarding, so the vanilla punch-tree toast is noise here. */
final class VanillaTutorialSuppressor {
    private static boolean suppressedForCurrentSession;

    private VanillaTutorialSuppressor() {
    }

    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            suppressedForCurrentSession = false;
            return;
        }
        if (suppressedForCurrentSession) {
            return;
        }
        if (!LivingKingdoms.MOD_ID.equals(minecraft.level.dimension().identifier().getNamespace())) {
            return;
        }

        minecraft.getTutorial().setStep(TutorialSteps.NONE);
        suppressedForCurrentSession = true;
        LivingKingdoms.LOGGER.info("Disabled vanilla tutorial hints inside the Living Kingdoms realm");
    }
}

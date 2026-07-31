package kr.moonseungjun.shaderlab;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(value = ShaderLab.MOD_ID, dist = Dist.CLIENT)
public final class ShaderLab {
    public static final String MOD_ID = "shaderlab";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final Identifier LUSH_GRADE =
            Identifier.fromNamespaceAndPath(MOD_ID, "lush_grade");

    private static boolean attemptedInCurrentWorld;

    public ShaderLab(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(ShaderLab::onClientTick);
        LOGGER.info("Shader Lab {} client entrypoint loaded", LUSH_GRADE);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            attemptedInCurrentWorld = false;
            return;
        }

        if (attemptedInCurrentWorld) {
            return;
        }
        attemptedInCurrentWorld = true;

        Identifier activeEffect = minecraft.gameRenderer.currentPostEffect();
        if (activeEffect != null && !LUSH_GRADE.equals(activeEffect)) {
            LOGGER.info(
                    "Shader Lab left the existing post effect active instead of replacing it: {}",
                    activeEffect
            );
            return;
        }

        try {
            minecraft.gameRenderer.setPostEffect(LUSH_GRADE);
            LOGGER.info("Shader Lab applied post effect {}", LUSH_GRADE);
        } catch (RuntimeException exception) {
            LOGGER.error("Shader Lab could not apply post effect {}", LUSH_GRADE, exception);
            if (LUSH_GRADE.equals(minecraft.gameRenderer.currentPostEffect())) {
                minecraft.gameRenderer.clearPostEffect();
            }
        }
    }
}

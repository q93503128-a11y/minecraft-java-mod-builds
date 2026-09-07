package kr.moonseungjun.turnboundre.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.client.BattlePresentationModel;
import kr.moonseungjun.turnboundre.client.ui.BattleCommandScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Configurable in-game entry point for the production battle command screen. */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class BattleInputHandler {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "battle"));

    private static final KeyMapping OPEN_COMMANDS = new KeyMapping(
            "key.turnbound_re.open_battle_commands",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY);

    private BattleInputHandler() {}

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_COMMANDS);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_COMMANDS.consumeClick()) {
            BattlePresentationModel model = BattleClientState.presentation().orElse(null);
            if (model == null || !model.awaitingPlayerCommand()) continue;
            Minecraft.getInstance().gui.setScreen(new BattleCommandScreen());
        }
    }

    public static Component openKeyName() {
        return OPEN_COMMANDS.getTranslatedKeyMessage();
    }
}

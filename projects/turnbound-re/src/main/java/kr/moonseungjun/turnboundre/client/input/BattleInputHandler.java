package kr.moonseungjun.turnboundre.client.input;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.BattleActionTimelineState;
import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.client.BattlePresentationModel;
import kr.moonseungjun.turnboundre.client.ui.BattleCommandScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.UUID;

/** Opens the command picker automatically on each new authoritative player turn. No TURNBOUND combat key is registered. */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class BattleInputHandler {
    private static UUID lastOpenedBattleId;
    private static long lastOpenedRevision = Long.MIN_VALUE;

    private BattleInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        BattlePresentationModel model = BattleClientState.presentation().orElse(null);
        if (model == null) {
            lastOpenedBattleId = null;
            lastOpenedRevision = Long.MIN_VALUE;
            return;
        }
        if (!model.awaitingPlayerCommand()) return;
        if (BattleActionTimelineState.isPlaying(model.battleId())) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null) return;
        if (model.battleId().equals(lastOpenedBattleId) && model.revision() == lastOpenedRevision) return;

        lastOpenedBattleId = model.battleId();
        lastOpenedRevision = model.revision();
        minecraft.gui.setScreen(new BattleCommandScreen());
    }

    public static Component openKeyName() {
        return Component.translatable("hud.turnbound_re.input.auto");
    }
}

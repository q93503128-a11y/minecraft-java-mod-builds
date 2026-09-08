package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.BattleResultClientState;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.UUID;

/** Opens one result screen per server-authored terminal result. */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class BattleResultClientEvents {
    private static UUID openedBattleId;

    private BattleResultClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var result = BattleResultClientState.result().orElse(null);
        if (result == null) {
            openedBattleId = null;
            return;
        }
        if (result.battleId().equals(openedBattleId)) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        openedBattleId = result.battleId();
        minecraft.gui.setScreen(new BattleResultScreen());
    }
}

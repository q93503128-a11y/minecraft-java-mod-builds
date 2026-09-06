package kr.moonseungjun.turnboundre.debug;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * DEBUG_ONLY diagnostic HUD for M2 playtesting. It is intentionally plain text and must never be
 * treated as, styled into, or promoted to the production battle HUD.
 */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class DebugBattleHud {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "debug_battle_state");

    private DebugBattleHud() {}

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, DebugBattleHud::render);
    }

    private static void render(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        BattleNetworkPayloads.DecodedSnapshot snapshot = DebugBattleClientState.latestSnapshot().orElse(null);
        if (snapshot == null) return;

        var font = Minecraft.getInstance().font;
        int x = 4;
        int y = 4;
        graphics.text(font, Component.literal("DEBUG_ONLY — NOT FINAL UI"), x, y, 0xFFFFFFFF, true);
        y += font.lineHeight + 1;
        graphics.text(font, Component.literal(
                "battle=" + shortId(snapshot.battleId().toString())
                        + " rev=" + snapshot.revision()
                        + " state=" + snapshot.state()
                        + " cycle=" + snapshot.cycle()), x, y, 0xFFFFFFFF, true);
        y += font.lineHeight + 1;
        graphics.text(font, Component.literal("actor=" + snapshot.currentActorId()), x, y, 0xFFFFFFFF, true);
        y += font.lineHeight + 1;

        for (BattleNetworkPayloads.SnapshotParticipant participant : snapshot.participants()) {
            graphics.text(font, Component.literal(
                    participant.id()
                            + " HP " + participant.hp() + "/" + participant.maxHp()
                            + " P " + participant.poise() + "/" + participant.poiseMax()
                            + " E " + participant.energy()
                            + (participant.exposed() ? " EXPOSED" : "")
                            + (participant.guard() ? " GUARD" : "")
                            + (participant.poiseGuard() ? " PG" : "")), x, y, 0xFFFFFFFF, true);
            y += font.lineHeight + 1;
        }
    }

    private static String shortId(String id) {
        return id.length() <= 8 ? id : id.substring(0, 8);
    }
}

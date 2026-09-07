package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.client.BattlePresentationModel;
import kr.moonseungjun.turnboundre.client.BattleTargetMarkerState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.TriState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * World-first target feedback for the command picker.
 * This changes only client presentation; entity state and target legality remain server-authoritative.
 */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class BattleWorldTargetMarker {
    private BattleWorldTargetMarker() {}

    @SubscribeEvent
    public static void markTarget(RenderNameTagEvent.CanRender event) {
        BattlePresentationModel model = BattleClientState.presentation().orElse(null);
        if (model == null || !model.awaitingPlayerCommand()) return;

        BattleTargetMarkerState.MarkerKind marker = BattleTargetMarkerState.markerFor(
                model.battleId(), model.revision(), event.getEntity().getUUID()).orElse(null);
        if (marker == null) return;
        int ordinal = BattleTargetMarkerState.markerOrdinalFor(
                model.battleId(), model.revision(), event.getEntity().getUUID()).orElse(0);

        Component original = event.getOriginalContent();
        Component base = original != null ? original.copy() : event.getEntity().getDisplayName().copy();
        Component label = switch (marker) {
            case ELIGIBLE -> Component.translatable("marker.turnbound_re.target.eligible")
                    .withStyle(ChatFormatting.YELLOW);
            case HOVERED -> Component.translatable("marker.turnbound_re.target.hovered")
                    .withStyle(ChatFormatting.AQUA);
            case SELECTED -> Component.translatable("marker.turnbound_re.target.selected")
                    .withStyle(ChatFormatting.GREEN);
        };
        Component number = ordinal > 0
                ? Component.literal("#" + ordinal + " ").withStyle(ChatFormatting.WHITE)
                : Component.empty();

        event.setContent(Component.empty()
                .append(base)
                .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                .append(number)
                .append(label));
        event.setCanRender(TriState.TRUE);
    }

    @SubscribeEvent
    public static void clearWhenCommandScreenCloses(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof BattleCommandScreen) {
            BattleTargetMarkerState.clear();
        }
    }
}

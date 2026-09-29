package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.FieldCommandPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;

/** Physical stable/waystation UI. Fast travel remains a world service rather than a global menu category. */
final class FacilityTravelScreen extends FacilityScreen {
    FacilityTravelScreen() {
        super("마구간", 0xFF6DC6FF, 350, 205, 300, 188);
    }

    @Override
    protected String subtitle() {
        return "직접 발견한 거점으로 이동";
    }

    @Override
    protected void buildFacility() {
        int y = bodyTop() + 6;
        int maxY = bodyBottom() - 4;
        for (var travel : ClientFieldState.snapshot().travels()) {
            if (!travel.unlocked()) continue;
            if (y + 18 > maxY) break;
            var button = new BattleHudButton(
                    left + 14, y, panelWidth - 28, 18,
                    Component.literal(travel.label() + (travel.current() ? " · 현재" : "")),
                    travel.current() ? 0xFF62D39A : 0xFF6DC6FF,
                    ignored -> {
                        ClientPacketDistributor.sendToServer(new FieldCommandPayload("TRAVEL|" + travel.id()));
                        onClose();
                    });
            button.active = !travel.current();
            addRenderableWidget(button);
            y += 21;
        }
    }

    @Override
    protected void renderFacility(@NotNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        inset(g, left + 10, bodyTop() + 1, panelWidth - 20, panelHeight - 62);
        if (ClientFieldState.snapshot().travels().stream().noneMatch(travel -> travel.unlocked())) {
            g.text(font, Component.literal("아직 발견한 이동 거점이 없습니다."),
                    left + 17, bodyTop() + 10, TurnboundUiTokens.TEXT_SECONDARY, false);
        }
    }
}

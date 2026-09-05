package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.EndgameBriefingPayload;
import io.github.q93503128.turnbound.network.GachaPresentationPayload;
import io.github.q93503128.turnbound.network.MetaSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Applies server-authored meta state and transient facility/presentation routing. */
public final class ClientMetaNetwork {
    private ClientMetaNetwork() {}

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(MetaSnapshotPayload.TYPE, ClientMetaNetwork::handle);
        event.register(EndgameBriefingPayload.TYPE, ClientMetaNetwork::handleBriefing);
        event.register(GachaPresentationPayload.TYPE, ClientMetaNetwork::handleGacha);
    }

    private static void handle(MetaSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            String raw = payload.snapshot();
            String hint = record(raw, "O|");
            String feedback = record(raw, "F|");
            if (!feedback.isBlank()) ClientUiFeedbackLayer.show(feedback);
            ClientSignatureTrialState.update(raw);
            ClientMetaState.update(raw);
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gui.screen() instanceof BattleScreen || minecraft.gui.screen() instanceof BattleResultScreen
                    || minecraft.gui.screen() instanceof GachaPresentationScreen) return;

            if (!hint.isBlank()) {
                FacilityUiAccess.applyHint(hint);
                if ("MARKET".equals(hint)) minecraft.gui.setScreen(new FacilityMarketScreen());
                else minecraft.gui.setScreen(new MetaMenuScreen(tab(hint)));
                return;
            }
            if (minecraft.gui.screen() instanceof FacilityMarketScreen market) market.refreshSnapshot();
            else if (minecraft.gui.screen() instanceof MetaMenuScreen screen) screen.refreshSnapshot();
            else if (!(minecraft.gui.screen() instanceof EndgameBriefingScreen)) {
                FacilityUiAccess.clear();
                minecraft.gui.setScreen(new MetaMenuScreen(MetaMenuScreen.Tab.PARTY));
            }
        });
    }

    private static void handleBriefing(EndgameBriefingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gui.screen() instanceof BattleScreen || minecraft.gui.screen() instanceof BattleResultScreen) return;
            boolean returnToSystemMenu = minecraft.gui.screen() instanceof MetaMenuScreen;
            minecraft.gui.setScreen(new EndgameBriefingScreen(EndgameBriefingScreen.decode(payload.briefing()), returnToSystemMenu));
        });
    }

    private static void handleGacha(GachaPresentationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gui.screen() instanceof BattleScreen || minecraft.gui.screen() instanceof BattleResultScreen) return;
            minecraft.gui.setScreen(new GachaPresentationScreen(GachaPresentationScreen.decode(payload.result())));
        });
    }

    private static String record(String snapshot, String prefix) {
        if (snapshot == null || snapshot.isBlank()) return "";
        for (String line : snapshot.split("\n")) if (line.startsWith(prefix) && line.length() > prefix.length()) return line.substring(prefix.length());
        return "";
    }

    private static MetaMenuScreen.Tab tab(String hint) {
        return switch (hint) {
            case "PARTY", "RELAY" -> MetaMenuScreen.Tab.PARTY;
            case "CHARACTERS", "MEMORIAL", "CLOCK", "BARRACKS" -> MetaMenuScreen.Tab.CHARACTERS;
            case "EQUIPMENT", "FORGE" -> MetaMenuScreen.Tab.EQUIPMENT;
            case "ARCHIVE" -> MetaMenuScreen.Tab.ARCHIVE;
            case "QUESTS" -> MetaMenuScreen.Tab.QUESTS;
            case "CODEX" -> MetaMenuScreen.Tab.CODEX;
            case "SYSTEM", "ENDGAME", "RIFT" -> MetaMenuScreen.Tab.SYSTEM;
            default -> MetaMenuScreen.Tab.PARTY;
        };
    }
}

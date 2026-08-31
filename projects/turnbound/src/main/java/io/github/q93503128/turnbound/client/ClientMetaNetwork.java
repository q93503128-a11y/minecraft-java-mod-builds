package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.MetaSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Applies server-authored meta state and transient facility tab routing. */
public final class ClientMetaNetwork {
    private ClientMetaNetwork() {}

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(MetaSnapshotPayload.TYPE, ClientMetaNetwork::handle);
    }

    private static void handle(MetaSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            String hint = openHint(payload.snapshot());
            ClientMetaState.update(payload.snapshot());
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gui.screen() instanceof BattleScreen || minecraft.gui.screen() instanceof BattleResultScreen) return;

            if (!hint.isBlank()) {
                minecraft.gui.setScreen(new MetaMenuScreen(tab(hint)));
                return;
            }
            if (minecraft.gui.screen() instanceof MetaMenuScreen screen) {
                screen.refreshSnapshot();
            } else {
                minecraft.gui.setScreen(new MetaMenuScreen(MetaMenuScreen.Tab.PARTY));
            }
        });
    }

    private static String openHint(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) return "";
        int end = snapshot.indexOf('\n');
        String first = end < 0 ? snapshot : snapshot.substring(0, end);
        return first.startsWith("O|") && first.length() > 2 ? first.substring(2) : "";
    }

    private static MetaMenuScreen.Tab tab(String hint) {
        return switch (hint) {
            case "PARTY", "RELAY" -> MetaMenuScreen.Tab.PARTY;
            case "CHARACTERS", "MEMORIAL", "CLOCK", "BARRACKS" -> MetaMenuScreen.Tab.CHARACTERS;
            case "EQUIPMENT", "MARKET", "FORGE" -> MetaMenuScreen.Tab.EQUIPMENT;
            case "ARCHIVE" -> MetaMenuScreen.Tab.ARCHIVE;
            case "QUESTS" -> MetaMenuScreen.Tab.QUESTS;
            case "CODEX" -> MetaMenuScreen.Tab.CODEX;
            case "SYSTEM", "ENDGAME", "RIFT" -> MetaMenuScreen.Tab.SYSTEM;
            default -> MetaMenuScreen.Tab.PARTY;
        };
    }
}

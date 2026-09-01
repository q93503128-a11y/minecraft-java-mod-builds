package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.EndgameEncounterCatalog;
import io.github.q93503128.turnbound.network.EndgameBriefingPayload;
import io.github.q93503128.turnbound.network.GachaPresentationPayload;
import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.network.MetaSnapshotPayload;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Server-to-client authority for the v0.4 management menu and post-story briefing surfaces. */
public final class MetaNetwork {
    public static final String PROTOCOL = "turnbound-meta-v04";
    private MetaNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(MetaSnapshotPayload.TYPE, MetaSnapshotPayload.STREAM_CODEC);
        registrar.playToClient(EndgameBriefingPayload.TYPE, EndgameBriefingPayload.STREAM_CODEC);
        registrar.playToClient(GachaPresentationPayload.TYPE, GachaPresentationPayload.STREAM_CODEC);
        registrar.playToServer(MetaCommandPayload.TYPE, MetaCommandPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    String raw = payload.command();
                    String effective = raw;
                    if (raw != null && raw.startsWith("DEPLOY|")) effective = "START|" + raw.substring("DEPLOY|".length());

                    String denial = MetaActionGate.denial(player.getUUID(), effective);
                    if (!denial.isBlank()) {
                        player.sendSystemMessage(Component.literal("TURNBOUND · " + denial));
                        sync(player);
                        return;
                    }

                    if (raw != null && raw.startsWith("START|") && !BattleSessionManager.exists(player)) {
                        String encounterId = raw.substring("START|".length());
                        if (EndgameEncounterCatalog.contains(encounterId) && EndgameEncounterCatalog.unlocked(player.getUUID(), encounterId)) {
                            EndgameBriefing.send(player, EndgameBriefing.build(player.getUUID(), encounterId));
                            return;
                        }
                    }
                    MetaMenuService.command(player, effective);
                }));
    }

    public static void sync(ServerPlayer player) {
        send(player, "");
    }

    /**
     * Opens the management menu on the facility-relevant tab without adding a second wire payload.
     * ClientMetaState safely ignores the O record while ClientMetaNetwork consumes it as transient UI routing.
     */
    public static void open(ServerPlayer player, String tabHint) {
        send(player, tabHint == null ? "" : tabHint.trim());
    }

    private static void send(ServerPlayer player, String tabHint) {
        String encoded = QuestMenuContentService.encode(player.getUUID()) + MetaUiCodec.encode(MetaMenuService.snapshot(player));
        if (!tabHint.isBlank()) encoded = "O|" + tabHint + "\n" + encoded;
        PacketDistributor.sendToPlayer(player, new MetaSnapshotPayload(encoded));
    }
}

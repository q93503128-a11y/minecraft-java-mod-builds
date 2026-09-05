package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.network.EndgameBriefingPayload;
import io.github.q93503128.turnbound.network.GachaPresentationPayload;
import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.network.MetaSnapshotPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-to-client authority for the v0.4 management menu and presentation surfaces. */
public final class MetaNetwork {
    public static final String PROTOCOL = "turnbound-meta-v04";
    private static final Map<UUID, String> FEEDBACK = new ConcurrentHashMap<>();
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
                    if (raw == null || raw.isBlank()) return;

                    if (raw.startsWith("DEPLOY|")) {
                        EndgameDeploymentService.deploy(player, raw.substring("DEPLOY|".length()));
                        return;
                    }

                    String denial = MetaActionGate.denial(player.getUUID(), raw);
                    if (denial.isBlank()) denial = MetaFacilityActionGate.denial(player, raw);
                    if (!denial.isBlank()) {
                        feedback(player, denial);
                        sync(player);
                        return;
                    }

                    if (GachaPresentationService.handle(player, raw)) return;
                    if (raw.startsWith("START|")) {
                        EndgameDeploymentService.brief(player, raw.substring("START|".length()));
                        return;
                    }
                    MetaMenuService.command(player, raw);
                    // Meta mutations can complete quests (notably the first-party tutorial). Keep the field guide in lockstep.
                    RadiaHubSessionManager.refreshProgress(player);
                }));
    }

    public static void feedback(ServerPlayer player, String text) {
        if (player == null || text == null || text.isBlank()) return;
        FEEDBACK.put(player.getUUID(), sanitize(text));
    }

    public static void sync(ServerPlayer player) { send(player, ""); }

    public static void open(ServerPlayer player, String tabHint) {
        send(player, tabHint == null ? "" : tabHint.trim());
    }

    private static void send(ServerPlayer player, String tabHint) {
        String encoded = QuestMenuContentService.encode(player.getUUID())
                + SignatureTrialMenuContentService.encode(player.getUUID())
                + MetaUiCodec.encode(MetaMenuService.snapshot(player));
        String feedback = FEEDBACK.remove(player.getUUID());
        if (feedback != null && !feedback.isBlank()) encoded = "F|" + feedback + "\n" + encoded;
        if (!tabHint.isBlank()) encoded = "O|" + sanitize(tabHint) + "\n" + encoded;
        PacketDistributor.sendToPlayer(player, new MetaSnapshotPayload(encoded));
    }

    private static String sanitize(String value) {
        return value.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }
}

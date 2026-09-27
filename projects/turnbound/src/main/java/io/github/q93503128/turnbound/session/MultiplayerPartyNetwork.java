package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.network.PartySnapshotPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/** Server-authoritative player-party projection. */
public final class MultiplayerPartyNetwork {
    public static final String PROTOCOL = "turnbound-party-v1";

    private MultiplayerPartyNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(PartySnapshotPayload.TYPE, PartySnapshotPayload.STREAM_CODEC);
    }

    public static void tick(ServerPlayer viewer) {
        if (viewer == null || viewer.tickCount % 10 != 0) return;
        sync(viewer);
    }

    public static void sync(ServerPlayer viewer) {
        if (viewer == null) return;
        var server = viewer.level().getServer();
        if (server == null) return;
        MultiplayerPartyService.Snapshot party = MultiplayerPartyService.snapshot(viewer.getUUID());

        StringBuilder out = new StringBuilder();
        out.append("H|").append(party.leader() == null ? "" : party.leader())
                .append('|').append(party.members().size()).append('\n');
        for (UUID id : party.members()) {
            if (id.equals(viewer.getUUID())) continue;
            ServerPlayer member = server.getPlayerList().getPlayer(id);
            boolean online = member != null;
            boolean sameLevel = online && member.level() == viewer.level();
            boolean battle = online && BattleSessionManager.exists(member);
            String name = online ? member.getName().getString() : id.toString().substring(0, 8);
            out.append("M|").append(id).append('|').append(text(name)).append('|')
                    .append(id.equals(party.leader()) ? '1' : '0').append('|')
                    .append(online ? '1' : '0').append('|')
                    .append(sameLevel ? '1' : '0').append('|')
                    .append(battle ? '1' : '0').append('|');
            if (sameLevel) {
                out.append(member.getX()).append('|').append(member.getY()).append('|').append(member.getZ());
            } else {
                out.append("0|0|0");
            }
            out.append('\n');
        }
        PacketDistributor.sendToPlayer(viewer, new PartySnapshotPayload(out.toString()));
    }

    private static String text(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }
}

package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.network.PartyCommandPayload;
import io.github.q93503128.turnbound.network.PartySnapshotPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative player-party commands and read-only client projection. */
public final class MultiplayerPartyNetwork {
    public static final String PROTOCOL = "turnbound-party-v1";
    private static final java.util.Map<UUID, String> FEEDBACK = new ConcurrentHashMap<>();

    private MultiplayerPartyNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(PartySnapshotPayload.TYPE, PartySnapshotPayload.STREAM_CODEC);
        registrar.playToServer(PartyCommandPayload.TYPE, PartyCommandPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    command(player, payload.command());
                }));
    }

    public static void tick(ServerPlayer viewer) {
        if (viewer == null || viewer.tickCount % 10 != 0) return;
        sync(viewer);
    }

    public static void sync(ServerPlayer viewer) {
        if (viewer == null) return;
        MinecraftServer server = viewer.level().getServer();
        if (server == null) return;
        MultiplayerPartyService.Snapshot party = MultiplayerPartyService.snapshot(viewer.getUUID());

        StringBuilder out = new StringBuilder();
        out.append("H|").append(party.leader() == null ? "" : party.leader())
                .append('|').append(party.members().size())
                .append('|').append(MultiplayerPartyService.MAX_PLAYERS).append('\n');

        for (UUID id : party.members()) {
            ServerPlayer member = server.getPlayerList().getPlayer(id);
            boolean online = member != null;
            boolean sameLevel = online && member.level() == viewer.level();
            boolean battle = BattleSessionManager.exists(id);
            String name = online ? member.getName().getString() : id.toString().substring(0, 8);
            out.append("M|").append(id).append('|').append(text(name)).append('|')
                    .append(id.equals(party.leader()) ? '1' : '0').append('|')
                    .append(online ? '1' : '0').append('|')
                    .append(sameLevel ? '1' : '0').append('|')
                    .append(battle ? '1' : '0').append('|')
                    .append(id.equals(viewer.getUUID()) ? '1' : '0').append('|');
            if (sameLevel) out.append(member.getX()).append('|').append(member.getY()).append('|').append(member.getZ());
            else out.append("0|0|0");
            out.append('\n');
        }

        UUID inviterId = MultiplayerPartyService.pendingInviter(viewer.getUUID());
        if (inviterId != null) {
            ServerPlayer inviter = server.getPlayerList().getPlayer(inviterId);
            String inviterName = inviter == null ? inviterId.toString().substring(0, 8) : inviter.getName().getString();
            out.append("I|").append(inviterId).append('|').append(text(inviterName)).append('\n');
        }

        boolean viewerCanInvite = !BattleSessionManager.exists(viewer)
                && viewer.getUUID().equals(party.leader())
                && party.members().size() < MultiplayerPartyService.MAX_PLAYERS;
        if (viewerCanInvite) {
            for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
                UUID id = candidate.getUUID();
                if (id.equals(viewer.getUUID()) || party.members().contains(id)) continue;
                if (candidate.level() != viewer.level() || MultiplayerPartyService.isGrouped(id)) continue;
                out.append("C|").append(id).append('|').append(text(candidate.getName().getString())).append('|')
                        .append(BattleSessionManager.exists(id) ? '1' : '0').append('|')
                        .append(candidate.getX()).append('|').append(candidate.getY()).append('|').append(candidate.getZ()).append('\n');
            }
        }

        String feedback = FEEDBACK.get(viewer.getUUID());
        if (feedback != null && !feedback.isBlank()) out.append("F|").append(text(feedback)).append('\n');
        PacketDistributor.sendToPlayer(viewer, new PartySnapshotPayload(out.toString()));
    }

    private static void command(ServerPlayer player, String raw) {
        if (raw == null || raw.isBlank()) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        String[] parts = raw.split("\\|", -1);

        Set<UUID> affected = new LinkedHashSet<>(MultiplayerPartyService.snapshot(player.getUUID()).members());
        affected.add(player.getUUID());
        try {
            switch (parts[0]) {
                case "SYNC" -> { }
                case "INVITE" -> {
                    if (parts.length < 2) throw new IllegalArgumentException("초대할 플레이어를 선택해 주세요.");
                    UUID targetId = UUID.fromString(parts[1]);
                    ServerPlayer target = server.getPlayerList().getPlayer(targetId);
                    if (target == null || target.level() != player.level()) {
                        throw new IllegalStateException("같은 차원의 온라인 플레이어만 초대할 수 있습니다.");
                    }
                    MultiplayerPartyService.invite(player, target);
                    FEEDBACK.put(player.getUUID(), target.getName().getString() + "에게 초대를 보냈습니다.");
                    FEEDBACK.put(targetId, player.getName().getString() + "의 협동 파티 초대가 도착했습니다.");
                    affected.add(targetId);
                }
                case "ACCEPT" -> {
                    UUID inviter = MultiplayerPartyService.pendingInviter(player.getUUID());
                    MultiplayerPartyService.Snapshot joined = MultiplayerPartyService.accept(player);
                    FEEDBACK.put(player.getUUID(), "협동 파티에 참가했습니다.");
                    affected.addAll(joined.members());
                    if (inviter != null) {
                        affected.add(inviter);
                        FEEDBACK.put(inviter, player.getName().getString() + "이(가) 파티에 참가했습니다.");
                    }
                }
                case "DECLINE" -> {
                    UUID inviter = MultiplayerPartyService.pendingInviter(player.getUUID());
                    MultiplayerPartyService.decline(player);
                    FEEDBACK.put(player.getUUID(), "파티 초대를 거절했습니다.");
                    if (inviter != null) affected.add(inviter);
                }
                case "LEAVE" -> {
                    MultiplayerPartyService.Snapshot before = MultiplayerPartyService.snapshot(player.getUUID());
                    affected.addAll(before.members());
                    MultiplayerPartyService.leave(player);
                    FEEDBACK.put(player.getUUID(), "협동 파티에서 나왔습니다.");
                }
                case "KICK" -> {
                    if (parts.length < 2) throw new IllegalArgumentException("내보낼 파티원을 선택해 주세요.");
                    UUID memberId = UUID.fromString(parts[1]);
                    affected.add(memberId);
                    MultiplayerPartyService.kick(player, memberId);
                    FEEDBACK.put(player.getUUID(), "파티원을 내보냈습니다.");
                    FEEDBACK.put(memberId, "협동 파티에서 제외되었습니다.");
                }
                default -> throw new IllegalArgumentException("지원하지 않는 파티 조작입니다.");
            }
        } catch (RuntimeException ex) {
            String message = ex.getMessage();
            FEEDBACK.put(player.getUUID(), message == null || message.isBlank() ? "파티 조작을 완료하지 못했습니다." : message);
        }

        affected.addAll(MultiplayerPartyService.snapshot(player.getUUID()).members());
        for (UUID id : affected) {
            ServerPlayer target = server.getPlayerList().getPlayer(id);
            if (target != null) sync(target);
        }
        player.sendSystemMessage(Component.literal(FEEDBACK.getOrDefault(player.getUUID(), "파티 상태를 갱신했습니다.")));
    }

    private static String text(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }
}

package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.SharedBattleRules;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative social player party, distinct from each player's four-character combat party.
 */
public final class MultiplayerPartyService {
    public static final int MAX_PLAYERS = SharedBattleRules.MAX_PLAYERS;

    public record Snapshot(UUID leader, List<UUID> members) {
        public Snapshot { members = List.copyOf(members); }
    }

    private static final Map<UUID, Group> GROUP_BY_MEMBER = new LinkedHashMap<>();
    private static final Map<UUID, UUID> PENDING_INVITE = new LinkedHashMap<>();

    private MultiplayerPartyService() {}

    public static void invite(ServerPlayer inviter, ServerPlayer target) {
        if (inviter == null || target == null) throw new IllegalArgumentException("플레이어를 찾을 수 없습니다.");
        UUID from = inviter.getUUID(), to = target.getUUID();
        if (from.equals(to)) throw new IllegalStateException("자기 자신은 초대할 수 없습니다.");
        if (BattleSessionManager.exists(inviter) || BattleSessionManager.exists(target)) {
            throw new IllegalStateException("전투 중에는 플레이어 파티를 변경할 수 없습니다.");
        }
        Group group = GROUP_BY_MEMBER.get(from);
        if (group != null && !group.leader.equals(from)) throw new IllegalStateException("파티장만 초대할 수 있습니다.");
        if (group != null && group.members.size() >= MAX_PLAYERS) throw new IllegalStateException("플레이어 파티가 가득 찼습니다.");
        if (GROUP_BY_MEMBER.containsKey(to)) throw new IllegalStateException("대상이 이미 플레이어 파티에 속해 있습니다.");
        PENDING_INVITE.put(to, from);
    }

    public static Snapshot accept(ServerPlayer target) {
        if (target == null) throw new IllegalArgumentException("플레이어를 찾을 수 없습니다.");
        UUID to = target.getUUID();
        UUID from = PENDING_INVITE.remove(to);
        if (from == null) throw new IllegalStateException("받은 파티 초대가 없습니다.");
        if (BattleSessionManager.exists(target)) throw new IllegalStateException("전투 중에는 플레이어 파티를 변경할 수 없습니다.");

        Group group = GROUP_BY_MEMBER.get(from);
        if (group == null) {
            group = new Group(from);
            add(group, from);
        }
        if (!group.leader.equals(from)) throw new IllegalStateException("초대한 플레이어가 더 이상 파티장이 아닙니다.");
        if (group.members.size() >= MAX_PLAYERS) throw new IllegalStateException("플레이어 파티가 가득 찼습니다.");
        if (GROUP_BY_MEMBER.containsKey(to)) throw new IllegalStateException("이미 플레이어 파티에 속해 있습니다.");
        add(group, to);
        return snapshot(to);
    }

    public static void decline(ServerPlayer target) {
        if (target != null) PENDING_INVITE.remove(target.getUUID());
    }

    public static void leave(ServerPlayer player) {
        if (player != null) removeMember(player.getUUID());
    }

    /** Transient logout keeps social-party membership so a same-server reconnect can rejoin a shared battle. */
    public static void disconnect(ServerPlayer player) {
        if (player == null) return;
        UUID id = player.getUUID();
        PENDING_INVITE.remove(id);
        PENDING_INVITE.entrySet().removeIf(entry -> entry.getValue().equals(id));
    }

    public static void remove(ServerPlayer player) {
        if (player == null) return;
        UUID id = player.getUUID();
        removeMember(id);
        PENDING_INVITE.remove(id);
        PENDING_INVITE.entrySet().removeIf(entry -> entry.getValue().equals(id));
    }

    public static boolean sameParty(UUID a, UUID b) {
        if (a == null || b == null) return false;
        Group group = GROUP_BY_MEMBER.get(a);
        return group != null && group == GROUP_BY_MEMBER.get(b);
    }

    public static Snapshot snapshot(UUID playerId) {
        Group group = GROUP_BY_MEMBER.get(playerId);
        if (group == null) return new Snapshot(playerId, playerId == null ? List.of() : List.of(playerId));
        return new Snapshot(group.leader, new ArrayList<>(group.members));
    }

    public static void clear() {
        GROUP_BY_MEMBER.clear();
        PENDING_INVITE.clear();
    }

    private static void removeMember(UUID playerId) {
        Group group = GROUP_BY_MEMBER.remove(playerId);
        if (group == null) return;
        group.members.remove(playerId);
        if (group.members.isEmpty()) return;
        if (group.leader.equals(playerId)) group.leader = group.members.iterator().next();
        for (UUID member : group.members) GROUP_BY_MEMBER.put(member, group);
    }

    private static void add(Group group, UUID member) {
        if (group.members.add(member)) GROUP_BY_MEMBER.put(member, group);
    }

    private static final class Group {
        private UUID leader;
        private final Set<UUID> members = new LinkedHashSet<>();
        private Group(UUID leader) { this.leader = leader; }
    }
}

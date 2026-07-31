package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class VillageCouncilState {
    private static final Map<UUID, VillageRole> ROLES = new LinkedHashMap<>();

    private static VillageSavedData savedData;
    private static UUID mayorId;
    private static String mayorName = "없음";
    private static int villageDay = 1;
    private static VillageTimePhase timePhase = VillageTimePhase.MORNING;
    private static Proposal activeProposal;

    private VillageCouncilState() {
    }

    public static synchronized void initializeServer(MinecraftServer server) {
        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageSavedData.TYPE);

        ROLES.clear();
        ROLES.putAll(savedData.roles());
        mayorId = savedData.mayorId().orElse(null);
        mayorName = mayorId == null ? "없음" : savedData.mayorName();
        villageDay = savedData.villageDay();
        timePhase = savedData.timePhase();
        activeProposal = null;
        freezeAndApplyTime(server);
    }

    public static synchronized void registerPlayer(ServerPlayer player) {
        if (mayorId == null) {
            mayorId = player.getUUID();
            mayorName = player.getGameProfile().name();
            persist();
            broadcast(player.getServer(), "§6" + mayorName + "§f 님이 첫 임시 촌장이 되었습니다.");
        }
    }

    public static synchronized boolean isMayor(ServerPlayer player) {
        return player.getUUID().equals(mayorId);
    }

    public static synchronized String chooseRole(ServerPlayer player, VillageRole role) {
        ROLES.put(player.getUUID(), role);
        persist();
        return player.getGameProfile().name() + "님의 역할이 " + role.displayName() + "(으)로 정해졌습니다.";
    }

    public static synchronized String transferMayor(ServerPlayer actor, ServerPlayer target) {
        if (!isMayor(actor)) {
            return "촌장만 촌장직을 넘길 수 있습니다.";
        }
        mayorId = target.getUUID();
        mayorName = target.getGameProfile().name();
        activeProposal = null;
        persist();
        broadcast(actor.getServer(), "§6촌장직이 " + mayorName + "§f 님에게 넘어갔습니다.");
        return "촌장직 이전 완료";
    }

    public static synchronized String proposeAdvanceTime(ServerPlayer proposer) {
        if (!isMayor(proposer)) {
            return "촌장만 마을 전체 안건을 발의할 수 있습니다.";
        }
        if (activeProposal != null) {
            return "이미 진행 중인 안건이 있습니다.";
        }

        activeProposal = new Proposal("advance_time", proposer.getUUID(), new LinkedHashMap<>());
        activeProposal.votes().put(proposer.getUUID(), true);
        broadcast(proposer.getServer(), "§e[마을 투표] §f다음 시간 단계로 진행할지 투표합니다. /vg vote yes 또는 /vg vote no");
        evaluateProposal(proposer.getServer());
        return "시간 진행 안건을 발의했습니다.";
    }

    public static synchronized String vote(ServerPlayer player, boolean yes) {
        if (activeProposal == null) {
            return "현재 진행 중인 안건이 없습니다.";
        }

        activeProposal.votes().put(player.getUUID(), yes);
        String result = player.getGameProfile().name() + "님이 " + (yes ? "찬성" : "반대") + "에 투표했습니다.";
        broadcast(player.getServer(), result);
        evaluateProposal(player.getServer());
        return result;
    }

    public static synchronized String status(MinecraftServer server, ServerPlayer viewer) {
        String viewerRole = ROLES.containsKey(viewer.getUUID())
                ? ROLES.get(viewer.getUUID()).displayName()
                : "미선택";
        String voteStatus = activeProposal == null
                ? "진행 중인 안건 없음"
                : activeProposal.id() + " / 찬성 " + countVotes(server, true)
                + " / 반대 " + countVotes(server, false)
                + " / 통과 기준 " + majority(server);

        return "§6[마을 현황] §f촌장: " + mayorName
                + " | 내 역할: " + viewerRole
                + " | 제 " + villageDay + "일 " + timePhase.koreanName()
                + " | " + voteStatus;
    }

    public static synchronized void enforceFrozenTime(MinecraftServer server) {
        freezeAndApplyTime(server);
    }

    private static void evaluateProposal(MinecraftServer server) {
        if (activeProposal == null) {
            return;
        }

        int required = majority(server);
        int yesVotes = countVotes(server, true);
        int noVotes = countVotes(server, false);

        if (yesVotes >= required) {
            String proposalId = activeProposal.id();
            activeProposal = null;
            if ("advance_time".equals(proposalId)) {
                advanceTime(server);
            }
            broadcast(server, "§a[투표 통과] §f마을 결정이 실행되었습니다.");
        } else if (noVotes >= required) {
            activeProposal = null;
            broadcast(server, "§c[투표 부결] §f안건이 취소되었습니다.");
        }
    }

    private static void advanceTime(MinecraftServer server) {
        VillageTimePhase previous = timePhase;
        timePhase = timePhase.next();
        if (previous == VillageTimePhase.NIGHT && timePhase == VillageTimePhase.MORNING) {
            villageDay++;
        }
        persist();
        freezeAndApplyTime(server);
        broadcast(server, "§b마을 시간이 제 " + villageDay + "일 " + timePhase.koreanName() + "(으)로 진행되었습니다.");
    }

    private static void persist() {
        if (savedData != null) {
            savedData.replaceState(mayorId, mayorName, villageDay, timePhase, ROLES);
        }
    }

    private static void freezeAndApplyTime(MinecraftServer server) {
        server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        server.overworld().setDayTime(timePhase.minecraftTime());
    }

    private static int majority(MinecraftServer server) {
        int online = Math.max(1, server.getPlayerList().getPlayerCount());
        return online / 2 + 1;
    }

    private static int countVotes(MinecraftServer server, boolean value) {
        if (activeProposal == null) {
            return 0;
        }
        return (int) activeProposal.votes().entrySet().stream()
                .filter(entry -> server.getPlayerList().getPlayer(entry.getKey()) != null)
                .filter(entry -> entry.getValue() == value)
                .count();
    }

    private static void broadcast(MinecraftServer server, String text) {
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(text), false);
        }
    }

    private record Proposal(String id, UUID proposer, Map<UUID, Boolean> votes) {
    }
}

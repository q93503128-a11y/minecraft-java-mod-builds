package kr.moonseungjun.anomalousclasses.state;

import kr.moonseungjun.anomalousclasses.network.ClassNetwork;
import kr.moonseungjun.anomalousclasses.network.payload.SkillTreeActionPayload;
import kr.moonseungjun.anomalousclasses.network.payload.SkillTreeStatePayload;
import net.minecraft.server.level.ServerPlayer;

public final class SkillTreeService {
    private SkillTreeService() {}

    public static void open(ServerPlayer player) {
        if (player.getServer() == null) return;
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        ClassId active = profile.currentClass().orElse(null);
        if (active == null) {
            ClassSelectionService.openMenu(player);
            return;
        }
        send(player, profile, active, SkillTreeStatePayload.Result.OPEN);
    }

    public static void handle(ServerPlayer player, SkillTreeActionPayload request) {
        if (player.getServer() == null) return;
        ClassProfileSavedData savedData = ClassProfileSavedData.get(player.getServer());
        PlayerClassProfile profile = savedData.profile(player.getUUID());
        ClassId active = profile.currentClass().orElse(null);
        ClassId requested = ClassId.fromNetworkIndex(request.classIndex()).orElse(null);
        if (active == null || requested == null || requested != active) {
            if (active != null) send(player, profile, active, SkillTreeStatePayload.Result.INVALID_CLASS);
            else ClassSelectionService.openMenu(player);
            return;
        }
        if (request.expectedRevision() != profile.revision()) {
            send(player, profile, active, SkillTreeStatePayload.Result.STALE_REQUEST);
            return;
        }

        ClassProgress progress = profile.progress(active);
        int[] ranks = normalize(active, progress.skillRanks());
        if (request.action() == SkillTreeActionPayload.Action.RESET.ordinal()) {
            if (CombatTracker.isLocked(player)) {
                send(player, profile, active, SkillTreeStatePayload.Result.COMBAT_LOCKED);
                return;
            }
            long refunded = (long) progress.skillPoints() + SkillTreeCatalog.spent(active, ranks);
            if (refunded > Integer.MAX_VALUE) {
                send(player, profile, active, SkillTreeStatePayload.Result.INVALID_ACTION);
                return;
            }
            profile.updateProgress(active, progress.withSkillTree((int) refunded, new int[ranks.length]));
            savedData.setDirty();
            send(player, profile, active, SkillTreeStatePayload.Result.RESET);
            return;
        }

        if (request.nodeIndex() < 0 || request.nodeIndex() >= ranks.length) {
            send(player, profile, active, SkillTreeStatePayload.Result.INVALID_NODE);
            return;
        }
        SkillTreeCatalog.Node node = SkillTreeCatalog.nodes(active).get(request.nodeIndex());
        int rank = ranks[request.nodeIndex()];
        if (rank >= node.maxRank()) {
            send(player, profile, active, SkillTreeStatePayload.Result.MAX_RANK);
            return;
        }
        int targetRank = switch (request.action()) {
            case 0 -> rank + 1;
            case 1 -> node.nextBreakpoint(rank);
            case 2 -> maxAffordableRank(node, rank, progress.skillPoints());
            default -> -1;
        };
        if (targetRank < 0) {
            send(player, profile, active, request.action() == SkillTreeActionPayload.Action.INVEST_TO_BREAKPOINT.ordinal()
                    ? SkillTreeStatePayload.Result.NO_BREAKPOINT : SkillTreeStatePayload.Result.INVALID_ACTION);
            return;
        }
        if (targetRank == rank) {
            send(player, profile, active, SkillTreeStatePayload.Result.INSUFFICIENT_SP);
            return;
        }
        int cost = node.costFromTo(rank, targetRank);
        if (cost <= 0 || cost > progress.skillPoints()) {
            send(player, profile, active, SkillTreeStatePayload.Result.INSUFFICIENT_SP);
            return;
        }
        ranks[request.nodeIndex()] = targetRank;
        profile.updateProgress(active, progress.withSkillTree(progress.skillPoints() - cost, ranks));
        savedData.setDirty();
        send(player, profile, active, SkillTreeStatePayload.Result.UPDATED);
    }

    private static int maxAffordableRank(SkillTreeCatalog.Node node, int rank, int available) {
        int next = rank;
        int spent = 0;
        int[] costs = node.rankCosts();
        while (next < costs.length && spent + costs[next] <= available) {
            spent += costs[next++];
        }
        return next;
    }

    private static int[] normalize(ClassId id, int[] existing) {
        int[] ranks = new int[SkillTreeCatalog.nodes(id).size()];
        for (int index = 0; index < Math.min(ranks.length, existing.length); index++) {
            ranks[index] = Math.min(SkillTreeCatalog.nodes(id).get(index).maxRank(), Math.max(0, existing[index]));
        }
        return ranks;
    }

    private static void send(ServerPlayer player, PlayerClassProfile profile, ClassId id,
                             SkillTreeStatePayload.Result result) {
        ClassNetwork.sendToPlayer(player, SkillTreeStatePayload.from(profile, id, CombatTracker.isLocked(player), result));
    }
}

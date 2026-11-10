package kr.moonseungjun.anomalousclasses.state;

import java.util.HashSet;
import java.util.Set;
import kr.moonseungjun.anomalousclasses.network.ClassNetwork;
import kr.moonseungjun.anomalousclasses.network.payload.SkillTreeActionPayload;
import kr.moonseungjun.anomalousclasses.network.payload.SkillTreeStatePayload;
import net.minecraft.server.level.ServerPlayer;

public final class SkillTreeService {
    private SkillTreeService() {}

    public static void open(ServerPlayer player) {
        if (player.getServer() == null) return;
        ClassProfileSavedData savedData = ClassProfileSavedData.get(player.getServer());
        PlayerClassProfile profile = savedData.profile(player.getUUID());
        if (!migrate(player, savedData, profile)) {
            ClassId active = profile.currentClass().orElse(null);
            if (active != null) send(player, profile, active, SkillTreeStatePayload.Result.MIGRATION_FAILED);
            return;
        }
        ClassId active = profile.currentClass().orElse(null);
        if (active == null) {
            ClassSelectionService.openMenu(player);
            return;
        }
        SkillTreeAttributeService.apply(player, active, profile.progress(active));
        send(player, profile, active, SkillTreeStatePayload.Result.OPEN);
    }

    public static void handle(ServerPlayer player, SkillTreeActionPayload request) {
        if (player.getServer() == null) return;
        ClassProfileSavedData savedData = ClassProfileSavedData.get(player.getServer());
        PlayerClassProfile profile = savedData.profile(player.getUUID());
        if (!migrate(player, savedData, profile)) {
            ClassId active = profile.currentClass().orElse(null);
            if (active != null) send(player, profile, active, SkillTreeStatePayload.Result.MIGRATION_FAILED);
            return;
        }
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
        if (request.action() == SkillTreeActionPayload.Action.RESET.ordinal()) {
            if (CombatTracker.isLocked(player)) {
                send(player, profile, active, SkillTreeStatePayload.Result.COMBAT_LOCKED);
                return;
            }
            long refund = (long) progress.skillPoints() + GraphSkillTreeCatalog.spent(active, progress.purchasedNodes());
            if (refund > Integer.MAX_VALUE) {
                send(player, profile, active, SkillTreeStatePayload.Result.INVALID_ACTION);
                return;
            }
            profile.updateProgress(active, progress.withGraphTree((int) refund, Set.of(), GraphSkillTreeCatalog.CURRENT_VERSION));
            savedData.setDirty();
            SkillTreeAttributeService.apply(player, active, profile.progress(active));
            send(player, profile, active, SkillTreeStatePayload.Result.RESET);
            return;
        }
        if (request.action() != SkillTreeActionPayload.Action.PURCHASE_NODE.ordinal()) {
            send(player, profile, active, SkillTreeStatePayload.Result.INVALID_ACTION);
            return;
        }

        GraphSkillTreeCatalog.Tree tree = GraphSkillTreeCatalog.tree(active);
        GraphSkillTreeCatalog.Node node = tree.purchaseNode(request.nodeIndex());
        if (node == null) {
            send(player, profile, active, SkillTreeStatePayload.Result.INVALID_NODE);
            return;
        }
        GraphSkillTreeCatalog.PurchaseCheck check = GraphSkillTreeCatalog.canPurchase(
                active, progress.purchasedNodes(), progress.skillPoints(), node.id());
        if (check != GraphSkillTreeCatalog.PurchaseCheck.PURCHASABLE) {
            send(player, profile, active, map(check));
            return;
        }

        Set<String> updatedNodes = new HashSet<>(progress.purchasedNodes());
        updatedNodes.add(node.id());
        profile.updateProgress(active, progress.withGraphTree(
                progress.skillPoints() - node.cost(), updatedNodes, GraphSkillTreeCatalog.CURRENT_VERSION));
        savedData.setDirty();
        SkillTreeAttributeService.apply(player, active, profile.progress(active));
        send(player, profile, active, SkillTreeStatePayload.Result.UPDATED);
    }

    private static SkillTreeStatePayload.Result map(GraphSkillTreeCatalog.PurchaseCheck check) {
        return switch (check) {
            case INSUFFICIENT_SP -> SkillTreeStatePayload.Result.INSUFFICIENT_SP;
            case ALREADY_OWNED -> SkillTreeStatePayload.Result.ALREADY_OWNED;
            case MISSING_REQUIREMENT -> SkillTreeStatePayload.Result.MISSING_REQUIREMENT;
            case NOT_CONNECTED -> SkillTreeStatePayload.Result.NOT_CONNECTED;
            case UNKNOWN_NODE, NOT_PURCHASABLE -> SkillTreeStatePayload.Result.INVALID_NODE;
            case PURCHASABLE -> SkillTreeStatePayload.Result.UPDATED;
        };
    }

    private static boolean migrate(ServerPlayer player, ClassProfileSavedData savedData, PlayerClassProfile profile) {
        try {
            if (GraphSkillTreeMigration.migrateAll(profile)) savedData.setDirty();
            return true;
        } catch (RuntimeException failure) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.anomalousclasses.skill_tree.migration_failed"));
            return false;
        }
    }

    private static void send(ServerPlayer player, PlayerClassProfile profile, ClassId id,
                             SkillTreeStatePayload.Result result) {
        ClassNetwork.sendToPlayer(player, SkillTreeStatePayload.from(profile, id, CombatTracker.isLocked(player), result));
    }
}

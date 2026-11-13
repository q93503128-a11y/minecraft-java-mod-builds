package kr.moonseungjun.anomalousclasses.state;

import kr.moonseungjun.anomalousclasses.network.ClassNetwork;
import kr.moonseungjun.anomalousclasses.network.payload.ClassSelectionStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.SelectClassPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

public final class ClassSelectionService {
    private ClassSelectionService() {}

    public static void onPlayerLogin(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        ClassProfileSavedData savedData = ClassProfileSavedData.get(player.getServer());
        PlayerClassProfile profile = savedData.profile(player.getUUID());
        if (migrate(player, savedData, profile)) {
            profile.currentClass().ifPresent(id -> SkillTreeAttributeService.apply(player, id, profile.progress(id)));
        }
        if (profile.currentClass().isEmpty()) send(player, profile, ClassSelectionStatePayload.Result.OPEN);
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        if (player.getServer() == null) return;
        ClassProfileSavedData savedData = ClassProfileSavedData.get(player.getServer());
        PlayerClassProfile profile = savedData.profile(player.getUUID());
        if (migrate(player, savedData, profile)) {
            profile.currentClass().ifPresent(id -> SkillTreeAttributeService.apply(player, id, profile.progress(id)));
        }
    }

    public static void openMenu(ServerPlayer player) {
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        send(player, profile, ClassSelectionStatePayload.Result.OPEN);
    }

    public static void sendStatus(ServerPlayer player) {
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        send(player, profile, ClassSelectionStatePayload.Result.STATUS);
    }

    public static void select(ServerPlayer player, SelectClassPayload request) {
        ClassProfileSavedData savedData = ClassProfileSavedData.get(player.getServer());
        PlayerClassProfile profile = savedData.profile(player.getUUID());
        if (!migrate(player, savedData, profile)) {
            send(player, profile, ClassSelectionStatePayload.Result.MIGRATION_FAILED);
            return;
        }
        ClassId.fromNetworkIndex(request.classIndex()).ifPresentOrElse(target -> {
            if (profile.currentClass().filter(target::equals).isPresent()) {
                send(player, profile, ClassSelectionStatePayload.Result.ALREADY_CURRENT);
                return;
            }
            if (CombatTracker.isLocked(player)) {
                send(player, profile, ClassSelectionStatePayload.Result.COMBAT_LOCKED);
                return;
            }
            if (request.expectedRevision() != profile.revision()) {
                send(player, profile, ClassSelectionStatePayload.Result.STALE_REQUEST);
                return;
            }

            ClassId previous = profile.currentClass().orElse(null);
            if (!MentalDominatorService.prepareClassChange(player, previous, target)) {
                send(player, profile, ClassSelectionStatePayload.Result.STALE_REQUEST);
                return;
            }

            profile.select(target);
            savedData.setDirty();
            SkillTreeAttributeService.apply(player, target, profile.progress(target));
            send(player, profile, ClassSelectionStatePayload.Result.SAVED);
        }, () -> send(player, profile, ClassSelectionStatePayload.Result.INVALID_CLASS));
    }

    private static void send(ServerPlayer player, PlayerClassProfile profile, ClassSelectionStatePayload.Result result) {
        ClassNetwork.sendToPlayer(player, ClassSelectionStatePayload.from(profile, CombatTracker.isLocked(player), result));
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
}

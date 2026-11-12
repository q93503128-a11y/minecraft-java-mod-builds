package kr.moonseungjun.anomalousclasses.state;

import kr.moonseungjun.anomalousclasses.network.ClassNetwork;
import kr.moonseungjun.anomalousclasses.network.payload.ClassManagementStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.ProfileStatePayload;
import net.minecraft.server.level.ServerPlayer;

public final class ProfileService {
    private ProfileService() {}

    public static void open(ServerPlayer player) {
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        profile.currentClass().ifPresentOrElse(id ->
                ClassNetwork.sendToPlayer(player, ProfileStatePayload.from(player, profile, id)),
                () -> ClassSelectionService.openMenu(player));
    }

    public static void openManagement(ServerPlayer player) {
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        profile.currentClass().ifPresentOrElse(id ->
                ClassNetwork.sendToPlayer(player, new ClassManagementStatePayload(
                        ProfileStatePayload.from(player, profile, id))),
                () -> ClassSelectionService.openMenu(player));
    }

    public static boolean grantLevelsForTesting(ServerPlayer player, int levels) {
        if (levels < 1 || levels > 100_000 || player.getServer() == null) return false;
        ClassProfileSavedData savedData = ClassProfileSavedData.get(player.getServer());
        PlayerClassProfile profile = savedData.profile(player.getUUID());
        ClassId active = profile.currentClass().orElse(null);
        if (active == null) return false;
        ClassProgress current = profile.progress(active);
        long nextLevel = (long) current.level() + levels;
        long nextPoints = (long) current.skillPoints() + (long) levels * 3L;
        if (nextLevel > Integer.MAX_VALUE || nextPoints > Integer.MAX_VALUE) return false;
        profile.updateProgress(active, current.withLevelAndExperience((int) nextLevel, current.experience(), (int) nextPoints));
        savedData.setDirty();
        open(player);
        return true;
    }
}

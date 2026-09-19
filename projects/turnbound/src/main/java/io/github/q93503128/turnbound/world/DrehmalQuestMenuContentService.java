package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;

/** Current first-route quest projection for the management-menu quest surface. */
final class DrehmalQuestMenuContentService {
    private DrehmalQuestMenuContentService() {}

    static String encode(ServerPlayer player) {
        if (player == null) return "";
        return encodeObjective(DrehmalFirstRouteRuntime.explorationSnapshot(player).objective());
    }

    static String encodeObjective(String objective) {
        String clean = objective == null ? "" : objective.replace('|', '/').replace('\n', ' ').replace('\r', ' ').trim();
        if (clean.isBlank()) return "";
        return "Q|New Drabyel로 가는 길|메인 · Capital Valley|1|0|" + clean + "\n";
    }
}

package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Resolves only this player's nearby tracked physical objective into client-local outline entity ids. */
final class QuestTargetGlowService {
    private static final double OUTLINE_RADIUS = 52.0D;
    private static final double OUTLINE_RADIUS_SQR = OUTLINE_RADIUS * OUTLINE_RADIUS;
    private static final String SERVICE_PREFIX = "turnbound_drabyel_service:";
    private static final String FIELD_NPC_PREFIX = "turnbound_drehmal_field_npc:";
    private static final String LOCAL_CLUE_PREFIX = "turnbound_drabyel_local_clue:";

    private QuestTargetGlowService() {}

    static String targetEntityIds(ServerPlayer player, FieldUiSnapshot snapshot) {
        if (player == null || snapshot == null || !snapshot.active() || player.isSpectator()
                || BattleSessionManager.exists(player) || !(player.level() instanceof ServerLevel level)) return "";
        FieldUiSnapshot.Navigation navigation = snapshot.navigation();
        if (navigation == null || !navigation.active()) return "";

        String target = navigation.id();
        List<Integer> ids = new ArrayList<>();
        for (Entity entity : level.getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(OUTLINE_RADIUS))) {
            if (entity == player || player.distanceToSqr(entity) > OUTLINE_RADIUS_SQR) continue;
            if (matches(target, entity)) ids.add(entity.getId());
        }
        Collections.sort(ids);
        if (ids.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        for (int id : ids) {
            if (result.length() > 0) result.append(',');
            result.append(id);
        }
        return result.toString();
    }

    private static boolean matches(String target, Entity entity) {
        if (target == null || target.isBlank() || entity == null) return false;
        for (String tag : entity.entityTags()) {
            if (tag.startsWith(SERVICE_PREFIX) && target.equals(tag.substring(SERVICE_PREFIX.length()))) return true;
            if (tag.startsWith(LOCAL_CLUE_PREFIX) && target.equals(tag.substring(LOCAL_CLUE_PREFIX.length()))) return true;
            if (!tag.startsWith(FIELD_NPC_PREFIX)) continue;
            String npcLocator = tag.substring(FIELD_NPC_PREFIX.length());
            if (target.equals(npcLocator)) return true;
            DrehmalFieldNpcCatalog.Npc npc = DrehmalFieldNpcCatalog.npc(npcLocator);
            if (npc != null && target.equals(npc.siteLocator())) return true;
        }
        return false;
    }

    static void clear() {}
}

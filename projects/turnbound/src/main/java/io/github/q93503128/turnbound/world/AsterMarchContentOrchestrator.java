package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** One place to build/sync/remove optional authored content layered over the main-story world sessions. */
public final class AsterMarchContentOrchestrator {
    private AsterMarchContentOrchestrator() {}

    public static void build(ServerLevel level) {
        AsterMarchAmbientDressing.build(level);
        AsterMarchRouteDensity.build(level);
        CharacterQuestWorldSites.build(level);
        RegionQuestWorldSites.build(level);
        ExplorationCodexSites.build(level);
        RadiaEndgameAtrium.build(level);
        SignatureTrialHall.build(level);
    }

    public static void tick(ServerLevel level, ServerPlayer player) {
        if (BattleSessionManager.exists(player) || player.tickCount % 10 != 0) return;
        AsterMarchBossAftermath.sync(level);
        AsterMarchApproachAtmosphere.tick(level, player);
        CharacterQuestWorldSites.sync(level, player);
        RegionQuestWorldSites.sync(level, player);
        ExplorationCodexSites.sync(level, player);
        RadiaEndgameAtrium.sync(level, player);
        SignatureTrialHall.sync(level, player);
    }

    public static boolean interact(ServerPlayer player, Entity entity) {
        return CharacterQuestWorldSites.interact(player, entity)
                || RegionQuestWorldSites.interact(player, entity)
                || ExplorationCodexSites.interact(player, entity)
                || RadiaEndgameAtrium.interact(player, entity)
                || SignatureTrialHall.interact(player, entity);
    }

    public static void remove(ServerPlayer player) {
        CharacterQuestWorldSites.remove(player);
        RegionQuestWorldSites.remove(player);
        ExplorationCodexSites.remove(player);
        RadiaEndgameAtrium.remove(player);
        SignatureTrialHall.remove(player);
    }
}

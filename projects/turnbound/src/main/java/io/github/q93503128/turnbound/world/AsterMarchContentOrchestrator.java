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
        if (BattleSessionManager.exists(player)) {
            AsterMarchStoryScenes.cancelForBattle(level, player);
            AsterMarchCharacterQuestPresentation.cancelForBattle(level, player);
            AsterMarchFieldIncidents.cancelForBattle(level, player);
            return;
        }
        // Story actors need normal tick cadence for readable speaker changes; the rest stays on the existing light cadence.
        AsterMarchStoryScenes.tick(level, player);
        if (player.tickCount % 10 != 0) return;
        AsterMarchBossAftermath.sync(level);
        AsterMarchApproachAtmosphere.tick(level, player);
        AsterMarchProgressStaging.tick(level, player);
        AsterMarchFieldIncidents.sync(level, player);
        AsterMarchFieldSequences.tick(level, player);
        AsterMarchPartyFieldBarks.tick(level, player);
        AsterMarchCharacterMilestones.tick(level, player);
        CharacterQuestWorldSites.sync(level, player);
        AsterMarchCharacterQuestPresentation.sync(level, player);
        RegionQuestWorldSites.sync(level, player);
        ExplorationCodexSites.sync(level, player);
        RadiaEndgameAtrium.sync(level, player);
        RadiaEndgamePresentation.tick(level, player);
        SignatureTrialHall.sync(level, player);
    }

    public static boolean interact(ServerPlayer player, Entity entity) {
        return AsterMarchFieldIncidents.interact(player, entity)
                || AsterMarchCharacterQuestPresentation.interact(player, entity)
                || CharacterQuestWorldSites.interact(player, entity)
                || RegionQuestWorldSites.interact(player, entity)
                || ExplorationCodexSites.interact(player, entity)
                || RadiaEndgameAtrium.interact(player, entity)
                || SignatureTrialHall.interact(player, entity);
    }

    public static void remove(ServerPlayer player) {
        AsterMarchStoryScenes.remove(player);
        AsterMarchProgressStaging.remove(player);
        AsterMarchFieldIncidents.remove(player);
        AsterMarchFieldSequences.remove(player);
        AsterMarchPartyFieldBarks.remove(player);
        AsterMarchCharacterMilestones.remove(player);
        AsterMarchCharacterQuestPresentation.remove(player);
        CharacterQuestWorldSites.remove(player);
        RegionQuestWorldSites.remove(player);
        ExplorationCodexSites.remove(player);
        RadiaEndgameAtrium.remove(player);
        RadiaEndgamePresentation.remove(player);
        SignatureTrialHall.remove(player);
    }
}

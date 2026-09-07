package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.persistence.RiftfrontierWorldData;
import net.minecraft.server.level.ServerLevel;

import java.util.Comparator;
import java.util.List;

/**
 * Server-start reconciliation for authoritative expedition state.
 *
 * M2 permits at most one non-terminal expedition in a healthy world, but persistence must not assume
 * that historical/migrated/corrupted state always satisfies the latest runtime invariant. A restart
 * therefore closes every persisted non-terminal run instead of only the newest one. This is bounded by
 * the persisted expedition list and runs once at server start; it does not scan entities, chunks or the
 * world each tick.
 */
public final class ExpeditionRestartReconciler {
    private ExpeditionRestartReconciler() {}

    /**
     * Pure selection boundary used by unit tests and the Minecraft adapter. Stable ascending sequence
     * order keeps reconciliation deterministic and makes logs/evidence easier to audit.
     */
    public static List<ExpeditionRun> nonTerminalRuns(List<ExpeditionRun> runs) {
        return List.copyOf(runs.stream()
            .filter(run -> !run.status().terminal())
            .sorted(Comparator.comparingLong(ExpeditionRun::sequence))
            .toList());
    }

    /**
     * Fails every persisted non-terminal run with SERVER_RESTART and records an honest terminal
     * checkpoint. liveThreats is unavailable because process-local ownership cannot survive restart.
     * Already-spent preparation supply is deliberately not refunded.
     */
    public static List<ExpeditionRun> reconcile(ServerLevel level) {
        RiftfrontierWorldData world = RiftfrontierWorldData.get(level);
        List<ExpeditionRun> candidates = nonTerminalRuns(world.expeditions());
        if (candidates.isEmpty()) return List.of();

        var lifecycle = new ExpeditionLifecycle(ContentRuntime.requireCurrent());
        long gameTime = level.getGameTime();

        return candidates.stream().map(run -> {
            ExpeditionRun failed = lifecycle.fail(run, gameTime, ExpeditionRun.EndReason.SERVER_RESTART);
            world.updateExpedition(failed);
            ExpeditionEvidenceCheckpoint terminalEvidence = new ExpeditionEvidenceCheckpoint(
                ExpeditionEvidenceCheckpoint.Stage.FAILED,
                gameTime,
                failed.recoveredResources().getOrDefault(ExpeditionGameplayService.RESOURCE_ID, 0),
                -1,
                world.securedRegion01Salvage(),
                world.expeditionSupply(),
                world.region01Pressure()
            );
            failed = world.updateExpedition(failed.appendEvidence(terminalEvidence));
            Region01EncounterRuntime.clearRun(
                level.getServer().overworld(),
                ExpeditionGameplayService.technicalRegionCenter(),
                failed.sequence()
            );
            return failed;
        }).toList();
    }
}

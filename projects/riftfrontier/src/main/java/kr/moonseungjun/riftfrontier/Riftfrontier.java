package kr.moonseungjun.riftfrontier;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.content.ContentServerReloadListener;
import kr.moonseungjun.riftfrontier.content.bootstrap.CoreContentBootstrap;
import kr.moonseungjun.riftfrontier.diagnostics.RuntimeDiagnosticsCommand;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionGameplayCommand;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionGameplayEvents;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionGameplayService;
import kr.moonseungjun.riftfrontier.gametest.RiftfrontierGameTests;
import kr.moonseungjun.riftfrontier.persistence.RiftfrontierWorldData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

@Mod(Riftfrontier.MOD_ID)
public final class Riftfrontier {
    public static final String MOD_ID = "riftfrontier";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Riftfrontier(IEventBus modEventBus, ModContainer modContainer) {
        var pack = CoreContentBootstrap.bootstrapAndValidate();
        var report = pack.validation();
        report.warnings().forEach(issue -> LOGGER.warn("[content] {} - {}", issue.source(), issue.message()));

        var snapshot = ContentRuntime.installValidated(pack.registry(), java.util.List.of(pack.packId()));
        LOGGER.info(
            "Riftfrontier core loaded: pack={}, schema={}, definitions={}, generation={}, catalog={}",
            pack.packId(), pack.schemaVersion(), report.definitionCount(), snapshot.generation(), snapshot.fingerprint()
        );

        RiftfrontierGameTests.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(Riftfrontier::addServerReloadListeners);
        NeoForge.EVENT_BUS.addListener(Riftfrontier::registerCommands);
        NeoForge.EVENT_BUS.addListener(Riftfrontier::serverStarted);
        NeoForge.EVENT_BUS.addListener(ExpeditionGameplayEvents::rightClickBlock);
        NeoForge.EVENT_BUS.addListener(ExpeditionGameplayEvents::entityJoinLevel);
        NeoForge.EVENT_BUS.addListener(ExpeditionGameplayEvents::playerClone);
        NeoForge.EVENT_BUS.addListener(ExpeditionGameplayEvents::playerLoggedOut);
    }

    private static void addServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(ContentServerReloadListener.ID, new ContentServerReloadListener());
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        RuntimeDiagnosticsCommand.register(event);
        ExpeditionGameplayCommand.register(event);
    }

    private static void serverStarted(ServerStartedEvent event) {
        var snapshot = ContentRuntime.requireCurrent();
        var worldData = RiftfrontierWorldData.get(event.getServer().overworld());
        boolean changed = worldData.synchronizeContentFingerprint(snapshot.fingerprint());
        var reconciled = ExpeditionGameplayService.reconcileAfterServerRestart(event.getServer().overworld());
        reconciled.ifPresent(run -> LOGGER.warn(
            "Riftfrontier restart reconciliation failed non-terminal expedition sequence={} status={} without refund; persisted proxy entities will be discarded when loaded",
            run.sequence(), run.status()
        ));
        LOGGER.info("Riftfrontier authoritative world root ready: changed={}, {}", changed, worldData.diagnosticSummary());
    }
}

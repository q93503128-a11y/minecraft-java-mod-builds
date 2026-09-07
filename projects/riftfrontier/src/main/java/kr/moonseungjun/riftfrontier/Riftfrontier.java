package kr.moonseungjun.riftfrontier;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.content.ContentServerReloadListener;
import kr.moonseungjun.riftfrontier.content.bootstrap.CoreContentBootstrap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
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

        NeoForge.EVENT_BUS.addListener(Riftfrontier::addServerReloadListeners);
    }

    private static void addServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(ContentServerReloadListener.ID, new ContentServerReloadListener());
    }
}

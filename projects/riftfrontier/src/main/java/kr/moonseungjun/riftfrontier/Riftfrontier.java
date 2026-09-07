package kr.moonseungjun.riftfrontier;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.riftfrontier.content.ContentCatalog;
import kr.moonseungjun.riftfrontier.content.bootstrap.CoreContentBootstrap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Riftfrontier.MOD_ID)
public final class Riftfrontier {
    public static final String MOD_ID = "riftfrontier";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Riftfrontier(IEventBus modEventBus, ModContainer modContainer) {
        var pack = CoreContentBootstrap.bootstrapAndValidate();
        var report = pack.validation();
        report.warnings().forEach(issue -> LOGGER.warn("[content] {} - {}", issue.source(), issue.message()));

        ContentCatalog catalog = ContentCatalog.from(pack.registry());
        LOGGER.info(
            "Riftfrontier core loaded: pack={}, schema={}, definitions={}, catalog={}",
            pack.packId(), pack.schemaVersion(), report.definitionCount(), catalog.fingerprint()
        );
    }
}

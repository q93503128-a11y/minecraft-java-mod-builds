package kr.moonseungjun.riftfrontier;

import com.mojang.logging.LogUtils;
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
        var report = CoreContentBootstrap.bootstrapAndValidate();
        if (report.hasErrors()) {
            throw new IllegalStateException("Riftfrontier core content validation failed:\n" + report.format());
        }
        report.warnings().forEach(issue -> LOGGER.warn("[content] {}", issue.message()));
        LOGGER.info("Riftfrontier core loaded: {} definitions", report.definitionCount());
    }
}

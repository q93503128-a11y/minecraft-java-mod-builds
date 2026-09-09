package dev.moonseungjun.fishinggame;

import dev.moonseungjun.fishinggame.fishing.FishingSessionManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FishingGameMod implements ModInitializer {
    public static final String MOD_ID = "fishinggame";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        FishingSessionManager.initialize();
        LOGGER.info("Fishing Game initialized");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

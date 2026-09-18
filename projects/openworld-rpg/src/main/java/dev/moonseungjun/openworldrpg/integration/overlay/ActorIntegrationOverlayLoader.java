package dev.moonseungjun.openworldrpg.integration.overlay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;

public final class ActorIntegrationOverlayLoader {
    private static final Gson GSON = new GsonBuilder().create();

    private ActorIntegrationOverlayLoader() {
    }

    public static ActorIntegrationOverlay parse(Reader reader) {
        ActorIntegrationOverlay overlay = GSON.fromJson(reader, ActorIntegrationOverlay.class);
        if (overlay == null) {
            throw new IllegalArgumentException("Actor integration overlay parsed to null.");
        }
        return overlay;
    }
}

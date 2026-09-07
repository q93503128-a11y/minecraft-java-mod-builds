package kr.moonseungjun.riftfrontier.content.bootstrap;

import kr.moonseungjun.riftfrontier.content.ContentPackLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** M1 technical fixture loaded through the same typed JSON path future content packs will use. */
public final class CoreContentBootstrap {
    private static final String FIXTURE_RESOURCE = "/data/riftfrontier/riftfrontier/content/vertical_slice_fixture.json";

    private CoreContentBootstrap() { }

    public static ContentPackLoader.LoadedPack loadFixturePack() {
        try (InputStream stream = CoreContentBootstrap.class.getResourceAsStream(FIXTURE_RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing core content fixture: " + FIXTURE_RESOURCE);
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return new ContentPackLoader().load(reader);
            }
        } catch (IOException error) {
            throw new IllegalStateException("Failed to read core content fixture: " + FIXTURE_RESOURCE, error);
        }
    }

    public static ContentPackLoader.LoadedPack bootstrapAndValidate() {
        ContentPackLoader.LoadedPack pack = loadFixturePack();
        pack.requireValid();
        return pack;
    }
}

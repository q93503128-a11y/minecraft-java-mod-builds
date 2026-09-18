package dev.moonseungjun.openworldrpg.integration.bootstrap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class DependencyManifestLoader {
    public static final String DEFAULT_RESOURCE = "data/openworld_rpg/integration/dependencies.json";
    private static final Gson GSON = new GsonBuilder().create();

    private DependencyManifestLoader() {
    }

    public static DependencyManifest loadDefault() {
        ClassLoader loader = DependencyManifestLoader.class.getClassLoader();
        InputStream stream = loader.getResourceAsStream(DEFAULT_RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Missing integration dependency manifest: " + DEFAULT_RESOURCE);
        }
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            DependencyManifest manifest = GSON.fromJson(reader, DependencyManifest.class);
            if (manifest == null) {
                throw new IllegalStateException("Dependency manifest parsed to null: " + DEFAULT_RESOURCE);
            }
            return manifest;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read dependency manifest: " + DEFAULT_RESOURCE, exception);
        }
    }
}

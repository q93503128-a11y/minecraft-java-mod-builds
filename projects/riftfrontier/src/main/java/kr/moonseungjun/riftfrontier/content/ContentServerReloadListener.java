package kr.moonseungjun.riftfrontier.content;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Loads all server data-pack content documents into an isolated registry, validates the merged graph,
 * and only then atomically replaces the runtime snapshot.
 */
public final class ContentServerReloadListener implements ResourceManagerReloadListener {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Riftfrontier.MOD_ID, "content_runtime");
    public static final String CONTENT_DIRECTORY = "riftfrontier/content";

    private final ContentPackLoader loader = new ContentPackLoader();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        Map<Identifier, Resource> resources = resourceManager.listResources(
            CONTENT_DIRECTORY,
            id -> Riftfrontier.MOD_ID.equals(id.getNamespace()) && id.getPath().endsWith(".json")
        );
        if (resources.isEmpty()) {
            throw new IllegalStateException("No Riftfrontier content documents found under data/" + Riftfrontier.MOD_ID + "/" + CONTENT_DIRECTORY);
        }

        ContentRegistry merged = new ContentRegistry();
        List<String> packIds = new ArrayList<>();
        List<Map.Entry<Identifier, Resource>> ordered = resources.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
            .toList();

        for (Map.Entry<Identifier, Resource> entry : ordered) {
            try (var reader = entry.getValue().openAsReader()) {
                ContentPackLoader.LoadedPack pack = loader.load(reader);
                pack.requireValid();
                packIds.add(pack.packId());
                for (CoreDefinition definition : pack.registry().all()) {
                    merged.register(definition);
                }
            } catch (IOException | RuntimeException error) {
                throw new IllegalStateException("Failed to load Riftfrontier content resource " + entry.getKey() + ": " + error.getMessage(), error);
            }
        }

        ContentRuntimeSnapshot snapshot = ContentRuntime.installValidated(merged, packIds);
        Riftfrontier.LOGGER.info(
            "Riftfrontier content snapshot published: generation={}, packs={}, definitions={}, fingerprint={}",
            snapshot.generation(), snapshot.packIds(), snapshot.definitionCount(), snapshot.fingerprint()
        );
    }
}

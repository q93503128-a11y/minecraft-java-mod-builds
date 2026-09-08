package kr.moonseungjun.riftfrontier.content;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationProfile;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationProfileCodec;
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
 * Loads all server data-pack content and presentation documents into isolated candidates, validates the
 * complete graph, and only then atomically replaces the runtime snapshot.
 */
public final class ContentServerReloadListener implements ResourceManagerReloadListener {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Riftfrontier.MOD_ID, "content_runtime");
    public static final String CONTENT_DIRECTORY = "riftfrontier/content";
    public static final String PRESENTATION_DIRECTORY = "riftfrontier/presentation";

    private final ContentPackLoader loader = new ContentPackLoader();
    private final BossPresentationProfileCodec presentationCodec = new BossPresentationProfileCodec();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        Map<Identifier, Resource> resources = resourceManager.listResources(
            CONTENT_DIRECTORY,
            id -> Riftfrontier.MOD_ID.equals(id.getNamespace()) && id.getPath().endsWith(".json")
        );
        if (resources.isEmpty()) {
            throw new IllegalStateException("No Riftfrontier content documents found under data/" + Riftfrontier.MOD_ID + "/" + CONTENT_DIRECTORY);
        }

        List<ContentPackLoader.LoadedPack> packs = new ArrayList<>();
        List<Map.Entry<Identifier, Resource>> ordered = resources.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
            .toList();

        for (Map.Entry<Identifier, Resource> entry : ordered) {
            String source = entry.getKey().toString();
            try (var reader = entry.getValue().openAsReader()) {
                packs.add(loader.load(reader, source));
            } catch (IOException | RuntimeException error) {
                throw new IllegalStateException("Failed to load Riftfrontier content resource " + source + ": " + error.getMessage(), error);
            }
        }

        List<BossPresentationProfile> presentationProfiles = loadPresentationProfiles(resourceManager);
        ContentPackSet.Merged merged = ContentPackSet.merge(packs);
        ContentRuntimeSnapshot snapshot = ContentRuntime.installValidated(merged.registry(), merged.packIds(), presentationProfiles);
        Riftfrontier.LOGGER.info(
            "Riftfrontier content snapshot published: generation={}, packs={}, definitions={}, bossPresentations={}, fingerprint={}, provenance={}",
            snapshot.generation(), snapshot.packIds(), snapshot.definitionCount(), snapshot.bossPresentationProfileCount(), snapshot.fingerprint(), merged.provenance()
        );
    }

    private List<BossPresentationProfile> loadPresentationProfiles(ResourceManager resourceManager) {
        Map<Identifier, Resource> resources = resourceManager.listResources(
            PRESENTATION_DIRECTORY,
            id -> Riftfrontier.MOD_ID.equals(id.getNamespace()) && id.getPath().endsWith(".json")
        );
        List<Map.Entry<Identifier, Resource>> ordered = resources.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
            .toList();
        List<BossPresentationProfile> profiles = new ArrayList<>(ordered.size());

        for (Map.Entry<Identifier, Resource> entry : ordered) {
            String source = entry.getKey().toString();
            try (var reader = entry.getValue().openAsReader()) {
                profiles.add(presentationCodec.decode(reader));
            } catch (IOException | RuntimeException error) {
                throw new IllegalStateException("Failed to load Riftfrontier presentation resource " + source + ": " + error.getMessage(), error);
            }
        }
        return List.copyOf(profiles);
    }
}

package kr.moonseungjun.turnboundre.data;

import kr.moonseungjun.turnboundre.TurnboundRe;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server datapack listener for data/&lt;namespace&gt;/turnbound_definitions/*.json. */
public final class DefinitionResourceLoader implements ResourceManagerReloadListener {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "definitions");
    private static final String ROOT = "turnbound_definitions";

    private final DefinitionRepository repository;

    public DefinitionResourceLoader(DefinitionRepository repository) {
        if (repository == null) throw new IllegalArgumentException("repository must not be null");
        this.repository = repository;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        if (resourceManager == null) throw new IllegalArgumentException("resourceManager must not be null");

        Map<Identifier, Resource> discovered = resourceManager.listResources(
                ROOT, id -> id.getPath().endsWith(".json"));
        List<Map.Entry<Identifier, Resource>> ordered = new ArrayList<>(discovered.entrySet());
        ordered.sort(Comparator.comparing(entry -> entry.getKey().toString()));

        Map<String, String> raw = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : ordered) {
            raw.put(entry.getKey().toString(), read(entry.getKey(), entry.getValue()));
        }

        DefinitionBundleParser.Parsed parsed = DefinitionBundleParser.parse(raw);
        DefinitionRepository.Snapshot installed = repository.install(parsed.registry(), parsed.hash());
        TurnboundRe.LOGGER.info(
                "Loaded TURNBOUND definitions generation={} hash={} resources={} actions={} characters={} statuses={} encounters={} rewards={}",
                installed.generation(), installed.hash(), parsed.resourceIds().size(),
                installed.registry().actions().size(), installed.registry().characters().size(),
                installed.registry().statuses().size(), installed.registry().encounters().size(), installed.registry().rewards().size());
    }

    private static String read(Identifier id, Resource resource) {
        try (BufferedReader reader = resource.openAsReader()) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!out.isEmpty()) out.append('\n');
                out.append(line);
            }
            return out.toString();
        } catch (IOException error) {
            throw new IllegalStateException("Failed to read TURNBOUND definition resource " + id, error);
        }
    }
}

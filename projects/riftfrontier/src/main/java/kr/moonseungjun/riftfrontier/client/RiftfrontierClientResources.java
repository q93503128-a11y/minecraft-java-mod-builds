package kr.moonseungjun.riftfrontier.client;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.client.render.Region01BossClientRenderRuntime;
import kr.moonseungjun.riftfrontier.client.render.Region01BossGeometryPreparation;
import kr.moonseungjun.riftfrontier.client.render.Region01BossRuntimeResources;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationClientAssetRuntime;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Physical-client-only resource reload integration for production boss presentation assets. */
@EventBusSubscriber(modid = Riftfrontier.MOD_ID, value = Dist.CLIENT)
public final class RiftfrontierClientResources {
    private static final Identifier BOSS_PRESENTATION_ASSET_RELOAD =
        Identifier.fromNamespaceAndPath(Riftfrontier.MOD_ID, "boss_presentation_assets");
    private static final AtomicReference<Region01BossGeometryPreparation.PreparedGeometry> PREPARED_BOSS_GEOMETRY =
        new AtomicReference<>();

    private RiftfrontierClientResources() {}

    @SubscribeEvent
    private static void addClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(BOSS_PRESENTATION_ASSET_RELOAD, createBossPresentationReloadListener());
    }

    static ResourceManagerReloadListener createBossPresentationReloadListener() {
        return RiftfrontierClientResources::reloadBossPresentationAssets;
    }

    /**
     * Returns geometry prepared from the currently staged resource-manager snapshot only.
     * A newer reload/clear turns an older capability stale and removes it on observation.
     */
    public static Optional<Region01BossGeometryPreparation.PreparedGeometry> preparedBossGeometry() {
        Region01BossGeometryPreparation.PreparedGeometry prepared = PREPARED_BOSS_GEOMETRY.get();
        if (prepared == null) return Optional.empty();
        try {
            prepared.runtimeAsset();
            return Optional.of(prepared);
        } catch (Region01BossGeometryPreparation.StaleReloadException stale) {
            PREPARED_BOSS_GEOMETRY.compareAndSet(prepared, null);
            return Optional.empty();
        }
    }

    private static void reloadBossPresentationAssets(ResourceManager clientResources) {
        // Bind the complete preparation transaction to this exact resource-manager snapshot before inspecting it.
        // A newer pack reload invalidates both the old renderer binding and every unfinished preparation capability.
        PREPARED_BOSS_GEOMETRY.set(null);
        var reloadTicket = Region01BossClientRenderRuntime.beginReload(clientResources);

        var content = ContentRuntime.requireCurrent();
        try {
            var published = BossPresentationClientAssetRuntime.reload(
                content.generation(),
                content.bossPresentationAssetManifest(),
                new MinecraftClientBossPresentationResourceProbe(clientResources)
            );
            if (published.ready()) {
                var staged = Region01BossClientRenderRuntime.stageValidated(reloadTicket, published)
                    .orElseThrow(() -> new IllegalStateException(
                        "boss presentation resource reload was superseded before validated staging"
                    ));
                var prepared = Region01BossGeometryPreparation.prepare(
                    staged,
                    Region01BossRuntimeResources.ACCEPTED_GEOMETRY
                );
                if (Region01BossClientRenderRuntime.staged().orElse(null) != staged) {
                    throw new IllegalStateException(
                        "boss geometry preparation was superseded before prepared capability staging"
                    );
                }
                PREPARED_BOSS_GEOMETRY.set(prepared);
                if (Region01BossClientRenderRuntime.staged().orElse(null) != staged) {
                    PREPARED_BOSS_GEOMETRY.compareAndSet(prepared, null);
                    throw new IllegalStateException("boss geometry preparation was superseded before publication staging");
                }
                Riftfrontier.LOGGER.info(
                    "Riftfrontier boss presentation client geometry prepared from current resource snapshot: resource={}, contentGeneration={}, publicationGeneration={}",
                    prepared.resourceId(), staged.contentGeneration(), staged.publicationGeneration()
                );
            } else {
                Riftfrontier.LOGGER.debug(
                    "Riftfrontier boss presentation client assets inactive: no selected production manifest at contentGeneration={}",
                    published.contentGeneration()
                );
            }
        } catch (BossPresentationClientAssetRuntime.ResourceValidationException invalid) {
            PREPARED_BOSS_GEOMETRY.set(null);
            Region01BossClientRenderRuntime.clear();
            invalid.report().issues().forEach(issue -> Riftfrontier.LOGGER.error(
                "[boss-presentation-resource] {} {} - {}",
                issue.code(), issue.logicalKey(), issue.message()
            ));
            throw invalid;
        } catch (IOException invalidGeometry) {
            PREPARED_BOSS_GEOMETRY.set(null);
            Region01BossClientRenderRuntime.clear();
            throw new IllegalStateException(
                "accepted Region 01 boss geometry could not be prepared from the current client resource snapshot",
                invalidGeometry
            );
        } catch (RuntimeException invalidRuntime) {
            PREPARED_BOSS_GEOMETRY.set(null);
            Region01BossClientRenderRuntime.clear();
            throw invalidRuntime;
        }
    }
}

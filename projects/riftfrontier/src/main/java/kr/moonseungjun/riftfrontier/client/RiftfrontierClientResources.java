package kr.moonseungjun.riftfrontier.client;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.client.render.Region01BossAnimationPreparation;
import kr.moonseungjun.riftfrontier.client.render.Region01BossClientRenderRuntime;
import kr.moonseungjun.riftfrontier.client.render.Region01BossGeometryPreparation;
import kr.moonseungjun.riftfrontier.client.render.Region01BossMaterialPreparation;
import kr.moonseungjun.riftfrontier.client.render.Region01BossRuntimeResources;
import kr.moonseungjun.riftfrontier.combat.presentation.BossAnimationSemanticBinding;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationClientAssetRuntime;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import net.minecraft.client.renderer.rendertype.RenderType;
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
    private static final AtomicReference<Region01BossGeometryPreparation.PreparedGeometry> PREPARED_BOSS_GEOMETRY = new AtomicReference<>();
    private static final AtomicReference<Region01BossAnimationPreparation.PreparedAnimation> PREPARED_BOSS_ANIMATION = new AtomicReference<>();
    private static final AtomicReference<Region01BossMaterialPreparation.PreparedMaterial> PREPARED_BOSS_MATERIAL = new AtomicReference<>();

    private RiftfrontierClientResources() {}

    @SubscribeEvent
    private static void addClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(BOSS_PRESENTATION_ASSET_RELOAD, createBossPresentationReloadListener());
    }

    static ResourceManagerReloadListener createBossPresentationReloadListener() {
        return RiftfrontierClientResources::reloadBossPresentationAssets;
    }

    public static Optional<Region01BossGeometryPreparation.PreparedGeometry> preparedBossGeometry() {
        Region01BossGeometryPreparation.PreparedGeometry prepared = PREPARED_BOSS_GEOMETRY.get();
        if (prepared == null) return Optional.empty();
        try {
            prepared.runtimeAsset();
            return Optional.of(prepared);
        } catch (Region01BossGeometryPreparation.StaleReloadException stale) {
            PREPARED_BOSS_GEOMETRY.compareAndSet(prepared, null);
            PREPARED_BOSS_ANIMATION.set(null);
            PREPARED_BOSS_MATERIAL.set(null);
            return Optional.empty();
        }
    }

    /** Prepares animation only after server semantics and reviewed source windows have been explicitly joined. */
    public static Optional<Region01BossAnimationPreparation.PreparedAnimation> prepareBossAnimation(
        BossAnimationSemanticBinding semanticBinding
    ) {
        var geometry = preparedBossGeometry();
        if (geometry.isEmpty()) return Optional.empty();
        try {
            var prepared = Region01BossAnimationPreparation.prepare(geometry.orElseThrow(), semanticBinding);
            PREPARED_BOSS_MATERIAL.set(null);
            PREPARED_BOSS_ANIMATION.set(prepared);
            if (!prepared.isCurrent()) {
                PREPARED_BOSS_ANIMATION.compareAndSet(prepared, null);
                return Optional.empty();
            }
            return Optional.of(prepared);
        } catch (Region01BossGeometryPreparation.StaleReloadException stale) {
            PREPARED_BOSS_MATERIAL.set(null);
            PREPARED_BOSS_ANIMATION.set(null);
            return Optional.empty();
        }
    }

    public static Optional<Region01BossAnimationPreparation.PreparedAnimation> preparedBossAnimation() {
        Region01BossAnimationPreparation.PreparedAnimation prepared = PREPARED_BOSS_ANIMATION.get();
        if (prepared == null) return Optional.empty();
        if (!prepared.isCurrent()) {
            PREPARED_BOSS_ANIMATION.compareAndSet(prepared, null);
            PREPARED_BOSS_MATERIAL.set(null);
            return Optional.empty();
        }
        return Optional.of(prepared);
    }

    public static Optional<Region01BossMaterialPreparation.PreparedMaterial> prepareBossMaterial(
        Region01BossMaterialPreparation.MaterialReview review,
        RenderType reviewedRenderType,
        int packedOverlay,
        int packedColor
    ) throws IOException {
        var animation = preparedBossAnimation();
        if (animation.isEmpty()) return Optional.empty();
        try {
            var prepared = Region01BossMaterialPreparation.prepare(
                animation.orElseThrow(), review, reviewedRenderType, packedOverlay, packedColor
            );
            PREPARED_BOSS_MATERIAL.set(prepared);
            if (!prepared.isCurrent()) {
                PREPARED_BOSS_MATERIAL.compareAndSet(prepared, null);
                return Optional.empty();
            }
            return Optional.of(prepared);
        } catch (Region01BossMaterialPreparation.StaleMaterialPreparationException stale) {
            PREPARED_BOSS_MATERIAL.set(null);
            return Optional.empty();
        }
    }

    public static Optional<Region01BossMaterialPreparation.PreparedMaterial> preparedBossMaterial() {
        Region01BossMaterialPreparation.PreparedMaterial prepared = PREPARED_BOSS_MATERIAL.get();
        if (prepared == null) return Optional.empty();
        if (!prepared.isCurrent()) {
            PREPARED_BOSS_MATERIAL.compareAndSet(prepared, null);
            return Optional.empty();
        }
        return Optional.of(prepared);
    }

    private static void reloadBossPresentationAssets(ResourceManager clientResources) {
        PREPARED_BOSS_MATERIAL.set(null);
        PREPARED_BOSS_ANIMATION.set(null);
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
                var prepared = Region01BossGeometryPreparation.prepare(staged, Region01BossRuntimeResources.ACCEPTED_GEOMETRY);
                if (Region01BossClientRenderRuntime.staged().orElse(null) != staged) {
                    throw new IllegalStateException("boss geometry preparation was superseded before prepared capability staging");
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
            clearPreparedPresentation();
            invalid.report().issues().forEach(issue -> Riftfrontier.LOGGER.error(
                "[boss-presentation-resource] {} {} - {}", issue.code(), issue.logicalKey(), issue.message()
            ));
            throw invalid;
        } catch (IOException invalidGeometry) {
            clearPreparedPresentation();
            throw new IllegalStateException(
                "accepted Region 01 boss geometry could not be prepared from the current client resource snapshot",
                invalidGeometry
            );
        } catch (RuntimeException invalidRuntime) {
            clearPreparedPresentation();
            throw invalidRuntime;
        }
    }

    /**
     * Retires every prepared/published boss-render capability at a network connection boundary.
     * Client resource reloads are not guaranteed to run between servers, so retaining one of these immutable
     * capabilities across logout could let a later connection feed fresh semantics through stale reviewed assets.
     */
    static void retireBossPresentationConnectionEpoch() {
        clearPreparedPresentation();
    }

    private static void clearPreparedPresentation() {
        PREPARED_BOSS_MATERIAL.set(null);
        PREPARED_BOSS_ANIMATION.set(null);
        PREPARED_BOSS_GEOMETRY.set(null);
        Region01BossClientRenderRuntime.clear();
    }
}

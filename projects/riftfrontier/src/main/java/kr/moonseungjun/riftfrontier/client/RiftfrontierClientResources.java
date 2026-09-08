package kr.moonseungjun.riftfrontier.client;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationClientAssetRuntime;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

/** Physical-client-only resource reload integration for production boss presentation assets. */
@EventBusSubscriber(modid = Riftfrontier.MOD_ID, value = Dist.CLIENT)
public final class RiftfrontierClientResources {
    private static final Identifier BOSS_PRESENTATION_ASSET_RELOAD =
        Identifier.fromNamespaceAndPath(Riftfrontier.MOD_ID, "boss_presentation_assets");

    private RiftfrontierClientResources() {}

    @SubscribeEvent
    private static void addClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(BOSS_PRESENTATION_ASSET_RELOAD, createBossPresentationReloadListener());
    }

    static ResourceManagerReloadListener createBossPresentationReloadListener() {
        return RiftfrontierClientResources::reloadBossPresentationAssets;
    }

    private static void reloadBossPresentationAssets(ResourceManager clientResources) {
        var content = ContentRuntime.requireCurrent();
        try {
            var published = BossPresentationClientAssetRuntime.reload(
                content.generation(),
                content.bossPresentationAssetManifest(),
                new MinecraftClientBossPresentationResourceProbe(clientResources)
            );
            if (published.ready()) {
                Riftfrontier.LOGGER.info(
                    "Riftfrontier boss presentation client assets validated and published: contentGeneration={}",
                    published.contentGeneration()
                );
            } else {
                Riftfrontier.LOGGER.debug(
                    "Riftfrontier boss presentation client assets inactive: no selected production manifest at contentGeneration={}",
                    published.contentGeneration()
                );
            }
        } catch (BossPresentationClientAssetRuntime.ResourceValidationException invalid) {
            invalid.report().issues().forEach(issue -> Riftfrontier.LOGGER.error(
                "[boss-presentation-resource] {} {} - {}",
                issue.code(), issue.logicalKey(), issue.message()
            ));
            throw invalid;
        }
    }
}

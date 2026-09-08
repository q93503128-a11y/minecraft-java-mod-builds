package kr.moonseungjun.riftfrontier.client;

import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationAssetManifest;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationClientResourcePath;
import kr.moonseungjun.riftfrontier.content.ContentId;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Objects;

/**
 * Minecraft client-resource-pack implementation of the boss presentation physical resource probe.
 *
 * <p>This adapter must be constructed with the client resource manager. It never consults the server datapack
 * resource manager and therefore cannot accidentally treat server data as proof that a model, animation, VFX, or
 * sound file is renderable on the client.</p>
 */
public final class MinecraftClientBossPresentationResourceProbe implements BossPresentationAssetManifest.ResourceProbe {
    private final ResourceManager clientResources;

    public MinecraftClientBossPresentationResourceProbe(ResourceManager clientResources) {
        this.clientResources = Objects.requireNonNull(clientResources, "clientResources");
    }

    @Override
    public boolean exists(BossPresentationAssetManifest.Kind kind, ContentId selectedResource) {
        return BossPresentationClientResourcePath.resolve(kind, selectedResource)
            .map(MinecraftClientBossPresentationResourceProbe::toIdentifier)
            .flatMap(clientResources::getResource)
            .isPresent();
    }

    private static Identifier toIdentifier(ContentId id) {
        return Identifier.fromNamespaceAndPath(id.namespace(), id.path());
    }
}

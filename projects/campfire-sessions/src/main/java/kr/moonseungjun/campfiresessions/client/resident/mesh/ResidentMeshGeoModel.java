package kr.moonseungjun.campfiresessions.client.resident.mesh;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.cache.model.ModelProperties;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * Programmatic GeckoLib model backed by Campfire's converted triangle hierarchy.
 */
public final class ResidentMeshGeoModel<T extends GeoAnimatable> extends GeoModel<T> {
    private final Identifier modelId;
    private final Identifier fallbackTexture;
    private final Identifier animationResource;
    private final ResidentMeshMaterial[] materials;
    private final BakedGeoModel bakedModel;

    public ResidentMeshGeoModel(
            Identifier modelId,
            Identifier fallbackTexture,
            Identifier animationResource,
            int textureWidth,
            int textureHeight,
            ResidentMeshMaterial[] materials,
            ResidentMeshBoneDefinition[] roots
    ) {
        if (materials.length == 0) {
            throw new IllegalArgumentException("resident mesh requires at least one material");
        }

        this.modelId = modelId;
        this.fallbackTexture = fallbackTexture;
        this.animationResource = animationResource;
        this.materials = materials.clone();

        GeoBone[] bakedRoots = new GeoBone[roots.length];
        for (int i = 0; i < roots.length; i++) {
            bakedRoots[i] = roots[i].bake(null);
        }

        this.bakedModel = new BakedGeoModel(
                bakedRoots,
                Map.<String, GeoLocator>of(),
                new ModelProperties(
                        modelId,
                        "geometry.campfiresessions.runtime_resident",
                        null,
                        null,
                        null,
                        textureWidth,
                        textureHeight
                )
        );
    }

    public int materialCount() {
        return this.materials.length;
    }

    public ResidentMeshMaterial material(int index) {
        return this.materials[index];
    }

    public <R extends GeoRenderState> void renderMaterial(
            RenderPassInfo<R> renderPassInfo,
            int materialIndex,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            int renderColor
    ) {
        for (GeoBone bone : this.bakedModel.topLevelBones()) {
            if (bone instanceof ResidentMeshBone meshBone) {
                meshBone.positionAndRenderMaterial(
                        renderPassInfo,
                        materialIndex,
                        vertexConsumer,
                        packedLight,
                        packedOverlay,
                        renderColor
                );
            }
        }
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return this.modelId;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return this.fallbackTexture;
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return this.animationResource;
    }

    @Override
    public BakedGeoModel getBakedModel(Identifier location) {
        return this.bakedModel;
    }
}

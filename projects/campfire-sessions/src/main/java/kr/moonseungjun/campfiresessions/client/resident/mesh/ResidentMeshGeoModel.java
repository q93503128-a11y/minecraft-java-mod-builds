package kr.moonseungjun.campfiresessions.client.resident.mesh;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.cache.model.ModelProperties;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * Programmatic GeckoLib model backed by Campfire's converted triangle hierarchy.
 *
 * <p>This avoids forcing arbitrary Plumberry meshes through Bedrock cuboids.
 * The model still participates in GeckoLib's entity render lifecycle, bone
 * snapshots and per-bone attachment positioning.</p>
 */
public final class ResidentMeshGeoModel<T extends GeoAnimatable> extends GeoModel<T> {
    private final Identifier modelId;
    private final Identifier texture;
    private final Identifier animationResource;
    private final BakedGeoModel bakedModel;

    public ResidentMeshGeoModel(
            Identifier modelId,
            Identifier texture,
            Identifier animationResource,
            int textureWidth,
            int textureHeight,
            ResidentMeshBoneDefinition[] roots
    ) {
        this.modelId = modelId;
        this.texture = texture;
        this.animationResource = animationResource;

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

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return this.modelId;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return this.texture;
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return this.animationResource;
    }

    /**
     * Deliberately bypass GeckoLib's .geo.json baked-model cache.
     * The source hierarchy is created from the converted Plumberry runtime data.
     */
    @Override
    public BakedGeoModel getBakedModel(Identifier location) {
        return this.bakedModel;
    }
}

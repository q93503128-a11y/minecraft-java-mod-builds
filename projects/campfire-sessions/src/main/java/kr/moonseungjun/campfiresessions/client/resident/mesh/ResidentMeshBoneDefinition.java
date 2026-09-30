package kr.moonseungjun.campfiresessions.client.resident.mesh;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import org.jspecify.annotations.Nullable;

/**
 * Immutable source hierarchy used to create singleton GeckoLib model bones.
 *
 * <p>The converted runtime asset stores local mesh vertices and a transform-node
 * hierarchy. Local T/R/S is sampled separately and applied through GeckoLib
 * BoneSnapshot state for each render frame.</p>
 */
public record ResidentMeshBoneDefinition(
        String name,
        ResidentMeshPrimitive[] primitives,
        ResidentMeshBoneDefinition[] children
) {
    public ResidentMeshBoneDefinition {
        primitives = primitives.clone();
        children = children.clone();
    }

    @Override
    public ResidentMeshPrimitive[] primitives() {
        return this.primitives.clone();
    }

    @Override
    public ResidentMeshBoneDefinition[] children() {
        return this.children.clone();
    }

    public ResidentMeshBone bake(@Nullable GeoBone parent) {
        GeoBone[] bakedChildren = new GeoBone[this.children.length];
        ResidentMeshBone bone = new ResidentMeshBone(
                parent,
                this.name,
                bakedChildren,
                new GeoLocator[0],
                this.primitives
        );

        for (int i = 0; i < this.children.length; i++) {
            bakedChildren[i] = this.children[i].bake(bone);
        }

        return bone;
    }
}

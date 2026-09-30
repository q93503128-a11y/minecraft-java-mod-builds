package kr.moonseungjun.campfiresessions.client.resident.mesh;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import org.jspecify.annotations.Nullable;

/**
 * Immutable source hierarchy used to create singleton GeckoLib model bones.
 *
 * <p>The converted Plumberry runtime asset stores local mesh vertices and a
 * transform-node hierarchy. Local transforms are applied per render frame via
 * BoneSnapshot; this definition only owns geometry and hierarchy.</p>
 */
public record ResidentMeshBoneDefinition(
        String name,
        ResidentMeshTriangle[] triangles,
        ResidentMeshBoneDefinition[] children
) {
    public ResidentMeshBone bake(@Nullable GeoBone parent) {
        GeoBone[] bakedChildren = new GeoBone[this.children.length];
        ResidentMeshBone bone = new ResidentMeshBone(
                parent,
                this.name,
                bakedChildren,
                new GeoLocator[0],
                this.triangles
        );

        for (int i = 0; i < this.children.length; i++) {
            bakedChildren[i] = this.children[i].bake(bone);
        }

        return bone;
    }
}

package kr.moonseungjun.campfiresessions.client.resident.mesh;

import net.minecraft.resources.Identifier;

/**
 * One converted glTF material.
 *
 * <p>Campfire currently supports the base-color texture/factor path. The
 * Plumberry probe rejects source features that would silently lose appearance
 * (blend materials, secondary UV sets, normal/emissive/metallic-roughness
 * textures or texture-transform extensions) before conversion.</p>
 */
public record ResidentMeshMaterial(
        String name,
        Identifier texture,
        int baseColorArgb
) {
    public ResidentMeshMaterial {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("material name must not be blank");
        }
        if (texture == null) {
            throw new IllegalArgumentException("material texture must not be null");
        }
    }
}

package kr.moonseungjun.campfiresessions.client.resident.mesh;

/**
 * One textured vertex in Campfire resident model-pixel coordinates.
 *
 * <p>Runtime conversion uses 16 model pixels per Minecraft block so the resident
 * hierarchy can share GeckoLib's ordinary entity-space conventions.</p>
 */
public record ResidentMeshVertex(
        float x,
        float y,
        float z,
        float u,
        float v,
        float normalX,
        float normalY,
        float normalZ
) {}

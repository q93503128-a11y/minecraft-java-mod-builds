package kr.moonseungjun.campfiresessions.client.resident.mesh;

/**
 * One source triangle. ResidentMeshBone emits it as a degenerate quad because
 * the standard entity render type consumes quad groups.
 */
public record ResidentMeshTriangle(
        ResidentMeshVertex a,
        ResidentMeshVertex b,
        ResidentMeshVertex c
) {}

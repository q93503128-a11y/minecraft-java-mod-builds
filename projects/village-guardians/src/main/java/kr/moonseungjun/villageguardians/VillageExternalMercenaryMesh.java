package kr.moonseungjun.villageguardians;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight client-side OBJ reader for the pinned CC0 Quaternius RPG Character Pack meshes.
 * Gameplay stays on the invisible, server-authoritative IronGolem entity.
 */
final class VillageExternalMercenaryMesh {
    private static final String[] RESOURCES = {
            "/assets/villageguardians/models/mercenary/bastion.obj",
            "/assets/villageguardians/models/mercenary/striker.obj",
            "/assets/villageguardians/models/mercenary/ranger.obj",
            "/assets/villageguardians/models/mercenary/medic.obj",
            "/assets/villageguardians/models/mercenary/warder.obj",
            "/assets/villageguardians/models/mercenary/artillerist.obj"
    };
    private static final Mesh[] CACHE = new Mesh[RESOURCES.length];

    private VillageExternalMercenaryMesh() {}

    static synchronized Mesh mesh(int style) {
        int safe = Math.max(0, Math.min(RESOURCES.length - 1, style));
        Mesh cached = CACHE[safe];
        if (cached != null) return cached;
        Mesh loaded = load(RESOURCES[safe]);
        CACHE[safe] = loaded;
        return loaded;
    }

    private static Mesh load(String path) {
        InputStream stream = VillageExternalMercenaryMesh.class.getResourceAsStream(path);
        if (stream == null) {
            VillageGuardians.LOGGER.warn("Missing mercenary model resource {}", path);
            return Mesh.EMPTY;
        }
        List<float[]> rawVertices = new ArrayList<>();
        List<Integer> triangles = new ArrayList<>();
        List<Byte> materials = new ArrayList<>();
        boolean weaponMaterial = false;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("v ")) {
                    String[] parts = trimmed.substring(2).trim().split("\\s+");
                    if (parts.length >= 3) {
                        rawVertices.add(new float[] {
                                Float.parseFloat(parts[0]),
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2])
                        });
                    }
                } else if (trimmed.startsWith("usemtl ") || trimmed.startsWith("o ")) {
                    String token = trimmed.substring(trimmed.indexOf(' ') + 1).toLowerCase(Locale.ROOT);
                    weaponMaterial = token.contains("sword") || token.contains("dagger")
                            || token.contains("bow") || token.contains("staff");
                } else if (trimmed.startsWith("f ")) {
                    String[] parts = trimmed.substring(2).trim().split("\\s+");
                    if (parts.length < 3) continue;
                    int[] face = new int[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        face[i] = parseIndex(parts[i], rawVertices.size());
                    }
                    for (int i = 1; i + 1 < face.length; i++) {
                        if (face[0] < 0 || face[i] < 0 || face[i + 1] < 0) continue;
                        triangles.add(face[0]);
                        triangles.add(face[i]);
                        triangles.add(face[i + 1]);
                        materials.add((byte) (weaponMaterial ? 1 : 0));
                    }
                }
            }
        } catch (Exception error) {
            VillageGuardians.LOGGER.warn("Failed to parse mercenary model {}", path, error);
            return Mesh.EMPTY;
        }
        if (rawVertices.isEmpty() || triangles.isEmpty()) return Mesh.EMPTY;

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (float[] vertex : rawVertices) {
            minX = Math.min(minX, vertex[0]); maxX = Math.max(maxX, vertex[0]);
            minY = Math.min(minY, vertex[1]); maxY = Math.max(maxY, vertex[1]);
            minZ = Math.min(minZ, vertex[2]); maxZ = Math.max(maxZ, vertex[2]);
        }
        float height = Math.max(0.01f, maxY - minY);
        float scale = 2.35f / height;
        float centerX = (minX + maxX) * 0.5f;
        float centerZ = (minZ + maxZ) * 0.5f;
        float[] positions = new float[rawVertices.size() * 3];
        for (int i = 0; i < rawVertices.size(); i++) {
            float[] vertex = rawVertices.get(i);
            positions[i * 3] = (vertex[0] - centerX) * scale;
            positions[i * 3 + 1] = (vertex[1] - minY) * scale;
            positions[i * 3 + 2] = -(vertex[2] - centerZ) * scale;
        }
        int[] triangleArray = new int[triangles.size()];
        for (int i = 0; i < triangles.size(); i++) triangleArray[i] = triangles.get(i);
        byte[] materialArray = new byte[materials.size()];
        for (int i = 0; i < materials.size(); i++) materialArray[i] = materials.get(i);
        return new Mesh(positions, triangleArray, materialArray);
    }

    private static int parseIndex(String token, int vertexCount) {
        if (token == null || token.isBlank()) return -1;
        String value = token.split("/", 2)[0];
        try {
            int raw = Integer.parseInt(value);
            int index = raw < 0 ? vertexCount + raw : raw - 1;
            return index >= 0 && index < vertexCount ? index : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    record Mesh(float[] positions, int[] triangles, byte[] materials) {
        static final Mesh EMPTY = new Mesh(new float[0], new int[0], new byte[0]);

        int triangleCount() { return triangles.length / 3; }
        int index(int triangle, int corner) { return triangles[triangle * 3 + corner]; }
        boolean weapon(int triangle) {
            return triangle >= 0 && triangle < materials.length && materials[triangle] != 0;
        }
        float x(int vertex) { return positions[vertex * 3]; }
        float y(int vertex) { return positions[vertex * 3 + 1]; }
        float z(int vertex) { return positions[vertex * 3 + 2]; }
        boolean empty() { return triangleCount() == 0; }
    }
}

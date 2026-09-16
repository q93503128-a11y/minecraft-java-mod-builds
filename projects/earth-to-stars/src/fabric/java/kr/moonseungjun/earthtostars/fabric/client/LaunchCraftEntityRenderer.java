package kr.moonseungjun.earthtostars.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import kr.moonseungjun.earthtostars.fabric.EarthToStarsFabric;
import kr.moonseungjun.earthtostars.fabric.entity.LaunchCraftEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LaunchCraftEntityRenderer extends EntityRenderer<LaunchCraftEntity, LaunchCraftRenderState> {
    private static final RenderType RENDER_TYPE = RenderTypes.entitySolid(
            EarthToStarsFabric.id("textures/entity/white.png")
    );
    private static final CraftMesh MESH = CraftMesh.load();

    public LaunchCraftEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 2.45F;
        shadowStrength = 0.72F;
    }

    @Override
    public LaunchCraftRenderState createRenderState() {
        return new LaunchCraftRenderState();
    }

    @Override
    public void extractRenderState(LaunchCraftEntity entity, LaunchCraftRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = entity.getYRot();
        state.pitch = entity.getXRot();
    }

    @Override
    public void submit(
            LaunchCraftRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState
    ) {
        super.submit(state, poseStack, collector, cameraState);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));
        collector.submitCustomGeometry(
                poseStack,
                RENDER_TYPE,
                (pose, vertices) -> MESH.emit(pose, vertices, state.lightCoords)
        );
        poseStack.popPose();
    }

    private record Vertex(float x, float y, float z, float nx, float ny, float nz) {
    }

    private record Face(Vertex a, Vertex b, Vertex c, Material material) {
    }

    private record Material(float red, float green, float blue) {
        private static Material fromName(String name) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "metalred" -> new Material(1.000F, 0.629F, 0.203F);
                case "dark" -> new Material(0.275F, 0.298F, 0.341F);
                case "metaldark" -> new Material(0.675F, 0.710F, 0.774F);
                default -> new Material(0.843F, 0.871F, 0.910F);
            };
        }
    }

    private static final class CraftMesh {
        private static final String RESOURCE = "/assets/earth_to_stars/models/entity/starter_craft.obj";
        private final List<Face> faces;

        private CraftMesh(List<Face> faces) {
            this.faces = List.copyOf(faces);
        }

        private static CraftMesh load() {
            try (InputStream stream = LaunchCraftEntityRenderer.class.getResourceAsStream(RESOURCE)) {
                if (stream == null) {
                    throw new IllegalStateException("missing Kenney launch craft mesh " + RESOURCE);
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    List<float[]> positions = new ArrayList<>();
                    List<float[]> normals = new ArrayList<>();
                    List<Face> faces = new ArrayList<>();
                    Material material = Material.fromName("metal");
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                            continue;
                        }
                        if (trimmed.startsWith("v ")) {
                            positions.add(parseVector(trimmed));
                        } else if (trimmed.startsWith("vn ")) {
                            normals.add(parseVector(trimmed));
                        } else if (trimmed.startsWith("usemtl ")) {
                            material = Material.fromName(trimmed.substring("usemtl ".length()).trim());
                        } else if (trimmed.startsWith("f ")) {
                            String[] tokens = trimmed.split("\\s+");
                            if (tokens.length != 4) {
                                throw new IllegalStateException("starter craft mesh contains non-triangle face: " + trimmed);
                            }
                            Vertex a = parseVertex(tokens[1], positions, normals);
                            Vertex b = parseVertex(tokens[2], positions, normals);
                            Vertex c = parseVertex(tokens[3], positions, normals);
                            faces.add(new Face(a, b, c, material));
                        }
                    }
                    if (faces.isEmpty()) {
                        throw new IllegalStateException("starter craft mesh contains no faces");
                    }
                    EarthToStarsFabric.LOGGER.info("Loaded Kenney launch craft mesh triangles={}", faces.size());
                    return new CraftMesh(faces);
                }
            } catch (IOException | RuntimeException failure) {
                throw new IllegalStateException("failed to load Kenney launch craft mesh", failure);
            }
        }

        private static float[] parseVector(String line) {
            String[] parts = line.split("\\s+");
            if (parts.length < 4) {
                throw new IllegalStateException("invalid OBJ vector: " + line);
            }
            return new float[]{
                    Float.parseFloat(parts[1]),
                    Float.parseFloat(parts[2]),
                    Float.parseFloat(parts[3])
            };
        }

        private static Vertex parseVertex(String token, List<float[]> positions, List<float[]> normals) {
            String[] indices = token.split("/");
            int positionIndex = Integer.parseInt(indices[0]) - 1;
            int normalIndex = Integer.parseInt(indices[2]) - 1;
            float[] position = positions.get(positionIndex);
            float[] normal = normals.get(normalIndex);
            return new Vertex(
                    position[0], position[1], position[2],
                    normal[0], normal[1], normal[2]
            );
        }

        private void emit(PoseStack.Pose pose, VertexConsumer consumer, int light) {
            for (Face face : faces) {
                emitVertex(pose, consumer, face.a(), face.material(), light);
                emitVertex(pose, consumer, face.b(), face.material(), light);
                emitVertex(pose, consumer, face.c(), face.material(), light);
                // entitySolid uses a quad topology. Repeating the final corner preserves the source triangle.
                emitVertex(pose, consumer, face.c(), face.material(), light);
            }
        }

        private static void emitVertex(
                PoseStack.Pose pose,
                VertexConsumer consumer,
                Vertex vertex,
                Material material,
                int light
        ) {
            consumer.addVertex(pose, vertex.x(), vertex.y(), vertex.z())
                    .setColor(material.red(), material.green(), material.blue(), 1.0F)
                    .setUv(0.5F, 0.5F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, vertex.nx(), vertex.ny(), vertex.nz());
        }
    }
}

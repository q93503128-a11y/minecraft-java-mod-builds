package kr.moonseungjun.campfiresessions.client.museum;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import kr.moonseungjun.campfiresessions.client.museum.MuseumExhibitAssets.MuseumStaticMeshAsset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.neoforge.client.event.RegisterFeatureRenderersEvent;
import net.neoforged.neoforge.client.extensions.OrderedSubmitNodeCollectorExtension;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HexFormat;

/**
 * Static GPU renderer for curated museum exhibits.
 *
 * <p>Geometry is loaded and uploaded at most once per FeatureRenderer lifetime,
 * then the same vertex/index buffers are reused for every frame. Missing or
 * hash-mismatched production resources are hard failures when an exhibit is
 * actually submitted; no placeholder mesh is generated.</p>
 */
public final class MuseumStaticMeshRenderer implements FeatureRenderer<MuseumStaticMeshRenderer.MuseumSubmit> {
    public static final FeatureRendererType<MuseumSubmit> TYPE =
            FeatureRendererType.create("campfiresessions:museum_static_mesh");

    private final Map<MuseumStaticMeshAsset, MuseumGpuMesh> meshes = new HashMap<>();

    public static void registerFeatureRenderer(RegisterFeatureRenderersEvent event) {
        event.register(TYPE, new MuseumStaticMeshRenderer());
    }

    /**
     * Submit a curated museum exhibit into NeoForge's solid feature phase.
     *
     * <p>The supplied pose is copied immediately so later PoseStack mutation cannot
     * alter the queued exhibit transform.</p>
     */
    public static void submit(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            MuseumStaticMeshAsset asset
    ) {
        RenderType renderType = MuseumRenderPipelines.baseColor(asset.baseColorTexture());
        ((OrderedSubmitNodeCollectorExtension) collector).submitSpecial(
                RenderPhaseKeys.SOLID,
                new MuseumSubmit(poseStack.last().copy(), asset, renderType)
        );
    }

    @Override
    public void prepareGroup(
            FeatureFrameContext context,
            List<MuseumSubmit> submits,
            boolean strictlyOrdered
    ) {
        for (MuseumSubmit submit : submits) {
            this.meshes.computeIfAbsent(submit.asset(), MuseumStaticMeshRenderer::loadVerifiedMesh);
        }
    }

    @Override
    public void executeGroup(
            FeatureFrameContext context,
            int groupIndex,
            List<MuseumSubmit> submits,
            boolean strictlyOrdered
    ) {
        for (MuseumSubmit submit : submits) {
            MuseumGpuMesh mesh = this.meshes.get(submit.asset());
            if (mesh == null || mesh.isClosed()) {
                throw new IllegalStateException("Campfire museum mesh was not prepared: " + submit.asset().mesh());
            }

            RenderSystem.getModelViewStack().pushMatrix();
            try {
                RenderSystem.getModelViewStack().mul(submit.pose().pose());
                submit.renderType().prepare().drawFromBuffer(
                        mesh.vertexBuffer(),
                        mesh.indexBuffer(),
                        IndexType.INT,
                        0,
                        0,
                        mesh.indexCount()
                );
            } finally {
                RenderSystem.getModelViewStack().popMatrix();
            }
        }
    }

    @Override
    public void close() {
        for (MuseumGpuMesh mesh : this.meshes.values()) {
            mesh.close();
        }
        this.meshes.clear();
    }

    private static MuseumGpuMesh loadVerifiedMesh(MuseumStaticMeshAsset asset) {
        byte[] bytes;
        try (InputStream input = Minecraft.getInstance()
                .getResourceManager()
                .getResourceOrThrow(asset.mesh())
                .open()) {
            bytes = input.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read production museum mesh " + asset.mesh(), exception);
        }

        String actualSha = sha256(bytes);
        if (!actualSha.equals(asset.meshSha256())) {
            throw new IllegalStateException(
                    "Production museum mesh SHA-256 mismatch for " + asset.mesh()
                            + ": expected=" + asset.meshSha256()
                            + " actual=" + actualSha
            );
        }

        MuseumStaticMeshData data = MuseumStaticMeshData.read(ByteBuffer.wrap(bytes));
        return MuseumGpuMesh.upload(asset.mesh().toString(), data);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM does not provide SHA-256", exception);
        }
    }

    public record MuseumSubmit(
            PoseStack.Pose pose,
            MuseumStaticMeshAsset asset,
            RenderType renderType
    ) implements SubmitNode {
        @Override
        public FeatureRendererType<? extends SubmitNode> featureType() {
            return TYPE;
        }
    }
}

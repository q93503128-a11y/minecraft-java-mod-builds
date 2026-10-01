package kr.moonseungjun.campfiresessions.client.museum;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * GPU-resident static museum geometry.
 *
 * <p>The source mesh is uploaded once and reused by the museum FeatureRenderer.
 * This keeps 100k-triangle exhibit geometry off the per-frame CPU submission
 * path and remains inside the Blaze3D device abstraction used by OpenGL/Vulkan.</p>
 */
public final class MuseumGpuMesh implements AutoCloseable {
    private final GpuBuffer vertexBuffer;
    private final GpuBuffer indexBuffer;
    private final int indexCount;

    private MuseumGpuMesh(GpuBuffer vertexBuffer, GpuBuffer indexBuffer, int indexCount) {
        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = indexBuffer;
        this.indexCount = indexCount;
    }

    public static MuseumGpuMesh upload(String debugName, MuseumStaticMeshData data) {
        GpuBuffer vertices = RenderSystem.getDevice().createBuffer(
                () -> "Campfire museum vertices: " + debugName,
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                data.vertexBytes()
        );

        try {
            GpuBuffer indices = RenderSystem.getDevice().createBuffer(
                    () -> "Campfire museum indices: " + debugName,
                    GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
                    data.indexBytes()
            );
            return new MuseumGpuMesh(vertices, indices, data.indexCount());
        } catch (RuntimeException exception) {
            vertices.close();
            throw exception;
        }
    }

    public GpuBuffer vertexBuffer() {
        return this.vertexBuffer;
    }

    public GpuBuffer indexBuffer() {
        return this.indexBuffer;
    }

    public int indexCount() {
        return this.indexCount;
    }

    public boolean isClosed() {
        return this.vertexBuffer.isClosed() || this.indexBuffer.isClosed();
    }

    @Override
    public void close() {
        if (!this.vertexBuffer.isClosed()) {
            this.vertexBuffer.close();
        }
        if (!this.indexBuffer.isClosed()) {
            this.indexBuffer.close();
        }
    }
}

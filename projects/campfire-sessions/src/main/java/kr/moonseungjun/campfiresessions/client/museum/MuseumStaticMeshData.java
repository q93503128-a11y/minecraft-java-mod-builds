package kr.moonseungjun.campfiresessions.client.museum;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Immutable validated view of Campfire's generated static museum mesh format.
 *
 * <p>This loader contains no fallback geometry. A missing/invalid production
 * mesh is an error instead of being replaced with a temporary model.</p>
 */
public final class MuseumStaticMeshData {
    public static final int VERSION = 1;
    public static final int HEADER_BYTES = 40;
    public static final int VERTEX_STRIDE = 32;
    public static final int INDEX_STRIDE = 4;

    private static final byte[] MAGIC = {'C', 'F', 'M', 'S'};

    private final int vertexCount;
    private final int indexCount;
    private final float[] bounds;
    private final ByteBuffer vertexBytes;
    private final ByteBuffer indexBytes;

    private MuseumStaticMeshData(
            int vertexCount,
            int indexCount,
            float[] bounds,
            ByteBuffer vertexBytes,
            ByteBuffer indexBytes
    ) {
        this.vertexCount = vertexCount;
        this.indexCount = indexCount;
        this.bounds = bounds;
        this.vertexBytes = vertexBytes.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        this.indexBytes = indexBytes.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
    }

    public static MuseumStaticMeshData read(InputStream input) throws IOException {
        return read(ByteBuffer.wrap(input.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN));
    }

    public static MuseumStaticMeshData read(ByteBuffer source) {
        ByteBuffer buffer = source.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        if (buffer.remaining() < HEADER_BYTES) {
            throw new IllegalArgumentException("Campfire museum mesh is shorter than its header");
        }

        for (byte expected : MAGIC) {
            if (buffer.get() != expected) {
                throw new IllegalArgumentException("Campfire museum mesh magic mismatch");
            }
        }

        int version = buffer.getInt();
        int vertexCount = buffer.getInt();
        int indexCount = buffer.getInt();
        if (version != VERSION) {
            throw new IllegalArgumentException("Unsupported Campfire museum mesh version: " + version);
        }
        if (vertexCount <= 0 || indexCount <= 0 || indexCount % 3 != 0) {
            throw new IllegalArgumentException(
                    "Invalid museum mesh counts: vertices=" + vertexCount + " indices=" + indexCount
            );
        }

        float[] bounds = new float[6];
        for (int i = 0; i < bounds.length; i++) {
            bounds[i] = buffer.getFloat();
        }

        long vertexByteCount = (long) vertexCount * VERTEX_STRIDE;
        long indexByteCount = (long) indexCount * INDEX_STRIDE;
        long expectedPayload = vertexByteCount + indexByteCount;
        if (expectedPayload > Integer.MAX_VALUE || buffer.remaining() != (int) expectedPayload) {
            throw new IllegalArgumentException(
                    "Museum mesh payload length mismatch: expected="
                            + expectedPayload + " actual=" + buffer.remaining()
            );
        }

        ByteBuffer vertices = buffer.slice().order(ByteOrder.LITTLE_ENDIAN);
        vertices.limit((int) vertexByteCount);
        buffer.position(buffer.position() + (int) vertexByteCount);

        ByteBuffer indices = buffer.slice().order(ByteOrder.LITTLE_ENDIAN);
        indices.limit((int) indexByteCount);

        return new MuseumStaticMeshData(vertexCount, indexCount, bounds, vertices, indices);
    }

    public int vertexCount() {
        return this.vertexCount;
    }

    public int indexCount() {
        return this.indexCount;
    }

    public int triangleCount() {
        return this.indexCount / 3;
    }

    public float[] bounds() {
        return this.bounds.clone();
    }

    public ByteBuffer vertexBytes() {
        return this.vertexBytes.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
    }

    public ByteBuffer indexBytes() {
        return this.indexBytes.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
    }
}

package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Deterministic, renderer-neutral importer for the accepted art-neutral Region 01 boss glTF shape.
 * It intentionally supports only the glTF features needed by the selected source and fails closed on
 * material/image payloads, sparse accessors, morph targets, external buffers and unsupported primitives.
 */
public final class GltfSkinnedMeshImporter {
    private static final int FLOAT = 5126;
    private static final int UNSIGNED_BYTE = 5121;
    private static final int UNSIGNED_SHORT = 5123;
    private static final int UNSIGNED_INT = 5125;

    private GltfSkinnedMeshImporter() {
    }

    public static SkinnedMeshAsset importArtNeutral(byte[] gltfUtf8) {
        if (gltfUtf8 == null || gltfUtf8.length == 0) {
            throw new IllegalArgumentException("glTF bytes must not be empty");
        }
        JsonObject root = parseUtf8(gltfUtf8);
        rejectArtPayload(root);

        JsonArray buffers = requireArray(root, "buffers");
        if (buffers.size() != 1) {
            throw new IllegalArgumentException("exactly one embedded glTF buffer is required");
        }
        byte[] binary = decodeEmbeddedBuffer(requireObject(buffers.get(0), "buffers[0]"));

        JsonArray meshes = requireArray(root, "meshes");
        JsonArray skins = requireArray(root, "skins");
        JsonArray nodes = requireArray(root, "nodes");
        if (meshes.size() != 1 || skins.size() != 1) {
            throw new IllegalArgumentException("accepted runtime import requires exactly one mesh and one skin");
        }

        JsonObject primitive = singleTrianglePrimitive(requireObject(meshes.get(0), "meshes[0]"));
        JsonObject attributes = requireObject(primitive.get("attributes"), "mesh primitive attributes");
        AccessorReader reader = new AccessorReader(root, binary);

        float[] positions = reader.readFloat(attribute(attributes, "POSITION"), "VEC3", 3, false);
        int vertexCount = positions.length / 3;
        float[] normals = reader.readFloat(attribute(attributes, "NORMAL"), "VEC3", 3, false);
        float[] uvs = reader.readFloat(attribute(attributes, "TEXCOORD_0"), "VEC2", 2, false);
        int[] joints = reader.readUnsigned(attribute(attributes, "JOINTS_0"), "VEC4", 4);
        float[] weights = reader.readFloat(attribute(attributes, "WEIGHTS_0"), "VEC4", 4, false);
        int[] indices = reader.readUnsigned(requireInt(primitive, "indices"), "SCALAR", 1);

        if (normals.length != vertexCount * 3 || uvs.length != vertexCount * 2
                || joints.length != vertexCount * 4 || weights.length != vertexCount * 4) {
            throw new IllegalArgumentException("mesh attribute accessor counts do not agree");
        }

        JsonObject skin = requireObject(skins.get(0), "skins[0]");
        JsonArray skinJoints = requireArray(skin, "joints");
        if (skinJoints.size() == 0) {
            throw new IllegalArgumentException("skin must contain at least one joint");
        }
        int[] jointNodes = new int[skinJoints.size()];
        Map<Integer, Integer> nodeToJoint = new HashMap<>();
        for (int joint = 0; joint < jointNodes.length; joint++) {
            int node = requireInt(skinJoints.get(joint), "skins[0].joints[" + joint + "]");
            checkIndex(node, nodes.size(), "joint node");
            if (nodeToJoint.put(node, joint) != null) {
                throw new IllegalArgumentException("skin joint node is duplicated: " + node);
            }
            jointNodes[joint] = node;
        }
        for (int joint : joints) {
            if (joint < 0 || joint >= jointNodes.length) {
                throw new IllegalArgumentException("JOINTS_0 references palette entry outside skin: " + joint);
            }
        }

        int[] nodeParents = deriveNodeParents(nodes);
        int[] parentJoints = new int[jointNodes.length];
        String[] names = new String[jointNodes.length];
        Affine3x4[] rest = new Affine3x4[jointNodes.length];
        for (int joint = 0; joint < jointNodes.length; joint++) {
            int nodeIndex = jointNodes[joint];
            JsonObject node = requireObject(nodes.get(nodeIndex), "nodes[" + nodeIndex + "]");
            names[joint] = node.has("name") && node.get("name").isJsonPrimitive()
                    ? node.get("name").getAsString() : "node_" + nodeIndex;
            rest[joint] = nodeTransform(node, nodeIndex);
            int parentNode = nodeParents[nodeIndex];
            int parentJoint = -1;
            while (parentNode >= 0) {
                Integer palette = nodeToJoint.get(parentNode);
                if (palette != null) {
                    parentJoint = palette;
                    break;
                }
                parentNode = nodeParents[parentNode];
            }
            parentJoints[joint] = parentJoint;
        }

        Affine3x4[] inverseBind = new Affine3x4[jointNodes.length];
        if (skin.has("inverseBindMatrices")) {
            float[] matrices = reader.readFloat(requireInt(skin, "inverseBindMatrices"), "MAT4", 16, false);
            if (matrices.length != jointNodes.length * 16) {
                throw new IllegalArgumentException("inverseBindMatrices count must equal skin joint count");
            }
            for (int joint = 0; joint < jointNodes.length; joint++) {
                inverseBind[joint] = affineFromColumnMajor(matrices, joint * 16, "inverseBindMatrices[" + joint + "]");
            }
        } else {
            Arrays.fill(inverseBind, Affine3x4.identity());
        }

        SkinnedTriangleMesh mesh = new SkinnedTriangleMesh(positions, normals, uvs, indices, joints, weights);
        JointRig rig = new JointRig(jointNodes, parentJoints, names, inverseBind, rest);
        return new SkinnedMeshAsset(mesh, rig);
    }

    public static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static JsonObject parseUtf8(byte[] bytes) {
        try {
            String json = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                throw new IllegalArgumentException("glTF root must be an object");
            }
            return root.getAsJsonObject();
        } catch (CharacterCodingException | RuntimeException exception) {
            if (exception instanceof IllegalArgumentException illegal) {
                throw illegal;
            }
            throw new IllegalArgumentException("invalid UTF-8 glTF JSON", exception);
        }
    }

    private static void rejectArtPayload(JsonObject root) {
        for (String key : new String[]{"materials", "textures", "images", "samplers"}) {
            if (root.has(key) && root.get(key).isJsonArray() && !root.getAsJsonArray(key).isEmpty()) {
                throw new IllegalArgumentException("art-neutral derivation must not contain " + key);
            }
        }
    }

    private static JsonObject singleTrianglePrimitive(JsonObject mesh) {
        JsonArray primitives = requireArray(mesh, "primitives");
        if (primitives.size() != 1) {
            throw new IllegalArgumentException("exactly one mesh primitive is required");
        }
        JsonObject primitive = requireObject(primitives.get(0), "meshes[0].primitives[0]");
        if (primitive.has("material")) {
            throw new IllegalArgumentException("art-neutral derivation must not bind a material");
        }
        if (primitive.has("targets")) {
            throw new IllegalArgumentException("morph targets are not supported by this runtime importer");
        }
        int mode = primitive.has("mode") ? requireInt(primitive, "mode") : 4;
        if (mode != 4) {
            throw new IllegalArgumentException("only TRIANGLES primitive mode is supported");
        }
        return primitive;
    }

    private static byte[] decodeEmbeddedBuffer(JsonObject buffer) {
        if (!buffer.has("uri") || !buffer.get("uri").isJsonPrimitive()) {
            throw new IllegalArgumentException("buffer must be an embedded data URI");
        }
        String uri = buffer.get("uri").getAsString();
        String marker = ";base64,";
        int split = uri.indexOf(marker);
        if (!uri.startsWith("data:") || split < 0) {
            throw new IllegalArgumentException("external or non-base64 buffers are not accepted");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(uri.substring(split + marker.length()));
            if (buffer.has("byteLength") && requireInt(buffer, "byteLength") != bytes.length) {
                throw new IllegalArgumentException("embedded buffer byteLength does not match decoded payload");
            }
            return bytes;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("invalid embedded base64 buffer", exception);
        }
    }

    private static int[] deriveNodeParents(JsonArray nodes) {
        int[] parents = new int[nodes.size()];
        Arrays.fill(parents, -1);
        for (int parent = 0; parent < nodes.size(); parent++) {
            JsonObject node = requireObject(nodes.get(parent), "nodes[" + parent + "]");
            if (!node.has("children")) {
                continue;
            }
            JsonArray children = requireArray(node, "children");
            for (JsonElement childElement : children) {
                int child = requireInt(childElement, "node child");
                checkIndex(child, nodes.size(), "node child");
                if (parents[child] != -1) {
                    throw new IllegalArgumentException("node has multiple parents: " + child);
                }
                parents[child] = parent;
            }
        }
        for (int start = 0; start < parents.length; start++) {
            boolean[] seen = new boolean[parents.length];
            int cursor = start;
            while (cursor >= 0) {
                if (seen[cursor]) {
                    throw new IllegalArgumentException("node hierarchy contains a cycle at node " + cursor);
                }
                seen[cursor] = true;
                cursor = parents[cursor];
            }
        }
        return parents;
    }

    private static Affine3x4 nodeTransform(JsonObject node, int nodeIndex) {
        boolean hasMatrix = node.has("matrix");
        boolean hasTrs = node.has("translation") || node.has("rotation") || node.has("scale");
        if (hasMatrix && hasTrs) {
            throw new IllegalArgumentException("node " + nodeIndex + " mixes matrix with TRS");
        }
        if (hasMatrix) {
            float[] values = jsonFloatArray(node.get("matrix"), 16, "nodes[" + nodeIndex + "].matrix");
            return affineFromColumnMajor(values, 0, "nodes[" + nodeIndex + "].matrix");
        }
        float[] t = node.has("translation") ? jsonFloatArray(node.get("translation"), 3, "translation") : new float[]{0, 0, 0};
        float[] q = node.has("rotation") ? jsonFloatArray(node.get("rotation"), 4, "rotation") : new float[]{0, 0, 0, 1};
        float[] s = node.has("scale") ? jsonFloatArray(node.get("scale"), 3, "scale") : new float[]{1, 1, 1};
        float qLen = (float) Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        if (!(qLen > 1.0e-8f) || !Float.isFinite(qLen)) {
            throw new IllegalArgumentException("node rotation quaternion is degenerate");
        }
        float x = q[0] / qLen, y = q[1] / qLen, z = q[2] / qLen, w = q[3] / qLen;
        float r00 = 1 - 2 * (y * y + z * z), r01 = 2 * (x * y - z * w), r02 = 2 * (x * z + y * w);
        float r10 = 2 * (x * y + z * w), r11 = 1 - 2 * (x * x + z * z), r12 = 2 * (y * z - x * w);
        float r20 = 2 * (x * z - y * w), r21 = 2 * (y * z + x * w), r22 = 1 - 2 * (x * x + y * y);
        return new Affine3x4(
                r00 * s[0], r01 * s[1], r02 * s[2], t[0],
                r10 * s[0], r11 * s[1], r12 * s[2], t[1],
                r20 * s[0], r21 * s[1], r22 * s[2], t[2]
        );
    }

    private static Affine3x4 affineFromColumnMajor(float[] v, int offset, String label) {
        float epsilon = 1.0e-5f;
        if (Math.abs(v[offset + 3]) > epsilon || Math.abs(v[offset + 7]) > epsilon
                || Math.abs(v[offset + 11]) > epsilon || Math.abs(v[offset + 15] - 1.0f) > epsilon) {
            throw new IllegalArgumentException(label + " is not an affine glTF matrix");
        }
        return new Affine3x4(
                v[offset], v[offset + 4], v[offset + 8], v[offset + 12],
                v[offset + 1], v[offset + 5], v[offset + 9], v[offset + 13],
                v[offset + 2], v[offset + 6], v[offset + 10], v[offset + 14]
        );
    }

    private static int attribute(JsonObject attributes, String key) {
        if (!attributes.has(key)) {
            throw new IllegalArgumentException("required mesh attribute is missing: " + key);
        }
        return requireInt(attributes, key);
    }

    private static JsonArray requireArray(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            throw new IllegalArgumentException(key + " must be an array");
        }
        return object.getAsJsonArray(key);
    }

    private static JsonArray requireArray(JsonElement element, String label) {
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException(label + " must be an array");
        }
        return element.getAsJsonArray();
    }

    private static JsonObject requireObject(JsonElement element, String label) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException(label + " must be an object");
        }
        return element.getAsJsonObject();
    }

    private static int requireInt(JsonObject object, String key) {
        if (!object.has(key)) {
            throw new IllegalArgumentException(key + " is required");
        }
        return requireInt(object.get(key), key);
    }

    private static int requireInt(JsonElement element, String label) {
        try {
            if (element == null || !element.isJsonPrimitive()) {
                throw new IllegalArgumentException(label + " must be an integer");
            }
            int value = element.getAsInt();
            if (value < 0 || element.getAsDouble() != value) {
                throw new IllegalArgumentException(label + " must be a non-negative integer");
            }
            return value;
        } catch (RuntimeException exception) {
            if (exception instanceof IllegalArgumentException illegal) {
                throw illegal;
            }
            throw new IllegalArgumentException(label + " must be an integer", exception);
        }
    }

    private static void checkIndex(int index, int size, String label) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException(label + " index outside range: " + index);
        }
    }

    private static float[] jsonFloatArray(JsonElement element, int expected, String label) {
        JsonArray array = requireArray(element, label);
        if (array.size() != expected) {
            throw new IllegalArgumentException(label + " must contain " + expected + " components");
        }
        float[] result = new float[expected];
        for (int i = 0; i < expected; i++) {
            result[i] = array.get(i).getAsFloat();
            if (!Float.isFinite(result[i])) {
                throw new IllegalArgumentException(label + " contains a non-finite component");
            }
        }
        return result;
    }

    private static final class AccessorReader {
        private final JsonArray accessors;
        private final JsonArray views;
        private final byte[] buffer;

        private AccessorReader(JsonObject root, byte[] buffer) {
            this.accessors = requireArray(root, "accessors");
            this.views = requireArray(root, "bufferViews");
            this.buffer = buffer;
        }

        float[] readFloat(int accessorIndex, String expectedType, int components, boolean allowNormalized) {
            Accessor accessor = accessor(accessorIndex, expectedType, components);
            if (accessor.componentType != FLOAT) {
                throw new IllegalArgumentException("accessor " + accessorIndex + " must use FLOAT components");
            }
            if (accessor.normalized && !allowNormalized) {
                throw new IllegalArgumentException("normalized float accessor is not accepted");
            }
            float[] result = new float[accessor.count * components];
            ByteBuffer bytes = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
            for (int item = 0; item < accessor.count; item++) {
                int base = accessor.start + item * accessor.stride;
                for (int component = 0; component < components; component++) {
                    float value = bytes.getFloat(base + component * 4);
                    if (!Float.isFinite(value)) {
                        throw new IllegalArgumentException("accessor contains non-finite float");
                    }
                    result[item * components + component] = value;
                }
            }
            return result;
        }

        int[] readUnsigned(int accessorIndex, String expectedType, int components) {
            Accessor accessor = accessor(accessorIndex, expectedType, components);
            int componentBytes;
            if (accessor.componentType == UNSIGNED_BYTE) componentBytes = 1;
            else if (accessor.componentType == UNSIGNED_SHORT) componentBytes = 2;
            else if (accessor.componentType == UNSIGNED_INT) componentBytes = 4;
            else throw new IllegalArgumentException("accessor " + accessorIndex + " must use an unsigned integer component type");
            if (accessor.normalized) {
                throw new IllegalArgumentException("normalized integer accessor is not accepted for joints/indices");
            }
            int[] result = new int[accessor.count * components];
            ByteBuffer bytes = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
            for (int item = 0; item < accessor.count; item++) {
                int base = accessor.start + item * accessor.stride;
                for (int component = 0; component < components; component++) {
                    int offset = base + component * componentBytes;
                    long value = switch (componentBytes) {
                        case 1 -> Byte.toUnsignedInt(bytes.get(offset));
                        case 2 -> Short.toUnsignedInt(bytes.getShort(offset));
                        case 4 -> Integer.toUnsignedLong(bytes.getInt(offset));
                        default -> throw new IllegalStateException();
                    };
                    if (value > Integer.MAX_VALUE) {
                        throw new IllegalArgumentException("unsigned accessor value exceeds runtime integer range");
                    }
                    result[item * components + component] = (int) value;
                }
            }
            return result;
        }

        private Accessor accessor(int accessorIndex, String expectedType, int components) {
            checkIndex(accessorIndex, accessors.size(), "accessor");
            JsonObject accessor = requireObject(accessors.get(accessorIndex), "accessors[" + accessorIndex + "]");
            if (accessor.has("sparse")) {
                throw new IllegalArgumentException("sparse accessors are not supported");
            }
            String type = accessor.has("type") ? accessor.get("type").getAsString() : "";
            if (!expectedType.equals(type)) {
                throw new IllegalArgumentException("accessor " + accessorIndex + " type " + type + " != " + expectedType);
            }
            int count = requireInt(accessor, "count");
            int componentType = requireInt(accessor, "componentType");
            boolean normalized = accessor.has("normalized") && accessor.get("normalized").getAsBoolean();
            int viewIndex = requireInt(accessor, "bufferView");
            checkIndex(viewIndex, views.size(), "bufferView");
            JsonObject view = requireObject(views.get(viewIndex), "bufferViews[" + viewIndex + "]");
            if (requireInt(view, "buffer") != 0) {
                throw new IllegalArgumentException("only embedded buffer 0 is supported");
            }
            int componentBytes = componentType == FLOAT || componentType == UNSIGNED_INT ? 4
                    : componentType == UNSIGNED_SHORT ? 2 : componentType == UNSIGNED_BYTE ? 1 : -1;
            if (componentBytes < 0) {
                throw new IllegalArgumentException("unsupported accessor componentType: " + componentType);
            }
            int packed = components * componentBytes;
            int stride = view.has("byteStride") ? requireInt(view, "byteStride") : packed;
            if (stride < packed) {
                throw new IllegalArgumentException("bufferView byteStride is smaller than packed accessor size");
            }
            int viewOffset = view.has("byteOffset") ? requireInt(view, "byteOffset") : 0;
            int accessorOffset = accessor.has("byteOffset") ? requireInt(accessor, "byteOffset") : 0;
            int start = viewOffset + accessorOffset;
            long requiredEnd = count == 0 ? start : (long) start + (long) (count - 1) * stride + packed;
            int viewLength = requireInt(view, "byteLength");
            if (start < viewOffset || requiredEnd > (long) viewOffset + viewLength || requiredEnd > buffer.length) {
                throw new IllegalArgumentException("accessor exceeds its bufferView or embedded buffer bounds");
            }
            return new Accessor(count, componentType, normalized, start, stride);
        }
    }

    private record Accessor(int count, int componentType, boolean normalized, int start, int stride) {
    }
}

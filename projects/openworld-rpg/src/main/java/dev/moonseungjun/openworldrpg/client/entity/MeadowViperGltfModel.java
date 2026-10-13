package dev.moonseungjun.openworldrpg.client.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Exact runtime reader/skinner for the pinned Quaternius Snake.glb. */
final class MeadowViperGltfModel {
    static final Identifier RESOURCE = Identifier.fromNamespaceAndPath(
            "openworld_rpg",
            "models/entity/meadow_viper/snake.glb"
    );
    static final String SOURCE_SHA256 =
            "70b4af1088dec939e167b07c0a5e69992a4851beac8bd24a9da1189b65ae101c";

    private static final int GLB_MAGIC = 0x46546C67;
    private static final int JSON_CHUNK = 0x4E4F534A;
    private static final int BIN_CHUNK = 0x004E4942;

    private final Node[] nodes;
    private final int[] jointNodes;
    private final Matrix4f[] inverseBind;
    private final Primitive[] primitives;
    private final Map<String, Clip> clips;

    private MeadowViperGltfModel(
            Node[] nodes,
            int[] jointNodes,
            Matrix4f[] inverseBind,
            Primitive[] primitives,
            Map<String, Clip> clips
    ) {
        this.nodes = nodes;
        this.jointNodes = jointNodes;
        this.inverseBind = inverseBind;
        this.primitives = primitives;
        this.clips = Map.copyOf(clips);
    }

    static MeadowViperGltfModel load() {
        try {
            var resource = Minecraft.getInstance()
                    .getResourceManager()
                    .getResource(RESOURCE)
                    .orElseThrow(() -> new IllegalStateException(
                            "Pinned Meadow Viper GLB is missing from runtime resources: " + RESOURCE
                    ));
            byte[] bytes;
            try (InputStream input = resource.open()) {
                bytes = input.readAllBytes();
            }
            verifySource(bytes);
            return parse(bytes);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read pinned Meadow Viper GLB.", exception);
        }
    }

    void emit(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            MeadowViperRenderState state,
            int light
    ) {
        AnimationSelection selection = AnimationSelection.forState(state);
        Clip clip = clips.get(selection.clipName());
        if (clip == null) {
            throw new IllegalStateException("Pinned Meadow Viper clip missing: " + selection.clipName());
        }

        Transform[] local = new Transform[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            local[i] = nodes[i].base().copy();
        }
        clip.apply(local, selection.clipTimeSeconds());

        Matrix4f[] global = new Matrix4f[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            resolveGlobal(i, local, global);
        }

        Matrix4f[] skin = new Matrix4f[jointNodes.length];
        for (int i = 0; i < jointNodes.length; i++) {
            skin[i] = new Matrix4f(global[jointNodes[i]]).mul(inverseBind[i]);
        }

        for (Primitive primitive : primitives) {
            for (int triangle = 0; triangle < primitive.indices().length; triangle += 3) {
                int a = primitive.indices()[triangle];
                int b = primitive.indices()[triangle + 1];
                int c = primitive.indices()[triangle + 2];
                emitVertex(pose, vertices, primitive, a, skin, light);
                emitVertex(pose, vertices, primitive, b, skin, light);
                emitVertex(pose, vertices, primitive, c, skin, light);
                // entity cutout is quad geometry in this render path.
                emitVertex(pose, vertices, primitive, c, skin, light);
            }
        }
    }

    private static void emitVertex(
            PoseStack.Pose pose,
            VertexConsumer out,
            Primitive primitive,
            int vertex,
            Matrix4f[] skin,
            int light
    ) {
        Vector3f position = new Vector3f();
        Vector3f normal = new Vector3f();
        int p = vertex * 3;
        int j = vertex * 4;
        Vector3f sourcePosition = new Vector3f(
                primitive.positions()[p],
                primitive.positions()[p + 1],
                primitive.positions()[p + 2]
        );
        Vector3f sourceNormal = new Vector3f(
                primitive.normals()[p],
                primitive.normals()[p + 1],
                primitive.normals()[p + 2]
        );

        for (int influence = 0; influence < 4; influence++) {
            float weight = primitive.weights()[j + influence];
            if (weight <= 0.0F) {
                continue;
            }
            int joint = primitive.joints()[j + influence];
            if (joint < 0 || joint >= skin.length) {
                throw new IllegalStateException("Meadow Viper GLB joint index is outside pinned skin.");
            }
            Vector3f weightedPosition = new Vector3f(sourcePosition);
            skin[joint].transformPosition(weightedPosition);
            position.fma(weight, weightedPosition);

            Vector3f weightedNormal = new Vector3f(sourceNormal);
            skin[joint].transformDirection(weightedNormal);
            normal.fma(weight, weightedNormal);
        }
        if (normal.lengthSquared() > 1.0e-8F) {
            normal.normalize();
        } else {
            normal.set(sourceNormal).normalize();
        }

        out.addVertex(pose, position.x, position.y, position.z)
                .setColor(primitive.argb())
                .setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, normal.x, normal.y, normal.z);
    }

    private Matrix4f resolveGlobal(
            int index,
            Transform[] local,
            Matrix4f[] global
    ) {
        if (global[index] != null) {
            return global[index];
        }
        Matrix4f own = local[index].matrix();
        int parent = nodes[index].parent();
        global[index] = parent < 0
                ? own
                : new Matrix4f(resolveGlobal(parent, local, global)).mul(own);
        return global[index];
    }

    private static MeadowViperGltfModel parse(byte[] bytes) {
        ByteBuffer glb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (glb.getInt() != GLB_MAGIC || glb.getInt() != 2 || glb.getInt() != bytes.length) {
            throw new IllegalStateException("Pinned Meadow Viper source is not the admitted GLB v2 payload.");
        }

        byte[] jsonBytes = null;
        byte[] binary = null;
        while (glb.remaining() >= 8) {
            int length = glb.getInt();
            int type = glb.getInt();
            if (length < 0 || length > glb.remaining()) {
                throw new IllegalStateException("Malformed Meadow Viper GLB chunk length.");
            }
            byte[] chunk = new byte[length];
            glb.get(chunk);
            if (type == JSON_CHUNK) {
                jsonBytes = chunk;
            } else if (type == BIN_CHUNK) {
                binary = chunk;
            }
        }
        if (jsonBytes == null || binary == null) {
            throw new IllegalStateException("Pinned Meadow Viper GLB requires JSON and BIN chunks.");
        }

        String jsonText = new String(jsonBytes, java.nio.charset.StandardCharsets.UTF_8)
                .replace("\u0000", "")
                .trim();
        JsonObject root = JsonParser.parseString(jsonText).getAsJsonObject();
        JsonArray nodeJson = root.getAsJsonArray("nodes");
        Node[] nodes = parseNodes(nodeJson);
        Access access = new Access(root, binary);

        JsonObject skinJson = root.getAsJsonArray("skins").get(0).getAsJsonObject();
        int[] joints = ints(skinJson.getAsJsonArray("joints"));
        float[] ibmFlat = access.floats(skinJson.get("inverseBindMatrices").getAsInt());
        if (ibmFlat.length != joints.length * 16) {
            throw new IllegalStateException("Pinned Meadow Viper inverse-bind matrix count changed.");
        }
        Matrix4f[] inverseBind = new Matrix4f[joints.length];
        for (int i = 0; i < joints.length; i++) {
            int o = i * 16;
            inverseBind[i] = new Matrix4f(
                    ibmFlat[o], ibmFlat[o + 1], ibmFlat[o + 2], ibmFlat[o + 3],
                    ibmFlat[o + 4], ibmFlat[o + 5], ibmFlat[o + 6], ibmFlat[o + 7],
                    ibmFlat[o + 8], ibmFlat[o + 9], ibmFlat[o + 10], ibmFlat[o + 11],
                    ibmFlat[o + 12], ibmFlat[o + 13], ibmFlat[o + 14], ibmFlat[o + 15]
            );
        }

        JsonArray materials = root.getAsJsonArray("materials");
        JsonArray primitiveJson = root.getAsJsonArray("meshes")
                .get(0).getAsJsonObject().getAsJsonArray("primitives");
        Primitive[] primitives = new Primitive[primitiveJson.size()];
        for (int i = 0; i < primitiveJson.size(); i++) {
            JsonObject primitive = primitiveJson.get(i).getAsJsonObject();
            JsonObject attributes = primitive.getAsJsonObject("attributes");
            float[] positions = access.floats(attributes.get("POSITION").getAsInt());
            float[] normals = access.floats(attributes.get("NORMAL").getAsInt());
            int[] jointIndices = access.ints(attributes.get("JOINTS_0").getAsInt());
            float[] weights = access.floats(attributes.get("WEIGHTS_0").getAsInt());
            int[] indices = access.ints(primitive.get("indices").getAsInt());
            int material = primitive.get("material").getAsInt();
            primitives[i] = new Primitive(
                    positions,
                    normals,
                    jointIndices,
                    weights,
                    indices,
                    materialArgb(materials.get(material).getAsJsonObject())
            );
        }

        Map<String, Clip> clips = parseAnimations(root.getAsJsonArray("animations"), access);
        requirePinnedContract(nodes, joints, primitives, clips);
        return new MeadowViperGltfModel(nodes, joints, inverseBind, primitives, clips);
    }

    private static Node[] parseNodes(JsonArray source) {
        Node[] nodes = new Node[source.size()];
        int[] parents = new int[source.size()];
        Arrays.fill(parents, -1);
        for (int i = 0; i < source.size(); i++) {
            JsonObject node = source.get(i).getAsJsonObject();
            if (node.has("children")) {
                for (JsonElement child : node.getAsJsonArray("children")) {
                    parents[child.getAsInt()] = i;
                }
            }
        }
        for (int i = 0; i < source.size(); i++) {
            JsonObject node = source.get(i).getAsJsonObject();
            nodes[i] = new Node(
                    node.has("name") ? node.get("name").getAsString() : "node_" + i,
                    parents[i],
                    new Transform(
                            vec3(node.getAsJsonArray("translation"), 0.0F, 0.0F, 0.0F),
                            quat(node.getAsJsonArray("rotation")),
                            vec3(node.getAsJsonArray("scale"), 1.0F, 1.0F, 1.0F)
                    )
            );
        }
        return nodes;
    }

    private static Map<String, Clip> parseAnimations(JsonArray animations, Access access) {
        Map<String, Clip> result = new HashMap<>();
        for (JsonElement element : animations) {
            JsonObject animation = element.getAsJsonObject();
            JsonArray samplers = animation.getAsJsonArray("samplers");
            List<Channel> channels = new ArrayList<>();
            float duration = 0.0F;
            for (JsonElement channelElement : animation.getAsJsonArray("channels")) {
                JsonObject channelJson = channelElement.getAsJsonObject();
                JsonObject target = channelJson.getAsJsonObject("target");
                JsonObject sampler = samplers.get(channelJson.get("sampler").getAsInt())
                        .getAsJsonObject();
                String interpolation = sampler.has("interpolation")
                        ? sampler.get("interpolation").getAsString()
                        : "LINEAR";
                if (!"LINEAR".equals(interpolation)) {
                    throw new IllegalStateException("Pinned Meadow Viper animation interpolation changed: " + interpolation);
                }
                float[] times = access.floats(sampler.get("input").getAsInt());
                float[] values = access.floats(sampler.get("output").getAsInt());
                if (times.length > 0) {
                    duration = Math.max(duration, times[times.length - 1]);
                }
                channels.add(new Channel(
                        target.get("node").getAsInt(),
                        Path.valueOf(target.get("path").getAsString().toUpperCase(Locale.ROOT)),
                        times,
                        values
                ));
            }
            String name = animation.get("name").getAsString();
            result.put(name, new Clip(name, duration, List.copyOf(channels)));
        }
        return result;
    }

    private static void requirePinnedContract(
            Node[] nodes,
            int[] joints,
            Primitive[] primitives,
            Map<String, Clip> clips
    ) {
        if (nodes.length != 23 || joints.length != 15 || primitives.length != 7) {
            throw new IllegalStateException(
                    "Pinned Meadow Viper GLB structure drifted: nodes=" + nodes.length
                            + " joints=" + joints.length + " primitives=" + primitives.length
            );
        }
        requireClip(clips, "SnakeArmature|Snake_Attack", 0.666667F, 12);
        requireClip(clips, "SnakeArmature|Snake_Idle", 2.083333F, 10);
        requireClip(clips, "SnakeArmature|Snake_Jump", 0.958333F, 12);
        requireClip(clips, "SnakeArmature|Snake_Walk", 0.833333F, 13);
    }

    private static void requireClip(
            Map<String, Clip> clips,
            String name,
            float expectedDuration,
            int expectedChannels
    ) {
        Clip clip = clips.get(name);
        if (clip == null
                || Math.abs(clip.duration() - expectedDuration) > 0.00001F
                || clip.channels().size() != expectedChannels) {
            throw new IllegalStateException("Pinned Meadow Viper animation contract drifted: " + name);
        }
    }

    private static int materialArgb(JsonObject material) {
        JsonArray color = material.getAsJsonObject("pbrMetallicRoughness")
                .getAsJsonArray("baseColorFactor");
        int r = Math.round(color.get(0).getAsFloat() * 255.0F);
        int g = Math.round(color.get(1).getAsFloat() * 255.0F);
        int b = Math.round(color.get(2).getAsFloat() * 255.0F);
        int a = Math.round(color.get(3).getAsFloat() * 255.0F);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static Vector3f vec3(JsonArray values, float x, float y, float z) {
        return values == null
                ? new Vector3f(x, y, z)
                : new Vector3f(values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat());
    }

    private static Quaternionf quat(JsonArray values) {
        return values == null
                ? new Quaternionf()
                : new Quaternionf(
                        values.get(0).getAsFloat(),
                        values.get(1).getAsFloat(),
                        values.get(2).getAsFloat(),
                        values.get(3).getAsFloat()
                );
    }

    private static int[] ints(JsonArray array) {
        int[] result = new int[array.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = array.get(i).getAsInt();
        }
        return result;
    }

    private static void verifySource(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format(Locale.ROOT, "%02x", value & 0xFF));
            }
            if (!SOURCE_SHA256.contentEquals(hex)) {
                throw new IllegalStateException(
                        "Runtime Meadow Viper GLB does not match the pinned CC0 source: " + hex
                );
            }
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable.", impossible);
        }
    }

    private record Node(String name, int parent, Transform base) {
    }

    private record Primitive(
            float[] positions,
            float[] normals,
            int[] joints,
            float[] weights,
            int[] indices,
            int argb
    ) {
    }

    private enum Path {
        TRANSLATION,
        ROTATION,
        SCALE
    }

    private record Channel(int node, Path path, float[] times, float[] values) {
        void apply(Transform target, float time) {
            if (times.length == 0) {
                return;
            }
            int components = path == Path.ROTATION ? 4 : 3;
            int lower = 0;
            if (time >= times[times.length - 1]) {
                lower = times.length - 1;
            } else {
                while (lower + 1 < times.length && times[lower + 1] <= time) {
                    lower++;
                }
            }
            int upper = Math.min(lower + 1, times.length - 1);
            float span = times[upper] - times[lower];
            float alpha = span <= 1.0e-8F ? 0.0F : (time - times[lower]) / span;
            alpha = Math.max(0.0F, Math.min(1.0F, alpha));
            int lo = lower * components;
            int hi = upper * components;
            switch (path) {
                case TRANSLATION -> target.translation.set(
                        lerp(values[lo], values[hi], alpha),
                        lerp(values[lo + 1], values[hi + 1], alpha),
                        lerp(values[lo + 2], values[hi + 2], alpha)
                );
                case SCALE -> target.scale.set(
                        lerp(values[lo], values[hi], alpha),
                        lerp(values[lo + 1], values[hi + 1], alpha),
                        lerp(values[lo + 2], values[hi + 2], alpha)
                );
                case ROTATION -> {
                    Quaternionf a = new Quaternionf(values[lo], values[lo + 1], values[lo + 2], values[lo + 3]);
                    Quaternionf b = new Quaternionf(values[hi], values[hi + 1], values[hi + 2], values[hi + 3]);
                    target.rotation.set(a).slerp(b, alpha).normalize();
                }
            }
        }
    }

    private record Clip(String name, float duration, List<Channel> channels) {
        void apply(Transform[] local, float time) {
            float clamped = Math.max(0.0F, Math.min(duration, time));
            for (Channel channel : channels) {
                channel.apply(local[channel.node()], clamped);
            }
        }
    }

    private static final class Transform {
        private final Vector3f translation;
        private final Quaternionf rotation;
        private final Vector3f scale;

        private Transform(Vector3f translation, Quaternionf rotation, Vector3f scale) {
            this.translation = new Vector3f(translation);
            this.rotation = new Quaternionf(rotation);
            this.scale = new Vector3f(scale);
        }

        private Transform copy() {
            return new Transform(translation, rotation, scale);
        }

        private Matrix4f matrix() {
            return new Matrix4f()
                    .identity()
                    .translate(translation)
                    .rotate(rotation)
                    .scale(scale);
        }
    }

    private static final class Access {
        private final JsonArray accessors;
        private final JsonArray bufferViews;
        private final ByteBuffer binary;

        private Access(JsonObject root, byte[] binary) {
            this.accessors = root.getAsJsonArray("accessors");
            this.bufferViews = root.getAsJsonArray("bufferViews");
            this.binary = ByteBuffer.wrap(binary).order(ByteOrder.LITTLE_ENDIAN);
        }

        private float[] floats(int accessorIndex) {
            JsonObject accessor = accessors.get(accessorIndex).getAsJsonObject();
            int componentType = accessor.get("componentType").getAsInt();
            if (componentType != 5126) {
                throw new IllegalStateException("Expected FLOAT accessor in pinned Meadow Viper GLB.");
            }
            int count = accessor.get("count").getAsInt();
            int components = components(accessor.get("type").getAsString());
            int start = start(accessor);
            int stride = stride(accessor, components * Float.BYTES);
            float[] result = new float[count * components];
            for (int row = 0; row < count; row++) {
                int rowStart = start + row * stride;
                for (int component = 0; component < components; component++) {
                    result[row * components + component] = binary.getFloat(rowStart + component * Float.BYTES);
                }
            }
            return result;
        }

        private int[] ints(int accessorIndex) {
            JsonObject accessor = accessors.get(accessorIndex).getAsJsonObject();
            int componentType = accessor.get("componentType").getAsInt();
            int componentBytes = switch (componentType) {
                case 5121 -> 1;
                case 5123 -> 2;
                case 5125 -> 4;
                default -> throw new IllegalStateException("Unsupported integer accessor in pinned Meadow Viper GLB: " + componentType);
            };
            int count = accessor.get("count").getAsInt();
            int components = components(accessor.get("type").getAsString());
            int start = start(accessor);
            int stride = stride(accessor, components * componentBytes);
            int[] result = new int[count * components];
            for (int row = 0; row < count; row++) {
                int rowStart = start + row * stride;
                for (int component = 0; component < components; component++) {
                    int offset = rowStart + component * componentBytes;
                    result[row * components + component] = switch (componentType) {
                        case 5121 -> Byte.toUnsignedInt(binary.get(offset));
                        case 5123 -> Short.toUnsignedInt(binary.getShort(offset));
                        case 5125 -> binary.getInt(offset);
                        default -> throw new AssertionError();
                    };
                }
            }
            return result;
        }

        private int start(JsonObject accessor) {
            JsonObject view = bufferViews.get(accessor.get("bufferView").getAsInt()).getAsJsonObject();
            return (view.has("byteOffset") ? view.get("byteOffset").getAsInt() : 0)
                    + (accessor.has("byteOffset") ? accessor.get("byteOffset").getAsInt() : 0);
        }

        private int stride(JsonObject accessor, int packed) {
            JsonObject view = bufferViews.get(accessor.get("bufferView").getAsInt()).getAsJsonObject();
            return view.has("byteStride") ? view.get("byteStride").getAsInt() : packed;
        }

        private static int components(String type) {
            return switch (type) {
                case "SCALAR" -> 1;
                case "VEC2" -> 2;
                case "VEC3" -> 3;
                case "VEC4" -> 4;
                case "MAT4" -> 16;
                default -> throw new IllegalStateException("Unsupported GLB accessor type: " + type);
            };
        }
    }

    private record AnimationSelection(String clipName, float clipTimeSeconds) {
        private static final String ATTACK = "SnakeArmature|Snake_Attack";
        private static final String IDLE = "SnakeArmature|Snake_Idle";
        private static final String JUMP = "SnakeArmature|Snake_Jump";
        private static final String WALK = "SnakeArmature|Snake_Walk";

        private static AnimationSelection forState(MeadowViperRenderState state) {
            if (!state.onGround && state.phase != dev.moonseungjun.openworldrpg.entity.MeadowViperEntity.Phase.BITE_ACTIVE) {
                return loop(JUMP, state.ageSeconds, 0.958333F);
            }
            return switch (state.phase) {
                case WARNING, BITE_WINDUP -> new AnimationSelection(
                        ATTACK,
                        Math.min(1.0F, state.phaseAgeSeconds / 0.45F) * 0.333333F
                );
                case BITE_ACTIVE -> new AnimationSelection(
                        ATTACK,
                        0.333333F + Math.min(1.0F, state.phaseAgeSeconds / 0.15F) * (0.416667F - 0.333333F)
                );
                case RECOVERY -> new AnimationSelection(
                        ATTACK,
                        0.416667F + Math.min(1.0F, state.phaseAgeSeconds / 0.55F) * (0.666667F - 0.416667F)
                );
                case SPACING -> loop(WALK, state.ageSeconds, 0.833333F);
                case NEUTRAL -> state.horizontalSpeed > 0.01F
                        ? loop(WALK, state.ageSeconds, 0.833333F)
                        : loop(IDLE, state.ageSeconds, 2.083333F);
            };
        }

        private static AnimationSelection loop(String name, float age, float duration) {
            float time = duration <= 0.0F ? 0.0F : age % duration;
            return new AnimationSelection(name, time);
        }
    }

    private static float lerp(float a, float b, float alpha) {
        return a + (b - a) * alpha;
    }
}

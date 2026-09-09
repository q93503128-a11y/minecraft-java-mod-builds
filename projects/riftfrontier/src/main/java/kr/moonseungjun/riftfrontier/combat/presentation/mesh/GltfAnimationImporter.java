package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Deterministic animation-channel importer for the accepted embedded glTF derivation. */
public final class GltfAnimationImporter {
    private static final int FLOAT = 5126;

    private GltfAnimationImporter() {
    }

    public static List<AnimationClip> importClips(byte[] gltfUtf8, JointRig rig) {
        if (gltfUtf8 == null || gltfUtf8.length == 0 || rig == null) {
            throw new IllegalArgumentException("glTF bytes and rig are required");
        }
        JsonElement parsed = JsonParser.parseString(new String(gltfUtf8, StandardCharsets.UTF_8));
        if (!parsed.isJsonObject()) {
            throw new IllegalArgumentException("glTF root must be an object");
        }
        JsonObject root = parsed.getAsJsonObject();
        JsonArray buffers = array(root, "buffers");
        if (buffers.size() != 1) {
            throw new IllegalArgumentException("animation import requires exactly one embedded buffer");
        }
        byte[] binary = embeddedBuffer(object(buffers.get(0), "buffers[0]"));
        Accessors accessors = new Accessors(root, binary);

        Map<Integer, Integer> nodeToJoint = new HashMap<>();
        for (int joint = 0; joint < rig.jointCount(); joint++) {
            nodeToJoint.put(rig.nodeIndex(joint), joint);
        }

        JsonArray animations = array(root, "animations");
        if (animations.isEmpty()) {
            throw new IllegalArgumentException("glTF contains no animation clips");
        }
        List<AnimationClip> result = new ArrayList<>(animations.size());
        for (int animationIndex = 0; animationIndex < animations.size(); animationIndex++) {
            JsonObject animation = object(animations.get(animationIndex), "animations[" + animationIndex + "]");
            String name = animation.has("name") ? animation.get("name").getAsString() : "animation_" + animationIndex;
            JsonArray samplers = array(animation, "samplers");
            JsonArray channels = array(animation, "channels");
            List<AnimationClip.Channel> importedChannels = new ArrayList<>(channels.size());
            float duration = 0.0f;

            for (int channelIndex = 0; channelIndex < channels.size(); channelIndex++) {
                JsonObject channel = object(channels.get(channelIndex), "animation channel");
                int samplerIndex = integer(channel, "sampler");
                checkIndex(samplerIndex, samplers.size(), "animation sampler");
                JsonObject sampler = object(samplers.get(samplerIndex), "animation sampler");
                JsonObject target = object(channel.get("target"), "animation target");
                int targetNode = integer(target, "node");
                Integer joint = nodeToJoint.get(targetNode);
                if (joint == null) {
                    throw new IllegalArgumentException("animation targets node outside imported skin palette: " + targetNode);
                }
                AnimationClip.Path path = switch (string(target, "path")) {
                    case "translation" -> AnimationClip.Path.TRANSLATION;
                    case "rotation" -> AnimationClip.Path.ROTATION;
                    case "scale" -> AnimationClip.Path.SCALE;
                    default -> throw new IllegalArgumentException("unsupported animation target path");
                };
                AnimationClip.Interpolation interpolation = sampler.has("interpolation")
                        ? interpolation(sampler.get("interpolation").getAsString())
                        : AnimationClip.Interpolation.LINEAR;
                float[] times = accessors.readFloat(integer(sampler, "input"), "SCALAR", 1);
                float[] values = accessors.readFloat(integer(sampler, "output"),
                        path == AnimationClip.Path.ROTATION ? "VEC4" : "VEC3", path.components());
                if (times.length == 0) {
                    throw new IllegalArgumentException("animation sampler has no keys");
                }
                int multiplier = interpolation == AnimationClip.Interpolation.CUBICSPLINE ? 3 : 1;
                if (values.length != times.length * path.components() * multiplier) {
                    throw new IllegalArgumentException("animation sampler output count does not match interpolation contract");
                }
                duration = Math.max(duration, times[times.length - 1]);
                importedChannels.add(new AnimationClip.Channel(joint, path, interpolation, times, values));
            }
            result.add(new AnimationClip(name, duration, importedChannels));
        }
        return List.copyOf(result);
    }

    private static AnimationClip.Interpolation interpolation(String value) {
        return switch (value) {
            case "STEP" -> AnimationClip.Interpolation.STEP;
            case "LINEAR" -> AnimationClip.Interpolation.LINEAR;
            case "CUBICSPLINE" -> AnimationClip.Interpolation.CUBICSPLINE;
            default -> throw new IllegalArgumentException("unsupported glTF interpolation: " + value);
        };
    }

    private static byte[] embeddedBuffer(JsonObject buffer) {
        String uri = string(buffer, "uri");
        int marker = uri.indexOf(";base64,");
        if (!uri.startsWith("data:") || marker < 0) {
            throw new IllegalArgumentException("animation buffer must be embedded base64 data URI");
        }
        byte[] decoded = Base64.getDecoder().decode(uri.substring(marker + 8));
        if (buffer.has("byteLength") && integer(buffer, "byteLength") != decoded.length) {
            throw new IllegalArgumentException("embedded animation buffer length mismatch");
        }
        return decoded;
    }

    private static final class Accessors {
        private final JsonArray accessors;
        private final JsonArray views;
        private final byte[] binary;

        private Accessors(JsonObject root, byte[] binary) {
            this.accessors = array(root, "accessors");
            this.views = array(root, "bufferViews");
            this.binary = binary;
        }

        private float[] readFloat(int accessorIndex, String expectedType, int components) {
            checkIndex(accessorIndex, accessors.size(), "accessor");
            JsonObject accessor = object(accessors.get(accessorIndex), "accessor");
            if (accessor.has("sparse")) {
                throw new IllegalArgumentException("sparse animation accessors are unsupported");
            }
            if (integer(accessor, "componentType") != FLOAT || !expectedType.equals(string(accessor, "type"))) {
                throw new IllegalArgumentException("animation accessor type/component mismatch");
            }
            int count = integer(accessor, "count");
            int viewIndex = integer(accessor, "bufferView");
            checkIndex(viewIndex, views.size(), "bufferView");
            JsonObject view = object(views.get(viewIndex), "bufferView");
            int stride = view.has("byteStride") ? integer(view, "byteStride") : components * Float.BYTES;
            if (stride < components * Float.BYTES || stride % Float.BYTES != 0) {
                throw new IllegalArgumentException("invalid animation accessor stride");
            }
            int base = (view.has("byteOffset") ? integer(view, "byteOffset") : 0)
                    + (accessor.has("byteOffset") ? integer(accessor, "byteOffset") : 0);
            if (count < 0 || base < 0 || (long) base + (long) Math.max(0, count - 1) * stride + (long) components * 4 > binary.length) {
                throw new IllegalArgumentException("animation accessor exceeds embedded buffer");
            }
            ByteBuffer bytes = ByteBuffer.wrap(binary).order(ByteOrder.LITTLE_ENDIAN);
            float[] out = new float[count * components];
            for (int element = 0; element < count; element++) {
                int offset = base + element * stride;
                for (int component = 0; component < components; component++) {
                    float value = bytes.getFloat(offset + component * 4);
                    if (!Float.isFinite(value)) {
                        throw new IllegalArgumentException("animation accessor contains non-finite value");
                    }
                    out[element * components + component] = value;
                }
            }
            return out;
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            throw new IllegalArgumentException(key + " must be an array");
        }
        return object.getAsJsonArray(key);
    }

    private static JsonObject object(JsonElement element, String label) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException(label + " must be an object");
        }
        return element.getAsJsonObject();
    }

    private static int integer(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
        double raw = object.get(key).getAsDouble();
        int value = object.get(key).getAsInt();
        if (value < 0 || raw != value) {
            throw new IllegalArgumentException(key + " must be a non-negative integer");
        }
        return value;
    }

    private static String string(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException(key + " must be a string");
        }
        return object.get(key).getAsString();
    }

    private static void checkIndex(int index, int size, String label) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException(label + " index outside array: " + index);
        }
    }
}

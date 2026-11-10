package io.github.q93503128.turnbound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Set;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;

/**
 * Server-owned destination for one opt-in Drehmal copy. A location never makes
 * itself safe: physical collision/ground validation is performed at teleport time.
 */
public record TurnboundCheckpointTarget(String dimension, double x, double y, double z,
                                        float yaw, float pitch) {
    private static final double LIMIT = 29999984.0;
    private static final Set<String> CONFIG_KEYS =
            Set.of("schema", "dimension", "x", "y", "z", "yaw", "pitch");

    public TurnboundCheckpointTarget {
        Identifier id = dimension == null ? null : Identifier.tryParse(dimension);
        if (id == null || !id.toString().equals(dimension)
                || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || Math.abs(x) >= LIMIT || Math.abs(z) >= LIMIT
                || y < -2048 || y > 2048
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)
                || Math.abs(yaw) > 1000000.0F || Math.abs(pitch) > 90.0F) {
            throw new IllegalArgumentException("Invalid checkpoint destination");
        }
    }

    /** Strict, opt-in copied-world file. No default or invented map coordinates. */
    public static TurnboundCheckpointTarget parseEntry(String raw) {
        JsonElement element = JsonParser.parseString(raw);
        if (!element.isJsonObject()) throw new IllegalArgumentException("Entry must be a JSON object");
        JsonObject json = element.getAsJsonObject();
        if (!json.keySet().equals(CONFIG_KEYS) || number(json, "schema") != 1.0
                || !json.get("dimension").isJsonPrimitive()
                || !json.get("dimension").getAsJsonPrimitive().isString()
                || !json.get("dimension").getAsString().equals("minecraft:overworld")) {
            throw new IllegalArgumentException("Entry schema/dimension/fields are not approved");
        }
        return new TurnboundCheckpointTarget(
                json.get("dimension").getAsString(),
                number(json, "x"), number(json, "y"), number(json, "z"),
                (float) number(json, "yaw"), (float) number(json, "pitch"));
    }

    private static double number(JsonObject json, String name) {
        JsonElement element = json.get(name);
        if (element == null || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Expected numeric entry field: " + name);
        }
        double value = element.getAsDouble();
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite entry field");
        return value;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Dimension", dimension);
        nbt.putDouble("X", x);
        nbt.putDouble("Y", y);
        nbt.putDouble("Z", z);
        nbt.putFloat("Yaw", yaw);
        nbt.putFloat("Pitch", pitch);
        return nbt;
    }

    public static TurnboundCheckpointTarget fromNbt(NbtCompound nbt) {
        if (!nbt.contains("Dimension", NbtElement.STRING_TYPE)
                || !nbt.contains("X", NbtElement.DOUBLE_TYPE)
                || !nbt.contains("Y", NbtElement.DOUBLE_TYPE)
                || !nbt.contains("Z", NbtElement.DOUBLE_TYPE)
                || !nbt.contains("Yaw", NbtElement.FLOAT_TYPE)
                || !nbt.contains("Pitch", NbtElement.FLOAT_TYPE)) {
            throw new IllegalArgumentException("Checkpoint NBT fields missing or mistyped");
        }
        return new TurnboundCheckpointTarget(nbt.getString("Dimension"),
                nbt.getDouble("X"), nbt.getDouble("Y"), nbt.getDouble("Z"),
                nbt.getFloat("Yaw"), nbt.getFloat("Pitch"));
    }
}

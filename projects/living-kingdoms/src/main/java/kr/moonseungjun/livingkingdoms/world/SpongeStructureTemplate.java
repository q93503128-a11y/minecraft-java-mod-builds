package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Minimal Sponge schematic v2/v3 reader used for audited external settlement templates. */
public final class SpongeStructureTemplate {
    private final int width;
    private final int height;
    private final int length;
    private final List<String> palette;
    private final int[] blocks;

    private SpongeStructureTemplate(int width, int height, int length,
                                    List<String> palette, int[] blocks) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.palette = List.copyOf(palette);
        this.blocks = blocks;
    }

    public int width() { return width; }
    public int height() { return height; }
    public int length() { return length; }
    public List<String> palette() { return palette; }
    public int paletteIndex(int x, int y, int z) {
        return blocks[x + z * width + y * width * length];
    }

    public static SpongeStructureTemplate load(String resourcePath) {
        try (InputStream raw = SpongeStructureTemplate.class.getResourceAsStream(resourcePath)) {
            if (raw == null) throw new IOException("Missing structure resource " + resourcePath);
            Map<String, Object> root = readRoot(raw);
            Object nested = root.get("Schematic");
            if (nested instanceof Map<?, ?> map) root = castMap(map);

            int version = number(root.get("Version"), 1);
            if (version < 2 || version > 3) {
                throw new IOException("Unsupported Sponge schematic version " + version);
            }
            int width = number(root.get("Width"), 0);
            int height = number(root.get("Height"), 0);
            int length = number(root.get("Length"), 0);
            if (width <= 0 || height <= 0 || length <= 0) {
                throw new IOException("Invalid schematic dimensions " + width + "x" + height + "x" + length);
            }

            Map<String, Object> blocksContainer = version >= 3
                    ? requiredMap(root, "Blocks") : root;
            Map<String, Object> paletteMap = requiredMap(blocksContainer, "Palette");
            int max = -1;
            for (Object value : paletteMap.values()) max = Math.max(max, number(value, -1));
            List<String> palette = new ArrayList<>();
            for (int i = 0; i <= max; i++) palette.add("minecraft:air");
            for (Map.Entry<String, Object> entry : paletteMap.entrySet()) {
                int id = number(entry.getValue(), -1);
                if (id >= 0) palette.set(id, entry.getKey());
            }

            Object dataObject = version >= 3 ? blocksContainer.get("Data") : root.get("BlockData");
            if (!(dataObject instanceof byte[] encoded)) throw new IOException("Missing schematic block data");
            int expected = Math.multiplyExact(Math.multiplyExact(width, height), length);
            int[] decoded = decodeVarInts(encoded, expected);
            LivingKingdoms.LOGGER.info("Loaded external structure {} size={}x{}x{} palette={} blocks={}",
                    resourcePath, width, height, length, palette.size(), decoded.length);
            return new SpongeStructureTemplate(width, height, length, palette, decoded);
        } catch (IOException | ArithmeticException exception) {
            throw new IllegalStateException("Unable to load external structure " + resourcePath, exception);
        }
    }

    private static int[] decodeVarInts(byte[] encoded, int expected) throws IOException {
        int[] values = new int[expected];
        int offset = 0;
        for (int i = 0; i < expected; i++) {
            int value = 0;
            int shift = 0;
            while (true) {
                if (offset >= encoded.length) throw new EOFException("Truncated schematic block data at " + i);
                int current = encoded[offset++] & 0xff;
                value |= (current & 0x7f) << shift;
                if ((current & 0x80) == 0) break;
                shift += 7;
                if (shift > 28) throw new IOException("Invalid schematic varint at " + i);
            }
            values[i] = value;
        }
        return values;
    }

    private static Map<String, Object> readRoot(InputStream source) throws IOException {
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(new GZIPInputStream(source)))) {
            int type = input.readUnsignedByte();
            if (type != 10) throw new IOException("Schematic root is not a compound tag");
            readString(input);
            return readCompound(input);
        }
    }

    private static Map<String, Object> readCompound(DataInputStream input) throws IOException {
        Map<String, Object> result = new HashMap<>();
        while (true) {
            int type = input.readUnsignedByte();
            if (type == 0) return result;
            String name = readString(input);
            result.put(name, readPayload(input, type));
        }
    }

    private static Object readPayload(DataInputStream input, int type) throws IOException {
        return switch (type) {
            case 1 -> input.readByte();
            case 2 -> input.readShort();
            case 3 -> input.readInt();
            case 4 -> input.readLong();
            case 5 -> input.readFloat();
            case 6 -> input.readDouble();
            case 7 -> {
                int length = input.readInt();
                if (length < 0) throw new IOException("Negative byte array length");
                yield input.readNBytes(length);
            }
            case 8 -> readString(input);
            case 9 -> {
                int childType = input.readUnsignedByte();
                int length = input.readInt();
                if (length < 0) throw new IOException("Negative list length");
                List<Object> list = new ArrayList<>(length);
                for (int i = 0; i < length; i++) list.add(readPayload(input, childType));
                yield list;
            }
            case 10 -> readCompound(input);
            case 11 -> {
                int length = input.readInt();
                if (length < 0) throw new IOException("Negative int array length");
                int[] values = new int[length];
                for (int i = 0; i < length; i++) values[i] = input.readInt();
                yield values;
            }
            case 12 -> {
                int length = input.readInt();
                if (length < 0) throw new IOException("Negative long array length");
                long[] values = new long[length];
                for (int i = 0; i < length; i++) values[i] = input.readLong();
                yield values;
            }
            default -> throw new IOException("Unsupported NBT tag type " + type);
        };
    }

    private static String readString(DataInputStream input) throws IOException {
        int length = input.readUnsignedShort();
        return new String(input.readNBytes(length), StandardCharsets.UTF_8);
    }

    private static int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static Map<String, Object> requiredMap(Map<String, Object> parent, String key) throws IOException {
        Object value = parent.get(key);
        if (!(value instanceof Map<?, ?> map)) throw new IOException("Missing compound tag " + key);
        return castMap(map);
    }

    private static Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> result = new HashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }
}

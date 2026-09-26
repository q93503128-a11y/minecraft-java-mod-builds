package io.github.q93503128.turnbound.content;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HeroTextureResourceTest {
    private static final List<String> HERO_TEXTURES = List.of(
            "kyren", "lumea", "bram", "elysia", "lynette", "morwen", "marion", "raze", "toto");

    @Test
    void allHeroTexturesAreCompleteCrcValidPngs() throws Exception {
        for (String hero : HERO_TEXTURES) {
            assertStandardPng("/assets/turnbound/textures/entity/hero/" + hero + ".png");
        }
    }

    private void assertStandardPng(String resource) throws Exception {
        try (InputStream raw = getClass().getResourceAsStream(resource)) {
            assertNotNull(raw, "missing hero texture " + resource);
            DataInputStream in = new DataInputStream(raw);
            assertArrayEquals(
                    new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
                    in.readNBytes(8),
                    "invalid PNG signature " + resource);

            List<String> chunks = new ArrayList<>();
            while (true) {
                int length = in.readInt();
                assertTrue(length >= 0 && length <= 16 * 1024 * 1024,
                        "invalid PNG chunk length in " + resource + ": " + length);

                byte[] typeBytes = in.readNBytes(4);
                assertEquals(4, typeBytes.length, "truncated PNG chunk type in " + resource);
                String type = new String(typeBytes, StandardCharsets.US_ASCII);
                byte[] data = in.readNBytes(length);
                assertEquals(length, data.length, "truncated PNG chunk " + type + " in " + resource);
                byte[] crcBytes = in.readNBytes(4);
                assertEquals(4, crcBytes.length, "missing PNG CRC for " + type + " in " + resource);

                CRC32 crc = new CRC32();
                crc.update(typeBytes);
                crc.update(data);
                assertEquals(unsignedInt(crcBytes), crc.getValue(),
                        "invalid PNG CRC for " + type + " in " + resource);

                chunks.add(type);
                if ("IEND".equals(type)) break;
            }

            assertTrue(!chunks.isEmpty() && "IHDR".equals(chunks.getFirst()),
                    "PNG must begin with IHDR: " + resource);
            assertTrue(chunks.contains("IDAT"), "PNG must contain IDAT: " + resource);
            assertEquals("IEND", chunks.getLast(), "PNG must end with IEND: " + resource);
            assertEquals(-1, in.read(), "trailing bytes after IEND in " + resource);
        }
    }

    private static long unsignedInt(byte[] bytes) {
        return ((bytes[0] & 0xFFL) << 24)
                | ((bytes[1] & 0xFFL) << 16)
                | ((bytes[2] & 0xFFL) << 8)
                | (bytes[3] & 0xFFL);
    }
}

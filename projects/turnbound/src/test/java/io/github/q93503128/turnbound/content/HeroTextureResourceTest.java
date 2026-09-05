package io.github.q93503128.turnbound.content;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HeroTextureResourceTest {
    @Test void elysiaTextureIsACompleteStandardPngWithoutCorruptTrailingChunks() throws Exception {
        try(InputStream raw=getClass().getResourceAsStream("/assets/turnbound/textures/entity/hero/elysia.png")){
            assertNotNull(raw);
            DataInputStream in=new DataInputStream(raw);
            assertArrayEquals(new byte[]{(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A},in.readNBytes(8));
            List<String> chunks=new ArrayList<>();
            while(true){
                int length=in.readInt();
                assertTrue(length>=0&&length<=16*1024*1024,"invalid PNG chunk length: "+length);
                String type=new String(in.readNBytes(4), StandardCharsets.US_ASCII);
                chunks.add(type);
                assertEquals(length,in.readNBytes(length).length,"truncated PNG chunk "+type);
                assertEquals(4,in.readNBytes(4).length,"missing PNG CRC");
                if("IEND".equals(type))break;
            }
            assertEquals(List.of("IHDR","IDAT","IEND"),chunks);
            assertEquals(-1,in.read(),"trailing bytes after IEND");
        }
    }
}

package kr.moonseungjun.turnboundre.client.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class M5BattleResultLayoutTest {
    @Test
    void supportedCanvasesStayBoundedAndReadable() {
        for (int[] size : new int[][]{{480,270},{640,360},{1280,720},{1920,1080}}) {
            var layout = BattleResultLayout.calculate(size[0], size[1]);
            assertTrue(layout.root().fitsInside(size[0], size[1]));
            assertTrue(layout.header().fitsInside(size[0], size[1]));
            assertTrue(layout.rewards().fitsInside(size[0], size[1]));
            assertTrue(layout.footer().fitsInside(size[0], size[1]));
            assertTrue(layout.root().width() >= 448, "result root became too narrow at " + size[0] + "x" + size[1]);
            assertTrue(layout.rewards().height() >= 160, "reward rows lost vertical room at " + size[0] + "x" + size[1]);
            assertTrue(layout.header().bottom() <= layout.rewards().y());
            assertTrue(layout.rewards().bottom() <= layout.footer().y());
        }
    }

    @Test
    void unsupportedTinyCanvasFailsClosed() {
        assertFalse(BattleResultLayout.supports(320, 180));
        assertThrows(IllegalArgumentException.class, () -> BattleResultLayout.calculate(320, 180));
    }
}

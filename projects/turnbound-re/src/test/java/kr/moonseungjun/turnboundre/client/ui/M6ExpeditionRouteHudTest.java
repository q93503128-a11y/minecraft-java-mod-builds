package kr.moonseungjun.turnboundre.client.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class M6ExpeditionRouteHudTest {
    @Test
    void cardinalDirectionMatchesMinecraftWorldAxes() {
        assertEquals("E", ExpeditionRouteHud.cardinal(10.0D, 0.0D));
        assertEquals("S", ExpeditionRouteHud.cardinal(0.0D, 10.0D));
        assertEquals("W", ExpeditionRouteHud.cardinal(-10.0D, 0.0D));
        assertEquals("N", ExpeditionRouteHud.cardinal(0.0D, -10.0D));
        assertEquals("NE", ExpeditionRouteHud.cardinal(10.0D, -10.0D));
    }
}

package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlayerWeaponMoveSlotResolverTest {
    @Test
    void resolvesStableContentIdOrderWithoutCreatingClientAuthority() {
        var entry = ContentId.rift("attack/player/mobile_pressure_entry");
        var finisher = ContentId.rift("attack/player/mobile_pressure_finisher");
        var family = new CoreDefinition.WeaponFamily(
            ContentId.rift("weapon_family/mobile_pressure"),
            Set.of(finisher, entry),
            Set.of("mobile_pressure"),
            Set.of("technique")
        );

        assertEquals(entry, PlayerWeaponMoveSlotResolver.resolve(family, 0).orElseThrow());
        assertEquals(finisher, PlayerWeaponMoveSlotResolver.resolve(family, 1).orElseThrow());
        assertTrue(PlayerWeaponMoveSlotResolver.resolve(family, 2).isEmpty());
        assertTrue(PlayerWeaponMoveSlotResolver.resolve(family, -1).isEmpty());
    }
}

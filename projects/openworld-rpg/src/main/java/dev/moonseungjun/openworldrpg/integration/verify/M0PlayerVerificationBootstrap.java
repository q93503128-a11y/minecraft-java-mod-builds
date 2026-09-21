package dev.moonseungjun.openworldrpg.integration.verify;

import dev.moonseungjun.openworldrpg.combat.state.AttributeAllocation;
import dev.moonseungjun.openworldrpg.combat.state.EquippedCombatItem;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Explicit local/manual M0 player bootstrap.
 *
 * <p>Disabled by default and never registered as player-facing gameplay. When the JVM verification
 * flag is enabled, a joining player in a disposable test world receives a canon-valid Lv8 Mage
 * combat profile and ItemLv8 Staff loadout so the real Spell Engine client flow can be exercised.
 * The physical held item/spell container is intentionally left to an explicit /give command, so
 * this path does not create a parallel inventory implementation.</p>
 */
public final class M0PlayerVerificationBootstrap {
    public static final String ENABLE_PROPERTY = "openworld_rpg.m0PlayerVerification";

    private M0PlayerVerificationBootstrap() {
    }

    public static boolean enabled() {
        return Boolean.parseBoolean(System.getProperty(ENABLE_PROPERTY, "false"));
    }

    public static void prepare(ServerPlayer player, Logger logger) {
        if (!enabled()) {
            return;
        }

        PlayerProgressionService.setCombatLevel(player, 8);
        PlayerProgressionService.selectClass(player, RootClass.MAGE);
        PlayerProgressionService.setAllocation(
                player,
                RootClass.MAGE,
                new AttributeAllocation(0, 0, 0, 0, 7, 0)
        );
        PlayerEquipmentService.equip(
                player,
                EquippedCombatItem.weapon(
                        "openworld_rpg:initiate_staff",
                        8,
                        ProjectWeaponFamily.STAFF,
                        List.of()
                )
        );

        logger.info(
                "OPENWORLD_RPG_M0_PLAYER_READY player={} level=8 class=MAGE intAllocation=7 weapon=STAFF itemLevel=8",
                player.getGameProfile().name()
        );
    }
}

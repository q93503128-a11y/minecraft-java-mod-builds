package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.ClericSkillCastRuntimeState;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Root Cleric mechanics at the accepted-cast boundary.
 */
public final class ClericSkillRuntime {
    private ClericSkillRuntime() {
    }

    public static void onAcceptedCast(
            ServerPlayer player,
            String spellId,
            long nowTick
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(spellId, "spellId");
        if (PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.CLERIC::equals)
                .isEmpty()) {
            return;
        }

        boolean damaging = ProjectSpellSpec.RADIANT_LANCE_ID.equals(
                spellId
        );
        boolean healingProtection = ProjectSpellSpec.MEND_ID.equals(
                spellId
        );
        if (!damaging && !healingProtection) {
            return;
        }

        var combat = CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick);
        var grace = CombatStateServices.clericGraceStates()
                .getOrCreate(player.getUUID());
        var doctrine = CombatStateServices.clericDoctrineStates()
                .getOrCreate(player.getUUID());

        boolean empowered = grace.consumeForSpender(
                nowTick,
                combat.lastCombatActivityTick()
        );
        double outputMultiplier;
        if (damaging) {
            outputMultiplier = doctrine.consumeDirectDamageMultiplier(
                    nowTick
            );
            doctrine.afterDamagingActive(nowTick);
        } else {
            outputMultiplier =
                    doctrine.consumeHealingProtectionMultiplier(
                            nowTick
                    );
            doctrine.afterHealingProtectionActive(nowTick);
        }

        CombatStateServices.clericSkillCastStates()
                .getOrCreate(player.getUUID())
                .begin(
                        spellId,
                        empowered,
                        outputMultiplier,
                        nowTick
                );
    }

    public static ClericSkillCastRuntimeState.PendingCast
            consumeAcceptedCast(
                    ServerPlayer player,
                    String spellId
            ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(spellId, "spellId");
        return CombatStateServices.clericSkillCastStates()
                .getOrCreate(player.getUUID())
                .consume(
                        spellId,
                        player.level().getGameTime()
                );
    }
}

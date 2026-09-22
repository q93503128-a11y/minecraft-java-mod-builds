package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;

/**
 * Rebuilds transient combat/vital/defense state from persistent project progression + equipment.
 */
public final class PlayerCombatBuildPublisher {
    private PlayerCombatBuildPublisher() {
    }

    public static Optional<PlayerCombatBuildState> refresh(Player player) {
        if (player.level().isClientSide()) {
            return Optional.empty();
        }

        PlayerProgressionState progression = PlayerProgressionService.state(player);
        PlayerEquipmentLoadoutState loadout = PlayerEquipmentService.state(player);
        EffectiveAttributes gearAttributes = loadout.aggregateFlatAttributeBonuses();

        AttributeAllocation allocation = progression.activeClass()
                .map(progression::allocation)
                .orElseGet(AttributeAllocation::unspent);
        double effectiveVit = allocation.value(CombatAttribute.VIT) + gearAttributes.vit();
        double effectiveEnd = allocation.value(CombatAttribute.END) + gearAttributes.end();
        double effectiveWil = allocation.value(CombatAttribute.WIL) + gearAttributes.wil();
        long gameTick = player.level().getGameTime();

        int maxHealth = ProjectCombatRules.maxPlayerHealth(
                progression.combatLevel(),
                effectiveVit,
                0.0
        );
        PlayerVitalsRuntime.synchronizeMaxHealth(player, maxHealth);
        CombatStateServices.states().synchronizeEndurance(
                player.getUUID(),
                (int) Math.round(effectiveEnd),
                gameTick
        );
        CombatStateServices.states().synchronizeWill(
                player.getUUID(),
                (int) Math.round(effectiveWil),
                gameTick
        );
        CombatStateServices.defenseSnapshots().bindAuthoritative(
                player.getUUID(),
                loadout.aggregateDefenseSnapshot()
        );

        Optional<EquipmentCombatState> equipment = loadout.aggregateCombatState();
        Optional<PlayerCombatBuildState> build = equipment.flatMap(
                progression::buildWith
        );

        if (build.isPresent()) {
            CombatStateServices.combatBuilds().bindAuthoritative(player.getUUID(), build.get());
        } else {
            CombatStateServices.combatBuilds().remove(player.getUUID());
        }
        return build;
    }
}

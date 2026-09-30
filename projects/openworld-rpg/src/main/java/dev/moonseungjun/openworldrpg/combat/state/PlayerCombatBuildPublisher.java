package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import dev.moonseungjun.openworldrpg.combat.runtime.HunterRootPassiveEffects;
import dev.moonseungjun.openworldrpg.combat.runtime.WarriorRootPassiveEffects;
import dev.moonseungjun.openworldrpg.combat.runtime.WarriorSkillRuntime;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
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
        EquipmentResourceModifiers resourceModifiers =
                loadout.aggregateResourceModifiers();

        AttributeAllocation allocation = progression.activeClass()
                .map(progression::allocation)
                .orElseGet(AttributeAllocation::unspent);
        double effectiveVit = allocation.value(CombatAttribute.VIT) + gearAttributes.vit();
        double effectiveEnd = allocation.value(CombatAttribute.END) + gearAttributes.end();
        double effectiveWil = allocation.value(CombatAttribute.WIL) + gearAttributes.wil();
        long gameTick = player.level().getGameTime();
        ServerPlayer serverPlayer =
                player instanceof ServerPlayer value
                        ? value
                        : null;

        int maxHealth = ProjectCombatRules.maxPlayerHealth(
                progression.combatLevel(),
                effectiveVit,
                resourceModifiers.maxHealthBonus()
                        + (serverPlayer != null
                                ? WarriorRootPassiveEffects
                                        .maxHealthPercentBonus(
                                                serverPlayer
                                        )
                                : 0.0)
        );
        PlayerVitalsRuntime.synchronizeMaxHealth(player, maxHealth);
        PlayerMovementRuntime.synchronize(
                player,
                loadout.aggregateMovementSpeedBonus()
        );
        ProjectWeaponFamily mainWeaponFamily = loadout
                .item(ProjectEquipmentSlot.MAIN_WEAPON)
                .flatMap(EquippedCombatItem::weaponFamily)
                .orElse(null);
        PlayerMovementRuntime.synchronizeClassMovementSpeedBonus(
                player,
                serverPlayer != null
                        ? HunterRootPassiveEffects.movementSpeedBonus(
                                serverPlayer,
                                mainWeaponFamily
                        )
                        : 0.0
        );
        PlayerAttackSpeedRuntime.synchronize(
                player,
                loadout.item(ProjectEquipmentSlot.MAIN_WEAPON)
                        .flatMap(EquippedCombatItem::weaponFamily),
                loadout.aggregateAttackSpeedBonus(),
                serverPlayer != null
                        ? WarriorRootPassiveEffects
                                .weaponRhythmAttackSpeedBonus(
                                        serverPlayer,
                                        WarriorSkillRuntime
                                                .momentumPips(
                                                        serverPlayer,
                                                        gameTick
                                                )
                                )
                        : 0.0
        );
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
        CombatStateServices.states().synchronizeResourceModifiers(
                player.getUUID(),
                resourceModifiers,
                gameTick
        );
        CombatStateServices.states()
                .synchronizeClassStaminaModifiers(
                        player.getUUID(),
                        serverPlayer != null
                                ? WarriorRootPassiveEffects
                                        .maxStaminaFlatBonus(
                                                serverPlayer
                                        )
                                : 0,
                        serverPlayer != null
                                ? WarriorRootPassiveEffects
                                        .staminaRecoveryBonus(
                                                serverPlayer
                                        )
                                : 0.0,
                        gameTick
                );
        CombatStateServices.states()
                .synchronizeClassSkillManaCostMultiplier(
                        player.getUUID(),
                        serverPlayer != null
                                ? HunterRootPassiveEffects
                                        .skillManaCostMultiplier(
                                                serverPlayer
                                        )
                                : 1.0,
                        gameTick
                );
        CombatStateServices.defenseSnapshots().bindAuthoritative(
                player.getUUID(),
                loadout.aggregateDefenseSnapshot()
        );
        double maxPoise = ProjectCombatRules.maxPlayerPoise(
                effectiveEnd,
                loadout.aggregateArmorPoise(),
                loadout.aggregatePoiseStaggerResistanceBonus()
        );
        CombatStateServices.playerPoiseStates().synchronize(
                player.getUUID(),
                maxPoise,
                gameTick
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

package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import dev.moonseungjun.openworldrpg.combat.runtime.ClericRootPassiveEffects;
import dev.moonseungjun.openworldrpg.combat.runtime.ClericRootPassiveRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.GuardianResolveRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.GuardianRootPassiveEffects;
import dev.moonseungjun.openworldrpg.combat.runtime.GuardianSkillRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.HunterRootPassiveEffects;
import dev.moonseungjun.openworldrpg.combat.runtime.MageArcaneWeaveRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.MageRootPassiveEffects;
import dev.moonseungjun.openworldrpg.combat.runtime.WarriorRootPassiveEffects;
import dev.moonseungjun.openworldrpg.combat.runtime.WarriorSkillRuntime;
import dev.moonseungjun.openworldrpg.recovery.R01NourishmentService;
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
                                        + GuardianRootPassiveEffects
                                                .maxHealthPercentBonus(
                                                        serverPlayer
                                                )
                                        + R01NourishmentService
                                                .maxHealthBonus(
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
                                + MageArcaneWeaveRuntime
                                        .arcaneMemoryMovementSpeedBonus(
                                                serverPlayer,
                                                gameTick
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
        PlayerCastSpeedRuntime.synchronize(
                player,
                serverPlayer != null
                        ? MageRootPassiveEffects.castSpeedBonus(
                                serverPlayer
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
                .synchronizeClassManaFlatBonus(
                        player.getUUID(),
                        serverPlayer != null
                                ? ClericRootPassiveEffects
                                        .maxManaFlatBonus(
                                                serverPlayer
                                        )
                                        + MageRootPassiveEffects
                                                .maxManaFlatBonus(
                                                        serverPlayer
                                                )
                                : 0,
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
                                        + GuardianRootPassiveEffects
                                                .maxStaminaFlatBonus(
                                                        serverPlayer
                                                )
                                : 0,
                        serverPlayer != null
                                ? WarriorRootPassiveEffects
                                        .staminaRecoveryBonus(
                                                serverPlayer
                                        )
                                        + GuardianRootPassiveEffects
                                                .staminaRecoveryBonus(
                                                        serverPlayer
                                                )
                                : 0.0,
                        gameTick
                );
        CombatStateServices.states()
                .getOrCreate(player.getUUID(), gameTick)
                .synchronizeNourishmentRecoveryModifiers(
                        serverPlayer != null
                                ? R01NourishmentService
                                        .manaRecoveryBonus(serverPlayer)
                                : 0.0,
                        serverPlayer != null
                                ? R01NourishmentService
                                        .staminaRecoveryBonus(serverPlayer)
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
                                        * MageRootPassiveEffects
                                                .skillManaCostMultiplier(
                                                        serverPlayer
                                                )
                                : 1.0,
                        gameTick
                );
        CombatStateServices.negativeStatusStates()
                .getOrCreate(player.getUUID())
                .synchronizeEquipmentNegativeStatusDurationReduction(
                        loadout.aggregateNegativeStatusDurationReduction()
                );
        PlayerDefenseAuthority.DefenseSnapshot defenseSnapshot =
                loadout.aggregateDefenseSnapshot();
        if (serverPlayer != null) {
            double equipmentMagicResistanceMultiplier =
                    ClericRootPassiveEffects
                            .equipmentMagicResistanceMultiplier(
                                    serverPlayer
                            );
            if (Math.abs(
                    equipmentMagicResistanceMultiplier - 1.0
            ) > 1.0e-9) {
                defenseSnapshot =
                        new PlayerDefenseAuthority.DefenseSnapshot(
                                defenseSnapshot.defense(),
                                defenseSnapshot.magicResistance()
                                        * equipmentMagicResistanceMultiplier,
                                defenseSnapshot.guardRating(),
                                defenseSnapshot.guardType()
                        );
            }
            CombatStateServices.clericGraceStates()
                    .getOrCreate(player.getUUID())
                    .synchronizeExpiryBonusTicks(
                            ClericRootPassiveEffects
                                    .graceExpiryBonusTicks(
                                            serverPlayer
                                    )
                    );
            ClericRootPassiveRuntime.synchronize(serverPlayer);
            GuardianResolveRuntime.synchronize(serverPlayer);
        }
        CombatStateServices.defenseSnapshots().bindAuthoritative(
                player.getUUID(),
                defenseSnapshot
        );
        double maxPoise = ProjectCombatRules.maxPlayerPoise(
                effectiveEnd,
                loadout.aggregateArmorPoise(),
                loadout.aggregatePoiseStaggerResistanceBonus()
        ) * (serverPlayer != null
                ? GuardianSkillRuntime
                        .standFirmPoiseMaxMultiplier(serverPlayer)
                : 1.0);
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

package dev.moonseungjun.openworldrpg.integration.verify;

import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongEncounterData;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongPhysicalEncounterRuntime;
import dev.moonseungjun.openworldrpg.combat.state.AttributeAllocation;
import dev.moonseungjun.openworldrpg.combat.state.EquippedCombatItem;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorCombatProfile;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Explicit local/manual M0 player bootstrap.
 *
 * <p>Disabled in normal builds and never registered as player-facing gameplay. A dedicated M0
 * playtest artifact embeds a verification marker so a joining player in a disposable test world
 * receives a canon-valid Lv8 Mage combat profile and ItemLv8 Staff loadout without requiring a JVM
 * option. The legacy JVM flag remains available for local development.</p>
 */
public final class M0PlayerVerificationBootstrap {
    public static final String ENABLE_PROPERTY = "openworld_rpg.m0PlayerVerification";
    public static final String EMBEDDED_MARKER =
            "data/openworld_rpg/integration/m0_player_verification.enabled";
    private static final String EARTHLOONG_COMMAND = "owr_spawn_earthloong";
    private static final String EARTHLOONG_TAIL_PREVIEW_COMMAND =
            "owr_earthloong_preview_tail";
    private static final String EARTHLOONG_FORKED_PREVIEW_COMMAND =
            "owr_earthloong_preview_forked";
    private static final String EARTHLOONG_EARTHLINE_PREVIEW_COMMAND =
            "owr_earthloong_preview_earthline";

    private M0PlayerVerificationBootstrap() {
    }

    public static boolean enabled() {
        if (Boolean.parseBoolean(System.getProperty(ENABLE_PROPERTY, "false"))) {
            return true;
        }
        return M0PlayerVerificationBootstrap.class.getResource("/" + EMBEDDED_MARKER) != null;
    }

    public static void registerCommands() {
        if (!enabled()) {
            return;
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
            dispatcher.register(
                    Commands.literal(EARTHLOONG_COMMAND)
                            .executes(context -> spawnEarthloongInFront(context.getSource().getEntity()))
            );
            dispatcher.register(
                    Commands.literal(EARTHLOONG_TAIL_PREVIEW_COMMAND)
                            .executes(context -> previewEarthloongAction(
                                    context.getSource().getEntity(),
                                    R01EarthloongEncounterData.ActionId.TAIL_SCYTHE,
                                    "Tail Scythe"
                            ))
            );
            dispatcher.register(
                    Commands.literal(EARTHLOONG_FORKED_PREVIEW_COMMAND)
                            .executes(context -> previewEarthloongAction(
                                    context.getSource().getEntity(),
                                    R01EarthloongEncounterData.ActionId.FORKED_HEAVEN,
                                    "Forked Heaven"
                            ))
            );
            dispatcher.register(
                    Commands.literal(EARTHLOONG_EARTHLINE_PREVIEW_COMMAND)
                            .executes(context -> previewEarthloongAction(
                                    context.getSource().getEntity(),
                                    R01EarthloongEncounterData.ActionId.EARTHLINE_SURGE,
                                    "Earthline Surge"
                            ))
            );
        });
    }

    private static int spawnEarthloongInFront(Entity commandEntity) {
        if (!(commandEntity instanceof ServerPlayer player)) {
            return 0;
        }

        Vec3 look = player.getLookAngle();
        double horizontal = Math.hypot(look.x, look.z);
        double forwardX = horizontal > 1.0e-6 ? look.x / horizontal : 0.0;
        double forwardZ = horizontal > 1.0e-6 ? look.z / horizontal : 1.0;
        BlockPos spawnPos = BlockPos.containing(
                player.getX() + forwardX * 7.0,
                player.getY() + 1.0,
                player.getZ() + forwardZ * 7.0
        );

        Entity spawned = ExternalActorBindingRuntime.spawnAuthored(
                (ServerLevel) player.level(),
                spawnPos,
                ExternalActorCombatProfile.r01Earthloong().entityId()
        );
        if (!(spawned instanceof net.minecraft.world.entity.LivingEntity living)
                || !R01EarthloongPhysicalEncounterRuntime.armVerificationFixture(living, player)) {
            spawned.discard();
            player.sendSystemMessage(Component.literal(
                    "[M0] Earthloong verification fixture failed to arm."
            ));
            return 0;
        }
        player.sendSystemMessage(Component.literal(
                "[M0] Earthloong verification fixture ready. Tail Scythe, Forked Heaven, "
                        + "and Earthline Surge will preview automatically once each."
        ));
        return 1;
    }

    private static int previewEarthloongAction(
            Entity commandEntity,
            R01EarthloongEncounterData.ActionId action,
            String displayName
    ) {
        if (!(commandEntity instanceof ServerPlayer player)) {
            return 0;
        }
        boolean accepted =
                R01EarthloongPhysicalEncounterRuntime.beginVerificationPreview(player, action);
        if (accepted) {
            player.sendSystemMessage(Component.literal(
                    "[M0] Preview started: " + displayName
            ));
            return 1;
        }
        player.sendSystemMessage(Component.literal(
                "[M0] Preview rejected: " + displayName
                        + ". Check distance, line of sight, and wait for the previous preview to finish."
        ));
        return 0;
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

package dev.moonseungjun.openworldrpg.integration.verify;

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
    private static final String EARTHLOONG_MOTION_1_COMMAND = "owr_earthloong_motion_1";
    private static final String EARTHLOONG_MOTION_2_COMMAND = "owr_earthloong_motion_2";
    private static final String EARTHLOONG_MOTION_3_COMMAND = "owr_earthloong_motion_3";
    private static final String EARTHLOONG_MOTION_4_COMMAND = "owr_earthloong_motion_4";
    private static final String SWORD_PROFILE_COMMAND = "owr_test_sword";
    private static final String CROSSBOW_PROFILE_COMMAND = "owr_test_crossbow";
    private static final String MAGIC_PROFILE_COMMAND = "owr_test_magic";

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
                    Commands.literal(EARTHLOONG_MOTION_1_COMMAND)
                            .executes(context -> previewEarthloongMotion(
                                    context.getSource().getEntity(), 1))
            );
            dispatcher.register(
                    Commands.literal(EARTHLOONG_MOTION_2_COMMAND)
                            .executes(context -> previewEarthloongMotion(
                                    context.getSource().getEntity(), 2))
            );
            dispatcher.register(
                    Commands.literal(EARTHLOONG_MOTION_3_COMMAND)
                            .executes(context -> previewEarthloongMotion(
                                    context.getSource().getEntity(), 3))
            );
            dispatcher.register(
                    Commands.literal(EARTHLOONG_MOTION_4_COMMAND)
                            .executes(context -> previewEarthloongMotion(
                                    context.getSource().getEntity(), 4))
            );
            dispatcher.register(
                    Commands.literal(SWORD_PROFILE_COMMAND)
                            .executes(context -> prepareSwordProfile(
                                    context.getSource().getEntity()))
            );
            dispatcher.register(
                    Commands.literal(CROSSBOW_PROFILE_COMMAND)
                            .executes(context -> prepareCrossbowProfile(
                                    context.getSource().getEntity()))
            );
            dispatcher.register(
                    Commands.literal(MAGIC_PROFILE_COMMAND)
                            .executes(context -> prepareMagicProfile(
                                    context.getSource().getEntity()))
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
            return 0;
        }
        return 1;
    }

    private static int previewEarthloongMotion(
            Entity commandEntity,
            int donorSkillNumber
    ) {
        if (!(commandEntity instanceof ServerPlayer player)) {
            return 0;
        }
        return R01EarthloongPhysicalEncounterRuntime.beginVerificationMotionPreview(
                player,
                donorSkillNumber
        ) ? 1 : 0;
    }

    private static int prepareSwordProfile(Entity commandEntity) {
        if (!(commandEntity instanceof ServerPlayer player)) {
            return 0;
        }
        applyVerificationProfile(
                player,
                RootClass.WARRIOR,
                new AttributeAllocation(0, 0, 7, 0, 0, 0),
                ProjectWeaponFamily.SWORD,
                "openworld_rpg:initiate_sword"
        );
        return 1;
    }

    private static int prepareCrossbowProfile(Entity commandEntity) {
        if (!(commandEntity instanceof ServerPlayer player)) {
            return 0;
        }
        applyVerificationProfile(
                player,
                RootClass.HUNTER,
                new AttributeAllocation(0, 0, 0, 7, 0, 0),
                ProjectWeaponFamily.CROSSBOW,
                "openworld_rpg:initiate_crossbow"
        );
        return 1;
    }

    private static int prepareMagicProfile(Entity commandEntity) {
        if (!(commandEntity instanceof ServerPlayer player)) {
            return 0;
        }
        applyVerificationProfile(
                player,
                RootClass.MAGE,
                new AttributeAllocation(0, 0, 0, 0, 7, 0),
                ProjectWeaponFamily.STAFF,
                "openworld_rpg:initiate_staff"
        );
        return 1;
    }

    private static void applyVerificationProfile(
            ServerPlayer player,
            RootClass rootClass,
            AttributeAllocation allocation,
            ProjectWeaponFamily weaponFamily,
            String itemId
    ) {
        PlayerProgressionService.setCombatLevel(player, 8);
        PlayerProgressionService.selectClass(player, rootClass);
        PlayerProgressionService.setAllocation(player, rootClass, allocation);
        PlayerEquipmentService.equip(
                player,
                EquippedCombatItem.weapon(
                        itemId,
                        8,
                        weaponFamily,
                        List.of()
                )
        );
    }

    public static void prepare(ServerPlayer player, Logger logger) {
        if (!enabled()) {
            return;
        }

        applyVerificationProfile(
                player,
                RootClass.WARRIOR,
                new AttributeAllocation(0, 0, 7, 0, 0, 0),
                ProjectWeaponFamily.SWORD,
                "openworld_rpg:initiate_sword"
        );

        logger.info(
                "OPENWORLD_RPG_M0_PLAYER_READY player={} level=8 class=WARRIOR strAllocation=7 weapon=SWORD itemLevel=8 profiles=sword,crossbow,magic",
                player.getGameProfile().name()
        );
    }
}

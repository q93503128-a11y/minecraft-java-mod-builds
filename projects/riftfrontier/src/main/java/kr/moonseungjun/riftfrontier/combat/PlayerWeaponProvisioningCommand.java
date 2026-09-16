package kr.moonseungjun.riftfrontier.combat;

import com.mojang.brigadier.Command;
import kr.moonseungjun.riftfrontier.content.ContentId;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Optional;

/**
 * Narrow field-play provisioning surface for the two locked M3 production weapon families.
 *
 * <p>The vanilla sword is only a temporary physical carrier. Combat identity remains the registered
 * server-owned ItemStack component, and all move/timing authority is still reconstructed from the
 * currently published content graph. Commands and the technical hub stations intentionally share this
 * single issuer so field-play convenience cannot create a second loadout contract.</p>
 */
public final class PlayerWeaponProvisioningCommand {
    private PlayerWeaponProvisioningCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("riftfrontier")
                .then(Commands.literal("weapon")
                    .then(Commands.literal("mobile")
                        .executes(context -> provision(
                            context.getSource().getPlayerOrException(),
                            PlayerWeaponItemStackLoadoutResolver.MOBILE_PRESSURE,
                            Optional.empty()
                        ))
                        .then(Commands.literal("pivot").executes(context -> provision(
                            context.getSource().getPlayerOrException(),
                            PlayerWeaponItemStackLoadoutResolver.MOBILE_PRESSURE,
                            Optional.of(PlayerWeaponItemStackLoadoutResolver.RECOVERY_PIVOT)
                        )))
                    )
                    .then(Commands.literal("reach")
                        .executes(context -> provision(
                            context.getSource().getPlayerOrException(),
                            PlayerWeaponItemStackLoadoutResolver.REACH_COMMITMENT,
                            Optional.empty()
                        ))
                        .then(Commands.literal("pivot").executes(context -> provision(
                            context.getSource().getPlayerOrException(),
                            PlayerWeaponItemStackLoadoutResolver.REACH_COMMITMENT,
                            Optional.of(PlayerWeaponItemStackLoadoutResolver.RECOVERY_PIVOT)
                        )))
                    )
                )
        );
    }

    private static int provision(ServerPlayer player, ContentId familyId, Optional<ContentId> moduleId) {
        issueLoadout(player, familyId, moduleId);
        return Command.SINGLE_SUCCESS;
    }

    /** Issues the same authoritative field rig used by the command surface. */
    public static void issueLoadout(ServerPlayer player, ContentId familyId, Optional<ContentId> moduleId) {
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        stack.set(
            RiftfrontierCombatDataComponents.PLAYER_WEAPON_LOADOUT.value(),
            new PlayerWeaponLoadoutComponent(familyId.toString(), moduleId.map(Object::toString))
        );
        Component loadoutName = Component.translatable(loadoutTranslationKey(familyId, moduleId));
        stack.set(DataComponents.CUSTOM_NAME, loadoutName);

        boolean inserted = player.addItem(stack);
        if (!inserted) player.drop(stack, false);

        player.sendSystemMessage(Component.translatable("riftfrontier.combat.loadout_issued", loadoutName));
    }

    private static String loadoutTranslationKey(ContentId familyId, Optional<ContentId> moduleId) {
        boolean pivot = moduleId.filter(PlayerWeaponItemStackLoadoutResolver.RECOVERY_PIVOT::equals).isPresent();
        if (PlayerWeaponItemStackLoadoutResolver.MOBILE_PRESSURE.equals(familyId)) {
            return pivot ? "riftfrontier.combat.loadout.mobile_pivot" : "riftfrontier.combat.loadout.mobile";
        }
        if (PlayerWeaponItemStackLoadoutResolver.REACH_COMMITMENT.equals(familyId)) {
            return pivot ? "riftfrontier.combat.loadout.reach_pivot" : "riftfrontier.combat.loadout.reach";
        }
        return "riftfrontier.combat.loadout.unknown";
    }
}

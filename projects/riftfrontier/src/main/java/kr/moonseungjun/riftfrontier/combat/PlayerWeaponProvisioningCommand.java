package kr.moonseungjun.riftfrontier.combat;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
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
 * currently published content graph.</p>
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

    private static int provision(
        ServerPlayer player,
        kr.moonseungjun.riftfrontier.content.ContentId familyId,
        Optional<kr.moonseungjun.riftfrontier.content.ContentId> moduleId
    ) {
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        stack.set(
            RiftfrontierCombatDataComponents.PLAYER_WEAPON_LOADOUT.value(),
            new PlayerWeaponLoadoutComponent(familyId.toString(), moduleId.map(Object::toString))
        );

        boolean inserted = player.addItem(stack);
        if (!inserted) player.drop(stack, false);

        String module = moduleId.map(id -> " + " + id).orElse("");
        player.sendSystemMessage(Component.literal(
            "Riftfrontier combat loadout issued: " + familyId + module + ". Hold the item in your main hand and bind the two Riftfrontier combat actions in Controls."
        ));
        return Command.SINGLE_SUCCESS;
    }
}

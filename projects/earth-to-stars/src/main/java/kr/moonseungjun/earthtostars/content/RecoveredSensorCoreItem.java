package kr.moonseungjun.earthtostars.content;

import kr.moonseungjun.earthtostars.ship.runtime.minecraft.OrbitalMissionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class RecoveredSensorCoreItem extends Item {
    public RecoveredSensorCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }

        OrbitalMissionManager.ScannerInstallResult result = OrbitalMissionManager.installRecoveredScanner(player);
        return switch (result) {
            case INSTALLED -> {
                if (!player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.upgrade.sensor_installed"));
                yield InteractionResult.SUCCESS_SERVER;
            }
            case EARTH_ONLY -> {
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.upgrade.sensor_earth_only"));
                yield InteractionResult.FAIL;
            }
            case NO_ACCESSIBLE_SHIP -> {
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.upgrade.sensor_no_ship"));
                yield InteractionResult.FAIL;
            }
            case ALREADY_INSTALLED -> {
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.upgrade.sensor_already"));
                yield InteractionResult.FAIL;
            }
            case SLOT_UNAVAILABLE -> {
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.upgrade.sensor_slot_blocked"));
                yield InteractionResult.FAIL;
            }
        };
    }
}

package kr.moonseungjun.riftfrontier.expedition;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * Minecraft-native technical interaction stations for the first hub loop.
 *
 * These vanilla blocks are validation affordances only, not final hub art/UI language. They intentionally
 * delegate every resource and lifecycle mutation to ExpeditionGameplayService so the existing server-authoritative
 * save contracts remain canonical.
 */
public final class ExpeditionHubTerminal {
    // Mirrors the bounded M2-B technical hub cell owned by ExpeditionGameplayService. This validation-only
    // presentation layer must move with that cell if the technical fixture is relocated before final hub authoring.
    private static final BlockPos TECHNICAL_HUB = new BlockPos(0, 100, 0);
    private static final int PROVISION_OFFSET_X = -2;
    private static final int DEPLOY_OFFSET_X = 2;

    private ExpeditionHubTerminal() {}

    /** Restores the two temporary hub affordances after an authoritative extraction returns the player home. */
    public static void ensurePresent(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        if (level != level.getServer().overworld()) return;
        level.setBlockAndUpdate(provisionPosition(), Blocks.SMITHING_TABLE.defaultBlockState());
        level.setBlockAndUpdate(deployPosition(), Blocks.LODESTONE.defaultBlockState());
    }

    /** Routes world interaction into the existing authoritative provision/start services. */
    public static boolean tryUse(ServerPlayer player, BlockPos clickedPos) {
        ServerLevel level = (ServerLevel) player.level();
        if (level != level.getServer().overworld()) return false;

        if (clickedPos.equals(provisionPosition()) && level.getBlockState(clickedPos).is(Blocks.SMITHING_TABLE)) {
            try {
                ExpeditionGameplayService.provision(player);
            } catch (IllegalStateException rejected) {
                player.sendSystemMessage(Component.literal(
                    "[Riftfrontier] Provision station unavailable: " + rejected.getMessage()
                ));
            }
            return true;
        }

        if (clickedPos.equals(deployPosition()) && level.getBlockState(clickedPos).is(Blocks.LODESTONE)) {
            try {
                ExpeditionGameplayService.start(player);
            } catch (IllegalStateException rejected) {
                player.sendSystemMessage(Component.literal(
                    "[Riftfrontier] Deployment station unavailable: " + rejected.getMessage()
                ));
            }
            return true;
        }

        return false;
    }

    static BlockPos provisionPosition() { return TECHNICAL_HUB.offset(PROVISION_OFFSET_X, 0, 0); }

    static BlockPos deployPosition() { return TECHNICAL_HUB.offset(DEPLOY_OFFSET_X, 0, 0); }
}

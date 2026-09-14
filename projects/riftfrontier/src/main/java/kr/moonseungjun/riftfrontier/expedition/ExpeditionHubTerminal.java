package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.persistence.RiftfrontierWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

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

    /**
     * Makes the very first vertical-slice deployment reachable without a command-only bootstrap.
     *
     * <p>This runs only while the authoritative world has no expedition history and no active run. It builds the same
     * bounded technical hub fixture already used by the expedition service, moves the player there once, and exposes
     * the existing deployment station. No station-owned lifecycle or balance state is introduced.</p>
     */
    public static boolean bootstrapFreshWorld(ServerPlayer player) {
        ServerLevel currentLevel = (ServerLevel) player.level();
        ServerLevel overworld = currentLevel.getServer().overworld();
        RiftfrontierWorldData world = RiftfrontierWorldData.get(overworld);
        if (ExpeditionGameplayService.active(world).isPresent() || !world.expeditions().isEmpty()) return false;

        prepareTechnicalHub(overworld);
        boolean moved = player.teleportTo(
            overworld,
            TECHNICAL_HUB.getX() + 0.5D,
            TECHNICAL_HUB.getY(),
            TECHNICAL_HUB.getZ() + 0.5D,
            Set.<Relative>of(),
            player.getYRot(),
            player.getXRot(),
            false
        );
        if (!moved) throw new IllegalStateException("Minecraft rejected Riftfrontier initial hub teleport");

        ensurePresent(player);
        player.sendSystemMessage(Component.translatable("riftfrontier.expedition.detail.hub_bootstrap"));
        ExpeditionPlayerFeedback.hubReady(player);
        return true;
    }

    /** Routes world interaction into the existing authoritative provision/start services. */
    public static boolean tryUse(ServerPlayer player, BlockPos clickedPos) {
        ServerLevel level = (ServerLevel) player.level();
        if (level != level.getServer().overworld()) return false;

        if (clickedPos.equals(provisionPosition()) && level.getBlockState(clickedPos).is(Blocks.SMITHING_TABLE)) {
            try {
                ExpeditionGameplayService.provision(player);
                ExpeditionPlayerFeedback.provisioned(player);
            } catch (IllegalStateException rejected) {
                RiftfrontierWorldData world = RiftfrontierWorldData.get(level);
                player.sendSystemMessage(Component.translatable(
                    "riftfrontier.expedition.detail.provision_rejected",
                    world.securedRegion01Salvage(),
                    world.expeditionSupply(),
                    world.region01PreparationSupplyCost()
                ));
            }
            return true;
        }

        if (clickedPos.equals(deployPosition()) && level.getBlockState(clickedPos).is(Blocks.LODESTONE)) {
            try {
                ExpeditionGameplayService.start(player);
                ExpeditionPlayerFeedback.deployed(player);
            } catch (IllegalStateException rejected) {
                RiftfrontierWorldData world = RiftfrontierWorldData.get(level);
                player.sendSystemMessage(Component.translatable(
                    "riftfrontier.expedition.detail.deploy_rejected",
                    world.expeditionSupply(),
                    world.region01PreparationSupplyCost()
                ));
            }
            return true;
        }

        return false;
    }

    private static void prepareTechnicalHub(ServerLevel level) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                level.setBlockAndUpdate(TECHNICAL_HUB.offset(dx, -1, dz), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int dy = 0; dy <= 4; dy++) {
                    level.setBlockAndUpdate(TECHNICAL_HUB.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    static BlockPos provisionPosition() { return TECHNICAL_HUB.offset(PROVISION_OFFSET_X, 0, 0); }

    static BlockPos deployPosition() { return TECHNICAL_HUB.offset(DEPLOY_OFFSET_X, 0, 0); }
}

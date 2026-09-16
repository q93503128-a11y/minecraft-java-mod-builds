package kr.moonseungjun.earthtostars.fabric.content;

import kr.moonseungjun.earthtostars.fabric.entity.LaunchCraftEntity;
import kr.moonseungjun.earthtostars.fabric.ship.EarthToStarsFabricShipAuthority;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.gameplay.LaunchCraftBlueprint;
import kr.moonseungjun.earthtostars.ship.runtime.ShipFlightRuntime;
import kr.moonseungjun.earthtostars.ship.runtime.ShipFlightTuning;
import kr.moonseungjun.earthtostars.ship.runtime.ShipTransform;
import kr.moonseungjun.earthtostars.ship.runtime.ShipVec3;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class LaunchCraftKitItem extends Item {
    private static final int DEPLOY_RADIUS = 2;
    private static final int DEPLOY_HEIGHT = 3;

    public LaunchCraftKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.FAIL;
        }
        if (!serverLevel.dimension().equals(Level.OVERWORLD)) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch_craft.earth_only"));
            return InteractionResult.FAIL;
        }
        if (EarthToStarsFabricShipAuthority.findOwnedShip(player.getUUID()).isPresent()) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch_craft.already_owned"));
            return InteractionResult.FAIL;
        }

        BlockPos base = context.getClickedPos().relative(context.getClickedFace());
        if (!hasClearance(serverLevel, base)) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch_craft.clearance"));
            return InteractionResult.FAIL;
        }

        double spawnX = base.getX() + 0.5D;
        double spawnY = base.getY() + 0.05D;
        double spawnZ = base.getZ() + 0.5D;

        ShipState ship = ShipState.create(
                ShipId.random(),
                player.getUUID(),
                LaunchCraftBlueprint.slots()
        );
        LaunchCraftBlueprint.installStarterModules(ship, kr.moonseungjun.earthtostars.fabric.EarthToStarsFabric.bootstrapCatalog());

        ShipTransform transform = new ShipTransform(
                new ShipVec3(spawnX, spawnY, spawnZ),
                ShipVec3.ZERO,
                player.getYRot(),
                0.0D
        );
        ShipFlightRuntime runtime = new ShipFlightRuntime(ship, transform, ShipFlightTuning.P0);

        try {
            EarthToStarsFabricShipAuthority.addPersistentShip(ship);
            EarthToStarsFabricShipAuthority.activateRuntime(runtime);

            LaunchCraftEntity craft = new LaunchCraftEntity(EarthToStarsFabricEntities.LAUNCH_CRAFT, serverLevel);
            craft.bindShipId(ship.shipId());
            craft.setPos(spawnX, spawnY, spawnZ);
            craft.setYRot(player.getYRot());
            if (!serverLevel.addFreshEntity(craft)) {
                EarthToStarsFabricShipAuthority.removePersistentShip(ship.shipId());
                player.sendSystemMessage(Component.translatable("message.earth_to_stars.launch_craft.deploy_failed"));
                return InteractionResult.FAIL;
            }
        } catch (RuntimeException deploymentFailure) {
            EarthToStarsFabricShipAuthority.findShip(ship.shipId()).ifPresent(ignored ->
                    EarthToStarsFabricShipAuthority.removePersistentShip(ship.shipId())
            );
            throw deploymentFailure;
        }

        if (!player.hasInfiniteMaterials()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean hasClearance(ServerLevel level, BlockPos base) {
        for (int y = 0; y < DEPLOY_HEIGHT; y++) {
            for (int x = -DEPLOY_RADIUS; x <= DEPLOY_RADIUS; x++) {
                for (int z = -DEPLOY_RADIUS; z <= DEPLOY_RADIUS; z++) {
                    BlockPos cursor = base.offset(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (!level.getFluidState(cursor).isEmpty()) {
                        return false;
                    }
                    if (!state.isAir()) {
                        if (!state.canBeReplaced() || !state.getCollisionShape(level, cursor).isEmpty()) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}

package kr.moonseungjun.earthtostars.fabric.entity;

import kr.moonseungjun.earthtostars.fabric.EarthToStarsFabric;
import kr.moonseungjun.earthtostars.fabric.ship.EarthToStarsFabricShipAuthority;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipPermission;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;
import kr.moonseungjun.earthtostars.ship.runtime.ShipFlightRuntime;
import kr.moonseungjun.earthtostars.ship.runtime.ShipTransform;
import kr.moonseungjun.earthtostars.ship.runtime.ShipVec3;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public final class LaunchCraftEntity extends VehicleEntity {
    private static final String TAG_SHIP_ID = "ship_id";
    private static final double CONTROL_RANGE_SQR = 64.0D;
    private static final EntityDataAccessor<String> DATA_SHIP_ID = SynchedEntityData.defineId(
            LaunchCraftEntity.class,
            EntityDataSerializers.STRING
    );

    private UUID lastControllerId;

    public LaunchCraftEntity(EntityType<? extends LaunchCraftEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SHIP_ID, "");
    }

    public void bindShipId(ShipId shipId) {
        String current = entityData.get(DATA_SHIP_ID);
        if (!current.isEmpty() && !current.equals(shipId.toString())) {
            throw new IllegalStateException("launch craft is already bound to ship " + current);
        }
        entityData.set(DATA_SHIP_ID, shipId.toString());
    }

    public Optional<ShipId> shipId() {
        String encoded = entityData.get(DATA_SHIP_ID);
        if (encoded.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new ShipId(UUID.fromString(encoded)));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        shipId().ifPresent(id -> output.putString(TAG_SHIP_ID, id.toString()));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String encoded = input.getStringOr(TAG_SHIP_ID, "");
        if (encoded.isEmpty()) {
            entityData.set(DATA_SHIP_ID, "");
            return;
        }
        try {
            bindShipId(new ShipId(UUID.fromString(encoded)));
        } catch (IllegalArgumentException malformed) {
            EarthToStarsFabric.LOGGER.error("Discarding malformed launch craft ship id {}", encoded);
            entityData.set(DATA_SHIP_ID, "");
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }

        Optional<ShipId> optionalShipId = shipId();
        if (optionalShipId.isEmpty()) {
            EarthToStarsFabric.LOGGER.error("Discarding launch craft entity {} without stable ship id", getUUID());
            discard();
            return;
        }
        ShipId shipId = optionalShipId.get();

        if (!EarthToStarsFabricShipAuthority.bindPhysicalEntity(shipId, getUUID())) {
            EarthToStarsFabric.LOGGER.error(
                    "Discarding duplicate physical launch craft entity={} ship={}",
                    getUUID(),
                    shipId
            );
            discard();
            return;
        }

        ShipState ship = EarthToStarsFabricShipAuthority.findShip(shipId).orElse(null);
        if (ship == null) {
            EarthToStarsFabric.LOGGER.error("Discarding orphaned launch craft entity={} ship={}", getUUID(), shipId);
            discard();
            return;
        }

        ShipFlightRuntime runtime = EarthToStarsFabricShipAuthority.ensureStarterRuntime(
                ship,
                transformFromEntity()
        );

        if (lastControllerId != null && getPassengers().stream().noneMatch(passenger -> passenger.getUUID().equals(lastControllerId))) {
            EarthToStarsFabricShipAuthority.releaseControl(lastControllerId);
            lastControllerId = null;
        }

        runtime.tick(level().getGameTime()).ifPresent(expiredController -> {
            EarthToStarsFabricShipAuthority.onControlLeaseExpired(shipId, expiredController);
            if (expiredController.equals(lastControllerId)) {
                lastControllerId = null;
            }
        });

        ShipTransform target = runtime.transform();
        Vec3 before = position();
        ShipVec3 targetPos = target.position();
        move(MoverType.SELF, new Vec3(
                targetPos.x() - before.x,
                targetPos.y() - before.y,
                targetPos.z() - before.z
        ));
        Vec3 resolved = position();
        Vec3 resolvedVelocity = resolved.subtract(before);
        setDeltaMovement(resolvedVelocity);
        setYRot((float) target.yawDegrees());
        setXRot((float) target.pitchDegrees());
        runtime.reconcileMotion(new ShipTransform(
                new ShipVec3(resolved.x, resolved.y, resolved.z),
                new ShipVec3(resolvedVelocity.x, resolvedVelocity.y, resolvedVelocity.z),
                target.yawDegrees(),
                target.pitchDegrees()
        ));
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        ShipId shipId = shipId().orElse(null);
        if (shipId == null || distanceToSqr(player) > CONTROL_RANGE_SQR) {
            return InteractionResult.FAIL;
        }
        ShipState ship = EarthToStarsFabricShipAuthority.findShip(shipId).orElse(null);
        if (ship == null || !ship.can(player.getUUID(), ShipPermission.PILOT)) {
            serverPlayer.displayClientMessage(Component.translatable("message.earth_to_stars.launch_craft.no_pilot_access"), true);
            return InteractionResult.FAIL;
        }
        if (!getPassengers().isEmpty() && !hasPassenger(player)) {
            serverPlayer.displayClientMessage(Component.translatable("message.earth_to_stars.launch_craft.seat_occupied"), true);
            return InteractionResult.FAIL;
        }

        EarthToStarsFabricShipAuthority.ensureStarterRuntime(ship, transformFromEntity());
        if (!hasPassenger(player) && !player.startRiding(this, true)) {
            return InteractionResult.FAIL;
        }
        if (EarthToStarsFabricShipAuthority.grantControl(serverPlayer, shipId, level().getGameTime()).isEmpty()) {
            if (player.getVehicle() == this) {
                player.stopRiding();
            }
            return InteractionResult.FAIL;
        }
        lastControllerId = player.getUUID();
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        return position().add(0.0D, 1.05D, 0.15D);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith(Entity entity) {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        // Physical damage must not destroy the entity until ship damage/destruction is bridged
        // transactionally to authoritative ShipState. Dropping a vehicle item here could duplicate a ship.
        return false;
    }

    @Override
    protected Item getDropItem() {
        return Items.AIR;
    }

    @Override
    public void onRemovedFromLevel() {
        shipId().ifPresent(id -> EarthToStarsFabricShipAuthority.unbindPhysicalEntity(id, getUUID()));
        if (lastControllerId != null) {
            EarthToStarsFabricShipAuthority.releaseControl(lastControllerId);
            lastControllerId = null;
        }
        super.onRemovedFromLevel();
    }

    private ShipTransform transformFromEntity() {
        Vec3 velocity = getDeltaMovement();
        return new ShipTransform(
                new ShipVec3(getX(), getY(), getZ()),
                new ShipVec3(velocity.x, velocity.y, velocity.z),
                getYRot(),
                getXRot()
        );
    }
}

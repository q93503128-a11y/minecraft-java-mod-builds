package kr.moonseungjun.earthtostars.ship.runtime.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.SynchedEntityData;

/**
 * Authoritative ride/interaction shell for a modular ship.
 *
 * This entity deliberately owns no persistent ship state. ShipState/SavedData remain
 * the authority and this exterior is recreated when needed. Rendering is supplied by
 * a separately managed production visual so vehicle hitbox and art can evolve
 * independently without bringing back temporary proxy collision hacks.
 */
public final class ShipExteriorEntity extends Entity {
    public ShipExteriorEntity(EntityType<? extends ShipExteriorEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /**
     * Punching a ship is intentionally non-destructive. Pack-up is an explicit
     * owner interaction so an accidental hit can never delete only the runtime
     * shell while leaving authoritative save state behind.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.earth_to_stars.ship.retire_hint"), true);
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            return ShipRuntimeManager.retireCraft(serverPlayer, this)
                    ? InteractionResult.SUCCESS_SERVER
                    : InteractionResult.FAIL;
        }
        return ShipRuntimeManager.boardAndControl(serverPlayer, this)
                ? InteractionResult.SUCCESS_SERVER
                : InteractionResult.FAIL;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && getPassengers().isEmpty();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }
}

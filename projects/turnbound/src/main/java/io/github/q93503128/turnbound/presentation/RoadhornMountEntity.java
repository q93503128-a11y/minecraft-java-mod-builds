package io.github.q93503128.turnbound.presentation;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Player-controlled rental mount used by the physical waystation network. */
public final class RoadhornMountEntity extends AbstractHorse implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.roadhorn_mount.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.roadhorn_mount.walk");
    private static final RawAnimation GALLOP = RawAnimation.begin().thenLoop("animation.roadhorn_mount.gallop");
    private static final RawAnimation JUMP = RawAnimation.begin().thenLoop("animation.roadhorn_mount.jump");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public RoadhornMountEntity(EntityType<? extends RoadhornMountEntity> type, Level level) {
        super(type, level);
        setTamed(true);
        setInvulnerable(true);
    }

    @Override
    protected void registerGoals() {
        // Rental mounts do not wander away from a waystation/player when dismounted.
    }

    @Override
    public boolean isSaddled() {
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        // Rentals are server-runtime objects. They are recalled/recreated from physical waystations.
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player == null || isBaby()) return InteractionResult.PASS;
        if (!level().isClientSide() && !isVehicle()) {
            setOwner(player);
            setTamed(true);
            doPlayerRide(player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        // No vanilla horse inventory/saddle management for a waystation rental.
    }

    public void ride(Player player) {
        if (player == null || level().isClientSide()) return;
        setOwner(player);
        setTamed(true);
        doPlayerRide(player);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<RoadhornMountEntity>("movement", 3, test -> {
            RoadhornMountEntity mount = test.animatable();
            if (!mount.onGround()) return test.setAndContinue(JUMP);
            if (!test.isMoving()) return test.setAndContinue(IDLE);
            var velocity = mount.getDeltaMovement();
            double horizontalSpeedSq = velocity.x * velocity.x + velocity.z * velocity.z;
            return test.setAndContinue(horizontalSpeedSq >= 0.055D ? GALLOP : WALK);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}

package io.github.q93503128.turnbound.presentation;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/** Visual-only GeckoLib combat actor. Battle logic remains fully server-authoritative in BattleEngine. */
public final class BattleActorEntity extends PathfinderMob implements GeoEntity {
    private static final RawAnimation CAST = RawAnimation.begin().thenPlay("attack.cast");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public BattleActorEntity(EntityType<? extends BattleActorEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(true);
        setInvulnerable(true);
        setSilent(true);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() { }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BattleActorEntity>("combat", 3,
                test -> test.setAndContinue(DefaultAnimations.IDLE))
                .triggerableAnim("strike", DefaultAnimations.ATTACK_STRIKE)
                .triggerableAnim("cast", CAST));
    }

    public void playStrike() { triggerAnim("combat", "strike"); }
    public void playCast() { triggerAnim("combat", "cast"); }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void doPush(Entity entity) { }
}

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

/** Visual-only GeckoLib combat/field actor. Authoritative gameplay logic remains outside the entity. */
public final class BattleActorEntity extends PathfinderMob implements GeoEntity {
    private static final RawAnimation CAST = RawAnimation.begin().thenPlay("attack.cast");
    private static final RawAnimation READY = RawAnimation.begin().thenPlay("misc.turn_ready");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlay("misc.death");
    private static final RawAnimation REVIVE = RawAnimation.begin().thenPlay("misc.revive");
    private static final RawAnimation VICTORY = RawAnimation.begin().thenPlay("misc.victory");
    private static final RawAnimation TELEGRAPH = RawAnimation.begin().thenPlay("boss.telegraph");
    private static final RawAnimation CHARGE = RawAnimation.begin().thenPlay("boss.charge");
    private static final RawAnimation SUMMON = RawAnimation.begin().thenPlay("boss.summon");
    private static final RawAnimation PHASE = RawAnimation.begin().thenPlay("boss.phase_enter");
    private static final RawAnimation FIELD_WALK = RawAnimation.begin().thenLoop("field.walk");
    private static final RawAnimation FIELD_IDLE = RawAnimation.begin().thenLoop("field.idle");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private boolean fieldModeInitialized;
    private boolean fieldWalking;

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
                .triggerableAnim("cast", CAST)
                .triggerableAnim("ready", READY)
                .triggerableAnim("death", DEATH)
                .triggerableAnim("revive", REVIVE)
                .triggerableAnim("victory", VICTORY)
                .triggerableAnim("telegraph", TELEGRAPH)
                .triggerableAnim("charge", CHARGE)
                .triggerableAnim("summon", SUMMON)
                .triggerableAnim("phase", PHASE)
                .triggerableAnim("field_walk", FIELD_WALK)
                .triggerableAnim("field_idle", FIELD_IDLE));
    }

    public void playStrike() { triggerAnim("combat", "strike"); }
    public void playCast() { triggerAnim("combat", "cast"); }
    public void playReady() { triggerAnim("combat", "ready"); }
    public void playDeath() { triggerAnim("combat", "death"); }
    public void playRevive() { triggerAnim("combat", "revive"); }
    public void playVictory() { triggerAnim("combat", "victory"); }
    public void playTelegraph() { triggerAnim("combat", "telegraph"); }
    public void playCharge() { triggerAnim("combat", "charge"); }
    public void playSummon() { triggerAnim("combat", "summon"); }
    public void playPhase() { triggerAnim("combat", "phase"); }

    /** Switches the authored actor between the looping field locomotion clips without retrigger spam. */
    public void setFieldWalking(boolean walking) {
        if (fieldModeInitialized && fieldWalking == walking) return;
        fieldModeInitialized = true;
        fieldWalking = walking;
        triggerAnim("combat", walking ? "field_walk" : "field_idle");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void doPush(Entity entity) { }
}

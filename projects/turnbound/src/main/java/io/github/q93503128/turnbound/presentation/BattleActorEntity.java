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

    @Override protected void registerGoals() { }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String prefix = TurnboundBattleActors.heroAnimationPrefix(getType());
        RawAnimation idle = prefix == null ? DefaultAnimations.IDLE : loop(prefix, "idle");
        RawAnimation ready = prefix == null ? READY : play(prefix, "turn_ready");
        RawAnimation basic = prefix == null ? DefaultAnimations.ATTACK_STRIKE : play(prefix, "basic");
        RawAnimation active1 = prefix == null ? CAST : play(prefix, "active_1");
        RawAnimation active2 = prefix == null ? CAST : play(prefix, "active_2");
        RawAnimation reaction = prefix == null ? DefaultAnimations.ATTACK_STRIKE
                : play(prefix, (prefix.equals("p03_bram") || prefix.equals("p05_lynette")) ? "reaction" : "basic");
        RawAnimation hitLight = prefix == null ? ready : play(prefix, "hit_light");
        RawAnimation hitHeavy = prefix == null ? ready : play(prefix, "hit_heavy");
        RawAnimation death = prefix == null ? DEATH : play(prefix, "death");
        RawAnimation revive = prefix == null ? REVIVE : play(prefix, "revive");
        RawAnimation victory = prefix == null ? VICTORY : play(prefix, "victory");

        controllers.add(new AnimationController<BattleActorEntity>("combat", 3, test -> test.setAndContinue(idle))
                .triggerableAnim("strike", DefaultAnimations.ATTACK_STRIKE)
                .triggerableAnim("cast", CAST)
                .triggerableAnim("ready", ready)
                .triggerableAnim("basic", basic)
                .triggerableAnim("active_1", active1)
                .triggerableAnim("active_2", active2)
                .triggerableAnim("reaction", reaction)
                .triggerableAnim("hit_light", hitLight)
                .triggerableAnim("hit_heavy", hitHeavy)
                .triggerableAnim("death", death)
                .triggerableAnim("revive", revive)
                .triggerableAnim("victory", victory)
                .triggerableAnim("telegraph", TELEGRAPH)
                .triggerableAnim("charge", CHARGE)
                .triggerableAnim("summon", SUMMON)
                .triggerableAnim("phase", PHASE)
                .triggerableAnim("field_walk", FIELD_WALK)
                .triggerableAnim("field_idle", FIELD_IDLE));
    }

    private static RawAnimation play(String prefix, String clip) {
        return RawAnimation.begin().thenPlay("animation." + prefix + "." + clip);
    }

    private static RawAnimation loop(String prefix, String clip) {
        return RawAnimation.begin().thenLoop("animation." + prefix + "." + clip);
    }

    public void playStrike() { triggerAnim("combat", "strike"); }
    public void playCast() { triggerAnim("combat", "cast"); }
    public void playReady() { triggerAnim("combat", "ready"); }
    public void playBasic() { triggerAnim("combat", "basic"); }
    public void playActive1() { triggerAnim("combat", "active_1"); }
    public void playActive2() { triggerAnim("combat", "active_2"); }
    public void playReaction() { triggerAnim("combat", "reaction"); }
    public void playHit(boolean heavy) { triggerAnim("combat", heavy ? "hit_heavy" : "hit_light"); }
    public void playDeath() { triggerAnim("combat", "death"); }
    public void playRevive() { triggerAnim("combat", "revive"); }
    public void playVictory() { triggerAnim("combat", "victory"); }
    public void playTelegraph() { triggerAnim("combat", "telegraph"); }
    public void playCharge() { triggerAnim("combat", "charge"); }
    public void playSummon() { triggerAnim("combat", "summon"); }
    public void playPhase() { triggerAnim("combat", "phase"); }

    /** Switches authored field actors between locomotion clips without per-tick retrigger spam. */
    public void setFieldWalking(boolean walking) {
        if (fieldModeInitialized && fieldWalking == walking) return;
        fieldModeInitialized = true;
        fieldWalking = walking;
        triggerAnim("combat", walking ? "field_walk" : "field_idle");
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }
    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(Entity entity) { }
}

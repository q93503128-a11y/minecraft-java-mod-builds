package io.github.q93503128.turnbound.presentation;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Visual-only GeckoLib combat/field actor. Authoritative gameplay logic remains outside the entity. */
public final class BattleActorEntity extends PathfinderMob implements GeoEntity {
    private static final RawAnimation CAST = RawAnimation.begin().thenPlay("attack.cast");
    private static final RawAnimation READY = RawAnimation.begin().thenPlay("misc.turn_ready");
    private static final RawAnimation HIT = RawAnimation.begin().thenPlay("combat.hit");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlay("misc.death");
    private static final RawAnimation REVIVE = RawAnimation.begin().thenPlay("misc.revive");
    private static final RawAnimation VICTORY = RawAnimation.begin().thenPlay("misc.victory");
    private static final RawAnimation TELEGRAPH = RawAnimation.begin().thenPlay("boss.telegraph");
    private static final RawAnimation CHARGE = RawAnimation.begin().thenPlay("boss.charge");
    private static final RawAnimation SUMMON = RawAnimation.begin().thenPlay("boss.summon");
    private static final RawAnimation PHASE = RawAnimation.begin().thenPlay("boss.phase_enter");
    private static final RawAnimation BOSS_ARMOR_BREAK_HOLD = RawAnimation.begin().thenPlay("boss.armor_broken_hold");
    private static final RawAnimation BOSS_STAGGER = RawAnimation.begin().thenPlay("boss.stagger");
    private static final RawAnimation BOSS_HIT_LIGHT = RawAnimation.begin().thenPlay("boss.hit_light");
    private static final RawAnimation BOSS_HIT_HEAVY = RawAnimation.begin().thenPlay("boss.hit_heavy");
    private static final RawAnimation FIELD_WALK = RawAnimation.begin().thenLoop("field.walk");
    private static final RawAnimation FIELD_IDLE = RawAnimation.begin().thenLoop("field.idle");
    private static final RawAnimation SERVICE_GREET = RawAnimation.begin().thenPlay("field.greet");
    private static final RawAnimation SERVICE_WORK = RawAnimation.begin().thenPlay("field.work");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private boolean fieldModeInitialized;
    private boolean fieldWalking;
    private boolean fieldThreatAlerted;
    private boolean fieldNavigationEnabled;
    private float fieldHomeYaw;

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
        String prefix = TurnboundBattleActors.animationPrefix(getType());
        if (prefix == null) prefix = SignatureBattleActors.heroAnimationPrefix(getType());
        boolean serviceAnimations = prefix == null && DrabyelServiceActors.serviceAnimationType(getType());
        boolean bossAnimations = prefix == null && !serviceAnimations && TurnboundBattleActors.bossAnimationType(getType());
        RawAnimation idle = serviceAnimations ? FIELD_IDLE : prefix == null ? DefaultAnimations.IDLE : loop(prefix, "idle");
        RawAnimation ready = prefix == null ? READY : play(prefix, "turn_ready");
        RawAnimation basic = prefix == null ? DefaultAnimations.ATTACK_STRIKE : play(prefix, "basic");
        RawAnimation active1 = prefix == null ? CAST : play(prefix, "active_1");
        RawAnimation active2 = prefix == null ? CAST : play(prefix, "active_2");
        RawAnimation movingBasic = prefix == null ? DefaultAnimations.ATTACK_STRIKE : moveThen(prefix, "basic");
        RawAnimation movingActive1 = prefix == null ? DefaultAnimations.ATTACK_STRIKE : moveThen(prefix, "active_1");
        RawAnimation movingActive2 = prefix == null ? DefaultAnimations.ATTACK_STRIKE : moveThen(prefix, "active_2");
        RawAnimation reaction = prefix == null ? DefaultAnimations.ATTACK_STRIKE
                : play(prefix, (prefix.equals("p03_bram") || prefix.equals("p05_lynette")) ? "reaction" : "basic");
        RawAnimation hitLight = prefix == null ? (bossAnimations ? BOSS_HIT_LIGHT : HIT) : play(prefix, "hit_light");
        RawAnimation hitHeavy = prefix == null ? (bossAnimations ? BOSS_HIT_HEAVY : HIT) : play(prefix, "hit_heavy");
        RawAnimation death = prefix == null ? DEATH : play(prefix, "death");
        RawAnimation revive = prefix == null ? REVIVE : play(prefix, "revive");
        RawAnimation victory = prefix == null ? VICTORY : play(prefix, "victory");
        RawAnimation buff = prefix == null ? ready : play(prefix, "buff");
        RawAnimation debuff = prefix == null ? ready : play(prefix, "debuff");
        RawAnimation fieldWalk = prefix == null ? FIELD_WALK : loop(prefix, "field_walk");
        RawAnimation fieldIdle = prefix == null ? FIELD_IDLE : loop(prefix, "field_idle");

        controllers.add(new AnimationController<BattleActorEntity>("combat", 3, test -> test.setAndContinue(idle))
                .triggerableAnim("strike", DefaultAnimations.ATTACK_STRIKE)
                .triggerableAnim("cast", CAST)
                .triggerableAnim("ready", ready)
                .triggerableAnim("basic", basic)
                .triggerableAnim("active_1", active1)
                .triggerableAnim("active_2", active2)
                .triggerableAnim("moving_basic", movingBasic)
                .triggerableAnim("moving_active_1", movingActive1)
                .triggerableAnim("moving_active_2", movingActive2)
                .triggerableAnim("reaction", reaction)
                .triggerableAnim("hit_light", hitLight)
                .triggerableAnim("hit_heavy", hitHeavy)
                .triggerableAnim("death", death)
                .triggerableAnim("revive", revive)
                .triggerableAnim("victory", victory)
                .triggerableAnim("buff", buff)
                .triggerableAnim("debuff", debuff)
                .triggerableAnim("telegraph", TELEGRAPH)
                .triggerableAnim("charge", CHARGE)
                .triggerableAnim("summon", SUMMON)
                .triggerableAnim("phase", PHASE)
                .triggerableAnim("stagger", BOSS_STAGGER)
                .triggerableAnim("field_walk", fieldWalk)
                .triggerableAnim("field_idle", fieldIdle)
                .triggerableAnim("service_greet", SERVICE_GREET)
                .triggerableAnim("service_work", SERVICE_WORK));

        if (heroSignatureStateAnimations(prefix)) {
            AnimationController<BattleActorEntity> signatureState =
                    new AnimationController<BattleActorEntity>("hero_state", 4, test -> PlayState.STOP)
                            .additiveAnimations()
                            .triggerableAnim("state_0", loop(prefix, "state_0"))
                            .triggerableAnim("state_1", loop(prefix, "state_1"))
                            .triggerableAnim("state_2", loop(prefix, "state_2"))
                            .triggerableAnim("state_3", loop(prefix, "state_3"));
            if ("p08_raze".equals(prefix)) {
                signatureState.triggerableAnim("overheat", loop(prefix, "overheat"));
            }
            controllers.add(signatureState);
        }

        if (heroSignaturePayoffAnimations(prefix)) {
            AnimationController<BattleActorEntity> payoff =
                    new AnimationController<BattleActorEntity>("hero_payoff", 1, test -> PlayState.STOP)
                            .additiveAnimations()
                            .triggerableAnim("payoff", play(prefix, "signature_payoff"));
            if ("p06_morwen".equals(prefix)) {
                payoff.triggerableAnim("return", play(prefix, "signature_return"));
            }
            controllers.add(payoff);
        }

        if (bossAnimations) {
            controllers.add(new AnimationController<BattleActorEntity>("boss_phase", 3, test -> PlayState.STOP)
                    .additiveAnimations()
                    .triggerableAnim("phase", PHASE));
            controllers.add(new AnimationController<BattleActorEntity>("boss_damage", 2, test -> PlayState.STOP)
                    .additiveAnimations()
                    .triggerableAnim("armor_break", BOSS_ARMOR_BREAK_HOLD));
        }
    }

    private static boolean heroSignatureStateAnimations(String prefix) {
        return "p01_kyren".equals(prefix)
                || "p03_bram".equals(prefix)
                || "p05_lynette".equals(prefix)
                || "p06_morwen".equals(prefix)
                || "p07_marion".equals(prefix)
                || "p08_raze".equals(prefix)
                || "toto".equals(prefix);
    }

    private static boolean heroSignaturePayoffAnimations(String prefix) {
        return "p01_kyren".equals(prefix)
                || "p03_bram".equals(prefix)
                || "p04_elysia".equals(prefix)
                || "p05_lynette".equals(prefix)
                || "p06_morwen".equals(prefix)
                || "p07_marion".equals(prefix)
                || "p08_raze".equals(prefix)
                || "toto".equals(prefix);
    }

    private static RawAnimation play(String prefix, String clip) {
        return RawAnimation.begin().thenPlay("animation." + prefix + "." + clip);
    }

    private static RawAnimation loop(String prefix, String clip) {
        return RawAnimation.begin().thenLoop("animation." + prefix + "." + clip);
    }

    private static RawAnimation moveThen(String prefix, String clip) {
        return RawAnimation.begin()
                .thenPlay("animation." + prefix + ".move_attack")
                .thenPlay("animation." + prefix + "." + clip);
    }

    public void playStrike() { triggerAnim("combat", "strike"); }
    public void playCast() { triggerAnim("combat", "cast"); }
    public void playReady() { triggerAnim("combat", "ready"); }
    public void playBasic() { triggerAnim("combat", "basic"); }
    public void playActive1() { triggerAnim("combat", "active_1"); }
    public void playActive2() { triggerAnim("combat", "active_2"); }
    public void playMovingBasic() { triggerAnim("combat", "moving_basic"); }
    public void playMovingActive1() { triggerAnim("combat", "moving_active_1"); }
    public void playMovingActive2() { triggerAnim("combat", "moving_active_2"); }
    public void playReaction() { triggerAnim("combat", "reaction"); }
    public void playSignaturePayoff() { triggerAnim("hero_payoff", "payoff"); }
    public void playSignatureReturn() { triggerAnim("hero_payoff", "return"); }
    public void playHit(boolean heavy) { triggerAnim("combat", heavy ? "hit_heavy" : "hit_light"); }
    public void playDeath() { triggerAnim("combat", "death"); }
    public void playRevive() { triggerAnim("combat", "revive"); }
    public void playVictory() { triggerAnim("combat", "victory"); }
    public void playBuff() { triggerAnim("combat", "buff"); }
    public void playDebuff() { triggerAnim("combat", "debuff"); }

    /** Applies persistent additive body language without taking ownership away from combat/action clips. */
    public void setHeroSignatureState(int stage, boolean overheat) {
        String prefix = TurnboundBattleActors.heroAnimationPrefix(getType());
        if (prefix == null) prefix = SignatureBattleActors.heroAnimationPrefix(getType());
        if (!heroSignatureStateAnimations(prefix)) return;
        int resolved = Math.max(0, Math.min(3, stage));
        triggerAnim("hero_state", overheat && "p08_raze".equals(prefix) ? "overheat" : "state_" + resolved);
    }

    public void playTelegraph() { triggerAnim("combat", "telegraph"); }
    public void playCharge() { triggerAnim("combat", "charge"); }
    public void playSummon() { triggerAnim("combat", "summon"); }
    public void playServiceGreeting() { triggerAnim("combat", "service_greet"); }
    public void playServiceWork() { triggerAnim("combat", "service_work"); }
    public void playPhase() {
        if (TurnboundBattleActors.bossAnimationType(getType())) triggerAnim("boss_phase", "phase");
        else triggerAnim("combat", "phase");
    }
    public void playBossStagger() {
        triggerAnim("combat", "stagger");
        if (TurnboundBattleActors.bossAnimationType(getType())) triggerAnim("boss_damage", "armor_break");
    }

    /** Switches authored field actors between locomotion clips without per-tick retrigger spam. */
    public void setFieldWalking(boolean walking) {
        if (!fieldModeInitialized) fieldHomeYaw = getYRot();
        if (fieldModeInitialized && fieldWalking == walking) return;
        fieldModeInitialized = true;
        fieldWalking = walking;
        triggerAnim("combat", walking ? "field_walk" : "field_idle");
    }

    /**
     * Enables ordinary Minecraft path navigation for a free-roaming field silhouette.
     * Battle actors remain frozen/no-gravity by default; only the external-world encounter runtime opts into this.
     */
    public boolean moveFieldTo(double x, double y, double z, double speedModifier) {
        if (!fieldNavigationEnabled) {
            fieldNavigationEnabled = true;
            setNoAi(false);
            setNoGravity(false);
            var speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.23D);
        }
        boolean accepted = getNavigation().moveTo(x, y, z, Math.max(0.05D, speedModifier));
        setFieldWalking(accepted);
        return accepted;
    }

    public void stopFieldNavigation() {
        if (fieldNavigationEnabled) getNavigation().stop();
        setFieldWalking(false);
    }

    public boolean fieldNavigationDone() {
        return !fieldNavigationEnabled || getNavigation().isDone();
    }

    @Override
    public void tick() {
        super.tick();
        tickFieldThreatPrelude();
    }

    /**
     * Field-only pre-combat body language. Ordinary enemies acquire the party late; elites expose their name and
     * ready pose; bosses reveal earlier and play their authored telegraph once per approach. When the party backs
     * away, actors smoothly return to the authored formation facing instead of remaining twisted toward where the
     * player happened to leave.
     */
    private void tickFieldThreatPrelude() {
        if (!fieldModeInitialized || level().isClientSide()) return;
        int tier = TurnboundBattleActors.fieldThreatTier(getType());
        if (tier <= 0) return;

        double searchRadius = tier >= 3 ? 30.0 : tier == 2 ? 20.0 : 12.0;
        double alertRadius = tier >= 3 ? 16.0 : tier == 2 ? 11.0 : 9.0;
        double revealRadius = tier >= 3 ? 26.0 : tier == 2 ? 16.0 : 0.0;
        Player player = level().getNearestPlayer(this, searchRadius);

        if (player == null) {
            fieldThreatAlerted = false;
            if (tier >= 2) setCustomNameVisible(false);
            returnFieldFacing();
            return;
        }

        double distanceSq = distanceToSqr(player);
        if (tier >= 2) setCustomNameVisible(distanceSq <= revealRadius * revealRadius);

        boolean alert = distanceSq <= alertRadius * alertRadius;
        if (alert) {
            faceFieldTarget(player);
            if (!fieldThreatAlerted) {
                fieldThreatAlerted = true;
                if (tier >= 3) playTelegraph();
                else if (tier == 2) playReady();
            }
        } else {
            if (fieldThreatAlerted && distanceSq > (alertRadius + 4.0) * (alertRadius + 4.0)) fieldThreatAlerted = false;
            if (!fieldThreatAlerted) returnFieldFacing();
        }
    }

    private void faceFieldTarget(Player player) {
        double dx = player.getX() - getX();
        double dz = player.getZ() - getZ();
        if (dx * dx + dz * dz <= 0.000001) return;
        setFieldYaw((float)Math.toDegrees(Math.atan2(-dx, dz)));
    }

    private void returnFieldFacing() {
        float current = getYRot();
        float delta = wrapDegrees(fieldHomeYaw - current);
        if (Math.abs(delta) < 0.5F) {
            setFieldYaw(fieldHomeYaw);
            return;
        }
        setFieldYaw(current + delta * 0.18F);
    }

    private void setFieldYaw(float yaw) {
        setYRot(yaw);
        setYHeadRot(yaw);
        setYBodyRot(yaw);
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) wrapped -= 360.0F;
        if (wrapped < -180.0F) wrapped += 360.0F;
        return wrapped;
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }
    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(Entity entity) { }
}

package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.BattleEngine;
import io.github.q93503128.turnbound.combat.BattleEvent;
import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.combat.EffectType;
import io.github.q93503128.turnbound.combat.P0Scenario;
import io.github.q93503128.turnbound.combat.SkillDefinition;
import io.github.q93503128.turnbound.combat.TargetRule;
import io.github.q93503128.turnbound.world.FieldSessionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class BattleSession {
    private final BattleEngine engine;
    private final String encounterId;
    private final Vec3 returnPosition;
    private final float returnYaw;
    private final float returnPitch;
    private final boolean playerWasInvisible;
    private final Vec3 battleAnchor;
    private final float battleYaw;
    private final BattlePresentation presentation = new BattlePresentation();
    private boolean auto;
    private int speed = 1;
    private int delayTicks = 8;
    private boolean finished;
    private boolean readyShown;

    BattleSession(ServerPlayer player) { this(player, ""); }

    BattleSession(ServerPlayer player, String encounterId) {
        this.encounterId = encounterId == null ? "" : encounterId;
        BattleState initial = FieldSessionManager.ENCOUNTER_A01_PATROL.equals(this.encounterId)
                ? P0Scenario.createFieldPatrol()
                : P0Scenario.create();
        engine = new BattleEngine(initial);
        returnPosition = player.position();
        returnYaw = player.getYRot();
        returnPitch = player.getXRot();
        playerWasInvisible = player.isInvisible();

        BattleArenaLocator.Arena arena = BattleArenaLocator.locate(player);
        battleAnchor = arena.center();
        battleYaw = arena.facingYaw();
        player.setInvisible(true);
        player.setPos(battleAnchor.x, battleAnchor.y, battleAnchor.z);
        player.setYRot(battleYaw);
        player.setXRot(0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        presentation.spawn((ServerLevel) player.level(), battleAnchor, battleYaw, engine.state().combatants());
    }

    public BattleState state() { return engine.state(); }
    public boolean auto() { return auto; }
    public int speed() { return speed; }
    public boolean finished() { return finished; }
    String encounterId() { return encounterId; }
    Vec3 battleAnchor() { return battleAnchor; }
    float battleYaw() { return battleYaw; }
    Vec3 combatantPosition(String id) { return presentation.home(id); }

    void tick(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        presentation.tick(level);
        lock(player);
        if (engine.state().outcome() != BattleOutcome.RUNNING) {
            finished = true;
            return;
        }
        if (delayTicks > 0) {
            delayTicks--;
            return;
        }
        CombatantState actor = engine.state().currentActorId() == null
                ? engine.nextReady()
                : engine.state().combatant(engine.state().currentActorId());
        if (!readyShown) {
            readyShown = true;
            delayTicks = presentationDelay();
            BattleNetwork.sync(player, this);
            return;
        }
        if (actor.side() == CombatantSide.ENEMY || auto) {
            presentation.clearFocus(level);
            autoAct(level, actor);
            readyShown = false;
            delayTicks = presentationDelay();
            BattleNetwork.sync(player, this);
        }
    }

    void action(ServerPlayer player, String actorId, String skillId, String targetId) {
        if (finished || auto || engine.state().outcome() != BattleOutcome.RUNNING) return;
        if (!actorId.equals(engine.state().currentActorId())) return;
        CombatantState actor = engine.state().combatant(actorId);
        if (actor.side() != CombatantSide.ALLY) return;
        ServerLevel level = (ServerLevel) player.level();
        try {
            SkillDefinition skill = actor.definition().skill(skillId);
            presentation.clearFocus(level);
            if (skill.targetRule() == TargetRule.SELF || skill.targetRule() == TargetRule.ALLY_ALL || skill.targetRule() == TargetRule.ENEMY_ALL) {
                engine.useSkill(actorId, skillId);
            } else {
                engine.useSkill(actorId, skillId, targetId);
            }
            animateDirectDamage(level, actor, skill, targetId);
            readyShown = false;
            delayTicks = presentationDelay();
            if (engine.state().outcome() != BattleOutcome.RUNNING) finished = true;
            BattleNetwork.sync(player, this);
        } catch (RuntimeException ignored) {
            BattleNetwork.sync(player, this);
        }
    }

    void focusTarget(ServerPlayer player, String targetId) {
        ServerLevel level = (ServerLevel) player.level();
        if (targetId == null || targetId.isBlank()) {
            presentation.clearFocus(level);
            return;
        }
        try {
            engine.state().combatant(targetId);
            presentation.focus(level, targetId);
        } catch (RuntimeException ignored) {
            presentation.clearFocus(level);
        }
    }

    void toggleAuto(ServerPlayer player) {
        if (finished) return;
        presentation.clearFocus((ServerLevel) player.level());
        auto = !auto;
        BattleNetwork.sync(player, this);
    }

    void toggleSpeed(ServerPlayer player) {
        if (finished) return;
        speed = speed == 1 ? 2 : 1;
        BattleNetwork.sync(player, this);
    }

    void cleanup(ServerPlayer player) {
        presentation.cleanup((ServerLevel) player.level());
        player.setInvisible(playerWasInvisible);
        player.setPos(returnPosition.x, returnPosition.y, returnPosition.z);
        player.setYRot(returnYaw);
        player.setXRot(returnPitch);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private void autoAct(ServerLevel level, CombatantState actor) {
        int eventStart = engine.state().events().size();
        try { P0Scenario.chooseAutoAction(engine, engine.state(), actor); }
        catch (RuntimeException ex) { safeBasicFallback(actor); }
        animateRecordedAction(level, actor, eventStart);
        if (engine.state().outcome() != BattleOutcome.RUNNING) finished = true;
    }

    private void animateRecordedAction(ServerLevel level, CombatantState actor, int eventStart) {
        List<BattleEvent> events = engine.state().events();
        for (int index = events.size() - 1; index >= eventStart; index--) {
            BattleEvent event = events.get(index);
            if (!"ACTION".equals(event.type()) || !actor.instanceId().equals(event.sourceId())) continue;
            SkillDefinition skill = actor.definition().skill(event.detail());
            String targetId = event.targetId();
            if (targetId != null && !targetId.isBlank()) {
                int comma = targetId.indexOf(',');
                if (comma >= 0) targetId = targetId.substring(0, comma);
            }
            animateDirectDamage(level, actor, skill, targetId);
            return;
        }
    }

    private void animateDirectDamage(ServerLevel level, CombatantState actor, SkillDefinition skill, String targetId) {
        boolean damages = skill.effects().stream().anyMatch(effect -> effect.type() == EffectType.DAMAGE);
        if (damages && targetId != null && !targetId.isBlank()) presentation.lunge(level, actor.instanceId(), targetId);
    }

    private void safeBasicFallback(CombatantState actor) {
        SkillDefinition basic = actor.definition().skill(actor.definition().basicSkillId());
        switch (basic.targetRule()) {
            case SELF, ALLY_ALL, ENEMY_ALL -> engine.useSkill(actor.instanceId(), basic.id());
            case ENEMY_SINGLE -> {
                List<CombatantState> targets = engine.state().living(actor.side().opposite());
                if (!targets.isEmpty()) engine.useSkill(actor.instanceId(), basic.id(), targets.getFirst().instanceId());
            }
            case ALLY_SINGLE -> {
                CombatantState target = engine.state().living(actor.side()).stream()
                        .min(Comparator.comparingDouble(unit -> unit.hp() / (double) unit.maxHp())).orElse(actor);
                engine.useSkill(actor.instanceId(), basic.id(), target.instanceId());
            }
            case DEAD_ALLY_SINGLE -> {
                List<CombatantState> targets = engine.state().downed(actor.side());
                if (!targets.isEmpty()) engine.useSkill(actor.instanceId(), basic.id(), targets.getFirst().instanceId());
            }
        }
    }

    private void lock(ServerPlayer player) {
        if (player.position().distanceToSqr(battleAnchor) > 0.0025) player.setPos(battleAnchor.x, battleAnchor.y, battleAnchor.z);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private int presentationDelay() { return speed == 2 ? 4 : 8; }
}

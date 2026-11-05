package io.github.q93503128.turnbound.combat;

/** Stable AUTO entrypoint. Hero and enemy decision policies are intentionally separate. */
public final class BattleAutoController {
    private BattleAutoController() {}

    public static void chooseAutoAction(BattleEngine engine, BattleState state, CombatantState actor) {
        if (actor.definition().summon()) {
            engine.useSkill(actor.instanceId(), actor.definition().basicSkillId());
        } else if (actor.side() == CombatantSide.ENEMY) {
            EnemyAutoPolicy.chooseEnemy(engine, state, actor);
        } else {
            HeroAutoPolicy.chooseHero(engine, state, actor);
        }
    }
}

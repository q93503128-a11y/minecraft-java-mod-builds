package kr.moonseungjun.turnboundre.debug;

import kr.moonseungjun.turnboundre.battle.BattleDefinitionContext;
import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleParticipant;
import kr.moonseungjun.turnboundre.battle.BattleTeam;
import kr.moonseungjun.turnboundre.data.ActionDefinition;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pure setup contract for the representative 3D/VFX visual audit. */
final class DebugVisualShowcaseScenario {
    static final String ENDERMAN_ACTOR_ID = "showcase_enderman";
    static final String SKELETON_ACTOR_ID = "showcase_skeleton";
    static final List<String> TARGET_IDS = List.of("showcase_target_1", "showcase_target_2", "showcase_target_3");

    private static final String ENDERMAN_CHARACTER = "turnbound_re:enderman";
    private static final String SKELETON_CHARACTER = "turnbound_re:skeleton";
    private static final String TARGET_CHARACTER = "turnbound_re:iron_golem";
    private static final int ENDERMAN_SPEED = 1000;
    private static final int SKELETON_SPEED = 900;
    private static final int TARGET_SPEED = 10;
    private static final int TARGET_MIN_HP = 2400;

    record BurstPlan(String actorId, String actionId, List<String> targetIds) {
        BurstPlan {
            targetIds = List.copyOf(targetIds);
        }
    }

    record Scenario(
            List<BattleParticipant> participants,
            BattleDefinitionContext context,
            Map<String, UUID> controllers,
            List<BurstPlan> bursts
    ) {
        Scenario {
            participants = List.copyOf(participants);
            controllers = Map.copyOf(controllers);
            bursts = List.copyOf(bursts);
        }
    }

    private DebugVisualShowcaseScenario() {}

    static Scenario create(DefinitionRegistry registry, String definitionHash, UUID controller) {
        if (registry == null) throw new IllegalArgumentException("registry must not be null");
        if (controller == null) throw new IllegalArgumentException("controller must not be null");

        CharacterDefinition enderman = requireCharacter(registry, ENDERMAN_CHARACTER);
        CharacterDefinition skeleton = requireCharacter(registry, SKELETON_CHARACTER);
        CharacterDefinition target = requireCharacter(registry, TARGET_CHARACTER);

        List<BattleParticipant> participants = List.of(
                actorParticipant(ENDERMAN_ACTOR_ID, 0, enderman, ENDERMAN_SPEED),
                actorParticipant(SKELETON_ACTOR_ID, 1, skeleton, SKELETON_SPEED),
                targetParticipant(TARGET_IDS.get(0), 2, target),
                targetParticipant(TARGET_IDS.get(1), 3, target),
                targetParticipant(TARGET_IDS.get(2), 4, target));

        Map<String, String> characterIds = new LinkedHashMap<>();
        characterIds.put(ENDERMAN_ACTOR_ID, enderman.id());
        characterIds.put(SKELETON_ACTOR_ID, skeleton.id());
        for (String targetId : TARGET_IDS) characterIds.put(targetId, target.id());

        BattleDefinitionContext context = new BattleDefinitionContext(registry, definitionHash, characterIds);
        Map<String, UUID> controllers = Map.of(
                ENDERMAN_ACTOR_ID, controller,
                SKELETON_ACTOR_ID, controller);

        List<BurstPlan> bursts = List.of(
                burstPlan(registry, ENDERMAN_ACTOR_ID, enderman, TARGET_IDS),
                burstPlan(registry, SKELETON_ACTOR_ID, skeleton, TARGET_IDS));
        return new Scenario(participants, context, controllers, bursts);
    }

    static void prepareBattle(BattleInstance battle, Scenario scenario) {
        if (battle == null || scenario == null) throw new IllegalArgumentException("battle/scenario required");
        for (BurstPlan plan : scenario.bursts()) battle.combatState(plan.actorId()).gainEnergy(100);
    }

    private static BurstPlan burstPlan(
            DefinitionRegistry registry,
            String actorId,
            CharacterDefinition character,
            List<String> enemyIds
    ) {
        ActionDefinition burst = requireAction(registry, character.burst());
        if (!"BURST".equals(burst.kind())) {
            throw new IllegalStateException("showcase action must remain BURST: " + burst.id());
        }
        if (!"ENEMY".equals(burst.targeting().team())) {
            throw new IllegalStateException("showcase burst must target ENEMY: " + burst.id());
        }
        int count = burst.targeting().count();
        if (count < 1 || count > enemyIds.size()) {
            throw new IllegalStateException("showcase cannot satisfy target count " + count + " for " + burst.id());
        }
        return new BurstPlan(actorId, burst.id(), enemyIds.subList(0, count));
    }

    private static BattleParticipant actorParticipant(
            String id,
            int ordinal,
            CharacterDefinition definition,
            int speed
    ) {
        CharacterDefinition.Stats stats = definition.baseStats();
        return new BattleParticipant(
                id, BattleTeam.PLAYER, ordinal, speed,
                stats.hp(), stats.atk(), stats.def(), stats.poise());
    }

    private static BattleParticipant targetParticipant(String id, int ordinal, CharacterDefinition definition) {
        CharacterDefinition.Stats stats = definition.baseStats();
        int hp = Math.max(TARGET_MIN_HP, stats.hp() * 6);
        return new BattleParticipant(
                id, BattleTeam.ENEMY, ordinal, TARGET_SPEED,
                hp, stats.atk(), stats.def(), stats.poise());
    }

    private static CharacterDefinition requireCharacter(DefinitionRegistry registry, String id) {
        CharacterDefinition character = registry.characters().get(id);
        if (character == null) throw new IllegalStateException("showcase missing CharacterDefinition: " + id);
        return character;
    }

    private static ActionDefinition requireAction(DefinitionRegistry registry, String id) {
        ActionDefinition action = registry.actions().get(id);
        if (action == null) throw new IllegalStateException("showcase missing ActionDefinition: " + id);
        return action;
    }
}

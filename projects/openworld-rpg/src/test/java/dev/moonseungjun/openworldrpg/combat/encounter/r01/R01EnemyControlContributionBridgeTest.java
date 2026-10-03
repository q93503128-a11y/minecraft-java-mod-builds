package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class R01EnemyControlContributionBridgeTest {
    @Test
    void exactEarthloongRegistryIsAControlSupportTarget() {
        assertEquals(
                R01EnemyControlContributionBridge.ActorKind.EARTHLOONG,
                R01EnemyControlContributionBridge.actorKind(
                        "threateningly_mobs:the_earthloong",
                        false
                )
        );
    }

    @Test
    void natureSpiritRequiresExactLegacyRegistryAndAuthoredSpawn() {
        assertEquals(
                R01EnemyControlContributionBridge.ActorKind.NATURE_SPIRIT,
                R01EnemyControlContributionBridge.actorKind(
                        "threateningly_mobs:nature_hamony",
                        true
                )
        );
        assertEquals(
                R01EnemyControlContributionBridge.ActorKind.NONE,
                R01EnemyControlContributionBridge.actorKind(
                        "threateningly_mobs:nature_hamony",
                        false
                )
        );
        assertEquals(
                R01EnemyControlContributionBridge.ActorKind.NONE,
                R01EnemyControlContributionBridge.actorKind(
                        "threateningly_mobs:nature_spirit",
                        true
                )
        );
    }

    @Test
    void regalhartRequiresExactRegistryAndAuthoredSpawn() {
        assertEquals(
                R01EnemyControlContributionBridge.ActorKind.REGALHART,
                R01EnemyControlContributionBridge.actorKind(
                        "threateningly_mobs:the_regalhart",
                        true
                )
        );
        assertEquals(
                R01EnemyControlContributionBridge.ActorKind.NONE,
                R01EnemyControlContributionBridge.actorKind(
                        "threateningly_mobs:the_regalhart",
                        false
                )
        );
    }

    @Test
    void ordinaryRewardEnemiesRequireAuthoredSpawnForControlParticipation() {
        assertEquals(
                R01EnemyControlContributionBridge.ActorKind.ORDINARY_ENEMY,
                R01EnemyControlContributionBridge.actorKind(
                        "alexsmobs:grizzly_bear",
                        true
                )
        );
        assertEquals(
                R01EnemyControlContributionBridge.ActorKind.ORDINARY_ENEMY,
                R01EnemyControlContributionBridge.actorKind(
                        "alexsmobs:bison",
                        true
                )
        );
        assertEquals(
                R01EnemyControlContributionBridge.ActorKind.ORDINARY_ENEMY,
                R01EnemyControlContributionBridge.actorKind(
                        "alexsmobs:centipede_head",
                        true
                )
        );
        assertEquals(
                R01EnemyControlContributionBridge.ActorKind.ORDINARY_ENEMY,
                R01EnemyControlContributionBridge.actorKind(
                        "threateningly_mobs:steelboar",
                        true
                )
        );
        assertEquals(
                R01EnemyControlContributionBridge.ActorKind.NONE,
                R01EnemyControlContributionBridge.actorKind(
                        "alexsmobs:grizzly_bear",
                        false
                )
        );
        assertEquals(
                R01EnemyControlContributionBridge.ActorKind.NONE,
                R01EnemyControlContributionBridge.actorKind(
                        "threateningly_mobs:louxia",
                        true
                )
        );
    }
}

#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]

def main() -> None:
    loot = read("VillageRaidLootSystem.java")
    raid = read("VillageRaidSystem.java")
    relic = read("VillageRelicSystem.java")
    enemy = read("VillageEnemyArchetypeSystem.java")
    merc = read("VillageMercenarySystem.java")

    # Automated defense kills still resolve one normal party reward recipient.
    drops = section(loot, "public static void handleDrops", "private static ServerPlayer rewardRecipient")
    assert "instanceof ServerPlayer killer" not in drops
    assert "rewardRecipient(mob" in drops
    recipient = section(loot, "private static ServerPlayer rewardRecipient", "private static ItemStack named")
    assert "VillageProgressionSystem.nightParticipants(server)" in recipient
    assert "player.distanceToSqr(mob)" in recipient

    # Boss relics are queued while combat is locked and opened only after victory resolution.
    offer = section(relic, "public static synchronized void offerToParty", "public static synchronized void openPendingChoicesForParty")
    assert "!VillageRaidSystem.isRaidLocked()" in offer
    victory = section(raid, "private static void finishVictory", "public static boolean shouldDiscardStaleRaidEnemy")
    assert "VillageUiService.openRepairSummaryForAll(server)" in victory
    assert "VillageRelicSystem.openPendingChoicesForParty(server)" in victory
    assert victory.index("VillageUiService.openRepairSummaryForAll(server)") < victory.index("VillageRelicSystem.openPendingChoicesForParty(server)")

    # Early XP stays near the halved playtest pace while later days accelerate more strongly.
    xp = section(raid, "public static int experienceForEnemy", "public static VillageEnemyArchetypeSystem.AerialRole")
    assert "0.59f" in xp and "lateScale" in xp and "day - 8" in xp
    assert "lateVictoryScale" in victory and "0.50f" in victory and "0.30f" in victory

    # Support-heavy wave identities remain, but sustain casters are sparse and weaker.
    selection = section(enemy, "private static Archetype select", "private static Archetype bossForDay")
    assert "if (slot == 5 || slot == 17) return Archetype.WAR_CHANTER" in selection
    assert "if (slot == 11) return Archetype.HEXER" in selection
    assert selection.count("if (slot == 12) return Archetype.HEXER") >= 2
    assert "if (slot == 13) return Archetype.HEXER" in selection
    abilities = section(enemy, "public static void tickAbility", "public static void onStructureHit")
    assert "trait == VillageWaveTrait.HEXED ? 180 : 220" in abilities
    assert "MobEffects.WEAKNESS, 60" in abilities
    assert "MobEffects.SLOWNESS, 45" in abilities
    assert "abilityReady(mob, globalTicks, 240)" in abilities
    assert "activeEnemiesNear(level, mob.position(), 10.0, 5" in abilities
    assert "ally.heal(3.0f + VillageCouncilState.currentDay() * 0.08f)" in abilities
    assert "abilityReady(mob, globalTicks, 150)" in abilities
    assert "ally.heal(4.0f)" in abilities

    # Stuck actors can be recovered even while a larger wave is still alive, with a bounded per-pass cap.
    recovery = section(raid, "private static void recoverFrozenFinalEnemies", "private static boolean shouldRecoverStalledEnemy")
    assert "ACTIVE_ENEMIES.size() > 2" not in recovery
    assert "waveElapsedTicks % 20 != 0" in recovery
    assert "MAX_STALL_RECOVERIES_PER_PASS" in recovery
    assert "mob.teleportTo(level" in recovery

    # Persistent mercenaries have a larger survivability budget and receive one bounded dawn recovery.
    assert "case BASTION -> 340.0" in merc
    assert "case STRIKER -> 250.0" in merc
    assert "case RANGER -> 215.0" in merc
    assert "case MEDIC -> 270.0" in merc
    assert "public static synchronized void healAtDawn" in merc
    assert "mercenary.getMaxHealth() * 0.30f" in merc
    assert "mercenary.getMaxHealth() * 0.02f" not in merc
    assert "MobEffects.INVISIBILITY" in merc
    assert "float damage = 5.2f * mercenaryPower(rank)" in merc
    assert "float amount = 2.8f * mercenaryPower(rank)" in merc

    print("[PASS] automated defenses preserve normal raid loot")
    print("[PASS] boss relic choices wait until the raid is over")
    print("[PASS] early XP stays reduced while late-day XP ramps upward")
    print("[PASS] sustain casters are sparse and healing is reduced")
    print("[PASS] stuck raid actors recover during full waves with a bounded relocation budget")
    print("[PASS] persistent mercenaries are tougher and recover once at dawn")

if __name__ == "__main__":
    main()

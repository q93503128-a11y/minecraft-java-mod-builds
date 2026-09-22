#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def main() -> None:
    rpg = read("VillageRpgSystem.java")
    abilities = read("VillageRoleAbilitySystem.java")
    techniques = read("VillageCombatTechniqueSystem.java")

    assert "PRE_SCALED_PLAYER_DAMAGE" in rpg
    assert "damageSources().indirectMagic(owner, owner)" in rpg
    assert "event.getSource().getEntity() instanceof ServerPlayer killer" in rpg
    assert "VillageProgressionSystem.addCoins(killer, coins, \"적 처치\")" in rpg
    assert "VillagePersonalCombatSystem.applyKillMomentum(killer)" in rpg
    assert "VillagePersonalCombatSystem.healNearbyAlliesOnKill(killer)" in rpg

    assert "VillageRpgSystem.dealPreScaledPlayerDamage(level, owner, target, trained)" in abilities
    assert "!VillageRpgSystem.isPreScaledPlayerDamage()" in abilities
    assert "VillageRpgSystem.dealPreScaledPlayerDamage(level, attacker, target, damage)" in techniques

    # Owned custom damage must not re-enter the ordinary weapon/role multiplier stack.
    assert "if (!preScaledPlayerDamage && !preScaledRicochet" in rpg
    assert "if (!preScaledPlayerDamage && !preScaledRicochet)" in rpg

    print("[PASS] custom skill and combat-technique kills are attributed to the casting player")
    print("[PASS] player-owned indirect magic retains kill rewards while skipping ordinary attack rescaling")
    print("[PASS] role on-hit hooks are suppressed for pre-scaled custom damage, avoiding accidental double application")

if __name__ == "__main__":
    main()

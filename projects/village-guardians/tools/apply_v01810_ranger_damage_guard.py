#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
ABILITY = JAVA / "VillageRoleAbilitySystem.java"
RPG = JAVA / "VillageRpgSystem.java"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, got {count}")
    return text.replace(old, new, 1)


def patch_ability() -> None:
    text = ABILITY.read_text(encoding="utf-8")
    text = replace_once(
        text,
        "    private static final List<RicochetHop> RICOCHET_HOPS = new ArrayList<>();\n",
        "    private static final List<RicochetHop> RICOCHET_HOPS = new ArrayList<>();\n"
        "    private static final Set<RicochetDamageKey> PRE_SCALED_RICOCHET_DAMAGE = new HashSet<>();\n",
        "pre-scaled damage field")
    text = replace_once(
        text,
        "        RICOCHET_HOPS.clear();\n        ARROW_RAIN_READY.clear();\n",
        "        RICOCHET_HOPS.clear();\n        PRE_SCALED_RICOCHET_DAMAGE.clear();\n        ARROW_RAIN_READY.clear();\n",
        "pre-scaled damage reset")

    old = '''    private static void hurtByPlayer(
            ServerLevel level, ServerPlayer owner, Mob target, float damage) {
        target.hurtServer(level, level.damageSources().playerAttack(owner), Math.max(0.1f, damage));
    }
'''
    new = '''    public static boolean isPreScaledRicochetDamage(ServerPlayer owner, Entity target) {
        return owner != null && target != null
                && PRE_SCALED_RICOCHET_DAMAGE.contains(
                        new RicochetDamageKey(owner.getUUID(), target.getUUID()));
    }

    private static void hurtByPlayer(
            ServerLevel level, ServerPlayer owner, Mob target, float damage) {
        RicochetDamageKey key = new RicochetDamageKey(owner.getUUID(), target.getUUID());
        PRE_SCALED_RICOCHET_DAMAGE.add(key);
        try {
            target.hurtServer(level, level.damageSources().playerAttack(owner), Math.max(0.1f, damage));
        } finally {
            PRE_SCALED_RICOCHET_DAMAGE.remove(key);
        }
    }
'''
    text = replace_once(text, old, new, "guarded player-attributed damage")
    text = replace_once(
        text,
        "    private record RicochetHop(long executeAt, UUID owner, UUID target, float damage, int hopIndex) {}\n",
        "    private record RicochetDamageKey(UUID owner, UUID target) {}\n\n"
        "    private record RicochetHop(long executeAt, UUID owner, UUID target, float damage, int hopIndex) {}\n",
        "damage key record")
    ABILITY.write_text(text, encoding="utf-8")


def patch_rpg() -> None:
    text = RPG.read_text(encoding="utf-8")
    old_head = '''    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && !(event.getEntity() instanceof ServerPlayer)) {
            boolean projectile = event.getSource().getDirectEntity() instanceof AbstractArrow;
'''
    new_head = '''    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        boolean preScaledRicochet = event.getSource().getEntity() instanceof ServerPlayer ricochetOwner
                && VillageRoleAbilitySystem.isPreScaledRicochetDamage(ricochetOwner, event.getEntity());
        if (!preScaledRicochet
                && event.getSource().getEntity() instanceof ServerPlayer attacker
                && !(event.getEntity() instanceof ServerPlayer)) {
            boolean projectile = event.getSource().getDirectEntity() instanceof AbstractArrow;
'''
    text = replace_once(text, old_head, new_head, "rpg pre-scaled guard")
    text = replace_once(
        text,
        "        VillagePersonalCombatSystem.handleIncomingDamage(event);\n"
        "        VillageCombatTechniqueSystem.handleIncomingDamage(event);\n",
        "        VillagePersonalCombatSystem.handleIncomingDamage(event);\n"
        "        if (!preScaledRicochet) VillageCombatTechniqueSystem.handleIncomingDamage(event);\n",
        "combat technique recursion guard")
    RPG.write_text(text, encoding="utf-8")


def main() -> None:
    patch_ability()
    patch_rpg()
    print("[PASS] secondary ricochet damage keeps player ownership without double scaling")
    print("[PASS] pre-scaled ricochet cannot recursively trigger normal combat-technique ricochets")


if __name__ == "__main__":
    main()

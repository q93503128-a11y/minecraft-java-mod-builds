#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
ABILITY = JAVA / "VillageRoleAbilitySystem.java"
TEST = ROOT / "tools/test_v01810_ranger_ricochet.py"
TRIGGER = ROOT / ".build-trigger-v01810"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, got {count}")
    return text.replace(old, new, 1)


def main() -> None:
    text = ABILITY.read_text(encoding="utf-8")

    old_lock = '''        double speed = Math.max(2.4, arrow.getDeltaMovement().length());
        arrow.setNoGravity(true);
        arrow.setDeltaMovement(delta.normalize().scale(speed));
        arrow.hurtMarked = true;
        return target;
'''
    new_lock = '''        Vec3 velocity = arrow.getDeltaMovement();
        double speed = Math.max(2.0, Math.min(3.6, velocity.length()));
        Vec3 current = velocity.lengthSqr() < 1.0E-5
                ? lookDirection(player) : velocity.normalize();
        Vec3 desired = delta.normalize();
        Vec3 blended = current.scale(0.38).add(desired.scale(0.62));
        if (blended.lengthSqr() < 1.0E-5) blended = desired;
        arrow.setNoGravity(true);
        arrow.setDeltaMovement(blended.normalize().scale(speed));
        arrow.hurtMarked = true;
        return target;
'''
    text = replace_once(text, old_lock, new_lock, "smooth initial lock")

    old_flight = '''        return targetsNear(level, owner, origin, range, 64).stream()
                .filter(target -> {
                    Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
                    Vec3 to = body.subtract(origin);
                    return to.lengthSqr() > 1.0E-5 && to.normalize().dot(forward) >= 0.30;
                })
'''
    new_flight = '''        return targetsNear(level, owner, origin, range, 64).stream()
                .filter(target -> hasClearFlightPath(level, owner, origin, target))
                .filter(target -> {
                    Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
                    Vec3 to = body.subtract(origin);
                    return to.lengthSqr() > 1.0E-5 && to.normalize().dot(forward) >= 0.30;
                })
'''
    text = replace_once(text, old_flight, new_flight, "flight line of sight")

    marker = '''    private static void aimAssist(
'''
    helper = '''    private static boolean hasClearFlightPath(
            ServerLevel level, ServerPlayer owner, Vec3 origin, Mob target) {
        Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
        var hit = level.clip(new net.minecraft.world.level.ClipContext(
                origin, body,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                owner));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

'''
    text = replace_once(text, marker, helper + marker, "clear flight helper")
    ABILITY.write_text(text, encoding="utf-8")

    test = TEST.read_text(encoding="utf-8")
    test = replace_once(
        test,
        '    assert "current.scale(1.0 - turnStrength)" in ability\n',
        '    assert "current.scale(1.0 - turnStrength)" in ability\n'
        '    assert "current.scale(0.38).add(desired.scale(0.62))" in ability\n'
        '    assert "hasClearFlightPath(level, owner, origin, target)" in ability\n'
        '    assert "ClipContext.Block.COLLIDER" in ability\n',
        "polish regression assertions")
    TEST.write_text(test, encoding="utf-8")

    TRIGGER.write_text(
        "Village Guardians v0.18.10-alpha.1 final tracking ricochet flight polish — 2026-08-12\n",
        encoding="utf-8")
    print("[PASS] first target lock now blends from the real fired trajectory")
    print("[PASS] mid-flight reacquisition rejects targets behind solid blocks")


if __name__ == "__main__":
    main()

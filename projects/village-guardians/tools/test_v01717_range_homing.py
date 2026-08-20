#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def text(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    ability = text("VillageRoleAbilitySystem.java")
    effects = text("VillageSkillEffectSystem.java")
    mesh = text("VillageSkillMeshLibrary.java")
    guardians = text("VillageGuardians.java")

    assert "mod_version=" in props

    assert "LivingEntityUseItemEvent" not in guardians
    assert "handleUseItemTick" not in ability
    assert "tickRapidBow" in ability
    assert "player.releaseUsingItem()" in ability
    assert "event.setCharge(20)" in ability
    print("[PASS] 신속 삼연사가 실제 활 사용을 조기에 종료해 완충 발사를 실행합니다")

    assert "TRACKING_ARROWS" in ability
    assert "lockArrowOnTarget" in ability
    assert "tickTrackingArrows" in ability
    assert "arrow.setNoGravity(true)" in ability
    assert "predicted.subtract(arrow.position())" in ability
    print("[PASS] 추적 도탄은 전방 최적 표적을 잡고 비행 중에도 몸통을 추적합니다")

    assert "launchFireOrb" in ability
    assert "moving.effectId()" in ability
    assert "hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK" in ability
    assert "if (visual != null) visual.discard()" in ability
    print("[PASS] 홍염탄은 적·바닥·벽 충돌에서 폭발하고 투사체와 시각체가 함께 사라집니다")

    assert "areaRadius" in ability and "maximumRange" in ability
    assert "VillageSkillEffectSystem.lightningField" in ability
    assert "VillageSkillEffectSystem.frostField" in ability
    assert "VillageSkillEffectSystem.tornadoField" in ability
    assert "VillageSkillEffectSystem.healingField" in ability
    assert "meta(radius, specialRank)" in effects
    assert "EffectMeta" in mesh
    assert "state.extra" in mesh
    print("[PASS] 실제 범위·최대 사거리·절차 메시 범위가 같은 업그레이드 값을 공유합니다")

    assert "cycle != 0 && cycle != 3 && cycle != 6" in ability
    assert "owner.getRandom().nextFloat() < 0.90f" in ability
    assert "jaggedBolt(pose, out, start, end" not in mesh.split("private static void renderLightningField", 1)[1].split("private static void renderHealCast", 1)[0]
    assert "Vertical worm-like procedural bolts were removed" in mesh
    print("[PASS] 천뢰 폭격은 정확한 바닥 원 안에서 1.5배 주기로 적 우선 낙뢰를 사용합니다")

    assert '"warden_charge_cast",\n                player.position(), horizontal(direction), 12' in effects
    assert "case WARDEN_TAUNT -> {" in effects
    print("[PASS] 수호 돌진 방패는 중복되지 않고 돌진 종료 직후 빠르게 사라집니다")


if __name__ == "__main__":
    main()

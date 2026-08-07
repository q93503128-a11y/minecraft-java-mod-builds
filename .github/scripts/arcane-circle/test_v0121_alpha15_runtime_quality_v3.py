from __future__ import annotations

from hashlib import sha256
from pathlib import Path
import subprocess
import sys

ROOT=Path.cwd()

def body(rel:str)->str:return (ROOT/rel).read_text(encoding="utf-8")
def digest(rel:str)->str:return sha256((ROOT/rel).read_bytes()).hexdigest()
def require(rel:str,*tokens:str)->None:
    text=body(rel)
    for token in tokens:
        if token not in text:raise SystemExit(f"{rel}: missing {token!r}")
def forbid(rel:str,*tokens:str)->None:
    text=body(rel)
    for token in tokens:
        if token in text:raise SystemExit(f"{rel}: forbidden {token!r}")

tracked=[
 "gradle.properties",
 "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java",
 "src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java",
 "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java",
 "src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneWorldMesh.java",
 "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java",
 "src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java",
 "src/main/java/kr/moonseungjun/arcanecircle/item/ArcaneTestKitItem.java",
 "src/main/resources/assets/arcanecircle/items/arcane_test_kit.json",
 "src/main/resources/assets/arcanecircle/models/item/arcane_test_kit.json",
 "src/main/resources/assets/arcanecircle/lang/ko_kr.json",
]

require("gradle.properties","mod_version=0.12.1-alpha.15")
require("src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java",
        'VERSION = "0.12.1-alpha.15"',"MageGearService::onIncomingDamage","MageGearService.tickMovement(player)")
require("src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java",
        "DamageTypes.FALL","event.setCanceled(true)","ignoreThreshold","grantStableDescent","SLOW_FALLING")
forbid("src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java",
       "isGlideBoots(","stabilizeDescent(","getDeltaMovement()")
require("src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java",
        "SpellCatalog.spells().values()","enableCreativeTestProfile(ServerPlayer player)","state.cooldowns.clear()")
require("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java",
        "SpellCatalog.spells().values().stream()","chooseCombatSpell(Mob caster, MageProfile profile)","roll<55")
require("src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneWorldMesh.java",
        "SATURATION_BOOST=1.28","ALPHA_BOOST=1.32")
require("src/main/java/kr/moonseungjun/arcanecircle/item/ArcaneTestKitItem.java",
        "hasInfiniteMaterials()","1_000_000_000L","ArcaneNetwork.sync(serverPlayer)")
require("src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java",
        "ARCANE_TEST_KIT","event.accept(ARCANE_TEST_KIT.get())")

before={rel:digest(rel) for rel in tracked}
script=Path("..")/".."/".github"/"scripts"/"arcane-circle"/"apply_v0121_alpha15_runtime_quality_v3.py"
subprocess.run([sys.executable,str(script)],cwd=ROOT,check=True)
after={rel:digest(rel) for rel in tracked}
changed=[rel for rel in tracked if before[rel]!=after[rel]]
if changed:raise SystemExit(f"alpha.15 v3 migration is not idempotent: {changed}")
print("Arcane Circle alpha.15 runtime-quality audit v3: PASS")

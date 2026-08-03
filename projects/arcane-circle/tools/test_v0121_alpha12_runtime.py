#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"

def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")

def source(rel: str) -> str:
    return (SRC / rel).read_text(encoding="utf-8")

def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)

props = read("gradle.properties")
core = source("ArcaneCircle.java")
network = source("network/ArcaneNetwork.java")
magic = source("magic/MagicPlayerData.java")
casting = source("magic/SpellCastingService.java")
kinetics = source("magic/SpellKineticsService.java")
growth = source("magic/CombatGrowthService.java")
client = source("client/ArcaneClient.java")
gear = source("client/ArcaneGearRenderer.java")
world_visual = source("client/WorldMagicTracker.java")
world_sender = source("magic/WorldMagicService.java")
encounters = source("world/ArcaneEncounterService.java")
mages = source("world/ArcaneMageService.java")
boots = source("magic/MageGearService.java")
expanded = source("magic/ExpandedSpellEffects.java")
high = source("magic/HighCircleSpellEffects.java")

require("mod_version=0.12.1-alpha.12" in props, "alpha.12 Gradle version missing")
require('VERSION = "0.12.1-alpha.12"' in core, "alpha.12 runtime version missing")
require("ninefold-arcana-12-1-alpha12" in network, "alpha.12 protocol missing")

require("Math.max(0.10, circleMana" in magic, "mana multiplier is not floored at 10%")
require("Math.max(0.10, circleCooldown" in magic, "cooldown multiplier is not floored at 10%")
require("rawCooldown < 2.0" in magic and "? 0 : Math.max(2" in magic, "cooldown zero threshold is not below 0.1 seconds")
require("raw < 2.0 ? 0" in casting, "cast-time zero threshold is not below 0.1 seconds")
require("instantGap" not in casting, "obsolete instant-gap shortcut remains")

require("mode == SpellArchetype.Mode.INSTANT || mode == SpellArchetype.Mode.PROJECTILE" in kinetics, "projectile impact is not resolved immediately")
require("MAX_PENDING_PER_PLAYER = 32" in kinetics, "bounded pending-cast queue missing")
require("removeFirst()" in kinetics, "pending queue does not evict stale entries")
require("clicked || (down[slot] && !SLOT_WAS_DOWN[slot])" in client,
        "short key taps are still discarded between client ticks")
require("SLOT_WAS_DOWN[primarySlot] || pressed[primarySlot]" in client,
        "same-tick press/release handling missing")

require("progression_version" in magic and "CURRENT_PROGRESSION_VERSION = 2" in magic,
        "progression migration version missing")
require("while (state.circle" not in magic, "one cast can still skip multiple circles")
require("MAX_MASTERY_PER_CAST = 30" in growth, "mastery gain is not capped at the audited value")
require("MAX_INSIGHT_PER_CAST = 8" in growth, "insight gain is not capped at the audited value")
require("new Impact(0, 0, 0, 0, 0, 0, 0" in growth, "empty impacts still grant progression")

require("avatar.yBodyRot" in gear, "robe motion still follows camera yaw instead of body facing")
require("tickMovement(ServerPlayer player)" in boots, "per-tick smooth descent path missing")
require("SLOW_FALLING" in boots, "feather-fall no longer uses smooth vanilla descent")

require("private static final Set<String> TRUE_BEAMS = Set.of(" in world_visual and "\"ray_of_frost\"" in world_visual, "true-beam allowlist missing")
require("buildProjectile(" in world_visual and "afterimages" in world_visual,
        "moving compact projectile visuals missing")
require("visual.startedAt" in world_visual and "buildRelease(visual, age)" in world_visual, "release animation age tracking missing")
require("charge(LivingEntity caster" in world_sender, "NPC charge-circle broadcasting missing")
require("release(LivingEntity caster" in world_sender, "NPC release broadcasting missing")

require("RETALIATION_TICKS = 1_200L" in mages, "sustained NPC retaliation window missing")
require("WorldMagicService.charge(caster" in mages, "NPCs do not draw charge circles")
require("RETALIATION_TARGET.remove(caster.getUUID())" not in mages,
        "NPC retaliation is still erased after one cast")

require("findLand(" in encounters and "isDryLandSite" in encounters,
        "dry-land structure resolver missing")
require("if(pos==null)return;Mob mob" in encounters,
        "elite spawns can still dereference a missing land site")
require("clearLegacyOutpost" in encounters, "legacy ocean outpost cleanup missing")
require("boolean added=entity.addTag(tag);if(added)entity.removeTag(tag);return !added" in encounters, "26.2-compatible encounter marker probe missing")

for text, label in ((casting, "SpellCastingService"), (expanded, "ExpandedSpellEffects"),
                    (high, "HighCircleSpellEffects")):
    require("sendParticles(" not in text, f"legacy server particle renderer remains in {label}")
require(not re.search(r"for\s*\([^\n]*\)\s*\{\s*\}", casting),
        "empty legacy loops remain in SpellCastingService")
require("private static void healingVisual" not in casting,
        "obsolete healingVisual helper remains")

for path in SRC.rglob("*.java"):
    raw = path.read_bytes()
    bad = sorted({b for b in raw if b < 9 or 13 < b < 32})
    require(not bad, f"control bytes in {path}: {bad}")

print("Arcane Circle alpha.12 runtime audit: PASS")

from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
def t(p): return (ROOT/p).read_text(encoding="utf-8")
def need(h,x,w):
    if x not in h: raise SystemExit(f"missing {w}: {x}")
def forbid(h,x,w):
    if x in h: raise SystemExit(f"forbidden {w}: {x}")

props=t("gradle.properties"); main=t("src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java")
index=t("src/main/resources/data/arcanecircle/spell_catalog/index.json")
low=t("src/main/java/kr/moonseungjun/arcanecircle/client/LowCircleVisualIdentity.java")
mid=t("src/main/java/kr/moonseungjun/arcanecircle/client/MidCircleVisualIdentity.java")
fifth=t("src/main/java/kr/moonseungjun/arcanecircle/client/FifthCircleVisualIdentity.java")
sixth=t("src/main/java/kr/moonseungjun/arcanecircle/client/SixthCircleVisualIdentity.java")
arch=t("src/main/java/kr/moonseungjun/arcanecircle/client/ArchmageVisualIdentity.java")
tracker=t("src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java")
profile=t("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellPresentationProfile.java")
kin=t("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellKineticsService.java")
fusion=t("src/main/java/kr/moonseungjun/arcanecircle/magic/FusionSpellEffects.java")
highfx=t("src/main/java/kr/moonseungjun/arcanecircle/magic/HighCircleSpellEffects.java")
casting=t("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java")
gear=t("src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneGearRenderer.java")
sil=t("src/main/java/kr/moonseungjun/arcanecircle/client/CastingSilhouetteRenderer.java")
ui=t("src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java")
doc=t("docs/PRESENTATION_OVERHAUL_PHASES.md")

for h in (props,main,index): need(h,"0.12.1-alpha.24","alpha24 version")
for x in ("missileRack","fireballReactor","lightningRail","chromaticCrown"): need(low,x,"phase1")
for x in ("fireWallInstallation","dimensionDoorCorridor","thunderCagePylons"): need(mid,x,"phase2A")
for x in ("cloudkillCollectors","forceWallAnchors","chainLightningRouter","teleportCircleAddress"): need(fifth,x,"phase2B")
sixids=['disintegrate', 'globe_of_invulnerability', 'mass_suggestion', 'move_earth', 'sunbeam', 'true_seeing', 'freezing_sphere', 'eyebite', 'flesh_to_stone', 'circle_of_death', 'solar_guard']
highids=['delayed_blast_fireball', 'etherealness', 'finger_of_death', 'fire_storm', 'forcecage', 'plane_shift', 'prismatic_spray', 'reverse_gravity', 'simulacrum', 'teleport', 'void_lance', 'winter_domain', 'antimagic_field', 'clone', 'control_weather', 'demiplane', 'dominate_monster', 'earthquake', 'feeblemind', 'incendiary_cloud', 'maze', 'sunburst', 'astral_prison', 'phoenix_requiem', 'meteor_swarm', 'power_word_kill', 'prismatic_wall', 'shapechange', 'time_stop', 'true_polymorph', 'weird', 'wish', 'gate', 'foresight', 'world_sunder']
for x in sixids:
    need(sixth,f'case "{x}"',"6C authored case")
    need(profile,f'put("{x}"',"6C profile")
for x in highids:
    need(arch,f'case "{x}"',"7-9C authored case")
    need(profile,f'put("{x}"',"7-9C profile")
need(sixth,"spell.circle() == 6","6C boundary")
need(arch,"spell.circle() >= 7 && spell.circle() <= 9","archmage boundary")

# Director order must be strictly authored before generic.
names=["LowCircleVisualIdentity","MidCircleVisualIdentity","FifthCircleVisualIdentity","SixthCircleVisualIdentity","ArchmageVisualIdentity"]
pos=[]; start=0
for n in names:
    i=tracker.index(f"if ({n}.owns(spell))",start);pos.append(i);start=i+1
generic=tracker.index("switch (profile.sigil())",pos[-1])
if not all(pos[i]<pos[i+1] for i in range(len(pos)-1)) or pos[-1]>=generic: raise SystemExit("bad charge director order")
pos2=[]; start=generic
for n in names:
    i=tracker.index(f"if ({n}.owns(spell))",start);pos2.append(i);start=i+1
generic2=tracker.index("switch (profile.motion())",pos2[-1])
if not all(pos2[i]<pos2[i+1] for i in range(len(pos2)-1)) or pos2[-1]>=generic2: raise SystemExit("bad release director order")

# Server/presentation range parity and range-cap regressions.
need(sixth,"Math.max(20,range)","true seeing range visualization")
need(sixth,"Math.max(7,range*.26)","mass suggestion footprint")
need(sixth,"Math.max(8,range*.55)","move earth footprint")
need(sixth,"7*Math.max(1,Math.sqrt(range/25.0))","freezing sphere footprint")
need(arch,"Math.max(10,range*.32)","reverse gravity footprint")
need(arch,"Math.max(9,range*.50)","antimagic footprint")
need(arch,"Math.max(18,range*.55)","weather footprint")
need(arch,"Math.max(10,range*.30)","incendiary footprint")
need(arch,"Math.max(8,range*.65)","earthquake footprint")
need(arch,"14*Math.max(1,Math.sqrt(range/25.0))","sunburst footprint")
need(arch,"Math.max(20,range*.55)","time stop footprint")
need(arch,"Math.max(14,range*.35)","weird footprint")
need(arch,"Math.max(12,range*.25)","prismatic wall footprint")
need(arch,"Math.max(6,range*.45)","winter domain footprint")
need(arch,"Math.max(8,range*.42)","phoenix footprint")
need(arch,"Math.max(12,range*.38)","world sunder footprint")
need(arch,"Math.max(8,range)","void lance range")
for bad in ("Math.min(42.0, Math.max(8.0, range))","Math.min(18.0, Math.max(6.0, range * 0.45))",
            "Math.min(20.0, Math.max(8.0, range * 0.42))","Math.min(28.0, Math.max(12.0, range * 0.38))",
            "Math.min(42.0, Math.max(5.0, range))"):
    forbid(fusion,bad,"high fusion range ceiling")
need(highfx,"double radius = Math.max(18.0, range * 0.55);","server weather footprint")
need(highfx,"double radius = Math.max(20.0, range * 0.55);","server time stop footprint")
need(highfx,"double half = Math.max(12.0, range * 0.25);","server prismatic wall footprint")
need(highfx,"Vec3 impact = center.add(Math.cos(angle) * 10.0","meteor four impact layout")
need(arch,"impact=c.add(g.point(a,10))","meteor visual impact layout")
need(arch,"impact.add(0,28,0)","meteor visual sky height")
need(arch,"11*e","meteor visible blast radius")
need(profile,'put("meteor_swarm", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 18.00, 6, 4, 0, 30, 2.60, 30);',"meteor delay profile")
need(profile,'put("power_word_kill", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 2.35',"compact PWK")

# Kinetic authority/held release.
need(kin,"WorldMagicService.release(player, cast);","release payload first")
need(kin,"presentationImpactDelay","impact synchronization")
need(kin,"clock(player) + presentationImpactDelay","authoritative impact queue")
need(casting,"required <= 0 ? 1.0 : 0.0","zero-time ready hold")
need(casting,"READY_HOLD_TIMEOUT_TICKS","ready hold timeout")
forbid(casting,"if (required <= 0) {\n            cast","zero-time auto fire")

# Phase4 and robe/UI regressions.
need(tracker,"CasterPoseSnapshot","caster pose state")
need(gear,"WorldMagicTracker.castingPose","gear pose integration")
need(gear,"robeStyle(ItemStack s)","robe style identity")
for x in ("CINDER_ROBE","GLACIER_ROBE","TEMPEST_ROBE","ARCHMAGE_ROBE","RIFT_ROBE"): need(gear,x,"robe family")
for x in ("SNAP=1","AIM=2","HEAVY=3","GROUND=4","WARD=5","PORTAL=6","RITUAL=7"): need(sil,x,"casting family")
for x in ("case 3 ->","case 4 ->","case 5 ->","case 6 ->","case 7 ->"): need(sil,x,"robe silhouette")
need(t("src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java"),"syncAtomicRobe","atomic robe")
need(ui,"academyCircleViewport()","responsive viewport");need(ui,"enableScissor(","scissor")
for x in ("Phase 2C — 6C (alpha.24)","Phase 3 — 7C-9C (alpha.24)","Phase 4 — caster motion and clothing presentation (alpha.24)","46/46"):
    need(doc,x,"completion docs")

print("Arcane Circle alpha.24 complete presentation audit: PASS")
print("6C authored:",len(sixids),"7C-9C authored:",len(highids),"remaining authored backlog: 0")
print("source_mutation=disabled")

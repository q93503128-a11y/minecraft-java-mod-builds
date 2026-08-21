#!/usr/bin/env python3
from pathlib import Path
import re
import sys
ROOT = Path(__file__).resolve().parents[1]
required = ["PROJECT.md","README.md","CHANGELOG.md","THIRD_PARTY_NOTICES.md","build.gradle","gradle.properties","settings.gradle","gradlew","gradle/wrapper/gradle-wrapper.jar","gradle/wrapper/gradle-wrapper.properties","src/main/templates/META-INF/neoforge.mods.toml","src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java","src/main/java/kr/moonseungjun/survivalascension/client/SurvivalAscensionClient.java","src/main/java/kr/moonseungjun/survivalascension/client/ClientSkillState.java","src/main/java/kr/moonseungjun/survivalascension/client/SkillHudOverlay.java","src/main/java/kr/moonseungjun/survivalascension/progress/SkillType.java","src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java","src/main/java/kr/moonseungjun/survivalascension/progress/SkillProgressData.java","src/main/java/kr/moonseungjun/survivalascension/progress/SkillProgressionService.java","src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java","src/main/java/kr/moonseungjun/survivalascension/network/SkillUpdatePayload.java","src/main/java/kr/moonseungjun/survivalascension/network/SkillSnapshotPayload.java","src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java","src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java","src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java","src/main/resources/assets/survivalascension/lang/ko_kr.json","src/main/resources/data/survivalascension/tags/block/valuable_ores.json"]
errors=[]
for rel in required:
    if not (ROOT/rel).exists(): errors.append(f"missing: {rel}")
props=(ROOT/"gradle.properties").read_text(encoding="utf-8")
for needle in ["minecraft_version=26.2","neo_version=26.2.0.38-beta","mod_id=survivalascension","mod_version=0.2.0-alpha.1"]:
    if needle not in props: errors.append(f"gradle.properties missing {needle}")
build=(ROOT/"build.gradle").read_text(encoding="utf-8")
if "JavaLanguageVersion.of(25)" not in build or "options.release = 25" not in build: errors.append("Java 25 toolchain/release contract missing")
progress=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/progress/SkillProgressData.java").read_text(encoding="utf-8")
for needle in ['Codec.unboundedMap(Codec.STRING, Codec.LONG)','optionalFieldOf("mining_xp", 0L)','"mining_progress_v1"','SkillType skill']:
    if needle not in progress: errors.append(f"shared progression/migration contract missing: {needle}")
mining=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java").read_text(encoding="utf-8")
for needle in ["SkillTuning.miningAreaSize","player.isShiftKeyDown()","player.gameMode.destroyBlock(target)","PlayerEvent.BreakSpeed","ItemTags.PICKAXES"]:
    if needle not in mining: errors.append(f"mining contract missing: {needle}")
if "Attributes.BLOCK_BREAK_SPEED" in mining: errors.append("mining speed must be tool-scoped through BreakSpeed, not a permanent attribute")
wood=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java").read_text(encoding="utf-8")
for needle in ["BlockTags.LOGS","ItemTags.AXES","woodcuttingLogLimit","player.gameMode.destroyBlock(next)","player.isShiftKeyDown()"]:
    if needle not in wood: errors.append(f"woodcutting contract missing: {needle}")
network=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java").read_text(encoding="utf-8")
for needle in ["RegisterPayloadHandlersEvent","playToClient","PacketDistributor.sendToPlayer"]:
    if needle not in network: errors.append(f"network contract missing: {needle}")
hud=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/SkillHudOverlay.java").read_text(encoding="utf-8")
for needle in ["GuiGraphicsExtractor","ClientSkillState.lastUpdate","graphics.fill","graphics.text"]:
    if needle not in hud: errors.append(f"HUD contract missing: {needle}")
notices=(ROOT/"THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")
for needle in ["Skill Proficiencies","MIT License","Copyright (c) 2026 balovich-matje","Project MMO 2.0","reference-only"]:
    if needle not in notices: errors.append(f"third-party notice missing: {needle}")
for path in (ROOT/"src").rglob("*"):
    if not path.is_file() or path.suffix.lower() not in {".java",".json",".toml",".mcmeta",".txt"}: continue
    text=path.read_text(encoding="utf-8",errors="ignore")
    for forbidden in ["harmonised.pmmo","Caltinor","pmmo:"]:
        if forbidden.lower() in text.lower(): errors.append(f"restricted Project MMO implementation marker in {path.relative_to(ROOT)}: {forbidden}")
if re.search(r"setBlock\s*\([^\n]*AIR",mining) or re.search(r"setBlock\s*\([^\n]*AIR",wood): errors.append("scaled destruction must not bypass normal player destruction with setBlock(AIR)")
if errors:
    print("SOURCE AUDIT FAILED")
    for error in errors: print("-",error)
    sys.exit(1)
print("SOURCE AUDIT PASS")
print("- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25")
print("- generic 0-100 per-skill XP map with alpha.1 migration")
print("- mining: tool-scoped speed + 3x3/5x5/7x7")
print("- woodcutting: tool-scoped speed + 16/48/128/256 connected-log scaling")
print("- client snapshot/update payloads + recent-skill XP HUD")
print("- Skill Proficiencies MIT notice retained; Project MMO remains reference-only")

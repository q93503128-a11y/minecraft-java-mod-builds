#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import base64
import re
import zlib

PART_COUNT = 6
B85_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%&()*+-;<=>?@^_`{|}~"
parts_dir = Path(__file__).with_name("v08_payload")
parts = [(parts_dir / f"part{i}.txt").read_text(encoding="utf-8").strip() for i in range(PART_COUNT)]
encoded = "".join(parts)


def unpack(candidate: str) -> str:
    compressed = base64.b85decode(candidate.encode("ascii"))
    return zlib.decompress(compressed).decode("utf-8")


def recover_single_character(candidate: str, failure: Exception) -> tuple[str, str]:
    """Recover one damaged character only when the complete zlib stream validates."""
    match = re.search(r"byte (\d+)", str(failure))
    center = int(match.group(1)) if match else max(0, len(candidate) // 2)
    left = max(0, center - 8)
    right = min(len(candidate), center + 13)

    attempts: list[tuple[str, str]] = []
    for pos in range(left, right):
        attempts.append((f"delete@{pos}:{candidate[pos]!r}", candidate[:pos] + candidate[pos + 1:]))
        for char in B85_ALPHABET:
            if char != candidate[pos]:
                attempts.append((f"replace@{pos}:{candidate[pos]!r}->{char!r}", candidate[:pos] + char + candidate[pos + 1:]))
    for pos in range(left, right + 1):
        for char in B85_ALPHABET:
            attempts.append((f"insert@{pos}:{char!r}", candidate[:pos] + char + candidate[pos:]))

    for description, repaired in attempts:
        try:
            source = unpack(repaired)
            compile(source, __file__ + "::<recovered-payload>", "exec")
            return source, description
        except (ValueError, zlib.error, UnicodeDecodeError, SyntaxError):
            continue

    boundaries = []
    running = 0
    for index, part in enumerate(parts):
        boundaries.append(f"part{index}=[{running},{running + len(part)}) len={len(part)}")
        running += len(part)
    context = candidate[max(0, center - 15):min(len(candidate), center + 20)]
    raise RuntimeError(
        "Arcane v0.8 payload could not be checksum-recovered. "
        f"original={failure!r}; failure_context={context!r}; " + "; ".join(boundaries)
    ) from failure


try:
    source = unpack(encoded)
    repair = "none"
except (ValueError, zlib.error, UnicodeDecodeError) as failure:
    source, repair = recover_single_character(encoded, failure)

source = source.replace("ninefold-arcana-6", "ninefold-arcana-7")
source = source.replace(
    '        g.text(font, Component.literal("지팡이 " + ArcaneClientState.text("staff", "맨손")), x, y + 24, 0xFFFFD98A);',
    '        g.text(font, Component.literal(compactName(ArcaneClientState.text("staff", "맨손"), 12)),\n'
    '                x, y + 1, 0xFFFFD489);'
)

print("Arcane v0.8 migration payload decoded:",
      ", ".join(f"part{i}={len(part)}" for i, part in enumerate(parts)),
      f"repair={repair}")
exec(compile(source, __file__ + "::<expanded>", "exec"), {"__name__": "__main__", "__file__": __file__})

project_root = Path(__file__).resolve().parents[1]
java_root = project_root / "src/main/java/kr/moonseungjun/arcanecircle"

main_java = java_root / "ArcaneCircle.java"
main_source = main_java.read_text(encoding="utf-8")
if "import kr.moonseungjun.arcanecircle.world.ArcaneEconomyService;" not in main_source:
    main_source = main_source.replace(
        "import kr.moonseungjun.arcanecircle.world.MagicWorldService;",
        "import kr.moonseungjun.arcanecircle.world.ArcaneEconomyService;\n"
        "import kr.moonseungjun.arcanecircle.world.MagicWorldService;"
    )
if "ArcaneEconomyService.balance(player);" not in main_source:
    main_source = main_source.replace(
        "        boolean firstAwakening = data.ensureProfile(player);",
        "        boolean firstAwakening = data.ensureProfile(player);\n"
        "        ArcaneEconomyService.balance(player);"
    )
main_java.write_text(main_source, encoding="utf-8")

sigil_java = java_root / "magic/SpellSigilService.java"
sigil_source = sigil_java.read_text(encoding="utf-8")
sigil_source = sigil_source.replace(
    "double ratio = spell.range() <= 0.0 ? 1.0 : Math.max(0.75, Math.min(3.2, range / spell.range()));",
    "double rangeRatio = spell.range() <= 0.0 ? 1.0 : Math.max(0.75, Math.min(3.2, range / spell.range()));"
)
sigil_source = sigil_source.replace("Math.sqrt(ratio) * familyScale", "Math.sqrt(rangeRatio) * familyScale")
sigil_java.write_text(sigil_source, encoding="utf-8")

renamed_calls = 0
for java_file in java_root.rglob("*.java"):
    text = java_file.read_text(encoding="utf-8")
    changed = text.replace("rewardCombat(", "awardCombat(")
    if changed != text:
        renamed_calls += text.count("rewardCombat(")
        java_file.write_text(changed, encoding="utf-8")
if renamed_calls < 2:
    raise RuntimeError(f"expected economy declaration and cast caller, renamed only {renamed_calls} occurrence(s)")

network_java = java_root / "network/ArcaneNetwork.java"
network_source = network_java.read_text(encoding="utf-8")
if "import kr.moonseungjun.arcanecircle.world.ArcaneEconomyService;" not in network_source:
    network_source = network_source.replace(
        "import kr.moonseungjun.arcanecircle.magic.SpellCatalog;",
        "import kr.moonseungjun.arcanecircle.magic.SpellCatalog;\n"
        "import kr.moonseungjun.arcanecircle.world.ArcaneEconomyService;"
    )
if '";marks="' not in network_source and '"marks="' not in network_source:
    anchor = '                + ";mana=" + (int) state.mana()'
    replacement = anchor + '\n                + ";" + "marks=" + ArcaneEconomyService.balance(player)'
    if anchor not in network_source:
        raise RuntimeError("could not locate Arcane network mana snapshot anchor")
    network_source = network_source.replace(anchor, replacement, 1)
network_source = network_source.replace('";marks="', '";" + "marks="')
network_source = network_source.replace('";tradition="', '";" + "tradition="')
network_java.write_text(network_source, encoding="utf-8")

# A magic-world build has one economy: persistent Arcana handled by the academy.
# The build generates only metadata and item presentation assets.
build_source = r'''plugins {
    id 'java-library'
    id 'net.neoforged.moddev' version '2.0.143'
    id 'idea'
}

version = mod_version
group = mod_group_id

base { archivesName = mod_id }
java.toolchain.languageVersion = JavaLanguageVersion.of(25)

sourceSets.main.resources { srcDir 'src/generated/resources' }

neoForge {
    version = project.neo_version
    runs {
        client { client() }
        server { server(); programArgument '--nogui' }
        data {
            clientData()
            programArguments.addAll '--mod', project.mod_id, '--all',
                    '--output', file('src/generated/resources/').absolutePath,
                    '--existing', file('src/main/resources/').absolutePath
        }
        configureEach { logLevel = org.slf4j.event.Level.INFO }
    }
    mods { "${mod_id}" { sourceSet sourceSets.main } }
}

var generateModMetadata = tasks.register('generateModMetadata', ProcessResources) {
    var replaceProperties = [
            minecraft_version: minecraft_version,
            minecraft_version_range: minecraft_version_range,
            neo_version: neo_version,
            mod_id: mod_id,
            mod_name: mod_name,
            mod_license: mod_license,
            mod_version: mod_version
    ]
    inputs.properties replaceProperties
    expand replaceProperties
    from 'src/main/templates'
    into 'build/generated/sources/modMetadata'
}

var generatedStaffResources = layout.buildDirectory.dir('generated/resources/staffTextures')
var generateStaffTextures = tasks.register('generateStaffTextures') {
    var catalog = file('src/main/staff-textures.json')
    inputs.file catalog
    outputs.dir generatedStaffResources
    doLast {
        var root = generatedStaffResources.get().asFile
        delete root
        var target = new File(root, 'assets/arcanecircle/textures/item')
        target.mkdirs()
        var textures = new groovy.json.JsonSlurper().parse(catalog) as Map
        textures.each { id, encoded ->
            new File(target, "${id}.png").bytes = java.util.Base64.getDecoder().decode(encoded as String)
        }
    }
}

var generatedSpellbookResources = layout.buildDirectory.dir('generated/resources/spellbooks')
var generateSpellbookResources = tasks.register('generateSpellbookResources') {
    var catalog = file('src/main/spellbooks.json')
    inputs.file catalog
    outputs.dir generatedSpellbookResources
    doLast {
        var root = generatedSpellbookResources.get().asFile
        delete root
        var entries = new groovy.json.JsonSlurper().parse(catalog) as List
        def writeJson = { File target, Object value ->
            target.parentFile.mkdirs()
            target.setText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(value)) + '\n', 'UTF-8')
        }

        writeJson(new File(root, 'assets/arcanecircle/items/beginner_grimoire.json'), [
                model: [type: 'minecraft:model', model: 'minecraft:item/written_book']
        ])
        entries.each { entry ->
            String itemId = "spellbook_${entry.id}"
            writeJson(new File(root, "assets/arcanecircle/items/${itemId}.json"), [
                    model: [type: 'minecraft:model', model: 'minecraft:item/enchanted_book']
            ])
        }
    }
}

sourceSets.main.resources.srcDir generateModMetadata
sourceSets.main.resources.srcDir generatedStaffResources
sourceSets.main.resources.srcDir generatedSpellbookResources
tasks.named('processResources').configure {
    dependsOn generateStaffTextures
    dependsOn generateSpellbookResources
}
neoForge.ideSyncTask generateModMetadata
neoForge.ideSyncTask generateStaffTextures
neoForge.ideSyncTask generateSpellbookResources

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    options.release = 25
}

jar {
    manifest {
        attributes(
                'Specification-Title': mod_name,
                'Specification-Version': mod_version,
                'Implementation-Title': mod_name,
                'Implementation-Version': mod_version,
                'Automatic-Module-Name': mod_group_id
        )
    }
}

idea { module { downloadSources = true; downloadJavadoc = true } }
'''
build_file = project_root / "build.gradle"
build_file.write_text(build_source, encoding="utf-8")

print("Arcane v0.8 lifecycle, sigils, Arcana economy, and academy-only build normalized")

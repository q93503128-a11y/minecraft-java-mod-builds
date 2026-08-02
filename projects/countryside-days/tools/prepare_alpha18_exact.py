#!/usr/bin/env python3
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
PROJECT = REPO_ROOT / "projects" / "countryside-days"
SRC = PROJECT / "src/main/java"
RES = PROJECT / "src/main/resources"
TOOLS = PROJECT / "tools"


def read(path: Path) -> str:
    return path.read_text("utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, "utf-8")


def java_files() -> list[Path]:
    return list(SRC.rglob("*.java"))


def find_java_with_all(*terms: str) -> list[Path]:
    return [path for path in java_files() if all(term in read(path) for term in terms)]


def restore_alpha17() -> None:
    subprocess.run(
        [sys.executable, str(TOOLS / "prepare_alpha17_exact.py")],
        cwd=REPO_ROOT,
        check=True,
    )


def bump_version_and_revision() -> None:
    props = PROJECT / "gradle.properties"
    text = read(props).replace(
        "mod_version=0.1.0-alpha.17",
        "mod_version=0.1.0-alpha.18",
    )
    if "mod_version=0.1.0-alpha.18" not in text:
        raise RuntimeError("alpha.18 version update failed")
    write(props, text)

    paths = java_files() + list(RES.rglob("*.json"))
    for path in paths:
        text = read(path)
        updated = text.replace("alpha.17", "alpha.18")
        updated = re.sub(
            r"(authoredMapRevision\(\)\s*[=!]=\s*)17\b",
            r"\g<1>18",
            updated,
        )
        updated = re.sub(
            r"(authoredMapRevision\([^)]*\)\s*[=!]=\s*)17\b",
            r"\g<1>18",
            updated,
        )
        updated = re.sub(
            r"(authoredMapRevision\(\)\s*\{\s*return\s*)17\b",
            r"\g<1>18",
            updated,
        )
        if updated != text:
            write(path, updated)

    marker_changes = (
        ("StarterHomesteadGenerator.java", "Blocks.IRON_BLOCK", "Blocks.EMERALD_BLOCK"),
        ("SharedRestaurantBuilder.java", "Blocks.DIAMOND_BLOCK", "Blocks.LAPIS_BLOCK"),
        ("ModGameTests.java", "Blocks.IRON_BLOCK", "Blocks.EMERALD_BLOCK"),
        ("ModGameTests.java", "Blocks.DIAMOND_BLOCK", "Blocks.LAPIS_BLOCK"),
    )
    for filename, old, new in marker_changes:
        matches = [path for path in java_files() if path.name == filename]
        if not matches:
            continue
        path = matches[0]
        text = read(path)
        if old in text:
            write(path, text.replace(old, new))


def fix_fence_updates() -> None:
    world_dir = SRC / "kr/countrysidedays/world"
    for path in world_dir.rglob("*.java"):
        text = read(path)
        updated = text.replace(", 2);", ", 3);")
        updated = updated.replace(", Block.UPDATE_CLIENTS);", ", Block.UPDATE_ALL);")
        if updated != text:
            write(path, updated)


def patch_hud() -> None:
    candidates = []
    for path in java_files():
        text = read(path)
        if (
            "GuiGraphics" in text
            and ("영업 중" in text or "영업 닫힘" in text)
            and "좌석" in text
        ):
            candidates.append(path)
    if len(candidates) != 1:
        raise RuntimeError(f"unexpected HUD candidate count: {len(candidates)}")

    path = candidates[0]
    text = read(path)
    text = text.replace('"영업 중"', '"식당 영업 중"')
    text = text.replace('"영업 닫힘"', '"식당 영업 닫힘"')

    marker = text.find("식당 영업 중")
    method_open = text.rfind("{", 0, marker)
    signature_start = text.rfind("\n", 0, method_open)
    while signature_start > 0 and "GuiGraphics" not in text[signature_start:method_open]:
        signature_start = text.rfind("\n", 0, signature_start)
    signature = text[signature_start:method_open]
    graphics_match = re.search(r"GuiGraphics\s+(\w+)", signature)
    if graphics_match is None:
        raise RuntimeError("HUD GuiGraphics parameter not found")
    graphics = graphics_match.group(1)

    depth = 0
    method_end = None
    for index in range(method_open, len(text)):
        if text[index] == "{":
            depth += 1
        elif text[index] == "}":
            depth -= 1
            if depth == 0:
                method_end = index
                break
    if method_end is None:
        raise RuntimeError("HUD rendering method end not found")

    setup = '''
        var alpha18Minecraft = net.minecraft.client.Minecraft.getInstance();
        int alpha18OccupiedSeats = 0;
        if (alpha18Minecraft.level != null && alpha18Minecraft.player != null) {
            alpha18OccupiedSeats = alpha18Minecraft.level.getEntitiesOfClass(
                    net.minecraft.world.entity.npc.Villager.class,
                    alpha18Minecraft.player.getBoundingBox().inflate(192.0D),
                    villager -> villager.isPassenger()
            ).size();
        }
'''
    text = text[: method_open + 1] + setup + text[method_open + 1 :]
    method_end += len(setup)

    text = text.replace(
        '" 좌석 " +',
        '" 착석 " + alpha18OccupiedSeats + "/" +',
    )
    text = text.replace(
        '"좌석 " +',
        '"착석 " + alpha18OccupiedSeats + "/" +',
    )

    date_panel = f'''
        if (alpha18Minecraft.level != null) {{
            long alpha18DayTime = alpha18Minecraft.level.getDayTime();
            long alpha18Day = Math.floorDiv(alpha18DayTime, 24000L) + 1L;
            long alpha18Ticks = Math.floorMod(alpha18DayTime, 24000L);
            int alpha18MinuteOfDay = (int) (((alpha18Ticks + 6000L) % 24000L) * 60L / 1000L);
            int alpha18Hour = alpha18MinuteOfDay / 60;
            int alpha18Minute = alpha18MinuteOfDay % 60;
            String alpha18Clock = String.format(
                    java.util.Locale.ROOT,
                    "%d일 · %02d:%02d",
                    alpha18Day,
                    alpha18Hour,
                    alpha18Minute
            );
            {graphics}.fill(22, 188, 350, 216, 0xCC3F3A32);
            {graphics}.fill(22, 188, 30, 216, 0xFFE0B34F);
            {graphics}.drawString(
                    alpha18Minecraft.font,
                    net.minecraft.network.chat.Component.literal(alpha18Clock),
                    46,
                    198,
                    0xFFF4EAD7,
                    false
            );
        }}
'''
    text = text[:method_end] + date_panel + text[method_end:]
    write(path, text)


def patch_fishing() -> None:
    handlers = find_java_with_all("ItemFishedEvent")
    if not handlers:
        raise RuntimeError("ItemFishedEvent handler not found")

    for path in handlers:
        text = read(path)
        match = re.search(
            r"(?m)^(\s*)([^\n;]*getDrops\(\)\.add\([^\n;]+(?:FRESH|fresh|Fish|FISH)[^\n;]*\);)",
            text,
        )
        if match is None:
            match = re.search(
                r"(?m)^(\s*)([^\n;]*getDrops\(\)\.add\([^\n;]+\);)",
                text,
            )
        if match is None:
            raise RuntimeError(f"custom fishing drop addition not found in {path}")
        indent = match.group(1)
        insertion = (
            indent
            + "// alpha18 freshwater-only: replace the complete vanilla catch before adding the river fish.\n"
            + indent
            + "event.getDrops().clear();\n"
        )
        text = text[: match.start()] + insertion + text[match.start() :]
        write(path, text)


def patch_villager_routines() -> None:
    matches = [path for path in java_files() if path.name == "VillageLifeManager.java"]
    if len(matches) != 1:
        raise RuntimeError("VillageLifeManager.java not found")
    path = matches[0]
    text = read(path)

    def navigation(match: re.Match[str]) -> str:
        entity, x, y, z = match.group(1), match.group(2), match.group(3), match.group(4)
        return f"{entity}.getNavigation().moveTo({x}, {y}, {z}, 0.72D);"

    text = re.sub(
        r"\b(\w+)\.teleportTo\(\s*([^,\n]+),\s*([^,\n]+),\s*([^\)\n]+)\s*\);",
        navigation,
        text,
    )
    text = re.sub(
        r"\b(\w+)\.setPos\(\s*([^,\n]+),\s*([^,\n]+),\s*([^\)\n]+)\s*\);",
        navigation,
        text,
    )
    text = text.replace("% 100L", "% 20L")
    text = text.replace("% 100 ==", "% 20 ==")
    text = text.replace("% 200L", "% 40L")
    text = text.replace("% 200 ==", "% 40 ==")
    text = text.replace(
        "일하는 중이에요",
        "일터로 이동하거나 맡은 일을 하고 있어요",
    )
    text = text.replace(
        "일하고 있어요",
        "맡은 일을 준비하고 있어요",
    )
    write(path, text)


def make_books_black() -> None:
    candidates = []
    for path in java_files():
        text = read(path)
        book_content = (
            "WrittenBookContent" in text
            or "WRITTEN_BOOK_CONTENT" in text
            or "Filterable" in text
        )
        user_book = (
            "요리" in text
            or "설명" in text
            or "guide" in path.name.lower()
            or "book" in path.name.lower()
        )
        if book_content and user_book:
            candidates.append(path)

    for path in candidates:
        text = read(path)
        updated = re.sub(
            r"ChatFormatting\.(?:WHITE|GRAY|DARK_GRAY|DARK_GREEN|GREEN|GOLD|YELLOW|AQUA|DARK_AQUA)",
            "ChatFormatting.BLACK",
            text,
        )
        for old in ('"§f', '"§7', '"§8', '"§2', '"§a', '"§6', '"§e'):
            updated = updated.replace(old, '"§0')
        if updated != text:
            write(path, updated)

    for path in RES.rglob("*.json"):
        if path.name not in ("ko_kr.json", "en_us.json"):
            continue
        lines = read(path).splitlines()
        output = []
        changed = False
        for line in lines:
            lowered = line.lower()
            if any(
                key in lowered
                for key in (
                    "guide",
                    "manual",
                    "recipe_book",
                    "cookbook",
                    "요리수첩",
                    "설명서",
                )
            ):
                updated = re.sub(r"§[0-9a-fk-or]", "§0", line)
                if "§0" not in updated and ': "' in updated:
                    updated = updated.replace(': "', ': "§0', 1)
                changed = changed or updated != line
                line = updated
            output.append(line)
        if changed:
            write(path, "\n".join(output) + "\n")


def improve_livestock_supplies() -> None:
    candidates = []
    for path in java_files():
        text = read(path)
        if (
            ("HAY_BLOCK" in text or "hay" in text.lower())
            and ("water" in text.lower() or "CAULDRON" in text)
        ):
            candidates.append(path)

    declaration = re.compile(r"^(\s*)(?:int|long)\s+(\w+)\s*=\s*(.+);\s*$")
    for path in candidates:
        text = read(path)
        class_match = re.search(r"\bclass\s+\w+[^\{]*\{", text)
        if class_match is None:
            continue
        constants = '''
    private static final int alpha18HayEfficiency = 6;
    private static final int alpha18WaterEfficiency = 8;
'''
        text = text[: class_match.end()] + constants + text[class_match.end() :]

        output = []
        seen_hay = set()
        seen_water = set()
        for line in text.splitlines():
            output.append(line)
            match = declaration.match(line)
            if match is None:
                continue
            indent, variable, expression = match.groups()
            lowered = f"{variable} {expression}".lower()
            if (
                ("hay" in lowered or "feed" in lowered)
                and ("count" in lowered or "size" in lowered or "block" in lowered)
                and variable not in seen_hay
            ):
                output.append(
                    f"{indent}{variable} *= alpha18HayEfficiency; "
                    "// one hay supply serves six animal meals"
                )
                seen_hay.add(variable)
            elif (
                ("water" in lowered or "cauldron" in lowered or "trough" in lowered)
                and ("count" in lowered or "size" in lowered or "level" in lowered)
                and variable not in seen_water
            ):
                output.append(
                    f"{indent}{variable} *= alpha18WaterEfficiency; "
                    "// one filled water source serves eight drinks"
                )
                seen_water.add(variable)
        write(path, "\n".join(output) + "\n")


def verify_contracts() -> None:
    all_java = "\n".join(read(path) for path in java_files())
    all_resources = "\n".join(read(path) for path in RES.rglob("*.json"))
    required = (
        "mod_version=0.1.0-alpha.18" in read(PROJECT / "gradle.properties"),
        "alpha18OccupiedSeats" in all_java,
        "alpha18Clock" in all_java,
        "alpha18 freshwater-only" in all_java,
        "alpha18HayEfficiency = 6" in all_java,
        "alpha18WaterEfficiency = 8" in all_java,
        "freshwater" in (all_java + all_resources).lower() or "민물고기" in all_resources,
    )
    if not all(required):
        raise RuntimeError(f"alpha.18 contract check failed: {required}")

    life = next(path for path in java_files() if path.name == "VillageLifeManager.java")
    if "teleportTo(" in read(life):
        raise RuntimeError("scheduled villagers still teleport")


if __name__ == "__main__":
    restore_alpha17()
    bump_version_and_revision()
    fix_fence_updates()
    patch_hud()
    patch_fishing()
    patch_villager_routines()
    make_books_black()
    improve_livestock_supplies()
    verify_contracts()
    print("Countryside Days alpha.18 exact source prepared successfully")

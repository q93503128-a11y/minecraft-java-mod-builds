#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "src/main/java/kr/moonseungjun/villageguardians/VillageRoleAbilitySystem.java"
text = PATH.read_text(encoding="utf-8")

replacements = {
    "import net.minecraft.world.entity.projectile.Snowball;\n": "",
    "SoundEvents.CROSSBOW_LOADING_END, 1.0f, 1.35f":
        "SoundEvents.CROSSBOW_LOADING_END.value(), 1.0f, 1.35f",
    "SoundEvents.BREEZE_WIND_CHARGE_BURST, 1.1f, 0.72f":
        "SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.1f, 0.72f",
    "SoundEvents.BREEZE_WIND_CHARGE_BURST, 0.7f, 0.85f":
        "SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 0.7f, 0.85f",
    "if (spawningGeneratedArrow || arrow.getTags().contains(GENERATED_ARROW)) return;":
        "if (spawningGeneratedArrow) return;",
    "        Snowball projectile = new Snowball(level, player);\n"
    "        projectile.setItem(item);\n":
        "        var projectile = EntityTypes.SNOWBALL.create(level, EntitySpawnReason.EVENT);\n"
        "        if (projectile == null) return;\n"
        "        projectile.setOwner(player);\n"
        "        projectile.setItem(item);\n",
    "        arrow.addTag(GENERATED_ARROW);\n": "",
    "        arrow.setBaseDamage(Math.max(1.0, source.getBaseDamage() * 0.82));\n":
        "        arrow.setBaseDamage(2.0);\n",
    "Blocks.LIGHT_BLUE_STAINED_GLASS": "Blocks.GLASS",
    "SHIELDS.put(id, new ShieldBlocks(level.dimension().location().toString(), replaced));":
        "SHIELDS.put(id, new ShieldBlocks(level, replaced));",
    "        for (ServerLevel level : server.getAllLevels()) {\n"
    "            if (!level.dimension().location().toString().equals(shield.dimension())) continue;\n"
    "            for (Map.Entry<BlockPos, BlockState> entry : shield.replaced().entrySet()) {\n"
    "                if (level.getBlockState(entry.getKey()).is(Blocks.GLASS)) {\n"
    "                    level.setBlock(entry.getKey(), entry.getValue(), 3);\n"
    "                }\n"
    "            }\n"
    "            return;\n"
    "        }\n":
        "        ServerLevel level = shield.level();\n"
        "        for (Map.Entry<BlockPos, BlockState> entry : shield.replaced().entrySet()) {\n"
        "            if (level.getBlockState(entry.getKey()).is(Blocks.GLASS)) {\n"
        "                level.setBlock(entry.getKey(), entry.getValue(), 3);\n"
        "            }\n"
        "        }\n",
    "    private record ShieldBlocks(String dimension, Map<BlockPos, BlockState> replaced) {}\n":
        "    private record ShieldBlocks(ServerLevel level, Map<BlockPos, BlockState> replaced) {}\n",
}

for old, new in replacements.items():
    if old not in text:
        raise SystemExit(f"compile adaptation target not found: {old[:100]!r}")
    text = text.replace(old, new)

# Generated arrows must not recursively trigger the ranger triple-shot hook.
text = text.replace(
    "        level.addFreshEntity(arrow);\n    }\n\n    private static void spawnSideArrow",
    "        spawningGeneratedArrow = true;\n"
    "        try { level.addFreshEntity(arrow); }\n"
    "        finally { spawningGeneratedArrow = false; }\n"
    "    }\n\n    private static void spawnSideArrow",
    1,
)
text = text.replace(
    "        level.addFreshEntity(arrow);\n    }\n\n    private static void aimAssist",
    "        spawningGeneratedArrow = true;\n"
    "        try { level.addFreshEntity(arrow); }\n"
    "        finally { spawningGeneratedArrow = false; }\n"
    "    }\n\n    private static void aimAssist",
    1,
)

PATH.write_text(text, encoding="utf-8")
print("Applied NeoForge 26.2 compile adaptations for role abilities")

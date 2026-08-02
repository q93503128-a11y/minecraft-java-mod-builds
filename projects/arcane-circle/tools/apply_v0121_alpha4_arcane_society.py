#!/usr/bin/env python3
from pathlib import Path
import base64, gzip

TOOLS = Path(__file__).resolve().parent
ROOT = TOOLS.parent
parts = [TOOLS / "alpha4_installer.part-01", TOOLS / "alpha4_installer.part-02"]
payload = "".join("".join(path.read_text(encoding="utf-8").split()) for path in parts)
source = gzip.decompress(base64.b64decode(payload)).decode("utf-8")
old_helper = '''    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"replacement anchor mismatch in {path}: expected 1, got {count}: {old[:100]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
'''
new_helper = '''    count = text.count(old)
    if old == OLD_VERSION:
        if count < 1:
            raise RuntimeError(f"version anchor missing in {path}: {old}")
        path.write_text(text.replace(old, new), encoding="utf-8")
        return
    if count != 1:
        raise RuntimeError(f"replacement anchor mismatch in {path}: expected 1, got {count}: {old[:100]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
'''
if old_helper not in source:
    raise RuntimeError("installer helper patch anchor missing")
source = source.replace(old_helper, new_helper, 1)
exec(compile(source, __file__, "exec"))


def replace_once(relative: str, old: str, new: str) -> None:
    path = ROOT / relative
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"26.2 compatibility anchor mismatch in {relative}: {count}: {old[:100]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


mage = "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java"
replace_once(mage,
    "import net.minecraft.world.entity.npc.Villager;",
    "import net.minecraft.world.entity.npc.villager.Villager;")
replace_once(mage,
    "    private static final Map<UUID, Long> LAST_CAST = new HashMap<>();",
    "    private static final Map<UUID, Long> LAST_CAST = new HashMap<>();\n"
    "    private static final Map<UUID, Integer> CIRCLES = new HashMap<>();\n"
    "    private static final String NAME_PREFIX = \"[마도사:\";")
replace_once(mage,
'''    public static boolean isMage(Entity entity) {
        return entity.getTags().contains(MAGE_TAG);
    }

    public static int circle(Entity entity) {
        for (String tag : entity.getTags()) {
            if (!tag.startsWith(CIRCLE_PREFIX)) continue;
            try { return Math.max(1, Math.min(9, Integer.parseInt(tag.substring(CIRCLE_PREFIX.length())))); }
            catch (NumberFormatException ignored) {}
        }
        return 1;
    }
''',
'''    public static boolean isMage(Entity entity) {
        return CIRCLES.containsKey(entity.getUUID()) || namedCircle(entity) > 0;
    }

    public static int circle(Entity entity) {
        Integer cached = CIRCLES.get(entity.getUUID());
        if (cached != null) return cached;
        int parsed = namedCircle(entity);
        if (parsed > 0) {
            CIRCLES.put(entity.getUUID(), parsed);
            return parsed;
        }
        return 1;
    }

    private static int namedCircle(Entity entity) {
        Component customName = entity.getCustomName();
        if (customName == null) return 0;
        String name = customName.getString();
        int start = name.indexOf(NAME_PREFIX);
        if (start < 0) return 0;
        int end = name.indexOf(']', start + NAME_PREFIX.length());
        if (end < 0) return 0;
        try {
            return Math.max(1, Math.min(9,
                    Integer.parseInt(name.substring(start + NAME_PREFIX.length(), end))));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
''')
replace_once(mage,
'''    private static void mark(Entity entity, int circle) {
        entity.addTag(MAGE_TAG);
        entity.getTags().stream().filter(tag -> tag.startsWith(CIRCLE_PREFIX)).toList().forEach(entity::removeTag);
        entity.addTag(CIRCLE_PREFIX + Math.max(1, Math.min(9, circle)));
    }
''',
'''    private static void mark(Entity entity, int circle) {
        CIRCLES.put(entity.getUUID(), Math.max(1, Math.min(9, circle)));
    }
''')
replace_once(mage,
    '        villager.setCustomName(Component.literal("§d" + circle + "써클 마도사"));',
    '        villager.setCustomName(Component.literal("§d[마도사:" + circle + "] " + circle + "써클 마도사"));')
replace_once(mage,
    '        mob.setCustomName(Component.literal("§5" + circle + "써클 " + mob.getName().getString()));',
    '        mob.setCustomName(Component.literal("§5[마도사:" + circle + "] " + circle + "써클 " + mob.getName().getString()));')
replace_once(mage,
'''            Vec3 away = target.position().subtract(caster.position());
            target.knockback(0.35 + circle * 0.05, -away.x, -away.z);
''',
'''            Vec3 away = target.position().subtract(caster.position());
            Vec3 push = new Vec3(away.x, 0.0, away.z);
            if (push.lengthSqr() > 0.00001) {
                push = push.normalize().scale(0.35 + circle * 0.05);
                target.push(push.x, 0.12, push.z);
            }
''')
path = ROOT / mage
text = path.read_text(encoding="utf-8")
if "getDayTime()" not in text:
    raise RuntimeError("mage day-time compatibility anchor missing")
path.write_text(text.replace("getDayTime()", "getGameTime()"), encoding="utf-8")

quest = ROOT / "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneQuestData.java"
text = quest.read_text(encoding="utf-8")
if text.count("getDayTime()") != 1:
    raise RuntimeError("quest day-time compatibility anchor mismatch")
quest.write_text(text.replace("getDayTime()", "getGameTime()"), encoding="utf-8")

fusion = "src/main/java/kr/moonseungjun/arcanecircle/magic/FusionSpellEffects.java"
replace_once(fusion,
    "            mob.knockback(0.35, -delta.x, -delta.z);",
    "            Vec3 push = horizontal(delta).scale(0.35);\n"
    "            mob.push(push.x, 0.12, push.z);")
replace_once(fusion,
    "        sound(level, player, SoundEvents.GENERIC_EXPLODE, 1.0F, 0.52F);",
    "        sound(level, player, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.52F);")

replace_once("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java",
    "        ArcaneQuestData.get(level.getServer()).recordCast(player, impact, spell.circle());",
    "        ArcaneQuestData.get(((ServerLevel) player.level()).getServer()).recordCast(player, impact, spell.circle());")

contract = TOOLS / "test_magic_contract.py"
text = contract.read_text(encoding="utf-8").replace(
    "apply_v0121_alpha3_freeze_hotfix.py", "apply_v0121_alpha4_arcane_society.py")
contract.write_text(text, encoding="utf-8")
for path in parts:
    if path.exists():
        path.unlink()

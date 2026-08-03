#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def patch(path: str, old: str, new: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    if old in text:
        target.write_text(text.replace(old, new), encoding="utf-8")


def rewrite(path: str, transform) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    target.write_text(transform(text), encoding="utf-8")


patch("src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
      ".replace('|', ' · ')", '.replace("|", " · ")')
patch("src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java",
      "MobEffects.MOVEMENT_SPEED", "MobEffects.SPEED")
patch("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEncounterData.java",
      "player.getGameProfile().name()", "player.getScoreboardName()")


def fix_rpg_scale(text: str) -> str:
    text = re.sub(
        r"if \(entity == null \|\| entity instanceof Player \|\| entity\.(?:getTags|getCommandTags)\(\)\.contains\(SCALED_TAG\)\) return;\s*"
        r"AttributeInstance health = entity\.getAttribute\(Attributes\.MAX_HEALTH\);\s*if \(health == null\) return;",
        "if (entity == null || entity instanceof Player) return;\n"
        "        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);\n"
        "        if (health == null || !entity.addTag(SCALED_TAG)) return;", text, count=1)
    text = text.replace("        entity.addTag(SCALED_TAG);\n", "", 1)
    text = re.sub(
        r"(String tag = [^;]+;)\s*if \(entity\.(?:getTags|getCommandTags)\(\)\.contains\(tag\)\) return;\s*"
        r"AttributeInstance health = entity\.getAttribute\(Attributes\.MAX_HEALTH\);\s*if \(health == null\) return;",
        r"\1\n        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);\n"
        r"        if (health == null || !entity.addTag(tag)) return;", text, count=1)
    text = text.replace("        entity.addTag(tag);\n", "", 1)
    text = re.sub(
        r"if \(target instanceof Player \|\| !target\.(?:getTags|getCommandTags)\(\)\.contains\(SCALED_TAG\)\) return;",
        "if (target instanceof Player) return;", text, count=1)
    return text


rewrite("src/main/java/kr/moonseungjun/arcanecircle/magic/RpgScaleService.java", fix_rpg_scale)


def fix_mage(text: str) -> str:
    return re.sub(
        r"attack!=null&&!mage\.(?:getTags|getCommandTags)\(\)\.contains\(\"arcanecircle_mage_attack\"\)",
        'attack!=null&&mage.addTag("arcanecircle_mage_attack")', text, count=1)


rewrite("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java", fix_mage)


def fix_encounters(text: str) -> str:
    text = text.replace("level.getServer().overworld().getSharedSpawnPos()", "BlockPos.ZERO")
    text = text.replace("level.getSharedSpawnPos()", "BlockPos.ZERO")

    death = '''    public static void onDeath(LivingDeathEvent event){
        if(!(event.getEntity().level() instanceof ServerLevel level))return;
        ArcaneEncounterData data=ArcaneEncounterData.get(level.getServer());
        String name=event.getEntity().getName().getString();
        for(FactionProfile.Entry faction:FactionProfile.entries()){
            if(name.contains(faction.representativeName())){
                data.markNamedDead(faction.representativeId());
                broadcast(level,"§8[영구 사망] §f"+faction.representativeName()+"§7이(가) 이 세계에서 사라졌습니다.");
                return;
            }
        }
        for(Zone zone:ZONES){
            if(name.contains(zone.bossName)){
                data.markBossDefeated(zone.id);
                broadcast(level,"§6[마수 토벌] §f"+zone.bossName+"§7이(가) 쓰러졌습니다.");
                return;
            }
        }
    }

    public static String zoneSummary'''
    text, count = re.subn(
        r"    public static void onDeath\(LivingDeathEvent event\)\{.*?\n    \}\n\n    public static String zoneSummary",
        death, text, count=1, flags=re.S)
    if count == 0 and "String name=event.getEntity().getName().getString();" not in text:
        raise SystemExit("alpha.10 encounter death handler anchor not found")

    text = re.sub(
        r"e->e\.isAlive\(\)&&e\.(?:getTags|getCommandTags)\(\)\.contains\(tag\)",
        "e->e.isAlive()&&hasTag(e,tag)", text, count=1)

    helper = '''    private static boolean hasTag(Mob entity,String tag){
        boolean added=entity.addTag(tag);
        if(added)entity.removeTag(tag);
        return !added;
    }
    '''
    if "private static boolean hasTag(Mob entity,String tag)" not in text:
        text, helper_count = re.subn(
            r"(    private static void broadcast\(ServerLevel level,String \w+\)\{)",
            helper + r"\1", text, count=1)
        if helper_count == 0:
            raise SystemExit("alpha.10 encounter broadcast anchor not found")

    warn = '''    private static void warn(ServerPlayer player,Zone z,ArcaneEncounterData data){long now=((ServerLevel)player.level()).getGameTime();long last=WARNED.getOrDefault(player.getUUID(),Long.MIN_VALUE/4);if(now-last<240)return;WARNED.put(player.getUUID(),now);int circle=MagicPlayerData.get(((ServerLevel)player.level()).getServer()).state(player).circle();String danger=circle<z.tier?"§c권장보다 "+(z.tier-circle)+"써클 낮음":"§6교전 가능";ArcaneNoticeService.push(player,Component.literal("§4[마력 재해권] §f"+z.name+" §7· 권장 "+z.tier+"써클 · "+danger+(data.isBossDefeated(z.id)?" · 마수 토벌됨":" · 마수 생존")),45);}'''
    text = re.sub(r"    private static void warn\(ServerPlayer player,Zone z,ArcaneEncounterData data\)\{.*?\}\n", warn + "\n", text, count=1)
    return text


rewrite("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEncounterService.java", fix_encounters)

print("Arcane Circle alpha.10 NeoForge 26.2 compatibility fixes applied")

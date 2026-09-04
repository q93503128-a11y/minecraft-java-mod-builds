from pathlib import Path

root = Path('projects/survival-ascension')
freight = root / 'src/main/java/kr/moonseungjun/survivalascension/production/FreightService.java'
main = root / 'src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java'
props = root / 'gradle.properties'

text = freight.read_text(encoding='utf-8')
old = '''            player.sendSystemMessage(Component.literal("§6[전선 화물 적재] §f원정1 + 전초방어1 + 요새방어1회분을 선별해 §e" + moved
                    + "개§f 적재했습니다. §7식량176 · 철56 · 연료8 · 통나무32 · 석재벽돌128"));
'''
new = '''            player.sendSystemMessage(Component.literal("§6[전선 화물 적재] §f원정1 + 전초방어1 + 요새방어1회분을 선별해 §e" + moved
                    + "개§f 적재했습니다. §7식량" + FRONTLINE_FOOD + " · 철" + FRONTLINE_IRON
                    + " · 연료" + FRONTLINE_FUEL + " · 통나무" + FRONTLINE_LOGS
                    + " · 석재벽돌" + FRONTLINE_STONE_BRICKS));
'''
assert old in text, 'expected stale freight manifest message not found'
text = text.replace(old, new, 1)
freight.write_text(text, encoding='utf-8')

text = props.read_text(encoding='utf-8')
assert 'mod_version=0.61.4-alpha.1' in text
props.write_text(text.replace('mod_version=0.61.4-alpha.1', 'mod_version=0.61.5-alpha.1', 1), encoding='utf-8')

text = main.read_text(encoding='utf-8')
assert 'public static final String VERSION = "0.61.4-alpha.1";' in text
main.write_text(text.replace('public static final String VERSION = "0.61.4-alpha.1";',
                             'public static final String VERSION = "0.61.5-alpha.1";', 1), encoding='utf-8')

print('SURVIVAL_AUDIT6_FIX_APPLIED')

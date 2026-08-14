from pathlib import Path

path = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/LivingKingdoms.java')
text = path.read_text(encoding='utf-8')

if 'import kr.moonseungjun.livingkingdoms.item.FantasyItems;' not in text:
    anchor = 'import kr.moonseungjun.livingkingdoms.foundation.FoundationCatalog;\n'
    if anchor not in text:
        raise SystemExit('FantasyItems import anchor missing')
    text = text.replace(anchor, anchor + 'import kr.moonseungjun.livingkingdoms.item.FantasyItems;\n', 1)

if 'FantasyItems.register(modEventBus);' not in text:
    anchor = '        FantasyEntityTypes.register(modEventBus);\n'
    if anchor not in text:
        raise SystemExit('FantasyItems registry anchor missing')
    text = text.replace(anchor, anchor + '        FantasyItems.register(modEventBus);\n', 1)

for token in ['FantasyItems;', 'FantasyItems.register(modEventBus);']:
    if token not in text:
        raise SystemExit('missing fantasy item wiring token: ' + token)

path.write_text(text, encoding='utf-8')
print('Wired Living Kingdoms fantasy ecology items to the mod event bus.')

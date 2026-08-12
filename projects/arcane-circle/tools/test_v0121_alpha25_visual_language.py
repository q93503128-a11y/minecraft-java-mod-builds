from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

base=(ROOT/'tools/test_v0121_alpha24_presentation_complete.py').read_text(encoding='utf-8')
base=base.replace('0.12.1-alpha.24','0.12.1-alpha.25')
base=base.replace('Arcane Circle alpha.24 complete presentation audit',
                  'Arcane Circle alpha.25 inherited presentation audit')
scope={'__file__': str(ROOT/'tools/test_v0121_alpha24_presentation_complete.py')}
exec(compile(base,'alpha24-regression-as-alpha25','exec'),scope)

def t(path): return (ROOT/path).read_text(encoding='utf-8')
def need(haystack, token, label):
    if token not in haystack: raise SystemExit(f'missing {label}: {token}')

ui=t('src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java')
codex=t('src/main/java/kr/moonseungjun/arcanecircle/client/CodexVisualLanguage.java')
tracker=t('src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java')
grammar=t('src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneSigilDetailGrammar.java')
gear=t('src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneGearRenderer.java')
regalia=t('src/main/java/kr/moonseungjun/arcanecircle/client/RobeRegaliaRenderer.java')
doc=t('docs/PRESENTATION_OVERHAUL_PHASES.md')

for token in ('bookFrame','bookmark','card','panel','action','seal'):
    need(codex,token,'codex visual primitive')
for token in ('CodexVisualLanguage.bookFrame','CodexVisualLanguage.bookmark',
              'CodexVisualLanguage.card','CodexVisualLanguage.panel',
              'CodexVisualLanguage.action'):
    need(ui,token,'grimoire codex integration')
need(ui,'Math.min(720','responsive width')
need(ui,'enableScissor(','scroll clipping')

for token in ('notationKernel','FRONT_COMPACT','FRONT_LANCE','GROUND_SEAL','TARGET_SEAL',
              'BODY_HALO','SKY_RITUAL','QUAD_ARRAY','WALL_MATRIX','PORTAL_GATE',
              'runeRing','brokenBand','spell.circle() >= 6','spell.circle() >= 8'):
    need(grammar,token,'sigil detail grammar')
if tracker.count('ArcaneSigilDetailGrammar.appendCharge') < 6:
    raise SystemExit('not all charge paths receive detail grammar')
if tracker.count('ArcaneSigilDetailGrammar.appendRelease') < 6:
    raise SystemExit('not all release paths receive detail grammar')

need(gear,'RobeRegaliaRenderer.render','robe regalia wiring')
for token in ('case 1 -> apprentice','case 2 -> sage','case 3 -> cinder','case 4 -> glacier',
              'case 5 -> tempest','case 6 -> archmage','case 7 -> rift'):
    need(regalia,token,'robe regalia family')
need(t('src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java'),
     'syncAtomicRobe','atomic robe invariant')
need(doc,'## Alpha.25 visual-language refinement','alpha25 documentation')
need(doc,'No external texture','external asset exclusion')

print('Arcane Circle alpha.25 visual-language audit: PASS')
print('codex=artifact-workspace sigil=secondary-notation robe=style-regalia')
print('source_mutation=disabled')

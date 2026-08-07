from pathlib import Path

path = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenAuthoredRoadNormalizer.java')
text = path.read_text(encoding='utf-8')
old = '    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;\n'
new = ('    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS\n'
       '            | Block.UPDATE_KNOWN_SHAPE\n'
       '            | Block.UPDATE_SUPPRESS_DROPS;\n')
if old not in text:
    if new in text:
        raise SystemExit(0)
    raise SystemExit('authored road update flags pattern missing')
path.write_text(text.replace(old, new, 1), encoding='utf-8')

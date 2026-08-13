from pathlib import Path
import runpy

repo=Path(__file__).resolve().parents[2]
original=repo/'.github/scripts/arcane_alpha30_20260813.py'
text=original.read_text(encoding='utf-8')
start=text.index("replace_once(g,\n             'action(g,a,label,inside(mouseX,mouseY,a),usable&&activeSlot>=0,accent);'")
end=text.index("\n\nnew_loadout =",start)
replacement='''text = read(g)\nold_action = 'action(g,a,label,inside(mouseX,mouseY,a),usable&&activeSlot>=0,accent);'\nnew_action = 'action(g,a,label,inside(mouseX,mouseY,a),usable&&(activeSlot>=0||firstEmptySlot()>=0),accent);'\nif text.count(old_action) != 2:\n    raise SystemExit(f'expected two detail action anchors, found {text.count(old_action)}')\nwrite(g, text.replace(old_action, new_action))\n'''
original.write_text(text[:start]+replacement+text[end:],encoding='utf-8')
runpy.run_path(str(original),run_name='__main__')
(repo/'.github/scripts/arcane_alpha30_retry_20260813.py').unlink(missing_ok=True)
(repo/'.github/workflows/maintenance-arcane30-retry-20260813.yml').unlink(missing_ok=True)
print('alpha.30 retry wrapper complete')

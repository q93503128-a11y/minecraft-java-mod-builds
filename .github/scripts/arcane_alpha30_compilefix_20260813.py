from pathlib import Path
repo=Path(__file__).resolve().parents[2]
root=repo/'projects/arcane-circle'
g=root/'src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java'
t=g.read_text(encoding='utf-8')
bad='    private void request    private void request(String next){'
good='    private void request(String next){'
if t.count(bad)!=1: raise SystemExit(f'expected exactly one malformed request declaration, found {t.count(bad)}')
g.write_text(t.replace(bad,good,1),encoding='utf-8')
audit=root/'tools/test_current_source.py'
a=audit.read_text(encoding='utf-8')
anchor="assert 'CodexVisualLanguage' not in grimoire\n"
extra="assert 'CodexVisualLanguage' not in grimoire\nassert 'private void request    private void request' not in grimoire\nassert grimoire.count('private void request(String next)') == 1\n"
if anchor not in a: raise SystemExit('missing audit insertion anchor')
audit.write_text(a.replace(anchor,extra,1),encoding='utf-8')
(repo/'.github/scripts/arcane_alpha30_compilefix_20260813.py').unlink(missing_ok=True)
(repo/'.github/workflows/maintenance-arcane30-compilefix-20260813.yml').unlink(missing_ok=True)
print('alpha.30 compile declaration fixed')

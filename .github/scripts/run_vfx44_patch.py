from pathlib import Path
import textwrap

workflow = Path('.github/workflows/tmp-vfx44.yml')
source = workflow.read_text(encoding='utf-8')
start_marker = "          python3 - <<'PY'\n"
end_marker = "\n          PY\n"
start = source.find(start_marker)
if start < 0:
    raise SystemExit('VFX44 embedded Python start marker not found')
start += len(start_marker)
end = source.find(end_marker, start)
if end < 0:
    raise SystemExit('VFX44 embedded Python end marker not found')
code = textwrap.dedent(source[start:end])
compile(code, '<vfx44-embedded-patch>', 'exec')
exec(compile(code, '<vfx44-embedded-patch>', 'exec'), {'__name__': '__main__'})

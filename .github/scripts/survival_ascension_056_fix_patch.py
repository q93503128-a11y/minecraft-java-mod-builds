from pathlib import Path

fix_path = Path(__file__)
patch_path = fix_path.with_name("survival_ascension_056_patch.py")
text = patch_path.read_text(encoding="utf-8")
old = "from textwrap import dedent\n"
new = '''from textwrap import dedent as _textwrap_dedent

def dedent(text: str) -> str:
    # Java source anchors intentionally start with their real class indentation.
    # Documentation/test payloads start at column zero and still use normal dedent.
    return text if text[:1].isspace() else _textwrap_dedent(text)
'''
if text.count(old) != 1:
    raise SystemExit(f"expected one dedent import, got {text.count(old)}")
text = text.replace(old, new, 1)
compile(text, str(patch_path), "exec")
patch_path.write_text(text, encoding="utf-8")
fix_path.unlink()
print("0.56 staging Java anchor indentation repaired; fixer self-removed")

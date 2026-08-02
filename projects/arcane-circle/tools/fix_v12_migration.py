#!/usr/bin/env python3
from pathlib import Path

path = Path(__file__).with_name("apply_v12_overhaul.py")
text = path.read_text(encoding="utf-8")
old = '''def strip_calls(source: str, token: str) -> str:
    while token in source:
        pos = source.index(token)
        line_start = source.rfind("\\n", 0, pos) + 1
        paren = source.find("(", pos)
        depth = 0
        end = -1
        for index in range(paren, len(source)):
            char = source[index]
            if char == "(":
                depth += 1
            elif char == ")":
                depth -= 1
                if depth == 0:
                    semicolon = source.find(";", index)
                    if semicolon < 0:
                        raise RuntimeError(f"missing semicolon after {token}")
                    end = semicolon + 1
                    break
        if end < 0:
            raise RuntimeError(f"unclosed call: {token}")
        indent = source[line_start:pos]
        source = source[:line_start] + indent + "WorldMagicService.noParticles();" + source[end:]
    return source
'''
new = '''def strip_calls(source: str, token: str) -> str:
    while token in source:
        pos = source.index(token)
        paren = source.find("(", pos)
        depth = 0
        end = -1
        for index in range(paren, len(source)):
            char = source[index]
            if char == "(":
                depth += 1
            elif char == ")":
                depth -= 1
                if depth == 0:
                    semicolon = source.find(";", index)
                    if semicolon < 0:
                        raise RuntimeError(f"missing semicolon after {token}")
                    end = semicolon + 1
                    break
        if end < 0:
            raise RuntimeError(f"unclosed call: {token}")
        source = source[:pos] + "WorldMagicService.noParticles();" + source[end:]
    return source
'''
if old in text:
    text = text.replace(old, new)
text = text.replace('strip_calls(casting, ".sendParticles")',
                    'strip_calls(casting, "level.sendParticles")')
text = text.replace(
    '''needle = '    "kr/moonseungjun/arcanecircle/network/ArcaneNetwork.class",\\n'\naddition = ('    "kr/moonseungjun/arcanecircle/network/ArcaneNetwork.class",\\n'\n''',
    '''needle = '    "kr/moonseungjun/arcanecircle/ArcaneCircle.class",\\n'\naddition = ('    "kr/moonseungjun/arcanecircle/ArcaneCircle.class",\\n'\n''')
path.write_text(text, encoding="utf-8")
print("v0.12 migration call stripper and JAR insertion point normalized")

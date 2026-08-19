#!/usr/bin/env python3
from pathlib import Path
import base64
import json
import subprocess
import sys
import time

ROOT = Path("projects/arcane-circle")
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"
CLIENT = JAVA / "client"
MAGIC = JAVA / "magic"
RES = ROOT / "src/main/resources/data/arcanecircle"
SELF = Path(".github/scripts/tmp_arcane_alpha46.py")

def read(p):
    return p.read_text(encoding="utf-8")

def write(p, s):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(s, encoding="utf-8")

def replace_once(p, old, new):
    s = read(p)
    count = s.count(old)
    if count != 1:
        raise SystemExit(f"{p}: expected one anchor, got {count}: {old[:100]!r}")
    write(p, s.replace(old, new, 1))

def replace_all_checked(p, old, new, min_count=1):
    s=read(p)
    count=s.count(old)
    if count < min_count:
        raise SystemExit(f"{p}: missing anchor {old[:100]!r}")
    write(p,s.replace(old,new))

gradle = ROOT / "gradle.properties"
if "mod_version=0.12.1-alpha.45" not in read(gradle):
    raise SystemExit("alpha.46 migration must start from alpha.45")

replace_once(gradle, "mod_version=0.12.1-alpha.45", "mod_version=0.12.1-alpha.46")
replace_once(JAVA / "ArcaneCircle.java",
             'public static final String VERSION = "0.12.1-alpha.45";',
             'public static final String VERSION = "0.12.1-alpha.46";')
replace_once(RES / "spell_catalog/index.json",
             '"version": "0.12.1-alpha.45"', '"version": "0.12.1-alpha.46"')
replace_once(RES / "spell_catalog/index.json",
             '"crafting_progression": false', '"crafting_progression": true')

regalia = base64.b64decode("CnBhY2thZ2Uga3IubW9vbnNldW5nanVuLmFyY2FuZWNpcmNsZS5jbGllbnQ7CgppbXBvcnQga3IubW9vbnNldW5nanVuLmFyY2FuZWNpcmNsZS5tYWdpYy5TcGVsbERlZmluaXRpb247CmltcG9ydCBuZXQubWluZWNyYWZ0LndvcmxkLnBoeXMuVmVjMzsKCmltcG9ydCBqYXZhLnV0aWwuU2V0OwoKLyoqCiAqIE1haW50YWluZWQgc2VsZi1idWZmcyB1c2Ugd2VhcmFibGUgc2lsaG91ZXR0ZXMgaW5zdGVhZCBvZiBrZWVwaW5nIGEgY2FzdGluZyBjaXJjbGUgaW4gdGhlCiAqIHBsYXllcidzIGZhY2UuIFRoZSBjYXN0L3JlbGVhc2Ugc3RpbGwgZ2V0cyBpdHMgYXV0aG9yZWQgY2lyY2xlIGZvciBhIHNob3J0IGFmdGVyZ2xvdzsgdGhpcyBjbGFzcwogKiBvd25zIHRoZSBsb25nLWxpdmVkIGlkZW50aXR5IGFmdGVyIHRoYXQgbW9tZW50LgogKi8KZmluYWwgY2xhc3MgUGVyc2lzdGVudEJ1ZmZSZWdhbGlhIHsKICAgIHByaXZhdGUgc3RhdGljIGZpbmFsIFNldDxTdHJpbmc+IE1BSU5UQUlORUQgPSBTZXQub2YoCiAgICAgICAgICAgICJzaGllbGQiLCAiZmVhdGhlcl9mYWxsIiwgImxpZ2h0IiwgIm1hZ2VfYXJtb3IiLCAibWlycm9yX2ltYWdlIiwgImludmlzaWJpbGl0eSIsCiAgICAgICAgICAgICJibHVyIiwgImZseSIsICJoYXN0ZSIsICJwcm90ZWN0aW9uX2Zyb21fZW5lcmd5IiwgImdyZWF0ZXJfaW52aXNpYmlsaXR5IiwKICAgICAgICAgICAgInJlc2lsaWVudF9zcGhlcmUiLCAic3RvbmVza2luIiwgImZyZWVkb21fb2ZfbW92ZW1lbnQiLCAidHJ1ZV9zZWVpbmciLAogICAgICAgICAgICAiZ2xvYmVfb2ZfaW52dWxuZXJhYmlsaXR5IiwgInNpbXVsYWNydW0iLCAiY2xvbmUiLCAiZmlyZV9zaGllbGQiLCAic29sYXJfZ3VhcmQiLAogICAgICAgICAgICAiZXRoZXJlYWxuZXNzIiwgInNoYXBlY2hhbmdlIiwgImZvcmVzaWdodCIpOwoKICAgIHByaXZhdGUgUGVyc2lzdGVudEJ1ZmZSZWdhbGlhKCkge30KCiAgICBzdGF0aWMgYm9vbGVhbiBoYW5kbGVzKFNwZWxsRGVmaW5pdGlvbiBzcGVsbCkgewogICAgICAgIHJldHVybiBzcGVsbCAhPSBudWxsICYmIE1BSU5UQUlORUQuY29udGFpbnMoc3BlbGwuaWQoKSk7CiAgICB9CgogICAgc3RhdGljIEFyY2FuZVdvcmxkTWVzaCByZWxlYXNlKFNwZWxsRGVmaW5pdGlvbiBzcGVsbCwgVmVjMyBkaXJlY3Rpb24sCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgZG91YmxlIGVsYXBzZWRTZWNvbmRzLCBkb3VibGUgZHVyYXRpb25TZWNvbmRzLCBsb25nIHNlZWQpIHsKICAgICAgICBBcmNhbmVXb3JsZE1lc2guQnVpbGRlciBiID0gc3BlbGwuY2lyY2xlKCkgPj0gNwogICAgICAgICAgICAgICAgPyBBcmNhbmVXb3JsZE1lc2guZGV0YWlsQnVpbGRlcig3ODApCiAgICAgICAgICAgICAgICA6IEFyY2FuZVdvcmxkTWVzaC5maW5lQnVpbGRlcig1MjApOwogICAgICAgIFZlYzMgZm9yd2FyZCA9IGZsYXQoZGlyZWN0aW9uKTsKICAgICAgICBWZWMzIHJpZ2h0ID0gbmV3IFZlYzMoLWZvcndhcmQuemwgLSBmb3J3YXJkLnpsKyBmb3J3YXJkLnosIDAuMCwgZm9yd2FyZC54KS5ub3JtYWxpemUoKTsKICAgICAgICBWZWMzIHVwID0gbmV3IFZlYzMoMC4wLCAxLjAsIDAuMCk7CiAgICAgICAgVmVjMyBiYWNrID0gZm9yd2FyZC5zY2FsZSgtMS4wKTsKICAgICAgICBkb3VibGUgdCA9IGVsYXBzZWRTZWNvbmRzOwogICAgICAgIGRvdWJsZSBwdWxzZSA9IC45MiArIC4wOCAqIE1hdGguc2luKHQgKiAzLjEgKyAoc2VlZCAmIDMxKSAqIC4xNyk7CgogICAgICAgIHN3aXRjaCAoc3BlbGwuaWQoKSkgewogICAgICAgICAgICBjYXNlICJmZWF0aGVyX2ZhbGwiIC0+IGZlYXRoZXJXaW5ncyhiLCBmb3J3YXJkLCByaWdodCwgdXAsIGJhY2ssIC44MiwgLjgyLCB0LCBmYWxzZSk7CiAgICAgICAgICAgIGNhc2UgImZseSIgLT4gZmVhdGhlcldpbmdzKGIsIGZvcndhcmQsIHJpZ2h0LCB1cCwgYmFjaywgMS40MiwgMS4yNiwgdCwgdHJ1ZSk7CiAgICAgICAgICAgIGNhc2UgImV0aGVyZWFsbmVzcyIgLT4gewogICAgICAgICAgICAgICAgZmVhdGhlcldpbmdzKGIsIGZvcndhcmQsIHJpZ2h0LCB1cCwgYmFjaywgMS41OCwgMS4zOCwgdCAqIC43MiwgdHJ1ZSk7CiAgICAgICAgICAgICAgICB2ZWlsU2hhcmRzKGIsIGZvcndhcmQsIHJpZ2h0LCB1cCwgYmFjaywgNiwgMS4yNCwgdCAqIC41NSk7CiAgICAgICAgICAgIH0KICAgICAgICAgICAgY2FzZSAic2hpZWxkIiAtPiBhcm1vck1hbnRsZShiLCBmb3J3YXJkLCByaWdodCwgdXAsIGJhY2ssIDIsIC40NiwgLjgyLCB0KTsKICAgICAgICAgICAgY2FzZSAibWFnZV9hcm1vciIgLT4gYXJtb3JNYW50bGUoYiwgZm9yd2FyZCwgcmlnaHQsIHVwLCBiYWNrLCA2LCAuNTgsIDEuMDgsIHQpOwogICAgICAgICAgICBjYXNlICJwcm90ZWN0aW9uX2Zyb21fZW5lcmd5IiAtPiBhcm1vck1hbnRsZShiLCBmb3J3YXJkLCByaWdodCwgdXAsIGJhY2ssIDgsIC42MiwgMS4xNiwgdCk7CiAgICAgICAgICAgIGNhc2UgInJlc2lsaWVudF9zcGhlcmUiIC0+IGFybW9yTWFudGxlKGIsIGZvcndhcmQsIHJpZ2h0LCB1cCwgYmFjaywgMTAsIC43MCwgMS4zMCwgdCk7CiAgICAgICAgICAgIGNhc2UgImdsb2JlX29mX2ludnVsbmVyYWJpbGl0eSIgLT4gewogICAgICAgICAgICAgICAgYXJtb3JNYW50bGUoYiwgZm9yd2FyZCwgcmlnaHQsIHVwLCBiYWNrLCAxMiwgLjc4LCAxLjQyLCB0ICogLjcwKTsKICAgICAgICAgICAgICAgIGNyb3duKGIsIGZvcndhcmQsIHJpZ2h0LCB1cCwgYmFjaywgNiwgMS4yNiwgLjI0LCB0ICogLjQ1KTsKICAgICAgICAgICAgfQogICAgICAgICAgICBjYXNlICJzdG9uZXNraW4iIC0+IHN0b25lQ2FyYXBhY2UoYiwgZm9yd2FyZCwgcmlnaHQsIHVwLCBiYWNrLCB0KTsKICAgICAgICAgICAgY2FzZSAiZmlyZV9zaGllbGQiIC0+IGZsYW1lQmxhZGVzKGIsIGZvcndhcmQsIHJpZ2h0LCB1cCwgYmFjaywgNiwgMS4wMiwgdCk7CiAgICAgICAgICAgIGNhc2UgInNvbGFyX2d1YXJkIiAtPiB7CiAgICAgICAgICAgICAgICBmbGFtZUJsYWRlcyhiLCBmb3J3YXJkLCByaWdodCwgdXAsIGJhY2ssIDgsIDEuMTgsIHQgKiAuNzgpOwogICAgICAgICAgICAgICAgY3Jvd24oYiwgZm9yd2FyZCwgcmlnaHQsIHVwLCBiYWNrLCA4LCAxLjM2LCAuMjIsIHQgKiAuNTUpOwogICAgICAgICAgICB9CiAgICAgICAgICAgIGNhc2UgImhhc3RlIiAtPiBzcGVlZEZpbnMoYiwgZm9yd2FyZCwgcmlnaHQsIHVwLCBiYWNrLCB0LCAxLjApOwogICAgICAgICAgICBjYXNlICJmcmVlZG9tX29mX21vdmVtZW50IiAtPiB7CiAgICAgICAgICAgICAgICBzcGVlZEZpbnMoYiwgZm9yd2FyZCwgcmlnaHQsIHVwLCBiYWNrLCB0ICogLjY1LCAuODYpOwogICAgICAgICAgICAgICAgdHJhaWxpbmdSaWJib25zKGIsIGZvcndhcmQsIHJpZ2h0LCB1cCwgYmFjaywgdCwgNCk7CiAgICAgICAgICAgIH0KICAgICAgICAgICAgY2FzZSAiaW52aXNpYmlsaXR5IiAtPiB2ZWlsU2hhcmRzKGIsIGZvcndhcmQsIHJpZ2h0LCB1cCwgYmFjaywgNCwgLjgyLCB0ICogLjQ1KTsKICAgICAgICAgICAgY2FzZSAiZ3JlYXRlcl9pbnZpc2liaWxpdHkiIC0+IHZlaWxTaGFyZHMoYiwgZm9yd2FyZCwgcmlnaHQsIHVwLCBiYWNrLCA4LCAxLjA4LCB0ICogLjYyKTsKICAgICAgICAgICAgY2FzZSAiYmx1ciIgLT4gZ2hvc3RPZmZzZXRzKGIsIGZvcndhcmQsIHJpZ2h0LCB1cCwgYmFjaywgdCk7CiAgICAgICAgICAgIGNhc2UgIm1pcnJvcl9pbWFnZSIgLT4gbWlycm9yTWFudGxlKGIsIGZvcndhcmQsIHJpZ2h0LCB1cCwgYmFjaywgdCk7CiAgICAgICAgICAgIGNhc2UgInRydWVfc2VlaW5nIiAtPiBzaWdodENyb3duKGIsIGZvcndhcmQsIHJpZ2h0LCB1cCwgYmFjaywgdCwgZmFsc2UpOwogICAgICAgICAgICBjYXNlICJmb3Jlc2lnaHQiIC0+IHNpZ2h0Q3Jvd24oYiwgZm9yd2FyZCwgcmlnaHQsIHVwLCBiYWNrLCB0LCB0cnVlKTsKICAgICAgICAgICAgY2FzZSAic2ltdWxhY3J1bSIgLT4gcmVzZXJ2ZUJvZHkoYiwgZm9yd2FyZCwgcmlnaHQsIHVwLCBiYWNrLCB0LCBmYWxzZSk7CiAgICAgICAgICAgIGNhc2UgImNsb25lIiAtPiByZXNlcnZlQm9keShiLCBmb3J3YXJkLCByaWdodCwgdXAsIGJhY2ssIHQsIHRydWUpOwogICAgICAgICAgICBjYXNlICJzaGFwZWNoYW5nZSIgLT4gc2hhcGVjaGFuZ2VNYW50bGUoYiwgZm9yd2FyZCwgcmlnaHQsIHVwLCBiYWNrLCB0KTsKICAgICAgICAgICAgY2FzZSAibGlnaHQiIC0+IHNob3VsZGVyV2lzcChiLCBmb3J3YXJkLCByaWdodCwgdXAsIGJhY2ssIHQpOwogICAgICAgICAgICBkZWZhdWx0IC0+IGFybW9yTWFudGxlKGIsIGZvcndhcmQsIHJpZ2h0LCB1cCwgYmFjaywgNCwgLjQ4LCAuOTAsIHQpOwogICAgICAgIH0KCiAgICAgICAgaWYgKHNwZWxsLmNpcmNsZSgpID49IDcpIHsKICAgICAgICAgICAgaW50IHBvaW50cyA9IHNwZWxsLmNpcmNsZSgpID09IDcgPyA1IDogc3BlbGwuY2lyY2xlKCkgPT0gOCA/IDcgOiA5OwogICAgICAgICAgICBkb3VibGUgaGVpZ2h0ID0gc3BlbGwuY2lyY2xlKCkgPT0gOSA/IDEuNzIgOiAxLjUyOwogICAgICAgICAgICBjcm93bihiLCBmb3J3YXJkLCByaWdodCwgdXAsIGJhY2ssIHBvaW50cywgaGVpZ2h0LCAuMTYgKiBwdWxzZSwgdCAqIC4yOCk7CiAgICAgIH0KICAgICAgICByZXR1cm4gYi5idWlsZCgpOwogICAgfQoKICAgIHByaXZhdGUgc3RhdGljIHZvaWQgZmVhdGhlcldpbmdzKEFyY2FuZVdvcmxkTWVzaC5CdWlsZGVyIGIsIFZlYzMgZiwgVmVjMyByLCBWZWMzIHUsIFZlYzMgYmFjaywKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGRvdWJsZSBzcGFuLCBkb3VibGUgaGVpZ2h0LCBkb3VibGUgdCwgYm9vbGVhbiBncmFuZCkgewogICAgICAgIGRvdWJsZSBmbGFwID0gTWF0aC5zaW4odCAqIChncmFuZCA/IDIuMSA6IDEuNTUpKSAqIChncmFuZCA/IC4xMiA6IC4wNik7CiAgICAgICAgVmVjMyByb290ID0gdS5zY2FsZShncmFuZCA/IDEuMDUgOiAuODgpLmFkZChiYWNrLnNjYWxlKGdyYW5kID8gLjM0IDogLjI0KSk7CiAgICAgICAgaW50IGZlYXRoZXJzID0gZ3JhbmQgPyA3IDogNTsKICAgICAgICBmb3IgKGludCBzaWRlIDogbmV3IGludFtdey0xLCAxfSkgewogICAgICAgICAgICBWZWMzIGhpbmdlID0gcm9vdC5hZGQoci5zY2FsZShzaWRlICogLjE4KSk7CiAgICAgICAgICAgIFZlYzMgc2hvdWxkZXIgPSByb290LmFkZChyLnNjYWxlKHNpZGUgKiAuMzQpKS5hZGQodS5zY2FsZSguMDgpKTsKICAgICAgICAgICAgYi5saW5lKGhpbmdlLCBzaG91bGRlciwgZ3JhbmQgPyAxLjI1RiA6IC45MkYsIDEuMThGLCAuNzhGKTsKICAgICAgICAgICAgZm9yIChpbnQgaSA9IDA7IGkgPCBmZWF0aGVyczsgaSsrKSB7CiAgICAgICAgICAgICAgICBkb3VibGUgcSA9IGkgLyAoZG91YmxlKSBNYXRoLm1heCgxLCBmZWF0aGVycyAtIDEpOwogICAgICAgICAgICAgICAgZG91YmxlIGxhdGVyYWwgPSBzcGFuICogKC41MiArIHEgKiAuNTgpOwogICAgICAgICAgICAgICAgZG91YmxlIGxpZnQgPSBoZWlnaHQgKiAoLjQ0IC0gcSAqIC4zNikgKyBmbGFwICogKDEuMCAtIHEgKiAuMzUpOwogICAgICAgICAgICAgICAgZG91YmxlIHJlYXIgPSAuMjIgKyBxICogKGdyYW5kID8gLjcyIDogLjQ4KTsKICAgICAgICAgICAgICAgIFZlYzMgYmFzZSA9IGhpbmdlLmFkZChyLnNjYWxlKHNpZGUgKiAoLjEwICsgcSAqIC4xNSkpKS5hZGQodS5zY2FsZSguMDQgLSBxICogLjA0KSk7CiAgICAgICAgICAgICAgICBWZWMzIG1pZCA9IHJvb3QuYWRkKHIuc2NhbGUoc2lkZSAqIGxhdGVyYWwgKiAuNjgpKS5hZGQodS5zY2FsZShsaWZ0ICogLjcyKSkKICAgICAgICAgICAgICAgICAgICAgICAgLmFkZChiYWNrLnNjYWxlKHJlYXIgKiAuNTgpKTsKICAgICAgICAgICAgICAgIFZlYzMgdGlwID0gcm9vdC5hZGQoci5zY2FsZShzaWRlICogbGF0ZXJhbCkpLmFkZCh1LnNjYWxlKGxpZnQpKS5hZGQoYmFjay5zY2FsZShyZWFyKSk7CiAgICAgICAgICAgICAgICBWZWMzIGxvd2VyID0gbWlkLmFkZCh1LnNjYWxlKC0uMTMgLSBxICogLjA2KSkuYWRkKHIuc2NhbGUoLXNpZGUgKiAuMDUpKTsKICAgICAgICAgICAgICAgIGIudHJpYW5nbGUoYmFzZSwgbWlkLCBsb3dlciwgMS4wMkYgKyAoZmxvYXQpKHEgKiAuMTYpLCBncmFuZCA/IC4zMEYgOiAuMjJGKTsKICAgICAgICAgICAgICAgIGIubGluZShiYXNlLCB0aXAsIGdyYW5kID8gLjg0RiA6IC42MkYsIDEuMThGLCAuNzJGKTsKICAgICAgICAgICAgICAgIGIubGluZShsb3dlciwgdGlwLCBncmFuZCA/IC41NEYgOiAuNDJGLCAuOTRGLCAuNDhGKTsKICAgICAgICAgICAgfQogICAgICAgIH0KICAgIH0KCiAgICBwcml2YXRlIHN0YXRpYyB2b2lkIGFybW9yTWFudGxlKEFyY2FuZVdvcmxkTWVzaC5CdWlsZGVyIGIsIFZlYzMgZiwgVmVjMyByLCBWZWMzIHUsIFZlYzMgYmFjaywKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGludCBwbGF0ZXMsIGRvdWJsZSB3aWR0aCwgZG91YmxlIGhlaWdodCwgZG91YmxlIHQpIHsKICAgICAgICBWZWMzIGFuY2hvciA9IHUuc2NhbGUoLjc0KS5hZGQoYmFjay5zY2FsZSguMjApKTsKICAgICAgICBBcmNhbmVXb3JsZE1lc2guQmFzaXMgZmFjaW5nID0gQXJjYW5lV29ybGRNZXNoLkJhc2lzLmZhY2luZyhmKTsKICAgICAgICBpbnQgbiA9IE1hdGgubWF4KDIsIHBsYXRlcyk7CiAgICAgICAgZm9yIChpbnQgaSA9IDA7IGkgPCBuOyBpKyspIHsKICAgICAgICAgICAgaW50IHNpZGUgPSAoaSAmIDEpID09IDAgPyAtMSA6IDE7CiAgICAgICAgICAgIGludCByb3cgPSBpIC8gMjsKICAgICAgICAgICAgZG91YmxlIHkgPSBoZWlnaHQgLSByb3cgKiAuMjc7CiAgICAgICAgICAgIGRvdWJsZSB4ID0gd2lkdGggKiAoLjYyICsgKHJvdyAlIDIpICogLjIyKTsKICAgICAgICAgICAgZG91YmxlIGJvYiA9IE1hdGguc2luKHQgKiAxLjcgKyBpICogMS4yKSAqIC4wMzU7CiAgICAgICAgICAgIFZlYzMgYXQgPSBhbmNob3IuYWRkKHIuc2NhbGUoc2lkZSAqIHgpKS5hZGQodS5zY2FsZSh5IC0gLjY2ICsgYm9iKSkKICAgICAgICAgICAgICAgICAgICAuYWRkKGJhY2suc2NhbGUoLjA2ICsgcm93ICogLjA0NSkpOwogICAgICAgICAgICBkb3VibGUgcmFkaXVzID0gTWF0aC5tYXgoLjEyLCAuMjIgLSByb3cgKiAuMDE1KTsKICAgICAgICAgICAgYi5kaWFtb25kKGZhY2luZywgYXQsIHJhZGl1cywgdCAqIC4xOCAqIHNpZGUsIDEuMDhGLCAuMzBGKTsKICAgICAgICAgICAgYi5saW5lKGFuY2hvci5hZGQoci5zY2FsZShzaWRlICogLjIyKSkuYWRkKHUuc2NhbGUoLjE4KSksIGF0LCAuNDJGLCAuNzhGLCAuMzBGKTsKICAgICAgICB9CiAgICB9CgogICAgcHJpdmF0ZSBzdGF0aWMgdm9pZCBzdG9uZUNhcmFwYWNlKEFyY2FuZVdvcmxkTWVzaC5CdWlsZGVyIGIsIFZlYzMgZiwgVmVjMyByLCBWZWMzIHUsIFZlYzMgYmFjaywgZG91YmxlIHQpIHsKICAgICAgICBWZWMzIGNlbnRlciA9IHUuc2NhbGUoLjY4KS5hZGQoYmFjay5zY2FsZSguMjgpKTsKICAgICAgICBBcmNhbmVXb3JsZE1lc2guQmFzaXMgZmFjZSA9IEFyY2FuZVdvcmxkTWVzaC5CYXNpcy5mYWNpbmcoZik7CiAgICAgICAgZm9yIChpbnQgaSA9IDA7IGkgPCA3OyBpKyspIHsKICAgICAgICAgICAgaW50IHNpZGUgPSAoaSAmIDEpID09IDAgPyAtMSA6IDE7CiAgICAgICAgICAgIGRvdWJsZSByb3cgPSBpIC8gMi4wOwogICAgICAgICAgICBWZWMzIGF0ID0gY2VudGVyLmFkZChyLnNjYWxlKHNpZGUgKiAoLjI0ICsgLjA4ICogKGkgJSAzKSkpKQogICAgICAgICAgICAgICAgICAgIC5hZGQodS5zY2FsZSguNTIgLSByb3cgKiAuMjMpKS5hZGQoYmFjay5zY2FsZSguMDQgKiBpKSk7CiAgICAgICAgICAgIGIucG9seWdvblBsYXRlKGZhY2UsIGF0LCAuMjAgKyAuMDI1ICogKGkgJSAyKSwgNSwgdCAqIC4wNSArIGksIC44MkYsIC4zNEYpOwogICAgICAgICAgICBiLnBvbHlnb24oZmFjZSwgYXQsIC4yMiArIC4wMjUgKiAoaSAlIDIpLCA1LCB0ICogLjA1ICsgaSwgLjQ4Rik7CiAgICAgIH0KICAgIH0KCiAgICBwcml2YXRlIHN0YXRpYyB2b2lkIGZsYW1lQmxhZGVzKEFyY2FuZVdvcmxkTWVzaC5CdWlsZGVyIGIsIFZlYzMgZiwgVmVjMyByLCBWZWMzIHUsIFZlYzMgYmFjaywKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGludCBjb3VudCwgZG91YmxlIHNjYWxlLCBkb3VibGUgdCkgewogICAgICAgIFZlYzMgYmFzZSA9IHUuc2NhbGUoLjcyKS5hZGQoYmFjay5zY2FsZSguMzApKTsKICAgICAgICBmb3IgKGludCBpID0gMDsgaSA8IGNvdW50OyBpKyspIHsKICAgICAgICAgICAgaW50IHNpZGUgPSAoaSAmIDEpID09IDAgPyAtMSA6IDE7CiAgICAgICAgICAgIGludCByb3cgPSBpIC8gMjsKICAgICAgICAgICAgVmVjMyIHJvb3QgPSBiYXNlLmFkZChyLnNjYWxlKHNpZGUgKiAoLjMyICsgcm93ICogLjEyKSkpLmFkZCh1LnNjYWxlKC4zNSAtIHJvdyAqIC4xOCkpOwogICAgICAgICAgICBWZWMzIHRpcCA9IHJvb3QuYWRkKHIuc2NhbGUoc2lkZSAqICguMTggKyByb3cgKiAuMDYpKSkKICAgICAgICAgICAgICAgICAgICAuYWRkKHUuc2NhbGUoKC42MiAtIHJvdyAqIC4wNykgKiBzY2FsZSkpCiAgICAgICAgICAgICAgICAgICAgLmFkZChiYWNrLnNjYWxlKC4xNiArIHJvdyAqIC4wOCkpOwogICAgICAgICAgICBWZWMzIGlubmVyID0gcm9vdC5hZGQoci5zY2FsZSgtc2lkZSAqIC4wOSkpLmFkZCh1LnNjYWxlKC4yMyAqIHNjYWxlKSkuYWRkKGJhY2suc2NhbGUoLjA4KSk7CiAgICAgICAgICAgIGIudHJpYW5nbGUocm9vdCwgaW5uZXIsIHRpcCwgMS4yMEYsIC4zMEYpOwogICAgICAgICAgICBiLmxpbmUocm9vdCwgdGlwLCAuNzJGLCAxLjMwRiwgLjgyRik7CiAgICAgICAgfQogICAgfQoKICAgIHByaXZhdGUgc3RhdGljIHZvaWQgc3BlZWRGaW5zKEFyY2FuZVdvcmxkTWVzaC5CdWlsZGVyIGIsIFZlYzMgZiwgVmVjMyByLCBWZWMzIHUsIFZlYzMgYmFjaywKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGRvdWJsZSB0LCBkb3VibGUgc2NhbGUpIHsKICAgICAgICBmb3IgKGludCBzaWRlIDogbmV3IGludFtdey0xLCAxfSkgewogICAgICAgICAgICBmb3IgKGludCBpID0gMDsgaSA8IDM7IGkrKykgewogICAgICAgICAgICAgICAgZG91YmxlIHkgPSAuMTIgKyBpICogLjE4OwogICAgICAgICAgICAgICAgVmVjMyIHJvb3QgPSByLnNjYWxlKHNpZGUgKiAoLjE4ICsgaSAqIC4wMzUpKS5hZGQodS5zY2FsZSh5KSk7CiAgICAgICAgICAgICAgICBWZWMzIHRpcCA9IHJvb3QuYWRkKGJhY2suc2NhbGUoKC41MiArIGkgKiAuMjEpICogc2NhbGUpKQogICAgICAgICAgICAgICAgICAgICAgLmFkZChyLnNjYWxlKHNpZGUgKiAoLjEyICsgaSAqIC4wNCkpKS5hZGQodS5zY2FsZSguMDUgKiBNYXRoLnNpbih0ICogNC4yICsgaSkpKTsKICAgICAgICAgICAgICAgIFZlYzMgbG93ZXIgPSByb290LmFkZChiYWNrLnNjYWxlKC4xOCAqIHNjYWxlKSkuYWRkKHUuc2NhbGUoLS4xMCkpOwogICAgICAgICAgICAgICAgYi50cmlhbmdsZShyb290LCBsb3dlciwgdGlwLCAxLjEwRiwgLjIyRik7CiAgICAgICAgICAgICAgICBiLmxpbmUocm9vdCwgdGlwLCAuNDhGLCAxLjI0RiwgLjY4Rik7CiAgICAgICAgICAgIH0KICAgICAgICB9CiAgICB9CgogICAgcHJpdmF0ZSBzdGF0aWMgdm9pZCB0cmFpbGluZ1JpYmJvbnMoQXJjYW5lV29ybGRNZXNoLkJ1aWxkZXIgYiwgVmVjMyBmLCBWZWMzIHIsIFZlYzMgdSwgVmVjMyBiYWNrLAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGRvdWJsZSB0LCBpbnQgY291bnQpIHsKICAgICAgICBmb3IgKGludCBpID0gMDsgaSA8IGNvdW50OyBpKyspIHsKICAgICAgICAgICAgaW50IHNpZGUgPSAoaSAmIDEpID09IDAgPyAtMSA6IDE7CiAgICAgICAgICAgIFZlYzMgcm9vdCA9IHIuc2NhbGUoc2lkZSAqICguMjAgKyAoaSAvIDIpICogLjExKSkuYWRkKHUuc2NhbGUoLjU4IC0gKGkgLyAyKSAqIC4yMCkpCiAgICAgICAgICAgICAgICAgICAgLmFkZChiYWNrLnNjYWxlKC4xOCkpOwogICAgICAgICAgICBWZWMzIHAxID0gcm9vdC5hZGQoYmFjay5zY2FsZSguNDUpKS5hZGQoci5zY2FsZShzaWRlICogLjE0KSk7CiAgICAgICAgICAgIFZlYzMgcDIgPSByb290LmFkZChiYWNrLnNjYWxlKC45MCkpLmFkZChyLnNjYWxlKHNpZGUgKiAoLjIyICsgLjA2ICogTWF0aC5zaW4odCAqIDIuNSArIGkpKSkKICAgICAgICAgICAgICAgICAgICAuYWRkKHUuc2NhbGUoLS4xMCArIC4wOCAqIE1hdGguc2luKHQgKiAyLjEgKyBpKSkpOwogICAgICAgICAgICBiLmxpbmUocm9vdCwgcDEsIC40MkYsIDEuMTJGLCAuNjRGKTsKICAgICAgICAgICAgYi5saW5lKHAxLCBwMiwgLjM2RiwgLjkyRiwgLjQ2Rik7CiAgICAgICAgfQogICAgfQoKICAgIHByaXZhdGUgc3RhdGljIHZvaWQgdmVpbFNoYXJkcyhBcmNhbmVXb3JsZE1lc2guQnVpbGRlciBiLCBWZWMzIGYsIFZlYzMgciwgVmVjMyB1LCBWZWMzIGJhY2ssCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaW50IGNvdW50LCBkb3VibGUgc2NhbGUsIGRvdWJsZSB0KSB7CiAgICAgICAgQXJjYW5lV29ybGRNZXNoLkJhc2lzIGZhY2UgPSBBcmNhbmVXb3JsZE1lc2guQmFzaXMuZmFjaW5nKGYpOwogICAgICAgIGZvciAoaW50IGkgPSAwOyBpIDwgY291bnQ7IGkrKykgewogICAgICAgICAgICBkb3VibGUgYSA9IE1hdGguUEkgKiAyLjAgKiBpIC8gY291bnQgKyB0ICogKGkgJSAyID09IDAgPyAuMTYgOiAtLjEyKTsKICAgICAgICAgICAgZG91YmxlIHNpZGUgPSBNYXRoLmNvcyhhKSAqIC41NCAqIHNjYWxlOwogICAgICAg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NjYWxlKC42NCkpOwogICAgICAgIEFyY2FuZVdvcmxkTWVzaC5CYXNpcyBmYWNlID0gQXJjYW5lV29ybGRNZXNoLkJhc2lzLmZhY2luZyhmKTsKICAgICAgICBiLmRpYW1vbmQoZmFjZSwgY29yZS5hZGQodS5zY2FsZSguMzYpKSwgY2xvbmUgPyAuMzQgOiAuMjgsIHQgKiAuMTIsIDEuMTJGLCAuMjNGKTsKICAgICAgICBiLmxpbmUoY29yZS5hZGQodS5zY2FsZSguMTgpKSwgY29yZS5hZGQodS5zY2FsZSgtLjUyKSksIC41OEYsIDEuMDZGLCAuNDhGKTsKICAgICAgICBmb3IgKGludCBzaWRlIDogbmV3IGludFtdey0xLCAxfSkgewogICAgICAgICAgICBiLmxpbmUoY29yZS5hZGQodS5zY2FsZSguMDIpKSwgY29yZS5hZGQoci5zY2FsZShzaWRlICogLjM0KSkuYWRkKHUuc2NhbGUoLS4yMCkpLCAuNDJGLCAuOTRGLCAuMzhGKTsKICAgICAgICAgICAgYi5saW5lKGNvcmUuYWRkKHUuc2NhbGUoLS40OCkpLCBjb3JlLmFkZChyLnNjYWxlKHNpZGUgKiAuMjQpKS5hZGQodS5zY2FsZSgtLjgyKSksIC40MkYsIC45NEYsIC4zOEYpOwogICAgICAgIH0KICAgICAgICBpZiAoY2xvbmUpIGIucG9seWdvbihmYWNlLCBjb3JlLmFkZCh1LnNjYWxlKC4zNikpLCAuNDIsIDYsIHQgKiAuMTAsIC40NEYpOwogICAgfQoKICAgIHByaXZhdGUgc3RhdGljIHZvaWQgc2hhcGVjaGFuZ2VNYW50bGUoQXJjYW5lV29ybGRNZXNoLkJ1aWxkZXIgYiwgVmVjMyBmLCBWZWMzIHIsIFZlYzMgdSwgVmVjMyBiYWNrLCBkb3VibGUgdCkgewogICAgICAgIFZlYzMgc3BpbmUgPSBiYWNrLnNjYWxlKC4zOCkuYWRkKHUuc2NhbGUoLjQ1KSk7CiAgICAgICAgZm9yIChpbnQgaSA9IDA7IGkgPCA2OyBpKyspIHsKICAgICAgICAgICAgZG91YmxlIHkgPSAtLjI1ICsgaSAqIC4yNDsKICAgICAgICAgICAgVmVjMyIHJvb3QgPSBzcGluZS5hZGQodS5zY2FsZSh5KSk7CiAgICAgICAgICAgIFZlYzMgdGlwID0gcm9vdC5hZGQoYmFjay5zY2FsZSguMzAgKyBpICogLjA2KSkuYWRkKHUuc2NhbGUoLjEyICsgaSAqIC4wMzUpKTsKICAgICAgICAgICAgYi50cmlhbmdsZShyb290LmFkZChyLnNjYWxlKC0uMTIpKSwgcm9vdC5hZGQoci5zY2FsZSguMTIpKSwgdGlwLCAxLjA0RiwgLjI0Rik7CiAgICAgICAgICAgIGIubGluZShyb290LCB0aXAsIC41MkYsIDEuMThGLCAuNjRGKTsKICAgICAgICB9CiAgICAgICAgZm9yIChpbnQgc2lkZSA6IG5ldyBpbnR7XXstMSwgMX0pIHsKICAgICAgICAgICAgVmVjMyIGhvcm5Sb290ID0gc3BpbmUuYWRkKHIuc2NhbGUoc2lkZSAqIC4yNCkpLmFkZCh1LnNjYWxlKC44MikpOwogICAgICAgICAgICBWZWMzIGhvcm5UaXAgPSBob3JuUm9vdC5hZGQoci5zY2FsZShzaWRlICogLjU0KSkuYWRkKHUuc2NhbGUoLjM4KSkuYWRkKGJhY2suc2NhbGUoLjI4KSk7CiAgICAgICAgICAgIGIubGluZShob3JuUm9vdCwgaG9yblRpcCwgLjgwRiwgMS4yNUYsIC43NkYpOwogICAgICAgICAgICBWZWMzIGNsYXcgPSBzcGluZS5hZGQoci5zY2FsZShzaWRlICogLjM0KSkuYWRkKHUuc2NhbGUoLS4xMikpOwogICAgICAgICAgICBmb3IgKGludCBpID0gMDsgaSA8IDM7IGkrKykKICAgICAgICAgICAgICAgIGIubGluZShjbGF3LCBjbGF3LmFkZChyLnNjYWxlKHNpZGUgKiAoLjMyICsgaSAqIC4wOCkpKS5hZGQodS5zY2FsZSgtLjIyIC0gaSAqIC4wOCkpCiAgICAgICAgICAgICAgICAgICAgICAgIC5hZGQoYmFjay5zY2FsZSguMTgpKSwgLjQ2RiwgMS4wOEYsIC41NUYpOwogICAgICAgIH0KICAgICAgICBmZWF0aGVyV2luZ3MoYiwgZiwgciwgdSwgYmFjaywgLjg2LCAuNzgsIHQgKiAuNTUsIGZhbHNlKTsKICAgIH0KCiAgICBwcml2YXRlIHN0YXRpYyB2b2lkIHNob3VsZGVyV2lzcChBcmNhbmVXb3JsZE1lc2guQnVpbGRlciBiLCBWZWMzIGYsIFZlYzMgciwgVmVjMyB1LCBWZWMzIGJhY2ssIGRvdWJsZSB0KSB7CiAgICAgICAgVmVjMyIGF0ID0gci5zY2FsZSguNDgpLmFkZCh1LnNjYWxlKC44MiArIC4wOCAqIE1hdGguc2luKHQgKiAyLjEpKSkuYWRkKGJhY2suc2NhbGUoLjIyKSk7CiAgICAgICAgQXJjYW5lV29ybGRNZXNoLkJhc2lzIGZhY2UgPSBBcmNhbmVXb3JsZE1lc2guQmFzaXMuZmFjaW5nKGYpOwogICAgICAgIGIuc3RhclBsYXRlKGZhY2UsIGF0LCAuMTYsIC4wNywgNSwgdCAqIC4zMiwgMS4xOEYsIC4zMEYpOwogICAgICAgIGIubGluZShhdCwgYXQuYWRkKHUuc2NhbGUoLjI2KSksIC4zNEYsIDEuMjJGLCAuNDZGKTsKICAgIH0KCiAgICBwcml2YXRlIHN0YXRpYyB2b2lkIGNyb3duKEFyY2FuZVdvcmxkTWVzaC5CdWlsZGVyIGIsIFZlYzMgZiwgVmVjMyByLCBWZWMzIHUsIFZlYzMgYmFjaywKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGludCBwb2ludHMsIGRvdWJsZSBoZWlnaHQsIGRvdWJsZSBzaXplLCBkb3VibGUgdCkgewogICAgICAgIFZlYzMgY2VudGVyID0gdS5zY2FsZShoZWlnaHQpLmFkZChiYWNrLnNjYWxlKC4zOCkpOwogICAgICAgIEFyY2FuZVdvcmxkTWVzaC5CYXNpcyBmYWNlID0gQXJjYW5lV29ybGRNZXNoLkJhc2lzLmZhY2luZyhmKTsKICAgICAgICBkb3VibGUgd2lkdGggPSBNYXRoLm1heCguMTIsIHNpemUpOwogICAgICAgIGZvciAoaW50IGkgPSAwOyBpIDwgcG9pbnRzOyBpKyspIHsKICAgICAgICAgICAgZG91YmxlIHggPSAoaSAtIChwb2ludHMgLSAxKSAvIDIuMCkgKiB3aWR0aCAqIC43MjsKICAgICAgICAgICAgZG91YmxlIGFyY2ggPSAxLjAgLSBNYXRoLm1pbigxLjAsIE1hdGguYWJzKHgpIC8gTWF0aC5tYXgoLjAxLCBwb2ludHMgKiB3aWR0aCAqIC4zNSkpOwogICAgICAgICAgICBWZWMzIGF0ID0gY2VudGVyLmFkZChyLnNjYWxlKHgpKS5hZGQodS5zY2FsZShhcmNoICogLjE4ICsgLjAyNSAqIE1hdGguc2luKHQgKiAyLjAgKyBpKSkpOwogICAgICAgICAgICBiLmRpYW1vbmQoZmFjZSwgYXQsIHdpZHRoICogKC40OCArIChpICUgMikgKiAuMTApLCB0ICogLjE1ICsgaSwgMS4xMkYsIC4yMkYpOwogICAgICAgICAgICBpZiAoaSA+IDApIHsKICAgICAgICAgICAgICAgIGRvdWJsZSBweCA9IChpIC0gMSAtIChwb2ludHMgLSAxKSAvIDIuMCkgKiB3aWR0aCAqIC43MjsKICAgICAgICAgICAgICAgIFZlYzMgcHJldiA9IGNlbnRlci5hZGQoci5zY2FsZShweCkpLmFkZCh1LnNjYWxlKCgxLjAgLSBNYXRoLm1pbigxLjAsIE1hdGguYWJzKHB4KSAvCiAgICAgICAgICAgICAgICAgICAgICAgIE1hdGgubWF4KC4wMSwgcG9pbnRzICogd2lkdGggKiAuMzUpKSkgKiAuMTgpKTsKICAgICAgICAgICAgICAgIGIubGluZShwcmV2LCBhdCwgLjM0RiwgMS4wMkYsIC40MEYpOwogICAgICAgICAgICB9CiAgICAgICAgfQogICAgfQoKICAgIHByaXZhdGUgc3RhdGljIFZlYzMgZmxhdChWZWMzIHZhbHVlKSB7CiAgICAgICAgVmVjMyB2ID0gdmFsdWUgPT0gbnVsbCA/IFZlYzMuWkVSTyA6IG5ldyBWZWMzKHZhbHVlLngsIDAuMCwgdmFsdWUueik7CiAgICAgICAgcmV0dXJuIHYubGVuZ3RoU3FyKCkgPCAxLjBFLTggPyBuZXcgVmVjMygwLjAsIDAuMCwgMS4wKSA6IHYubm9ybWFsaXplKCk7CiAgICB9Cn0K").decode("utf-8")
write(CLIENT / "PersistentBuffRegalia.java", regalia)

tracker = CLIENT / "WorldMagicTracker.java"
old_release = '''            float opacity=releaseOpacity(v,now);
            Vec3 center=renderCenter(v);
            int color=SpellCinematicDirector.color(v.spell);
            if(!"prismatic_wall".equals(v.spell.id())&&!ArcaneSpellVisualOverhaul.replacesBaseSigil(v.spell)){
                ArcaneWorldMesh echo=MeteorBarragePattern.withSeed(v.seed,
                        ()->ArcaneSigilDirector.releaseEcho(v.spell,v.direction,targetOffset(v),v.range,age,v.fusion,v.startedAt));
                if(echo.size()>0)entries.add(new RenderEntry(center,echo,ArcaneSigilDirector.releaseEchoColor(color,age),36,opacity));
            }
            if(!"prismatic_wall".equals(v.spell.id())&&!ArcaneSpellVisualOverhaul.replacesBaseRelease(v.spell)){
                ArcaneWorldMesh releaseMesh=MeteorBarragePattern.withSeed(v.seed,
                        ()->SpellCinematicDirector.release(v.spell,v.direction,targetOffset(v),v.range,v.power,
                                age,v.impactAge,v.fusion,v.ingredients));
                entries.add(new RenderEntry(center,releaseMesh,color,70,opacity));
            }
            ArcaneWorldMesh authoredRelease=MeteorBarragePattern.withSeed(v.seed,
                    ()->ArcaneSpellVisualOverhaul.release(v.spell,v.direction,targetOffset(v),v.range,v.power,
                            age,elapsedSeconds,durationSeconds,v.seed));
            if(authoredRelease.size()>0)entries.add(new RenderEntry(center,authoredRelease,color,82,opacity));
            if(v.spell.circle()>=7){
                ArcaneWorldMesh timeline=MeteorBarragePattern.withSeed(v.seed,
                        ()->AuthoredHighCircleTimeline.release(v.spell,v.direction,targetOffset(v),v.range,
                                age,v.impactAge,elapsedSeconds,durationSeconds,v.seed));
                if(timeline.size()>0)entries.add(new RenderEntry(center,timeline,color,100,opacity));
                ArcaneWorldMesh maintenance=HighCircleMaintenanceOverlay.release(v.spell,v.direction,targetOffset(v),
                        elapsedSeconds,durationSeconds,v.seed);
                if(maintenance.size()>0)entries.add(new RenderEntry(center,maintenance,color,108,opacity));
            }
'''
new_release = '''            float opacity=releaseOpacity(v,now);
            Vec3 center=renderCenter(v);
            int color=SpellCinematicDirector.color(v.spell);
            boolean regalia=PersistentBuffRegalia.handles(v.spell);
            boolean castingAfterglow=!regalia||elapsedSeconds<.85;
            Vec3 presentationDirection=regalia?maintainedDirection(v):v.direction;
            if(castingAfterglow&&!"prismatic_wall".equals(v.spell.id())&&!ArcaneSpellVisualOverhaul.replacesBaseSigil(v.spell)){
                ArcaneWorldMesh echo=MeteorBarragePattern.withSeed(v.seed,
                        ()->ArcaneSigilDirector.releaseEcho(v.spell,v.direction,targetOffset(v),v.range,age,v.fusion,v.startedAt));
                if(echo.size()>0)entries.add(new RenderEntry(center,echo,ArcaneSigilDirector.releaseEchoColor(color,age),36,opacity));
            }
            if(castingAfterglow&&!"prismatic_wall".equals(v.spell.id())&&!ArcaneSpellVisualOverhaul.replacesBaseRelease(v.spell)){
                ArcaneWorldMesh releaseMesh=MeteorBarragePattern.withSeed(v.seed,
                        ()->SpellCinematicDirector.release(v.spell,v.direction,targetOffset(v),v.range,v.power,
                                age,v.impactAge,v.fusion,v.ingredients));
                entries.add(new RenderEntry(center,releaseMesh,color,70,opacity));
            }
            if(castingAfterglow){
                ArcaneWorldMesh authoredRelease=MeteorBarragePattern.withSeed(v.seed,
                        ()->ArcaneSpellVisualOverhaul.release(v.spell,v.direction,targetOffset(v),v.range,v.power,
                                age,elapsedSeconds,durationSeconds,v.seed));
                if(authoredRelease.size()>0)entries.add(new RenderEntry(center,authoredRelease,color,82,opacity));
                if(v.spell.circle()>=7){
                    ArcaneWorldMesh timeline=MeteorBarragePattern.withSeed(v.seed,
                            ()->AuthoredHighCircleTimeline.release(v.spell,v.direction,targetOffset(v),v.range,
                                    age,v.impactAge,elapsedSeconds,durationSeconds,v.seed));
                    if(timeline.size()>0)entries.add(new RenderEntry(center,timeline,color,100,opacity));
                    ArcaneWorldMesh maintenance=HighCircleMaintenanceOverlay.release(v.spell,v.direction,targetOffset(v),
                            elapsedSeconds,durationSeconds,v.seed);
                    if(maintenance.size()>0)entries.add(new RenderEntry(center,maintenance,color,108,opacity));
                }
            }
            if(regalia){
                ArcaneWorldMesh maintained=MeteorBarragePattern.withSeed(v.seed,
                        ()->PersistentBuffRegalia.release(v.spell,presentationDirection,elapsedSeconds,durationSeconds,v.seed));
                if(maintained.size()>0)entries.add(new RenderEntry(center,maintained,color,116,opacity));
            }
'''
replace_once(tracker, old_release, new_release)
replace_once(tracker,
'''    private static boolean followsCaster(SpellDefinition spell){
        if("time_stop".equals(spell.id()))return false;
        if("antimagic_field".equals(spell.id())||"control_weather".equals(spell.id()))return true;
''',
'''    private static boolean followsCaster(SpellDefinition spell){
        if("time_stop".equals(spell.id()))return false;
        if(PersistentBuffRegalia.handles(spell))return true;
        if("antimagic_field".equals(spell.id())||"control_weather".equals(spell.id()))return true;
''')
replace_once(tracker,
'''    private static Vec3 attachmentOffset(UUID caster,SpellDefinition spell,Vec3 originalCenter){
''',
'''    private static Vec3 maintainedDirection(Visual visual){
        LivingEntity entity=findLiving(visual.caster);
        if(entity!=null){
            Vec3 look=entity.getLookAngle();
            Vec3 flat=new Vec3(look.x,0.0,look.z);
            if(flat.lengthSqr()>1.0E-8)return flat.normalize();
        }
        return visual.direction;
    }

    private static Vec3 attachmentOffset(UUID caster,SpellDefinition spell,Vec3 originalCenter){
''')

grimoire = CLIENT / "GrimoireScreen.java"
replace_once(grimoire,
'''    private boolean clickStaffs(MouseButtonEvent e,Layout l){
        List<StaffProfile> profiles=ModItems.profiles();
        for(int i=0;i<profiles.size();i++)if(inside(e.x(),e.y(),l.listRow(i,scroll,31))){String id=profiles.get(i).id();selectedStaffId=id.equals(selectedStaffId)?"":id;return true;}
        return false;
    }
''',
'''    private boolean clickStaffs(MouseButtonEvent e,Layout l){
        List<StaffProfile> profiles=ModItems.profiles();
        for(int i=0;i<profiles.size();i++)if(inside(e.x(),e.y(),l.staffRow(i,scroll))){String id=profiles.get(i).id();selectedStaffId=id;return true;}
        return false;
    }
''')
replace_once(grimoire,
'''    private void drawStaffs(GuiGraphicsExtractor g,Layout l,int mouseX,int mouseY){
        Rect b=l.body();title(g,b,"지팡이 서고","클릭하면 제원과 조합법을 펼칩니다");Rect v=l.viewport();List<StaffProfile> profiles=ModItems.profiles();
        int listW=l.isWide()?Math.max(190,v.w()/2):v.w();Rect list=new Rect(v.x(),v.y(),listW,v.h());g.enableScissor(list.x(),list.y(),list.right(),list.bottom());
        for(int i=0;i<profiles.size();i++){Rect r=new Rect(list.x(),list.y()+i*31-scroll,list.w(),29);StaffProfile p=profiles.get(i);boolean selected=p.id().equals(selectedStaffId),equipped=p.id().equals(ArcaneClientState.text("staff_id","none"));int accent=p.favoredSchool()==null?0xFFC9A568:ArcaneRenderUtil.schoolColor(p.favoredSchool());if(selected)g.fill(r.x(),r.y()+3,r.x()+2,r.bottom()-3,accent);ArcaneRenderUtil.diamond(g,r.x()+12,r.y()+14,selected?6:4,equipped?0xFFFFD47B:accent);g.text(font,Component.literal(fit(p.displayName(),r.w()-34)),r.x()+25,r.y()+5,selected?0xFFF0E1CA:0xFFB8AE9F);tiny(g,fit(staffStats(p),(r.w()-34)*2),r.x()+25,r.y()+18,0xFF786F65,.50F,false);rule(g,r.bottom(),r.x()+24,r.right()-4,0xFF3B342C);}
        g.disableScissor();if(l.isWide())drawStaffDetail(g,l,selectedStaffId);
    }

    private void drawStaffDetail(GuiGraphicsExtractor g,Layout l,String id){
        Rect d=l.detail();if(d.w()<70||d.h()<45)return;g.enableScissor(d.x(),d.y(),d.right(),d.bottom());g.fill(d.x(),d.y()+4,d.x()+1,d.bottom()-4,0xFF554838);StaffProfile p=id.isBlank()?StaffProfile.NONE:ModItems.profile(id);int cx=d.x()+d.w()/2;int iconR=Math.max(9,Math.min(18,d.h()/7));ArcaneRenderUtil.diamond(g,cx,d.y()+iconR+7,iconR,p==StaffProfile.NONE?0xFF5D564D:0xFFD2AE70);g.centeredText(font,Component.literal(fit(p.displayName(),Math.max(40,d.w()-8))),cx,d.y()+iconR*2+13,0xFFEADCC7);int y=d.y()+iconR*2+30,remaining=Math.max(0,d.bottom()-y-4),lines=Math.max(0,Math.min(4,remaining/9));for(String line:wrap(p.summary(),d.w()-18,lines)){tiny(g,line,d.x()+9,y,0xFFAFA496,.60F,false);y+=9;}if(y+8<d.bottom()){y+=5;int recipeLines=Math.max(0,Math.min(3,(d.bottom()-y-3)/9));for(String line:wrap(p.recipeHint().isBlank()?"제작 정보 없음":"제작 · "+p.recipeHint(),d.w()-18,recipeLines)){tiny(g,line,d.x()+9,y,0xFFD0B789,.56F,false);y+=9;}}g.disableScissor();
    }
''',
'''    private void drawStaffs(GuiGraphicsExtractor g,Layout l,int mouseX,int mouseY){
        Rect b=l.body();title(g,b,"지팡이 서고","제원과 실제 제작 조합을 항상 표시합니다");List<StaffProfile> profiles=ModItems.profiles();
        Rect list=l.staffList();g.enableScissor(list.x(),list.y(),list.right(),list.bottom());
        for(int i=0;i<profiles.size();i++){Rect r=l.staffRow(i,scroll);StaffProfile p=profiles.get(i);boolean selected=p.id().equals(selectedStaffId),equipped=p.id().equals(ArcaneClientState.text("staff_id","none"));int accent=p.favoredSchool()==null?0xFFC9A568:ArcaneRenderUtil.schoolColor(p.favoredSchool());if(selected)g.fill(r.x(),r.y()+3,r.x()+2,r.bottom()-3,accent);ArcaneRenderUtil.diamond(g,r.x()+12,r.y()+14,selected?6:4,equipped?0xFFFFD47B:accent);g.text(font,Component.literal(fit(p.displayName(),r.w()-34)),r.x()+25,r.y()+5,selected?0xFFF0E1CA:0xFFB8AE9F);tiny(g,fit(staffStats(p),(r.w()-34)*2),r.x()+25,r.y()+18,0xFF786F65,.50F,false);rule(g,r.bottom(),r.x()+24,r.right()-4,0xFF3B342C);}
        g.disableScissor();drawStaffDetail(g,l.staffDetail(),selectedStaffId);
    }

    private void drawStaffDetail(GuiGraphicsExtractor g,Rect d,String id){
        if(d.w()<70||d.h()<42)return;g.enableScissor(d.x(),d.y(),d.right(),d.bottom());g.fill(d.x(),d.y()+2,d.x()+1,d.bottom()-2,0xFF554838);StaffProfile p=id.isBlank()?StaffProfile.NONE:ModItems.profile(id);int cx=d.x()+d.w()/2;boolean compact=d.h()<120;int iconR=compact?7:Math.max(9,Math.min(16,d.h()/8));ArcaneRenderUtil.diamond(g,cx,d.y()+iconR+4,iconR,p==StaffProfile.NONE?0xFF5D564D:0xFFD2AE70);g.centeredText(font,Component.literal(fit(p.displayName(),Math.max(40,d.w()-8))),cx,d.y()+iconR*2+8,0xFFEADCC7);int y=d.y()+iconR*2+21,remaining=Math.max(0,d.bottom()-y-4),summaryLines=compact?1:Math.max(1,Math.min(3,remaining/18));for(String line:wrap(p.summary(),d.w()-18,summaryLines)){tiny(g,line,d.x()+9,y,0xFFAFA496,.56F,false);y+=8;}if(y+7<d.bottom()){y+=3;int recipeLines=Math.max(1,Math.min(compact?2:4,(d.bottom()-y-2)/8));for(String line:wrap(p.recipeHint().isBlank()?"제작 정보 없음":"제작 · "+p.recipeHint(),d.w()-18,recipeLines)){tiny(g,line,d.x()+9,y,0xFFD8BD89,.54F,false);y+=8;}}g.disableScissor();
    }
''')
replace_once(grimoire,
'''    private void drawSpellDetail(GuiGraphicsExtractor g,Layout l,SpellDefinition s,int mouseX,int mouseY){
        Rect d=l.detail();if(d.w()<70||d.h()<56||s==null)return;
''',
'''    private void drawSpellDetail(GuiGraphicsExtractor g,Layout l,SpellDefinition s,int mouseX,int mouseY){
        Rect d=l.detail();if(d.w()<70||d.h()<56||s==null)return;
        if(l.stackedDetail()){drawCompactSpellDetail(g,l,d,s,mouseX,mouseY);return;}
''')
replace_once(grimoire,
'''    private void drawLoadout(GuiGraphicsExtractor g,Rect r,int slot,int mouseX,int mouseY){
''',
'''    private void drawCompactSpellDetail(GuiGraphicsExtractor g,Layout l,Rect d,SpellDefinition s,int mouseX,int mouseY){
        g.enableScissor(d.x(),d.y(),d.right(),d.bottom());
        int accent=ArcaneRenderUtil.schoolColor(s.school()),y=d.y()+4;Rect a=l.primaryAction();
        g.text(font,Component.literal(fit(s.name(),Math.max(40,d.w()-8))),d.x()+4,y,0xFFF0E4D1);
        tiny(g,s.circle()+"C · "+s.school().displayName()+" · "+s.sigilAnchor().displayName(),d.right()-4,y+2,accent,.48F,true);
        y+=14;
        int descLines=Math.max(1,Math.min(4,Math.max(1,(a.y()-y-13)/8)));
        for(String line:wrap(s.description(),d.w()-8,descLines)){tiny(g,line,d.x()+4,y,0xFFC7BAA7,.54F,false);y+=8;}
        if(y+8<a.y())tiny(g,"MP "+s.manaCost()+" · 쿨 "+one(s.cooldownTicks()/20.0)+"s · 범위 "+one(s.range()),d.x()+4,a.y()-10,0xFF9D917F,.50F,false);
        int empty=firstEmptySlot();boolean usable=usable(s);String label=activeSlot>=0?(activeSlot+1)+"번에 장착":empty>=0?"빈 슬롯에 장착":"슬롯 선택 필요";
        action(g,a,label,inside(mouseX,mouseY,a),usable&&(activeSlot>=0||empty>=0),accent);
        g.disableScissor();
    }

    private void drawLoadout(GuiGraphicsExtractor g,Rect r,int slot,int mouseX,int mouseY){
''')
replace_once(grimoire,
'''    private void normalizeSelections(){atlasCircle=clamp(atlasCircle,1,9);fusionCircle=clamp(fusionCircle,1,9);academyCircle=clamp(academyCircle,1,9);ensureInspectedSpell();}
''',
'''    private void normalizeSelections(){atlasCircle=clamp(atlasCircle,1,9);fusionCircle=clamp(fusionCircle,1,9);academyCircle=clamp(academyCircle,1,9);ensureInspectedSpell();if("staffs".equals(page)&&selectedStaffId.isBlank()&&!ModItems.profiles().isEmpty())selectedStaffId=ModItems.profiles().getFirst().id();}
''')
replace_once(grimoire,
'''    private int maxScroll(Layout l){return switch(page){case "atlas"->{int count=SpellCatalog.spellsInCircle(atlasCircle).size();yield l.maxTileScroll(count);}case "recipes"->Math.max(0,fusionsInCircle(fusionCircle).size()*43-l.viewport().h());case "staffs"->Math.max(0,ModItems.profiles().size()*31-l.viewport().h());case "academy"->Math.max(0,AcademyOfferCatalog.forCircle(academyCircle).size()*31-l.academyOffers().h());case "quests"->Math.max(0,Math.min(3,ArcaneClientState.integer("quest_count",0))*48-l.viewport().h()+70);default->0;};}
''',
'''    private int maxScroll(Layout l){return switch(page){case "atlas"->{int count=SpellCatalog.spellsInCircle(atlasCircle).size();yield l.maxTileScroll(count);}case "recipes"->Math.max(0,fusionsInCircle(fusionCircle).size()*43-l.viewport().h());case "staffs"->Math.max(0,ModItems.profiles().size()*31-l.staffList().h());case "academy"->Math.max(0,AcademyOfferCatalog.forCircle(academyCircle).size()*31-l.academyOffers().h());case "quests"->Math.max(0,Math.min(3,ArcaneClientState.integer("quest_count",0))*48-l.viewport().h()+70);default->0;};}
''')
replace_once(grimoire,
'''        int detailWidth(){Rect b=body();int available=Math.max(0,b.w()-38);if(available<178)return 0;int preferred=Math.max(96,Math.min(205,b.w()/3)),minBrowser=66,maxDetail=Math.max(70,available-minBrowser-5);return Math.min(preferred,maxDetail);}
        Rect detail(){Rect b=body();int w=detailWidth(),y=contentTop(),h=Math.max(1,contentBottom()-y);return new Rect(w<=0?b.right():b.right()-w,y,w,h);}
        Rect browserViewport(){Rect b=body(),d=detail();int x=b.x()+38,y=contentTop(),right=d.w()>0?d.x()-5:b.right();return new Rect(x,y,Math.max(1,right-x),Math.max(1,contentBottom()-y));}
''',
'''        int detailWidth(){Rect b=body();int available=Math.max(0,b.w()-38);if(available<178)return 0;int preferred=Math.max(96,Math.min(205,b.w()/3)),minBrowser=66,maxDetail=Math.max(70,available-minBrowser-5);return Math.min(preferred,maxDetail);}
        boolean stackedDetail(){Rect b=body();return detailWidth()==0&&b.w()>=130&&contentBottom()-contentTop()>=118;}
        Rect detail(){Rect b=body();int w=detailWidth(),y=contentTop(),h=Math.max(1,contentBottom()-y);if(stackedDetail()){int sh=Math.max(72,Math.min(112,h/2));return new Rect(b.x()+38,contentBottom()-sh,Math.max(70,b.w()-40),sh);}return new Rect(w<=0?b.right():b.right()-w,y,w,h);}
        Rect browserViewport(){Rect b=body(),d=detail();int x=b.x()+38,y=contentTop();if(stackedDetail())return new Rect(x,y,Math.max(1,b.right()-x),Math.max(1,d.y()-5-y));int right=d.w()>0?d.x()-5:b.right();return new Rect(x,y,Math.max(1,right-x),Math.max(1,contentBottom()-y));}
        Rect staffList(){Rect v=viewport();if(isWide())return new Rect(v.x(),v.y(),Math.max(150,v.w()/2-4),v.h());int h=Math.max(62,Math.min(v.h()-46,(int)Math.round(v.h()*.52)));return new Rect(v.x(),v.y(),v.w(),h);}
        Rect staffDetail(){Rect v=viewport(),s=staffList();if(isWide())return new Rect(s.right()+6,v.y(),Math.max(70,v.right()-s.right()-6),v.h());int y=s.bottom()+4;return new Rect(v.x(),y,v.w(),Math.max(42,v.bottom()-y));}
        Rect staffRow(int i,int scroll){Rect v=staffList();return new Rect(v.x(),v.y()+i*31-scroll,v.w(),29);}
''')

field = MAGIC / "ArcaneFieldService.java"
replace_once(field, "public static final int TIME_STOP_TICKS = 120;", "public static final int TIME_STOP_TICKS = 160;")
replace_once(field, "public static final int ANTIMAGIC_TICKS = 240;", "public static final int ANTIMAGIC_TICKS = 320;")
replace_once(field, "double radius = Math.max(9.0, Math.min(18.0, range * .75));",
             "double radius = Math.max(12.0, Math.min(24.0, range * .85));")
replace_once(field, '''        ArcaneNoticeService.push(player, Component.literal("§5[반마법장] §f12.0초 · 반경 "
                + one(radius) + " · 상태효과와 Arcane 시전을 지속 억제"), 100);
''',
'''        ArcaneNoticeService.push(player, Component.literal("§5[반마법장] §f" + one(ANTIMAGIC_TICKS / 20.0) + "초 · 반경 "
                + one(radius) + " · 상태효과와 Arcane 시전을 지속 억제"), 100);
''')
replace_once(field, "double radius = Math.max(16.0, Math.min(40.0, range * .65));",
             "double radius = Math.max(20.0, Math.min(48.0, range * .75));")
replace_once(field, '''        ArcaneNoticeService.push(player, Component.literal("§b[시간 정지] §f6.0초 · 반경 "
                + one(radius) + " · 비아군의 AI·이동·Arcane 시전 정지"), 100);
''',
'''        ArcaneNoticeService.push(player, Component.literal("§b[시간 정지] §f" + one(TIME_STOP_TICKS / 20.0) + "초 · 반경 "
                + one(radius) + " · 비아군의 AI·이동·Arcane 시전 정지"), 100);
''')

gameplay = MAGIC / "SpellGameplayService.java"
replace_once(gameplay, 'case "forcecage" -> controlSingle(player, spellId, power, snapshot, 300);',
             'case "forcecage" -> controlSingle(player, spellId, power, snapshot, 400);')
replace_once(gameplay, 'case "dominate_monster" -> controlSingle(player, spellId, power, snapshot, 360);',
             'case "dominate_monster" -> controlSingle(player, spellId, power, snapshot, 480);')
replace_once(gameplay, 'case "maze" -> controlSingle(player, spellId, power, snapshot, 240);',
             'case "maze" -> controlSingle(player, spellId, power, snapshot, 360);')
replace_once(gameplay, 'case "true_polymorph" -> controlSingle(player, spellId, power, snapshot, 300);',
             'case "true_polymorph" -> controlSingle(player, spellId, power, snapshot, 480);')
replace_once(gameplay, 'case "prismatic_wall" -> 280;', 'case "prismatic_wall" -> 400;')
replace_once(gameplay, 'case "control_weather" -> 400;', 'case "control_weather" -> 600;')
replace_once(gameplay, 'case "hold_monster", "forcecage", "true_polymorph" -> 300;',
'''case "hold_monster" -> 300;
            case "forcecage" -> 400;
            case "true_polymorph" -> 480;''')
replace_once(gameplay, 'case "flesh_to_stone", "dominate_monster" -> 360;',
'''case "flesh_to_stone" -> 360;
            case "dominate_monster" -> 480;''')
replace_once(gameplay, 'case "maze" -> 240;', 'case "maze" -> 360;')
replace_once(gameplay,
'''            case "move_earth" -> Math.max(8.0, range * .34); case "circle_of_death", "delayed_blast_fireball" -> Math.max(10.0, range * .30);
            case "fire_storm" -> Math.max(11.0, range * .30); case "reverse_gravity" -> Math.max(10.0, range * .32);
            case "earthquake" -> Math.max(14.0, range * .34); case "sunburst", "weird" -> Math.max(14.0, range * .32); default -> 7.0;
''',
'''            case "move_earth" -> Math.max(8.0, range * .34); case "circle_of_death" -> Math.max(10.0, range * .30);
            case "delayed_blast_fireball" -> Math.max(13.0, range * .38);
            case "fire_storm" -> Math.max(14.0, range * .38); case "reverse_gravity" -> Math.max(13.0, range * .40);
            case "earthquake" -> Math.max(18.0, range * .42); case "sunburst" -> Math.max(18.0, range * .42);
            case "weird" -> Math.max(18.0, range * .40); default -> 7.0;
''')
replace_once(gameplay,
'''                    ArcaneDamage.hurt(level, player, target, (float) (power * .58)); target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 180));
''',
'''                    ArcaneDamage.hurt(level, player, target, (float) (power * .72)); target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 260));
''')
replace_once(gameplay,
'''                case "flame_strike", "delayed_blast_fireball" -> { ArcaneDamage.hurt(level, player, target, (float) power); target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 260)); }
                case "move_earth", "earthquake" -> { ArcaneDamage.hurt(level, player, target, (float) power); Vec3 away = flat(target.position().subtract(center)); double force = "earthquake".equals(id) ? 1.9 : 1.2; target.push(away.x * force, "earthquake".equals(id) ? 1.7 : 1.0, away.z * force); }
                case "circle_of_death" -> ArcaneDamage.hurt(level, player, target, (float) power);
                case "reverse_gravity" -> { ArcaneDamage.hurt(level, player, target, (float) (power * .45)); target.push(0.0, 3.2 + power / 100.0, 0.0); target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 160, 5)); }
                case "sunburst" -> { ArcaneDamage.hurt(level, player, target, (float) power); target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 160)); target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 180, 2)); }
                case "weird" -> { ArcaneDamage.hurt(level, player, target, (float) power); target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 500, 4)); target.addEffect(new MobEffectInstance(MobEffects.WITHER, 500, 5)); target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 500, 5)); }
''',
'''                case "flame_strike" -> { ArcaneDamage.hurt(level, player, target, (float) power); target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 260)); }
                case "delayed_blast_fireball" -> { ArcaneDamage.hurt(level, player, target, (float) (power * 1.20)); target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 360)); }
                case "move_earth", "earthquake" -> { double scale="earthquake".equals(id)?1.15:1.0; ArcaneDamage.hurt(level, player, target, (float) (power*scale)); Vec3 away = flat(target.position().subtract(center)); double force = "earthquake".equals(id) ? 2.6 : 1.2; target.push(away.x * force, "earthquake".equals(id) ? 2.1 : 1.0, away.z * force); }
                case "circle_of_death" -> ArcaneDamage.hurt(level, player, target, (float) power);
                case "reverse_gravity" -> { ArcaneDamage.hurt(level, player, target, (float) (power * .65)); target.push(0.0, 4.0 + power / 90.0, 0.0); target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 220, 7)); }
                case "sunburst" -> { ArcaneDamage.hurt(level, player, target, (float) (power * 1.15)); target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 240)); target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 260, 3)); }
                case "weird" -> { ArcaneDamage.hurt(level, player, target, (float) (power * 1.25)); target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 700, 5)); target.addEffect(new MobEffectInstance(MobEffects.WITHER, 700, 6)); target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 700, 6)); }
''')
replace_once(gameplay,
'''    private static boolean controlWeather(ServerPlayer player, double range, double power) { ServerLevel level = (ServerLevel) player.level(); int duration = 400; setWeather(level, true, duration); WEATHER.put(player.getUUID(), new WeatherState(level, player.getUUID(), Math.max(20.0, range * .60), power, level.getGameTime() + duration, level.getGameTime())); ArcaneNoticeService.push(player, Component.literal("§9[기후 조종] §f20초간 실제 폭우·뇌우를 일으키고 주변 적을 주기적으로 타격합니다."), 80); return true; }
''',
'''    private static boolean controlWeather(ServerPlayer player, double range, double power) { ServerLevel level = (ServerLevel) player.level(); int duration = 600; setWeather(level, true, duration); WEATHER.put(player.getUUID(), new WeatherState(level, player.getUUID(), Math.max(28.0, range * .72), power, level.getGameTime() + duration, level.getGameTime())); ArcaneNoticeService.push(player, Component.literal("§9[기후 조종] §f30초간 실제 폭우·뇌우를 지배하고 주변 적을 연속 낙뢰로 타격합니다."), 90); return true; }
''')
replace_once(gameplay,
'''    private static void tickWeather(ServerLevel level, long now) { boolean hadState = false; Iterator<Map.Entry<UUID, WeatherState>> iterator = WEATHER.entrySet().iterator(); while (iterator.hasNext()) { WeatherState state = iterator.next().getValue(); if (state.level() != level) continue; hadState = true; Entity raw = level.getEntity(state.ownerId()); if (!(raw instanceof ServerPlayer owner) || !owner.isAlive() || now >= state.expiresAt()) { iterator.remove(); continue; } if (now < state.nextPulse()) continue; WEATHER.put(state.ownerId(), new WeatherState(level, state.ownerId(), state.radius(), state.power(), state.expiresAt(), now + 20)); List<LivingEntity> targets = enemies(owner, owner.position(), state.radius(), state.radius() * .70); int strikes = Math.min(4, targets.size()); for (int i = 0; i < strikes; i++) { LivingEntity target = targets.get(Math.floorMod(i * 7 + (int) now, targets.size())); ArcaneDamage.hurt(level, owner, target, (float) (state.power() * .18)); target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 35, 2)); level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, .65F, 1.25F + i * .06F); } } boolean activeNow = WEATHER.values().stream().anyMatch(state -> state.level() == level && state.active()); if (hadState && !activeNow) setWeather(level, false, 100); }
''',
'''    private static void tickWeather(ServerLevel level, long now) { boolean hadState = false; Iterator<Map.Entry<UUID, WeatherState>> iterator = WEATHER.entrySet().iterator(); while (iterator.hasNext()) { WeatherState state = iterator.next().getValue(); if (state.level() != level) continue; hadState = true; Entity raw = level.getEntity(state.ownerId()); if (!(raw instanceof ServerPlayer owner) || !owner.isAlive() || now >= state.expiresAt()) { iterator.remove(); continue; } if (now < state.nextPulse()) continue; WEATHER.put(state.ownerId(), new WeatherState(level, state.ownerId(), state.radius(), state.power(), state.expiresAt(), now + 15)); List<LivingEntity> targets = enemies(owner, owner.position(), state.radius(), state.radius() * .78); int strikes = Math.min(6, targets.size()); for (int i = 0; i < strikes; i++) { LivingEntity target = targets.get(Math.floorMod(i * 7 + (int) now, targets.size())); ArcaneDamage.hurt(level, owner, target, (float) (state.power() * .24)); target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, 3)); level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, .72F, 1.18F + i * .055F); } } boolean activeNow = WEATHER.values().stream().anyMatch(state -> state.level() == level && state.active()); if (hadState && !activeNow) setWeather(level, false, 100); }
''')

buff = MAGIC / "ArcaneBuffRuntime.java"
replace_once(buff, 'state.nextChargeAt = now + 60;', 'state.nextChargeAt = now + 40;')
replace_once(buff, '''            case "shapechange" -> {
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, 3, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 2, true, false));
                notice(player, "§d[셰이프체인지] §f90초 동안 변이 육체가 피해를 흘리고 자체 재생합니다.");
            }
            case "foresight" -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, true, false));
                state.nextChargeAt = now;
                notice(player, "§e[포사이트] §f120초 동안 3초마다 다음 치명 궤적 하나를 미리 읽어 완전히 회피합니다.");
            }
''', '''            case "shapechange" -> {
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, 5, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 3, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, duration, 3, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 2, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 10, true, false));
                notice(player, "§d[셰이프체인지] §f90초 동안 초월 육체가 절반의 피해를 흘리고 강하게 재생·가속합니다.");
            }
            case "foresight" -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 3, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 1, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, duration, 4, true, false));
                state.nextChargeAt = now;
                notice(player, "§e[포사이트] §f120초 동안 2초마다 다음 궤적을 완전 회피하고, 틈의 피해도 예지로 줄입니다.");
            }
''')
replace_once(buff, 'if (shape != null) multiplier = Math.min(multiplier, .65);', '''if (shape != null) multiplier = Math.min(multiplier, .50);
        if (foresight != null) multiplier = Math.min(multiplier, .75);''')
replace_once(buff, '''            if ("shapechange".equals(state.spellId) && now % 20L == 0L)
                player.heal((float) Math.max(.45, .25 + state.power * .004));
''', '''            if ("shapechange".equals(state.spellId) && now % 20L == 0L)
                player.heal((float) Math.min(5.0, Math.max(1.0, .60 + state.power * .006)));
''')

high = MAGIC / "HighCircleSpellEffects.java"
replace_once(high, '(float) (power * 1.45)', '(float) (power * 1.75)')
replace_once(high, 'new MobEffectInstance(MobEffects.WITHER, 300, 4)', 'new MobEffectInstance(MobEffects.WITHER, 420, 5)')
replace_once(high, 'double threshold = Math.max(60.0, power * 0.85);', 'double threshold = Math.max(90.0, power * 1.05);')
replace_once(high, ': (float) (power * 0.65);', ': (float) (power * 0.90);')

summary = MAGIC / "SpellEffectSummary.java"
summary_replacements = {
    'case "delayed_blast_fireball" -> "지연 후 고정 목표 대형 화염 폭발 · 화상·강한 지형 파괴";':'case "delayed_blast_fireball" -> "지연 후 초대형 화염 폭발 · 증폭 피해·장기 화상·강한 지형 파괴";',
    'case "fire_storm" -> "고정 목표 주변 7지점 화염 폭격 · 화상·지형 파괴";':'case "fire_storm" -> "대범위 7지점 연쇄 화염 폭격 · 증폭 피해·장기 화상·지형 파괴";',
    'case "forcecage" -> "조준 대상을 15초간 AI·이동·Arcane 시전 완전 봉쇄";':'case "forcecage" -> "조준 대상을 20초간 AI·이동·Arcane 시전 완전 봉쇄";',
    'case "reverse_gravity" -> "고정 목표 범위 피해 · 강한 상승/공중 부양";':'case "reverse_gravity" -> "대범위 피해 · 11초간 초강제 상승/공중 부양";',
    'case "antimagic_field" -> "12초 이동형 반마법장 · 버프/지속마법 제거 + Arcane 시전 차단";':'case "antimagic_field" -> "16초 대형 이동 반마법장 · 버프/지속마법 제거 + Arcane 시전 완전 차단";',
    'case "control_weather" -> "20초 실제 폭우·뇌우 · 주변 적 주기적 번개 피해/둔화";':'case "control_weather" -> "30초 실제 폭우·뇌우 지배 · 최대 6대상 연속 낙뢰 피해/강한 둔화";',
    'case "dominate_monster" -> "조준 대상 AI·이동·Arcane 시전을 18초간 완전 봉쇄";':'case "dominate_monster" -> "조준 대상 AI·이동·Arcane 시전을 24초간 완전 봉쇄";',
    'case "earthquake" -> "고정 목표 지면 대규모 피해·띄우기 · 강한 실제 지형 파괴";':'case "earthquake" -> "초대형 지면 피해·강제 띄우기 · 8써클급 광역 실제 지형 붕괴";',
    'case "maze" -> "조준 대상을 12초간 완전 격리 상태 + 실명·혼란";':'case "maze" -> "조준 대상을 18초간 완전 격리 상태 + 실명·혼란";',
    'case "sunburst" -> "고정 목표 초대형 태양광 폭발 · 광역 피해·화상·실명";':'case "sunburst" -> "초대형 태양광 폭발 · 증폭 광역 피해·장기 화상·강한 실명";',
    'case "power_word_kill" -> "약해진 대상 즉사급 처형 · 그 외에는 대형 단일 피해";':'case "power_word_kill" -> "높은 처형 임계치 이하 대상 즉사급 명령 · 실패해도 9써클 대형 단일 피해";',
    'case "prismatic_wall" -> "14초 지속 7색 장벽 · 수명 90%까지 선명 유지 · 반복 피해·상태이상·통과 저지";':'case "prismatic_wall" -> "20초 지속 7색 장벽 · 반복 피해·상태이상·강제 통과 저지";',
    'case "shapechange" -> "90초 변이 육체 · 35%급 피해 경감 + 자체 재생 + 전투 신체 강화";':'case "shapechange" -> "90초 초월 육체 · 피해 50% 경감 + 강한 자체 재생·근력·속도·도약 강화";',
    'case "time_stop" -> "6초 고정 시간장 · 주변 비아군 AI·이동·Arcane 시전 정지";':'case "time_stop" -> "8초 초대형 고정 시간장 · 주변 비아군 AI·이동·Arcane 시전 완전 정지";',
    'case "true_polymorph" -> "대상 피해 · 15초 축소 변형 + AI·이동·Arcane 시전 봉쇄";':'case "true_polymorph" -> "대상 피해 · 24초 축소 변형 + AI·이동·Arcane 시전 완전 봉쇄";',
    'case "weird" -> "고정 목표 대형 정신 피해 · 실명·위더·둔화";':'case "weird" -> "초대형 정신 붕괴 · 35초 증폭 피해·실명·고단계 위더·둔화";',
    'case "foresight" -> "120초 예지 · 3초마다 다음 피격 1회 완전 회피 + 예지 시야";':'case "foresight" -> "120초 예지 · 2초마다 다음 피격 완전 회피 + 사이 피해 25% 경감 + 예지 시야";',
}
for old,new in summary_replacements.items(): replace_once(summary, old, new)

recipes = {
"ember_staff":{"type":"minecraft:crafting_shaped","category":"equipment","pattern":[" B "," M "," N "],"key":{"B":"minecraft:blaze_rod","M":"minecraft:magma_cream","N":"arcanecircle:novice_staff"},"result":{"count":1,"id":"arcanecircle:ember_staff"}},
"glacial_staff":{"type":"minecraft:crafting_shaped","category":"equipment","pattern":[" I "," A "," N "],"key":{"I":"minecraft:blue_ice","A":"minecraft:amethyst_shard","N":"arcanecircle:novice_staff"},"result":{"count":1,"id":"arcanecircle:glacial_staff"}},
"zephyr_staff":{"type":"minecraft:crafting_shaped","category":"equipment","pattern":[" P "," B "," N "],"key":{"P":"minecraft:phantom_membrane","B":"minecraft:breeze_rod","N":"arcanecircle:novice_staff"},"result":{"count":1,"id":"arcanecircle:zephyr_staff"}},
"aegis_staff":{"type":"minecraft:crafting_shaped","category":"equipment","pattern":[" G "," S "," N "],"key":{"G":"minecraft:gold_block","S":"minecraft:shield","N":"arcanecircle:novice_staff"},"result":{"count":1,"id":"arcanecircle:aegis_staff"}},
"verdant_staff":{"type":"minecraft:crafting_shaped","category":"equipment","pattern":[" E "," A "," N "],"key":{"E":"minecraft:emerald_block","A":"minecraft:golden_apple","N":"arcanecircle:novice_staff"},"result":{"count":1,"id":"arcanecircle:verdant_staff"}},
"rift_staff":{"type":"minecraft:crafting_shaped","category":"equipment","pattern":[" E ","CH "," N "],"key":{"E":"minecraft:ender_eye","C":"minecraft:crying_obsidian","H":"minecraft:echo_shard","N":"arcanecircle:novice_staff"},"result":{"count":1,"id":"arcanecircle:rift_staff"}},
"sage_staff":{"type":"minecraft:crafting_shaped","category":"equipment","pattern":[" E ","GD "," R "],"key":{"E":"minecraft:enchanted_book","G":"minecraft:gold_block","D":"minecraft:diamond_block","R":"arcanecircle:rift_staff"},"result":{"count":1,"id":"arcanecircle:sage_staff"}},
"archmage_staff":{"type":"minecraft:crafting_shaped","category":"equipment","pattern":[" N ","EH "," S "],"key":{"N":"minecraft:nether_star","E":"minecraft:echo_shard","H":"minecraft:netherite_block","S":"arcanecircle:sage_staff"},"result":{"count":1,"id":"arcanecircle:archmage_staff"}},
}
recipe_dir = RES / "recipe"; recipe_dir.mkdir(parents=True, exist_ok=True)
for name,data in recipes.items(): write(recipe_dir/f"{name}.json",json.dumps(data,ensure_ascii=False,indent=2)+"\n")

audit = ROOT / "tools/test_current_source.py"
replace_all_checked(audit, "0.12.1-alpha.45", "0.12.1-alpha.46")
replace_all_checked(audit, "TIME_STOP_TICKS = 120", "TIME_STOP_TICKS = 160")
replace_all_checked(audit, "ANTIMAGIC_TICKS = 240", "ANTIMAGIC_TICKS = 320")
replace_all_checked(audit, 'case "prismatic_wall" -> 280', 'case "prismatic_wall" -> 400')
replace_all_checked(audit, 'case "control_weather" -> 400', 'case "control_weather" -> 600')
replace_all_checked(audit, '14초 지속 7색 장벽', '20초 지속 7색 장벽')
with audit.open("a",encoding="utf-8") as f:
    f.write(r'''

# Alpha.46 maintained-regalia, high-circle authority and recipe/UI visibility pass.
regalia_path=client/'PersistentBuffRegalia.java'; assert regalia_path.exists(); regalia=text(regalia_path)
for token in ['MAINTAINED = Set.of','"fly"','"etherealness"','"shapechange"','"foresight"','featherWings','armorMantle','speedFins','sightCrown','shapechangeMantle','reserveBody']: assert token in regalia, token
for token in ['PersistentBuffRegalia.handles(v.spell)','castingAfterglow=!regalia||elapsedSeconds<.85','PersistentBuffRegalia.release','maintainedDirection','if(PersistentBuffRegalia.handles(spell))return true;']: assert token in tracker, token
assert 'if(l.stackedDetail()){drawCompactSpellDetail' in grimoire
for token in ['stackedDetail()','drawCompactSpellDetail','staffList()','staffDetail()','staffRow(i,scroll)','drawStaffDetail(g,l.staffDetail(),selectedStaffId)']: assert token in grimoire, token
recipe_dir=root/'src/main/resources/data/arcanecircle/recipe'; recipe_files=sorted(recipe_dir.glob('*_staff.json')); assert len(recipe_files)==8,[p.name for p in recipe_files]
for p in recipe_files:
    value=text(p); assert '"type": "minecraft:crafting_shaped"' in value,p; assert '"category": "equipment"' in value,p; assert '"result": {' in value and '"id": "arcanecircle:' in value,p
for token in ['TIME_STOP_TICKS = 160','ANTIMAGIC_TICKS = 320','Math.max(20.0, Math.min(48.0, range * .75))','Math.max(12.0, Math.min(24.0, range * .85))']: assert token in field,token
for token in ['case "forcecage" -> controlSingle(player, spellId, power, snapshot, 400)','case "dominate_monster" -> controlSingle(player, spellId, power, snapshot, 480)','case "maze" -> controlSingle(player, spellId, power, snapshot, 360)','case "true_polymorph" -> controlSingle(player, spellId, power, snapshot, 480)','int duration = 600','Math.min(6, targets.size())','state.power() * .24','case "prismatic_wall" -> 400','case "control_weather" -> 600']: assert token in gameplay,token
for token in ['state.nextChargeAt = now + 40','multiplier = Math.min(multiplier, .50)','multiplier = Math.min(multiplier, .75)','MobEffects.JUMP_BOOST','Math.min(5.0']: assert token in buff,token
for token in ['case "time_stop"','8초 초대형','case "antimagic_field"','16초 대형','case "shapechange"','피해 50% 경감','case "foresight"','2초마다','case "prismatic_wall"','20초 지속']: assert token in summary,token
print('alpha46_maintained_buff_regalia=PASS'); print('alpha46_responsive_effect_recipe_ui=PASS'); print('alpha46_staff_crafting_recipes=PASS'); print('alpha46_high_circle_rule_authority=PASS')
''')

for p in recipe_dir.glob("*.json"): json.loads(read(p))
json.loads(read(RES/"spell_catalog/index.json"))
if SELF.exists(): SELF.unlink()
subprocess.run(["git","config","user.name","github-actions[bot]"],check=True)
subprocess.run(["git","config","user.email","41898282+github-actions[bot]@users.noreply.github.com"],check=True)
subprocess.run(["git","add","-A","projects/arcane-circle",str(SELF)],check=True)
subprocess.run(["git","diff","--cached","--check"],check=True)
subprocess.run(["git","commit","-m","feat(arcane-circle): ship alpha.46 regalia and high-circle authority"],check=True)
for attempt in range(1,7):
    pushed=subprocess.run(["git","push","origin","HEAD:main"])
    if pushed.returncode==0: sys.exit(0)
    subprocess.run(["git","fetch","origin","main"],check=True); subprocess.run(["git","rebase","origin/main"],check=True); time.sleep(attempt*2)
raise SystemExit("failed to push alpha.46 migration")

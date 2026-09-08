from pathlib import Path

path = Path('.github/frontier_alpha129_worker_runtime_patch.py')
text = path.read_text(encoding='utf-8')
text = text.replace(
    "    private static final int QUARRY_SEARCH_RADIUS = 40;\n    private static final int MANAGED_QUARRY_MAX_OVERBURDEN = 12;",
    "    private static final int QUARRY_SEARCH_RADIUS = 40;\n    private static final int QUARRY_SEARCH_DOWN = 16;\n    private static final int QUARRY_SEARCH_UP = 12;\n    private static final int MANAGED_QUARRY_MAX_OVERBURDEN = 12;",
    1,
)
start = text.find("# Drop now-unused exhaustive exposed-stone method if it survived before managed method.")
if start < 0:
    raise SystemExit('remove-method marker not found')
next_block = text.find("worker = sub_once(worker,\n    r'    private static BlockPos findManagedQuarryStone", start)
if next_block < 0:
    raise SystemExit('managed-quarry replacement marker not found')
text = text[:start] + text[next_block:]
needle = "test = replace_once(test, 'mod_version=0.1.0-alpha.128', 'mod_version=0.1.0-alpha.129', 'test version')\n"
if needle not in text:
    raise SystemExit('test-version marker not found')
text = text.replace(
    needle,
    needle + "test = test.replace('\\\"광맥 고갈\\\" in worker', '\\\"광맥 탐색 중\\\" in worker')\n",
    1,
)
path.write_text(text, encoding='utf-8')
print('Alpha.129 patcher matching/audit fixed')

#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
A81 = ROOT / 'tools/test_alpha81_source.py'
_real_read = Path.read_text


def legacy_read(self, *args, **kwargs):
    s = _real_read(self, *args, **kwargs)
    if self.name == 'gradle.properties':
        s = s.replace('mod_version=0.1.0-alpha.82', 'mod_version=0.1.0-alpha.81')
        s = s.replace(', plus Alpha.82 collision-safe pack key profile with M as the Frontier settlement menu and default-only Xaero quick-waypoint B normalization that preserves user-customized controls.', '.')
    elif self.name == 'BuildingPlacementClient.java':
        s = s.replace('GLFW.GLFW_KEY_M, CATEGORY', 'GLFW.GLFW_KEY_B, CATEGORY')
    elif self.name == 'SettlementHudOverlay.java':
        s = s.replace('M을 눌러 시작', 'B를 눌러 시작')
        s = s.replace('M 메뉴', 'B 팔레트')
    elif self.name == 'SettlementGuideScreen.java':
        s = s.replace('M →', 'B →')
    elif self.name == 'BuildingPaletteScreen.java':
        s = s.replace('M 닫기/다시 열기', 'B 닫기/다시 열기')
    elif self.name == 'FrontierSettlementClient.java':
        s = s.replace('        NeoForge.EVENT_BUS.addListener(CompanionKeyProfile::tick);\n', '')
        s = s.replace('        CompanionKeyProfile.resetSession();\n', '')
    elif self.name == 'test_alpha72_source.py':
        s = s.replace('len(java_files)!=105', 'len(java_files)!=109')
        s = s.replace('expected 105 Java files', 'expected 109 Java files')
    elif self.name == 'test_alpha73_source.py':
        s = s.replace("rglob('*.java')))!=105", "rglob('*.java')))!=109")
    elif self.name == 'test_alpha74_source.py':
        s = s.replace("rglob('*.java')))!=106", "rglob('*.java')))!=110")
    return s


Path.read_text = legacy_read
try:
    chain = _real_read(A81, encoding='utf-8').replace(
        "print('Frontier Settlement alpha.23-81 cumulative source audit: PASS')", 'pass')
    ns = {'__file__': str(A81), '__name__': '__main__'}
    exec(compile(chain, str(A81), 'exec'), ns, ns)
finally:
    Path.read_text = _real_read


def text(path): return Path(path).read_text(encoding='utf-8')
def must(src, tokens, label):
    for token in tokens:
        if token not in src:
            raise SystemExit(f'{label} missing: {token}')
def forbid(src, tokens, label):
    for token in tokens:
        if token in src:
            raise SystemExit(f'{label} forbidden: {token}')

props = text(ROOT / 'gradle.properties')
placement = text(ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/client/BuildingPlacementClient.java')
profile = text(ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/client/CompanionKeyProfile.java')
client = text(ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/client/FrontierSettlementClient.java')
hud = text(ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/client/SettlementHudOverlay.java')
guide = text(ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/client/SettlementGuideScreen.java')
palette = text(ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/client/BuildingPaletteScreen.java')
ko = text(ROOT / 'src/main/resources/assets/frontier_settlement/lang/ko_kr.json')

must(props, ('mod_version=0.1.0-alpha.82', 'Alpha.82 collision-safe pack key profile', 'preserves user-customized controls'), 'alpha.82 props')
must(placement, ('"key.frontier_settlement.build_mode", GLFW.GLFW_KEY_M, CATEGORY', 'GLFW.GLFW_KEY_R', 'GLFW.GLFW_KEY_ENTER', 'GLFW.GLFW_KEY_BACKSPACE'), 'alpha.82 Frontier keys')
forbid(placement, ('"key.frontier_settlement.build_mode", GLFW.GLFW_KEY_B, CATEGORY',), 'alpha.82 Frontier must release B')
must(profile, (
    'if (!mapping.isDefault()) continue;',
    'mapping.getName().toLowerCase(Locale.ROOT)',
    'name.contains("xaero")',
    'mapping.getKey().getValue() != GLFW.GLFW_KEY_B',
    'minecraft.options.setKey(mapping, InputConstants.UNKNOWN)',
    'KeyMapping.resetMapping()',
    'minecraft.options.save()',
), 'alpha.82 companion key profile')
must(client, ('NeoForge.EVENT_BUS.addListener(CompanionKeyProfile::tick);', 'CompanionKeyProfile.resetSession();'), 'alpha.82 key profile lifecycle')
must(hud, ('공동 개척지 없음 · M을 눌러 시작', 'M 메뉴   R 회전   Enter 건설'), 'alpha.82 HUD controls')
must(guide, ('M → ‘현재 위치에 개척지 세우기’', 'M → 건물 선택', 'M → 도로 계획'), 'alpha.82 guide controls')
must(palette, ('M 닫기/다시 열기 · 배치 후 R 회전 / Enter 확정',), 'alpha.82 palette controls')
must(ko, ('"key.frontier_settlement.build_mode": "마을 메뉴 열기"', '"key.frontier_settlement.rotate_building": "배치 중 건물 90도 회전"'), 'alpha.82 key translations')

print('Frontier Settlement alpha.23-82 cumulative source audit: PASS')

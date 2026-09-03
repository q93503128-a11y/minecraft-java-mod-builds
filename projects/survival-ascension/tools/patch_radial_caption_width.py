from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CLIENT = ROOT / "src/main/java/kr/moonseungjun/survivalascension/client"

PATCHES = {
    "InfrastructureRadialMenuScreen.java": (
        'String caption="기능을 먼저 보고 선택하세요 · 비용은 싱글플레이 체급으로 조정됨";graphics.text(this.font,caption,cx-this.font.width(caption)/2,cy-102,0xFFE0E0E0,true);',
        'String caption=ellipsize("기능을 먼저 보고 선택하세요 · 비용은 싱글플레이 체급으로 조정됨",Math.min(420,Math.max(120,this.width-24)));graphics.text(this.font,caption,cx-this.font.width(caption)/2,cy-102,0xFFE0E0E0,true);'
    ),
    "ProductionRadialMenuScreen.java": (
        'String caption="등록 물류 통은 같은 차원 로딩 중이면 원격 사용 · 등록 통 파괴 시 내용물째 포장 이동";graphics.text(this.font,caption,cx-this.font.width(caption)/2,cy-102,0xFFE0E0E0,true);',
        'String caption=ellipsize("등록 물류 통은 같은 차원 로딩 중이면 원격 사용 · 등록 통 파괴 시 내용물째 포장 이동",Math.min(420,Math.max(120,this.width-24)));graphics.text(this.font,caption,cx-this.font.width(caption)/2,cy-102,0xFFE0E0E0,true);'
    ),
}

for name, (old, new) in PATCHES.items():
    path = CLIENT / name
    text = path.read_text(encoding="utf-8")
    if new not in text:
        if text.count(old) != 1:
            raise SystemExit(f"{name}: caption anchor count={text.count(old)}")
        text = text.replace(old, new, 1)
    if "private String ellipsize" not in text:
        raise SystemExit(f"{name}: shared ellipsize helper missing")
    path.write_text(text, encoding="utf-8")

print("RADIAL CAPTION WIDTH PATCH PASS")

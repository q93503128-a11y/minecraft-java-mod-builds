from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CLIENT = ROOT / "src/main/java/kr/moonseungjun/survivalascension/client"

HELPER = '''    private void renderDetailLines(GuiGraphicsExtractor graphics, String detail, int cx, int startY) {
        int maxWidth = Math.min(420, Math.max(120, this.width - 24));
        List<String> lines = wrapDetail(detail, maxWidth, 3);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            graphics.text(this.font, line, cx - this.font.width(line) / 2, startY + i * 10, 0xFFB8B8B8, false);
        }
    }

    private List<String> wrapDetail(String text, int maxWidth, int maxLines) {
        if (text == null || text.isBlank()) return List.of("");
        String remaining = text.trim();
        List<String> lines = new ArrayList<>();
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            int fit = fitPrefix(remaining, maxWidth);
            if (fit >= remaining.length()) {
                lines.add(remaining);
                remaining = "";
                break;
            }
            int preferred = remaining.lastIndexOf(" · ", fit);
            int split = preferred > 0 ? preferred : fit;
            if (split <= 0) split = Math.min(remaining.length(), Math.max(1, fit));
            String line = remaining.substring(0, split).trim();
            if (line.isEmpty()) {
                line = remaining.substring(0, Math.min(remaining.length(), Math.max(1, fit))).trim();
                split = Math.max(1, fit);
            }
            lines.add(line);
            remaining = remaining.substring(Math.min(split, remaining.length())).trim();
            if (remaining.startsWith("·")) remaining = remaining.substring(1).trim();
        }
        if (!remaining.isEmpty() && !lines.isEmpty()) {
            int last = lines.size() - 1;
            lines.set(last, ellipsize(lines.get(last) + " · " + remaining, maxWidth));
        }
        return lines.isEmpty() ? List.of(ellipsize(text, maxWidth)) : List.copyOf(lines);
    }

    private int fitPrefix(String text, int maxWidth) {
        if (text.isEmpty()) return 0;
        int low = 1;
        int high = text.length();
        int best = 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (this.font.width(text.substring(0, mid)) <= maxWidth) {
                best = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return best;
    }

    private String ellipsize(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) return text;
        String suffix = "…";
        int fit = fitPrefix(text, Math.max(1, maxWidth - this.font.width(suffix)));
        return text.substring(0, Math.max(1, fit)).trim() + suffix;
    }

'''


def patch(name: str) -> None:
    path = CLIENT / name
    text = path.read_text(encoding="utf-8")

    if "import java.util.ArrayList;" not in text:
        marker = "import org.joml.Matrix3x2f;\n"
        if marker not in text:
            raise SystemExit(f"{name}: import anchor missing")
        text = text.replace(marker, marker + "\nimport java.util.ArrayList;\nimport java.util.List;\n", 1)

    if name == "InfrastructureRadialMenuScreen.java":
        old = "Entry entry=ENTRIES[selected];String detail=detailFor(entry);graphics.text(this.font,entry.title(),cx-this.font.width(entry.title())/2,cy-5,0xFFFFFFFF,true);graphics.text(this.font,detail,cx-this.font.width(detail)/2,cy+8,0xFFB8B8B8,false);"
        new = "Entry entry=ENTRIES[selected];String detail=detailFor(entry);graphics.text(this.font,entry.title(),cx-this.font.width(entry.title())/2,cy-10,0xFFFFFFFF,true);renderDetailLines(graphics,detail,cx,cy+3);"
    else:
        old = "Entry entry=ENTRIES[selected];graphics.text(this.font,entry.title(),cx-this.font.width(entry.title())/2,cy-5,0xFFFFFFFF,true);graphics.text(this.font,entry.detail(),cx-this.font.width(entry.detail())/2,cy+8,0xFFB8B8B8,false);"
        new = "Entry entry=ENTRIES[selected];graphics.text(this.font,entry.title(),cx-this.font.width(entry.title())/2,cy-10,0xFFFFFFFF,true);renderDetailLines(graphics,entry.detail(),cx,cy+3);"

    if new not in text:
        if text.count(old) != 1:
            raise SystemExit(f"{name}: render anchor count={text.count(old)}")
        text = text.replace(old, new, 1)

    if "private void renderDetailLines" not in text:
        anchor = "    private enum Action"
        index = text.find(anchor)
        if index < 0:
            raise SystemExit(f"{name}: enum anchor missing")
        text = text[:index] + HELPER + text[index:]

    path.write_text(text, encoding="utf-8")


for file_name in ["InfrastructureRadialMenuScreen.java", "ProductionRadialMenuScreen.java"]:
    patch(file_name)
    current = (CLIENT / file_name).read_text(encoding="utf-8")
    required = [
        "renderDetailLines",
        "wrapDetail",
        "fitPrefix",
        "ellipsize",
        "this.width - 24",
        "List<String>",
    ]
    for token in required:
        if token not in current:
            raise SystemExit(f"{file_name}: missing {token}")

print("RADIAL DETAIL WRAP PATCH PASS")

package dev.moonseungjun.augmentationsurvival.augment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Player-facing presentation text derived from the canonical AUGMENTS.md bundled in the jar.
 * This keeps cards/tooltips in lockstep with the design canon instead of maintaining a second
 * hand-copied description table.
 */
public final class AugmentPresentationCatalog {
    private static final String RESOURCE = "data/augmentation_survival/augmentation/AUGMENTS.md";
    private static volatile Map<String, Entry> entries;

    private AugmentPresentationCatalog() { }

    public static void initialize() {
        Map<String, Entry> loaded = load();
        for (AugmentDefinition definition : AugmentCatalog.allDefinitions()) {
            if (definition.id().equals(AugmentCatalog.REROLL_ID)) continue;
            if (!loaded.containsKey(definition.canonicalNameKo())) {
                throw new IllegalStateException("Missing player-facing augment text for " + definition.canonicalNameKo());
            }
        }
        entries = Map.copyOf(loaded);
    }

    public static Entry get(AugmentDefinition definition) {
        if (definition.id().equals(AugmentCatalog.REROLL_ID)) {
            return new Entry(
                    "재추첨",
                    Kind.PASSIVE,
                    "코어 뽑기마다 후보 카드 하나를 1 / 2 / 3회 새 후보로 바꿀 수 있습니다.",
                    "Lv.1 / 2 / 3에서 코어 뽑기마다 재추첨 1 / 2 / 3회를 얻습니다. 한 번 사용할 때 선택한 카드 한 장만 같은 등급의 새 후보로 바뀌며 다른 두 카드는 유지됩니다. 현재 표시된 다른 후보와는 중복되지 않습니다."
            );
        }
        Map<String, Entry> current = entries;
        if (current == null) {
            initialize();
            current = entries;
        }
        Entry entry = current.get(definition.canonicalNameKo());
        if (entry != null) return playerFacingOverride(definition, entry);
        return new Entry(displayName(definition.canonicalNameKo()), Kind.PASSIVE,
                "효과 설명을 불러오지 못했습니다.", "효과 설명을 불러오지 못했습니다.");
    }

    /**
     * Small UX corrections for legacy canon rows whose old label no longer matches the live
     * interaction contract. Runtime authority still comes from the actual augment services.
     */
    private static Entry playerFacingOverride(AugmentDefinition definition, Entry source) {
        String id = definition.id();
        if (id.endsWith(":instant_mining")) {
            return new Entry(
                    "순간 채굴",
                    Kind.PASSIVE,
                    "보유 중, 현재 도구로 정상 수확 가능한 블록을 한 틱에 채굴합니다.",
                    "별도 조작 없이 항상 적용됩니다. 현재 도구로 정상 수확 가능한 직접 채굴 블록을 한 틱에 파괴하며, 도구 내구도·Silk Touch·Fortune은 정상 적용됩니다."
            );
        }
        if (id.endsWith(":pockets")) {
            return new Entry(
                    source.displayName(),
                    Kind.PASSIVE,
                    "획득 즉시 증강 저장소의 개인 슬롯이 +3 / +6 / +9칸 늘어납니다.",
                    "별도 아이템을 제작하지 않습니다. 획득 즉시 적용되며 B키로 증강 저장소를 열어 개인 주머니 슬롯을 사용합니다. Lv.1 / 2 / 3에서 +3 / +6 / +9칸이 열립니다."
            );
        }
        return source;
    }

    private static Map<String, Entry> load() {
        InputStream stream = AugmentPresentationCatalog.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (stream == null) throw new IllegalStateException("Missing bundled augment presentation canon: " + RESOURCE);
        LinkedHashMap<String, Entry> result = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("|") || !line.endsWith("|")) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length < 5) continue;
                String rawName = clean(parts[1]);
                String rawLevel = clean(parts[2]);
                if (rawName.equals("이름") || rawName.isBlank() || rawName.chars().allMatch(c -> c == '-' || c == ':')) continue;
                if (rawLevel.isBlank() || !rawLevel.chars().anyMatch(Character::isDigit)) continue;
                String effect = clean(parts[3]);
                if (effect.isBlank()) continue;
                Kind kind = rawName.contains("(액티브 · 토글)") ? Kind.TOGGLE
                        : rawName.contains("(액티브)") ? Kind.ACTIVE : Kind.PASSIVE;
                String detail = sanitizePlayerText(effect);
                String summary = summarize(detail);
                result.put(rawName, new Entry(displayName(rawName), kind, summary, detail));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load augment presentation canon", exception);
        }
        return result;
    }

    private static String clean(String value) {
        return value.trim()
                .replace("`", "")
                .replace("**", "")
                .replace("\\|", "|")
                .replace("<br>", " ")
                .replace("<br/>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String sanitizePlayerText(String value) {
        String replaced = value
                .replaceAll("augmentation_survival:[a-z0-9_./-]+", "지정된 대상")
                .replace("whitelist", "대상 목록");
        String[] sentences = replaced.split("(?<=\\.)\\s+");
        List<String> kept = new ArrayList<>();
        for (String sentence : sentences) {
            String lower = sentence.toLowerCase();
            if (lower.contains("root") || lower.contains("derived") || lower.contains("better combat")
                    || lower.contains("namespace") || lower.contains("재귀 차단") || lower.contains("내부 판정")
                    || lower.contains("서버 권한") || lower.contains("호환 시")) {
                continue;
            }
            kept.add(sentence.trim());
        }
        if (kept.isEmpty()) return replaced;
        return String.join(" ", kept).replaceAll("\\s+", " ").trim();
    }

    private static String summarize(String detail) {
        String[] sentences = detail.split("(?<=\\.)\\s+");
        StringBuilder result = new StringBuilder();
        for (String sentence : sentences) {
            if (sentence.isBlank()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(sentence.trim());
            if (result.length() >= 72 || result.length() + (sentences.length > 1 ? 1 : 0) >= 108) break;
            if (result.length() >= 48) break;
        }
        String summary = result.length() == 0 ? detail : result.toString();
        if (summary.length() > 128) summary = summary.substring(0, 125).stripTrailing() + "…";
        return summary;
    }

    private static String displayName(String canonical) {
        return canonical.replace(" (액티브 · 토글)", "").replace(" (액티브)", "");
    }

    public enum Kind {
        PASSIVE("패시브"), ACTIVE("액티브"), TOGGLE("토글");
        private final String label;
        Kind(String label) { this.label = label; }
        public String label() { return label; }
    }

    public record Entry(String displayName, Kind kind, String summary, String detail) { }
}

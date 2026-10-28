package dev.moonseungjun.augmentationsurvival.augment;

import dev.moonseungjun.augmentationsurvival.augment.combat.PlayerRescueAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.movement.FlightAugmentService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Player-facing presentation text derived from canonical design plus explicit live-runtime overrides. */
public final class AugmentPresentationCatalog {
    private static final String RESOURCE = "data/augmentation_survival/augmentation/AUGMENTS.md";
    private static volatile Map<String, Entry> entries;

    private AugmentPresentationCatalog() { }

    public static void initialize() {
        Map<String, Entry> loaded = load();
        for (AugmentDefinition definition : AugmentCatalog.allDefinitions()) {
            if (definition.id().equals(AugmentCatalog.REROLL_ID) || directOverride(definition) != null) continue;
            if (!loaded.containsKey(definition.canonicalNameKo())) {
                throw new IllegalStateException("Missing player-facing augment text for " + definition.canonicalNameKo());
            }
        }
        entries = Map.copyOf(loaded);
    }

    public static Entry get(AugmentDefinition definition) {
        if (definition.id().equals(AugmentCatalog.REROLL_ID)) {
            return new Entry("재추첨", Kind.PASSIVE,
                    "코어 뽑기마다 후보 카드 하나를 1 / 2 / 3회 새 후보로 바꿀 수 있습니다.",
                    "Lv.1 / 2 / 3에서 코어 뽑기마다 재추첨 1 / 2 / 3회를 얻습니다. 한 번 사용할 때 선택한 카드 한 장만 같은 등급의 새 후보로 바뀌며 다른 두 카드는 유지됩니다. 현재 표시된 다른 후보와는 중복되지 않습니다.");
        }
        Entry direct = directOverride(definition);
        if (direct != null) return direct;
        Map<String, Entry> current = entries;
        if (current == null) { initialize(); current = entries; }
        Entry entry = current.get(definition.canonicalNameKo());
        if (entry != null) return playerFacingOverride(definition, entry);
        return new Entry(displayName(definition.canonicalNameKo()), Kind.PASSIVE,
                "효과 설명을 불러오지 못했습니다.", "효과 설명을 불러오지 못했습니다.");
    }

    private static Entry directOverride(AugmentDefinition definition) {
        if (definition.id().equals(PlayerRescueAugmentService.RESCUE_ID)) {
            return new Entry("동료 구조", Kind.PASSIVE,
                    "둘 이상이 함께할 때, 치명상을 입은 당신이 잠시 쓰러져 구조를 기다립니다.",
                    "온라인 비관전자 플레이어가 2명 이상일 때만 후보에 등장합니다. 다른 사망 방지 효과가 모두 실패한 치명 피해를 받으면 10초 동안 쓰러집니다. 다른 플레이어가 가까이서 2초 구조하면 최대 체력 30%로 복귀하며, 구조되지 못하면 실제로 사망합니다. 쓰러짐 발동 후 15초 동안 다시 발동하지 않습니다.");
        }
        if (definition.id().endsWith(":instant_mining")) {
            return new Entry("순간 채굴", Kind.TOGGLE,
                    "전용 단축키로 켜고 끄며, 활성화 중 정상 수확 가능한 블록을 한 틱에 채굴합니다.",
                    "기본 N키로 켜고 끕니다. 키 설정에서 변경할 수 있습니다. 활성화 중 현재 도구로 정상 수확 가능한 직접 채굴 블록을 1 game tick에 파괴합니다. 파괴 불가 블록은 제외되며 도구 내구도·Silk Touch·Fortune은 정상 적용됩니다. 자동 파생 블록까지 1틱 채굴로 재귀 확장하지 않습니다.");
        }
        if (definition.id().equals(FlightAugmentService.FLIGHT_ID)) {
            return new Entry("비행", Kind.TOGGLE,
                    "시선 방향으로 가속해 호버·순항·초고속 비행을 오가는 전용 비행 능력입니다.",
                    "기본 V키로 켜고 끕니다. 키 설정에서 변경할 수 있습니다. Creative 비행이 아니라 시선 방향과 이동 입력에 따라 관성 있게 움직입니다. 전진하면서 질주를 유지하면 순항으로 가속하고, 계속 속도를 올리면 더 빠른 초고속 비행으로 전환됩니다.");
        }
        return null;
    }

    private static Entry playerFacingOverride(AugmentDefinition definition, Entry source) {
        String id = definition.id();
        if (id.endsWith(":dexterity")) return new Entry(source.displayName(), Kind.PASSIVE,
                "잎·꽃·대나무·사탕수수 등 일부 채집물을 캘 때 드롭 +1 확률 10% / 20% / 30%.",
                "잎·꽃·대나무·사탕수수·선인장·버섯·덩굴·켈프·해초·베리 덤불·코코아 등 일부 비전투 채집물을 직접 캘 때 10% / 20% / 30% 확률로 실제 드롭 중 1개를 추가로 얻습니다.");
        if (id.endsWith(":pockets")) return new Entry(source.displayName(), Kind.PASSIVE,
                "개인 저장공간 +3 / +6 / +9칸. 인벤토리 초과 아이템을 가능한 경우 자동 수납합니다.",
                "별도 아이템을 제작하지 않습니다. Lv.1 / 2 / 3에서 개인 주머니 슬롯 +3 / +6 / +9칸이 열리고, 일반 인벤토리에 들어가지 못한 아이템은 가능한 경우 주머니로 자동 수납됩니다. 기본 B키로 증강 저장소를 엽니다. 키 설정에서 변경할 수 있습니다.");
        if (id.endsWith(":remote_storage")) return new Entry(source.displayName(), source.kind(), source.summary(), append(source.detail(), "기본 B키로 원격 보관함을 엽니다. 키 설정에서 변경할 수 있습니다."));
        if (id.endsWith(":spatial_link")) return new Entry(source.displayName(), source.kind(), source.summary(), append(source.detail(), "기본 G키로 공간 연결 화면을 엽니다. 키 설정에서 변경할 수 있습니다."));
        if (id.endsWith(":authority_theft")) return new Entry(source.displayName(), source.kind(), source.summary(), append(source.detail(), "일반 액티브 슬롯을 사용하지 않습니다. 기본 J키로 탈취한 권능을 해방하며 키 설정에서 변경할 수 있습니다."));
        return source;
    }

    private static String append(String source, String suffix) { return source == null || source.isBlank() ? suffix : source.stripTrailing() + " " + suffix; }

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
                Kind kind = rawName.contains("(액티브 · 토글)") ? Kind.TOGGLE : rawName.contains("(액티브)") ? Kind.ACTIVE : Kind.PASSIVE;
                String detail = sanitizePlayerText(effect);
                result.put(rawName, new Entry(displayName(rawName), kind, summarize(detail), detail));
            }
        } catch (IOException exception) { throw new IllegalStateException("Failed to load augment presentation canon", exception); }
        return result;
    }

    private static String clean(String value) { return value.trim().replace("`", "").replace("**", "").replace("\\|", "|").replace("<br>", " ").replace("<br/>", " ").replaceAll("\\s+", " ").trim(); }
    private static String sanitizePlayerText(String value) {
        String replaced = value.replaceAll("augmentation_survival:[a-z0-9_./-]+", "지정된 대상").replace("whitelist", "대상 목록");
        String[] sentences = replaced.split("(?<=\\.)\\s+");
        List<String> kept = new ArrayList<>();
        for (String sentence : sentences) {
            String lower = sentence.toLowerCase();
            if (lower.contains("root") || lower.contains("derived") || lower.contains("better combat") || lower.contains("namespace") || lower.contains("재귀 차단") || lower.contains("내부 판정") || lower.contains("서버 권한") || lower.contains("호환 시")) continue;
            kept.add(sentence.trim());
        }
        return kept.isEmpty() ? replaced : String.join(" ", kept).replaceAll("\\s+", " ").trim();
    }
    private static String summarize(String detail) {
        String[] sentences = detail.split("(?<=\\.)\\s+"); StringBuilder result = new StringBuilder();
        for (String sentence : sentences) { String normalized = sentence.trim(); if (normalized.isBlank()) continue; if (result.length() > 0 && result.length() + 1 + normalized.length() > 104) break; if (result.length() > 0) result.append(' '); result.append(normalized); if (result.length() >= 58) break; }
        return result.length() == 0 ? detail.trim() : result.toString();
    }
    private static String displayName(String canonical) { return canonical.replace(" (액티브 · 토글)", "").replace(" (액티브)", ""); }
    public enum Kind { PASSIVE("패시브"), ACTIVE("액티브"), TOGGLE("토글"); private final String label; Kind(String label) { this.label = label; } public String label() { return label; } }
    public record Entry(String displayName, Kind kind, String summary, String detail) { }
}

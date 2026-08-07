package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class VillageRelicSystem {
    private static final String SEP = "\u001F";
    private static final Map<UUID, Integer> OWNED = new LinkedHashMap<>();
    private static final Map<UUID, String> PENDING = new LinkedHashMap<>();
    private static VillageRelicData savedData;

    private VillageRelicSystem() {}

    public static synchronized void initializeServer(MinecraftServer server) {
        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageRelicData.TYPE);
        OWNED.clear();
        PENDING.clear();
        savedData.owned().forEach((key, value) -> parseUuid(key, uuid -> OWNED.put(uuid, sanitizeMask(value))));
        savedData.pending().forEach((key, value) -> parseUuid(key, uuid -> PENDING.put(uuid, value)));
        persist();
    }

    public static synchronized void offerToParty(MinecraftServer server) {
        int day = VillageCouncilState.currentDay();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            List<Relic> choices = choicesFor(player, day);
            if (choices.isEmpty()) continue;
            PENDING.put(player.getUUID(), choices.stream().map(Relic::id)
                    .reduce((first, second) -> first + "," + second).orElse(""));
            persist();
            openChoice(player);
        }
    }

    public static synchronized void openChoice(ServerPlayer player) {
        List<Relic> choices = pendingChoices(player);
        if (choices.isEmpty()) return;
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (Relic relic : choices) {
            actions.add("relic_select:" + relic.id());
            labels.add(relic.displayName() + "|" + relic.description());
        }
        VillageNetwork.open(player, new VillageNetwork.OpenVillageUiPayload(
                "relic_choice", "보스 유물 선택",
                "보스를 쓰러뜨렸습니다. 세 유물 중 하나를 선택하면 이 플레이어에게 영구 적용됩니다.",
                String.join(SEP, actions), String.join(SEP, labels)));
    }

    public static synchronized void openCollection(ServerPlayer player) {
        int owned = ownedCount(player);
        List<String> labels = new ArrayList<>();
        for (Relic relic : Relic.values()) {
            boolean acquired = has(player, relic);
            labels.add(String.join("|", "relic", relic.id(), acquired ? "owned" : "locked",
                    relic.displayName(), relic.description()));
        }
        String body = "보유 " + owned + " / " + Relic.values().length
                + " · 보스 처치 후 3개 중 하나 선택 · 플레이어별 영구 적용\n"
                + aggregateSummary(player);
        VillageNetwork.open(player, new VillageNetwork.OpenVillageUiPayload(
                "relic_collection", "획득 유물", body, "", String.join(SEP, labels)));
    }

    public static synchronized String select(ServerPlayer player, String id) {
        Relic relic = Relic.fromId(id);
        if (relic == null) return "알 수 없는 유물입니다.";
        List<Relic> choices = pendingChoices(player);
        if (!choices.contains(relic)) return "현재 제시된 유물이 아닙니다.";
        int mask = OWNED.getOrDefault(player.getUUID(), 0);
        OWNED.put(player.getUUID(), sanitizeMask(mask | relic.bit()));
        PENDING.remove(player.getUUID());
        persist();
        return relic.displayName() + " 획득 · " + relic.description();
    }

    public static synchronized boolean has(ServerPlayer player, Relic relic) {
        return player != null && relic != null
                && (OWNED.getOrDefault(player.getUUID(), 0) & relic.bit()) != 0;
    }

    public static synchronized int ownedCount(ServerPlayer player) {
        if (player == null) return 0;
        return Integer.bitCount(sanitizeMask(OWNED.getOrDefault(player.getUUID(), 0)));
    }

    public static synchronized void resetForNewGame() {
        OWNED.clear();
        PENDING.clear();
        persist();
    }

    public static float meleeMultiplier(ServerPlayer player) {
        float result = 1.0f;
        if (has(player, Relic.WAR_SIGIL)) result *= 1.15f;
        if (has(player, Relic.EXECUTION_EDGE)) result *= 1.10f;
        if (has(player, Relic.BLOOD_CHALICE)) result *= 1.08f;
        if (has(player, Relic.STORM_FEATHER)) result *= 1.05f;
        return result;
    }

    public static float projectileMultiplier(ServerPlayer player) {
        float result = 1.0f;
        if (has(player, Relic.HUNTERS_EYE)) result *= 1.18f;
        if (has(player, Relic.WAR_SIGIL)) result *= 1.08f;
        if (has(player, Relic.STORM_FEATHER)) result *= 1.10f;
        return result;
    }

    public static float incomingMultiplier(ServerPlayer player) {
        float result = 1.0f;
        if (has(player, Relic.WARD_STONE)) result *= 0.86f;
        if (has(player, Relic.LAST_LIGHT)) result *= 0.92f;
        if (has(player, Relic.BASTION_CORE)) result *= 0.90f;
        if (has(player, Relic.STORM_FEATHER)) result *= 0.96f;
        return result;
    }

    public static float skillMultiplier(ServerPlayer player) {
        float result = 1.0f;
        if (has(player, Relic.ARCANE_HEART)) result *= 1.20f;
        if (has(player, Relic.LAST_LIGHT)) result *= 1.08f;
        if (has(player, Relic.DAWN_PRISM)) result *= 1.12f;
        return result;
    }

    public static float executionMultiplier(ServerPlayer player, float health, float maximumHealth) {
        if (!has(player, Relic.EXECUTION_EDGE) || maximumHealth <= 0.0f) return 1.0f;
        return health <= maximumHealth * 0.30f ? 1.20f : 1.0f;
    }

    public static int cooldownReductionSeconds(ServerPlayer player) {
        return has(player, Relic.CHRONO_SHARD) ? 4 : 0;
    }

    public static float vanguardLifeStealBonus(ServerPlayer player) {
        return has(player, Relic.BLOOD_CHALICE) ? 0.04f : 0.0f;
    }

    public static synchronized String summary(ServerPlayer player) {
        List<String> names = new ArrayList<>();
        for (Relic relic : Relic.values()) if (has(player, relic)) names.add(relic.displayName());
        return names.isEmpty() ? "없음" : String.join(" · ", names);
    }

    public static String aggregateSummary(ServerPlayer player) {
        int melee = roundedPercent(meleeMultiplier(player) - 1.0f);
        int projectile = roundedPercent(projectileMultiplier(player) - 1.0f);
        int skill = roundedPercent(skillMultiplier(player) - 1.0f);
        int reduction = roundedPercent(1.0f - incomingMultiplier(player));
        int cooldown = cooldownReductionSeconds(player);
        int lifeSteal = Math.round(vanguardLifeStealBonus(player) * 100.0f);
        List<String> effects = new ArrayList<>();
        if (melee > 0) effects.add("근접 +" + melee + "%");
        if (projectile > 0) effects.add("원거리 +" + projectile + "%");
        if (skill > 0) effects.add("기술 +" + skill + "%");
        if (reduction > 0) effects.add("피해 감소 " + reduction + "%");
        if (cooldown > 0) effects.add("쿨다운 -" + cooldown + "초");
        if (lifeSteal > 0) effects.add("선봉 흡혈 +" + lifeSteal + "%p");
        if (has(player, Relic.EXECUTION_EDGE)) effects.add("체력 30% 이하 적 추가 피해 +20%");
        return effects.isEmpty() ? "현재 적용 중인 유물 효과 없음" : String.join(" · ", effects);
    }

    private static int roundedPercent(float value) {
        return Math.max(0, Math.round(value * 100.0f));
    }

    private static List<Relic> choicesFor(ServerPlayer player, int day) {
        List<Relic> available = new ArrayList<>();
        int mask = OWNED.getOrDefault(player.getUUID(), 0);
        for (Relic relic : Relic.values()) if ((mask & relic.bit()) == 0) available.add(relic);
        if (available.isEmpty()) return List.of();
        List<Relic> result = new ArrayList<>();
        int seed = player.getUUID().hashCode() * 31 + day * 17 + Integer.bitCount(mask) * 13;
        while (!available.isEmpty() && result.size() < 3) {
            int index = Math.floorMod(seed + result.size() * 37, available.size());
            result.add(available.remove(index));
        }
        return result;
    }

    private static List<Relic> pendingChoices(ServerPlayer player) {
        String raw = PENDING.getOrDefault(player.getUUID(), "");
        List<Relic> result = new ArrayList<>();
        for (String id : raw.split(",")) {
            Relic relic = Relic.fromId(id);
            if (relic != null) result.add(relic);
        }
        return result;
    }

    private static int sanitizeMask(int value) {
        int validBits = (1 << Relic.values().length) - 1;
        return value & validBits;
    }

    private static void persist() {
        if (savedData == null) return;
        Map<String, Integer> owned = new LinkedHashMap<>();
        OWNED.forEach((uuid, value) -> owned.put(uuid.toString(), sanitizeMask(value)));
        Map<String, String> pending = new LinkedHashMap<>();
        PENDING.forEach((uuid, value) -> pending.put(uuid.toString(), value));
        savedData.replace(owned, pending);
    }

    private static void parseUuid(String value, java.util.function.Consumer<UUID> consumer) {
        try {
            consumer.accept(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            // Ignore malformed legacy UUID keys instead of breaking world loading.
        }
    }

    public enum Relic {
        WAR_SIGIL("war_sigil", "전쟁의 인장", "근접 피해 +15%, 원거리 피해 +8%"),
        HUNTERS_EYE("hunters_eye", "추적자의 눈", "원거리 피해 +18%"),
        WARD_STONE("ward_stone", "수호석", "받는 피해 14% 감소"),
        ARCANE_HEART("arcane_heart", "비전 심장", "직업 기술 피해·치유 +20%"),
        EXECUTION_EDGE("execution_edge", "처형의 칼날", "근접 피해 +10%, 체력 30% 이하 적에게 추가 피해 +20%"),
        LAST_LIGHT("last_light", "마지막 등불", "받는 피해 8% 감소, 직업 기술 피해·치유 +8%"),
        CHRONO_SHARD("chrono_shard", "시간균열 파편", "모든 직업 기술 재사용 대기시간 4초 감소"),
        BLOOD_CHALICE("blood_chalice", "붉은 성배", "근접 피해 +8%, 선봉검사 흡혈 +4%p"),
        BASTION_CORE("bastion_core", "성채의 심핵", "받는 피해 10% 감소"),
        DAWN_PRISM("dawn_prism", "여명의 프리즘", "직업 기술 피해·치유 +12%"),
        STORM_FEATHER("storm_feather", "폭풍매의 깃", "원거리 피해 +10%, 근접 피해 +5%, 받는 피해 4% 감소");

        private final String id;
        private final String displayName;
        private final String description;

        Relic(String id, String displayName, String description) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }
        public String description() { return description; }
        public int bit() { return 1 << ordinal(); }

        public static Relic fromId(String id) {
            if (id == null) return null;
            String normalized = id.toLowerCase(Locale.ROOT);
            for (Relic relic : values()) if (relic.id.equals(normalized)) return relic;
            return null;
        }
    }
}

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
    private static final String OFFER_SEP = ";";
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
        if (server == null) return;
        int day = VillageCouncilState.currentDay();
        for (UUID playerId : VillageProgressionSystem.nightParticipants(server)) {
            List<Relic> choices = choicesFor(playerId, day);
            if (choices.isEmpty()) continue;
            String encoded = choices.stream().map(Relic::id)
                    .reduce((first, second) -> first + "," + second).orElse("");
            String previous = PENDING.getOrDefault(playerId, "");
            PENDING.put(playerId, previous.isBlank() ? encoded : previous + OFFER_SEP + encoded);
            persist();
            ServerPlayer online = server.getPlayerList().getPlayer(playerId);
            if (previous.isBlank() && online != null && !VillageRaidSystem.isRaidLocked()) openChoice(online);
        }
    }

    public static synchronized void openPendingChoicesForParty(MinecraftServer server) {
        if (server == null || VillageRaidSystem.isRaidLocked()) return;
        for (UUID playerId : VillageProgressionSystem.nightParticipants(server)) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null && hasPendingChoice(player)) openChoice(player);
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
                "세 유물 중 하나를 선택하면 영구 적용됩니다. 이미 보유했거나 선택 대기 중인 유물은 다시 제시되지 않습니다.",
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
                + " · 보스 처치 후 3개 중 하나 선택 · 중복 획득 없음 · 플레이어별 영구 적용\n"
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
        consumePendingOffer(player.getUUID());
        persist();
        return relic.displayName() + " 획득 · " + relic.description();
    }

    public static synchronized boolean hasPendingChoice(ServerPlayer player) {
        return player != null && !pendingChoices(player).isEmpty();
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
        if (has(player, Relic.WAR_SIGIL)) result *= 1.18f;
        if (has(player, Relic.EXECUTION_EDGE)) result *= 1.08f;
        if (has(player, Relic.BLOOD_CHALICE)) result *= 1.10f;
        if (has(player, Relic.STORM_FEATHER)) result *= 1.06f;
        return result;
    }

    public static float projectileMultiplier(ServerPlayer player) {
        float result = 1.0f;
        if (has(player, Relic.HUNTERS_EYE)) result *= 1.20f;
        if (has(player, Relic.WAR_SIGIL)) result *= 1.10f;
        if (has(player, Relic.STORM_FEATHER)) result *= 1.14f;
        return result;
    }

    public static float projectileTargetMultiplier(ServerPlayer player, net.minecraft.world.entity.Mob target) {
        if (!has(player, Relic.HUNTERS_EYE) || target == null) return 1.0f;
        VillageEnemyArchetypeSystem.Archetype archetype = VillageRaidSystem.archetypeOf(target);
        return VillageRaidSystem.isAerialEnemy(target)
                || VillageEnemyArchetypeSystem.isTacticalThreat(archetype) ? 1.20f : 1.0f;
    }

    public static float incomingMultiplier(ServerPlayer player) {
        float result = 1.0f;
        if (has(player, Relic.WARD_STONE)) result *= 0.82f;
        if (has(player, Relic.LAST_LIGHT)) {
            result *= 0.96f;
            if (player != null && player.getHealth() <= player.getMaxHealth() * 0.35f) result *= 0.82f;
        }
        if (has(player, Relic.BASTION_CORE)) result *= 0.88f;
        if (has(player, Relic.STORM_FEATHER)) result *= 0.95f;
        return result;
    }

    public static float skillMultiplier(ServerPlayer player) {
        float result = 1.0f;
        if (has(player, Relic.ARCANE_HEART)) result *= 1.28f;
        if (has(player, Relic.LAST_LIGHT)) {
            result *= 1.06f;
            if (player != null && player.getHealth() <= player.getMaxHealth() * 0.35f) result *= 1.18f;
        }
        if (has(player, Relic.DAWN_PRISM)) result *= 1.15f;
        return result;
    }

    public static float skillDurationMultiplier(ServerPlayer player) {
        return has(player, Relic.ARCANE_HEART) ? 1.15f : 1.0f;
    }

    public static float executionMultiplier(ServerPlayer player, float health, float maximumHealth) {
        if (!has(player, Relic.EXECUTION_EDGE) || maximumHealth <= 0.0f) return 1.0f;
        return health <= maximumHealth * 0.35f ? 1.35f : 1.0f;
    }

    public static float cooldownMultiplier(ServerPlayer player) {
        float result = 1.0f;
        if (has(player, Relic.CHRONO_SHARD)) result *= 0.78f;
        if (has(player, Relic.DAWN_PRISM)) result *= 0.90f;
        if (has(player, Relic.STORM_FEATHER)) result *= 0.95f;
        return result;
    }

    public static int cooldownReductionSeconds(ServerPlayer player) { return 0; }

    public static float meleeLifeStealBonus(ServerPlayer player) {
        return has(player, Relic.BLOOD_CHALICE) ? 0.03f : 0.0f;
    }

    public static float vanguardLifeStealBonus(ServerPlayer player) {
        return has(player, Relic.BLOOD_CHALICE) ? 0.03f : 0.0f;
    }

    public static float tauntDurationMultiplier(ServerPlayer player) {
        return has(player, Relic.BASTION_CORE) ? 1.35f : 1.0f;
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
        int cooldown = Math.max(0, Math.round((1.0f - cooldownMultiplier(player)) * 100.0f));
        int lifeSteal = Math.round(meleeLifeStealBonus(player) * 100.0f);
        List<String> effects = new ArrayList<>();
        if (melee > 0) effects.add("근접 +" + melee + "%");
        if (projectile > 0) effects.add("원거리 +" + projectile + "%");
        if (skill > 0) effects.add("기술 +" + skill + "%");
        if (reduction > 0) effects.add("피해 감소 " + reduction + "%");
        if (cooldown > 0) effects.add("재사용 시간 -" + cooldown + "%");
        if (lifeSteal > 0) effects.add("근접 흡혈 +" + lifeSteal + "%");
        if (has(player, Relic.EXECUTION_EDGE)) effects.add("체력 35% 이하 적 추가 피해 +35%");
        if (has(player, Relic.HUNTERS_EYE)) effects.add("공중·전술 표적 원거리 추가 피해 +20%");
        if (has(player, Relic.ARCANE_HEART)) effects.add("기술 지속시간 +15%");
        if (has(player, Relic.BASTION_CORE)) effects.add("도발 지속시간 +35%");
        if (has(player, Relic.LAST_LIGHT)) effects.add("위기 체력에서 추가 방어·기술 강화");
        return effects.isEmpty() ? "현재 적용 중인 유물 효과 없음" : String.join(" · ", effects);
    }

    private static int roundedPercent(float value) {
        return Math.max(0, Math.round(value * 100.0f));
    }

    private static List<Relic> choicesFor(UUID playerId, int day) {
        List<Relic> available = new ArrayList<>();
        int mask = OWNED.getOrDefault(playerId, 0);
        java.util.Set<Relic> reserved = pendingRelics(playerId);
        for (Relic relic : Relic.values()) {
            if ((mask & relic.bit()) == 0 && !reserved.contains(relic)) available.add(relic);
        }
        if (available.isEmpty()) return List.of();
        List<Relic> result = new ArrayList<>();
        int seed = playerId.hashCode() * 31 + day * 17
                + Integer.bitCount(mask) * 13 + reserved.size() * 19;
        while (!available.isEmpty() && result.size() < 3) {
            int index = Math.floorMod(seed + result.size() * 37, available.size());
            result.add(available.remove(index));
        }
        return result;
    }

    private static List<Relic> pendingChoices(ServerPlayer player) {
        if (player == null) return List.of();
        String raw = PENDING.getOrDefault(player.getUUID(), "");
        if (raw.isBlank()) return List.of();
        String first = raw.split(OFFER_SEP, 2)[0];
        List<Relic> result = new ArrayList<>();
        for (String id : first.split(",")) {
            Relic relic = Relic.fromId(id);
            if (relic != null) result.add(relic);
        }
        return result;
    }

    private static java.util.Set<Relic> pendingRelics(UUID playerId) {
        java.util.Set<Relic> result = java.util.EnumSet.noneOf(Relic.class);
        String raw = PENDING.getOrDefault(playerId, "");
        if (raw.isBlank()) return result;
        for (String offer : raw.split(OFFER_SEP)) {
            for (String id : offer.split(",")) {
                Relic relic = Relic.fromId(id);
                if (relic != null) result.add(relic);
            }
        }
        return result;
    }

    private static void consumePendingOffer(UUID playerId) {
        String raw = PENDING.getOrDefault(playerId, "");
        if (raw.isBlank()) {
            PENDING.remove(playerId);
            return;
        }
        int separator = raw.indexOf(OFFER_SEP);
        if (separator < 0 || separator + OFFER_SEP.length() >= raw.length()) {
            PENDING.remove(playerId);
        } else {
            PENDING.put(playerId, raw.substring(separator + OFFER_SEP.length()));
        }
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
        WAR_SIGIL("war_sigil", "전쟁의 인장", "근접 피해 +18%, 원거리 피해 +10%"),
        HUNTERS_EYE("hunters_eye", "추적자의 눈", "원거리 피해 +20%, 공중·전술 표적에게 추가 +20%"),
        WARD_STONE("ward_stone", "수호석", "받는 피해 18% 감소"),
        ARCANE_HEART("arcane_heart", "비전 심장", "직업 기술 피해·치유 +28%, 지속시간 +15%"),
        EXECUTION_EDGE("execution_edge", "처형의 칼날", "근접 피해 +8%, 체력 35% 이하 적에게 추가 피해 +35%"),
        LAST_LIGHT("last_light", "마지막 등불", "평상시 소폭 방어·기술 강화, 체력 35% 이하에서 효과가 크게 증폭"),
        CHRONO_SHARD("chrono_shard", "시간균열 파편", "직업 기술 재사용 시간을 22% 단축"),
        BLOOD_CHALICE("blood_chalice", "붉은 성배", "근접 피해 +10%, 모든 근접 공격 흡혈 3%, 선봉검사 추가 흡혈 +3%p"),
        BASTION_CORE("bastion_core", "성채의 심핵", "받는 피해 12% 감소, 도발 지속시간 +35%"),
        DAWN_PRISM("dawn_prism", "여명의 프리즘", "직업 기술 피해·치유 +15%, 재사용 시간 10% 단축"),
        STORM_FEATHER("storm_feather", "폭풍매의 깃", "원거리 +14%, 근접 +6%, 피해 감소 5%, 재사용 시간 5% 단축");

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

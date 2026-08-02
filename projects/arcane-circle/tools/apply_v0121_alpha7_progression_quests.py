#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"
RES = ROOT / "src/main/resources"


def path(relative: str) -> Path:
    return ROOT / relative


def replace_once(relative: str, old: str, new: str) -> None:
    target = path(relative)
    text = target.read_text(encoding="utf-8")
    if old not in text:
        if new in text:
            return
        raise RuntimeError(f"missing patch anchor in {relative}: {old[:120]!r}")
    if text.count(old) != 1:
        raise RuntimeError(f"ambiguous patch anchor in {relative}: {text.count(old)} matches")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


def write(relative: str, content: str) -> None:
    target = path(relative)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


properties = path("gradle.properties").read_text(encoding="utf-8")
if "mod_version=0.12.1-alpha.7" in properties:
    required = [
        JAVA / "network/QuestActionPayload.java",
        JAVA / "magic/CombatGrowthService.java",
        ROOT / "src/main/resources/assets/arcanecircle/items/froststep_boots.json",
    ]
    missing = [str(value) for value in required if not value.exists()]
    if missing:
        raise RuntimeError(f"alpha.7 version exists but files are missing: {missing}")
    print("Arcane Circle v0.12.1-alpha.7 progression migration already applied")
    raise SystemExit(0)

replace_once("gradle.properties", "mod_version=0.12.1-alpha.6", "mod_version=0.12.1-alpha.7")
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java",
    'public static final String VERSION = "0.12.1-alpha.6";',
    'public static final String VERSION = "0.12.1-alpha.7";',
)
replace_once(
    "src/main/resources/data/arcanecircle/spell_catalog/index.json",
    '"version": "0.12.1-alpha.6"',
    '"version": "0.12.1-alpha.7"',
)

write("src/main/java/kr/moonseungjun/arcanecircle/world/MagicTradition.java", 'package kr.moonseungjun.arcanecircle.world;\n\nimport kr.moonseungjun.arcanecircle.magic.SpellDefinition;\n\n/**\n * Save-compatible affiliation keys. Factions remain social organizations, but each now teaches\n * a distinct combat doctrine with an explicit strength and drawback.\n */\npublic enum MagicTradition {\n    UNBOUND(\n            "무소속",\n            "어느 조직에도 속하지 않은 떠돌이·은둔·생활 마법사입니다.",\n            "제약 없는 중립 관계", "전용 교리와 보상 보정 없음",\n            1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00),\n    ARCANE(\n            "왕국 마도연맹",\n            "면허·정밀 조준·공공 전투 교리를 중시하는 왕국 공인 마도 조직입니다.",\n            "사거리 +18% · 쿨타임 -14% · 비전술 +15%",\n            "마력 소모 +8% · 일반 위력 -4%",\n            1.08, 0.96, 1.18, 0.86, 1.00, 1.10, 1.08, 0.90, 1.04),\n    DIVINE(\n            "백은 성약",\n            "보호·치유·재난 대응과 안정적인 장기전을 우선하는 독립 성약입니다.",\n            "마력 회복 +28% · 생명/수호술 +25% · 의뢰 보상 +18%",\n            "일반 공격 위력 -8% · 쿨타임 +5%",\n            0.94, 0.92, 1.02, 1.05, 1.28, 1.02, 1.18, 1.02, 1.02),\n    OCCULT(\n            "녹월 결사",\n            "민간 전승과 금단 직전의 복합 회로를 연구하는 느슨한 비밀 결사입니다.",\n            "마력 소모 -22% · 공격 위력 +12% · 융합 위력 +18%",\n            "사거리 -8% · 쿨타임 +12%",\n            0.78, 1.12, 0.92, 1.12, 1.05, 1.08, 1.05, 1.06, 1.18),\n    PRIMAL(\n            "재의 밀약",\n            "지배와 파괴를 추구하며 다른 학회와 적대하는 고위험 전투 조직입니다.",\n            "위력 +24% · 전투 아르카나 +30% · 원소술 +15%",\n            "마력 소모 +16% · 사거리 -12% · 쿨타임 +22%",\n            1.16, 1.24, 0.88, 1.22, 0.92, 1.30, 1.12, 1.12, 1.08);\n\n    private final String displayName;\n    private final String description;\n    private final String strength;\n    private final String weakness;\n    private final double manaMultiplier;\n    private final double powerMultiplier;\n    private final double rangeMultiplier;\n    private final double cooldownMultiplier;\n    private final double regenMultiplier;\n    private final double combatRewardMultiplier;\n    private final double questRewardMultiplier;\n    private final double castTimeMultiplier;\n    private final double fusionMultiplier;\n\n    MagicTradition(String displayName, String description, String strength, String weakness,\n                   double manaMultiplier, double powerMultiplier, double rangeMultiplier,\n                   double cooldownMultiplier, double regenMultiplier, double combatRewardMultiplier,\n                   double questRewardMultiplier, double castTimeMultiplier, double fusionMultiplier) {\n        this.displayName = displayName;\n        this.description = description;\n        this.strength = strength;\n        this.weakness = weakness;\n        this.manaMultiplier = manaMultiplier;\n        this.powerMultiplier = powerMultiplier;\n        this.rangeMultiplier = rangeMultiplier;\n        this.cooldownMultiplier = cooldownMultiplier;\n        this.regenMultiplier = regenMultiplier;\n        this.combatRewardMultiplier = combatRewardMultiplier;\n        this.questRewardMultiplier = questRewardMultiplier;\n        this.castTimeMultiplier = castTimeMultiplier;\n        this.fusionMultiplier = fusionMultiplier;\n    }\n\n    public String displayName() { return displayName; }\n    public String description() { return description; }\n    public String strength() { return strength; }\n    public String weakness() { return weakness; }\n    public double manaMultiplier() { return manaMultiplier; }\n    public double powerMultiplier() { return powerMultiplier; }\n    public double rangeMultiplier() { return rangeMultiplier; }\n    public double cooldownMultiplier() { return cooldownMultiplier; }\n    public double regenMultiplier() { return regenMultiplier; }\n    public double combatRewardMultiplier() { return combatRewardMultiplier; }\n    public double questRewardMultiplier() { return questRewardMultiplier; }\n    public double castTimeMultiplier() { return castTimeMultiplier; }\n    public double fusionMultiplier() { return fusionMultiplier; }\n\n    public double powerFor(SpellDefinition.School school) {\n        double schoolDoctrine = switch (this) {\n            case ARCANE -> school == SpellDefinition.School.ARCANE ? 1.15 : 1.0;\n            case DIVINE -> school == SpellDefinition.School.LIFE || school == SpellDefinition.School.WARD ? 1.25 : 1.0;\n            case OCCULT -> school == SpellDefinition.School.ARCANE || school == SpellDefinition.School.LIFE ? 1.12 : 1.0;\n            case PRIMAL -> school == SpellDefinition.School.FIRE\n                    || school == SpellDefinition.School.FROST\n                    || school == SpellDefinition.School.WIND ? 1.15 : 1.0;\n            default -> 1.0;\n        };\n        return powerMultiplier * schoolDoctrine;\n    }\n\n    public static MagicTradition parse(String value) {\n        if (value == null || value.isBlank()) return UNBOUND;\n        try { return valueOf(value.toUpperCase()); }\n        catch (IllegalArgumentException ignored) { return UNBOUND; }\n    }\n}\n')

write("src/main/java/kr/moonseungjun/arcanecircle/magic/CombatGrowthService.java", 'package kr.moonseungjun.arcanecircle.magic;\n\nimport kr.moonseungjun.arcanecircle.world.ArcaneMageService;\nimport net.minecraft.core.Holder;\nimport net.minecraft.core.registries.BuiltInRegistries;\nimport net.minecraft.server.level.ServerLevel;\nimport net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.world.entity.EquipmentSlot;\nimport net.minecraft.world.entity.Mob;\nimport net.minecraft.world.entity.TamableAnimal;\nimport net.minecraft.world.entity.ai.attributes.Attribute;\nimport net.minecraft.world.entity.ai.attributes.AttributeInstance;\nimport net.minecraft.world.entity.ai.attributes.Attributes;\nimport net.minecraft.world.entity.monster.Enemy;\nimport net.minecraft.world.phys.AABB;\n\nimport java.util.ArrayList;\nimport java.util.List;\n\n/** Measures actual combat output and estimates enemy strength from the complete combat profile. */\npublic final class CombatGrowthService {\n    private static final List<EquipmentSlot> THREAT_SLOTS = List.of(\n            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,\n            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);\n\n    private CombatGrowthService() {}\n\n    public record Sample(Mob mob, float health, float maxHealth, int threat) {}\n    public record Snapshot(List<Sample> samples) {\n        public static final Snapshot EMPTY = new Snapshot(List.of());\n    }\n    public record Impact(int hits, int kills, int strongHits, int strongKills, int damage, int masteryGain,\n                         int insightGain, int threatPoints, int peakThreat, long combatValue) {\n        public static final Impact NONE = new Impact(0, 0, 0, 0, 0, 1, 0, 0, 0, 0L);\n        public boolean meaningful() { return hits > 0 || kills > 0; }\n    }\n\n    public static Snapshot capture(ServerPlayer player, double range) {\n        ServerLevel level = (ServerLevel) player.level();\n        double radius = Math.max(12.0, range + 12.0);\n        AABB box = player.getBoundingBox().inflate(radius, Math.max(10.0, radius * 0.55), radius);\n        List<Sample> samples = new ArrayList<>();\n        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, mob -> validTarget(player, mob))) {\n            samples.add(new Sample(mob, mob.getHealth(), mob.getMaxHealth(), threatScore(mob)));\n        }\n        return new Snapshot(List.copyOf(samples));\n    }\n\n    public static Impact measure(Snapshot snapshot, int spellCircle) {\n        if (snapshot == null || snapshot.samples().isEmpty()) return Impact.NONE;\n        int hits = 0;\n        int kills = 0;\n        int strongHits = 0;\n        int strongKills = 0;\n        double damage = 0.0;\n        int threatPoints = 0;\n        int peakThreat = 0;\n        long combatValue = 0L;\n\n        for (Sample sample : snapshot.samples()) {\n            Mob mob = sample.mob();\n            float after = mob.isAlive() && !mob.isRemoved() ? Math.max(0.0F, mob.getHealth()) : 0.0F;\n            double dealt = Math.max(0.0, sample.health() - after);\n            boolean killed = sample.health() > 0.0F && (!mob.isAlive() || mob.isRemoved() || after <= 0.0F);\n            if (dealt <= 0.001 && !killed) continue;\n\n            hits++;\n            damage += dealt;\n            int threat = Math.max(1, sample.threat());\n            peakThreat = Math.max(peakThreat, threat);\n            int tier = threatTier(threat);\n            if (tier > 0) strongHits++;\n            if (killed) {\n                kills++;\n                if (tier > 0) strongKills++;\n            }\n\n            threatPoints = Math.min(1_000_000, threatPoints\n                    + (killed ? threat * 2 : Math.max(1, threat / 5)));\n            long hitValue = Math.max(1L, Math.round(Math.pow(threat, 1.25) * 0.60));\n            long killValue = killed ? Math.max(1L, Math.round(Math.pow(threat, 1.75) * 1.80)) : 0L;\n            combatValue = Math.min(50_000_000L, combatValue + hitValue + killValue);\n        }\n\n        if (hits == 0 && kills == 0) return Impact.NONE;\n        int damagePoints = Math.min(18, (int) Math.floor(damage / 10.0));\n        int mastery = 1 + hits + kills * 3 + Math.min(36, threatPoints / 8) + damagePoints;\n        int insight = hits + kills * 3 + Math.min(90, threatPoints / 4) + Math.max(0, spellCircle - 1);\n        return new Impact(hits, kills, strongHits, strongKills, (int) Math.round(damage),\n                mastery, insight, threatPoints, peakThreat, combatValue);\n    }\n\n    public static int threatScore(Mob mob) {\n        double health = Math.max(1.0, mob.getMaxHealth());\n        double attack = attribute(mob, Attributes.ATTACK_DAMAGE);\n        double armor = attribute(mob, Attributes.ARMOR);\n        double toughness = attribute(mob, Attributes.ARMOR_TOUGHNESS);\n        double speed = attribute(mob, Attributes.MOVEMENT_SPEED);\n        double follow = attribute(mob, Attributes.FOLLOW_RANGE);\n        int equipment = 0;\n        for (EquipmentSlot slot : THREAT_SLOTS) if (!mob.getItemBySlot(slot).isEmpty()) equipment++;\n\n        double score = 1.0\n                + Math.sqrt(health) * 0.80\n                + Math.pow(Math.max(0.0, attack), 1.25) * 0.80\n                + Math.pow(Math.max(0.0, armor), 1.12) * 0.36\n                + toughness * 0.90\n                + speed * 8.0\n                + follow / 20.0\n                + equipment * 1.5\n                + mob.getActiveEffects().size() * 1.25;\n        if (mob instanceof Enemy) score *= 1.10;\n        if (ArcaneMageService.isMage(mob)) {\n            int circle = ArcaneMageService.circle(mob);\n            score += circle * circle * 2.6;\n        }\n\n        String type = typePath(mob);\n        score += switch (type) {\n            case "warden" -> 80.0;\n            case "wither" -> 95.0;\n            case "ender_dragon" -> 110.0;\n            case "elder_guardian" -> 34.0;\n            case "ravager" -> 25.0;\n            case "evoker" -> 20.0;\n            case "piglin_brute" -> 14.0;\n            default -> 0.0;\n        };\n        return Math.max(1, Math.min(250, (int) Math.round(score)));\n    }\n\n    private static double attribute(Mob mob, Holder<Attribute> attribute) {\n        AttributeInstance instance = mob.getAttribute(attribute);\n        return instance == null ? 0.0 : instance.getValue();\n    }\n\n    private static int threatTier(int threat) {\n        if (threat >= 150) return 6;\n        if (threat >= 100) return 5;\n        if (threat >= 65) return 4;\n        if (threat >= 40) return 3;\n        if (threat >= 24) return 2;\n        if (threat >= 16) return 1;\n        return 0;\n    }\n\n    private static String typePath(Mob mob) {\n        var key = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());\n        return key == null ? "" : key.getPath();\n    }\n\n    private static boolean validTarget(ServerPlayer player, Mob mob) {\n        if (!mob.isAlive() || mob.isRemoved()) return false;\n        if (mob instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(player)) return false;\n        return player.getTeam() == null || mob.getTeam() == null || !player.isAlliedTo(mob);\n    }\n}\n')

write("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneQuestData.java", 'package kr.moonseungjun.arcanecircle.world;\n\nimport com.mojang.serialization.Codec;\nimport com.mojang.serialization.codecs.RecordCodecBuilder;\nimport kr.moonseungjun.arcanecircle.ArcaneCircle;\nimport kr.moonseungjun.arcanecircle.magic.ArcaneNoticeService;\nimport kr.moonseungjun.arcanecircle.magic.CombatGrowthService;\nimport net.minecraft.network.chat.Component;\nimport net.minecraft.resources.Identifier;\nimport net.minecraft.server.MinecraftServer;\nimport net.minecraft.server.level.ServerLevel;\nimport net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.world.level.saveddata.SavedData;\nimport net.minecraft.world.level.saveddata.SavedDataType;\n\nimport java.util.ArrayList;\nimport java.util.HashMap;\nimport java.util.List;\nimport java.util.Map;\n\n/** Three-slot quest board with a save-compatible migration from the old single commission. */\npublic final class ArcaneQuestData extends SavedData {\n    public static final int MAX_ACTIVE = 3;\n\n    private record QuestEntry(String id, int target, int progress, int circle, long reward, String affiliation) {\n        private static final QuestEntry EMPTY = new QuestEntry("", 0, 0, 1, 0L, "UNBOUND");\n        private static final Codec<QuestEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(\n                Codec.STRING.optionalFieldOf("id", "").forGetter(QuestEntry::id),\n                Codec.INT.optionalFieldOf("target", 0).forGetter(QuestEntry::target),\n                Codec.INT.optionalFieldOf("progress", 0).forGetter(QuestEntry::progress),\n                Codec.INT.optionalFieldOf("circle", 1).forGetter(QuestEntry::circle),\n                Codec.LONG.optionalFieldOf("reward", 0L).forGetter(QuestEntry::reward),\n                Codec.STRING.optionalFieldOf("affiliation", "UNBOUND").forGetter(QuestEntry::affiliation)\n        ).apply(instance, QuestEntry::new));\n    }\n\n    private record PlayerEntry(\n            String uuid,\n            String quest,\n            int target,\n            int progress,\n            int circle,\n            long reward,\n            List<QuestEntry> active,\n            QuestEntry offered,\n            int offerSerial\n    ) {\n        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(\n                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),\n                Codec.STRING.optionalFieldOf("quest", "").forGetter(PlayerEntry::quest),\n                Codec.INT.optionalFieldOf("target", 0).forGetter(PlayerEntry::target),\n                Codec.INT.optionalFieldOf("progress", 0).forGetter(PlayerEntry::progress),\n                Codec.INT.optionalFieldOf("circle", 1).forGetter(PlayerEntry::circle),\n                Codec.LONG.optionalFieldOf("reward", 0L).forGetter(PlayerEntry::reward),\n                QuestEntry.CODEC.listOf().optionalFieldOf("active", List.of()).forGetter(PlayerEntry::active),\n                QuestEntry.CODEC.optionalFieldOf("offered", QuestEntry.EMPTY).forGetter(PlayerEntry::offered),\n                Codec.INT.optionalFieldOf("offer_serial", 0).forGetter(PlayerEntry::offerSerial)\n        ).apply(instance, PlayerEntry::new));\n    }\n\n    public static final SavedDataType<ArcaneQuestData> TYPE = new SavedDataType<>(\n            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "mage_commissions_v1"),\n            ArcaneQuestData::new,\n            RecordCodecBuilder.create(instance -> instance.group(\n                    PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(ArcaneQuestData::entries)\n            ).apply(instance, ArcaneQuestData::new))\n    );\n\n    private static final class Quest {\n        String id = "";\n        int target;\n        int progress;\n        int circle = 1;\n        long reward;\n        MagicTradition affiliation = MagicTradition.UNBOUND;\n\n        Quest copy() {\n            Quest result = new Quest();\n            result.id = id;\n            result.target = target;\n            result.progress = progress;\n            result.circle = circle;\n            result.reward = reward;\n            result.affiliation = affiliation;\n            return result;\n        }\n    }\n\n    private final Map<String, List<Quest>> active = new HashMap<>();\n    private final Map<String, Quest> offered = new HashMap<>();\n    private final Map<String, Integer> offerSerial = new HashMap<>();\n\n    public ArcaneQuestData() {}\n\n    private ArcaneQuestData(List<PlayerEntry> entries) {\n        for (PlayerEntry entry : entries) {\n            List<Quest> quests = new ArrayList<>();\n            for (QuestEntry value : entry.active()) {\n                Quest quest = decode(value);\n                if (valid(quest) && quests.size() < MAX_ACTIVE) quests.add(quest);\n            }\n            if (quests.isEmpty() && !entry.quest().isBlank() && entry.target() > 0) {\n                Quest migrated = new Quest();\n                migrated.id = normalize(entry.quest());\n                migrated.target = Math.max(0, entry.target());\n                migrated.progress = Math.max(0, Math.min(migrated.target, entry.progress()));\n                migrated.circle = clamp(entry.circle(), 1, 9);\n                migrated.reward = Math.max(0L, entry.reward());\n                if (valid(migrated)) quests.add(migrated);\n            }\n            if (!quests.isEmpty()) active.put(entry.uuid(), quests);\n            Quest proposal = decode(entry.offered());\n            if (valid(proposal)) offered.put(entry.uuid(), proposal);\n            if (entry.offerSerial() > 0) offerSerial.put(entry.uuid(), entry.offerSerial());\n        }\n    }\n\n    public static ArcaneQuestData get(MinecraftServer server) {\n        return server.getDataStorage().computeIfAbsent(TYPE);\n    }\n\n    public List<QuestStatus> statuses(ServerPlayer player) {\n        return active.getOrDefault(key(player), List.of()).stream().map(ArcaneQuestData::status).toList();\n    }\n\n    public QuestStatus status(ServerPlayer player) {\n        List<QuestStatus> list = statuses(player);\n        return list.isEmpty() ? QuestStatus.NONE : list.getFirst();\n    }\n\n    public QuestStatus offerStatus(ServerPlayer player) {\n        Quest quest = offered.get(key(player));\n        return quest == null ? QuestStatus.NONE : status(quest);\n    }\n\n    public QuestStatus offer(ServerPlayer player, int mageCircle, MagicTradition issuer) {\n        String key = key(player);\n        Quest existing = offered.get(key);\n        if (existing != null) return status(existing);\n        if (active.getOrDefault(key, List.of()).size() >= MAX_ACTIVE) {\n            ArcaneNoticeService.push(player, Component.literal(\n                    "§c[의뢰 한도] §f동시에 진행할 수 있는 의뢰는 최대 3개입니다."), 100);\n            return QuestStatus.NONE;\n        }\n\n        int circle = clamp(mageCircle, 1, 9);\n        MagicTradition source = issuer == null ? MagicTradition.UNBOUND : issuer;\n        long day = ((ServerLevel) player.level()).getGameTime() / 24000L;\n        int serial = offerSerial.getOrDefault(key, 0);\n        int selector = Math.floorMod(player.getUUID().hashCode() + (int) day * 31\n                + circle * 17 + source.ordinal() * 13 + serial * 7, 6);\n\n        Quest quest = new Quest();\n        quest.id = switch (selector) {\n            case 1 -> "hits";\n            case 2 -> "kills";\n            case 3 -> "damage";\n            case 4 -> "threat";\n            case 5 -> "fusion";\n            default -> "casts";\n        };\n        quest.circle = circle;\n        quest.affiliation = source;\n        quest.target = targetFor(quest.id, circle);\n        double typeMultiplier = switch (quest.id) {\n            case "kills" -> 1.35;\n            case "damage" -> 1.20;\n            case "threat" -> 1.55;\n            case "fusion" -> 1.45;\n            default -> 1.0;\n        };\n        quest.reward = Math.max(500L, Math.round(baseReward(circle) * typeMultiplier\n                * source.questRewardMultiplier() * (1.0 + quest.target * 0.025)));\n        offered.put(key, quest);\n        setDirty();\n        ArcaneNoticeService.push(player, Component.literal("§5[새 의뢰 제안] §f" + description(quest.id)\n                + " §7· 목표 " + quest.target + " · 보상 §6" + quest.reward\n                + " 아르카나 §7· 의뢰 탭에서 수락 또는 거절"), 160);\n        return status(quest);\n    }\n\n    public boolean acceptOffer(ServerPlayer player) {\n        String key = key(player);\n        Quest proposal = offered.get(key);\n        if (proposal == null) {\n            ArcaneNoticeService.push(player, Component.literal("§7[의뢰] 수락할 제안이 없습니다."));\n            return false;\n        }\n        List<Quest> list = active.computeIfAbsent(key, ignored -> new ArrayList<>());\n        if (list.size() >= MAX_ACTIVE) {\n            ArcaneNoticeService.push(player, Component.literal("§c[의뢰 한도] §f동시에 최대 3개까지 진행할 수 있습니다."));\n            return false;\n        }\n        list.add(proposal.copy());\n        offered.remove(key);\n        offerSerial.merge(key, 1, Integer::sum);\n        setDirty();\n        ArcaneNoticeService.push(player, Component.literal("§5[의뢰 수락] §f" + description(proposal.id)\n                + " §7· 보상 §6" + proposal.reward + " 아르카나"), 100);\n        return true;\n    }\n\n    public boolean rejectOffer(ServerPlayer player) {\n        String key = key(player);\n        Quest removed = offered.remove(key);\n        if (removed == null) return false;\n        offerSerial.merge(key, 1, Integer::sum);\n        setDirty();\n        ArcaneNoticeService.push(player, Component.literal("§7[의뢰 거절] 제안을 돌려보냈습니다."), 80);\n        return true;\n    }\n\n    public long claim(ServerPlayer player, int index) {\n        String key = key(player);\n        List<Quest> list = active.get(key);\n        if (list == null || index < 0 || index >= list.size()) return 0L;\n        Quest quest = list.get(index);\n        if (quest.progress < quest.target) {\n            ArcaneNoticeService.push(player, Component.literal("§7[의뢰] 아직 목표를 완료하지 못했습니다."));\n            return 0L;\n        }\n        long reward = quest.reward;\n        ArcaneWorldData.get(((ServerLevel) player.level()).getServer()).addMarks(player, reward);\n        list.remove(index);\n        if (list.isEmpty()) active.remove(key);\n        setDirty();\n        ArcaneNoticeService.push(player, Component.literal("§6[의뢰 보상] §f+" + reward + " 아르카나"), 110);\n        return reward;\n    }\n\n    /** Compatibility entry point for old callers. */\n    public long claim(ServerPlayer player) {\n        List<QuestStatus> list = statuses(player);\n        for (int i = 0; i < list.size(); i++) if (list.get(i).complete()) return claim(player, i);\n        return 0L;\n    }\n\n    /** Compatibility entry point: creates and immediately accepts one offer. */\n    public QuestStatus assign(ServerPlayer player, int mageCircle) {\n        QuestStatus proposal = offer(player, mageCircle, MagicTradition.UNBOUND);\n        if (proposal.active()) acceptOffer(player);\n        return status(player);\n    }\n\n    public void recordCast(ServerPlayer player, CombatGrowthService.Impact impact, int spellCircle, boolean fusion) {\n        List<Quest> list = active.get(key(player));\n        if (list == null || list.isEmpty()) return;\n        CombatGrowthService.Impact value = impact == null ? CombatGrowthService.Impact.NONE : impact;\n        boolean changed = false;\n        for (Quest quest : list) {\n            if (quest.progress >= quest.target) continue;\n            int delta = switch (quest.id) {\n                case "hits" -> Math.max(0, value.hits());\n                case "kills" -> Math.max(0, value.kills());\n                case "damage" -> Math.max(0, value.damage());\n                case "threat" -> Math.max(0, value.threatPoints());\n                case "fusion" -> fusion && spellCircle >= Math.max(1, quest.circle - 1) ? 1 : 0;\n                default -> spellCircle >= Math.max(1, quest.circle - 1) ? 1 : 0;\n            };\n            if (delta <= 0) continue;\n            int before = quest.progress;\n            quest.progress = Math.min(quest.target, quest.progress + delta);\n            changed |= quest.progress != before;\n            if (before < quest.target && quest.progress >= quest.target) {\n                ArcaneNoticeService.push(player, Component.literal("§6[의뢰 완료] §f" + description(quest.id)\n                        + " §7· 의뢰 탭에서 §6" + quest.reward + " 아르카나§7를 수령하세요."), 140);\n            }\n        }\n        if (changed) setDirty();\n    }\n\n    private List<PlayerEntry> entries() {\n        java.util.Set<String> keys = new java.util.HashSet<>();\n        keys.addAll(active.keySet());\n        keys.addAll(offered.keySet());\n        keys.addAll(offerSerial.keySet());\n        return keys.stream().sorted().map(uuid -> {\n            List<QuestEntry> quests = active.getOrDefault(uuid, List.of()).stream()\n                    .limit(MAX_ACTIVE).map(ArcaneQuestData::encode).toList();\n            QuestEntry proposal = offered.containsKey(uuid) ? encode(offered.get(uuid)) : QuestEntry.EMPTY;\n            Quest legacy = active.getOrDefault(uuid, List.of()).stream().findFirst().orElse(null);\n            return new PlayerEntry(uuid, legacy == null ? "" : legacy.id, legacy == null ? 0 : legacy.target,\n                    legacy == null ? 0 : legacy.progress, legacy == null ? 1 : legacy.circle,\n                    legacy == null ? 0L : legacy.reward, quests, proposal, offerSerial.getOrDefault(uuid, 0));\n        }).toList();\n    }\n\n    private static QuestEntry encode(Quest quest) {\n        return new QuestEntry(quest.id, quest.target, quest.progress, quest.circle, quest.reward, quest.affiliation.name());\n    }\n\n    private static Quest decode(QuestEntry value) {\n        Quest quest = new Quest();\n        if (value == null) return quest;\n        quest.id = normalize(value.id());\n        quest.target = Math.max(0, value.target());\n        quest.progress = Math.max(0, Math.min(quest.target, value.progress()));\n        quest.circle = clamp(value.circle(), 1, 9);\n        quest.reward = Math.max(0L, value.reward());\n        quest.affiliation = MagicTradition.parse(value.affiliation());\n        return quest;\n    }\n\n    private static QuestStatus status(Quest quest) {\n        if (!valid(quest)) return QuestStatus.NONE;\n        return new QuestStatus(true, quest.progress >= quest.target, quest.id, quest.target, quest.progress,\n                quest.circle, quest.reward, description(quest.id), quest.affiliation);\n    }\n\n    private static boolean valid(Quest quest) {\n        return quest != null && !quest.id.isBlank() && quest.target > 0;\n    }\n\n    private static int targetFor(String id, int circle) {\n        return switch (id) {\n            case "hits" -> 10 + circle * 4;\n            case "kills" -> 3 + circle;\n            case "damage" -> 80 + circle * circle * 28;\n            case "threat" -> 22 + circle * circle * 7;\n            case "fusion" -> 2 + Math.max(1, circle / 2);\n            default -> 5 + circle;\n        };\n    }\n\n    private static long baseReward(int circle) {\n        return switch (circle) {\n            case 1 -> 1_200L;\n            case 2 -> 3_000L;\n            case 3 -> 7_000L;\n            case 4 -> 16_000L;\n            case 5 -> 36_000L;\n            case 6 -> 80_000L;\n            case 7 -> 175_000L;\n            case 8 -> 380_000L;\n            case 9 -> 820_000L;\n            default -> 1_200L;\n        };\n    }\n\n    private static String normalize(String id) {\n        return switch (id == null ? "" : id) {\n            case "casts", "hits", "kills", "damage", "threat", "fusion" -> id;\n            default -> "";\n        };\n    }\n\n    private static String description(String id) {\n        return switch (id) {\n            case "hits" -> "마법으로 적을 적중";\n            case "kills" -> "마법으로 적을 처치";\n            case "damage" -> "마법 피해 누적";\n            case "threat" -> "위협도 높은 적과 교전";\n            case "fusion" -> "융합 주문 시전";\n            default -> "요구 써클 이상의 주문 시전";\n        };\n    }\n\n    private static String key(ServerPlayer player) { return player.getUUID().toString(); }\n    private static int clamp(int value, int minimum, int maximum) {\n        return Math.max(minimum, Math.min(maximum, value));\n    }\n\n    public record QuestStatus(boolean active, boolean complete, String id, int target, int progress,\n                              int circle, long reward, String description, MagicTradition affiliation) {\n        public static final QuestStatus NONE = new QuestStatus(\n                false, false, "", 0, 0, 1, 0L, "", MagicTradition.UNBOUND);\n    }\n}\n')

write("src/main/java/kr/moonseungjun/arcanecircle/network/QuestActionPayload.java", 'package kr.moonseungjun.arcanecircle.network;\n\nimport io.netty.buffer.ByteBuf;\nimport kr.moonseungjun.arcanecircle.ArcaneCircle;\nimport net.minecraft.network.codec.ByteBufCodecs;\nimport net.minecraft.network.codec.StreamCodec;\nimport net.minecraft.network.protocol.common.custom.CustomPacketPayload;\nimport net.minecraft.resources.Identifier;\n\npublic record QuestActionPayload(String action) implements CustomPacketPayload {\n    public static final Type<QuestActionPayload> TYPE = new Type<>(\n            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "quest_action"));\n    public static final StreamCodec<ByteBuf, QuestActionPayload> STREAM_CODEC = StreamCodec.composite(\n            ByteBufCodecs.STRING_UTF8, QuestActionPayload::action,\n            QuestActionPayload::new);\n    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }\n}\n')

write("src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java", 'package kr.moonseungjun.arcanecircle.magic;\n\nimport kr.moonseungjun.arcanecircle.registry.ModItems;\nimport net.minecraft.core.BlockPos;\nimport net.minecraft.network.chat.Component;\nimport net.minecraft.server.level.ServerLevel;\nimport net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.world.effect.MobEffectInstance;\nimport net.minecraft.world.effect.MobEffects;\nimport net.minecraft.world.entity.EquipmentSlot;\nimport net.minecraft.world.entity.player.Player;\nimport net.minecraft.world.item.ItemStack;\nimport net.minecraft.world.level.block.Blocks;\n\nimport java.util.HashSet;\nimport java.util.Set;\nimport java.util.UUID;\n\n/** Three equipment lines with three efficiency tiers and a two-slot robe runtime. */\npublic final class MageGearService {\n    private static final String TWO_SLOT_ROBE_RUNTIME = "mage_robe_hem|sage_robe_hem|archmage_robe_hem";\n    private static final Set<UUID> ROBE_SLOT_WARNED = new HashSet<>();\n\n    private MageGearService() {}\n\n    public static void tick(ServerPlayer player) {\n        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);\n        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);\n        int robeTier = robeTier(chest);\n        int hemTier = hemTier(legs);\n\n        if (robeTier > 0 && legs.isEmpty()) {\n            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(hemForTier(robeTier)));\n            hemTier = robeTier;\n            ROBE_SLOT_WARNED.remove(player.getUUID());\n        } else if (robeTier == 0 && hemTier > 0) {\n            player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);\n            ROBE_SLOT_WARNED.remove(player.getUUID());\n            hemTier = 0;\n        } else if (robeTier > 0 && hemTier > 0 && robeTier != hemTier) {\n            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(hemForTier(robeTier)));\n            hemTier = robeTier;\n            ROBE_SLOT_WARNED.remove(player.getUUID());\n        } else if (robeTier > 0 && hemTier == 0 && !legs.isEmpty()\n                && ROBE_SLOT_WARNED.add(player.getUUID())) {\n            ArcaneNoticeService.push(player, Component.literal(\n                    "§c[로브 비활성] §f로브는 몸·바지 두 슬롯을 사용합니다. 현재 바지 장비를 먼저 빼세요."), 110);\n        } else if (robeTier == 0) {\n            ROBE_SLOT_WARNED.remove(player.getUUID());\n        }\n\n        GearStats stats = stats(player);\n        if (stats.boots()) {\n            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, stats.bootsTier() - 1, true, false));\n            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 30, stats.bootsTier() - 1, true, false));\n            if (stats.bootsTier() >= 2 && !player.onGround()) {\n                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30,\n                        stats.bootsTier() >= 3 ? 1 : 0, true, false));\n            }\n            if (stats.bootsTier() >= 3) freezeWater(player);\n        }\n        if (stats.robe()) {\n            int healthAmplifier = switch (stats.robeTier()) {\n                case 2 -> 3;\n                case 3 -> 7;\n                default -> 1;\n            };\n            int resistance = stats.robeTier() >= 3 ? 1 : 0;\n            player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 30, healthAmplifier, true, false));\n            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, resistance, true, false));\n            if (stats.robeTier() >= 3) {\n                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30, 0, true, false));\n            }\n        }\n    }\n\n    public static GearStats stats(Player player) {\n        int hatTier = hatTier(player.getItemBySlot(EquipmentSlot.HEAD));\n        int chestTier = robeTier(player.getItemBySlot(EquipmentSlot.CHEST));\n        int legsTier = hemTier(player.getItemBySlot(EquipmentSlot.LEGS));\n        int robeTier = chestTier > 0 && chestTier == legsTier ? chestTier : 0;\n        int bootsTier = bootsTier(player.getItemBySlot(EquipmentSlot.FEET));\n\n        Piece hat = hat(hatTier);\n        Piece robe = robe(robeTier);\n        Piece boots = boots(bootsTier);\n        return new GearStats(hatTier, robeTier, bootsTier,\n                hat.mana + robe.mana + boots.mana,\n                hat.regen * robe.regen * boots.regen,\n                hat.manaCost * robe.manaCost * boots.manaCost,\n                hat.power * robe.power * boots.power,\n                hat.range * robe.range * boots.range,\n                hat.cooldown * robe.cooldown * boots.cooldown);\n    }\n\n    public static String hatName(Player player) {\n        return switch (hatTier(player.getItemBySlot(EquipmentSlot.HEAD))) {\n            case 1 -> "비전 모자";\n            case 2 -> "현자의 모자";\n            case 3 -> "대마도사 관";\n            default -> "모자 없음";\n        };\n    }\n\n    public static String robeName(Player player) {\n        int chestTier = robeTier(player.getItemBySlot(EquipmentSlot.CHEST));\n        if (chestTier == 0) return "로브 없음";\n        String name = switch (chestTier) {\n            case 2 -> "현자의 로브";\n            case 3 -> "대마도사 예복";\n            default -> "중층 마도 로브";\n        };\n        return stats(player).robeTier() == chestTier ? name : name + " · 바지 슬롯 필요";\n    }\n\n    public static String bootsName(Player player) {\n        return switch (bootsTier(player.getItemBySlot(EquipmentSlot.FEET))) {\n            case 1 -> "유랑 마도화";\n            case 2 -> "천공 마도화";\n            case 3 -> "빙결 보행화";\n            default -> "마도화 없음";\n        };\n    }\n\n    private static int hatTier(ItemStack stack) {\n        if (stack.getItem() == ModItems.ARCHMAGE_CROWN.get()) return 3;\n        if (stack.getItem() == ModItems.SAGE_HAT.get()) return 2;\n        if (stack.getItem() == ModItems.MAGE_HAT.get()) return 1;\n        return 0;\n    }\n\n    private static int robeTier(ItemStack stack) {\n        if (stack.getItem() == ModItems.ARCHMAGE_ROBE.get()) return 3;\n        if (stack.getItem() == ModItems.SAGE_ROBE.get()) return 2;\n        if (stack.getItem() == ModItems.MAGE_ROBE.get()) return 1;\n        return 0;\n    }\n\n    private static int hemTier(ItemStack stack) {\n        if (stack.getItem() == ModItems.ARCHMAGE_ROBE_HEM.get()) return 3;\n        if (stack.getItem() == ModItems.SAGE_ROBE_HEM.get()) return 2;\n        if (stack.getItem() == ModItems.MAGE_ROBE_HEM.get()) return 1;\n        return 0;\n    }\n\n    private static int bootsTier(ItemStack stack) {\n        if (stack.getItem() == ModItems.FROSTSTEP_BOOTS.get()) return 3;\n        if (stack.getItem() == ModItems.SKYWALKER_BOOTS.get()) return 2;\n        if (stack.getItem() == ModItems.MAGE_BOOTS.get()) return 1;\n        return 0;\n    }\n\n    private static net.minecraft.world.item.Item hemForTier(int tier) {\n        return switch (tier) {\n            case 3 -> ModItems.ARCHMAGE_ROBE_HEM.get();\n            case 2 -> ModItems.SAGE_ROBE_HEM.get();\n            default -> ModItems.MAGE_ROBE_HEM.get();\n        };\n    }\n\n    private static Piece hat(int tier) {\n        return switch (tier) {\n            case 1 -> new Piece(90, 1.20, 0.92, 1.03, 1.01, 0.97);\n            case 2 -> new Piece(360, 1.55, 0.78, 1.10, 1.08, 0.84);\n            case 3 -> new Piece(1200, 2.30, 0.52, 1.28, 1.20, 0.58);\n            default -> Piece.NONE;\n        };\n    }\n\n    private static Piece robe(int tier) {\n        return switch (tier) {\n            case 1 -> new Piece(45, 1.08, 0.97, 1.09, 1.03, 0.95);\n            case 2 -> new Piece(260, 1.25, 0.90, 1.28, 1.12, 0.82);\n            case 3 -> new Piece(900, 1.70, 0.75, 1.65, 1.30, 0.60);\n            default -> Piece.NONE;\n        };\n    }\n\n    private static Piece boots(int tier) {\n        return switch (tier) {\n            case 1 -> new Piece(10, 1.03, 0.99, 1.02, 1.07, 0.94);\n            case 2 -> new Piece(90, 1.10, 0.96, 1.08, 1.25, 0.75);\n            case 3 -> new Piece(300, 1.25, 0.90, 1.18, 1.55, 0.48);\n            default -> Piece.NONE;\n        };\n    }\n\n    private static void freezeWater(ServerPlayer player) {\n        if (!(player.level() instanceof ServerLevel level)) return;\n        BlockPos center = player.blockPosition().below();\n        for (int x = -2; x <= 2; x++) {\n            for (int z = -2; z <= 2; z++) {\n                if (x * x + z * z > 6) continue;\n                BlockPos pos = center.offset(x, 0, z);\n                if (level.getBlockState(pos).is(Blocks.WATER)\n                        && level.getBlockState(pos.above()).isAir()) {\n                    level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());\n                }\n            }\n        }\n    }\n\n    public static void clear(UUID playerId) {\n        ROBE_SLOT_WARNED.remove(playerId);\n    }\n\n    private record Piece(int mana, double regen, double manaCost, double power, double range, double cooldown) {\n        private static final Piece NONE = new Piece(0, 1.0, 1.0, 1.0, 1.0, 1.0);\n    }\n\n    public record GearStats(\n            int hatTier,\n            int robeTier,\n            int bootsTier,\n            int maxManaBonus,\n            double regenMultiplier,\n            double manaCostMultiplier,\n            double powerMultiplier,\n            double rangeMultiplier,\n            double cooldownMultiplier\n    ) {\n        public boolean hat() { return hatTier > 0; }\n        public boolean robe() { return robeTier > 0; }\n        public boolean boots() { return bootsTier > 0; }\n    }\n}\n')

# Circle damage grows nonlinearly while utility spells remain utility spells.
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCatalog.java",
    "import java.util.Optional;",
    "import java.util.Optional;\nimport java.util.Set;",
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCatalog.java",
    '''    private static final Map<String, SpellDefinition> SPELLS = new LinkedHashMap<>();
    private static final List<FusionFormula> FUSIONS = new ArrayList<>();''',
    '''    private static final Map<String, SpellDefinition> SPELLS = new LinkedHashMap<>();
    private static final List<FusionFormula> FUSIONS = new ArrayList<>();
    private static final Set<String> DAMAGING_SPELLS = Set.of(
            "magic_missile", "fire_bolt", "ray_of_frost", "thunderwave", "scorching_ray", "shatter",
            "fireball", "lightning_bolt", "vampiric_touch", "sleet_storm", "wall_of_fire", "ice_storm",
            "blight", "phantasmal_killer", "cone_of_cold", "cloudkill", "flame_strike", "insect_plague",
            "disintegrate", "move_earth", "sunbeam", "freezing_sphere", "eyebite", "circle_of_death",
            "delayed_blast_fireball", "finger_of_death", "fire_storm", "prismatic_spray", "simulacrum",
            "antimagic_field", "clone", "control_weather", "dominate_monster", "earthquake", "feeblemind",
            "incendiary_cloud", "maze", "sunburst", "meteor_swarm", "power_word_kill", "prismatic_wall",
            "shapechange", "true_polymorph", "weird", "foresight", "burning_hands", "ice_knife",
            "chromatic_orb", "wind_wall", "fire_shield", "wall_of_ice", "chain_lightning", "arcane_hand",
            "steam_burst", "frost_step", "thunder_cage", "solar_guard", "void_lance", "winter_domain",
            "astral_prison", "phoenix_requiem", "world_sunder");''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCatalog.java",
    '''    public static boolean isFusionResult(String spellId) {
        return spell(spellId).map(spell -> spell.acquisition() == FUSION).orElse(false);
    }
''',
    '''    public static boolean isFusionResult(String spellId) {
        return spell(spellId).map(spell -> spell.acquisition() == FUSION).orElse(false);
    }

    public static boolean isDamaging(String spellId) {
        return spellId != null && DAMAGING_SPELLS.contains(spellId);
    }

    public static double damageTierMultiplier(int circle) {
        return switch (circle) {
            case 2 -> 1.18;
            case 3 -> 1.42;
            case 4 -> 1.75;
            case 5 -> 2.15;
            case 6 -> 2.65;
            case 7 -> 3.25;
            case 8 -> 4.00;
            case 9 -> 5.00;
            default -> 1.00;
        };
    }
''',
)

# High circle mages compress low-circle mana and cooldown costs down to real zero.
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java",
    '''        double regen = state.baseRegenPerHalfSecond() * staff.regenMultiplier() * gear.regenMultiplier();''',
    '''        kr.moonseungjun.arcanecircle.world.MagicTradition chosen =
                kr.moonseungjun.arcanecircle.world.ArcaneWorldData.get(((ServerLevel) player.level()).getServer())
                        .tradition(player);
        double regen = state.baseRegenPerHalfSecond() * staff.regenMultiplier() * gear.regenMultiplier()
                * chosen.regenMultiplier();''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java",
    '''        // Affiliation is a social team, not a spell school. It never locks or buffs a school directly.
        double facultyMana = 1.0;
        double facultyPower = 1.0;
        double facultyRange = 1.0;
        double facultyCooldown = 1.0;''',
    '''        // Affiliations never lock a school, but each teaches an explicit doctrine with a drawback.
        double facultyMana = chosen.manaMultiplier();
        double facultyPower = chosen.powerFor(spell.school());
        double facultyRange = chosen.rangeMultiplier();
        double facultyCooldown = chosen.cooldownMultiplier();''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java",
    '''        double circleMana = Math.max(0.48, 1.0 - masteryGap * 0.09);
        double circleCooldown = Math.max(0.38, 1.0 - masteryGap * 0.14);
        double circleRange = 1.0 + masteryGap * 0.08;
        double circlePower = 1.0 + masteryGap * 0.10;''',
    '''        double circleMana = Math.max(0.06, Math.pow(0.72, masteryGap));
        double circleCooldown = Math.max(0.08, Math.pow(0.62, masteryGap));
        double circleRange = 1.0 + masteryGap * 0.07;
        double circlePower = 1.0 + masteryGap * 0.04;''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java",
    '''        int cooldown = Math.max(8, (int) Math.round(spell.cooldownTicks() * circleCooldown * masteryCooldown
                * staff.cooldownMultiplier() * gear.cooldownMultiplier() * facultyCooldown));''',
    '''        double rawCooldown = spell.cooldownTicks() * circleCooldown * masteryCooldown
                * staff.cooldownMultiplier() * gear.cooldownMultiplier() * facultyCooldown;
        int cooldown = rawCooldown < 0.75 ? 0 : Math.max(1, (int) Math.round(rawCooldown));''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java",
    '''        double power = spell.power() * circlePower * masteryPower * staff.powerFor(spell.school())
                * gear.powerMultiplier() * facultyPower;''',
    '''        double tierPower = SpellCatalog.isDamaging(spell.id())
                ? SpellCatalog.damageTierMultiplier(spell.circle()) : 1.0;
        double power = spell.power() * tierPower * circlePower * masteryPower * staff.powerFor(spell.school())
                * gear.powerMultiplier() * facultyPower;''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java",
    '''                double ingredientPower = ingredient.power()
                        * (1.0 + ingredientGap * 0.10)
                        * (1.0 + ingredientTier * 0.04)
                        * staff.powerFor(ingredient.school())
                        * gear.powerMultiplier();''',
    '''                double ingredientPower = ingredient.power()
                        * (SpellCatalog.isDamaging(ingredient.id())
                                ? SpellCatalog.damageTierMultiplier(ingredient.circle()) : 1.0)
                        * (1.0 + ingredientGap * 0.04)
                        * (1.0 + ingredientTier * 0.04)
                        * staff.powerFor(ingredient.school())
                        * gear.powerMultiplier()
                        * chosen.powerFor(ingredient.school());''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java",
    '''            double fusionFloor = strongestIngredient * (ingredients.size() >= 3 ? 1.45 : 1.25);
            power = Math.max(power, fusionFloor);''',
    '''            double fusionFloor = strongestIngredient * (ingredients.size() >= 3 ? 1.55 : 1.32)
                    * chosen.fusionMultiplier();
            power = Math.max(power * chosen.fusionMultiplier(), fusionFloor);''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java",
    '''    public void startCooldown(ServerPlayer player, String spellId, int totalTicks) {
        MageState state = state(player);
        int total = Math.max(1, totalTicks);
        state.cooldowns.put(spellId, new CooldownEntry(spellId, serverClock(player) + total, total));
        setDirty();
    }''',
    '''    public void startCooldown(ServerPlayer player, String spellId, int totalTicks) {
        MageState state = state(player);
        if (totalTicks <= 0) {
            if (state.cooldowns.remove(spellId) != null) setDirty();
            return;
        }
        int total = totalTicks;
        state.cooldowns.put(spellId, new CooldownEntry(spellId, serverClock(player) + total, total));
        setDirty();
    }''',
)

# Zero-time casts wait only for the key release so a tap casts while a held key can still become a fusion chord.
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java",
    '''        if (required <= 0) {
            CHARGES.remove(player.getUUID());
            castPrepared(player, data, cast);
            return;
        }

        ChargeState charge = new ChargeState(slot, cast.spell().id(), serverClock(player), required);''',
    '''        if (required <= 0) {
            CHARGES.put(player.getUUID(), new ChargeState(slot, cast.spell().id(), serverClock(player), 0));
            return;
        }

        ChargeState charge = new ChargeState(slot, cast.spell().id(), serverClock(player), required);''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java",
    '''        if ((elapsed & 1L) == 0L) {
            WorldMagicService.charge(player, spell, false, List.of(), cast.range(),
                    Math.min(1.0, elapsed / (double) Math.max(1, charge.requiredTicks)));
        }''',
    '''        if (charge.requiredTicks <= 0) return;
        if ((elapsed & 1L) == 0L) {
            WorldMagicService.charge(player, spell, false, List.of(), cast.range(),
                    Math.min(1.0, elapsed / (double) Math.max(1, charge.requiredTicks)));
        }''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java",
    '''        int calculated = (int) Math.round(sameCircleTicks[circle] * gapScale * masteryScale);
        return Math.max(minimumTicks[circle], calculated);''',
    '''        int calculated = (int) Math.round(sameCircleTicks[circle] * gapScale * masteryScale);
        int instantGap = circle <= 2 ? 4 : circle <= 4 ? 5 : 99;
        if (circleGap >= instantGap || (circleGap >= 3 && masteryTier >= 8)) return 0;
        int bounded = Math.max(minimumTicks[circle], calculated);
        kr.moonseungjun.arcanecircle.world.MagicTradition chosen =
                kr.moonseungjun.arcanecircle.world.ArcaneWorldData.get(((ServerLevel) player.level()).getServer())
                        .tradition(player);
        int result = (int) Math.round(bounded * chosen.castTimeMultiplier());
        return result <= 1 ? 0 : result;''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java",
    '''        return Math.max(minimum, calculated);
    }

    private static String fusionCooldownBlock''',
    '''        int resultTicks = Math.max(minimum, calculated);
        if (registered && result.circle() <= 3 && masteryTier >= 8) return 0;
        return resultTicks;
    }

    private static String fusionCooldownBlock''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java",
    '''        ArcaneQuestData.get(((ServerLevel) player.level()).getServer()).recordCast(player, impact, spell.circle());''',
    '''        ArcaneQuestData.get(((ServerLevel) player.level()).getServer())
                .recordCast(player, impact, spell.circle(), cast.fusion());''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java",
    '''                    + (int) state.mana() + "/" + stats.maxMana() + " · 쿨 "
                    + String.format("%.1f", cast.cooldownTicks() / 20.0) + "초"));''',
    '''                    + (int) state.mana() + "/" + stats.maxMana() + " · 쿨 "
                    + (cast.cooldownTicks() <= 0 ? "없음" : String.format("%.1f", cast.cooldownTicks() / 20.0) + "초")));''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java",
    '''                    + " · 처치 " + impact.kills() + threat + " §7· 숙련 +" + impact.masteryGain()
                    + " · 통찰 +" + impact.insightGain() + " · 아르카나 +" + marksEarned));''',
    '''                    + " · 처치 " + impact.kills() + threat + " §7· 최고 위협 " + impact.peakThreat()
                    + " · 숙련 +" + impact.masteryGain() + " · 통찰 +" + impact.insightGain()
                    + " · 아르카나 +" + marksEarned));''',
)

replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEconomyService.java",
    '''        long reward = Math.max(1L, spellCircle)
                * (impact.hits() * 2L + impact.kills() * 14L + impact.strongHits() * 18L
                + impact.strongKills() * 120L + Math.max(0, impact.damage()) / 8L);
        reward = Math.max(1L, reward);''',
    '''        MagicTradition affiliation = ArcaneMageService.affiliation(player);
        long base = impact.hits() * 3L + impact.kills() * 18L
                + Math.max(0, impact.damage()) / 5L
                + Math.max(0, impact.threatPoints())
                + Math.max(0L, impact.combatValue());
        long reward = Math.max(1L, Math.round(Math.max(1, spellCircle) * base
                * affiliation.combatRewardMultiplier()));''',
)

# Register two additional tiers in each equipment line.
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java",
    '''    public static final DeferredItem<Item> MAGE_BOOTS = ITEMS.registerItem("mage_boots",
            properties -> new Item(properties.rarity(Rarity.UNCOMMON)
                    .humanoidArmor(ArmorMaterials.LEATHER, ArmorType.BOOTS)));
''',
    '''    public static final DeferredItem<Item> MAGE_BOOTS = ITEMS.registerItem("mage_boots",
            properties -> new Item(properties.rarity(Rarity.UNCOMMON)
                    .humanoidArmor(ArmorMaterials.LEATHER, ArmorType.BOOTS)));
    public static final DeferredItem<Item> SAGE_HAT = ITEMS.registerItem("sage_hat",
            properties -> new Item(properties.rarity(Rarity.RARE)
                    .humanoidArmor(ArmorMaterials.LEATHER, ArmorType.HELMET)));
    public static final DeferredItem<Item> SAGE_ROBE = ITEMS.registerItem("sage_robe",
            properties -> new Item(properties.rarity(Rarity.EPIC)
                    .humanoidArmor(ArmorMaterials.LEATHER, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> SAGE_ROBE_HEM = ITEMS.registerItem("sage_robe_hem",
            properties -> new Item(properties.rarity(Rarity.EPIC)
                    .humanoidArmor(ArmorMaterials.LEATHER, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> SKYWALKER_BOOTS = ITEMS.registerItem("skywalker_boots",
            properties -> new Item(properties.rarity(Rarity.RARE)
                    .humanoidArmor(ArmorMaterials.LEATHER, ArmorType.BOOTS)));
    public static final DeferredItem<Item> ARCHMAGE_CROWN = ITEMS.registerItem("archmage_crown",
            properties -> new Item(properties.rarity(Rarity.EPIC)
                    .humanoidArmor(ArmorMaterials.GOLD, ArmorType.HELMET)));
    public static final DeferredItem<Item> ARCHMAGE_ROBE = ITEMS.registerItem("archmage_robe",
            properties -> new Item(properties.rarity(Rarity.EPIC)
                    .humanoidArmor(ArmorMaterials.LEATHER, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> ARCHMAGE_ROBE_HEM = ITEMS.registerItem("archmage_robe_hem",
            properties -> new Item(properties.rarity(Rarity.EPIC)
                    .humanoidArmor(ArmorMaterials.LEATHER, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> FROSTSTEP_BOOTS = ITEMS.registerItem("froststep_boots",
            properties -> new Item(properties.rarity(Rarity.EPIC)
                    .humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.BOOTS)));
''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java",
    '''            event.accept(MAGE_HAT.get());
            event.accept(MAGE_ROBE.get());
            event.accept(MAGE_BOOTS.get());''',
    '''            event.accept(MAGE_HAT.get());
            event.accept(MAGE_ROBE.get());
            event.accept(MAGE_BOOTS.get());
            event.accept(SAGE_HAT.get());
            event.accept(SAGE_ROBE.get());
            event.accept(SKYWALKER_BOOTS.get());
            event.accept(ARCHMAGE_CROWN.get());
            event.accept(ARCHMAGE_ROBE.get());
            event.accept(FROSTSTEP_BOOTS.get());''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java",
    '''        return switch (id) {
            case "mage_hat" -> MAGE_HAT;
            case "mage_robe" -> MAGE_ROBE;
            case "mage_boots" -> MAGE_BOOTS;
            default -> MAGE_HAT;
        };''',
    '''        return switch (id) {
            case "mage_hat" -> MAGE_HAT;
            case "mage_robe" -> MAGE_ROBE;
            case "mage_boots" -> MAGE_BOOTS;
            case "sage_hat" -> SAGE_HAT;
            case "sage_robe" -> SAGE_ROBE;
            case "skywalker_boots" -> SKYWALKER_BOOTS;
            case "archmage_crown" -> ARCHMAGE_CROWN;
            case "archmage_robe" -> ARCHMAGE_ROBE;
            case "froststep_boots" -> FROSTSTEP_BOOTS;
            default -> MAGE_HAT;
        };''',
)

replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/world/AcademyOfferCatalog.java",
    '''        result.add(new Offer("gear:mage_robe", "중층 마도 로브", "몸과 바지 슬롯을 함께 사용하며 생존력과 주문 위력을 높입니다.",
                3, 7200L, Kind.GEAR, "mage_robe"));''',
    '''        result.add(new Offer("gear:mage_robe", "중층 마도 로브", "몸과 바지 슬롯을 함께 사용하며 생존력과 주문 위력을 높입니다.",
                3, 7200L, Kind.GEAR, "mage_robe"));
        result.add(new Offer("gear:sage_hat", "현자의 모자", "고위 마력 운용과 회복 효율을 크게 높이는 2단계 모자.",
                5, 55_000L, Kind.GEAR, "sage_hat"));
        result.add(new Offer("gear:skywalker_boots", "천공 마도화", "높은 점프와 체공, 빠른 이동을 제공하는 2단계 신발.",
                5, 75_000L, Kind.GEAR, "skywalker_boots"));
        result.add(new Offer("gear:sage_robe", "현자의 로브", "몸·바지 두 칸을 사용하며 마력·방어·주문 효율을 크게 증폭합니다.",
                6, 160_000L, Kind.GEAR, "sage_robe"));
        result.add(new Offer("gear:archmage_crown", "대마도사 관", "극한의 마력량과 회복·효율을 제공하는 최상위 모자.",
                8, 1_100_000L, Kind.GEAR, "archmage_crown"));
        result.add(new Offer("gear:froststep_boots", "빙결 보행화", "장시간 체공하고 물 위에 얼음 길을 만드는 최상위 신발.",
                8, 1_400_000L, Kind.GEAR, "froststep_boots"));
        result.add(new Offer("gear:archmage_robe", "대마도사 예복", "몸·바지 두 칸을 대가로 생존력과 주문 효율을 압도적으로 높입니다.",
                9, 3_200_000L, Kind.GEAR, "archmage_robe"));''',
)

# Resident interaction now creates a reviewable offer instead of silently accepting it.
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java",
    '''        ArcaneQuestData quests = ArcaneQuestData.get(((ServerLevel) player.level()).getServer());
        ArcaneQuestData.QuestStatus status = quests.status(player);
        if (status.complete()) quests.claim(player);
        else if (!status.active()) quests.assign(player, mage.circle());
        else {
            ArcaneNoticeService.push(player, Component.literal("§5[마도사 의뢰] §f" + status.description()
                    + " §d" + status.progress() + "/" + status.target() + " §7· 보상 " + status.reward() + " A"), 90);
        }
''',
    '''        ArcaneQuestData quests = ArcaneQuestData.get(((ServerLevel) player.level()).getServer());
        quests.offer(player, mage.circle(), mage.affiliation());
''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java",
    '''        ArcaneNetwork.openPage(player, "academy");''',
    '''        ArcaneNetwork.openPage(player, "quests");''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java",
    '''    private static LivingEntity findResidentTarget(ServerLevel level, Villager caster, MageProfile mage) {
        LivingEntity hostileMage = level.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(15.0),''',
    '''    private static LivingEntity findResidentTarget(ServerLevel level, Villager caster, MageProfile mage) {
        LivingEntity attacker = recentAttacker(caster);
        if (attacker != null && caster.distanceToSqr(attacker) <= 32.0 * 32.0) return attacker;
        LivingEntity hostileMage = level.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(15.0),''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java",
    '''        LivingEntity target = caster.getTarget();
        if (target == null || !target.isAlive()) return;
        MagicTradition targetAffiliation = target instanceof ServerPlayer player''',
    '''        LivingEntity attacker = recentAttacker(caster);
        LivingEntity target = attacker != null ? attacker : caster.getTarget();
        if (target == null || !target.isAlive()) return;
        boolean retaliating = attacker != null && target == attacker;
        if (retaliating && caster.getTarget() != target) caster.setTarget(target);
        MagicTradition targetAffiliation = target instanceof ServerPlayer player''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java",
    '''        if (MageSociety.avoidsAutoTarget(mage.affiliation(), targetAffiliation)) {
            caster.setTarget(null);
            return;
        }
        if (isMage(target) && !MageSociety.hostile(mage.affiliation(), targetAffiliation)
                && mage.role() != MageSociety.Role.VILLAIN) return;''',
    '''        if (!retaliating && MageSociety.avoidsAutoTarget(mage.affiliation(), targetAffiliation)) {
            caster.setTarget(null);
            return;
        }
        if (!retaliating && isMage(target) && !MageSociety.hostile(mage.affiliation(), targetAffiliation)
                && mage.role() != MageSociety.Role.VILLAIN) return;''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java",
    '''        int interval = Math.max(34, 92 - mage.circle() * 7);''',
    '''        int interval = Math.max(22, (int) Math.round((92 - mage.circle() * 7)
                * mage.affiliation().cooldownMultiplier()));''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java",
    '''        float damage = (1.8F + mage.circle() * 1.25F) * roleScale;''',
    '''        float damage = (float) ((1.8F + mage.circle() * 1.25F) * roleScale
                * mage.affiliation().powerMultiplier());''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java",
    '''        if (!ready(caster, now, Math.max(30, 94 - mage.circle() * 7))) return;
        float roleScale = mage.role() == MageSociety.Role.VILLAIN ? 1.30F : 1.0F;
        ArcaneDamage.hurt(level, caster, target, (2.0F + mage.circle() * 1.28F) * roleScale);''',
    '''        int interval = Math.max(20, (int) Math.round((94 - mage.circle() * 7)
                * mage.affiliation().cooldownMultiplier()));
        if (!ready(caster, now, interval)) return;
        float roleScale = mage.role() == MageSociety.Role.VILLAIN ? 1.30F : 1.0F;
        float doctrineScale = (float) mage.affiliation().powerMultiplier();
        ArcaneDamage.hurt(level, caster, target,
                (2.0F + mage.circle() * 1.28F) * roleScale * doctrineScale);''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java",
    '''    private static void pushAway(LivingEntity caster, LivingEntity target, double strength) {''',
    '''    private static LivingEntity recentAttacker(LivingEntity caster) {
        LivingEntity attacker = caster.getLastHurtByMob();
        if (attacker == null || !attacker.isAlive() || attacker == caster) return null;
        return attacker;
    }

    private static void pushAway(LivingEntity caster, LivingEntity target, double strength) {''',
)

write("src/main/java/kr/moonseungjun/arcanecircle/network/ArcaneNetwork.java", 'package kr.moonseungjun.arcanecircle.network;\n\nimport kr.moonseungjun.arcanecircle.item.ArcaneStaffItem.StaffProfile;\nimport kr.moonseungjun.arcanecircle.magic.ArcaneNoticeService;\nimport kr.moonseungjun.arcanecircle.magic.MagicPlayerData;\nimport kr.moonseungjun.arcanecircle.magic.MageGearService;\nimport kr.moonseungjun.arcanecircle.magic.SpellCastingService;\nimport kr.moonseungjun.arcanecircle.magic.SpellCatalog;\nimport kr.moonseungjun.arcanecircle.world.ArcaneEconomyService;\nimport kr.moonseungjun.arcanecircle.world.ArcaneQuestData;\nimport net.minecraft.network.chat.Component;\nimport net.minecraft.server.level.ServerLevel;\nimport net.minecraft.server.level.ServerPlayer;\nimport net.neoforged.neoforge.network.PacketDistributor;\nimport net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;\nimport net.neoforged.neoforge.network.handling.IPayloadContext;\nimport net.neoforged.neoforge.network.registration.PayloadRegistrar;\n\nimport java.util.List;\nimport java.util.Set;\nimport java.util.stream.Collectors;\n\npublic final class ArcaneNetwork {\n    public static final String PROTOCOL_VERSION = "ninefold-arcana-12-1-alpha7";\n    private static final Set<String> PAGES = Set.of(\n            "atlas", "recipes", "staffs", "core", "academy", "quests", "sync");\n\n    private ArcaneNetwork() {}\n\n    public static void register(RegisterPayloadHandlersEvent event) {\n        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);\n        registrar.playToClient(GrimoireSnapshotPayload.TYPE, GrimoireSnapshotPayload.STREAM_CODEC);\n        registrar.playToClient(WorldMagicPayload.TYPE, WorldMagicPayload.STREAM_CODEC);\n        registrar.playToServer(RequestGrimoirePayload.TYPE, RequestGrimoirePayload.STREAM_CODEC, ArcaneNetwork::handleRequest);\n        registrar.playToServer(BeginCastPayload.TYPE, BeginCastPayload.STREAM_CODEC, ArcaneNetwork::handleBeginCast);\n        registrar.playToServer(ReleaseCastPayload.TYPE, ReleaseCastPayload.STREAM_CODEC, ArcaneNetwork::handleReleaseCast);\n        registrar.playToServer(QueueFusionPayload.TYPE, QueueFusionPayload.STREAM_CODEC, ArcaneNetwork::handleQueueFusion);\n        registrar.playToServer(CommitFusionPayload.TYPE, CommitFusionPayload.STREAM_CODEC, ArcaneNetwork::handleCommitFusion);\n        registrar.playToServer(EquipSpellPayload.TYPE, EquipSpellPayload.STREAM_CODEC, ArcaneNetwork::handleEquip);\n        registrar.playToServer(PurchaseAcademyItemPayload.TYPE, PurchaseAcademyItemPayload.STREAM_CODEC,\n                ArcaneNetwork::handlePurchase);\n        registrar.playToServer(ChooseTraditionPayload.TYPE, ChooseTraditionPayload.STREAM_CODEC,\n                ArcaneNetwork::handleTradition);\n        registrar.playToServer(QuestActionPayload.TYPE, QuestActionPayload.STREAM_CODEC, ArcaneNetwork::handleQuest);\n    }\n\n    public static void sync(ServerPlayer player) {\n        PacketDistributor.sendToPlayer(player, snapshot(player, "sync"));\n    }\n\n    public static void openPage(ServerPlayer player, String page) {\n        String requested = PAGES.contains(page) && !"sync".equals(page) ? page : "academy";\n        PacketDistributor.sendToPlayer(player, snapshot(player, requested));\n    }\n\n    private static void handleRequest(RequestGrimoirePayload payload, IPayloadContext context) {\n        ServerPlayer player = requirePlayer(context);\n        if (player == null) return;\n        String requested = "mastery".equals(payload.page()) ? "recipes" : payload.page();\n        String page = PAGES.contains(requested) && !"sync".equals(requested) ? requested : "atlas";\n        context.reply(snapshot(player, page));\n    }\n\n    private static void handleBeginCast(BeginCastPayload payload, IPayloadContext context) {\n        ServerPlayer player = requirePlayer(context);\n        if (player == null) return;\n        SpellCastingService.beginSlotCharge(player, payload.slot());\n        context.reply(snapshot(player, "sync"));\n    }\n\n    private static void handleReleaseCast(ReleaseCastPayload payload, IPayloadContext context) {\n        ServerPlayer player = requirePlayer(context);\n        if (player == null) return;\n        SpellCastingService.releaseSlotCharge(player, payload.slot());\n        context.reply(snapshot(player, "sync"));\n    }\n\n    private static void handleQueueFusion(QueueFusionPayload payload, IPayloadContext context) {\n        ServerPlayer player = requirePlayer(context);\n        if (player == null) return;\n        SpellCastingService.queueFusionSlot(player, payload.slot());\n        context.reply(snapshot(player, "sync"));\n    }\n\n    private static void handleCommitFusion(CommitFusionPayload payload, IPayloadContext context) {\n        ServerPlayer player = requirePlayer(context);\n        if (player == null) return;\n        if (payload.action() == 0) SpellCastingService.commitFusion(player);\n        else {\n            SpellCastingService.clearFusion(player, true);\n            SpellCastingService.cancelCharge(player, true);\n        }\n        context.reply(snapshot(player, "sync"));\n    }\n\n    private static void handlePurchase(PurchaseAcademyItemPayload payload, IPayloadContext context) {\n        ServerPlayer player = requirePlayer(context);\n        if (player == null) return;\n        ArcaneEconomyService.purchase(player, payload.offerId());\n        context.reply(snapshot(player, "academy"));\n    }\n\n    private static void handleTradition(ChooseTraditionPayload payload, IPayloadContext context) {\n        ServerPlayer player = requirePlayer(context);\n        if (player == null) return;\n        ArcaneEconomyService.chooseTradition(player, payload.traditionId());\n        context.reply(snapshot(player, "academy"));\n    }\n\n    private static void handleQuest(QuestActionPayload payload, IPayloadContext context) {\n        ServerPlayer player = requirePlayer(context);\n        if (player == null) return;\n        ArcaneQuestData quests = ArcaneQuestData.get(((ServerLevel) player.level()).getServer());\n        String action = payload.action() == null ? "" : payload.action();\n        if ("accept".equals(action)) quests.acceptOffer(player);\n        else if ("reject".equals(action)) quests.rejectOffer(player);\n        else if (action.startsWith("claim:")) {\n            try { quests.claim(player, Integer.parseInt(action.substring("claim:".length()))); }\n            catch (NumberFormatException ignored) {\n                ArcaneNoticeService.push(player, Component.literal("§c[의뢰] 잘못된 보상 요청입니다."));\n            }\n        }\n        context.reply(snapshot(player, "quests"));\n    }\n\n    private static void handleEquip(EquipSpellPayload payload, IPayloadContext context) {\n        ServerPlayer player = requirePlayer(context);\n        if (player == null) return;\n        boolean selected = data(player).selectSpell(player, payload.slot(), payload.spellId());\n        if (!selected) {\n            ArcaneNoticeService.push(player, Component.literal(\n                    "§c[마도서] §f현재 써클에서 사용할 수 없거나 아직 각인되지 않은 주문입니다."));\n        }\n        context.reply(snapshot(player, "sync"));\n    }\n\n    private static MagicPlayerData data(ServerPlayer player) {\n        return MagicPlayerData.get(((ServerLevel) player.level()).getServer());\n    }\n\n    private static ServerPlayer requirePlayer(IPayloadContext context) {\n        if (context.player() instanceof ServerPlayer player) return player;\n        context.disconnect(Component.literal("잘못된 마도서 요청입니다."));\n        return null;\n    }\n\n    public static GrimoireSnapshotPayload snapshot(ServerPlayer player, String page) {\n        MagicPlayerData magicData = data(player);\n        MagicPlayerData.MageState state = magicData.state(player);\n        MagicPlayerData.EffectiveStats stats = magicData.effectiveStats(player);\n        StaffProfile staff = stats.staff();\n        MageGearService.GearStats gear = MageGearService.stats(player);\n        ArcaneQuestData questData = ArcaneQuestData.get(((ServerLevel) player.level()).getServer());\n        List<ArcaneQuestData.QuestStatus> quests = questData.statuses(player);\n        ArcaneQuestData.QuestStatus offered = questData.offerStatus(player);\n        ArcaneQuestData.QuestStatus legacyQuest = quests.isEmpty() ? ArcaneQuestData.QuestStatus.NONE : quests.getFirst();\n        String known = state.known().stream().sorted().collect(Collectors.joining("|"));\n        String mastery = SpellCatalog.spells().values().stream()\n                .map(spell -> spell.id() + ":" + state.mastery(spell.id()))\n                .collect(Collectors.joining("|"));\n        String slots = String.join("|", state.slots());\n        List<String> queue = SpellCastingService.pendingFusion(player);\n        String queued = String.join("|", queue);\n        String result = SpellCatalog.fusionFor(queue).map(SpellCatalog.FusionFormula::result).orElse("");\n        String candidates = SpellCatalog.candidatesFor(queue).stream()\n                .map(SpellCatalog.FusionFormula::result)\n                .collect(Collectors.joining("|"));\n        String snapshot = "circle=" + state.circle()\n                + ";mana=" + (int) state.mana()\n                + ";max=" + stats.maxMana()\n                + ";regen_milli=" + (int) Math.round(stats.regenPerHalfSecond() * 2000.0)\n                + ";insight=" + state.insight()\n                + ";next=" + state.nextCircleInsight()\n                + ";slots=" + slots\n                + ";charging=" + SpellCastingService.chargingSpell(player)\n                + ";charging_slot=" + SpellCastingService.chargingSlot(player)\n                + ";charge_ticks=" + SpellCastingService.chargingTicks(player)\n                + ";charge_required=" + SpellCastingService.chargingRequiredTicks(player)\n                + ";queue=" + queued\n                + ";queue_result=" + result\n                + ";queue_candidates=" + candidates\n                + ";queue_extend=" + (SpellCatalog.canExtend(queue) ? 1 : 0)\n                + ";fusion_charging=" + SpellCastingService.fusionChargingSpell(player)\n                + ";fusion_charge_ticks=" + SpellCastingService.fusionChargingTicks(player)\n                + ";fusion_charge_required=" + SpellCastingService.fusionChargingRequiredTicks(player)\n                + ";cooldowns=" + magicData.cooldownSnapshot(player)\n                + ";staff_id=" + staff.id()\n                + ";staff=" + staff.displayName()\n                + ";staff_summary=" + staff.summary()\n                + ";staff_mana=" + staff.maxManaBonus()\n                + ";staff_cost=" + permille(staff.manaCostMultiplier())\n                + ";staff_power=" + permille(staff.powerMultiplier())\n                + ";staff_range=" + permille(staff.rangeMultiplier())\n                + ";staff_cooldown=" + permille(staff.cooldownMultiplier())\n                + ";staff_regen=" + permille(staff.regenMultiplier())\n                + ";gear_hat=" + MageGearService.hatName(player)\n                + ";gear_robe=" + MageGearService.robeName(player)\n                + ";gear_boots=" + MageGearService.bootsName(player)\n                + ";gear_mana=" + gear.maxManaBonus()\n                + ";gear_regen=" + permille(gear.regenMultiplier())\n                + ";marks=" + ArcaneEconomyService.balance(player)\n                + ";tradition=" + kr.moonseungjun.arcanecircle.world.ArcaneWorldData\n                        .get(((ServerLevel) player.level()).getServer()).tradition(player).name()\n                + ";known=" + known\n                + ";mastery=" + mastery\n                + ";notice_seq=" + ArcaneNoticeService.sequence(player)\n                + ";notice_ttl=" + ArcaneNoticeService.ttl(player)\n                + ";notice=" + ArcaneNoticeService.text(player)\n                + ";quest_id=" + legacyQuest.id()\n                + ";quest_target=" + legacyQuest.target()\n                + ";quest_progress=" + legacyQuest.progress()\n                + ";quest_circle=" + legacyQuest.circle()\n                + ";quest_reward=" + legacyQuest.reward()\n                + ";quest_desc=" + legacyQuest.description()\n                + ";" + questSnapshot(offered, quests)\n                + ";spell_count=" + SpellCatalog.spells().size();\n        return new GrimoireSnapshotPayload(page, snapshot);\n    }\n\n    private static String questSnapshot(ArcaneQuestData.QuestStatus offered,\n                                        List<ArcaneQuestData.QuestStatus> quests) {\n        StringBuilder result = new StringBuilder();\n        result.append("quest_count=").append(Math.min(ArcaneQuestData.MAX_ACTIVE, quests.size()));\n        appendQuest(result, "quest_offer", offered);\n        for (int index = 0; index < ArcaneQuestData.MAX_ACTIVE; index++) {\n            ArcaneQuestData.QuestStatus quest = index < quests.size()\n                    ? quests.get(index) : ArcaneQuestData.QuestStatus.NONE;\n            appendQuest(result, "quest_" + index, quest);\n        }\n        return result.toString();\n    }\n\n    private static void appendQuest(StringBuilder result, String prefix, ArcaneQuestData.QuestStatus quest) {\n        result.append(\';\').append(prefix).append("_id=").append(quest.id())\n                .append(\';\').append(prefix).append("_target=").append(quest.target())\n                .append(\';\').append(prefix).append("_progress=").append(quest.progress())\n                .append(\';\').append(prefix).append("_circle=").append(quest.circle())\n                .append(\';\').append(prefix).append("_reward=").append(quest.reward())\n                .append(\';\').append(prefix).append("_desc=").append(quest.description())\n                .append(\';\').append(prefix).append("_affiliation=").append(quest.affiliation().name());\n    }\n\n    private static int permille(double value) {\n        return (int) Math.round(value * 1000.0);\n    }\n}\n')

# Dedicated quest tab, explicit accept/reject, and visible faction doctrines.
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    "import kr.moonseungjun.arcanecircle.network.PurchaseAcademyItemPayload;\nimport kr.moonseungjun.arcanecircle.network.RequestGrimoirePayload;",
    "import kr.moonseungjun.arcanecircle.network.PurchaseAcademyItemPayload;\n"
    "import kr.moonseungjun.arcanecircle.network.QuestActionPayload;\n"
    "import kr.moonseungjun.arcanecircle.network.RequestGrimoirePayload;",
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''    private static final List<Tab> TABS = List.of(
            new Tab("atlas", "주문"), new Tab("recipes", "융합"), new Tab("staffs", "지팡이"),
            new Tab("academy", "마도회"), new Tab("core", "마력핵"));''',
    '''    private static final List<Tab> TABS = List.of(
            new Tab("atlas", "주문"), new Tab("recipes", "융합"), new Tab("staffs", "지팡이"),
            new Tab("academy", "마도회"), new Tab("quests", "의뢰"), new Tab("core", "마력핵"));''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''        if ("academy".equals(page)) return clickAcademy(event, l) || super.mouseClicked(event, doubleClick);
        if ("staffs".equals(page)) return clickStaffs(event, l) || super.mouseClicked(event, doubleClick);''',
    '''        if ("academy".equals(page)) return clickAcademy(event, l) || super.mouseClicked(event, doubleClick);
        if ("quests".equals(page)) return clickQuests(event, l) || super.mouseClicked(event, doubleClick);
        if ("staffs".equals(page)) return clickStaffs(event, l) || super.mouseClicked(event, doubleClick);''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''    private boolean clickStaffs(MouseButtonEvent event, Layout l) {''',
    '''    private boolean clickQuests(MouseButtonEvent event, Layout l) {
        String offered = ArcaneClientState.text("quest_offer_id", "");
        if (!offered.isBlank()) {
            if (inside(event.x(), event.y(), l.questAccept())) {
                ClientPacketDistributor.sendToServer(new QuestActionPayload("accept"));
                notice("의뢰 수락 요청");
                return true;
            }
            if (inside(event.x(), event.y(), l.questReject())) {
                ClientPacketDistributor.sendToServer(new QuestActionPayload("reject"));
                notice("의뢰 거절 요청");
                return true;
            }
        }
        int count = Math.min(3, ArcaneClientState.integer("quest_count", 0));
        for (int i = 0; i < count; i++) {
            int progress = ArcaneClientState.integer("quest_" + i + "_progress", 0);
            int target = ArcaneClientState.integer("quest_" + i + "_target", 0);
            if (target > 0 && progress >= target && inside(event.x(), event.y(), l.questClaim(i))) {
                ClientPacketDistributor.sendToServer(new QuestActionPayload("claim:" + i));
                notice((i + 1) + "번 의뢰 보상 수령 요청");
                return true;
            }
        }
        return false;
    }

    private boolean clickStaffs(MouseButtonEvent event, Layout l) {''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''            case "academy" -> academy(g, l, mouseX, mouseY);
            case "core" -> core(g, l);''',
    '''            case "academy" -> academy(g, l, mouseX, mouseY);
            case "quests" -> quests(g, l, mouseX, mouseY);
            case "core" -> core(g, l);''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''        g.text(font, Component.literal(current.displayName()), c.right() - font.width(current.displayName()) - 2, c.y() + 4, 0xFFD9C8ED);
        questPanel(g, l);

        MagicTradition[] traditions = traditions();''',
    '''        g.text(font, Component.literal(current.displayName()), c.right() - font.width(current.displayName()) - 2, c.y() + 4, 0xFFD9C8ED);

        MagicTradition[] traditions = traditions();''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''            g.centeredText(font, Component.literal(t.displayName()), r.x() + r.w() / 2, r.y() + 5,
                    selected ? 0xFFFFE4A7 : 0xFFE9E0F1);
        }

        if (academyCircle == 0) {''',
    '''            g.centeredText(font, Component.literal(t.displayName()), r.x() + r.w() / 2, r.y() + 5,
                    selected ? 0xFFFFE4A7 : 0xFFE9E0F1);
            Rect info = l.traditionInfo(i);
            g.fill(info.x(), info.y(), info.right(), info.bottom(), selected ? 0xFF201B31 : 0xFF101827);
            g.text(font, Component.literal(fit("장점 · " + t.strength(), info.w() - 8)),
                    info.x() + 4, info.y() + 5, 0xFF80D5A7);
            g.text(font, Component.literal(fit("단점 · " + t.weakness(), info.w() - 8)),
                    info.x() + 4, info.y() + 19, 0xFFE18A92);
            g.text(font, Component.literal(fit(t.description(), info.w() - 8)),
                    info.x() + 4, info.y() + 33, 0xFF909DB0);
        }

        if (academyCircle == 0) {''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''            long price = offer.basePrice();
            if (offer.kind() == AcademyOfferCatalog.Kind.SPELLBOOK && current != MagicTradition.UNBOUND
                    && SpellWorldLore.tradition(offer.targetId()) == current) {
                price = Math.max(1L, Math.round(price * 0.82));
            }
            boolean enough = marks >= price;''',
    '''            long price = offer.basePrice();
            boolean enough = marks >= price;''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''    private void questPanel(GuiGraphicsExtractor g, Layout l) {
        Rect c = l.content();
        String id = ArcaneClientState.text("quest_id", "");
        String line;
        int color;
        if (id.isBlank()) {
            line = "마도사 주민과 대화해 아르카나 의뢰를 받으세요";
            color = 0xFF8F98A8;
        } else {
            int progress = ArcaneClientState.integer("quest_progress", 0);
            int target = ArcaneClientState.integer("quest_target", 0);
            long reward = ArcaneClientState.longInteger("quest_reward", 0L);
            String description = ArcaneClientState.text("quest_desc", "마도 의뢰");
            line = description + " " + progress + "/" + target + " · 보상 " + reward + " A"
                    + (progress >= target && target > 0 ? " · 수령 가능" : "");
            color = progress >= target && target > 0 ? 0xFFFFD66F : 0xFF9FC6E8;
        }
        g.text(font, Component.literal(fit(line, c.w() - 4)), c.x() + 2, c.y() + 43, color);
    }

    private void core(GuiGraphicsExtractor g, Layout l) {''',
    '''    private void quests(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        Rect c = l.content();
        int count = Math.min(3, ArcaneClientState.integer("quest_count", 0));
        sectionTitle(g, l, "의뢰 게시판", "동시에 최대 3개");
        String offered = ArcaneClientState.text("quest_offer_id", "");
        Rect offer = l.questOffer();
        g.fill(offer.x(), offer.y(), offer.right(), offer.bottom(), 0xFF111A2A);
        g.fill(offer.x(), offer.y(), offer.x() + 3, offer.bottom(), offered.isBlank() ? 0xFF4E5563 : 0xFFFFC65D);
        if (offered.isBlank()) {
            g.text(font, Component.literal("검토 중인 의뢰 없음"), offer.x() + 9, offer.y() + 8, 0xFF8F98A8);
            g.text(font, Component.literal("마도사 주민과 대화하면 내용을 먼저 확인할 수 있습니다."),
                    offer.x() + 9, offer.y() + 25, 0xFF9EABC0);
        } else {
            int circle = ArcaneClientState.integer("quest_offer_circle", 1);
            int target = ArcaneClientState.integer("quest_offer_target", 0);
            long reward = ArcaneClientState.longInteger("quest_offer_reward", 0L);
            String desc = ArcaneClientState.text("quest_offer_desc", "마도 의뢰");
            MagicTradition issuer = MagicTradition.parse(
                    ArcaneClientState.text("quest_offer_affiliation", "UNBOUND"));
            g.text(font, Component.literal(fit(circle + "써클 제안 · " + desc, offer.w() - 18)),
                    offer.x() + 9, offer.y() + 7, 0xFFFFE0A0);
            g.text(font, Component.literal(fit("목표 " + target + " · 보상 " + reward + " A", offer.w() - 18)),
                    offer.x() + 9, offer.y() + 23, 0xFFFFC967);
            g.text(font, Component.literal(fit("의뢰처 " + issuer.displayName(), offer.w() - 18)),
                    offer.x() + 9, offer.y() + 39, 0xFFAEB7C8);
            button(g, l.questAccept(), "수락", inside(mouseX, mouseY, l.questAccept()), true);
            button(g, l.questReject(), "거절", inside(mouseX, mouseY, l.questReject()), true);
        }

        for (int i = 0; i < 3; i++) {
            Rect card = l.questCard(i);
            boolean active = i < count && !ArcaneClientState.text("quest_" + i + "_id", "").isBlank();
            g.fill(card.x(), card.y(), card.right(), card.bottom(), 0xFF101827);
            g.fill(card.x(), card.y(), card.x() + 3, card.bottom(), active ? 0xFF7560A2 : 0xFF343945);
            if (!active) {
                g.text(font, Component.literal((i + 1) + "번 의뢰 슬롯 · 비어 있음"),
                        card.x() + 9, card.y() + 23, 0xFF666F7D);
                continue;
            }
            int progress = ArcaneClientState.integer("quest_" + i + "_progress", 0);
            int target = Math.max(1, ArcaneClientState.integer("quest_" + i + "_target", 1));
            int circle = ArcaneClientState.integer("quest_" + i + "_circle", 1);
            long reward = ArcaneClientState.longInteger("quest_" + i + "_reward", 0L);
            String desc = ArcaneClientState.text("quest_" + i + "_desc", "마도 의뢰");
            boolean complete = progress >= target;
            int textW = card.w() - 105;
            g.text(font, Component.literal(fit((i + 1) + ". " + circle + "써클 · " + desc, textW)),
                    card.x() + 9, card.y() + 7, complete ? 0xFFFFD36B : 0xFFE9E0F1);
            g.text(font, Component.literal(fit(progress + "/" + target + " · " + reward + " A", textW)),
                    card.x() + 9, card.y() + 24, complete ? 0xFFFFC65D : 0xFF9FC6E8);
            g.fill(card.x() + 9, card.y() + 43, card.x() + 9 + Math.max(1, textW - 8), card.y() + 47, 0xFF293244);
            int fill = (int) Math.round(Math.max(0, textW - 8) * Math.min(1.0, progress / (double) target));
            g.fill(card.x() + 9, card.y() + 43, card.x() + 9 + fill, card.y() + 47,
                    complete ? 0xFFFFC65D : 0xFF7569C2);
            if (complete) button(g, l.questClaim(i), "보상 수령",
                    inside(mouseX, mouseY, l.questClaim(i)), true);
        }
    }

    private void core(GuiGraphicsExtractor g, Layout l) {''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''                ArcaneClientState.text("gear_boots", "마도화 없음"),
                "소속 " + MagicTradition.parse(ArcaneClientState.text("tradition", "UNBOUND")).displayName()));''',
    '''                ArcaneClientState.text("gear_boots", "마도화 없음"),
                "소속 " + MagicTradition.parse(ArcaneClientState.text("tradition", "UNBOUND")).displayName(),
                "강점 " + MagicTradition.parse(ArcaneClientState.text("tradition", "UNBOUND")).strength(),
                "약점 " + MagicTradition.parse(ArcaneClientState.text("tradition", "UNBOUND")).weakness()));''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''    private static String normalize(String p) { return "recipes".equals(p)||"staffs".equals(p)||"academy".equals(p)||"core".equals(p) ? p : "atlas"; }''',
    '''    private static String normalize(String p) { return "recipes".equals(p)||"staffs".equals(p)||"academy".equals(p)||"quests".equals(p)||"core".equals(p) ? p : "atlas"; }''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''            case "academy" -> academyCircle == 0 ? 0 : l.maxOfferScroll(AcademyOfferCatalog.forCircle(academyCircle).size());
            case "atlas" -> atlasCircle == 0 ? 0 : l.maxSpellScroll(SpellCatalog.spellsInCircle(atlasCircle).size());''',
    '''            case "academy" -> academyCircle == 0 ? 0 : l.maxOfferScroll(AcademyOfferCatalog.forCircle(academyCircle).size());
            case "quests" -> 0;
            case "atlas" -> atlasCircle == 0 ? 0 : l.maxSpellScroll(SpellCatalog.spellsInCircle(atlasCircle).size());''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''        Rect tradition(int i){Rect c=content();int gap=4;int w=(c.w()-gap*3)/4;return new Rect(c.x()+i*(w+gap),c.y()+20,w,20);}
        Rect academyCircleCard(int circle){
            Rect c=content();int cols=c.w()>=420?9:3,gap=4;int w=(c.w()-gap*(cols-1))/cols;int col=(circle-1)%cols,row=(circle-1)/cols;int h=34;
            return new Rect(c.x()+col*(w+gap),c.y()+62+row*(h+gap),w,h);
        }
        Rect academyBack(){Rect c=content();return new Rect(c.x(),c.y()+59,66,19);}
        Rect academyViewport(){Rect c=content();return new Rect(c.x(),c.y()+84,c.w(),c.h()-85);}
        Rect offerCard(int i,int scroll){Rect v=academyViewport();int cols=v.w()>=540?4:2,gap=5;int w=(v.w()-gap*(cols-1))/cols;int row=i/cols,col=i%cols;return new Rect(v.x()+col*(w+gap),v.y()+row*43-scroll,w,38);}
        int maxOfferScroll(int count){Rect v=academyViewport();int cols=v.w()>=540?4:2;return Math.max(0,((count+cols-1)/cols)*43-v.h());}''',
    '''        Rect tradition(int i){Rect c=content();int gap=4;int w=(c.w()-gap*3)/4;return new Rect(c.x()+i*(w+gap),c.y()+20,w,20);}
        Rect traditionInfo(int i){Rect c=content();int gap=4;int w=(c.w()-gap*3)/4;return new Rect(c.x()+i*(w+gap),c.y()+43,w,46);}
        Rect academyCircleCard(int circle){
            Rect c=content();int cols=c.w()>=420?9:3,gap=4;int w=(c.w()-gap*(cols-1))/cols;int col=(circle-1)%cols,row=(circle-1)/cols;int h=34;
            return new Rect(c.x()+col*(w+gap),c.y()+102+row*(h+gap),w,h);
        }
        Rect academyBack(){Rect c=content();return new Rect(c.x(),c.y()+99,66,19);}
        Rect academyViewport(){Rect c=content();return new Rect(c.x(),c.y()+124,c.w(),c.h()-125);}
        Rect offerCard(int i,int scroll){Rect v=academyViewport();int cols=v.w()>=540?4:2,gap=5;int w=(v.w()-gap*(cols-1))/cols;int row=i/cols,col=i%cols;return new Rect(v.x()+col*(w+gap),v.y()+row*43-scroll,w,38);}
        int maxOfferScroll(int count){Rect v=academyViewport();int cols=v.w()>=540?4:2;return Math.max(0,((count+cols-1)/cols)*43-v.h());}

        Rect questOffer(){Rect c=content();return new Rect(c.x(),c.y()+28,c.w(),64);}
        Rect questAccept(){Rect c=content();return new Rect(c.right()-166,c.y()+96,78,21);}
        Rect questReject(){Rect c=content();return new Rect(c.right()-82,c.y()+96,78,21);}
        Rect questCard(int i){Rect c=content();return new Rect(c.x(),c.y()+128+i*66,c.w(),60);}
        Rect questClaim(int i){Rect r=questCard(i);return new Rect(r.right()-92,r.y()+18,82,23);}''',
)

# New high-tier gear item/model resources and Korean names.
gear_resources = {
    "sage_hat": ("현자의 모자", "minecraft:item/leather_helmet"),
    "sage_robe": ("현자의 로브", "minecraft:item/leather_chestplate"),
    "sage_robe_hem": ("현자의 로브 자락", "minecraft:item/leather_leggings"),
    "skywalker_boots": ("천공 마도화", "minecraft:item/leather_boots"),
    "archmage_crown": ("대마도사 관", "minecraft:item/golden_helmet"),
    "archmage_robe": ("대마도사 예복", "minecraft:item/leather_chestplate"),
    "archmage_robe_hem": ("대마도사 예복 자락", "minecraft:item/leather_leggings"),
    "froststep_boots": ("빙결 보행화", "minecraft:item/diamond_boots"),
}
for item_id, (display_name, vanilla_texture) in gear_resources.items():
    write(f"src/main/resources/assets/arcanecircle/items/{item_id}.json",
          json.dumps({"model": {"type": "minecraft:model",
                                "model": f"arcanecircle:item/{item_id}"}},
                     ensure_ascii=False, separators=(",", ":")) + "\n")
    write(f"src/main/resources/assets/arcanecircle/models/item/{item_id}.json",
          json.dumps({"parent": "minecraft:item/generated",
                      "textures": {"layer0": vanilla_texture}},
                     ensure_ascii=False, separators=(",", ":")) + "\n")

lang_path = path("src/main/resources/assets/arcanecircle/lang/ko_kr.json")
lang = json.loads(lang_path.read_text(encoding="utf-8"))
for item_id, (display_name, _) in gear_resources.items():
    lang[f"item.arcanecircle.{item_id}"] = display_name
lang_path.write_text(json.dumps(lang, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java",
    '''            player.sendSystemMessage(Component.literal(
                    "§71~5를 길게 눌러 회로를 전개합니다. 누른 채 다른 숫자 주문을 더하면 융합되고, 처음 누른 키를 놓아 시전합니다."));
            player.sendSystemMessage(Component.literal(
                    "§7자신보다 낮은 써클 주문은 성장과 숙련에 따라 빠르게 전개되며 충분한 격차에서는 즉발됩니다."));''',
    '''            player.sendSystemMessage(Component.literal(
                    "§71~5를 눌러 회로를 전개합니다. 시전시간 0초 주문은 짧게 눌렀다 놓으면 즉시 발동하며, 누른 채 다른 주문을 더하면 융합됩니다."));
            player.sendSystemMessage(Component.literal(
                    "§7높은 써클일수록 하위 주문의 마력 소모·시전시간·재사용 대기시간이 크게 감소합니다."));''',
)

print("Arcane Circle v0.12.1-alpha.7 progression, quests, factions and threat economy migration applied")

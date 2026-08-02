package kr.moonseungjun.arcanecircle.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.magic.ArcaneNoticeService;
import kr.moonseungjun.arcanecircle.magic.CombatGrowthService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Three-slot quest board with a save-compatible migration from the old single commission. */
public final class ArcaneQuestData extends SavedData {
    public static final int MAX_ACTIVE = 3;

    private record QuestEntry(String id, int target, int progress, int circle, long reward, String affiliation) {
        private static final QuestEntry EMPTY = new QuestEntry("", 0, 0, 1, 0L, "UNBOUND");
        private static final Codec<QuestEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("id", "").forGetter(QuestEntry::id),
                Codec.INT.optionalFieldOf("target", 0).forGetter(QuestEntry::target),
                Codec.INT.optionalFieldOf("progress", 0).forGetter(QuestEntry::progress),
                Codec.INT.optionalFieldOf("circle", 1).forGetter(QuestEntry::circle),
                Codec.LONG.optionalFieldOf("reward", 0L).forGetter(QuestEntry::reward),
                Codec.STRING.optionalFieldOf("affiliation", "UNBOUND").forGetter(QuestEntry::affiliation)
        ).apply(instance, QuestEntry::new));
    }

    private record PlayerEntry(
            String uuid,
            String quest,
            int target,
            int progress,
            int circle,
            long reward,
            List<QuestEntry> active,
            QuestEntry offered,
            int offerSerial
    ) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                Codec.STRING.optionalFieldOf("quest", "").forGetter(PlayerEntry::quest),
                Codec.INT.optionalFieldOf("target", 0).forGetter(PlayerEntry::target),
                Codec.INT.optionalFieldOf("progress", 0).forGetter(PlayerEntry::progress),
                Codec.INT.optionalFieldOf("circle", 1).forGetter(PlayerEntry::circle),
                Codec.LONG.optionalFieldOf("reward", 0L).forGetter(PlayerEntry::reward),
                QuestEntry.CODEC.listOf().optionalFieldOf("active", List.of()).forGetter(PlayerEntry::active),
                QuestEntry.CODEC.optionalFieldOf("offered", QuestEntry.EMPTY).forGetter(PlayerEntry::offered),
                Codec.INT.optionalFieldOf("offer_serial", 0).forGetter(PlayerEntry::offerSerial)
        ).apply(instance, PlayerEntry::new));
    }

    public static final SavedDataType<ArcaneQuestData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "mage_commissions_v1"),
            ArcaneQuestData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(ArcaneQuestData::entries)
            ).apply(instance, ArcaneQuestData::new))
    );

    private static final class Quest {
        String id = "";
        int target;
        int progress;
        int circle = 1;
        long reward;
        MagicTradition affiliation = MagicTradition.UNBOUND;

        Quest copy() {
            Quest result = new Quest();
            result.id = id;
            result.target = target;
            result.progress = progress;
            result.circle = circle;
            result.reward = reward;
            result.affiliation = affiliation;
            return result;
        }
    }

    private final Map<String, List<Quest>> active = new HashMap<>();
    private final Map<String, Quest> offered = new HashMap<>();
    private final Map<String, Integer> offerSerial = new HashMap<>();

    public ArcaneQuestData() {}

    private ArcaneQuestData(List<PlayerEntry> entries) {
        for (PlayerEntry entry : entries) {
            List<Quest> quests = new ArrayList<>();
            for (QuestEntry value : entry.active()) {
                Quest quest = decode(value);
                if (valid(quest) && quests.size() < MAX_ACTIVE) quests.add(quest);
            }
            if (quests.isEmpty() && !entry.quest().isBlank() && entry.target() > 0) {
                Quest migrated = new Quest();
                migrated.id = normalize(entry.quest());
                migrated.target = Math.max(0, entry.target());
                migrated.progress = Math.max(0, Math.min(migrated.target, entry.progress()));
                migrated.circle = clamp(entry.circle(), 1, 9);
                migrated.reward = Math.max(0L, entry.reward());
                if (valid(migrated)) quests.add(migrated);
            }
            if (!quests.isEmpty()) active.put(entry.uuid(), quests);
            Quest proposal = decode(entry.offered());
            if (valid(proposal)) offered.put(entry.uuid(), proposal);
            if (entry.offerSerial() > 0) offerSerial.put(entry.uuid(), entry.offerSerial());
        }
    }

    public static ArcaneQuestData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<QuestStatus> statuses(ServerPlayer player) {
        return active.getOrDefault(key(player), List.of()).stream().map(ArcaneQuestData::status).toList();
    }

    public QuestStatus status(ServerPlayer player) {
        List<QuestStatus> list = statuses(player);
        return list.isEmpty() ? QuestStatus.NONE : list.getFirst();
    }

    public QuestStatus offerStatus(ServerPlayer player) {
        Quest quest = offered.get(key(player));
        return quest == null ? QuestStatus.NONE : status(quest);
    }

    public QuestStatus offer(ServerPlayer player, int mageCircle, MagicTradition issuer) {
        String key = key(player);
        Quest existing = offered.get(key);
        if (existing != null) return status(existing);
        if (active.getOrDefault(key, List.of()).size() >= MAX_ACTIVE) {
            ArcaneNoticeService.push(player, Component.literal(
                    "§c[의뢰 한도] §f동시에 진행할 수 있는 의뢰는 최대 3개입니다."), 100);
            return QuestStatus.NONE;
        }

        int circle = clamp(mageCircle, 1, 9);
        MagicTradition source = issuer == null ? MagicTradition.UNBOUND : issuer;
        long day = ((ServerLevel) player.level()).getGameTime() / 24000L;
        int serial = offerSerial.getOrDefault(key, 0);
        int selector = Math.floorMod(player.getUUID().hashCode() + (int) day * 31
                + circle * 17 + source.ordinal() * 13 + serial * 7, 6);

        Quest quest = new Quest();
        quest.id = switch (selector) {
            case 1 -> "hits";
            case 2 -> "kills";
            case 3 -> "damage";
            case 4 -> "threat";
            case 5 -> "fusion";
            default -> "casts";
        };
        quest.circle = circle;
        quest.affiliation = source;
        quest.target = targetFor(quest.id, circle);
        double typeMultiplier = switch (quest.id) {
            case "kills" -> 1.35;
            case "damage" -> 1.20;
            case "threat" -> 1.55;
            case "fusion" -> 1.45;
            default -> 1.0;
        };
        quest.reward = Math.max(500L, Math.round(baseReward(circle) * typeMultiplier
                * source.questRewardMultiplier() * (1.0 + quest.target * 0.025)));
        offered.put(key, quest);
        setDirty();
        ArcaneNoticeService.push(player, Component.literal("§5[새 의뢰 제안] §f" + description(quest.id)
                + " §7· 목표 " + quest.target + " · 보상 §6" + quest.reward
                + " 아르카나 §7· 의뢰 탭에서 수락 또는 거절"), 160);
        return status(quest);
    }

    public boolean acceptOffer(ServerPlayer player) {
        String key = key(player);
        Quest proposal = offered.get(key);
        if (proposal == null) {
            ArcaneNoticeService.push(player, Component.literal("§7[의뢰] 수락할 제안이 없습니다."));
            return false;
        }
        List<Quest> list = active.computeIfAbsent(key, ignored -> new ArrayList<>());
        if (list.size() >= MAX_ACTIVE) {
            ArcaneNoticeService.push(player, Component.literal("§c[의뢰 한도] §f동시에 최대 3개까지 진행할 수 있습니다."));
            return false;
        }
        list.add(proposal.copy());
        offered.remove(key);
        offerSerial.merge(key, 1, Integer::sum);
        setDirty();
        ArcaneNoticeService.push(player, Component.literal("§5[의뢰 수락] §f" + description(proposal.id)
                + " §7· 보상 §6" + proposal.reward + " 아르카나"), 100);
        return true;
    }

    public boolean rejectOffer(ServerPlayer player) {
        String key = key(player);
        Quest removed = offered.remove(key);
        if (removed == null) return false;
        offerSerial.merge(key, 1, Integer::sum);
        setDirty();
        ArcaneNoticeService.push(player, Component.literal("§7[의뢰 거절] 제안을 돌려보냈습니다."), 80);
        return true;
    }

    public long claim(ServerPlayer player, int index) {
        String key = key(player);
        List<Quest> list = active.get(key);
        if (list == null || index < 0 || index >= list.size()) return 0L;
        Quest quest = list.get(index);
        if (quest.progress < quest.target) {
            ArcaneNoticeService.push(player, Component.literal("§7[의뢰] 아직 목표를 완료하지 못했습니다."));
            return 0L;
        }
        long reward = quest.reward;
        ArcaneWorldData.get(((ServerLevel) player.level()).getServer()).addMarks(player, reward);
        list.remove(index);
        if (list.isEmpty()) active.remove(key);
        setDirty();
        ArcaneNoticeService.push(player, Component.literal("§6[의뢰 보상] §f+" + reward + " 아르카나"), 110);
        return reward;
    }

    /** Compatibility entry point for old callers. */
    public long claim(ServerPlayer player) {
        List<QuestStatus> list = statuses(player);
        for (int i = 0; i < list.size(); i++) if (list.get(i).complete()) return claim(player, i);
        return 0L;
    }

    /** Compatibility entry point: creates and immediately accepts one offer. */
    public QuestStatus assign(ServerPlayer player, int mageCircle) {
        QuestStatus proposal = offer(player, mageCircle, MagicTradition.UNBOUND);
        if (proposal.active()) acceptOffer(player);
        return status(player);
    }

    public void recordCast(ServerPlayer player, CombatGrowthService.Impact impact, int spellCircle, boolean fusion) {
        List<Quest> list = active.get(key(player));
        if (list == null || list.isEmpty()) return;
        CombatGrowthService.Impact value = impact == null ? CombatGrowthService.Impact.NONE : impact;
        boolean changed = false;
        for (Quest quest : list) {
            if (quest.progress >= quest.target) continue;
            int delta = switch (quest.id) {
                case "hits" -> Math.max(0, value.hits());
                case "kills" -> Math.max(0, value.kills());
                case "damage" -> Math.max(0, value.damage());
                case "threat" -> Math.max(0, value.threatPoints());
                case "fusion" -> fusion && spellCircle >= Math.max(1, quest.circle - 1) ? 1 : 0;
                default -> spellCircle >= Math.max(1, quest.circle - 1) ? 1 : 0;
            };
            if (delta <= 0) continue;
            int before = quest.progress;
            quest.progress = Math.min(quest.target, quest.progress + delta);
            changed |= quest.progress != before;
            if (before < quest.target && quest.progress >= quest.target) {
                ArcaneNoticeService.push(player, Component.literal("§6[의뢰 완료] §f" + description(quest.id)
                        + " §7· 의뢰 탭에서 §6" + quest.reward + " 아르카나§7를 수령하세요."), 140);
            }
        }
        if (changed) setDirty();
    }

    private List<PlayerEntry> entries() {
        java.util.Set<String> keys = new java.util.HashSet<>();
        keys.addAll(active.keySet());
        keys.addAll(offered.keySet());
        keys.addAll(offerSerial.keySet());
        return keys.stream().sorted().map(uuid -> {
            List<QuestEntry> quests = active.getOrDefault(uuid, List.of()).stream()
                    .limit(MAX_ACTIVE).map(ArcaneQuestData::encode).toList();
            QuestEntry proposal = offered.containsKey(uuid) ? encode(offered.get(uuid)) : QuestEntry.EMPTY;
            Quest legacy = active.getOrDefault(uuid, List.of()).stream().findFirst().orElse(null);
            return new PlayerEntry(uuid, legacy == null ? "" : legacy.id, legacy == null ? 0 : legacy.target,
                    legacy == null ? 0 : legacy.progress, legacy == null ? 1 : legacy.circle,
                    legacy == null ? 0L : legacy.reward, quests, proposal, offerSerial.getOrDefault(uuid, 0));
        }).toList();
    }

    private static QuestEntry encode(Quest quest) {
        return new QuestEntry(quest.id, quest.target, quest.progress, quest.circle, quest.reward, quest.affiliation.name());
    }

    private static Quest decode(QuestEntry value) {
        Quest quest = new Quest();
        if (value == null) return quest;
        quest.id = normalize(value.id());
        quest.target = Math.max(0, value.target());
        quest.progress = Math.max(0, Math.min(quest.target, value.progress()));
        quest.circle = clamp(value.circle(), 1, 9);
        quest.reward = Math.max(0L, value.reward());
        quest.affiliation = MagicTradition.parse(value.affiliation());
        return quest;
    }

    private static QuestStatus status(Quest quest) {
        if (!valid(quest)) return QuestStatus.NONE;
        return new QuestStatus(true, quest.progress >= quest.target, quest.id, quest.target, quest.progress,
                quest.circle, quest.reward, description(quest.id), quest.affiliation);
    }

    private static boolean valid(Quest quest) {
        return quest != null && !quest.id.isBlank() && quest.target > 0;
    }

    private static int targetFor(String id, int circle) {
        return switch (id) {
            case "hits" -> 10 + circle * 4;
            case "kills" -> 3 + circle;
            case "damage" -> 80 + circle * circle * 28;
            case "threat" -> 22 + circle * circle * 7;
            case "fusion" -> 2 + Math.max(1, circle / 2);
            default -> 5 + circle;
        };
    }

    private static long baseReward(int circle) {
        return switch (circle) {
            case 1 -> 1_200L;
            case 2 -> 3_000L;
            case 3 -> 7_000L;
            case 4 -> 16_000L;
            case 5 -> 36_000L;
            case 6 -> 80_000L;
            case 7 -> 175_000L;
            case 8 -> 380_000L;
            case 9 -> 820_000L;
            default -> 1_200L;
        };
    }

    private static String normalize(String id) {
        return switch (id == null ? "" : id) {
            case "casts", "hits", "kills", "damage", "threat", "fusion" -> id;
            default -> "";
        };
    }

    private static String description(String id) {
        return switch (id) {
            case "hits" -> "마법으로 적을 적중";
            case "kills" -> "마법으로 적을 처치";
            case "damage" -> "마법 피해 누적";
            case "threat" -> "위협도 높은 적과 교전";
            case "fusion" -> "융합 주문 시전";
            default -> "요구 써클 이상의 주문 시전";
        };
    }

    private static String key(ServerPlayer player) { return player.getUUID().toString(); }
    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record QuestStatus(boolean active, boolean complete, String id, int target, int progress,
                              int circle, long reward, String description, MagicTradition affiliation) {
        public static final QuestStatus NONE = new QuestStatus(
                false, false, "", 0, 0, 1, 0L, "", MagicTradition.UNBOUND);
    }
}

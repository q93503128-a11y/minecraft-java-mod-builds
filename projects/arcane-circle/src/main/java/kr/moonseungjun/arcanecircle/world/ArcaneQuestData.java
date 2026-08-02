package kr.moonseungjun.arcanecircle.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.magic.CombatGrowthService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Persistent mage-resident commissions. Existing character and spell save formats are untouched. */
public final class ArcaneQuestData extends SavedData {
    private record PlayerEntry(String uuid, String quest, int target, int progress, int circle, long reward) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                Codec.STRING.optionalFieldOf("quest", "").forGetter(PlayerEntry::quest),
                Codec.INT.optionalFieldOf("target", 0).forGetter(PlayerEntry::target),
                Codec.INT.optionalFieldOf("progress", 0).forGetter(PlayerEntry::progress),
                Codec.INT.optionalFieldOf("circle", 1).forGetter(PlayerEntry::circle),
                Codec.LONG.optionalFieldOf("reward", 0L).forGetter(PlayerEntry::reward)
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
    }

    private final Map<String, Quest> players = new HashMap<>();

    public ArcaneQuestData() {}

    private ArcaneQuestData(List<PlayerEntry> entries) {
        for (PlayerEntry entry : entries) {
            Quest quest = new Quest();
            quest.id = normalize(entry.quest());
            quest.target = Math.max(0, entry.target());
            quest.progress = Math.max(0, Math.min(quest.target, entry.progress()));
            quest.circle = Math.max(1, Math.min(9, entry.circle()));
            quest.reward = Math.max(0L, entry.reward());
            players.put(entry.uuid(), quest);
        }
    }

    public static ArcaneQuestData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public QuestStatus status(ServerPlayer player) {
        Quest quest = players.get(player.getUUID().toString());
        if (quest == null || quest.id.isBlank() || quest.target <= 0) return QuestStatus.NONE;
        return new QuestStatus(true, quest.progress >= quest.target, quest.id, quest.target,
                quest.progress, quest.circle, quest.reward, description(quest.id));
    }

    public QuestStatus assign(ServerPlayer player, int mageCircle) {
        QuestStatus existing = status(player);
        if (existing.active()) return existing;
        int circle = Math.max(1, Math.min(9, mageCircle));
        long day = ((ServerLevel) player.level()).getGameTime() / 24000L;
        int selector = Math.floorMod(player.getUUID().hashCode() + (int) day + circle, 3);
        Quest quest = new Quest();
        quest.id = selector == 0 ? "casts" : selector == 1 ? "hits" : "kills";
        quest.circle = circle;
        quest.target = switch (quest.id) {
            case "hits" -> 7 + circle * 3;
            case "kills" -> 2 + Math.max(1, circle / 2);
            default -> 3 + circle;
        };
        quest.reward = 55L + circle * 85L + quest.target * 12L;
        players.put(player.getUUID().toString(), quest);
        setDirty();
        QuestStatus result = status(player);
        player.sendSystemMessage(Component.literal("§5[마도사 의뢰] §f" + result.description()
                + " §7· 보상 §d" + result.reward() + " 아르카나"));
        return result;
    }

    public void recordCast(ServerPlayer player, CombatGrowthService.Impact impact, int spellCircle) {
        Quest quest = players.get(player.getUUID().toString());
        if (quest == null || quest.id.isBlank() || quest.progress >= quest.target) return;
        CombatGrowthService.Impact value = impact == null ? CombatGrowthService.Impact.NONE : impact;
        int delta = switch (quest.id) {
            case "hits" -> Math.max(0, value.hits());
            case "kills" -> Math.max(0, value.kills());
            default -> spellCircle >= Math.max(1, quest.circle - 1) ? 1 : 0;
        };
        if (delta <= 0) return;
        int before = quest.progress;
        quest.progress = Math.min(quest.target, quest.progress + delta);
        if (quest.progress != before) setDirty();
        if (before < quest.target && quest.progress >= quest.target) {
            player.sendSystemMessage(Component.literal("§6[의뢰 완료] §f마도사 주민에게 돌아가 보상을 받으세요. §d"
                    + quest.reward + " 아르카나"));
        }
    }

    public long claim(ServerPlayer player) {
        Quest quest = players.get(player.getUUID().toString());
        if (quest == null || quest.id.isBlank() || quest.progress < quest.target) return 0L;
        long reward = quest.reward;
        ArcaneWorldData.get(((ServerLevel) player.level()).getServer()).addMarks(player, reward);
        players.remove(player.getUUID().toString());
        setDirty();
        player.sendSystemMessage(Component.literal("§6[의뢰 보상] §f+" + reward + " 아르카나"));
        return reward;
    }

    private List<PlayerEntry> entries() {
        return players.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> new PlayerEntry(entry.getKey(), entry.getValue().id, entry.getValue().target,
                        entry.getValue().progress, entry.getValue().circle, entry.getValue().reward))
                .toList();
    }

    private static String normalize(String id) {
        return "casts".equals(id) || "hits".equals(id) || "kills".equals(id) ? id : "";
    }

    private static String description(String id) {
        return switch (id) {
            case "hits" -> "마법으로 적을 적중";
            case "kills" -> "마법으로 적을 처치";
            default -> "요구 써클 이상의 주문을 시전";
        };
    }

    public record QuestStatus(boolean active, boolean complete, String id, int target, int progress,
                              int circle, long reward, String description) {
        public static final QuestStatus NONE = new QuestStatus(false, false, "", 0, 0, 1, 0L, "");
    }
}

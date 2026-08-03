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

/** Three fixed-difficulty quest slots; rewards never depend on player or issuer circle. */
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

    private record PlayerEntry(String uuid, String quest, int target, int progress, int circle, long reward,
                               List<QuestEntry> active, QuestEntry offered, int offerSerial) {
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
        int difficulty = 1;
        long reward;
        MagicTradition affiliation = MagicTradition.UNBOUND;
        Quest copy() { Quest q = new Quest(); q.id=id; q.target=target; q.progress=progress;
            q.difficulty=difficulty; q.reward=reward; q.affiliation=affiliation; return q; }
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
                migrated.target = Math.max(1, entry.target());
                migrated.progress = Math.max(0, Math.min(migrated.target, entry.progress()));
                migrated.difficulty = clamp(entry.circle(), 1, 6);
                migrated.reward = Math.max(0L, entry.reward());
                if (valid(migrated)) quests.add(migrated);
            }
            if (!quests.isEmpty()) active.put(entry.uuid(), quests);
            Quest proposal = decode(entry.offered());
            if (valid(proposal)) offered.put(entry.uuid(), proposal);
            if (entry.offerSerial() > 0) offerSerial.put(entry.uuid(), entry.offerSerial());
        }
    }

    public static ArcaneQuestData get(MinecraftServer server) { return server.getDataStorage().computeIfAbsent(TYPE); }
    public List<QuestStatus> statuses(ServerPlayer player) {
        return active.getOrDefault(key(player), List.of()).stream().map(ArcaneQuestData::status).toList();
    }
    public QuestStatus status(ServerPlayer player) { List<QuestStatus> list=statuses(player); return list.isEmpty()?QuestStatus.NONE:list.getFirst(); }
    public QuestStatus offerStatus(ServerPlayer player) { Quest q=offered.get(key(player)); return q==null?QuestStatus.NONE:status(q); }

    /** mageCircle is retained only for binary/source compatibility; it never changes difficulty or reward. */
    public QuestStatus offer(ServerPlayer player, int mageCircle, MagicTradition issuer) {
        String key = key(player);
        Quest existing = offered.get(key);
        if (existing != null) return status(existing);
        if (active.getOrDefault(key, List.of()).size() >= MAX_ACTIVE) {
            ArcaneNoticeService.push(player, Component.literal("§c[의뢰 한도] §f동시에 진행할 수 있는 의뢰는 최대 3개입니다."), 100);
            return QuestStatus.NONE;
        }
        MagicTradition source = issuer == null ? MagicTradition.UNBOUND : issuer;
        long day = ((ServerLevel) player.level()).getGameTime() / 24000L;
        int serial = offerSerial.getOrDefault(key, 0);
        int seed = player.getUUID().hashCode() * 31 + (int) day * 67 + source.ordinal() * 19 + serial * 101;
        int typeRoll = Math.floorMod(seed, 6);
        int difficultyRoll = Math.floorMod(seed * 17 + 41, 100);

        Quest quest = new Quest();
        quest.id = switch (typeRoll) {
            case 1 -> "hits"; case 2 -> "kills"; case 3 -> "damage";
            case 4 -> "threat"; case 5 -> "fusion"; default -> "casts";
        };
        quest.difficulty = difficultyRoll < 32 ? 1 : difficultyRoll < 59 ? 2
                : difficultyRoll < 79 ? 3 : difficultyRoll < 91 ? 4 : difficultyRoll < 98 ? 5 : 6;
        quest.affiliation = source;
        quest.target = targetFor(quest.id, quest.difficulty);
        quest.reward = Math.round(baseReward(quest.difficulty) * typeMultiplier(quest.id));
        offered.put(key, quest);
        setDirty();
        ArcaneNoticeService.push(player, Component.literal("§5[새 의뢰] §f" + difficultyName(quest.difficulty)
                + " · " + description(quest.id) + " §7· 목표 " + quest.target
                + " · 보상 §6" + quest.reward + " 아르카나"), 150);
        return status(quest);
    }

    public boolean acceptOffer(ServerPlayer player) {
        String key=key(player); Quest proposal=offered.get(key);
        if (proposal==null) { ArcaneNoticeService.push(player, Component.literal("§7[의뢰] 수락할 제안이 없습니다.")); return false; }
        List<Quest> list=active.computeIfAbsent(key, ignored->new ArrayList<>());
        if (list.size()>=MAX_ACTIVE) return false;
        list.add(proposal.copy()); offered.remove(key); offerSerial.merge(key,1,Integer::sum); setDirty();
        ArcaneNoticeService.push(player, Component.literal("§5[의뢰 수락] §f"+difficultyName(proposal.difficulty)
                +" · "+description(proposal.id)+" §7· 보상 §6"+proposal.reward+" 아르카나"),100);
        return true;
    }

    public boolean rejectOffer(ServerPlayer player) {
        String key=key(player); if(offered.remove(key)==null)return false;
        offerSerial.merge(key,1,Integer::sum); setDirty();
        ArcaneNoticeService.push(player, Component.literal("§7[의뢰 거절] 제안을 돌려보냈습니다."),80); return true;
    }

    public long claim(ServerPlayer player,int index){
        String key=key(player); List<Quest> list=active.get(key);
        if(list==null||index<0||index>=list.size())return 0L; Quest q=list.get(index);
        if(q.progress<q.target){ArcaneNoticeService.push(player,Component.literal("§7[의뢰] 아직 목표를 완료하지 못했습니다."));return 0L;}
        ArcaneWorldData.get(((ServerLevel)player.level()).getServer()).addMarks(player,q.reward);
        long reward=q.reward; list.remove(index); if(list.isEmpty())active.remove(key); setDirty();
        ArcaneNoticeService.push(player,Component.literal("§6[의뢰 보상] §f+"+reward+" 아르카나"),110); return reward;
    }
    public long claim(ServerPlayer player){List<QuestStatus> l=statuses(player);for(int i=0;i<l.size();i++)if(l.get(i).complete())return claim(player,i);return 0L;}
    public QuestStatus assign(ServerPlayer player,int mageCircle){QuestStatus q=offer(player,mageCircle,MagicTradition.UNBOUND);if(q.active())acceptOffer(player);return status(player);}

    public void recordCast(ServerPlayer player, CombatGrowthService.Impact impact, int spellCircle, boolean fusion) {
        List<Quest> list=active.get(key(player)); if(list==null||list.isEmpty())return;
        CombatGrowthService.Impact value=impact==null?CombatGrowthService.Impact.NONE:impact; boolean changed=false;
        for(Quest q:list){ if(q.progress>=q.target)continue;
            int delta=switch(q.id){
                case "hits"->Math.max(0,value.hits()); case "kills"->Math.max(0,value.kills());
                case "damage"->Math.max(0,value.damage()); case "threat"->Math.max(0,value.threatPoints());
                case "fusion"->fusion&&spellCircle>=Math.max(1,q.difficulty-1)?1:0;
                default->spellCircle>=Math.max(1,q.difficulty-1)?1:0;};
            if(delta<=0)continue; int before=q.progress; q.progress=Math.min(q.target,q.progress+delta); changed|=before!=q.progress;
            if(before<q.target&&q.progress>=q.target)ArcaneNoticeService.push(player,Component.literal("§6[의뢰 완료] §f"
                    +difficultyName(q.difficulty)+" · "+description(q.id)+" §7· 보상 §6"+q.reward+" 아르카나"),140);
        } if(changed)setDirty();
    }

    private List<PlayerEntry> entries(){java.util.Set<String> keys=new java.util.HashSet<>();keys.addAll(active.keySet());keys.addAll(offered.keySet());keys.addAll(offerSerial.keySet());
        return keys.stream().sorted().map(uuid->{List<QuestEntry> qs=active.getOrDefault(uuid,List.of()).stream().limit(MAX_ACTIVE).map(ArcaneQuestData::encode).toList();
            QuestEntry proposal=offered.containsKey(uuid)?encode(offered.get(uuid)):QuestEntry.EMPTY;Quest legacy=active.getOrDefault(uuid,List.of()).stream().findFirst().orElse(null);
            return new PlayerEntry(uuid,legacy==null?"":legacy.id,legacy==null?0:legacy.target,legacy==null?0:legacy.progress,
                    legacy==null?1:legacy.difficulty,legacy==null?0L:legacy.reward,qs,proposal,offerSerial.getOrDefault(uuid,0));}).toList();}
    private static QuestEntry encode(Quest q){return new QuestEntry(q.id,q.target,q.progress,q.difficulty,q.reward,q.affiliation.name());}
    private static Quest decode(QuestEntry v){Quest q=new Quest();if(v==null)return q;q.id=normalize(v.id());q.target=Math.max(0,v.target());q.progress=Math.max(0,Math.min(q.target,v.progress()));q.difficulty=clamp(v.circle(),1,6);q.reward=Math.max(0L,v.reward());q.affiliation=MagicTradition.parse(v.affiliation());return q;}
    private static QuestStatus status(Quest q){return !valid(q)?QuestStatus.NONE:new QuestStatus(true,q.progress>=q.target,q.id,q.target,q.progress,q.difficulty,q.reward,description(q.id),q.affiliation);}
    private static boolean valid(Quest q){return q!=null&&!q.id.isBlank()&&q.target>0;}
    private static int targetFor(String id,int d){return switch(id){case"hits"->8+d*6;case"kills"->2+d*2;case"damage"->100*d*d;case"threat"->20*d*d;case"fusion"->1+d;default->3+d*2;};}
    private static double typeMultiplier(String id){return switch(id){case"kills"->1.35;case"damage"->1.20;case"threat"->1.65;case"fusion"->1.45;default->1.0;};}
    private static long baseReward(int d){return switch(d){case 1->1_200L;case 2->4_000L;case 3->14_000L;case 4->55_000L;case 5->240_000L;case 6->1_250_000L;default->1_200L;};}
    private static String normalize(String id){return switch(id==null?"":id){case"casts","hits","kills","damage","threat","fusion"->id;default->"";};}
    private static String description(String id){return switch(id){case"hits"->"마법 적중";case"kills"->"적 처치";case"damage"->"마법 피해 누적";case"threat"->"강적 위협도 누적";case"fusion"->"융합 주문 시전";default->"고난도 주문 시전";};}
    public static String difficultyName(int d){return switch(d){case 1->"견습";case 2->"일반";case 3->"정예";case 4->"영웅";case 5->"전설";case 6->"재앙";default->"견습";};}
    private static String key(ServerPlayer p){return p.getUUID().toString();}
    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}

    public record QuestStatus(boolean active,boolean complete,String id,int target,int progress,int circle,long reward,String description,MagicTradition affiliation){
        public static final QuestStatus NONE=new QuestStatus(false,false,"",0,0,1,0L,"",MagicTradition.UNBOUND);
        public int difficulty(){return circle;} public String difficultyName(){return ArcaneQuestData.difficultyName(circle);}
    }
}

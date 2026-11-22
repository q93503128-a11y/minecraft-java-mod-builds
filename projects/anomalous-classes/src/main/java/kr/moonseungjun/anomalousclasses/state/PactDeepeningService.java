package kr.moonseungjun.anomalousclasses.state;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import kr.moonseungjun.anomalousclasses.entity.PuppetEntity;

/**
 * Four tiers of voluntary deepening. Endgame second stages require genuine
 * high-threat victories or diverse-biome survival, not recurring relic tolls.
 */
public final class PactDeepeningService {
    public record Stage(int proofs, List<PactCatalog.Material> offering,String event) {
        public Stage {
            if (proofs < 1 || offering.isEmpty() || event==null || event.isBlank())
                throw new IllegalArgumentException("Empty or unnamed rite");
            offering=List.copyOf(offering);
        }
    }
    public record Rite(String patron, String event, List<Stage> stages) {
        public Rite { stages = List.copyOf(stages); }
    }
    private static double eliteMinHealth=70.0;
    private static final Map<String, Rite> RITES = load();
    private static MinecraftServer currentServer;
    private static final Map<UUID, Map<String, Evidence>> EVIDENCE = new HashMap<>();
    private static final class Evidence {
        long nextEligible;
        final Map<UUID, Long> targetReady = new HashMap<>();
    }
    private PactDeepeningService() {}

    public static Map<String, Rite> all() { return RITES; }
    /** Data-defined health threshold; boss kill also needs player damage attribution. */
    public static double eliteMinHealth() { return eliteMinHealth; }
    public static Rite rite(String patron) { return RITES.get(patron); }
    public static Stage next(String patron, int depth) {
        Rite rite = RITES.get(patron);
        return rite == null || depth < 0 || depth >= rite.stages().size()
                ? null : rite.stages().get(depth);
    }
    public static int requirement(String patron, int depth) {
        Stage stage = next(patron,depth);
        return stage == null ? 0 : stage.proofs();
    }
    /**
     * Only the currently earned stage counts for rite proof. After the last
     * stage, original legitimate ability use STILL contributes relationship.
     */
    public static boolean acceptsEvent(String patron,int depth,String event) {
        Rite rite=RITES.get(patron);
        if(rite==null || event==null || depth<0)return false;
        Stage stage=next(patron,depth);
        return (stage==null ? rite.event() : stage.event()).equals(event);
    }
    public static void clear(ServerPlayer player) { EVIDENCE.remove(player.getUUID()); }

    /**
     * Only actual successful owner casts call this with a validated hostile.
     * A target can provide evidence at most once per 60s, and the same caster
     * can progress at most once per 8s, to reject simple click spam.
     */
    public static void record(ServerPlayer caster, String patron, String event, Mob hostile) {
        if (caster.getServer() == null || !PactService.isPact(caster)
                || hostile == null || !(hostile instanceof Enemy)
                || hostile.level()!=caster.level() || hostile instanceof PuppetEntity
                || caster.isAlliedTo(hostile)
                || MentalDominatorSavedData.get(caster.getServer()).controlled(hostile.getUUID())!=null
                || ParasiteSavedData.get(caster.getServer()).ownerOf(hostile.getUUID())!=null) return;
        PactSavedData saved=PactSavedData.get(caster.getServer());
        PactSavedData.Bond bond=saved.player(caster.getUUID()).bond(patron);
        Rite rite=RITES.get(patron);
        if (bond == null || rite == null) return;
        Stage next=next(patron,bond.deepening());
        // Completed pacts still earn relationship through their original
        // genuine ability use; only NEW rite evidence is stage-restricted.
        // This preserves prior tier-one/two/three post-mastery progression.
        if(!acceptsEvent(patron,bond.deepening(),event))return;
        MinecraftServer server=caster.getServer();
        if (currentServer != server) { EVIDENCE.clear();currentServer=server; }
        long now=server.getTickCount();
        Evidence evidence=EVIDENCE.computeIfAbsent(caster.getUUID(),x->new HashMap<>())
                .computeIfAbsent(patron,x->new Evidence());
        if (now<evidence.nextEligible || now<evidence.targetReady.getOrDefault(hostile.getUUID(),0L))
            return;
        evidence.nextEligible=now+160;
        evidence.targetReady.entrySet().removeIf(e->e.getValue()<=now);
        if (evidence.targetReady.size()>=128) evidence.targetReady.clear();
        evidence.targetReady.put(hostile.getUUID(),now+1200);
        boolean earned=next!=null
                && saved.addDeepeningProof(caster.getUUID(),patron,next.proofs());
        // Isael's trust rises only from an actual villager rescue via the
        // existing ward code; personal blocking can still prove mastery.
        if(!patron.equals("isael"))saved.relation(caster.getUUID(),patron,1);
        if(!patron.equals("isael"))PactBlessingService.apply(caster);
        if(earned && saved.player(caster.getUUID()).bond(patron).progress()==next.proofs())
            caster.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.anomalousclasses.pact.deepening_ready",PactCatalog.get(patron).name()),true);
    }
    /** A biome counts only following valid damage AND actual rooted-combat healing.
     * The server resolves biome IDs; no client-supplied exploration token.
     */
    public static void recordViarBiome(ServerPlayer caster,Mob hostile) {
        if(caster.getServer()==null || !PactService.isPact(caster)
                || hostile==null || !(hostile instanceof Enemy)
                || hostile.level()!=caster.level() || hostile instanceof PuppetEntity
                || caster.isAlliedTo(hostile)
                || MentalDominatorSavedData.get(caster.getServer()).controlled(hostile.getUUID())!=null
                || ParasiteSavedData.get(caster.getServer()).ownerOf(hostile.getUUID())!=null)return;
        PactSavedData data=PactSavedData.get(caster.getServer());
        PactSavedData.Bond bond=data.player(caster.getUUID()).bond("viar");
        Stage stage=bond==null?null:next("viar",bond.deepening());
        if(stage==null || !"renewal_in_distinct_biomes".equals(stage.event()))return;
        var key=caster.serverLevel().getBiome(caster.blockPosition()).unwrapKey();
        if(key.isEmpty())return;
        if(!data.addViarBiomeProof(caster.getUUID(),key.get().location()))return;
        data.relation(caster.getUUID(),"viar",1);
        PactBlessingService.apply(caster);
        if(data.player(caster.getUUID()).bond("viar").progress()==stage.proofs())
            caster.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.anomalousclasses.pact.deepening_ready",
                    PactCatalog.get("viar").name()),true);
    }
    private static Map<String,Rite> load() {
        Map<String,Rite> loaded=new LinkedHashMap<>();
        loadTier(loaded,"pact_deepening_tier1_v1.json",1);
        loadTier(loaded,"pact_deepening_tier2_v1.json",2);
        loadTier(loaded,"pact_deepening_tier3_v1.json",3);
        loadTier(loaded,"pact_deepening_tier4_v1.json",4);
        if(loaded.size()!=16)throw new IllegalStateException("Expected sixteen active rites");
        return Map.copyOf(loaded);
    }
    private static void loadTier(Map<String,Rite> loaded,String resource,int tier) {
        try (var stream=PactDeepeningService.class.getResourceAsStream(
                "/data/anomalousclasses/"+resource)) {
            if (stream==null)throw new IllegalStateException("Missing rite resource: "+resource);
            JsonObject root=JsonParser.parseReader(
                    new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            if(tier==4) {
                double threshold=root.get("elite_min_health").getAsDouble();
                if(!Double.isFinite(threshold) || threshold<40 || threshold>1000)
                    throw new IllegalStateException("Invalid endgame strong-foe threshold");
                eliteMinHealth=threshold;
            }
            int count=0;
            for(JsonElement element:root.getAsJsonArray("entries")) {
                JsonObject row=element.getAsJsonObject();
                String id=row.get("id").getAsString();
                String event=row.get("event").getAsString();
                var patron=PactCatalog.get(id);
                if (patron==null || patron.tier()!=tier || event.isBlank()
                        || loaded.containsKey(id))
                    throw new IllegalStateException("Invalid tier "+tier+" rite: "+id);
                var stages=new java.util.ArrayList<Stage>();
                for(JsonElement elementStage:row.getAsJsonArray("stages")) {
                    JsonObject stage=elementStage.getAsJsonObject();
                    var materials=new java.util.ArrayList<PactCatalog.Material>();
                    for(JsonElement elementMaterial:stage.getAsJsonArray("offering")) {
                        JsonObject material=elementMaterial.getAsJsonObject();
                        materials.add(new PactCatalog.Material(
                                ResourceLocation.parse(material.get("item").getAsString()),
                                material.get("count").getAsInt()));
                    }
                    String stageEvent=stage.has("event")
                            ? stage.get("event").getAsString() : event;
                    stages.add(new Stage(stage.get("proofs").getAsInt(),materials,stageEvent));
                }
                if(stages.size()!=2)
                    throw new IllegalStateException("Expected two playable rites: "+id);
                loaded.put(id,new Rite(id,event,stages));
                count++;
            }
            if(count!=4)throw new IllegalStateException("Incomplete tier "+tier+" rites");
        } catch(Exception bad) {
            throw new IllegalStateException("Invalid tier "+tier+" covenant rites",bad);
        }
    }

}

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

/**
 * First-tier voluntary deepening: real combat evidence, a separate offering,
 * and explicit confirmation. Later tiers require their own approved trials.
 */
public final class PactDeepeningService {
    public record Stage(int proofs, List<PactCatalog.Material> offering) {
        public Stage {
            if (proofs < 1 || offering.isEmpty()) throw new IllegalArgumentException("Empty rite");
            offering = List.copyOf(offering);
        }
    }
    public record Rite(String patron, String event, List<Stage> stages) {
        public Rite { stages = List.copyOf(stages); }
    }
    private static final Map<String, Rite> RITES = load();
    private static MinecraftServer currentServer;
    private static final Map<UUID, Map<String, Evidence>> EVIDENCE = new HashMap<>();
    private static final class Evidence {
        long nextEligible;
        final Map<UUID, Long> targetReady = new HashMap<>();
    }
    private PactDeepeningService() {}

    public static Map<String, Rite> all() { return RITES; }
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
    public static void clear(ServerPlayer player) { EVIDENCE.remove(player.getUUID()); }

    /**
     * Only actual successful owner casts call this with a validated hostile.
     * A target can provide evidence at most once per 60s, and the same caster
     * can progress at most once per 8s, to reject simple click spam.
     */
    public static void record(ServerPlayer caster, String patron, String event, Mob hostile) {
        if (caster.getServer() == null || !PactService.isPact(caster)
                || hostile == null || !PactCombatService.enemy(caster,hostile)) return;
        PactSavedData saved=PactSavedData.get(caster.getServer());
        PactSavedData.Bond bond=saved.player(caster.getUUID()).bond(patron);
        Rite rite=RITES.get(patron);
        if (bond == null || rite == null || !rite.event().equals(event)) return;
        Stage next=next(patron,bond.deepening());
        if (next == null || bond.progress()>=next.proofs()) return;
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
        if (saved.addDeepeningProof(caster.getUUID(),patron,next.proofs())) {
            saved.relation(caster.getUUID(),patron,1);
            PactBlessingService.apply(caster);
            if (saved.player(caster.getUUID()).bond(patron).progress()==next.proofs())
                caster.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.anomalousclasses.pact.deepening_ready",PactCatalog.get(patron).name()),true);
        }
    }
    private static Map<String,Rite> load() {
        try (var stream=PactDeepeningService.class.getResourceAsStream(
                "/data/anomalousclasses/pact_deepening_tier1_v1.json")) {
            if (stream==null)throw new IllegalStateException("Missing first-tier rites");
            JsonObject root=JsonParser.parseReader(
                    new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String,Rite> loaded=new LinkedHashMap<>();
            for(JsonElement element:root.getAsJsonArray("entries")) {
                JsonObject row=element.getAsJsonObject();
                String id=row.get("id").getAsString();
                String event=row.get("event").getAsString();
                var patron=PactCatalog.get(id);
                if (patron==null || patron.tier()!=1 || event.isBlank()
                        || loaded.containsKey(id))throw new IllegalStateException("Bad rite: "+id);
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
                    stages.add(new Stage(stage.get("proofs").getAsInt(),materials));
                }
                if(stages.size()!=2)throw new IllegalStateException("Expected two playable rites: "+id);
                loaded.put(id,new Rite(id,event,stages));
            }
            if(loaded.size()!=4)throw new IllegalStateException("Missing a first-tier rite");
            return Map.copyOf(loaded);
        } catch(Exception bad) { throw new IllegalStateException("Invalid first-tier deepening rites",bad); }
    }
}

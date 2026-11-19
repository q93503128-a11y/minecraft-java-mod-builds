import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kr.moonseungjun.anomalousclasses.block.PactAltarBlock;
import kr.moonseungjun.anomalousclasses.state.PactCatalog;
import kr.moonseungjun.anomalousclasses.worldgen.PactAltarPlacement;

/** Checks deterministic identities, ocean-only rules and sixteen shared loot resources.
 * This runs without bootstrapping Minecraft's mutable game registries.
 */
public final class PactSixteenAltarVerifier {
    private static void require(boolean valid, String message) {
        if(!valid)throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        List<List<String>> tiers=List.of(
                List.of("isael","velkan","urgen","ridna"),
                List.of("karda","nerea","shero","hastel"),
                List.of("sevenwing","merzad","taron","arken"),
                List.of("nahar","rakiel","ormas","viar"));
        Set<String> listed=new HashSet<>();
        for(int rank=0;rank<tiers.size();rank++) {
            for(int place=0;place<4;place++) {
                String id=tiers.get(rank).get(place);
                var config=PactCatalog.get(id);
                require(listed.add(id) && config!=null && config.tier()==rank+1,
                        "Every altar has a unique canonical catalog identity: "+id);
                require(PactAltarBlock.index(id)==rank*4+place,
                        "Persisted sixteen-state altar order cannot shift: "+id);
                require(config.enabled() && config.accessible()==(rank==0),
                        "Only tier-one covenants work without an altar: "+id);
                String resource="/data/anomalousclasses/loot_table/chests/pact_"+id+".json";
                try(var stream=PactSixteenAltarVerifier.class.getResourceAsStream(resource)) {
                    require(stream!=null,"Every altar has public exploration loot: "+id);
                    JsonObject root=JsonParser.parseReader(
                            new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
                    require("minecraft:chest".equals(root.get("type").getAsString()),
                            "Loot table remains an ordinary shared chest: "+id);
                    var pools=root.getAsJsonArray("pools");
                    require(pools!=null && pools.size()>=1,"Loot pool is not empty: "+id);
                    int entries=0;
                    for(JsonElement poolElement:pools) {
                        JsonObject pool=poolElement.getAsJsonObject();
                        require(pool.get("rolls").getAsInt()>0,"Positive chest rolls: "+id);
                        for(JsonElement itemElement:pool.getAsJsonArray("entries")) {
                            JsonObject item=itemElement.getAsJsonObject();
                            require("minecraft:item".equals(item.get("type").getAsString()),
                                    "Only direct item entries for shared chests: "+id);
                            require(item.get("name").getAsString().startsWith("minecraft:"),
                                    "No unverified external-item dependency: "+id);
                            entries++;
                        }
                    }
                    require(entries>=2,"Each shrine offers more than one find: "+id);
                }
            }
        }
        require(listed.size()==16 && PactCatalog.all().size()==16,
                "Exactly sixteen patron identities preserved");

        int[] counts=new int[4];
        Set<String> generated=new HashSet<>();
        int locations=0;
        for(long rx=-30;rx<=30;rx++)for(long rz=-30;rz<=30;rz++) {
            int[] target=PactAltarPlacement.targetChunk(rx,rz);
            require(Math.floorDiv(target[0],40)==rx && Math.floorDiv(target[1],40)==rz,
                    "One altar attempt per shared forty-chunk region");
            require(Math.floorMod(target[0],40)>=7 && Math.floorMod(target[0],40)<=32,
                    "Footprint remains safely inside candidate chunk");
            require(Math.floorMod(target[1],40)>=7 && Math.floorMod(target[1],40)<=32,
                    "Footprint remains safely inside candidate chunk");
            String dry=PactAltarPlacement.selectPatron(target[0],target[1],false);
            String sea=PactAltarPlacement.selectPatron(target[0],target[1],true);
            require("nerea".equals(sea),"Only Nerea may spawn on an ocean floor");
            require(!"nerea".equals(dry) && listed.contains(dry),
                    "Fifteen other identities remain dry-land shrines");
            generated.add(dry);
            counts[PactCatalog.get(dry).tier()-1]++;
            locations++;
        }
        require(generated.size()==15 && !generated.contains("nerea"),
                "All fifteen dry patrons are selectable");
        require(locations==61*61,"No change in shared worldgen attempt density");
        double[] wanted={.40,.30,.20,.10};
        for(int index=0;index<4;index++) {
            double observed=counts[index]/(double)locations;
            require(Math.abs(observed-wanted[index])<.05,
                    "Dry tier "+(index+1)+" distribution deviated from planning baseline: "+observed);
        }
        System.out.println("Sixteen shrines, shared loot, ocean-only Nerea, stable block IDs, "
                +"one placement grid, dry distribution: PASS "
                +counts[0]+"/"+counts[1]+"/"+counts[2]+"/"+counts[3]);
    }
}

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Method;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.state.PactBlessingService;
import kr.moonseungjun.anomalousclasses.state.PactCatalog;
import kr.moonseungjun.anomalousclasses.state.PactDeepeningService;
import kr.moonseungjun.anomalousclasses.state.PactSavedData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Verifies endgame rite design, save isolation, real progression bounds. */
public final class PactTierFourDeepeningVerifier {
    private static void require(boolean yes,String msg) {
        if(!yes)throw new AssertionError(msg);
    }
    private static PactSavedData reload(PactSavedData data) throws Exception {
        Method load=PactSavedData.class.getDeclaredMethod(
                "load",CompoundTag.class,net.minecraft.core.HolderLookup.Provider.class);
        load.setAccessible(true);
        return (PactSavedData)load.invoke(null,
                data.save(new CompoundTag(),RegistryAccess.EMPTY),RegistryAccess.EMPTY);
    }
    private static UUID playerId(int index) {
        return UUID.fromString("00000000-0000-0000-0000-"+String.format("%012d",index));
    }
    private static void localized(List<String> ids) throws Exception {
        for(String lang:List.of("ko_kr","en_us")) {
            String name="/assets/anomalousclasses/lang/"+lang+".json";
            try(var stream=PactTierFourDeepeningVerifier.class.getResourceAsStream(name)) {
                require(stream!=null,"Missing translated covenant catalogue: "+lang);
                JsonObject json=JsonParser.parseReader(
                        new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
                for(String id:ids) {
                    for(String key:List.of(
                            "pact_deepening_goal.anomalousclasses."+id+".1",
                            "pact_deepening_goal.anomalousclasses."+id+".2",
                            "pact_deepening_reward.anomalousclasses."+id+".1",
                            "pact_deepening_reward.anomalousclasses."+id+".2",
                            "pact_terms.anomalousclasses."+id,
                            "pact_relation.anomalousclasses."+id)) {
                        require(json.has(key) && !json.get(key).getAsString().isBlank(),
                                "No raw missing player-facing text in "+lang+": "+key);
                    }
                }
            }
        }
    }
    public static void main(String[] args) throws Exception {
        List<String> ids=List.of("nahar","rakiel","ormas","viar");
        localized(ids);
        require(PactDeepeningService.all().size()==16,
                "All sixteen pact identities have voluntary rite definitions");
        Set<String> events=new HashSet<>();
        for(String id:ids) {
            PactCatalog.Patron patron=PactCatalog.get(id);
            PactDeepeningService.Rite rite=PactDeepeningService.rite(id);
            require(patron!=null && patron.tier()==4 && rite!=null
                    && rite.patron().equals(id) && rite.stages().size()==2,
                    "Fourth-tier patron has its own two independent rites: "+id);
            var first=PactDeepeningService.next(id,0);
            var second=PactDeepeningService.next(id,1);
            require(first!=null && second!=null && PactDeepeningService.next(id,2)==null,
                    "Finite endgame rite stages: "+id);
            require(first.event().equals(rite.event()) && !first.event().equals(second.event())
                    && events.add(first.event()) && events.add(second.event()),
                    "Phase two is a genuinely different achievement, not a second click: "+id);
            require(first.proofs()>=2 && first.proofs()<=5
                    && second.proofs()>=2 && second.proofs()<=5,
                    "Distinct real milestones with bounded replay resistance: "+id);
            require(first.offering().size()>=2 && second.offering().size()>=2,
                    "Each stage has a separate informed one-off payment: "+id);
            for(var stage:rite.stages())for(var m:stage.offering())
                require(m.count()>0
                        && m.itemId().getNamespace().equals("minecraft")
                        && !Set.of("nether_star","echo_shard","netherite_ingot",
                             "ancient_debris","enchanted_golden_apple")
                              .contains(m.itemId().getPath()),
                        "No repeat of the initial fourth-tier ultra-rare entry cost: "+id);
            require(Math.abs(PactBlessingService.deepeningFactor(patron,0)-1.0)<1e-8
                    && Math.abs(PactBlessingService.deepeningFactor(patron,1)-1.08)<1e-8
                    && Math.abs(PactBlessingService.deepeningFactor(patron,2)-1.20)<1e-8,
                    "All buffs scale only the fourth-tier borrowed increase: "+id);
        }
        require(PactDeepeningService.next("viar",1).event()
                    .equals("renewal_in_distinct_biomes")
                && PactDeepeningService.next("ormas",1).event()
                    .equals("continent_warded_elite_victory")
                && PactDeepeningService.next("rakiel",1).event()
                    .equals("oath_guarded_elite_victory")
                && PactDeepeningService.next("nahar",1).event()
                    .equals("star_recorded_elite_victory"),
                "Nahar/Rakiel/Ormas require attributed strong-foe victories, Viar needs diverse-biome survival");
        require(Math.abs(PactBlessingService.deepeningFactor(PactCatalog.get("isael"),2)-1.25)<1e-8
                    && Math.abs(PactBlessingService.deepeningFactor(PactCatalog.get("nerea"),2)-1.22)<1e-8
                    && Math.abs(PactBlessingService.deepeningFactor(PactCatalog.get("arken"),2)-1.18)<1e-8,
                "Earlier tiers preserve all earned grace factors");
        UUID owner=playerId(410),other=playerId(411);
        PactSavedData saved=new PactSavedData();
        require(!saved.addDeepeningProof(owner,"nahar",3)
                && !saved.addViarBiomeProof(owner,ResourceLocation.withDefaultNamespace("plains")),
                "Neither unsigned faction evidence nor forged biome access grants progress");
        for(String id:ids)require(saved.sign(owner,id),"Four independent fourth-tier bonds");
        for(String id:ids)require(saved.completeTrial(owner,id),"Persist original one-time guardian proof");
        int nFirst=PactDeepeningService.requirement("nahar",0);
        require(!saved.addDeepeningProof(owner,"nahar",nFirst-1)
                && !saved.deepen(owner,"nahar",0,nFirst-1),
                "Cannot forge a lower rite threshold");
        for(int n=0;n<nFirst;n++)
            require(saved.addDeepeningProof(owner,"nahar",nFirst),"Actual Nahar evidence recorded");
        require(!saved.addDeepeningProof(owner,"nahar",nFirst),"Stage cap enforced");
        require(!saved.deepen(owner,"nahar",1,nFirst),"Cannot skip to stage two");
        saved=reload(saved);
        require(saved.player(owner).bond("nahar").progress()==nFirst
                && saved.player(owner).hasProof("nahar"),
                "Progress and entry guardian proof survive NBT round trip");
        require(saved.deepen(owner,"nahar",0,nFirst)
                && !saved.deepen(owner,"nahar",0,nFirst),
                "First rite server-validated and never redeemed twice");
        int nSecond=PactDeepeningService.requirement("nahar",1);
        for(int n=0;n<nSecond;n++)
            require(saved.addDeepeningProof(owner,"nahar",nSecond),
                    "Attributed strong foe victories may advance a separate second rite");
        require(saved.deepen(owner,"nahar",1,nSecond)
                && !saved.addDeepeningProof(owner,"nahar",nSecond),
                "Completed endgame cannot farm infinite rite progress");
        int viarFirst=PactDeepeningService.requirement("viar",0);
        for(int i=0;i<viarFirst;i++)
            require(saved.addDeepeningProof(owner,"viar",viarFirst),"Real rooted healing phase one");
        require(saved.deepen(owner,"viar",0,viarFirst),"Survival mastery precedes biome exploration");
        var plains=ResourceLocation.withDefaultNamespace("plains");
        var forest=ResourceLocation.withDefaultNamespace("forest");
        var swamp=ResourceLocation.withDefaultNamespace("swamp");
        require(saved.addViarBiomeProof(owner,plains),"First genuine distinct biome");
        require(!saved.addViarBiomeProof(owner,plains),"Cannot farm the same biome");
        saved=reload(saved);
        require(saved.player(owner).bond("viar").progress()==1
                && saved.player(owner).bond("viar").viarSurvivalBiomes().contains(plains),
                "Biome identity and stage progress persist across restart");
        require(!saved.addViarBiomeProof(owner,plains)
                && saved.addViarBiomeProof(owner,forest)
                && saved.addViarBiomeProof(owner,swamp),
                "Different biomes each count once, even after restart");
        require(!saved.addViarBiomeProof(owner,swamp)
                && saved.player(owner).bond("viar").progress()==3,
                "No overflow or same-biome duplicate");
        require(saved.deepen(owner,"viar",1,3)
                && saved.player(owner).bond("viar").viarSurvivalBiomes().isEmpty(),
                "Completed survival clears proof collection without touching other patrons");
        saved=reload(saved);
        require(saved.player(owner).bond("nahar").deepening()==2
                && saved.player(owner).bond("viar").deepening()==2
                && saved.player(owner).bond("ormas").progress()==0
                && saved.player(owner).bond("rakiel").progress()==0,
                "Fourth-tier contracts remain independently persisted");
        require(saved.sign(other,"viar"),"Second player joins the same pantheon");
        require(!saved.addViarBiomeProof(other,plains)
                && saved.player(other).bond("viar").progress()==0,
                "No cross-player biome proof leak or second-phase skip");
        require(saved.breakBond(owner,"viar") && saved.sign(owner,"viar")
                && saved.player(owner).bond("viar").deepening()==0
                && saved.player(owner).bond("viar").progress()==0,
                "Re-signing cannot steal refunded stages or free offerings");
        require(saved.player(owner).hasProof("viar"),"Prior admission trial preserved");
        System.out.println("Four earned fourth-tier quests, strong-hostile conquest phases, "
                +"distinct-biome NBT, legacy stages, payments, multi-bond isolation and no replay: PASS");
    }
}

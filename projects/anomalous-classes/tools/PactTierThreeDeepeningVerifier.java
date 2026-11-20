import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.state.PactBlessingService;
import kr.moonseungjun.anomalousclasses.state.PactCatalog;
import kr.moonseungjun.anomalousclasses.state.PactDeepeningService;
import kr.moonseungjun.anomalousclasses.state.PactSavedData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;

/** Contract-specific earned proof and immutable persistence; no fake client gameplay. */
public final class PactTierThreeDeepeningVerifier {
    private static void require(boolean value,String reason) {
        if(!value)throw new AssertionError(reason);
    }
    private static PactSavedData reload(PactSavedData state) throws Exception {
        Method loader=PactSavedData.class.getDeclaredMethod(
                "load",CompoundTag.class,net.minecraft.core.HolderLookup.Provider.class);
        loader.setAccessible(true);
        return (PactSavedData)loader.invoke(null,
                state.save(new CompoundTag(),RegistryAccess.EMPTY),RegistryAccess.EMPTY);
    }
    public static void main(String[] args) throws Exception {
        var patrons=List.of("sevenwing","merzad","taron","arken");
        var eventByPatron=Map.of(
                "sevenwing","marked_verdict_landed",
                "merzad","crowned_flame_chain",
                "taron","armored_mountain_sunder",
                "arken","observed_record_reprise");
        require(PactDeepeningService.all().size()==12,
                "Four unique rites per each of the first three tiers");
        Set<String> distinct=new HashSet<>();
        for(String id:patrons) {
            PactCatalog.Patron patron=PactCatalog.get(id);
            PactDeepeningService.Rite rite=PactDeepeningService.rite(id);
            require(patron!=null && patron.tier()==3 && rite!=null
                    && rite.patron().equals(id) && rite.event().equals(eventByPatron.get(id))
                    && distinct.add(rite.event()),"Distinct third-tier combat achievement: "+id);
            require(rite.stages().size()==2 && PactDeepeningService.next(id,2)==null,
                    "Two voluntary rites without unlimited recurring offerings: "+id);
            int lastProof=0;
            for(int depth=0;depth<2;depth++) {
                var stage=PactDeepeningService.next(id,depth);
                require(stage.proofs()>lastProof && stage.proofs()<=10
                        && stage.offering().size()>=2,
                        "An earned stage with real requirements: "+id);
                lastProof=stage.proofs();
                for(PactCatalog.Material item:stage.offering()) {
                    require(item.count()>0 && item.itemId().getNamespace().equals("minecraft")
                            && !Set.of("nether_star","echo_shard","enchanted_golden_apple")
                                  .contains(item.itemId().getPath()),
                            "No repeating exceptional entry relic at stage "+depth+": "+id);
                }
            }
            require(Math.abs(PactBlessingService.deepeningFactor(patron,0)-1.00)<1e-9
                    && Math.abs(PactBlessingService.deepeningFactor(patron,1)-1.08)<1e-9
                    && Math.abs(PactBlessingService.deepeningFactor(patron,2)-1.18)<1e-9,
                    "Tier-three specific borrowed grace bonus: "+id);
        }
        require(PactDeepeningService.rite("nahar")==null
                && PactDeepeningService.rite("rakiel")==null
                && PactDeepeningService.rite("ormas")==null
                && PactDeepeningService.rite("viar")==null,
                "Fourth-tier manifestation and unapproved quest details remain separate");
        require(Math.abs(PactBlessingService.deepeningFactor(PactCatalog.get("isael"),1)-1.12)<1e-9
                && Math.abs(PactBlessingService.deepeningFactor(PactCatalog.get("nerea"),2)-1.22)<1e-9,
                "Earlier tier progression unchanged");
        UUID owner=UUID.fromString("00000000-0000-0000-0000-000000000313");
        PactSavedData saved=new PactSavedData();
        require(!saved.addDeepeningProof(owner,"sevenwing",3)
                && !saved.deepen(owner,"sevenwing",0,3),
                "An unsigned covenant cannot receive evidence or paid upgrades");
        for(String id:patrons)
            require(saved.sign(owner,id),"Multiple third-tier contracts remain independent: "+id);
        require(saved.completeTrial(owner,"sevenwing"),"First guardian proof recorded");
        saved.relation(owner,"merzad",6);
        int originalMerzad=saved.player(owner).bond("merzad").relation();
        int first=PactDeepeningService.requirement("sevenwing",0);
        require(!saved.addDeepeningProof(owner,"sevenwing",1)
                && !saved.deepen(owner,"sevenwing",0,1),
                "Caller cannot forge a smaller ritual proof threshold");
        for(int n=0;n<first;n++)
            require(saved.addDeepeningProof(owner,"sevenwing",first),
                    "Real, server-authorized combat evidence progresses");
        require(!saved.addDeepeningProof(owner,"sevenwing",first),
                "Evidence cannot overflow the next stage");
        require(!saved.deepen(owner,"sevenwing",1,first),
                "No out-of-order upgrades");
        require(saved.player(owner).bond("merzad").progress()==0
                && saved.player(owner).bond("merzad").relation()==originalMerzad,
                "One patron's actions cannot advance another's relationship");
        saved=reload(saved);
        require(saved.player(owner).bond("sevenwing").progress()==first
                && saved.player(owner).hasProof("sevenwing"),
                "Guardian proof and incomplete ritual survive NBT save/restart");
        require(saved.deepen(owner,"sevenwing",0,first),
                "First authorized ritual can be completed exactly once");
        require(!saved.deepen(owner,"sevenwing",0,first)
                && saved.player(owner).bond("sevenwing").progress()==0,
                "No duplicated rite redemption or carried-over progress");
        int second=PactDeepeningService.requirement("sevenwing",1);
        for(int n=0;n<second;n++)
            require(saved.addDeepeningProof(owner,"sevenwing",second),
                    "Independent second-stage combat evidence");
        require(saved.deepen(owner,"sevenwing",1,second),
                "Second voluntary rite confirmed");
        require(!saved.addDeepeningProof(owner,"sevenwing",second)
                && !saved.deepen(owner,"sevenwing",2,second),
                "No infinite deepening after stage two");
        for(String id:List.of("merzad","taron","arken")) {
            int target=PactDeepeningService.requirement(id,0);
            for(int n=0;n<target;n++)
                require(saved.addDeepeningProof(owner,id,target),"Each pact keeps its own progress");
        }
        saved=reload(saved);
        require(saved.player(owner).bond("sevenwing").deepening()==2
                && saved.player(owner).bond("sevenwing").progress()==0
                && saved.player(owner).hasProof("sevenwing"),
                "Finished upgrade and guardian proof persist");
        for(String id:List.of("merzad","taron","arken"))
            require(saved.player(owner).bond(id).deepening()==0
                    && saved.player(owner).bond(id).progress()
                         ==PactDeepeningService.requirement(id,0),
                    "Third-tier stage and evidence never bleed into other pacts");
        require(saved.breakBond(owner,"sevenwing"),"Formal release");
        require(saved.sign(owner,"sevenwing")
                && saved.player(owner).bond("sevenwing").deepening()==0
                && saved.player(owner).bond("sevenwing").progress()==0
                && saved.player(owner).hasProof("sevenwing"),
                "Re-signed pact restarts depth but prior one-time guardian proof persists");
        System.out.println("Four tier-three earned rites, no repeated rare relics, "
                +"separate guardian proof, NBT upgrades, multi-bond isolation and no replay: PASS");
    }
}

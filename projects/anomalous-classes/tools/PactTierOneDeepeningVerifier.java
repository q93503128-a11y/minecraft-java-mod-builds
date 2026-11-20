import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.state.PactBlessingService;
import kr.moonseungjun.anomalousclasses.state.PactCatalog;
import kr.moonseungjun.anomalousclasses.state.PactDeepeningService;
import kr.moonseungjun.anomalousclasses.state.PactSavedData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;

public final class PactTierOneDeepeningVerifier {
    private static void require(boolean yes,String reason) {
        if(!yes)throw new AssertionError(reason);
    }
    public static void main(String[] args) throws Exception {
        var expected=List.of("isael","velkan","urgen","ridna");
        require(PactDeepeningService.all().keySet().containsAll(expected)
                && PactDeepeningService.all().size()==4,
                "Four distinct first-tier rites");
        var events=new java.util.HashSet<String>();
        for(String patron:expected) {
            var config=PactDeepeningService.rite(patron);
            require(config!=null && events.add(config.event()),"Distinct event identity: "+patron);
            require(config.stages().size()==2 && PactDeepeningService.next(patron,2)==null,
                    "No unbounded click-to-upgrade path: "+patron);
            require(PactCatalog.get(patron).tier()==1,"First-tier contract only");
            for(int level=0;level<2;level++) {
                var stage=PactDeepeningService.next(patron,level);
                require(stage.proofs()>=2 && stage.proofs()<=10,"Attainable proof budget");
                for(var material:stage.offering()) {
                    require(material.count()>0 && material.itemId()!=null
                            && material.itemId().getNamespace().equals("minecraft"),
                            "Usable low-tier offering: "+patron);
                }
            }
            var first=PactDeepeningService.next(patron,0).offering();
            var second=PactDeepeningService.next(patron,1).offering();
            require(first.stream().noneMatch(i->i.itemId().getPath().equals("nether_star")
                            || i.itemId().getPath().equals("echo_shard"))
                    && second.stream().noneMatch(i->i.itemId().getPath().equals("nether_star")
                            || i.itemId().getPath().equals("echo_shard")),
                    "No repeated endgame relics in common early rites");
        }
        require(PactDeepeningService.rite("nahar")==null
                && PactDeepeningService.next("rakiel",0)==null,
                "Unapproved fourth-tier quest sets are not silently implemented");
        require(Math.abs(PactBlessingService.deepeningFactor(0)-1.0)<1e-9
                && Math.abs(PactBlessingService.deepeningFactor(1)-1.12)<1e-9
                && Math.abs(PactBlessingService.deepeningFactor(2)-1.25)<1e-9,
                "Each rite changes borrowed graces but not the underlying player base");
        UUID owner=UUID.fromString("00000000-0000-0000-0000-000000000018");
        PactSavedData state=new PactSavedData();
        require(!state.addDeepeningProof(owner,"isael",3),"Unsigned rites may never progress");
        require(state.sign(owner,"isael"),"First signature");
        require(!state.addDeepeningProof(owner,"isael",1)
                && !state.addDeepeningProof(owner,"isael",100),
                "Proof threshold must match the rite definition, not a supplied shortcut");
        require(state.sign(owner,"velkan"),"Second signature independently");
        int firstCount=PactDeepeningService.requirement("isael",0);
        for(int n=0;n<firstCount;n++)
            require(state.addDeepeningProof(owner,"isael",firstCount),"Server evidence persisted");
        require(!state.addDeepeningProof(owner,"isael",firstCount),
                "Evidence capped; repeated events cannot overflow next rite");
        require(!state.deepen(owner,"isael",1,firstCount),"Out-of-order confirmation blocked");
        require(!state.deepen(owner,"isael",0,1),
                "Cannot forge a smaller completion threshold");
        require(state.player(owner).bond("velkan").progress()==0,
                "Other contracts cannot inherit the oath evidence");
        CompoundTag pending=state.save(new CompoundTag(),RegistryAccess.EMPTY);
        Method load=PactSavedData.class.getDeclaredMethod(
                "load",CompoundTag.class,net.minecraft.core.HolderLookup.Provider.class);
        load.setAccessible(true);
        PactSavedData restored=(PactSavedData)load.invoke(null,pending,RegistryAccess.EMPTY);
        require(restored.player(owner).bond("isael").progress()==firstCount,
                "An unfinished earned rite survives restart");
        require(restored.deepen(owner,"isael",0,firstCount),"First rite completed");
        require(!restored.deepen(owner,"isael",0,firstCount),"Cannot redeem the first rite twice");
        require(restored.player(owner).bond("isael").deepening()==1
                && restored.player(owner).bond("isael").progress()==0,
                "Rite transition resets only its own progress");
        int secondCount=PactDeepeningService.requirement("isael",1);
        for(int n=0;n<secondCount;n++)
            require(restored.addDeepeningProof(owner,"isael",secondCount),"Second-stage evidence");
        require(restored.deepen(owner,"isael",1,secondCount),"Final rite");
        require(!restored.addDeepeningProof(owner,"isael",secondCount),
                "No infinite progression after maximum");
        CompoundTag completed=restored.save(new CompoundTag(),RegistryAccess.EMPTY);
        PactSavedData again=(PactSavedData)load.invoke(null,completed,RegistryAccess.EMPTY);
        require(again.player(owner).bond("isael").deepening()==2,
                "Second rite persists across a real NBT roundtrip");
        require(again.player(owner).bond("velkan").deepening()==0,
                "Bond states remain separate even after migration");
        require(again.breakBond(owner,"isael"),"Voluntary release");
        require(again.sign(owner,"isael")
                && again.player(owner).bond("isael").deepening()==0,
                "A new contract cannot retrieve an old paid rite for free");
        System.out.println("Four earned first-tier rites, separate evidence, staged costs, NBT and "
                +"no free replay: PASS");
    }
}

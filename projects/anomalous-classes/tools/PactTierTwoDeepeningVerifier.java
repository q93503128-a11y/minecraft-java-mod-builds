import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import kr.moonseungjun.anomalousclasses.state.PactBlessingService;
import kr.moonseungjun.anomalousclasses.state.PactCatalog;
import kr.moonseungjun.anomalousclasses.state.PactDeepeningService;
import kr.moonseungjun.anomalousclasses.state.PactSavedData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;

/** Bootstrapping-free validation of tier-two rites and existing tier-one migrations. */
public final class PactTierTwoDeepeningVerifier {
    private static void check(boolean ok,String why) {
        if(!ok)throw new AssertionError(why);
    }
    public static void main(String[] args) throws Exception {
        var ids=List.of("karda","nerea","shero","hastel");
        var names=new HashSet<String>();
        var events=new HashSet<String>();
        check(PactDeepeningService.all().size()==16,"All sixteen tier-specific rites");
        for(String id:ids) {
            var patron=PactCatalog.get(id);
            var rite=PactDeepeningService.rite(id);
            check(patron!=null && patron.tier()==2 && rite!=null && ids.contains(rite.patron()),
                    "Four second-tier signed patrons: "+id);
            check(names.add(rite.patron()) && events.add(rite.event()),
                    "Distinct rite and achievement: "+id);
            check(rite.stages().size()==2 && PactDeepeningService.next(id,2)==null,
                    "No unlimited material-click levels: "+id);
            for(int stage=0;stage<2;stage++) {
                var goal=rite.stages().get(stage);
                check(goal.proofs()>=2 && goal.proofs()<=10,
                        "Achievable distinct combat progress: "+id);
                check(goal.offering().size()>=2,"Different materials required: "+id);
                for(var item:goal.offering())check(item.count()>0
                        && "minecraft".equals(item.itemId().getNamespace())
                        && !List.of("nether_star","echo_shard","enchanted_golden_apple")
                             .contains(item.itemId().getPath()),
                        "No repeated high-tier covenant entry fee: "+id);
            }
        }
        check(PactDeepeningService.rite("nahar")!=null
                && PactDeepeningService.rite("rakiel")!=null,
                "Fourth-tier quests are now separately registered");
        check(Math.abs(PactBlessingService.deepeningFactor(PactCatalog.get("karda"),0)-1)<1e-8
                && Math.abs(PactBlessingService.deepeningFactor(PactCatalog.get("karda"),1)-1.10)<1e-8
                && Math.abs(PactBlessingService.deepeningFactor(PactCatalog.get("karda"),2)-1.22)<1e-8,
                "Tier-two tier-specific boon progression");
        check(Math.abs(PactBlessingService.deepeningFactor(PactCatalog.get("isael"),1)-1.12)<1e-8,
                "Original first-tier bonus preserved");
        check(Math.abs(PactBlessingService.deepeningFactor(PactCatalog.get("nahar"),2)-1.20)<1e-8,
                "Fourth-tier progression uses its own tier-specific trial factor");
        UUID owner=UUID.fromString("00000000-0000-0000-0000-000000000022");
        PactSavedData saved=new PactSavedData();
        check(!saved.addDeepeningProof(owner,"hastel",3),
                "Unsigned debt grants no rite progress");
        check(saved.sign(owner,"karda") && saved.sign(owner,"nerea")
                && saved.sign(owner,"shero") && saved.sign(owner,"hastel"),
                "Multiple independent second-tier bonds can coexist");
        saved.debt(owner,"hastel",9);
        check(!saved.addDeepeningProof(owner,"karda",1),
                "Cannot lower a canonical rite threshold");
        int first=PactDeepeningService.requirement("karda",0);
        for(int i=0;i<first;i++)check(saved.addDeepeningProof(owner,"karda",first),
                "Valid distinct evidence recorded");
        check(!saved.addDeepeningProof(owner,"karda",first),"Stage evidence capped");
        check(!saved.deepen(owner,"karda",0,1),"Forged approval threshold rejected");
        check(!saved.deepen(owner,"karda",1,first),"Wrong-order approval rejected");
        check(saved.player(owner).bond("nerea").progress()==0
                && saved.player(owner).bond("hastel").debt()==9,
                "Other patrons' progress and debts unaffected");
        Method loader=PactSavedData.class.getDeclaredMethod("load",
                CompoundTag.class,net.minecraft.core.HolderLookup.Provider.class);
        loader.setAccessible(true);
        var stored=saved.save(new CompoundTag(),RegistryAccess.EMPTY);
        var recovered=(PactSavedData)loader.invoke(null,stored,RegistryAccess.EMPTY);
        check(recovered.player(owner).bond("karda").progress()==first,
                "Progress survives server restart");
        check(recovered.deepen(owner,"karda",0,first),
                "Stage-one rite server-validated");
        check(!recovered.deepen(owner,"karda",0,first),
                "Cannot redeem the same rite twice");
        int second=PactDeepeningService.requirement("karda",1);
        for(int i=0;i<second;i++)check(recovered.addDeepeningProof(owner,"karda",second),
                "Stage-two actual evidence");
        check(recovered.deepen(owner,"karda",1,second),"Stage-two rite server-validated");
        check(!recovered.addDeepeningProof(owner,"karda",second),
                "No repeated further rite after completion");
        var finalState=(PactSavedData)loader.invoke(null,
                recovered.save(new CompoundTag(),RegistryAccess.EMPTY),RegistryAccess.EMPTY);
        check(finalState.player(owner).bond("karda").deepening()==2
                && finalState.player(owner).bond("karda").progress()==0,
                "Progress transitions persist");
        check(finalState.player(owner).bond("hastel").debt()==9,
                "Debt cannot disappear through another patron's deepening");
        check(finalState.breakBond(owner,"karda") && finalState.sign(owner,"karda")
                && finalState.player(owner).bond("karda").deepening()==0,
                "Re-signing never gives old rite benefits free");
        System.out.println("Tier-two distinct rites, canon thresholds, multi-bond debt, "
                +"NBT roundtrip, proof cap and paid stage transitions: PASS");
    }
}

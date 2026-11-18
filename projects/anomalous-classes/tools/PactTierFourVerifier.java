import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.block.PactAltarBlock;
import kr.moonseungjun.anomalousclasses.state.PactCatalog;
import kr.moonseungjun.anomalousclasses.state.PactSavedData;
import kr.moonseungjun.anomalousclasses.worldgen.PactAltarPlacement;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;

public final class PactTierFourVerifier {
    private static void require(boolean condition,String reason) {
        if(!condition)throw new AssertionError(reason);
    }
    public static void main(String[] args) throws Exception {
        var ids=List.of("nahar","rakiel","ormas","viar");
        require(PactCatalog.all().size()==16,"Original sixteen patrons intact");
        require(PactCatalog.all().values().stream().filter(PactCatalog.Patron::enabled).count()==16,
                "All four tiers playable");
        require(PactCatalog.introductory().size()==4,"Only tier one remotely available");
        for(String id:ids) {
            var patron=PactCatalog.get(id);
            require(patron!=null && patron.tier()==4 && patron.enabled() && !patron.accessible(),
                    "Fourth-tier patrons require an altar: "+id);
            require(patron.offering().size()>0 && patron.release().size()>0,
                    "Covenant and withdrawal prices are mandatory: "+id);
            require(patron.abilities().size()>=1 && patron.abilities().size()<=2,
                    "Fourth-tier powers stay focused: "+id);
            require(PactAltarBlock.index(id)>=12 && PactAltarBlock.index(id)<=15,
                    "Persisted blockstate index preserved: "+id);
            for(var item:patron.offering()) {
                require(item.itemId()!=null && "minecraft".equals(item.itemId().getNamespace())
                        && !item.itemId().getPath().isBlank() && item.count()>0,
                        "Well-formed positive vanilla offering: "+id);
                require(PactCatalog.required(item,2)==item.count()*4L,
                        "Three concurrent covenants pay fourfold: "+id);
            }
        }
        require(PactCatalog.get("ormas").abilities().get(0).id().equals("continent_skin"),
                "Continental skin remains primary defensive skill");
        require(PactCatalog.get("ormas").abilities().get(1).id().equals("continent_break"),
                "Continental break remains independently castable");
        UUID owner=UUID.fromString("00000000-0000-0000-0000-000000000014");
        PactSavedData saved=new PactSavedData();
        require(saved.completeTrial(owner,"nahar"),"Earned fourth-tier proof");
        require(!saved.completeTrial(owner,"nahar"),"Proof cannot be repeatedly granted");
        require(!saved.player(owner).has("nahar"),"Proof alone never signs a covenant");
        require(saved.completeTrial(owner,"rakiel") && saved.completeTrial(owner,"ormas")
                && saved.completeTrial(owner,"viar"),"All four unique proofs recorded");
        require(saved.sign(owner,"rakiel"),"Signed bond recorded separately");
        saved.cooldown(owner,"rakiel:fate_refusal",1_985_000_000_000L);
        CompoundTag nbt=saved.save(new CompoundTag(),RegistryAccess.EMPTY);
        Method load=PactSavedData.class.getDeclaredMethod("load",CompoundTag.class,
                net.minecraft.core.HolderLookup.Provider.class);
        load.setAccessible(true);
        PactSavedData restored=(PactSavedData)load.invoke(null,nbt,RegistryAccess.EMPTY);
        for(String id:ids)
            require(restored.player(owner).hasProof(id),"Earned proof persisted: "+id);
        require(restored.player(owner).has("rakiel"),"Bond survived save roundtrip");
        require(!restored.player(owner).has("nahar"),"Unsigned proof did not become a bond");
        require(restored.player(owner).readyAt("rakiel:fate_refusal")==1_985_000_000_000L,
                "Single-event fate rejection cooldown survives server restart");
        Set<String> seen=new HashSet<>();
        int second=0,third=0,fourth=0;
        for(int rx=-30;rx<=30;rx++)for(int rz=-30;rz<=30;rz++) {
            int[] goal=PactAltarPlacement.targetChunk(rx,rz);
            require(Math.floorDiv(goal[0],40)==rx && Math.floorDiv(goal[1],40)==rz,
                    "One shared forty-chunk region");
            String id=PactAltarPlacement.selectLandPatron(goal[0],goal[1]);
            seen.add(id);
            int tier=PactCatalog.get(id).tier();
            if(tier==2)second++;
            else if(tier==3)third++;
            else if(tier==4)fourth++;
            else throw new AssertionError("Unexpected or underwater land altar: "+id);
        }
        require(seen.containsAll(List.of("karda","shero","hastel","sevenwing",
                "merzad","taron","arken","nahar","rakiel","ormas","viar")),
                "Every dry land identity must be reachable");
        int total=second+third+fourth;
        require(second>total*.40 && second<total*.60,
                "Tier two remains the most common land choice");
        require(third>total*.25 && third<total*.42,
                "Tier three share remains near a third");
        require(fourth>total*.10 && fourth<total*.25,
                "Fourth-tier land share stays limited without additional grid attempts");
        System.out.println("Tier4 offerings, powers, proof/cooldown NBT, placement distribution: PASS "
                +second+"/"+third+"/"+fourth);
    }
}

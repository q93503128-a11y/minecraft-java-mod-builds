import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.block.PactAltarBlock;
import kr.moonseungjun.anomalousclasses.state.PactCatalog;
import kr.moonseungjun.anomalousclasses.state.PactSavedData;
import kr.moonseungjun.anomalousclasses.worldgen.PactAltarPlacement;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;

public final class PactTierThreeVerifier {
    private static void require(boolean yes,String why) {
        if(!yes) throw new AssertionError(why);
    }
    public static void main(String[] args) throws Exception {
        List<String> patrons=List.of("sevenwing","merzad","taron","arken");
        for(String id:patrons) {
            var patron=PactCatalog.get(id);
            require(patron!=null&&patron.tier()==3&&patron.enabled()&&!patron.accessible(),
                    "Tier three requires a real altar: "+id);
            require(!patron.offering().isEmpty()&&!patron.release().isEmpty(),"Payment and penalty: "+id);
            require(patron.abilities().size()>=1&&patron.abilities().size()<=2,"Combat powers: "+id);
            require(PactAltarBlock.index(id)>=8&&PactAltarBlock.index(id)<=11,"Stable world blockstate: "+id);
        }
        require(PactCatalog.all().size()==16,"All existing names preserved");
        require(PactCatalog.all().values().stream().filter(PactCatalog.Patron::enabled).count()==16,
                "All tier-four patrons enabled");
        UUID player=UUID.fromString("00000000-0000-0000-0000-000000000013");
        PactSavedData save=new PactSavedData();
        require(save.completeTrial(player,"sevenwing"),"First legitimate trial");
        require(!save.completeTrial(player,"sevenwing"),"Cannot repeat the same proof");
        require(save.completeTrial(player,"nahar"),"Fourth-tier altar trials use the same earned proof system");
        require(!save.player(player).has("nahar"),"Trial is not a free fourth-tier contract");
        require(!save.player(player).has("sevenwing"),"Proof is not a free contract");
        save.observe(player,"projectile");
        save.observe(player,"melee");
        save.observe(player,"fire");
        require(save.player(player).observations().equals(List.of("melee","fire")),
                "Only two recent combat observations retained");
        save.observe(player,"untrusted_external_spell");
        require(save.player(player).observations().equals(List.of("melee","fire")),
                "Unsupported attacks are never copied");
        CompoundTag compound=save.save(new CompoundTag(),RegistryAccess.EMPTY);
        Method load=PactSavedData.class.getDeclaredMethod("load",CompoundTag.class,
                net.minecraft.core.HolderLookup.Provider.class);
        load.setAccessible(true);
        PactSavedData again=(PactSavedData)load.invoke(null,compound,RegistryAccess.EMPTY);
        require(again.player(player).hasProof("sevenwing"),"Old world data gains proof field");
        require(again.player(player).observations().equals(List.of("melee","fire")),
                "Combat records survive the server save roundtrip");
        require(!again.player(player).has("sevenwing"),"Restored proof remains separate from contract");
        var seen=new java.util.HashSet<String>();
        for(int x=-15;x<=15;x++)for(int z=-15;z<=15;z++) {
            int[] chunk=PactAltarPlacement.targetChunk(x,z);
            seen.add(PactAltarPlacement.selectLandPatron(chunk[0],chunk[1]));
        }
        require(seen.containsAll(List.of("karda","shero","hastel","sevenwing",
                "merzad","taron","arken",
                "nahar","rakiel","ormas","viar")),"Every land identity has reachable altar placement");
        System.out.println("Tier3 altars, hard gating, proof NBT, safe 2-profile records, grid: PASS");
    }
}

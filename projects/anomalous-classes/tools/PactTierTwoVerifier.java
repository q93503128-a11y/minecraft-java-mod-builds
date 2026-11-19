import java.util.HashSet;
import java.util.List;
import kr.moonseungjun.anomalousclasses.block.PactAltarBlock;
import kr.moonseungjun.anomalousclasses.state.PactCatalog;
import kr.moonseungjun.anomalousclasses.worldgen.PactAltarPlacement;

/** Checks shared 40-chunk terrain attempts, stable block identities and four playable patrons. */
public final class PactTierTwoVerifier {
    private static void require(boolean ok,String message) {
        if(!ok)throw new AssertionError(message);
    }
    public static void main(String[] args) {
        require(PactCatalog.all().size()==16,"Sixteen identities");
        require(PactCatalog.all().values().stream().filter(PactCatalog.Patron::enabled).count()==16,
                "All four tiers enabled");
        var ids=List.of("karda","nerea","shero","hastel");
        for(String id:ids){
            var p=PactCatalog.get(id);
            require(p.enabled() && !p.accessible() && p.tier()==2,"Tier-two altar-only: "+id);
            require(!p.offering().isEmpty()&&!p.release().isEmpty(),"Real resource cost: "+id);
            require(p.abilities().size()>=1&&p.abilities().size()<=2,"Ability count: "+id);
            require(PactAltarBlock.index(id)>=4&&PactAltarBlock.index(id)<=7,
                    "Stable world blockstate: "+id);
        }
        int tier3=0;
        var patrons=new java.util.HashSet<String>();
        int count=0;
        var encountered=new HashSet<String>();
        for(int rx=-8;rx<8;rx++)for(int rz=-8;rz<8;rz++){
            int[] pos=PactAltarPlacement.targetChunk(rx,rz);
            require(Math.floorDiv(pos[0],40)==rx && Math.floorDiv(pos[1],40)==rz,"Cell owner");
            require(Math.floorMod(pos[0],40)>=7 && Math.floorMod(pos[0],40)<=32,
                    "No cross-chunk edge");
            count++;
            encountered.add(pos[0]+":"+pos[1]);
            String altar=PactAltarPlacement.selectLandPatron(pos[0],pos[1]);
            patrons.add(altar);
            if (List.of("sevenwing","merzad","taron","arken").contains(altar))tier3++;
            require(pos[0]!=Integer.MIN_VALUE && pos[1]!=Integer.MIN_VALUE,"Finite regions");
        }
        require(count==256 && encountered.size()==256,"One shared candidate per grid region");
        require(tier3>=30 && tier3<=75,"Third-tier dry-land share near one fifth: "+tier3);
        require(patrons.containsAll(List.of("isael","velkan","urgen","ridna",
                "karda","shero","hastel",
                "sevenwing","merzad","taron","arken",
                "nahar","rakiel","ormas","viar")),"All land themes reachable");
        require(PactCatalog.get("hastel").offering().size()==2,"Debt pact has real materials");
        require(PactCatalog.get("nerea").offering().size()==1,"Sea-specific offering");
        System.out.println("Pact tier 2, altar identity, 40-chunk grid, resource gates: PASS");
    }
}

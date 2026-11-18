import java.util.List;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.state.*;

public final class PactFoundationVerifier {
    private static void check(boolean ok,String reason) {
        if (!ok) throw new AssertionError(reason);
    }
    private static void same(double a,double b,String reason) {
        if (!Double.isFinite(b)||Math.abs(a-b)>1e-5) throw new AssertionError(reason+": "+b+" != "+a);
    }
    public static void main(String[] args) {
        check(PactCatalog.all().size()==16,"Sixteen patrons");
        check(PactCatalog.introductory().size()==4,"Four accessible patrons");
        check(PactCatalog.all().values().stream().filter(PactCatalog.Patron::enabled).count()==12,
                "Four patrons each in tiers one, two and three enabled");
        for (String id : List.of("karda","nerea","shero","hastel")) {
            var patron=PactCatalog.get(id);
            check(patron.tier()==2 && patron.enabled() && !patron.accessible(),
                    "Tier two must use physical altar: "+id);
            check(!patron.offering().isEmpty() && !patron.release().isEmpty()
                    && !patron.abilities().isEmpty(), "Tier two offerings and powers: "+id);
        }
        for (var patron : PactCatalog.all().values()) if(patron.tier()>2)
            check(!patron.enabled(), "Higher tier remains locked: "+patron.id());
        for (var p:PactCatalog.introductory()) {
            check(p.tier()==1&&!p.offering().isEmpty()&&!p.release().isEmpty()
                    &&!p.abilities().isEmpty(),"Playable first tier "+p.id());
        }
        for (var p:PactCatalog.all().values())
            if (p.tier()>1) check(!p.accessible(),"Altar required "+p.id());
        same(1,PactCatalog.multiplier(0),"First multiplier");
        same(2,PactCatalog.multiplier(1),"Second multiplier");
        same(4,PactCatalog.multiplier(2),"Third multiplier");
        same(8,PactCatalog.multiplier(3),"Fourth multiplier");
        same(16,PactCatalog.multiplier(4),"Fifth multiplier");
        same(42.5,PactCatalog.aggregate(List.of(25.0,30.0)),"Two blessings");
        same(57.5,PactCatalog.aggregate(List.of(30.0,30.0,25.0)),"Three blessings");
        same(0,PactCatalog.aggregate(List.of()),"No phantom bonus");
        check(PactCatalog.required(PactCatalog.get("urgen").offering().get(0),2)==32,"Third iron sacrifice");
        check(PactCatalog.get("velkan").hpSacrifice()==4,"One-time HP price");
        check(PactCatalog.get("isael").abilities().size()==2,"Mark and ward");
        check(PactCatalog.get("velkan").abilities().size()==1,"One Belkan ability");
        check(PactCatalog.get("urgen").abilities().size()==2,"Two Urgen abilities");
        check(PactCatalog.get("ridna").abilities().size()==2,"Two Ridna abilities");
        same(.75,PactBlessingService.relationFactor(0),"Low faith");
        same(1,PactBlessingService.relationFactor(50),"Starting faith");
        same(1.25,PactBlessingService.relationFactor(100),"High faith");
        PactSavedData data=new PactSavedData();
        UUID player=UUID.fromString("00000000-0000-0000-0000-000000000004");
        check(data.sign(player,"isael"),"Sign first");
        check(!data.sign(player,"isael"),"No duplicate");
        check(data.player(player).bond("isael").relation()==50,"Faith initially 50");
        data.relation(player,"isael",-10);
        check(data.player(player).bond("isael").relation()==40,"Violation penalty");
        check(data.sign(player,"velkan")&&data.sign(player,"urgen"),"Three contracts");
        same(.30+.5*(.25+.15*.95),
                PactBlessingService.combined(data.player(player),"attack"),"Multi attack blessing");
        same(.40+.5*.25*.95,
                PactBlessingService.combined(data.player(player),"hp"),"Multi HP blessing");
        data.cooldown(player,"isael:mark",999_999L);
        check(data.player(player).readyAt("isael:mark")==999_999L,"Persistent cooldown");
        check(data.breakBond(player,"velkan"),"Release");
        check(data.player(player).count()==2,"Remaining contracts");
        System.out.println("Pact 16 identities/4 first-tier, offerings, 3-contract graces: PASS");
    }
}

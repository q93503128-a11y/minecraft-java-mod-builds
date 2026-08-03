package kr.moonseungjun.arcanecircle.world;

import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.registry.ModItems;
import java.util.ArrayList;import java.util.List;import java.util.Optional;

public final class AcademyOfferCatalog {
    public enum Kind{PRIMER,SPELLBOOK,STAFF,GEAR}
    public record Offer(String id,String displayName,String description,int circle,long basePrice,Kind kind,String targetId){}
    private static List<Offer> cached;private AcademyOfferCatalog(){}
    public static List<Offer> offers(){if(cached!=null)return cached;List<Offer> r=new ArrayList<>();r.add(new Offer("primer","초심자 마도서","1써클 기초 주문을 각인합니다.",1,SpellCatalog.arcaneMarkPrice(1),Kind.PRIMER,"beginner_grimoire"));for(SpellDefinition s:SpellCatalog.bookSpells())r.add(new Offer("spell:"+s.id(),"주문서: "+s.name(),s.description(),s.circle(),SpellCatalog.arcaneMarkPrice(s.circle()),Kind.SPELLBOOK,s.id()));long[] prices={0,300,650,1200,4000,12000,36000,180000,1200000};for(int i=1;i<ModItems.profiles().size();i++){var p=ModItems.profiles().get(i);r.add(new Offer("staff:"+p.id(),p.displayName(),p.summary(),Math.min(9,i+1),prices[i],Kind.STAFF,p.id()));}
        gear(r,"mage_hat","비전 모자","입문 마력 효율 모자",2,1800);gear(r,"mage_boots","유랑 마도화","입문 기동 마도화",2,2400);gear(r,"mage_robe","중층 마도 로브","초급 생존·위력 로브",3,7200);
        gear(r,"cinder_hat","잿불 전투모","화염 저항과 공격 위력",4,24000);gear(r,"cinder_robe","잿불 전투로브","화염 공격 특화 로브",4,42000);gear(r,"cinder_boots","화염답화","화염 전투 기동화",4,30000);
        gear(r,"glacier_hat","빙정 관모","동결 제어와 마력 효율",5,60000);gear(r,"glacier_robe","빙정 의복","저항과 동결 범위",5,110000);gear(r,"glacier_boots","설원답화","빙결 보행 기동화",5,80000);
        gear(r,"sage_hat","현자의 모자","고위 마력 운용 모자",5,55000);gear(r,"skywalker_boots","천공 마도화","높은 점프와 체공",5,75000);gear(r,"sage_robe","현자의 로브","고위 범용 로브",6,160000);
        gear(r,"tempest_hat","폭풍 후드","빠른 연속 시전",6,180000);gear(r,"tempest_robe","폭풍비단 로브","가속·범위 특화",6,320000);gear(r,"tempest_boots","천뢰 장화","최상급 지상·공중 기동",6,260000);
        gear(r,"rift_hat","균열 관","공간술 마력 관",8,950000);gear(r,"rift_robe","균열 예복","고위 공간술 예복",8,2100000);gear(r,"rift_boots","성간 보행화","자유 비행과 초장거리 보정",8,1750000);
        gear(r,"archmage_crown","대마도사 관","최상위 범용 관",8,1100000);gear(r,"froststep_boots","빙결 보행화","최상위 빙결 기동화",8,1400000);gear(r,"archmage_robe","대마도사 예복","최상위 범용 예복",9,3200000);
        cached=List.copyOf(r);return cached;}
    private static void gear(List<Offer> r,String id,String name,String desc,int c,long price){r.add(new Offer("gear:"+id,name,desc,c,price,Kind.GEAR,id));}
    public static Optional<Offer> offer(String id){return offers().stream().filter(v->v.id().equals(id)).findFirst();}public static List<Offer> forCircle(int c){return offers().stream().filter(v->c<=0||v.circle()==c).toList();}
}

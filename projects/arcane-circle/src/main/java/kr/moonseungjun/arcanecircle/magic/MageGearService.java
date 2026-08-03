package kr.moonseungjun.arcanecircle.magic;

import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Expanded, role-balanced mage equipment with two-slot robes and high-tier flight footwear. */
public final class MageGearService {
    private static final Set<UUID> ROBE_SLOT_WARNED=new HashSet<>();
    private static final Set<UUID> FLIGHT_GRANTED=new HashSet<>();
    private MageGearService(){}

    public static void tick(ServerPlayer player){
        ItemStack chest=player.getItemBySlot(EquipmentSlot.CHEST),legs=player.getItemBySlot(EquipmentSlot.LEGS);Item expected=hemFor(chest.getItem());
        if(expected!=null&&legs.isEmpty()){player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(expected));ROBE_SLOT_WARNED.remove(player.getUUID());}
        else if(expected==null&&isHem(legs.getItem())){player.setItemSlot(EquipmentSlot.LEGS,ItemStack.EMPTY);ROBE_SLOT_WARNED.remove(player.getUUID());}
        else if(expected!=null&&isHem(legs.getItem())&&legs.getItem()!=expected){player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(expected));ROBE_SLOT_WARNED.remove(player.getUUID());}
        else if(expected!=null&&!legs.isEmpty()&&!isHem(legs.getItem())&&ROBE_SLOT_WARNED.add(player.getUUID()))ArcaneNoticeService.push(player,Component.literal("§c[로브 비활성] §f전투 로브는 몸·바지 두 슬롯을 사용합니다."),100);

        Item boots=player.getItemBySlot(EquipmentSlot.FEET).getItem();int tier=bootsTier(boots);
        if(tier>0){player.addEffect(new MobEffectInstance(MobEffects.SPEED,30,Math.max(0,tier-1),true,false));player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,30,Math.max(0,tier-1),true,false));}
        if(isSlowFallBoots(boots)&&!player.onGround())player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,30,tier>=3?1:0,true,false));
        if(isFrostBoots(boots))freezeWater(player);
        setFlight(player,isFlightBoots(boots));

        Item hat=player.getItemBySlot(EquipmentSlot.HEAD).getItem();
        if(hat==ModItems.RIFT_CROWN.get())player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,240,0,true,false));
        if(hat==ModItems.CINDER_HOOD.get()||chest.getItem()==ModItems.CINDER_ROBE.get())player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,40,0,true,false));
        if(chest.getItem()==ModItems.GLACIER_ROBE.get())player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,40,1,true,false));
        if(chest.getItem()==ModItems.TEMPEST_ROBE.get())player.addEffect(new MobEffectInstance(MobEffects.SPEED,40,1,true,false));
        int robeTier=robeTier(chest.getItem());if(robeTier>0&&expected==player.getItemBySlot(EquipmentSlot.LEGS).getItem()){
            int healthAmp=robeTier==1?1:robeTier==2?3:7;player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST,30,healthAmp,true,false));
            if(robeTier>=2)player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,30,robeTier>=3?1:0,true,false));}
    }

    public static GearStats stats(Player player){Piece h=piece(player.getItemBySlot(EquipmentSlot.HEAD).getItem());Item chest=player.getItemBySlot(EquipmentSlot.CHEST).getItem();Item legs=player.getItemBySlot(EquipmentSlot.LEGS).getItem();Piece r=hemFor(chest)!=null&&hemFor(chest)==legs?piece(chest):Piece.NONE;Piece b=piece(player.getItemBySlot(EquipmentSlot.FEET).getItem());return new GearStats(h.tier,r.tier,b.tier,h.mana+r.mana+b.mana,h.regen*r.regen*b.regen,h.cost*r.cost*b.cost,h.power*r.power*b.power,h.range*r.range*b.range,h.cooldown*r.cooldown*b.cooldown);}
    public static String hatName(Player p){return name(p.getItemBySlot(EquipmentSlot.HEAD).getItem(),"모자 없음");}
    public static String robeName(Player p){Item c=p.getItemBySlot(EquipmentSlot.CHEST).getItem();if(hemFor(c)==null)return"로브 없음";return name(c,"로브 없음")+(hemFor(c)==p.getItemBySlot(EquipmentSlot.LEGS).getItem()?"":" · 바지 슬롯 필요");}
    public static String bootsName(Player p){return name(p.getItemBySlot(EquipmentSlot.FEET).getItem(),"마도화 없음");}

    private static Piece piece(Item i){
        if(i==ModItems.MAGE_HAT.get())return new Piece(1,90,1.20,.92,1.03,1.01,.97);if(i==ModItems.SAGE_HAT.get())return new Piece(2,300,1.42,.82,1.09,1.08,.86);if(i==ModItems.ARCHMAGE_CROWN.get())return new Piece(3,850,1.85,.65,1.20,1.17,.68);
        if(i==ModItems.MAGE_ROBE.get())return new Piece(1,45,1.08,.97,1.08,1.03,.95);if(i==ModItems.SAGE_ROBE.get())return new Piece(2,210,1.20,.91,1.20,1.11,.84);if(i==ModItems.ARCHMAGE_ROBE.get())return new Piece(3,650,1.48,.80,1.42,1.25,.67);
        if(i==ModItems.MAGE_BOOTS.get())return new Piece(1,10,1.03,.99,1.02,1.07,.94);if(i==ModItems.SKYWALKER_BOOTS.get())return new Piece(2,75,1.08,.96,1.07,1.20,.80);if(i==ModItems.FROSTSTEP_BOOTS.get())return new Piece(3,220,1.18,.91,1.13,1.42,.58);
        if(i==ModItems.CINDER_HOOD.get())return new Piece(2,170,1.18,.90,1.17,1.04,.90);if(i==ModItems.CINDER_ROBE.get())return new Piece(2,160,1.15,.92,1.27,1.05,.88);if(i==ModItems.CINDER_BOOTS.get())return new Piece(2,55,1.06,.97,1.12,1.16,.83);
        if(i==ModItems.GLACIER_CIRCLET.get())return new Piece(2,220,1.30,.86,1.12,1.16,.88);if(i==ModItems.GLACIER_ROBE.get())return new Piece(2,250,1.22,.90,1.18,1.22,.86);if(i==ModItems.GLACIER_BOOTS.get())return new Piece(2,80,1.10,.96,1.08,1.30,.78);
        if(i==ModItems.TEMPEST_HOOD.get())return new Piece(2,190,1.24,.89,1.11,1.24,.76);if(i==ModItems.TEMPEST_ROBE.get())return new Piece(2,180,1.18,.92,1.16,1.30,.75);if(i==ModItems.TEMPEST_BOOTS.get())return new Piece(3,150,1.16,.94,1.10,1.55,.55);
        if(i==ModItems.RIFT_CROWN.get())return new Piece(3,700,1.70,.70,1.26,1.45,.66);if(i==ModItems.RIFT_ROBE.get())return new Piece(3,760,1.52,.78,1.48,1.52,.64);if(i==ModItems.RIFT_BOOTS.get())return new Piece(3,380,1.32,.86,1.20,1.80,.46);
        return Piece.NONE;
    }
    private static int robeTier(Item i){return piece(i).tier;}
    private static int bootsTier(Item i){return piece(i).tier;}
    private static Item hemFor(Item i){if(i==ModItems.MAGE_ROBE.get())return ModItems.MAGE_ROBE_HEM.get();if(i==ModItems.SAGE_ROBE.get())return ModItems.SAGE_ROBE_HEM.get();if(i==ModItems.ARCHMAGE_ROBE.get())return ModItems.ARCHMAGE_ROBE_HEM.get();if(i==ModItems.CINDER_ROBE.get())return ModItems.CINDER_ROBE_HEM.get();if(i==ModItems.GLACIER_ROBE.get())return ModItems.GLACIER_ROBE_HEM.get();if(i==ModItems.TEMPEST_ROBE.get())return ModItems.TEMPEST_ROBE_HEM.get();if(i==ModItems.RIFT_ROBE.get())return ModItems.RIFT_ROBE_HEM.get();return null;}
    private static boolean isHem(Item i){return i==ModItems.MAGE_ROBE_HEM.get()||i==ModItems.SAGE_ROBE_HEM.get()||i==ModItems.ARCHMAGE_ROBE_HEM.get()||i==ModItems.CINDER_ROBE_HEM.get()||i==ModItems.GLACIER_ROBE_HEM.get()||i==ModItems.TEMPEST_ROBE_HEM.get()||i==ModItems.RIFT_ROBE_HEM.get();}
    private static boolean isSlowFallBoots(Item i){return i==ModItems.SKYWALKER_BOOTS.get()||i==ModItems.FROSTSTEP_BOOTS.get()||i==ModItems.TEMPEST_BOOTS.get()||i==ModItems.RIFT_BOOTS.get();}
    private static boolean isFrostBoots(Item i){return i==ModItems.FROSTSTEP_BOOTS.get()||i==ModItems.GLACIER_BOOTS.get();}
    private static boolean isFlightBoots(Item i){return i==ModItems.RIFT_BOOTS.get();}
    private static void setFlight(ServerPlayer p,boolean enabled){if(enabled){if(!p.getAbilities().mayfly){p.getAbilities().mayfly=true;p.onUpdateAbilities();}FLIGHT_GRANTED.add(p.getUUID());}else if(FLIGHT_GRANTED.remove(p.getUUID())&&!p.isCreative()&&!p.isSpectator()){p.getAbilities().flying=false;p.getAbilities().mayfly=false;p.onUpdateAbilities();}}
    private static void freezeWater(ServerPlayer p){if(!(p.level() instanceof ServerLevel l))return;BlockPos c=p.blockPosition().below();for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){if(x*x+z*z>6)continue;BlockPos pos=c.offset(x,0,z);if(l.getBlockState(pos).is(Blocks.WATER)&&l.getBlockState(pos.above()).isAir())l.setBlockAndUpdate(pos,Blocks.FROSTED_ICE.defaultBlockState());}}
    private static String name(Item i,String fallback){if(i==ModItems.MAGE_HAT.get())return"비전 모자";if(i==ModItems.SAGE_HAT.get())return"현자의 모자";if(i==ModItems.ARCHMAGE_CROWN.get())return"대마도사 관";if(i==ModItems.MAGE_ROBE.get())return"중층 마도 로브";if(i==ModItems.SAGE_ROBE.get())return"현자의 로브";if(i==ModItems.ARCHMAGE_ROBE.get())return"대마도사 예복";if(i==ModItems.MAGE_BOOTS.get())return"유랑 마도화";if(i==ModItems.SKYWALKER_BOOTS.get())return"천공 마도화";if(i==ModItems.FROSTSTEP_BOOTS.get())return"빙결 보행화";if(i==ModItems.CINDER_HOOD.get())return"잿불 전투모";if(i==ModItems.CINDER_ROBE.get())return"잿불 전투로브";if(i==ModItems.CINDER_BOOTS.get())return"화염답화";if(i==ModItems.GLACIER_CIRCLET.get())return"빙정 관모";if(i==ModItems.GLACIER_ROBE.get())return"빙정 의복";if(i==ModItems.GLACIER_BOOTS.get())return"설원답화";if(i==ModItems.TEMPEST_HOOD.get())return"폭풍 후드";if(i==ModItems.TEMPEST_ROBE.get())return"폭풍비단 로브";if(i==ModItems.TEMPEST_BOOTS.get())return"천뢰 장화";if(i==ModItems.RIFT_CROWN.get())return"균열 관";if(i==ModItems.RIFT_ROBE.get())return"균열 예복";if(i==ModItems.RIFT_BOOTS.get())return"성간 보행화";return fallback;}
    public static void clear(UUID id){ROBE_SLOT_WARNED.remove(id);FLIGHT_GRANTED.remove(id);}
    private record Piece(int tier,int mana,double regen,double cost,double power,double range,double cooldown){private static final Piece NONE=new Piece(0,0,1,1,1,1,1);}
    public record GearStats(int hatTier,int robeTier,int bootsTier,int maxManaBonus,double regenMultiplier,double manaCostMultiplier,double powerMultiplier,double rangeMultiplier,double cooldownMultiplier){public boolean hat(){return hatTier>0;}public boolean robe(){return robeTier>0;}public boolean boots(){return bootsTier>0;}}
}

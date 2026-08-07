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
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Multiplicative gear stacking with explicit flat and relative vitality/mana bonuses. */
public final class MageGearService {
    private static final Set<UUID> ROBE_SLOT_WARNED=new HashSet<>();
    private static final Set<UUID> FLIGHT_GRANTED=new HashSet<>();
    private static final Map<UUID,Long> STABLE_DESCENT_UNTIL=new HashMap<>();
    private MageGearService(){}

    public static void tick(ServerPlayer player){
        ItemStack chest=player.getItemBySlot(EquipmentSlot.CHEST),legs=player.getItemBySlot(EquipmentSlot.LEGS);Item expected=hemFor(chest.getItem());
        if(expected!=null&&legs.isEmpty()){player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(expected));ROBE_SLOT_WARNED.remove(player.getUUID());}
        else if(expected==null&&isHem(legs.getItem())){player.setItemSlot(EquipmentSlot.LEGS,ItemStack.EMPTY);ROBE_SLOT_WARNED.remove(player.getUUID());}
        else if(expected!=null&&isHem(legs.getItem())&&legs.getItem()!=expected){player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(expected));ROBE_SLOT_WARNED.remove(player.getUUID());}
        else if(expected!=null&&!legs.isEmpty()&&!isHem(legs.getItem())&&ROBE_SLOT_WARNED.add(player.getUUID()))ArcaneNoticeService.push(player,Component.literal("§c[로브 비활성] §f전투 로브는 몸·바지 두 슬롯을 사용합니다."),100);

        Item boots=player.getItemBySlot(EquipmentSlot.FEET).getItem();int tier=bootsTier(boots);
        if(tier>0){player.addEffect(new MobEffectInstance(MobEffects.SPEED,30,Math.max(0,tier-1),true,false));player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,30,Math.max(0,tier-1),true,false));}
        if(isFrostBoots(boots))freezeWater(player);
        setFlight(player,isFlightBoots(boots));

        Item hat=player.getItemBySlot(EquipmentSlot.HEAD).getItem();
        if(hat==ModItems.RIFT_CROWN.get())player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,240,0,true,false));
        if(hat==ModItems.CINDER_HOOD.get()||chest.getItem()==ModItems.CINDER_ROBE.get())player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,40,0,true,false));
        if(chest.getItem()==ModItems.GLACIER_ROBE.get())player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,40,1,true,false));
        if(chest.getItem()==ModItems.TEMPEST_ROBE.get())player.addEffect(new MobEffectInstance(MobEffects.SPEED,40,1,true,false));
        int robeTier=robeTier(chest.getItem());if(robeTier>=2&&expected==player.getItemBySlot(EquipmentSlot.LEGS).getItem())player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,30,robeTier>=3?1:0,true,false));
    }

    /** Spell-driven feather fall remains explicit; armor no longer edits airborne velocity. */
    public static void tickMovement(ServerPlayer player){
        long now=((ServerLevel)player.level()).getServer().overworld().getGameTime();
        long until=STABLE_DESCENT_UNTIL.getOrDefault(player.getUUID(),0L);
        boolean spellDescent=until>now;
        if(!spellDescent&&until>0L)STABLE_DESCENT_UNTIL.remove(player.getUUID());
        if(!spellDescent||player.onGround()||player.getAbilities().flying||player.isShiftKeyDown())return;
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,12,0,true,false));
        player.fallDistance=0.0F;
    }

    public static void grantStableDescent(ServerPlayer player,int ticks){long now=((ServerLevel)player.level()).getServer().overworld().getGameTime();STABLE_DESCENT_UNTIL.merge(player.getUUID(),now+Math.max(1,ticks),Math::max);}

    /**
     * Armor landing protection changes only incoming FALL damage. A fully protected soft landing
     * is cancelled before LivingEntity damage processing, preventing hurt flash/sound as well.
     */
    public static void onIncomingDamage(LivingIncomingDamageEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player)||!event.getSource().is(DamageTypes.FALL))return;
        Item boots=player.getItemBySlot(EquipmentSlot.FEET).getItem();
        Item chest=player.getItemBySlot(EquipmentSlot.CHEST).getItem();
        int bootTier=bootsTier(boots),robe=robeTier(chest);
        int hat=piece(player.getItemBySlot(EquipmentSlot.HEAD).getItem()).tier;
        if(bootTier<=0&&robe<=0&&hat<=0)return;
        double bootReduction=switch(bootTier){case 1->0.26;case 2->0.46;case 3->0.66;default->0.0;};
        double supportReduction=Math.min(0.18,robe*0.045+hat*0.015);
        double reduction=Math.min(0.82,bootReduction+supportReduction);
        float reduced=(float)Math.max(0.0,event.getAmount()*(1.0-reduction));
        double ignoreThreshold=0.45+bootTier*1.35+robe*0.35;
        if(reduced<=ignoreThreshold){event.setCanceled(true);return;}
        event.setAmount(reduced);
    }

    public static GearStats stats(Player player){
        Piece h=piece(player.getItemBySlot(EquipmentSlot.HEAD).getItem());Item chest=player.getItemBySlot(EquipmentSlot.CHEST).getItem();Item legs=player.getItemBySlot(EquipmentSlot.LEGS).getItem();Piece r=hemFor(chest)!=null&&hemFor(chest)==legs?piece(chest):Piece.NONE;Piece b=piece(player.getItemBySlot(EquipmentSlot.FEET).getItem());
        return new GearStats(h.tier,r.tier,b.tier,h.manaFlat+r.manaFlat+b.manaFlat,h.manaPercent*r.manaPercent*b.manaPercent,h.healthFlat+r.healthFlat+b.healthFlat,h.healthPercent*r.healthPercent*b.healthPercent,h.regen*r.regen*b.regen,h.cost*r.cost*b.cost,h.power*r.power*b.power,h.range*r.range*b.range,h.cooldown*r.cooldown*b.cooldown);
    }
    public static String hatName(Player p){return name(p.getItemBySlot(EquipmentSlot.HEAD).getItem(),"모자 없음");}
    public static String robeName(Player p){Item c=p.getItemBySlot(EquipmentSlot.CHEST).getItem();if(hemFor(c)==null)return"로브 없음";return name(c,"로브 없음")+(hemFor(c)==p.getItemBySlot(EquipmentSlot.LEGS).getItem()?"":" · 바지 슬롯 필요");}
    public static String bootsName(Player p){return name(p.getItemBySlot(EquipmentSlot.FEET).getItem(),"마도화 없음");}

    private static Piece piece(Item i){
        if(i==ModItems.MAGE_HAT.get())return p(1,60,1.04,0,1.00,1.12,.94,1.03,1.02,.96);if(i==ModItems.SAGE_HAT.get())return p(2,180,1.08,20,1.03,1.25,.88,1.08,1.08,.88);if(i==ModItems.ARCHMAGE_CROWN.get())return p(3,420,1.14,60,1.06,1.45,.78,1.16,1.15,.76);
        if(i==ModItems.MAGE_ROBE.get())return p(1,40,1.03,30,1.10,1.07,.97,1.08,1.03,.95);if(i==ModItems.SAGE_ROBE.get())return p(2,140,1.07,90,1.20,1.16,.92,1.18,1.10,.86);if(i==ModItems.ARCHMAGE_ROBE.get())return p(3,360,1.13,250,1.36,1.32,.84,1.34,1.22,.74);
        if(i==ModItems.MAGE_BOOTS.get())return p(1,20,1.02,10,1.03,1.03,.99,1.02,1.06,.95);if(i==ModItems.SKYWALKER_BOOTS.get())return p(2,70,1.04,30,1.07,1.07,.97,1.06,1.16,.86);if(i==ModItems.FROSTSTEP_BOOTS.get())return p(3,150,1.06,60,1.10,1.12,.94,1.10,1.28,.72);
        if(i==ModItems.CINDER_HOOD.get())return p(2,140,1.07,20,1.04,1.16,.90,1.15,1.04,.90);if(i==ModItems.CINDER_ROBE.get())return p(2,160,1.08,80,1.18,1.14,.91,1.25,1.06,.88);if(i==ModItems.CINDER_BOOTS.get())return p(2,70,1.03,25,1.05,1.06,.97,1.10,1.14,.84);
        if(i==ModItems.GLACIER_CIRCLET.get())return p(2,170,1.08,30,1.05,1.22,.88,1.10,1.14,.90);if(i==ModItems.GLACIER_ROBE.get())return p(2,190,1.09,110,1.24,1.18,.90,1.16,1.18,.87);if(i==ModItems.GLACIER_BOOTS.get())return p(2,80,1.04,40,1.08,1.08,.96,1.07,1.22,.82);
        if(i==ModItems.TEMPEST_HOOD.get())return p(2,160,1.07,20,1.04,1.18,.90,1.09,1.20,.82);if(i==ModItems.TEMPEST_ROBE.get())return p(2,170,1.08,70,1.16,1.16,.91,1.14,1.24,.80);if(i==ModItems.TEMPEST_BOOTS.get())return p(3,130,1.06,45,1.09,1.12,.94,1.09,1.38,.68);
        if(i==ModItems.RIFT_CROWN.get())return p(3,340,1.13,60,1.08,1.38,.80,1.20,1.32,.76);if(i==ModItems.RIFT_ROBE.get())return p(3,380,1.14,220,1.34,1.30,.83,1.38,1.38,.72);if(i==ModItems.RIFT_BOOTS.get())return p(3,220,1.09,80,1.12,1.22,.88,1.18,1.52,.62);
        return Piece.NONE;
    }
    private static Piece p(int tier,int manaFlat,double manaPercent,int healthFlat,double healthPercent,double regen,double cost,double power,double range,double cooldown){return new Piece(tier,manaFlat,manaPercent,healthFlat,healthPercent,regen,cost,power,range,cooldown);}
    private static int robeTier(Item i){return piece(i).tier;}
    private static int bootsTier(Item i){return piece(i).tier;}
    private static Item hemFor(Item i){if(i==ModItems.MAGE_ROBE.get())return ModItems.MAGE_ROBE_HEM.get();if(i==ModItems.SAGE_ROBE.get())return ModItems.SAGE_ROBE_HEM.get();if(i==ModItems.ARCHMAGE_ROBE.get())return ModItems.ARCHMAGE_ROBE_HEM.get();if(i==ModItems.CINDER_ROBE.get())return ModItems.CINDER_ROBE_HEM.get();if(i==ModItems.GLACIER_ROBE.get())return ModItems.GLACIER_ROBE_HEM.get();if(i==ModItems.TEMPEST_ROBE.get())return ModItems.TEMPEST_ROBE_HEM.get();if(i==ModItems.RIFT_ROBE.get())return ModItems.RIFT_ROBE_HEM.get();return null;}
    private static boolean isHem(Item i){return i==ModItems.MAGE_ROBE_HEM.get()||i==ModItems.SAGE_ROBE_HEM.get()||i==ModItems.ARCHMAGE_ROBE_HEM.get()||i==ModItems.CINDER_ROBE_HEM.get()||i==ModItems.GLACIER_ROBE_HEM.get()||i==ModItems.TEMPEST_ROBE_HEM.get()||i==ModItems.RIFT_ROBE_HEM.get();}
    private static boolean isFrostBoots(Item i){return i==ModItems.FROSTSTEP_BOOTS.get()||i==ModItems.GLACIER_BOOTS.get();}
    private static boolean isFlightBoots(Item i){return i==ModItems.RIFT_BOOTS.get();}
    private static void setFlight(ServerPlayer p,boolean enabled){if(enabled){if(!p.getAbilities().mayfly){p.getAbilities().mayfly=true;p.onUpdateAbilities();}FLIGHT_GRANTED.add(p.getUUID());}else if(FLIGHT_GRANTED.remove(p.getUUID())&&!p.isCreative()&&!p.isSpectator()){p.getAbilities().flying=false;p.getAbilities().mayfly=false;p.onUpdateAbilities();}}
    private static void freezeWater(ServerPlayer p){if(!(p.level() instanceof ServerLevel l))return;BlockPos c=p.blockPosition().below();for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){if(x*x+z*z>6)continue;BlockPos pos=c.offset(x,0,z);if(l.getBlockState(pos).is(Blocks.WATER)&&l.getBlockState(pos.above()).isAir())l.setBlockAndUpdate(pos,Blocks.FROSTED_ICE.defaultBlockState());}}
    private static String name(Item i,String fallback){if(i==ModItems.MAGE_HAT.get())return"비전 모자";if(i==ModItems.SAGE_HAT.get())return"현자의 모자";if(i==ModItems.ARCHMAGE_CROWN.get())return"대마도사 관";if(i==ModItems.MAGE_ROBE.get())return"중층 마도 로브";if(i==ModItems.SAGE_ROBE.get())return"현자의 로브";if(i==ModItems.ARCHMAGE_ROBE.get())return"대마도사 예복";if(i==ModItems.MAGE_BOOTS.get())return"유랑 마도화";if(i==ModItems.SKYWALKER_BOOTS.get())return"천공 마도화";if(i==ModItems.FROSTSTEP_BOOTS.get())return"빙결 보행화";if(i==ModItems.CINDER_HOOD.get())return"잿불 전투모";if(i==ModItems.CINDER_ROBE.get())return"잿불 전투로브";if(i==ModItems.CINDER_BOOTS.get())return"화염답화";if(i==ModItems.GLACIER_CIRCLET.get())return"빙정 관모";if(i==ModItems.GLACIER_ROBE.get())return"빙정 의복";if(i==ModItems.GLACIER_BOOTS.get())return"설원답화";if(i==ModItems.TEMPEST_HOOD.get())return"폭풍 후드";if(i==ModItems.TEMPEST_ROBE.get())return"폭풍비단 로브";if(i==ModItems.TEMPEST_BOOTS.get())return"천뢰 장화";if(i==ModItems.RIFT_CROWN.get())return"균열 관";if(i==ModItems.RIFT_ROBE.get())return"균열 예복";if(i==ModItems.RIFT_BOOTS.get())return"성간 보행화";return fallback;}
    public static void clear(UUID id){ROBE_SLOT_WARNED.remove(id);FLIGHT_GRANTED.remove(id);STABLE_DESCENT_UNTIL.remove(id);}
    private record Piece(int tier,int manaFlat,double manaPercent,int healthFlat,double healthPercent,double regen,double cost,double power,double range,double cooldown){private static final Piece NONE=new Piece(0,0,1,0,1,1,1,1,1,1);}
    public record GearStats(int hatTier,int robeTier,int bootsTier,int maxManaBonus,double maxManaMultiplier,int healthBonus,double healthMultiplier,double regenMultiplier,double manaCostMultiplier,double powerMultiplier,double rangeMultiplier,double cooldownMultiplier){public boolean hat(){return hatTier>0;}public boolean robe(){return robeTier>0;}public boolean boots(){return bootsTier>0;}}
}

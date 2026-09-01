package kr.moonseungjun.titanbreak.entity;

import kr.moonseungjun.titanbreak.combat.BreachService;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.combat.TemporalRated;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

public final class GravemarchColossusEntity extends Giant implements TemporalRated, TitanGeoEntity {
    public static final int PART_LEFT_ANKLE = 1 << 0, PART_RIGHT_ANKLE = 1 << 1;
    public static final int PART_LEFT_KNEE = 1 << 2, PART_RIGHT_KNEE = 1 << 3;
    public static final int PART_LEFT_ELBOW = 1 << 4, PART_RIGHT_ELBOW = 1 << 5;
    public static final int PART_CHEST_HEART = 1 << 6, PART_SKULL_ARMOR = 1 << 7;
    public static final int ALL_PARTS_MASK = 0xFF;
    public static final double CANONICAL_VISIBLE_MAX_HEALTH = 18_000.0D;

    private static final EntityDataAccessor<Integer> BROKEN_PARTS =
            SynchedEntityData.defineId(GravemarchColossusEntity.class, EntityDataSerializers.INT);
    private static final PartSpec[] SPECS = {
            new PartSpec(PartSlot.LEFT_ANKLE,-10,9,0,10,13,180), new PartSpec(PartSlot.RIGHT_ANKLE,10,9,0,10,13,180),
            new PartSpec(PartSlot.LEFT_KNEE,-10,30,0,12,15,240), new PartSpec(PartSlot.RIGHT_KNEE,10,30,0,12,15,240),
            new PartSpec(PartSlot.LEFT_ELBOW,-24,58,-1,11,14,220), new PartSpec(PartSlot.RIGHT_ELBOW,24,58,-1,11,14,220),
            new PartSpec(PartSlot.CHEST_HEART,0,69,-7,17,18,420), new PartSpec(PartSlot.SKULL_ARMOR,0,91,-2,18,16,500)
    };

    private final GravemarchPart[] parts = new GravemarchPart[SPECS.length];
    private final ServerBossEvent bossBar;
    private boolean partsInitialized;
    private int actionCooldown = 50, shockwaveBursts, shockwaveDelay, debrisImpactDelay;
    private Vec3 debrisImpact;

    public GravemarchColossusEntity(EntityType<? extends Giant> type, Level level) {
        super(type, level);
        bossBar = new ServerBossEvent(getUUID(), Component.translatable("entity.titanbreak.gravemarch_colossus"),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        for (int i=0;i<SPECS.length;i++) {
            PartSpec s=SPECS[i];
            parts[i]=new GravemarchPart(this,s.slot(),s.width(),s.height(),s.health());
        }
        xpReward=120;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(BROKEN_PARTS,0);}
    @Override protected void registerGoals(){goalSelector.addGoal(1,new CombatGoal());targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,true));}
    @Override public int temporalRating(){return 10;}
    public double canonicalVisibleHealth(){return CANONICAL_VISIBLE_MAX_HEALTH*Math.max(0,getHealth())/Math.max(1,getMaxHealth());}
    public int phase(){if(isBroken(PartSlot.CHEST_HEART)||getHealth()<=getMaxHealth()*.30F)return 3;return brokenLegPartCount()>0?2:1;}
    public int brokenPartsMask(){return getEntityData().get(BROKEN_PARTS)&ALL_PARTS_MASK;}
    public boolean isPartBroken(int mask){return (brokenPartsMask()&mask)!=0;}
    public boolean chestExposed(){return brokenLegPartCount()>=2;}
    private boolean isBroken(PartSlot s){return isPartBroken(s.mask());}
    private int brokenPartCount(){return Integer.bitCount(brokenPartsMask());}
    private int brokenLegPartCount(){int n=0;for(PartSlot s:new PartSlot[]{PartSlot.LEFT_ANKLE,PartSlot.RIGHT_ANKLE,PartSlot.LEFT_KNEE,PartSlot.RIGHT_KNEE})if(isBroken(s))n++;return n;}
    private void markBroken(PartSlot s){int old=brokenPartsMask(),next=old|s.mask();if(next!=old)getEntityData().set(BROKEN_PARTS,next);}

    @Override public void aiStep(){
        super.aiStep(); updatePartPositions();
        if(!level().isClientSide()) bossBar.setProgress(Math.max(0,Math.min(1,getHealth()/Math.max(1,getMaxHealth()))));
        int legs=brokenLegPartCount();
        if(legs>0){Vec3 m=getDeltaMovement();double f=legs>=3?.12:legs==2?.22:.38;setDeltaMovement(m.x*f,m.y,m.z*f);}
        if(!(level() instanceof ServerLevel sl))return;
        if(debrisImpactDelay>0&&--debrisImpactDelay==0&&debrisImpact!=null){impactArea(sl,debrisImpact,7+rageRadiusBonus(),58*rageDamageMultiplier(),1.05);fractureTerrain(sl,BlockPos.containing(debrisImpact),3,18);debrisImpact=null;}
        if(shockwaveDelay>0&&--shockwaveDelay==0&&shockwaveBursts>0){int seq=4-shockwaveBursts;impactArea(sl,position(),14+seq*5+rageRadiusBonus(),(46+seq*10)*rageDamageMultiplier(),1.30);fractureTerrain(sl,blockPosition(),3,16);shockwaveBursts--;if(shockwaveBursts>0)shockwaveDelay=10;}
    }

    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);bossBar.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bossBar.removePlayer(p);}
    private double rageDamageMultiplier(){return 1+brokenPartCount()*.08;}
    private double rageRadiusBonus(){return brokenPartCount()*.65;}

    private void updatePartPositions(){
        Vec3[] old=new Vec3[parts.length];for(int i=0;i<parts.length;i++)old[i]=parts[i].position();
        double yaw=Math.toRadians(-getYRot()),c=Math.cos(yaw),s=Math.sin(yaw);
        for(int i=0;i<parts.length;i++){PartSpec p=SPECS[i];double x=p.x()*c-p.z()*s,z=p.x()*s+p.z()*c;parts[i].setPos(getX()+x,getY()+p.y(),getZ()+z);}
        for(int i=0;i<parts.length;i++){GravemarchPart p=parts[i];Vec3 o=partsInitialized?old[i]:p.position();p.xo=o.x;p.yo=o.y;p.zo=o.z;p.xOld=o.x;p.yOld=o.y;p.zOld=o.z;}
        partsInitialized=true;
    }

    @Override public AABB getBoundingBoxForCulling(){if(!partsInitialized)return getBoundingBox().inflate(30,105,30);AABB b=getBoundingBox();for(GravemarchPart p:parts)b=b.minmax(p.getBoundingBox());return b;}

    private boolean hurtPart(GravemarchPart p,ServerLevel level,DamageSource source,float amount){
        if(p.broken()&&p.slot!=PartSlot.CHEST_HEART)return false;
        if(p.slot==PartSlot.CHEST_HEART&&p.broken())return super.hurtServer(level,source,amount);
        float partDamage=amount;if(p.slot==PartSlot.CHEST_HEART&&!chestExposed())partDamage*=.12F;p.applyPartDamage(partDamage);if(p.broken())markBroken(p.slot);
        float transferred=switch(p.slot){case LEFT_ANKLE,RIGHT_ANKLE,LEFT_KNEE,RIGHT_KNEE->amount*.20F;case LEFT_ELBOW,RIGHT_ELBOW->amount*.16F;case SKULL_ARMOR->amount*.08F;case CHEST_HEART->amount*(chestExposed()?1.10F:.04F);};
        return super.hurtServer(level,source,transferred);
    }

    private void groundSlam(ServerLevel l){swing(InteractionHand.MAIN_HAND);impactArea(l,position(),14+rageRadiusBonus(),68*rageDamageMultiplier(),1.20);fractureTerrain(l,blockPosition(),3,20);}
    private void mountainPush(ServerLevel l,LivingEntity target){
        swing(InteractionHand.MAIN_HAND);Vec3 facing=target.position().subtract(position());if(facing.horizontalDistanceSqr()<1E-6)return;facing=new Vec3(facing.x,0,facing.z).normalize();double range=27+rageRadiusBonus();
        for(Player p:l.getEntitiesOfClass(Player.class,getBoundingBox().inflate(range,12,range),Player::isAlive)){Vec3 off=p.position().subtract(position());if(off.horizontalDistance()>range||off.horizontalDistance()<1E-5)continue;Vec3 dir=new Vec3(off.x,0,off.z).normalize();if(dir.dot(facing)<.25)continue;p.hurtServer(l,damageSources().mobAttack(this),(float)CombatScale.toInternal(54*rageDamageMultiplier()));p.push(dir.x*2.2,.45,dir.z*2.2);}
        fractureTerrain(l,blockPosition().offset((int)Math.round(facing.x*5),0,(int)Math.round(facing.z*5)),3,18);
    }
    private void grabThrow(ServerLevel l,LivingEntity target){if(isBroken(PartSlot.LEFT_ELBOW)&&isBroken(PartSlot.RIGHT_ELBOW)){groundSlam(l);return;}swing(InteractionHand.MAIN_HAND);target.hurtServer(l,damageSources().mobAttack(this),(float)CombatScale.toInternal(82*rageDamageMultiplier()));Vec3 a=target.position().subtract(position());if(a.horizontalDistanceSqr()<1E-6)a=getLookAngle();a=new Vec3(a.x,0,a.z).normalize();target.setDeltaMovement(a.x*2.4,1.35,a.z*2.4);}
    private void scheduleDebrisThrow(LivingEntity target){swing(InteractionHand.MAIN_HAND);debrisImpact=target.position().add(target.getDeltaMovement().scale(18));debrisImpactDelay=26;}
    private void startOverloadShockwaves(){shockwaveBursts=3;shockwaveDelay=1;}

    private void impactArea(ServerLevel l,Vec3 center,double radius,double visible,double knockback){
        AABB area=new AABB(center,center).inflate(radius,Math.max(8,radius*.55),radius);
        for(Player p:l.getEntitiesOfClass(Player.class,area,Player::isAlive)){double d=p.position().distanceTo(center);if(d>radius)continue;double scale=Math.max(.20,1-d/Math.max(1,radius));p.hurtServer(l,damageSources().mobAttack(this),(float)CombatScale.toInternal(visible*(.45+scale*.55)));Vec3 push=p.position().subtract(center);if(push.horizontalDistanceSqr()>1E-6){push=new Vec3(push.x,0,push.z).normalize();p.push(push.x*knockback*(.6+scale),.28+scale*.35,push.z*knockback*(.6+scale));}}
    }
    private void fractureTerrain(ServerLevel l,BlockPos center,int power,int max){int broken=0,r=4;for(BlockPos c:BlockPos.betweenClosed(center.offset(-r,-2,-r),center.offset(r,1,r))){if(broken>=Math.min(24,max))break;BlockPos p=c.immutable();if(p.distSqr(center)>r*r+2)continue;var state=l.getBlockState(p);if(BreachService.requiredPower(l,p,state)>power)continue;if(l.destroyBlock(p,false,this))broken++;}}

    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);int saved=in.getIntOr("TitanbreakGravemarchBrokenParts",0)&ALL_PARTS_MASK,rebuilt=0;for(int i=0;i<parts.length;i++){GravemarchPart p=parts[i];float hp=in.getFloatOr("TitanbreakGravemarchPartHealth"+i,SPECS[i].health());if((saved&p.slot.mask())!=0)hp=0;p.setPartHealth(hp);if(p.broken())rebuilt|=p.slot.mask();}getEntityData().set(BROKEN_PARTS,rebuilt);partsInitialized=false;}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("TitanbreakGravemarchBrokenParts",brokenPartsMask());for(int i=0;i<parts.length;i++)out.putFloat("TitanbreakGravemarchPartHealth"+i,parts[i].partHealth);}
    @Override public boolean isMultipartEntity(){return true;}
    @Override public PartEntity<?>[] getParts(){return parts;}
    @Override public boolean isPickable(){return false;}
    @Override public void recreateFromPacket(ClientboundAddEntityPacket packet){super.recreateFromPacket(packet);for(int i=0;i<parts.length;i++)parts[i].setId(packet.getId()+i+1);partsInitialized=false;updatePartPositions();}
    @Override public void setId(int id){super.setId(id);for(int i=0;i<parts.length;i++)parts[i].setId(id+i+1);}

    private final class CombatGoal extends Goal {
        private int clock;
        @Override public boolean canUse(){LivingEntity t=getTarget();return t!=null&&t.isAlive();}
        @Override public boolean canContinueToUse(){return canUse();}
        @Override public void tick(){LivingEntity t=getTarget();if(t==null||!(level() instanceof ServerLevel sl))return;int ph=phase();if(actionCooldown>0)actionCooldown--;clock++;double speed=ph==1?.72:ph==2?.40:.34;if(brokenLegPartCount()>=2)speed*=.55;getNavigation().moveTo(t,speed);getLookControl().setLookAt(t,38,28);if(ph==3&&shockwaveBursts==0&&shockwaveDelay==0&&clock%74==0){startOverloadShockwaves();actionCooldown=Math.max(actionCooldown,30);return;}if(actionCooldown>0||debrisImpactDelay>0)return;double d=distanceTo(t);int choice=getRandom().nextInt(ph==1?4:5);if(d<=10&&choice==0){grabThrow(sl,t);actionCooldown=52;}else if(d<=18&&choice<=2){groundSlam(sl);actionCooldown=ph==1?62:48;}else if(d<=30&&choice==3){mountainPush(sl,t);actionCooldown=58;}else if(d<=52){scheduleDebrisThrow(t);actionCooldown=64;}}
    }

    private enum PartSlot {LEFT_ANKLE(PART_LEFT_ANKLE),RIGHT_ANKLE(PART_RIGHT_ANKLE),LEFT_KNEE(PART_LEFT_KNEE),RIGHT_KNEE(PART_RIGHT_KNEE),LEFT_ELBOW(PART_LEFT_ELBOW),RIGHT_ELBOW(PART_RIGHT_ELBOW),CHEST_HEART(PART_CHEST_HEART),SKULL_ARMOR(PART_SKULL_ARMOR);private final int mask;PartSlot(int m){mask=m;}int mask(){return mask;}}
    private record PartSpec(PartSlot slot,double x,double y,double z,float width,float height,float health){}

    private static final class GravemarchPart extends PartEntity<GravemarchColossusEntity> {
        private final PartSlot slot;private final EntityDimensions dimensions;private float partHealth;
        private GravemarchPart(GravemarchColossusEntity parent,PartSlot slot,float w,float h,float hp){super(parent);this.slot=slot;dimensions=EntityDimensions.scalable(w,h);partHealth=hp;refreshDimensions();}
        private boolean broken(){return partHealth<=0;}private void setPartHealth(float hp){partHealth=Math.max(0,hp);}private void applyPartDamage(float a){setPartHealth(partHealth-Math.max(0,a));}
        @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
        @Override protected void readAdditionalSaveData(ValueInput i){}
        @Override protected void addAdditionalSaveData(ValueOutput o){}
        @Override public boolean isPickable(){return !broken()||slot==PartSlot.CHEST_HEART;}
        @Override public boolean hurtServer(ServerLevel l,DamageSource s,float a){return !isInvulnerableToBase(s)&&getParent().hurtPart(this,l,s,a);}
        @Override public boolean is(Entity e){return this==e||getParent()==e;}
        @Override public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity e){throw new UnsupportedOperationException();}
        @Override public EntityDimensions getDimensions(Pose p){return dimensions;}
        @Override public boolean shouldBeSaved(){return false;}
    }
}

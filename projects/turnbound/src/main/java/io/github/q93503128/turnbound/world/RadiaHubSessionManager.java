package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Radia hub runtime and v0.4 Prologue quest flow. */
public final class RadiaHubSessionManager {
    private static final List<String> TUTORIALS=List.of("TUTORIAL_1","TUTORIAL_2","TUTORIAL_3");
    private static final Map<UUID,Session> SESSIONS=new LinkedHashMap<>();
    private RadiaHubSessionManager(){}

    public static boolean enter(ServerPlayer player){
        if(player.level().dimension()!=Level.OVERWORLD) return false;
        FieldSessionManager.remove(player); remove(player);
        ServerLevel level=(ServerLevel)player.level(); RadiaHubWorld.BuiltHub hub=RadiaHubWorld.build(level);
        Session s=new Session(hub); SESSIONS.put(player.getUUID(),s); s.refresh(level,player); s.spawn(level);
        player.setPos(hub.spawn().x,hub.spawn().y,hub.spawn().z); player.setYRot(180); player.setXRot(3); player.setDeltaMovement(Vec3.ZERO);
        player.sendSystemMessage(Component.literal("TURNBOUND · 라디아").withStyle(ChatFormatting.GOLD));
        FieldNetwork.sync(player,s.snapshot(player,FieldUiSnapshot.Mode.QUEST,null)); return true;
    }
    public static boolean active(ServerPlayer p){ return SESSIONS.containsKey(p.getUUID())&&p.level().dimension()==Level.OVERWORLD; }
    public static void tick(ServerPlayer p){ Session s=SESSIONS.get(p.getUUID()); if(s==null||BattleSessionManager.exists(p)) return; ServerLevel l=(ServerLevel)p.level(); if(!RadiaHubWorld.contains(p.position())){ p.setPos(s.hub.spawn().x,s.hub.spawn().y,s.hub.spawn().z); p.setDeltaMovement(Vec3.ZERO);} if(p.tickCount%20==0) clearMobs(l); }

    public static boolean interactEntity(ServerPlayer p,Entity target){
        Session s=SESSIONS.get(p.getUUID()); if(s==null||target==null) return false; UUID id=target.getUUID();
        if(id.equals(s.director)){ CampaignProgressStore.questInteract(p.getUUID(),"Director Iven"); CampaignPersistence.saveIfDirty(p); s.refresh((ServerLevel)p.level(),p); FieldNetwork.sync(p,s.snapshot(p,FieldUiSnapshot.Mode.QUEST,null)); return true; }
        if(id.equals(s.partyConsole)){ CampaignProgressStore.setActiveParty(p.getUUID(),CampaignProgressStore.activeParty(p.getUUID())); CampaignPersistence.saveIfDirty(p); s.refresh((ServerLevel)p.level(),p); FieldNetwork.sync(p,s.snapshot(p,FieldUiSnapshot.Mode.QUEST,null)); return true; }
        if(id.equals(s.relay)){ FieldNetwork.sync(p,s.snapshot(p,FieldUiSnapshot.Mode.TRAVEL,null)); return true; }
        if(id.equals(s.southGate)){ if(s.regionUnlocked(p)) transitionToMeadow(p); else FieldNetwork.sync(p,s.snapshot(p,FieldUiSnapshot.Mode.QUEST,null)); return true; }
        for(int i=0;i<s.tutorialActors.size();i++) if(id.equals(s.tutorialActors.get(i))){ s.startTutorial(p,i); return true; }
        return false;
    }

    public static void command(ServerPlayer p,String raw){ Session s=SESSIONS.get(p.getUUID()); if(s==null||raw==null||BattleSessionManager.exists(p)) return; String[] a=raw.split("\\|",-1); if(a.length<2||!"TRAVEL".equals(a[0])) return; if("SOUTH_GATE".equals(a[1])||AsterMarchRegionCatalog.FT_MEADOW.equals(a[1])){ if(s.regionUnlocked(p)) transitionToMeadow(p); } else if(AsterMarchRegionCatalog.FT_RADIA.equals(a[1])){ p.setPos(s.hub.spawn().x,s.hub.spawn().y,s.hub.spawn().z); p.setDeltaMovement(Vec3.ZERO); FieldNetwork.sync(p,s.snapshot(p,FieldUiSnapshot.Mode.NONE,null)); } }

    public static void onBattleEnded(ServerPlayer p,String encounterId,BattleOutcome outcome){
        Session s=SESSIONS.get(p.getUUID()); if(s==null||!TUTORIALS.contains(encounterId)) return; s.refresh((ServerLevel)p.level(),p);
        V04Catalogs.Encounter spec=CampaignEncounterCatalog.spec(encounterId); FieldUiSnapshot.Reward r=new FieldUiSnapshot.Reward(spec.label(),0,0,outcome==BattleOutcome.ALLY_VICTORY,false);
        FieldNetwork.sync(p,s.snapshot(p,outcome==BattleOutcome.ALLY_VICTORY?FieldUiSnapshot.Mode.RESULT:FieldUiSnapshot.Mode.QUEST,r));
    }

    public static void remove(ServerPlayer p){ Session s=SESSIONS.remove(p.getUUID()); if(s!=null&&p.level() instanceof ServerLevel l) s.despawn(l); if(s!=null) FieldNetwork.close(p); }
    public static void clearAll(Iterable<ServerPlayer> players){ for(ServerPlayer p:players) remove(p); SESSIONS.clear(); }

    private static void transitionToMeadow(ServerPlayer p){ remove(p); FieldSessionManager.enter(p); ServerLevel l=(ServerLevel)p.level(); int y=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,0,123)+1; p.setPos(0.5,y,123.5); p.setYRot(180); p.setDeltaMovement(Vec3.ZERO); }
    private static void clearMobs(ServerLevel l){ AABB a=new AABB(-132,54,-116,132,100,132); for(Mob m:l.getEntitiesOfClass(Mob.class,a)) m.discard(); }

    private static final class Session{
        private final RadiaHubWorld.BuiltHub hub; private UUID director,partyConsole,relay,southGate; private final List<UUID> tutorialActors=new ArrayList<>();
        private Session(RadiaHubWorld.BuiltHub hub){this.hub=hub;}
        private void refresh(ServerLevel l,ServerPlayer p){ RadiaHubWorld.setSouthGateOpen(l,regionUnlocked(p)); }
        private boolean flag(ServerPlayer p,String f){ return CampaignProgressStore.snapshot(p.getUUID()).quests().unlockFlags().contains(f); }
        private boolean complete(ServerPlayer p,String q){ return CampaignProgressStore.snapshot(p.getUUID()).quests().completed().contains(q); }
        private boolean regionUnlocked(ServerPlayer p){ return flag(p,"REGION_MEADOW")||complete(p,"MQ_P00_03_south_gate"); }
        private boolean tutorialUnlocked(ServerPlayer p,int i){ if(!flag(p,"BATTLE_TUTORIAL")) return false; Set<String> c=CampaignProgressStore.snapshot(p.getUUID()).clearedEncounters(); return i==0||c.contains(TUTORIALS.get(i-1)); }
        private void startTutorial(ServerPlayer p,int i){ if(i<0||i>=3||!tutorialUnlocked(p,i)||CampaignProgressStore.snapshot(p.getUUID()).clearedEncounters().contains(TUTORIALS.get(i))) return; Vec3 a=hub.tutorialBattleAnchors().get(i); BattleSessionManager.startEncounterAt(p,TUTORIALS.get(i),false,false,a,180); }
        private void spawn(ServerLevel l){
            director=spawnActor(l,hub.director(),"Director Iven",Items.SPYGLASS,ChatFormatting.GOLD);
            partyConsole=spawnActor(l,hub.partyConsole(),"파티 편성 콘솔",Items.COMPASS,ChatFormatting.AQUA);
            relay=spawnActor(l,hub.relay(),"라디아 계전소 · FT_RADIA",Items.AMETHYST_SHARD,ChatFormatting.LIGHT_PURPLE);
            southGate=spawnActor(l,hub.southGate(),"South Gate",Items.IRON_SWORD,ChatFormatting.GREEN);
            for(int i=0;i<3;i++) tutorialActors.add(spawnActor(l,hub.tutorialPedestals().get(i),"전투 훈련 "+(i+1),i==2?Items.TNT:Items.IRON_SWORD,ChatFormatting.YELLOW));
        }
        private UUID spawnActor(ServerLevel l,Vec3 p,String name,net.minecraft.world.item.Item item,ChatFormatting color){ ArmorStand s=new ArmorStand(l,p.x,p.y,p.z); s.setInvulnerable(true); s.setNoGravity(true); s.setShowArms(true); s.setCustomName(Component.literal(name).withStyle(color)); s.setCustomNameVisible(true); s.setItemSlot(EquipmentSlot.MAINHAND,item.getDefaultInstance()); l.addFreshEntity(s); return s.getUUID(); }
        private void despawn(ServerLevel l){ desp(l,director);desp(l,partyConsole);desp(l,relay);desp(l,southGate);for(UUID id:tutorialActors)desp(l,id);tutorialActors.clear(); }
        private void desp(ServerLevel l,UUID id){ if(id!=null){Entity e=l.getEntity(id);if(e!=null)e.discard();} }
        private FieldUiSnapshot snapshot(ServerPlayer p,FieldUiSnapshot.Mode mode,FieldUiSnapshot.Reward reward){
            Set<String> clears=CampaignProgressStore.snapshot(p.getUUID()).clearedEncounters(); List<FieldUiSnapshot.Encounter> es=new ArrayList<>(); int n=0;
            for(int i=0;i<3;i++){ String id=TUTORIALS.get(i); boolean done=clears.contains(id); if(done)n++; es.add(new FieldUiSnapshot.Encounter(id,CampaignEncounterCatalog.spec(id).label(),done,tutorialUnlocked(p,i),false)); }
            List<FieldUiSnapshot.Travel> ts=List.of(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_RADIA,"라디아 계전소",true,true),new FieldUiSnapshot.Travel("SOUTH_GATE","남문 초원 진입",regionUnlocked(p),false));
            return new FieldUiSnapshot(true,mode,n,3,false,false,0,0,objective(p,n),dialogue(p),reward==null?FieldUiSnapshot.Reward.none():reward,es,ts);
        }
        private String objective(ServerPlayer p,int wins){ if(!complete(p,"MQ_P00_01_arrival")) return "MQ_P00_01 라디아 도착 · Relay Hall의 Director Iven과 대화"; if(!complete(p,"MQ_P00_02_first_party")) return "MQ_P00_02 첫 파티 · 파티 편성 콘솔에서 P01/P03/P04/F03 편성 확인"; if(!complete(p,"MQ_P00_03_south_gate")) return "MQ_P00_03 남문 개방 · Training Yard 전투 훈련 "+wins+"/3"; return "Prologue 완료 · South Gate를 통해 남문 초원으로 이동"; }
        private String dialogue(ServerPlayer p){ if(!complete(p,"MQ_P00_01_arrival")) return "이븐 국장이 Relay Hall에서 기다리고 있다."; if(!complete(p,"MQ_P00_02_first_party")) return "첫 출동 전에 현재 4인 파티를 확인해."; if(!complete(p,"MQ_P00_03_south_gate")) return "훈련장 세 전투를 순서대로 끝내면 남문이 열린다."; return "남문 개방 완료. 초원 순찰 임무를 시작할 수 있다."; }
    }
}

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

/** Radia hub runtime, Prologue and all five main-story chapter routes. */
public final class RadiaHubSessionManager {
    private static final List<String> TUTORIALS=List.of("TUTORIAL_1","TUTORIAL_2","TUTORIAL_3");
    private static final Map<UUID,Session> SESSIONS=new LinkedHashMap<>();
    private RadiaHubSessionManager(){}

    public static boolean enter(ServerPlayer p){
        if(p.level().dimension()!=Level.OVERWORLD)return false;
        FieldSessionManager.remove(p);GloamwoodSessionManager.remove(p);BrokenAqueductSessionManager.remove(p);EmberQuarrySessionManager.remove(p);OldRelayStationSessionManager.remove(p);remove(p);
        ServerLevel l=(ServerLevel)p.level();RadiaHubWorld.BuiltHub hub=RadiaHubWorld.build(l);Session s=new Session(hub);SESSIONS.put(p.getUUID(),s);s.refresh(l,p);s.spawn(l);
        p.setPos(hub.spawn().x,hub.spawn().y,hub.spawn().z);p.setYRot(180);p.setXRot(3);p.setDeltaMovement(Vec3.ZERO);p.sendSystemMessage(Component.literal("TURNBOUND · 라디아").withStyle(ChatFormatting.GOLD));FieldNetwork.sync(p,s.snapshot(p,FieldUiSnapshot.Mode.QUEST,null));return true;
    }
    public static boolean active(ServerPlayer p){return SESSIONS.containsKey(p.getUUID())&&p.level().dimension()==Level.OVERWORLD;}
    public static void tick(ServerPlayer p){Session s=SESSIONS.get(p.getUUID());if(s==null||BattleSessionManager.exists(p))return;ServerLevel l=(ServerLevel)p.level();if(!RadiaHubWorld.contains(p.position())){Vec3 q=s.hub.spawn();p.setPos(q.x,q.y,q.z);p.setDeltaMovement(Vec3.ZERO);}if(p.tickCount%20==0)clearMobs(l);}

    public static boolean interactEntity(ServerPlayer p,Entity target){
        Session s=SESSIONS.get(p.getUUID());if(s==null||target==null)return false;UUID id=target.getUUID();
        if(id.equals(s.director)){CampaignProgressStore.questInteract(p.getUUID(),"Director Iven");CampaignPersistence.saveIfDirty(p);s.refresh((ServerLevel)p.level(),p);FieldNetwork.sync(p,s.snapshot(p,FieldUiSnapshot.Mode.QUEST,null));return true;}
        if(id.equals(s.partyConsole)){CampaignProgressStore.setActiveParty(p.getUUID(),CampaignProgressStore.activeParty(p.getUUID()));CampaignPersistence.saveIfDirty(p);s.refresh((ServerLevel)p.level(),p);FieldNetwork.sync(p,s.snapshot(p,FieldUiSnapshot.Mode.QUEST,null));return true;}
        if(id.equals(s.relay)){
            if(s.chapterFourComplete(p)&&!s.relayKeyComplete(p)){int submitted=RelayFragmentBridgeService.submitAvailable(p);if(submitted>0){CampaignPersistence.saveIfDirty(p);p.sendSystemMessage(Component.literal("Relay fragment 제출 "+submitted+"개 · 구 중계소 경로 확인").withStyle(ChatFormatting.LIGHT_PURPLE));}}
            FieldNetwork.sync(p,s.snapshot(p,FieldUiSnapshot.Mode.TRAVEL,null));return true;
        }
        if(id.equals(s.southGate)){if(s.regionUnlocked(p))transitionToMeadow(p);else FieldNetwork.sync(p,s.snapshot(p,FieldUiSnapshot.Mode.QUEST,null));return true;}
        for(int i=0;i<s.tutorialActors.size();i++)if(id.equals(s.tutorialActors.get(i))){s.startTutorial(p,i);return true;}
        return false;
    }

    public static void command(ServerPlayer p,String raw){Session s=SESSIONS.get(p.getUUID());if(s==null||raw==null||BattleSessionManager.exists(p))return;String[] a=raw.split("\\|",-1);if(a.length<2||!"TRAVEL".equals(a[0]))return;String d=a[1];
        if(("SOUTH_GATE".equals(d)||AsterMarchRegionCatalog.FT_MEADOW.equals(d))&&s.regionUnlocked(p))transitionToMeadow(p);
        else if(AsterMarchRegionCatalog.FT_GLOAM.equals(d)&&s.chapterOneComplete(p)){remove(p);GloamwoodSessionManager.enter(p);}
        else if(AsterMarchRegionCatalog.FT_AQUEDUCT.equals(d)&&s.chapterTwoComplete(p)){remove(p);BrokenAqueductSessionManager.enter(p);}
        else if(AsterMarchRegionCatalog.FT_QUARRY.equals(d)&&s.chapterThreeComplete(p)){remove(p);EmberQuarrySessionManager.enter(p);}
        else if(AsterMarchRegionCatalog.FT_RELAY.equals(d)&&s.relayKeyComplete(p)){remove(p);OldRelayStationSessionManager.enter(p);}
        else if(AsterMarchRegionCatalog.FT_RADIA.equals(d)){Vec3 q=s.hub.spawn();p.setPos(q.x,q.y,q.z);p.setDeltaMovement(Vec3.ZERO);FieldNetwork.sync(p,s.snapshot(p,FieldUiSnapshot.Mode.NONE,null));}
    }

    public static void onBattleEnded(ServerPlayer p,String id,BattleOutcome outcome){Session s=SESSIONS.get(p.getUUID());if(s==null||!TUTORIALS.contains(id))return;s.refresh((ServerLevel)p.level(),p);V04Catalogs.Encounter spec=CampaignEncounterCatalog.spec(id);FieldUiSnapshot.Reward r=new FieldUiSnapshot.Reward(spec.label(),0,0,outcome==BattleOutcome.ALLY_VICTORY,false);FieldNetwork.sync(p,s.snapshot(p,outcome==BattleOutcome.ALLY_VICTORY?FieldUiSnapshot.Mode.RESULT:FieldUiSnapshot.Mode.QUEST,r));}
    public static void remove(ServerPlayer p){Session s=SESSIONS.remove(p.getUUID());if(s!=null&&p.level() instanceof ServerLevel l)s.despawn(l);if(s!=null)FieldNetwork.close(p);}
    public static void clearAll(Iterable<ServerPlayer> ps){for(ServerPlayer p:ps)remove(p);SESSIONS.clear();}
    private static void transitionToMeadow(ServerPlayer p){remove(p);FieldSessionManager.enter(p);ServerLevel l=(ServerLevel)p.level();int y=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,0,123)+1;p.setPos(.5,y,123.5);p.setYRot(180);p.setDeltaMovement(Vec3.ZERO);}
    private static void clearMobs(ServerLevel l){AABB a=new AABB(-132,54,-116,132,100,132);for(Mob m:l.getEntitiesOfClass(Mob.class,a))m.discard();}

    private static final class Session{
        private final RadiaHubWorld.BuiltHub hub;private UUID director,partyConsole,relay,southGate;private final List<UUID> tutorialActors=new ArrayList<>();
        private Session(RadiaHubWorld.BuiltHub h){hub=h;}
        private void refresh(ServerLevel l,ServerPlayer p){RadiaHubWorld.setSouthGateOpen(l,regionUnlocked(p));}
        private boolean flag(ServerPlayer p,String f){return CampaignProgressStore.snapshot(p.getUUID()).quests().unlockFlags().contains(f);}
        private boolean complete(ServerPlayer p,String q){return CampaignProgressStore.snapshot(p.getUUID()).quests().completed().contains(q);}
        private boolean cleared(ServerPlayer p,String id){return CampaignProgressStore.snapshot(p.getUUID()).clearedEncounters().contains(id);}
        private boolean regionUnlocked(ServerPlayer p){return flag(p,"REGION_MEADOW")||complete(p,"MQ_P00_03_south_gate");}
        private boolean chapterOneComplete(ServerPlayer p){return cleared(p,"BATTLE_B01")||complete(p,"MQ_C01_03_graul");}
        private boolean chapterTwoComplete(ServerPlayer p){return cleared(p,"BATTLE_B02")||complete(p,"MQ_C02_03_verna");}
        private boolean chapterThreeComplete(ServerPlayer p){return cleared(p,"BATTLE_B03")||complete(p,"MQ_C03_03_oro7");}
        private boolean chapterFourComplete(ServerPlayer p){return cleared(p,"BATTLE_B04")||complete(p,"MQ_C04_03_kolvak");}
        private boolean relayKeyComplete(ServerPlayer p){return complete(p,"MQ_C05_01_relay_key")||flag(p,"OLD_RELAY_ENTRANCE");}
        private boolean storyComplete(ServerPlayer p){return complete(p,"MQ_C05_03_reconnect")||flag(p,"ENDGAME");}
        private boolean tutorialUnlocked(ServerPlayer p,int i){if(!flag(p,"BATTLE_TUTORIAL"))return false;Set<String> c=CampaignProgressStore.snapshot(p.getUUID()).clearedEncounters();return i==0||c.contains(TUTORIALS.get(i-1));}
        private void startTutorial(ServerPlayer p,int i){if(i<0||i>=3||!tutorialUnlocked(p,i)||cleared(p,TUTORIALS.get(i)))return;Vec3 a=hub.tutorialBattleAnchors().get(i);BattleSessionManager.startEncounterAt(p,TUTORIALS.get(i),false,false,a,180);}
        private void spawn(ServerLevel l){director=actor(l,hub.director(),"Director Iven",Items.SPYGLASS,ChatFormatting.GOLD);partyConsole=actor(l,hub.partyConsole(),"파티 편성 콘솔",Items.COMPASS,ChatFormatting.AQUA);relay=actor(l,hub.relay(),"라디아 계전소 · FT_RADIA",Items.AMETHYST_SHARD,ChatFormatting.LIGHT_PURPLE);southGate=actor(l,hub.southGate(),"South Gate",Items.IRON_SWORD,ChatFormatting.GREEN);for(int i=0;i<3;i++)tutorialActors.add(actor(l,hub.tutorialPedestals().get(i),"전투 훈련 "+(i+1),i==2?Items.TNT:Items.IRON_SWORD,ChatFormatting.YELLOW));}
        private UUID actor(ServerLevel l,Vec3 p,String name,net.minecraft.world.item.Item item,ChatFormatting color){ArmorStand a=new ArmorStand(l,p.x,p.y,p.z);a.setInvulnerable(true);a.setNoGravity(true);a.setShowArms(true);a.setCustomName(Component.literal(name).withStyle(color));a.setCustomNameVisible(true);a.setItemSlot(EquipmentSlot.MAINHAND,item.getDefaultInstance());l.addFreshEntity(a);return a.getUUID();}
        private void despawn(ServerLevel l){desp(l,director);desp(l,partyConsole);desp(l,relay);desp(l,southGate);for(UUID id:tutorialActors)desp(l,id);tutorialActors.clear();}private void desp(ServerLevel l,UUID id){if(id!=null){Entity e=l.getEntity(id);if(e!=null)e.discard();}}

        private FieldUiSnapshot snapshot(ServerPlayer p,FieldUiSnapshot.Mode mode,FieldUiSnapshot.Reward reward){Set<String> clears=CampaignProgressStore.snapshot(p.getUUID()).clearedEncounters();List<FieldUiSnapshot.Encounter> es=new ArrayList<>();int wins=0;for(int i=0;i<3;i++){String id=TUTORIALS.get(i);boolean done=clears.contains(id);if(done)wins++;es.add(new FieldUiSnapshot.Encounter(id,CampaignEncounterCatalog.spec(id).label(),done,tutorialUnlocked(p,i),false));}List<FieldUiSnapshot.Travel> ts=new ArrayList<>();ts.add(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_RADIA,"라디아 계전소",true,true));ts.add(new FieldUiSnapshot.Travel("SOUTH_GATE","남문 초원 진입",regionUnlocked(p),false));ts.add(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_GLOAM,"그늘숲 Chapter 2",chapterOneComplete(p),false));ts.add(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_AQUEDUCT,"붕괴 수로 Chapter 3",chapterTwoComplete(p),false));ts.add(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_QUARRY,"잿불 채석장 Chapter 4",chapterThreeComplete(p),false));ts.add(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_RELAY,"구 중계소 Chapter 5",relayKeyComplete(p),false));return new FieldUiSnapshot(true,mode,wins,3,false,storyComplete(p),0,0,objective(p,wins),dialogue(p),reward==null?FieldUiSnapshot.Reward.none():reward,es,List.copyOf(ts));}
        private String objective(ServerPlayer p,int wins){if(!complete(p,"MQ_P00_01_arrival"))return "MQ_P00_01 라디아 도착 · Director Iven과 대화";if(!complete(p,"MQ_P00_02_first_party"))return "MQ_P00_02 첫 파티 · P01/P03/P04/F03 편성 확인";if(!complete(p,"MQ_P00_03_south_gate"))return "MQ_P00_03 남문 개방 · 전투 훈련 "+wins+"/3";if(!chapterOneComplete(p))return "Chapter 1 · B01 그라울 격파";if(!chapterTwoComplete(p))return "Chapter 2 · B02 베르나 격파";if(!chapterThreeComplete(p))return "Chapter 3 · B03 ORO-7 정지";if(!chapterFourComplete(p))return "Chapter 4 · B04 콜바크 격파";if(!relayKeyComplete(p))return "MQ_C05_01 중계소 열쇠 · 라디아 계전소에 세 지역 Relay fragment 제출";if(!storyComplete(p))return "Chapter 5 해금 · 구 중계소로 이동해 세라크와 Relay 재연결 진행";return "메인 스토리 완료 · Endgame 콘텐츠 개방";}
        private String dialogue(ServerPlayer p){if(!complete(p,"MQ_P00_03_south_gate"))return "훈련을 끝내고 남문을 개방해야 한다.";if(!chapterFourComplete(p))return "아스테르 변경의 Relay 이상 신호를 각 지역에서 추적해.";if(!relayKeyComplete(p))return "확보한 세 지역 Relay 조각을 중앙 계전소에 제출하면 구 중계소 좌표를 복원할 수 있다.";if(!storyComplete(p))return "구 중계소 좌표가 복원됐다. 세라크와 마지막 Relay를 확인해.";return "Relay 일부가 재가동되었고 동쪽 외부 지역에서 후속 신호가 잡힌다.";}
    }
}

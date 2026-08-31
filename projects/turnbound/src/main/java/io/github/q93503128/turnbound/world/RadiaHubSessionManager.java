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

/** Radia hub runtime, Prologue and chapter travel routing. */
public final class RadiaHubSessionManager {
    private static final List<String> TUTORIALS = List.of("TUTORIAL_1", "TUTORIAL_2", "TUTORIAL_3");
    private static final Map<UUID, Session> SESSIONS = new LinkedHashMap<>();
    private RadiaHubSessionManager() {}

    public static boolean enter(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return false;
        FieldSessionManager.remove(player); GloamwoodSessionManager.remove(player);
        BrokenAqueductSessionManager.remove(player); EmberQuarrySessionManager.remove(player); remove(player);
        ServerLevel level = (ServerLevel) player.level();
        RadiaHubWorld.BuiltHub hub = RadiaHubWorld.build(level);
        Session session = new Session(hub); SESSIONS.put(player.getUUID(), session);
        session.refresh(level, player); session.spawn(level);
        player.setPos(hub.spawn().x, hub.spawn().y, hub.spawn().z); player.setYRot(180.0F); player.setXRot(3.0F); player.setDeltaMovement(Vec3.ZERO);
        player.sendSystemMessage(Component.literal("TURNBOUND · 라디아").withStyle(ChatFormatting.GOLD));
        FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
        return true;
    }

    public static boolean active(ServerPlayer player) { return SESSIONS.containsKey(player.getUUID()) && player.level().dimension() == Level.OVERWORLD; }
    public static void tick(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID()); if (session == null || BattleSessionManager.exists(player)) return;
        ServerLevel level = (ServerLevel) player.level();
        if (!RadiaHubWorld.contains(player.position())) { Vec3 p=session.hub.spawn(); player.setPos(p.x,p.y,p.z); player.setDeltaMovement(Vec3.ZERO); }
        if (player.tickCount % 20 == 0) clearMobs(level);
    }

    public static boolean interactEntity(ServerPlayer player, Entity target) {
        Session s=SESSIONS.get(player.getUUID()); if(s==null||target==null)return false; UUID id=target.getUUID();
        if(id.equals(s.director)){CampaignProgressStore.questInteract(player.getUUID(),"Director Iven");CampaignPersistence.saveIfDirty(player);s.refresh((ServerLevel)player.level(),player);FieldNetwork.sync(player,s.snapshot(player,FieldUiSnapshot.Mode.QUEST,null));return true;}
        if(id.equals(s.partyConsole)){CampaignProgressStore.setActiveParty(player.getUUID(),CampaignProgressStore.activeParty(player.getUUID()));CampaignPersistence.saveIfDirty(player);s.refresh((ServerLevel)player.level(),player);FieldNetwork.sync(player,s.snapshot(player,FieldUiSnapshot.Mode.QUEST,null));return true;}
        if(id.equals(s.relay)){FieldNetwork.sync(player,s.snapshot(player,FieldUiSnapshot.Mode.TRAVEL,null));return true;}
        if(id.equals(s.southGate)){if(s.regionUnlocked(player))transitionToMeadow(player);else FieldNetwork.sync(player,s.snapshot(player,FieldUiSnapshot.Mode.QUEST,null));return true;}
        for(int i=0;i<s.tutorialActors.size();i++)if(id.equals(s.tutorialActors.get(i))){s.startTutorial(player,i);return true;}
        return false;
    }

    public static void command(ServerPlayer player,String raw){
        Session s=SESSIONS.get(player.getUUID());if(s==null||raw==null||BattleSessionManager.exists(player))return;String[] a=raw.split("\\|",-1);if(a.length<2||!"TRAVEL".equals(a[0]))return;String d=a[1];
        if(("SOUTH_GATE".equals(d)||AsterMarchRegionCatalog.FT_MEADOW.equals(d))&&s.regionUnlocked(player))transitionToMeadow(player);
        else if(AsterMarchRegionCatalog.FT_GLOAM.equals(d)&&s.chapterOneComplete(player)){remove(player);GloamwoodSessionManager.enter(player);}
        else if(AsterMarchRegionCatalog.FT_AQUEDUCT.equals(d)&&s.chapterTwoComplete(player)){remove(player);BrokenAqueductSessionManager.enter(player);}
        else if(AsterMarchRegionCatalog.FT_QUARRY.equals(d)&&s.chapterThreeComplete(player)){remove(player);EmberQuarrySessionManager.enter(player);}
        else if(AsterMarchRegionCatalog.FT_RADIA.equals(d)){Vec3 p=s.hub.spawn();player.setPos(p.x,p.y,p.z);player.setDeltaMovement(Vec3.ZERO);FieldNetwork.sync(player,s.snapshot(player,FieldUiSnapshot.Mode.NONE,null));}
    }

    public static void onBattleEnded(ServerPlayer player,String encounterId,BattleOutcome outcome){
        Session s=SESSIONS.get(player.getUUID());if(s==null||!TUTORIALS.contains(encounterId))return;s.refresh((ServerLevel)player.level(),player);V04Catalogs.Encounter spec=CampaignEncounterCatalog.spec(encounterId);
        FieldUiSnapshot.Reward r=new FieldUiSnapshot.Reward(spec.label(),0,0,outcome==BattleOutcome.ALLY_VICTORY,false);
        FieldNetwork.sync(player,s.snapshot(player,outcome==BattleOutcome.ALLY_VICTORY?FieldUiSnapshot.Mode.RESULT:FieldUiSnapshot.Mode.QUEST,r));
    }

    public static void remove(ServerPlayer player){Session s=SESSIONS.remove(player.getUUID());if(s!=null&&player.level() instanceof ServerLevel l)s.despawn(l);if(s!=null)FieldNetwork.close(player);}
    public static void clearAll(Iterable<ServerPlayer> players){for(ServerPlayer p:players)remove(p);SESSIONS.clear();}
    private static void transitionToMeadow(ServerPlayer p){remove(p);FieldSessionManager.enter(p);ServerLevel l=(ServerLevel)p.level();int y=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,0,123)+1;p.setPos(0.5,y,123.5);p.setYRot(180);p.setDeltaMovement(Vec3.ZERO);}
    private static void clearMobs(ServerLevel l){AABB a=new AABB(-132,54,-116,132,100,132);for(Mob m:l.getEntitiesOfClass(Mob.class,a))m.discard();}

    private static final class Session{
        private final RadiaHubWorld.BuiltHub hub; private UUID director,partyConsole,relay,southGate; private final List<UUID> tutorialActors=new ArrayList<>();
        private Session(RadiaHubWorld.BuiltHub hub){this.hub=hub;}
        private void refresh(ServerLevel l,ServerPlayer p){RadiaHubWorld.setSouthGateOpen(l,regionUnlocked(p));}
        private boolean flag(ServerPlayer p,String f){return CampaignProgressStore.snapshot(p.getUUID()).quests().unlockFlags().contains(f);}
        private boolean complete(ServerPlayer p,String q){return CampaignProgressStore.snapshot(p.getUUID()).quests().completed().contains(q);}
        private boolean cleared(ServerPlayer p,String id){return CampaignProgressStore.snapshot(p.getUUID()).clearedEncounters().contains(id);}
        private boolean regionUnlocked(ServerPlayer p){return flag(p,"REGION_MEADOW")||complete(p,"MQ_P00_03_south_gate");}
        private boolean chapterOneComplete(ServerPlayer p){return cleared(p,"BATTLE_B01")||complete(p,"MQ_C01_03_graul");}
        private boolean chapterTwoComplete(ServerPlayer p){return cleared(p,"BATTLE_B02")||complete(p,"MQ_C02_03_verna");}
        private boolean chapterThreeComplete(ServerPlayer p){return cleared(p,"BATTLE_B03")||complete(p,"MQ_C03_03_oro7");}
        private boolean tutorialUnlocked(ServerPlayer p,int i){if(!flag(p,"BATTLE_TUTORIAL"))return false;Set<String> c=CampaignProgressStore.snapshot(p.getUUID()).clearedEncounters();return i==0||c.contains(TUTORIALS.get(i-1));}
        private void startTutorial(ServerPlayer p,int i){if(i<0||i>=3||!tutorialUnlocked(p,i)||cleared(p,TUTORIALS.get(i)))return;Vec3 a=hub.tutorialBattleAnchors().get(i);BattleSessionManager.startEncounterAt(p,TUTORIALS.get(i),false,false,a,180);}
        private void spawn(ServerLevel l){director=actor(l,hub.director(),"Director Iven",Items.SPYGLASS,ChatFormatting.GOLD);partyConsole=actor(l,hub.partyConsole(),"파티 편성 콘솔",Items.COMPASS,ChatFormatting.AQUA);relay=actor(l,hub.relay(),"라디아 계전소 · FT_RADIA",Items.AMETHYST_SHARD,ChatFormatting.LIGHT_PURPLE);southGate=actor(l,hub.southGate(),"South Gate",Items.IRON_SWORD,ChatFormatting.GREEN);for(int i=0;i<3;i++)tutorialActors.add(actor(l,hub.tutorialPedestals().get(i),"전투 훈련 "+(i+1),i==2?Items.TNT:Items.IRON_SWORD,ChatFormatting.YELLOW));}
        private UUID actor(ServerLevel l,Vec3 p,String name,net.minecraft.world.item.Item item,ChatFormatting color){ArmorStand s=new ArmorStand(l,p.x,p.y,p.z);s.setInvulnerable(true);s.setNoGravity(true);s.setShowArms(true);s.setCustomName(Component.literal(name).withStyle(color));s.setCustomNameVisible(true);s.setItemSlot(EquipmentSlot.MAINHAND,item.getDefaultInstance());l.addFreshEntity(s);return s.getUUID();}
        private void despawn(ServerLevel l){desp(l,director);desp(l,partyConsole);desp(l,relay);desp(l,southGate);for(UUID id:tutorialActors)desp(l,id);tutorialActors.clear();}
        private void desp(ServerLevel l,UUID id){if(id!=null){Entity e=l.getEntity(id);if(e!=null)e.discard();}}

        private FieldUiSnapshot snapshot(ServerPlayer p,FieldUiSnapshot.Mode mode,FieldUiSnapshot.Reward reward){
            Set<String> clears=CampaignProgressStore.snapshot(p.getUUID()).clearedEncounters();List<FieldUiSnapshot.Encounter> es=new ArrayList<>();int wins=0;for(int i=0;i<3;i++){String id=TUTORIALS.get(i);boolean done=clears.contains(id);if(done)wins++;es.add(new FieldUiSnapshot.Encounter(id,CampaignEncounterCatalog.spec(id).label(),done,tutorialUnlocked(p,i),false));}
            List<FieldUiSnapshot.Travel> ts=new ArrayList<>();ts.add(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_RADIA,"라디아 계전소",true,true));ts.add(new FieldUiSnapshot.Travel("SOUTH_GATE","남문 초원 진입",regionUnlocked(p),false));ts.add(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_GLOAM,"그늘숲 Chapter 2",chapterOneComplete(p),false));ts.add(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_AQUEDUCT,"붕괴 수로 Chapter 3",chapterTwoComplete(p),false));ts.add(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_QUARRY,"잿불 채석장 Chapter 4",chapterThreeComplete(p),false));
            return new FieldUiSnapshot(true,mode,wins,3,false,false,0,0,objective(p,wins),dialogue(p),reward==null?FieldUiSnapshot.Reward.none():reward,es,List.copyOf(ts));
        }
        private String objective(ServerPlayer p,int wins){if(!complete(p,"MQ_P00_01_arrival"))return "MQ_P00_01 라디아 도착 · Director Iven과 대화";if(!complete(p,"MQ_P00_02_first_party"))return "MQ_P00_02 첫 파티 · P01/P03/P04/F03 편성 확인";if(!complete(p,"MQ_P00_03_south_gate"))return "MQ_P00_03 남문 개방 · 전투 훈련 "+wins+"/3";if(!chapterOneComplete(p))return "Chapter 1 · B01 그라울까지 격파";if(!chapterTwoComplete(p))return "Chapter 2 · 그늘숲 베르나 격파";if(!chapterThreeComplete(p))return "Chapter 3 · 붕괴 수로 ORO-7 정지";if(!complete(p,"MQ_C04_03_kolvak"))return "Chapter 4 해금 · 라디아 계전소에서 잿불 채석장으로 이동";return "Chapter 4 완료 · 구 중계소 진입 준비";}
        private String dialogue(ServerPlayer p){if(!complete(p,"MQ_P00_03_south_gate"))return "훈련을 끝내고 남문을 개방해야 한다.";if(!chapterOneComplete(p))return "남문 초원의 Relay 이상 신호를 추적해.";if(!chapterTwoComplete(p))return "그늘숲 Relay 신호를 복구해.";if(!chapterThreeComplete(p))return "서쪽 수로의 오래된 관리기 신호를 정지시켜.";if(!complete(p,"MQ_C04_03_kolvak"))return "남동쪽 채석장에 대단절 당시의 열핵 반응이 남아 있다.";return "콜바크 내부에서 구 중계소로 이어지는 Relay 단서를 확보했다.";}
    }
}

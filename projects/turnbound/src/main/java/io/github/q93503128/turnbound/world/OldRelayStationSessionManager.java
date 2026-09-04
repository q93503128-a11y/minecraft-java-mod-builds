package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Final main-story field runtime: four Serak records, R01-R05, B05 and post-boss Relay reconnect. */
public final class OldRelayStationSessionManager {
    private static final List<String> NORMAL_IDS = List.of("ENC_R01", "ENC_R02", "ENC_R03", "ENC_R04", "ENC_R05");
    private static final String BOSS_ID = "BATTLE_B05";
    private static final String RECORD_QUEST = "MQ_C05_02_serak_record";
    private static final String FINAL_QUEST = "MQ_C05_03_reconnect";
    private static final Map<UUID, Session> SESSIONS = new LinkedHashMap<>();

    private OldRelayStationSessionManager() {}

    public static boolean enter(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD || !chapterUnlocked(player)) return false;
        RadiaHubSessionManager.remove(player); FieldSessionManager.remove(player); GloamwoodSessionManager.remove(player);
        BrokenAqueductSessionManager.remove(player); EmberQuarrySessionManager.remove(player); remove(player);
        ServerLevel level = (ServerLevel) player.level();
        OldRelayStationWorld.BuiltChapter chapter = OldRelayStationWorld.build(level);
        Session session = new Session(chapter); SESSIONS.put(player.getUUID(), session);
        session.refresh(level, player); session.spawnAll(level, player);
        Vec3 p = chapter.entry(); player.setPos(p.x,p.y,p.z); player.setYRot(90); player.setXRot(4); player.setDeltaMovement(Vec3.ZERO);
        player.sendSystemMessage(Component.literal("TURNBOUND · Chapter 5 구 중계소").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("세라크 기록 4개를 복원하고 균열감시자를 격파한 뒤 Relay console을 재가동하십시오.").withStyle(ChatFormatting.GRAY));
        FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
        return true;
    }

    public static boolean active(ServerPlayer p){return SESSIONS.containsKey(p.getUUID())&&p.level().dimension()==Level.OVERWORLD;}
    public static void tick(ServerPlayer p){Session s=SESSIONS.get(p.getUUID());if(s==null||BattleSessionManager.exists(p))return;ServerLevel l=(ServerLevel)p.level();if(!OldRelayStationWorld.contains(p.position())){Vec3 e=s.chapter.entry();p.setPos(e.x,e.y,e.z);p.setDeltaMovement(Vec3.ZERO);return;}if(p.tickCount%20==0)clearVanillaMobs(l);s.tickEncounters(l,p);}

    public static boolean interactEntity(ServerPlayer p,Entity target){
        Session s=SESSIONS.get(p.getUUID());if(s==null||target==null)return false;UUID id=target.getUUID();
        if(id.equals(s.relay)){FieldNetwork.sync(p,s.snapshot(p,FieldUiSnapshot.Mode.TRAVEL,null));return true;}
        Integer record=s.recordActors.remove(id);if(record!=null){if(!s.questComplete(p,RECORD_QUEST)){CampaignProgressStore.questInteract(p.getUUID(),"SERAK_RECORD");Entity e=((ServerLevel)p.level()).getEntity(id);if(e!=null)e.discard();CampaignPersistence.saveIfDirty(p);s.refresh((ServerLevel)p.level(),p);s.spawnMissing((ServerLevel)p.level(),p);FieldNetwork.sync(p,s.snapshot(p,FieldUiSnapshot.Mode.QUEST,null));}return true;}
        if(id.equals(s.finalConsole)){if(s.bossCleared(p)&&!s.questComplete(p,FINAL_QUEST)){CampaignProgressStore.questInteract(p.getUUID(),"RELAY_CONSOLE");CampaignPersistence.saveIfDirty(p);s.refresh((ServerLevel)p.level(),p);FieldNetwork.sync(p,s.snapshot(p,FieldUiSnapshot.Mode.RESULT,null));if(s.questComplete(p,FINAL_QUEST))p.sendSystemMessage(Component.literal("Aster March Relay 재연결 · Endgame 개방").withStyle(ChatFormatting.LIGHT_PURPLE,ChatFormatting.BOLD));}return true;}
        return false;
    }

    public static void command(ServerPlayer p,String raw){Session s=SESSIONS.get(p.getUUID());if(s==null||raw==null||BattleSessionManager.exists(p))return;String[] a=raw.split("\\|",-1);if(a.length<2||!"TRAVEL".equals(a[0]))return;if(AsterMarchRegionCatalog.FT_RADIA.equals(a[1])){remove(p);RadiaHubSessionManager.enter(p);}else if(AsterMarchRegionCatalog.FT_RELAY.equals(a[1])){Vec3 ft=s.chapter.fastTravel();p.setPos(ft.x,ft.y,ft.z);p.setYRot(90);p.setDeltaMovement(Vec3.ZERO);FieldNetwork.sync(p,s.snapshot(p,FieldUiSnapshot.Mode.NONE,null));}}

    public static void onBattleEnded(ServerPlayer p,String encounterId,BattleOutcome outcome){
        Session s=SESSIONS.get(p.getUUID());if(s==null)return;EncounterActor actor=s.encounters.get(encounterId);if(actor==null)return;actor.engaged=false;actor.group=null;actor.graceTicks=outcome==BattleOutcome.ALLY_VICTORY?0:40;ServerLevel l=(ServerLevel)p.level();s.refresh(l,p);s.spawnMissing(l,p);V04Catalogs.Encounter spec=CampaignEncounterCatalog.spec(encounterId);boolean win=outcome==BattleOutcome.ALLY_VICTORY;FieldUiSnapshot.Reward r=new FieldUiSnapshot.Reward(spec.label(),win?V04Catalogs.battleXp(spec):0,win?V04Catalogs.battleGold(spec):0,win,win&&BOSS_ID.equals(encounterId));if(win&&BOSS_ID.equals(encounterId))p.sendSystemMessage(Component.literal("세라크 격파 · 최종 Relay console을 작동하십시오.").withStyle(ChatFormatting.GOLD,ChatFormatting.BOLD));FieldNetwork.sync(p,s.snapshot(p,win?FieldUiSnapshot.Mode.RESULT:FieldUiSnapshot.Mode.QUEST,r));
    }

    public static void remove(ServerPlayer p){Session s=SESSIONS.remove(p.getUUID());if(s!=null&&p.level() instanceof ServerLevel l)s.despawnAll(l);if(s!=null)FieldNetwork.close(p);}
    public static void clearAll(Iterable<ServerPlayer> ps){for(ServerPlayer p:ps)remove(p);SESSIONS.clear();}
    public static boolean chapterUnlocked(ServerPlayer p){var q=CampaignProgressStore.snapshot(p.getUUID()).quests();return q.completed().contains("MQ_C05_01_relay_key")||q.unlockFlags().contains("OLD_RELAY_ENTRANCE");}
    private static void clearVanillaMobs(ServerLevel l){AABB a=new AABB(240,46,AsterMarchRegionCatalog.OLD_RELAY.minZ()-12,AsterMarchRegionCatalog.OLD_RELAY.maxX()+12,106,-160);for(Mob m:l.getEntitiesOfClass(Mob.class,a))if(!(m instanceof BattleActorEntity))m.discard();}

    private static final class Session{
        private final OldRelayStationWorld.BuiltChapter chapter;private final Map<String,EncounterActor> encounters=new LinkedHashMap<>();private final Map<UUID,Integer> recordActors=new LinkedHashMap<>();private UUID relay,finalConsole;
        private Session(OldRelayStationWorld.BuiltChapter chapter){this.chapter=chapter;for(OldRelayStationWorld.EncounterPoint p:chapter.encounters())encounters.put(p.id(),new EncounterActor(p));}
        private boolean questComplete(ServerPlayer p,String id){return CampaignProgressStore.snapshot(p.getUUID()).quests().completed().contains(id);}
        private boolean flag(ServerPlayer p,String id){return CampaignProgressStore.snapshot(p.getUUID()).quests().unlockFlags().contains(id);}
        private boolean cleared(ServerPlayer p,String id){return CampaignProgressStore.snapshot(p.getUUID()).clearedEncounters().contains(id);}
        private boolean bossOpen(ServerPlayer p){return flag(p,"B05_GATE")||questComplete(p,RECORD_QUEST);}
        private boolean bossCleared(ServerPlayer p){return cleared(p,BOSS_ID);}
        private int recordCount(ServerPlayer p){return CampaignProgressStore.quests(p.getUUID()).counters().getOrDefault(RECORD_QUEST,0);}
        private boolean unlocked(ServerPlayer p,String id){return !id.equals(BOSS_ID)||bossOpen(p);}
        private void refresh(ServerLevel l,ServerPlayer p){OldRelayStationWorld.setEntranceOpen(l,true);OldRelayStationWorld.setBossGateOpen(l,bossOpen(p));}
        private void spawnAll(ServerLevel l,ServerPlayer p){spawnRelay(l);spawnMissing(l,p);}
        private void spawnMissing(ServerLevel l,ServerPlayer p){spawnRecordsMissing(l,p);spawnFinalConsole(l,p);for(EncounterActor a:encounters.values()){if(!unlocked(p,a.point.id())||cleared(p,a.point.id())||a.engaged){a.despawn(l);continue;}a.spawn(l);}}
        private void spawnRelay(ServerLevel l){if(relay!=null&&l.getEntity(relay)!=null)return;ArmorStand a=actor(l,chapter.fastTravel(),"구 중계소 계전소 · FT_RELAY",Items.AMETHYST_SHARD,ChatFormatting.LIGHT_PURPLE);l.addFreshEntity(a);relay=a.getUUID();}
        private void spawnRecordsMissing(ServerLevel l,ServerPlayer p){if(questComplete(p,RECORD_QUEST))return;int n=Math.min(4,recordCount(p));for(int i=n;i<chapter.recordConsoles().size();i++){if(recordActors.containsValue(i))continue;ArmorStand a=actor(l,chapter.recordConsoles().get(i),"세라크 기록 "+(i+1)+"/4",Items.WRITABLE_BOOK,ChatFormatting.AQUA);l.addFreshEntity(a);recordActors.put(a.getUUID(),i);}}
        private void spawnFinalConsole(ServerLevel l,ServerPlayer p){if(!bossCleared(p)||questComplete(p,FINAL_QUEST)){if(finalConsole!=null){Entity e=l.getEntity(finalConsole);if(e!=null)e.discard();finalConsole=null;}return;}if(finalConsole!=null&&l.getEntity(finalConsole)!=null)return;ArmorStand a=actor(l,chapter.relayConsole(),"Relay 재연결 콘솔",Items.COMPARATOR,ChatFormatting.LIGHT_PURPLE);l.addFreshEntity(a);finalConsole=a.getUUID();}
        private void tickEncounters(ServerLevel l,ServerPlayer p){for(EncounterActor a:encounters.values()){if(!unlocked(p,a.point.id())||cleared(p,a.point.id()))continue;if(a.graceTicks>0){a.graceTicks--;continue;}a.spawn(l);if(a.group==null||a.engaged)continue;Entity lead=a.group.lead(l);if(lead==null){a.group=null;continue;}if(p.position().distanceToSqr(a.group.center())<=12.25){boolean started=BattleSessionManager.startEncounterAt(p,a.point.id(),true,true,a.point.battleAnchor(),a.point.battleYaw());if(started){a.despawn(l);a.engaged=true;return;}a.graceTicks=40;}}}
        private FieldUiSnapshot snapshot(ServerPlayer p,FieldUiSnapshot.Mode mode,FieldUiSnapshot.Reward reward){Set<String> c=CampaignProgressStore.snapshot(p.getUUID()).clearedEncounters();List<FieldUiSnapshot.Encounter> views=new ArrayList<>();for(String id:NORMAL_IDS)views.add(view(p,id,c));views.add(view(p,BOSS_ID,c));List<FieldUiSnapshot.Travel> travel=List.of(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_RELAY,"구 중계소 계전소",true,p.position().distanceToSqr(chapter.fastTravel())<=196),new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_RADIA,"라디아 귀환",true,false));int n=0;for(String id:NORMAL_IDS)if(c.contains(id))n++;return new FieldUiSnapshot(true,mode,n,5,bossOpen(p),questComplete(p,FINAL_QUEST),0,0,objective(p),dialogue(p),reward==null?FieldUiSnapshot.Reward.none():reward,views,travel);}
        private FieldUiSnapshot.Encounter view(ServerPlayer p,String id,Set<String> c){V04Catalogs.Encounter e=CampaignEncounterCatalog.spec(id);return new FieldUiSnapshot.Encounter(id,e.label(),c.contains(id),unlocked(p,id),e.boss());}
        private String objective(ServerPlayer p){if(!questComplete(p,RECORD_QUEST))return "MQ_C05_02 세라크 기록 · 기록실 조사 "+Math.min(4,recordCount(p))+"/4";if(!bossCleared(p))return "MQ_C05_03 재연결 · B05 균열감시자 세라크 격파";if(!questComplete(p,FINAL_QUEST))return "MQ_C05_03 재연결 · Relay console 작동";return "메인 스토리 완료 · Rift Gate / Hard Boss / Signature Trial 개방";}
        private String dialogue(ServerPlayer p){if(!bossOpen(p))return "중계소 기록실 네 곳에 세라크가 남긴 봉쇄 기록이 흩어져 있다.";if(!bossCleared(p))return "기록 복원이 끝났다. 세라크는 중계소를 지키기 위해 스스로 균열과 결합했다.";if(!questComplete(p,FINAL_QUEST))return "세라크는 쓰러졌지만 Relay는 아직 멈춰 있다. 마지막 콘솔을 직접 재가동해.";return "Relay 일부가 재가동되었고 동쪽 외부 지역의 미약한 신호가 잡힌다.";}
        private void despawnAll(ServerLevel l){if(relay!=null){Entity e=l.getEntity(relay);if(e!=null)e.discard();relay=null;}if(finalConsole!=null){Entity e=l.getEntity(finalConsole);if(e!=null)e.discard();finalConsole=null;}for(UUID id:List.copyOf(recordActors.keySet())){Entity e=l.getEntity(id);if(e!=null)e.discard();}recordActors.clear();for(EncounterActor a:encounters.values())a.despawn(l);}
    }

    private static final class EncounterActor{
        private final OldRelayStationWorld.EncounterPoint point;private FieldEncounterPresentation.Group group;private boolean engaged;private int graceTicks;
        private EncounterActor(OldRelayStationWorld.EncounterPoint p){point=p;}
        private void spawn(ServerLevel l){if(group!=null&&group.alive(l))return;if(group!=null)group.despawn(l);group=FieldEncounterPresentation.spawn(l,point.id(),point.fieldPosition(),point.battleYaw());}
        private void despawn(ServerLevel l){if(group!=null){group.despawn(l);group=null;}}
    }
    private static ArmorStand actor(ServerLevel l,Vec3 p,String name,Item item,ChatFormatting color){ArmorStand a=new ArmorStand(l,p.x,p.y,p.z);a.setInvulnerable(true);a.setNoGravity(true);a.setShowArms(true);a.setCustomName(Component.literal(name).withStyle(color));a.setCustomNameVisible(true);a.setItemSlot(EquipmentSlot.MAINHAND,item.getDefaultInstance());a.setItemSlot(EquipmentSlot.HEAD,Items.IRON_HELMET.getDefaultInstance());return a;}
}

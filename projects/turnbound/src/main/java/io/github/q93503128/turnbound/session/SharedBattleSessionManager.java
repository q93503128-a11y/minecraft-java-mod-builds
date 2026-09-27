package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.combat.SharedBattleFactory;
import io.github.q93503128.turnbound.world.CampaignPersistence;
import io.github.q93503128.turnbound.world.ExternalWorldBootstrap;
import io.github.q93503128.turnbound.world.RewardGrantService;
import io.github.q93503128.turnbound.world.WorldSessionRouter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Lifecycle and routing authority for multi-owner PvE battles. */
final class SharedBattleSessionManager {
    private static final Map<UUID,SharedBattleSession> BY_PARTICIPANT=new LinkedHashMap<>();
    private static final Set<SharedBattleSession> SESSIONS=java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    private SharedBattleSessionManager(){}

    static boolean exists(UUID id){return id!=null&&BY_PARTICIPANT.containsKey(id);}
    static boolean active(ServerPlayer p){SharedBattleSession s=p==null?null:BY_PARTICIPANT.get(p.getUUID());return s!=null&&!s.finished();}
    static boolean finished(ServerPlayer p){SharedBattleSession s=p==null?null:BY_PARTICIPANT.get(p.getUUID());return s!=null&&s.finished();}

    static boolean startEncounterAt(List<ServerPlayer> participants,UUID initiatorId,String encounterId,
                                    boolean autoAllowed,boolean speedAllowed,Vec3 center,float yaw){
        if(participants==null||participants.size()<2||participants.size()>4||initiatorId==null)return false;
        LinkedHashMap<UUID,ServerPlayer> unique=new LinkedHashMap<>();
        for(ServerPlayer p:participants){
            if(p==null||unique.putIfAbsent(p.getUUID(),p)!=null)return false;
            if(exists(p.getUUID())||BattleSessionManager.privateExists(p.getUUID()))return false;
        }
        ServerPlayer initiator=unique.get(initiatorId);
        if(initiator==null||!(initiator.level() instanceof ServerLevel level))return false;
        for(ServerPlayer p:unique.values())if(p.level()!=level)return false;

        BattleArenaLocator.Arena arena=BattleArenaLocator.fixedIfOpen(initiator,center,yaw,unique.size());
        if(arena==null)return false;
        SharedBattleFactory.Blueprint blueprint;
        try{blueprint=SharedBattleFactory.create(new ArrayList<>(unique.keySet()),encounterId);}
        catch(RuntimeException ex){Turnbound.LOGGER.error("TURNBOUND failed to build shared encounter {}",encounterId,ex);return false;}
        boolean fleeAllowed;
        try{fleeAllowed=!CampaignEncounterCatalog.spec(encounterId).boss();}
        catch(RuntimeException ex){return false;}

        for(ServerPlayer p:unique.values())BattleSessionManager.prepareClientTransition(p);
        try{
            SharedBattleSession session=new SharedBattleSession(level,List.copyOf(unique.values()),initiatorId,encounterId,autoAllowed,speedAllowed,fleeAllowed,arena,blueprint);
            SESSIONS.add(session);
            for(UUID id:session.participantIds())BY_PARTICIPANT.put(id,session);
            session.syncAll();
            return true;
        }catch(RuntimeException ex){
            Turnbound.LOGGER.error("TURNBOUND failed to start shared encounter {}",encounterId,ex);
            for(ServerPlayer p:unique.values())try{ExternalWorldBootstrap.refreshAfterBattle(p);}catch(RuntimeException ignored){}
            return false;
        }
    }

    static boolean tick(ServerPlayer p){
        SharedBattleSession s=p==null?null:BY_PARTICIPANT.get(p.getUUID());
        if(s==null)return false;
        s.tick(p);
        if(p.tickCount%5==0)BattleNetwork.sync(p,s);
        return true;
    }

    static boolean command(ServerPlayer p,String command){
        SharedBattleSession s=p==null?null:BY_PARTICIPANT.get(p.getUUID());
        if(s==null)return false;
        if(command==null)return true;
        String[] parts=command.split("\\|",-1);
        switch(parts[0]){
            case "FLEE"->{if(s.finished())requestResultExit(s,p,false);else if(s.fleeAllowed(p.getUUID()))abortRunning(s,p);else BattleNetwork.sync(p,s);}
            case "ACT"->{if(parts.length>=4)s.action(p,parts[1],parts[2],parts[3]);}
            case "FOCUS"->s.focusTarget(p,parts.length>=2?parts[1]:"");
            case "AUTO"->s.toggleAuto(p);
            case "SPEED"->s.toggleSpeed(p);
            default->{}
        }
        return true;
    }

    static boolean resumeIfPresent(ServerPlayer p){SharedBattleSession s=p==null?null:BY_PARTICIPANT.get(p.getUUID());return s!=null&&s.resume(p);}
    static boolean disconnectForLifecycle(ServerPlayer p){SharedBattleSession s=p==null?null:BY_PARTICIPANT.get(p.getUUID());if(s==null)return true;s.disconnect(p);return false;}

    static boolean forceEnd(ServerPlayer p){
        SharedBattleSession s=p==null?null:BY_PARTICIPANT.get(p.getUUID());
        if(s==null)return false;
        if(!s.initiatorId().equals(p.getUUID()))return true;
        abortRunning(s,p);
        return true;
    }

    static void clearAll(Iterable<ServerPlayer> players){
        for(SharedBattleSession s:List.copyOf(SESSIONS)){
            ServerPlayer initiator=s.online(s.initiatorId());
            if(s.finished()&&s.allParticipantsOnline()&&initiator!=null){
                for(UUID id:s.participantIds())s.markReadyToExit(id);
                if(requestResultExit(s,initiator,true))continue;
            }
            try{s.cleanupAllOnline();}catch(RuntimeException ex){Turnbound.LOGGER.error("TURNBOUND failed to clean shared battle during server stop",ex);}
            for(ServerPlayer p:s.onlineParticipants())BattleNetwork.close(p);
            removeSession(s);
        }
        BY_PARTICIPANT.clear();
        SESSIONS.clear();
    }

    private static boolean requestResultExit(SharedBattleSession s,ServerPlayer requester,boolean lifecycle){
        if(!s.finished())return false;
        s.markReadyToExit(requester.getUUID());
        if(!lifecycle&&(!s.allReadyToExit()||!s.allParticipantsOnline())){
            requester.sendSystemMessage(Component.literal("동료의 전투 결과 확인을 기다리고 있습니다."));
            s.syncAll();
            return false;
        }
        if(!s.allParticipantsOnline())return false;

        if(s.state().outcome()==BattleOutcome.ALLY_VICTORY&&!s.encounterId().isBlank()){
            for(ServerPlayer p:s.onlineParticipants()){
                UUID owner=p.getUUID();
                if(s.settled(owner))continue;
                try{
                    RewardGrantService.commitAndSave(p,s.rewardTransactionId(owner),s.encounterId(),s.state(),s.state().outcome());
                    CampaignPersistence.saveIfDirty(p);
                    s.markSettled(owner);
                }catch(RewardGrantService.SettlementException ex){
                    if(lifecycle&&ex.recoverableFromJournal()){
                        s.markSettled(owner);
                        Turnbound.LOGGER.warn("TURNBOUND deferred shared reward transaction {} for {} to durable journal",s.rewardTransactionId(owner),owner);
                        continue;
                    }
                    return settlementFailed(s,p,ex);
                }catch(RuntimeException ex){return settlementFailed(s,p,ex);}
            }
        }else for(ServerPlayer p:s.onlineParticipants())CampaignPersistence.saveIfDirty(p);

        finishAndRestoreField(s);
        return true;
    }

    private static boolean settlementFailed(SharedBattleSession s,ServerPlayer failed,RuntimeException ex){
        Turnbound.LOGGER.error("TURNBOUND failed to settle shared reward transaction {} for {}",s.rewardTransactionId(failed.getUUID()),failed.getUUID(),ex);
        for(ServerPlayer p:s.onlineParticipants()){
            p.sendSystemMessage(Component.literal("TURNBOUND 전투 보상을 안전하게 저장하지 못했습니다. 결과 화면에서 다시 복귀를 시도해 주세요."));
            BattleNetwork.sync(p,s);
        }
        return false;
    }

    private static void finishAndRestoreField(SharedBattleSession s){
        List<ServerPlayer> participants=s.onlineParticipants();
        ServerPlayer initiator=s.online(s.initiatorId());
        BattleOutcome outcome=s.state().outcome();
        String encounterId=s.encounterId();
        try{s.cleanupAllOnline();}catch(RuntimeException ex){Turnbound.LOGGER.error("TURNBOUND failed to clean shared battle after settlement",ex);}
        removeSession(s);
        if(initiator!=null&&!encounterId.isBlank()){
            try{if(!ExternalWorldBootstrap.onBattleEnded(initiator,encounterId,outcome))WorldSessionRouter.onBattleEnded(initiator,encounterId,outcome);}
            catch(RuntimeException ex){Turnbound.LOGGER.error("TURNBOUND failed to restore shared field encounter {}",encounterId,ex);}
        }
        for(ServerPlayer p:participants){
            BattleNetwork.close(p);
            try{ExternalWorldBootstrap.refreshAfterBattle(p);}catch(RuntimeException ex){Turnbound.LOGGER.error("TURNBOUND failed to refresh shared field for {}",p.getUUID(),ex);}
        }
    }

    private static void abortRunning(SharedBattleSession s,ServerPlayer initiator){
        List<ServerPlayer> participants=s.onlineParticipants();
        String encounterId=s.encounterId();
        BattleOutcome outcome=s.state().outcome();
        try{s.cleanupAllOnline();}catch(RuntimeException ex){Turnbound.LOGGER.error("TURNBOUND failed to clean aborted shared battle",ex);}
        removeSession(s);
        if(initiator!=null&&!encounterId.isBlank()){
            try{if(!ExternalWorldBootstrap.onBattleEnded(initiator,encounterId,outcome))WorldSessionRouter.onBattleEnded(initiator,encounterId,outcome);}
            catch(RuntimeException ex){Turnbound.LOGGER.error("TURNBOUND failed to release aborted shared encounter {}",encounterId,ex);}
        }
        for(ServerPlayer p:participants){BattleNetwork.close(p);try{ExternalWorldBootstrap.refreshAfterBattle(p);}catch(RuntimeException ignored){}}
    }

    private static void removeSession(SharedBattleSession s){
        SESSIONS.remove(s);
        for(UUID id:s.participantIds())if(BY_PARTICIPANT.get(id)==s)BY_PARTICIPANT.remove(id);
    }
}

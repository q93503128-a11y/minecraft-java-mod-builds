package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;
import java.util.Set;

/** Durable, player-owned early New Drabyel main-quest state. */
final class DrabyelLocalArcProgress {
    static final String ACCEPTED="DRABYEL_LOCAL_ACCEPTED";
    static final String CRATE="DRABYEL_LOCAL_CRATE";
    static final String SCOUT="DRABYEL_LOCAL_SCOUT";
    static final String RECORD="DRABYEL_LOCAL_RECORD";
    static final String COMPLETE="DRABYEL_LOCAL_COMPLETE";
    private static final Set<String> CLUES=Set.of(CRATE,SCOUT,RECORD);

    private DrabyelLocalArcProgress(){}

    static boolean offerReady(Set<String> clears,Set<String> flags,Set<String> availableRoles){
        if(clears==null||flags==null||availableRoles==null)return false;
        if(!DrabyelOpeningTutorial.patrolCleared(clears)||!flags.contains(DrehmalContextualOnboarding.HUB_MENU_VIEWED))return false;
        if(flags.contains(ACCEPTED)||flags.contains(COMPLETE))return false;
        for(String role:Set.of("BLACKSMITH","MARKET","TRAVEL")){
            if(!flags.contains(DrehmalContextualOnboarding.serviceFlag(role)))return false;
        }
        if(DrehmalContentUnlocks.summonUnlocked(clears)
                &&!flags.contains(DrehmalContextualOnboarding.serviceFlag("SUMMON")))return false;
        return true;
    }

    static boolean available(Set<String> flags){return flags!=null&&(flags.contains(ACCEPTED)||flags.contains(COMPLETE));}
    static boolean active(Set<String> flags){return flags!=null&&flags.contains(ACCEPTED)&&!complete(flags);}
    static int count(Set<String> flags){int n=0;if(flags!=null)for(String clue:CLUES)if(flags.contains(clue))n++;return n;}
    static boolean complete(Set<String> flags){return flags!=null&&(flags.contains(COMPLETE)||count(flags)>=2);}

    static boolean regionalGateReady(Set<String> clears,Set<String> flags){
        if(!complete(flags)||clears==null)return false;
        return clears.contains("CV_DRABYEL_NORTH")
                ||clears.contains(DrehmalContentUnlocks.WARNING_CAVE_ELITE)
                ||clears.contains(DrehmalWorldBossPlacementRules.ENCOUNTER_ID);
    }

    static boolean mark(ServerPlayer player,String flag){
        if(player==null||flag==null||flag.isBlank()||player.level().getServer()==null)return false;
        ExternalWorldSavedData data=ExternalWorldSavedData.get(player.level().getServer());
        if(data.onboardingFlag(player.getUUID(),flag))return false;
        data.markOnboardingFlag(player.getUUID(),flag);return true;
    }

    static boolean reconcile(ServerPlayer player){
        if(player==null||player.level().getServer()==null)return false;
        Set<String> flags=ExternalWorldSavedData.get(player.level().getServer()).onboardingFlags(player.getUUID());
        if(!flags.contains(ACCEPTED)||flags.contains(COMPLETE)||count(flags)<2)return false;
        return mark(player,COMPLETE);
    }
}

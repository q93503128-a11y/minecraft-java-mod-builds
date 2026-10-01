package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Server-authored markers for exact runtime NPC/service positions and simultaneously active quest targets. */
final class OpenworldMapPointService {
    private OpenworldMapPointService() {}

    static List<FieldUiSnapshot.MapPoint> points(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level) || player.level().getServer() == null) return List.of();
        var server = player.level().getServer();
        Set<String> flags = ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());
        Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
        List<FieldUiSnapshot.MapPoint> out = new ArrayList<>();

        if (DrehmalFirstRouteProgress.reached(flags, DrehmalFirstRouteProgress.HUB_REACHED)) {
            for (var service : DrabyelHubAutoPlacement.runtimeServices(level)) {
                if (service.runtimePosition() == null) continue;
                var pos = service.runtimePosition();
                out.add(new FieldUiSnapshot.MapPoint(service.locator(), service.playerLabel(), "SERVICE", pos.x()+0.5D, pos.z()+0.5D, false));
            }
        }
        for (var npc : DrehmalFieldNpcCatalog.all()) {
            if (!fieldNpcVisible(npc, flags)) continue;
            var site = fieldNpcSite(player, npc.siteLocator());
            if (site == null || site.runtimePosition() == null) continue;
            var pos = site.runtimePosition();
            boolean objective = !npc.progressFlag().isBlank()
                    && (npc.progressRequiresFlag().isBlank() || flags.contains(npc.progressRequiresFlag()))
                    && !flags.contains(npc.progressFlag())
                    && (!npc.progressFlag().startsWith("AVSAL_MQ_AV02_")
                    || !flags.contains(AvsalExpansionProgress.INVESTIGATION_COMPLETE))
                    && (!npc.progressFlag().startsWith("DRABYEL_LOCAL_")
                    || !flags.contains(DrabyelLocalArcProgress.COMPLETE));
            out.add(new FieldUiSnapshot.MapPoint(npc.locator(), npc.playerLabel(), objective ? "QUEST" : "NPC",
                    pos.x()+0.5D, pos.z()+0.5D, objective));
        }

        out.addAll(DrabyelLocalArcRuntime.objectMapPoints(player));

        Set<String> production = new LinkedHashSet<>();
        DrehmalAdaptiveRoutePlacement.productionEncounters(player).stream().map(DrehmalFirstRouteCatalog.EncounterSlot::combatEncounterId).filter(id->id!=null&&!id.isBlank()).forEach(production::add);
        AvsalExpansionRuntime.productionEncounters(player).stream().map(DrehmalFirstRouteCatalog.EncounterSlot::combatEncounterId).filter(id->id!=null&&!id.isBlank()).forEach(production::add);

        for (var quest : DrehmalQuestCatalog.all()) {
            if (!DrehmalQuestCatalog.visible(quest, flags, production) || DrehmalQuestCatalog.completed(quest, flags, clears)) continue;
            var target=drehmalQuestTarget(player,quest); if(target==null||target.runtimePosition()==null)continue; var pos=target.runtimePosition();
            out.add(new FieldUiSnapshot.MapPoint("quest:"+quest.id(),quest.title(),quest.kind()==DrehmalQuestCatalog.Kind.HIDDEN?"SECRET":"QUEST",pos.x()+0.5D,pos.z()+0.5D,true));
        }
        for (var quest : AvsalQuestCatalog.all()) {
            if (!AvsalQuestCatalog.visible(quest, flags, production) || AvsalQuestCatalog.completed(quest, flags, clears)) continue;
            if ("MQ_AV04".equals(quest.id())) {
                if (!flags.contains(AvsalExpansionProgress.CLUE_RECORDS)) {
                    var records=AvsalExpansionRuntime.site(player,AvsalExpansionRuntime.RECORDS_CLUE_SITE);
                    if(records!=null&&records.runtimePosition()!=null){
                        var pos=records.runtimePosition();
                        out.add(new FieldUiSnapshot.MapPoint("quest:MQ_AV04:records","낡은 기록","QUEST",pos.x()+0.5D,pos.z()+0.5D,true));
                    }
                }
                continue;
            }
            if ("MQ_AV05".equals(quest.id())) {
                addAvsalQuestPoint(out, player, "quest:MQ_AV05:west", "서부 배전실",
                        AvsalExpansionRuntime.RELAY_WEST_SITE, !flags.contains(AvsalExpansionProgress.RELAY_WEST));
                addAvsalQuestPoint(out, player, "quest:MQ_AV05:guard", "수로 중계기 파수조",
                        AvsalExpansionRuntime.RELAY_GUARD_SITE,
                        !flags.contains(AvsalExpansionProgress.RELAY_GUARD) && !clears.contains("AV_RELAY_GUARD"));
                addAvsalQuestPoint(out, player, "quest:MQ_AV05:east", "동부 중계기",
                        AvsalExpansionRuntime.RELAY_EAST_SITE, !flags.contains(AvsalExpansionProgress.RELAY_EAST));
                continue;
            }
            var target=avsalQuestTarget(player,quest); if(target==null||target.runtimePosition()==null)continue; var pos=target.runtimePosition();
            out.add(new FieldUiSnapshot.MapPoint("quest:"+quest.id(),quest.title(),"QUEST",pos.x()+0.5D,pos.z()+0.5D,true));
        }
        return List.copyOf(out);
    }

    private static void addAvsalQuestPoint(List<FieldUiSnapshot.MapPoint> out, ServerPlayer player,
                                           String id, String label, String siteLocator, boolean unresolved) {
        if (!unresolved) return;
        var site = AvsalExpansionRuntime.site(player, siteLocator);
        if (site == null || site.runtimePosition() == null) return;
        var pos = site.runtimePosition();
        out.add(new FieldUiSnapshot.MapPoint(id, label, "QUEST", pos.x()+0.5D, pos.z()+0.5D, true));
    }

    private static boolean fieldNpcVisible(DrehmalFieldNpcCatalog.Npc npc, Set<String> flags) {
        if (DrabyelLocalArcRuntime.ownsSite(npc.siteLocator())) return DrabyelLocalArcProgress.available(flags);
        if (AvsalExpansionRuntime.ownsSite(npc.siteLocator())) {
            if (!npc.progressRequiresFlag().isBlank()) return flags.contains(npc.progressRequiresFlag());
            return flags.contains(AvsalExpansionProgress.OUTSKIRTS_REACHED);
        }
        if (npc.siteLocator().contains("tower_watch")) return flags.contains(DrehmalFirstRouteProgress.TOWER_REACHED);
        if (npc.siteLocator().contains("camp_explorer")) return flags.contains(DrehmalFirstRouteProgress.CAMP_REACHED);
        return true;
    }

    private static DrehmalFirstRouteCatalog.Site fieldNpcSite(ServerPlayer player,String locator){
        var first=DrehmalAdaptiveRoutePlacement.site(player,locator);
        if(first!=null)return first;
        var local=DrabyelLocalArcRuntime.site(player,locator);
        return local!=null?local:AvsalExpansionRuntime.site(player,locator);
    }

    private static DrehmalFirstRouteCatalog.Site drehmalQuestTarget(ServerPlayer player,DrehmalQuestCatalog.Quest quest){
        if(!quest.encounterId().isBlank())for(var encounter:DrehmalAdaptiveRoutePlacement.productionEncounters(player))if(quest.encounterId().equals(encounter.combatEncounterId()))return DrehmalAdaptiveRoutePlacement.site(player,encounter.siteLocator());
        return switch(quest.completionFlag()){
            case DrehmalFirstRouteProgress.HUB_REACHED->DrehmalAdaptiveRoutePlacement.site(player,"turnbound:site/capital_valley/new_drabyel");
            case DrehmalFirstRouteProgress.TOWER_REACHED->DrehmalAdaptiveRoutePlacement.site(player,"turnbound:site/capital_valley/tower");
            case DrehmalFirstRouteProgress.CAMP_REACHED->DrehmalAdaptiveRoutePlacement.site(player,"turnbound:site/capital_valley/explorer_camp");
            case DrehmalFirstRouteProgress.APPROACH_REACHED->DrehmalAdaptiveRoutePlacement.site(player,"turnbound:site/capital_valley/drabyel_approach");
            default->null;
        };
    }

    private static DrehmalFirstRouteCatalog.Site avsalQuestTarget(ServerPlayer player,AvsalQuestCatalog.Quest quest){
        if(!quest.encounterId().isBlank())for(var encounter:AvsalExpansionRuntime.productionEncounters(player))if(quest.encounterId().equals(encounter.combatEncounterId()))return AvsalExpansionRuntime.site(player,encounter.siteLocator());
        return switch(quest.id()){
            case "MQ_AV01"->AvsalExpansionRuntime.site(player,AvsalExpansionRuntime.ROAD_EVENT_SITE);
            case "MQ_AV02"->AvsalExpansionRuntime.site(player,AvsalExpansionRuntime.ROAD_COURIER_SITE);
            case "MQ_AV03"->AvsalExpansionRuntime.site(player,AvsalExpansionRuntime.OUTSKIRTS_SITE);
            case "MQ_AV07"->AvsalExpansionRuntime.site(player,"turnbound:site/avsal/contract_broker");
            case "SQ_AV04_CART"->AvsalExpansionRuntime.site(player,AvsalExpansionRuntime.WAYSIDE_CACHE_SITE);
            default->null;
        };
    }
}

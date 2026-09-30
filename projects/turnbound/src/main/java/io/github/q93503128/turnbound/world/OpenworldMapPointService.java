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
            var site = DrehmalAdaptiveRoutePlacement.site(player, npc.siteLocator());
            if (site == null || site.runtimePosition() == null) continue;
            var pos = site.runtimePosition();
            out.add(new FieldUiSnapshot.MapPoint(npc.locator(), npc.playerLabel(), "NPC", pos.x()+0.5D, pos.z()+0.5D, false));
        }

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
            var target=avsalQuestTarget(player,quest); if(target==null||target.runtimePosition()==null)continue; var pos=target.runtimePosition();
            out.add(new FieldUiSnapshot.MapPoint("quest:"+quest.id(),quest.title(),"QUEST",pos.x()+0.5D,pos.z()+0.5D,true));
        }
        return List.copyOf(out);
    }

    private static boolean fieldNpcVisible(DrehmalFieldNpcCatalog.Npc npc, Set<String> flags) {
        if (npc.siteLocator().contains("tower_watch")) return flags.contains(DrehmalFirstRouteProgress.TOWER_REACHED);
        if (npc.siteLocator().contains("camp_explorer")) return flags.contains(DrehmalFirstRouteProgress.CAMP_REACHED);
        return true;
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
        return switch(quest.id()){case "MQ_AV01","MQ_AV02"->AvsalExpansionRuntime.site(player,AvsalExpansionRuntime.OUTSKIRTS_SITE);default->null;};
    }
}

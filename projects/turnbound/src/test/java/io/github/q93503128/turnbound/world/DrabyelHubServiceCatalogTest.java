package io.github.q93503128.turnbound.world;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class DrabyelHubServiceCatalogTest {
 @Test void catalogIsValidAndUsesDrabyel(){assertTrue(DrabyelHubServiceCatalog.validate().isEmpty(),()->String.join("; ",DrabyelHubServiceCatalog.validate()));assertEquals(DrehmalWorldProfile.HUB_LOCATOR,DrabyelHubServiceCatalog.hub().hubLocator());assertNotNull(DrehmalWorldProfile.enabled(DrabyelHubServiceCatalog.hub().hubLocator()));}
 @Test void firstHubRolesExistExactlyOnce(){Map<String,Long> roles=DrabyelHubServiceCatalog.hub().services().stream().collect(Collectors.groupingBy(DrabyelHubServiceCatalog.Service::role,Collectors.counting()));for(String role:new String[]{"GREETER","TRAVEL","MARKET","BLACKSMITH","STORY","SUMMON"})assertEquals(1L,roles.getOrDefault(role,0L),role);}
 @Test void surveyGateIsFailClosed(){assertTrue(DrabyelHubServiceCatalog.productionServices().isEmpty());for(var s:DrabyelHubServiceCatalog.hub().services())if(s.productionEnabled()){assertTrue(s.verifiedIn26_2(),s.locator());assertNotNull(s.runtimePosition(),s.locator());assertNotNull(s.runtimeYaw(),s.locator());}}
 @Test void shopRolesUseFinalExternalVisualFamilies(){var market=DrabyelHubServiceCatalog.hub().services().stream().filter(s->s.role().equals("MARKET")).findFirst().orElseThrow();var smith=DrabyelHubServiceCatalog.hub().services().stream().filter(s->s.role().equals("BLACKSMITH")).findFirst().orElseThrow();assertEquals("DRABYEL_MERCHANT",market.visualAsset());assertEquals("DRABYEL_BLACKSMITH",smith.visualAsset());assertEquals("MARKET",market.facilityHint());assertEquals("FORGE",smith.facilityHint());assertFalse(market.productionEnabled());assertFalse(smith.productionEnabled());}
}

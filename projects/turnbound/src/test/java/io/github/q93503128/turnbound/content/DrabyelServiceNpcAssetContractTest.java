package io.github.q93503128.turnbound.content;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DrabyelServiceNpcAssetContractTest {
 @Test void merchantAndBlacksmithShipExternalProductionRigs(){assertNpc("drabyel_merchant","geometry.turnbound.npc_drabyel_merchant");assertNpc("drabyel_blacksmith","geometry.turnbound.npc_drabyel_blacksmith");}
 @Test void sharedNpcMotionHasIdleGreetingAndWork(){JsonObject a=load("assets/turnbound/geckolib/animations/entity/npc/drabyel_service.animation.json").getAsJsonObject("animations");for(String clip:Set.of("field.idle","field.greet","field.work"))assertTrue(a.has(clip),clip);}
 private static void assertNpc(String path,String id){JsonObject g=load("assets/turnbound/geckolib/models/entity/npc/"+path+".geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();assertEquals(id,g.getAsJsonObject("description").get("identifier").getAsString());Set<String>b=new HashSet<>();for(var e:g.getAsJsonArray("bones"))b.add(e.getAsJsonObject().get("name").getAsString());assertTrue(b.contains("RightHandItem"));assertTrue(b.contains("LeftHandItem"));resource("assets/turnbound/textures/entity/npc/"+path+".png");}
 private static void resource(String p){try(InputStream s=DrabyelServiceNpcAssetContractTest.class.getClassLoader().getResourceAsStream(p)){assertNotNull(s,p);assertTrue(s.read()>=0,p);}catch(Exception e){throw new AssertionError(e);}}
 private static JsonObject load(String p){InputStream s=DrabyelServiceNpcAssetContractTest.class.getClassLoader().getResourceAsStream(p);assertNotNull(s,p);try(s;InputStreamReader r=new InputStreamReader(s,StandardCharsets.UTF_8)){return JsonParser.parseReader(r).getAsJsonObject();}catch(Exception e){throw new AssertionError(e);}}
}

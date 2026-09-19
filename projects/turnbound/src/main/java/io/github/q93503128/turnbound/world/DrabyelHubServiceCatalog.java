package io.github.q93503128.turnbound.world;

import com.google.gson.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class DrabyelHubServiceCatalog {
    public static final int SCHEMA_VERSION=1;
    public static final String HUB_LOCATOR=DrehmalWorldProfile.HUB_LOCATOR;
    private static final String RESOURCE="/data/turnbound/world/new_drabyel_services_v1.json";
    private static final Hub HUB=load();
    private static final Set<String> ROLES=Set.of("GREETER","TRAVEL","MARKET","BLACKSMITH","STORY","SUMMON");
    private static final Set<String> HINTS=Set.of("","MAP","MARKET","FORGE","PARTY","ARCHIVE");

    public record Position(int x,int y,int z){}
    public record Service(String locator,String role,String playerLabel,String facilityHint,String zone,String visualAsset,
                          Position runtimePosition,int interactionRadius,boolean verifiedIn26_2,boolean productionEnabled){}
    public record Hub(String hubLocator,List<Service> services){public Hub{services=List.copyOf(services);}}
    private DrabyelHubServiceCatalog(){}

    public static Hub hub(){return HUB;}
    public static Service service(String locator){if(locator==null)return null;for(Service s:HUB.services())if(locator.equals(s.locator()))return s;return null;}
    public static List<Service> productionServices(){return HUB.services().stream().filter(Service::productionEnabled).filter(Service::verifiedIn26_2).filter(s->s.runtimePosition()!=null).toList();}

    public static List<String> validate(){
        List<String> errors=new ArrayList<>();
        if(!HUB_LOCATOR.equals(HUB.hubLocator()))errors.add("unexpected Drabyel hub locator "+HUB.hubLocator());
        if(DrehmalWorldProfile.enabled(HUB.hubLocator())==null)errors.add("Drabyel hub anchor is not enabled");
        Set<String> locators=new HashSet<>(),roles=new HashSet<>();
        for(Service s:HUB.services()){
            if(!locators.add(s.locator()))errors.add("duplicate Drabyel service "+s.locator());
            if(!ROLES.contains(s.role()))errors.add("unknown Drabyel service role "+s.role());
            if(!HINTS.contains(s.facilityHint()))errors.add("unknown Drabyel facility hint "+s.facilityHint());
            if(s.playerLabel().isBlank())errors.add("blank Drabyel service label "+s.locator());
            if(s.zone().isBlank())errors.add("blank Drabyel service zone "+s.locator());
            if(s.visualAsset().isBlank())errors.add("blank Drabyel service visual asset "+s.locator());
            if(s.interactionRadius()<2||s.interactionRadius()>8)errors.add("invalid Drabyel service interaction radius "+s.locator());
            if(!roles.add(s.role()))errors.add("duplicate Drabyel service role "+s.role());
            if(s.productionEnabled()&&(!s.verifiedIn26_2()||s.runtimePosition()==null))errors.add("unverified Drabyel production service "+s.locator());
        }
        for(String required:List.of("GREETER","TRAVEL","MARKET","BLACKSMITH","STORY","SUMMON"))if(!roles.contains(required))errors.add("missing Drabyel service role "+required);
        return List.copyOf(errors);
    }

    private static Hub load(){
        try(InputStream stream=DrabyelHubServiceCatalog.class.getResourceAsStream(RESOURCE)){
            if(stream==null)throw new IllegalStateException("Missing New Drabyel service catalog "+RESOURCE);
            JsonObject root=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            int schema=root.has("schemaVersion")?root.get("schemaVersion").getAsInt():-1;
            if(schema!=SCHEMA_VERSION)throw new IllegalStateException("Unsupported Drabyel service schema "+schema);
            List<Service> services=new ArrayList<>();
            JsonArray array=root.getAsJsonArray("services");if(array==null)throw new IllegalStateException("Missing Drabyel services");
            for(JsonElement element:array){JsonObject raw=element.getAsJsonObject();services.add(new Service(string(raw,"locator"),string(raw,"role"),string(raw,"playerLabel"),
                    optionalString(raw,"facilityHint"),string(raw,"zone"),string(raw,"visualAsset"),position(raw),integer(raw,"interactionRadius",-1),
                    bool(raw,"verifiedIn26_2",false),bool(raw,"productionEnabled",false)));}
            return new Hub(string(root,"hubLocator"),services);
        }catch(Exception ex){if(ex instanceof RuntimeException runtime)throw runtime;throw new IllegalStateException("Failed loading New Drabyel service catalog",ex);}
    }
    private static Position position(JsonObject raw){if(raw==null||!raw.has("position")||!raw.get("position").isJsonObject())return null;JsonObject p=raw.getAsJsonObject("position");if(!p.has("x")||!p.has("y")||!p.has("z"))return null;return new Position(p.get("x").getAsInt(),p.get("y").getAsInt(),p.get("z").getAsInt());}
    private static String string(JsonObject o,String k){String v=optionalString(o,k);if(v.isBlank())throw new IllegalStateException("Missing Drabyel service field "+k);return v;}
    private static String optionalString(JsonObject o,String k){return o!=null&&o.has(k)&&o.get(k).isJsonPrimitive()?o.get(k).getAsString().trim():"";}
    private static int integer(JsonObject o,String k,int f){return o!=null&&o.has(k)&&o.get(k).isJsonPrimitive()?o.get(k).getAsInt():f;}
    private static boolean bool(JsonObject o,String k,boolean f){return o!=null&&o.has(k)&&o.get(k).isJsonPrimitive()?o.get(k).getAsBoolean():f;}
}

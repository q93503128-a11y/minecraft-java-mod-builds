package io.github.q93503128.turnbound.world;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Compact New Drabyel early-game investigation sites. All coordinates are live-world seeds, never terrain writes. */
public final class DrabyelLocalArcCatalog {
    public static final int SCHEMA_VERSION=1;
    private static final String RESOURCE="/data/turnbound/world/drabyel_local_arc_v1.json";
    private static final int HUB_X=530,HUB_Z=1848;
    private static final List<SitePlan> SITES=load();

    public record Point(int x,int z){}
    public record SitePlan(String locator,String kind,String playerLabel,Point seed,int searchRadius,
                           int detectionRadius,String progressFlag,String itemVisual){}

    private DrabyelLocalArcCatalog(){}

    public static List<SitePlan> sites(){return SITES;}
    public static SitePlan site(String locator){
        if(locator==null)return null;
        for(var site:SITES)if(locator.equals(site.locator()))return site;
        return null;
    }

    public static List<String> validate(){
        List<String> errors=new ArrayList<>();
        Set<String> ids=new HashSet<>(),flags=new HashSet<>();
        int npc=0,clue=0;
        for(var site:SITES){
            if(site.locator().isBlank()||!ids.add(site.locator()))errors.add("duplicate/blank local site "+site.locator());
            if(site.playerLabel().isBlank())errors.add("blank local site label "+site.locator());
            if(!flags.add(site.progressFlag()))errors.add("duplicate local progress flag "+site.progressFlag());
            if(site.searchRadius()<4||site.searchRadius()>18)errors.add("invalid local search radius "+site.locator());
            if(site.detectionRadius()<3||site.detectionRadius()>12)errors.add("invalid local detection radius "+site.locator());
            double distance=Math.hypot(site.seed().x()-HUB_X,site.seed().z()-HUB_Z);
            if(distance<64.0D||distance>110.0D)errors.add("local site leaves 64-110 block early-game ring "+site.locator()+" d="+distance);
            if("NPC_ZONE".equals(site.kind())){npc++;if(!site.itemVisual().isBlank())errors.add("npc site has item visual "+site.locator());}
            else if("CLUE_ZONE".equals(site.kind())){
                clue++;
                if(!Set.of("CHEST","WRITABLE_BOOK").contains(site.itemVisual()))errors.add("unsupported local clue visual "+site.locator());
            }else errors.add("unsupported local site kind "+site.locator());
        }
        if(npc!=1||clue!=2)errors.add("local arc must use one NPC clue and two object clues");
        return List.copyOf(errors);
    }

    private static List<SitePlan> load(){
        try(InputStream stream=DrabyelLocalArcCatalog.class.getResourceAsStream(RESOURCE)){
            if(stream==null)throw new IllegalStateException("Missing New Drabyel local arc catalog");
            JsonObject root=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            if(root.get("schemaVersion").getAsInt()!=SCHEMA_VERSION)throw new IllegalStateException("Unsupported local arc schema");
            List<SitePlan> out=new ArrayList<>();
            for(JsonElement element:root.getAsJsonArray("sites")){
                JsonObject raw=element.getAsJsonObject(),seed=raw.getAsJsonObject("seed");
                out.add(new SitePlan(
                        required(raw,"locator"),required(raw,"kind"),required(raw,"playerLabel"),
                        new Point(seed.get("x").getAsInt(),seed.get("z").getAsInt()),
                        raw.get("searchRadius").getAsInt(),raw.get("detectionRadius").getAsInt(),
                        required(raw,"progressFlag"),optional(raw,"itemVisual")));
            }
            return List.copyOf(out);
        }catch(Exception ex){
            if(ex instanceof RuntimeException runtime)throw runtime;
            throw new IllegalStateException("Failed loading New Drabyel local arc catalog",ex);
        }
    }
    private static String required(JsonObject raw,String key){
        String value=optional(raw,key);if(value.isBlank())throw new IllegalStateException("Missing local arc field "+key);return value;
    }
    private static String optional(JsonObject raw,String key){
        return raw!=null&&raw.has(key)&&raw.get(key).isJsonPrimitive()?raw.get(key).getAsString().trim():"";
    }
}

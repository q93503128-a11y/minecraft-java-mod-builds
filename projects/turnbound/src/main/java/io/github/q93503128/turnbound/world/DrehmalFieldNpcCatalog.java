package io.github.q93503128.turnbound.world;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class DrehmalFieldNpcCatalog {
    public static final int SCHEMA_VERSION=1;
    private static final String RESOURCE="/data/turnbound/world/capital_valley_field_npcs_v1.json";
    private static final Set<String> VISUALS=Set.of("DRABYEL_GREETER","DRABYEL_STABLEMASTER","DRABYEL_STORYKEEPER");
    private static final List<Npc> NPCS=load();

    public record Npc(String locator,String siteLocator,String playerLabel,String visualAsset,String dialogue,int interactionRadius,
                      String questOfferFlag,String questOfferId,String questOfferDialogue){}

    private DrehmalFieldNpcCatalog(){}
    public static List<Npc> all(){return NPCS;}
    public static Npc npc(String locator){if(locator==null)return null;for(Npc npc:NPCS)if(locator.equals(npc.locator()))return npc;return null;}

    public static List<String> validate(){
        List<String> errors=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(Npc npc:NPCS){
            if(!ids.add(npc.locator()))errors.add("duplicate field npc "+npc.locator());
            if(DrehmalFirstRouteCatalog.site(npc.siteLocator())==null)errors.add("unknown npc site "+npc.siteLocator());
            if(npc.playerLabel().isBlank()||npc.dialogue().isBlank())errors.add("blank field npc copy "+npc.locator());
            if(!VISUALS.contains(npc.visualAsset()))errors.add("unsupported field npc visual "+npc.visualAsset());
            if(npc.interactionRadius()<2||npc.interactionRadius()>6)errors.add("invalid field npc radius "+npc.locator());
            if(!npc.questOfferFlag().isBlank()&&(npc.questOfferId().isBlank()||npc.questOfferDialogue().isBlank()))errors.add("incomplete field npc quest offer "+npc.locator());
        }
        return List.copyOf(errors);
    }

    private static List<Npc> load(){
        try(InputStream stream=DrehmalFieldNpcCatalog.class.getResourceAsStream(RESOURCE)){
            if(stream==null)throw new IllegalStateException("Missing Capital Valley field NPC catalog");
            JsonObject root=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            if(root.get("schemaVersion").getAsInt()!=SCHEMA_VERSION)throw new IllegalStateException("Unsupported field NPC schema");
            List<Npc> out=new ArrayList<>();
            for(JsonElement element:root.getAsJsonArray("npcs")){
                JsonObject raw=element.getAsJsonObject();
                out.add(new Npc(str(raw,"locator"),str(raw,"siteLocator"),str(raw,"playerLabel"),str(raw,"visualAsset"),str(raw,"dialogue"),
                        raw.get("interactionRadius").getAsInt(),optional(raw,"questOfferFlag"),optional(raw,"questOfferId"),optional(raw,"questOfferDialogue")));
            }
            return List.copyOf(out);
        }catch(Exception ex){
            if(ex instanceof RuntimeException runtime)throw runtime;
            throw new IllegalStateException("Failed loading Capital Valley field NPC catalog",ex);
        }
    }
    private static String optional(JsonObject raw,String key){return raw!=null&&raw.has(key)&&raw.get(key).isJsonPrimitive()?raw.get(key).getAsString().trim():"";}
    private static String str(JsonObject raw,String key){
        String value=raw.get(key).getAsString().trim();if(value.isBlank())throw new IllegalStateException("Blank field NPC field "+key);return value;
    }
}

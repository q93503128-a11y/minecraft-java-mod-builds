package io.github.q93503128.turnbound.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Player-facing passive descriptions. BattleEngine remains the server-authoritative runtime. */
public final class CharacterPassiveCatalog {
    public record Passive(String id, String owner, String name, String description) {}

    private static final Map<String, List<Passive>> BY_OWNER=load();

    private CharacterPassiveCatalog(){}

    public static List<Passive> forOwner(String owner){
        return BY_OWNER.getOrDefault(owner,List.of());
    }

    private static Map<String,List<Passive>> load(){
        String resource="/data/turnbound/passives/v04.json";
        try(InputStream stream=CharacterPassiveCatalog.class.getResourceAsStream(resource)){
            if(stream==null)throw new IllegalStateException("Missing TURNBOUND passive catalog");
            JsonObject root=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            if(root.get("schemaVersion").getAsInt()!=4)throw new IllegalStateException("Invalid TURNBOUND passive schema");
            Map<String,List<Passive>> out=new LinkedHashMap<>();
            for(JsonElement element:root.getAsJsonArray("definitions")){
                JsonObject row=element.getAsJsonObject();
                String owner=text(row,"owner"),description=text(row,"description");
                if(owner.startsWith("P")&&!description.isBlank()){
                    out.computeIfAbsent(owner,ignored->new ArrayList<>()).add(
                            new Passive(text(row,"id"),owner,text(row,"name"),description));
                }
            }
            Map<String,List<Passive>> frozen=new LinkedHashMap<>();
            for(var entry:out.entrySet())frozen.put(entry.getKey(),List.copyOf(entry.getValue()));
            return Map.copyOf(frozen);
        }catch(Exception ex){
            if(ex instanceof RuntimeException runtime)throw runtime;
            throw new IllegalStateException("Failed loading TURNBOUND passive catalog",ex);
        }
    }

    private static String text(JsonObject row,String key){
        return row.has(key)?row.get(key).getAsString():"";
    }
}

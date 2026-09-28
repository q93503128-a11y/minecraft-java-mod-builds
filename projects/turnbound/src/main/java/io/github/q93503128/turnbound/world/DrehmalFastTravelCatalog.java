package io.github.q93503128.turnbound.world;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class DrehmalFastTravelCatalog {
    public static final int SCHEMA_VERSION=1;
    private static final String RESOURCE="/data/turnbound/world/drehmal_fast_travel_v1.json";
    private static final List<Node> NODES=load();

    public record Node(String id,String label,String siteLocator,int mapX,int preferredY,int mapZ,
                       int unlockRadius,int arrivalRadius,float yaw){}

    private DrehmalFastTravelCatalog(){}

    public static List<Node> nodes(){return NODES;}
    public static Node node(String id){
        if(id==null)return null;
        for(Node node:NODES)if(id.equals(node.id()))return node;
        return null;
    }

    public static List<String> validate(){
        List<String> errors=new ArrayList<>();
        Set<String> ids=new HashSet<>(),sites=new HashSet<>();
        for(Node node:NODES){
            if(!ids.add(node.id()))errors.add("duplicate fast travel id "+node.id());
            if(!sites.add(node.siteLocator()))errors.add("duplicate fast travel site "+node.siteLocator());
            if(node.label().isBlank())errors.add("blank fast travel label "+node.id());
            if(DrehmalFirstRouteCatalog.site(node.siteLocator())==null)errors.add("unknown fast travel site "+node.siteLocator());
            if(node.unlockRadius()<16||node.unlockRadius()>128)errors.add("invalid unlock radius "+node.id());
            if(node.arrivalRadius()<8||node.arrivalRadius()>48)errors.add("invalid arrival radius "+node.id());
            if(node.yaw()<-180.0F||node.yaw()>180.0F)errors.add("invalid travel yaw "+node.id());
        }
        return List.copyOf(errors);
    }

    private static List<Node> load(){
        try(InputStream stream=DrehmalFastTravelCatalog.class.getResourceAsStream(RESOURCE)){
            if(stream==null)throw new IllegalStateException("Missing Drehmal fast travel catalog");
            JsonObject root=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            if(root.get("schemaVersion").getAsInt()!=SCHEMA_VERSION)throw new IllegalStateException("Unsupported fast travel schema");
            List<Node> out=new ArrayList<>();
            for(JsonElement element:root.getAsJsonArray("nodes")){
                JsonObject raw=element.getAsJsonObject();
                out.add(new Node(
                        string(raw,"id"),string(raw,"label"),string(raw,"siteLocator"),
                        raw.get("mapX").getAsInt(),raw.get("preferredY").getAsInt(),raw.get("mapZ").getAsInt(),
                        raw.get("unlockRadius").getAsInt(),raw.get("arrivalRadius").getAsInt(),raw.get("yaw").getAsFloat()));
            }
            return List.copyOf(out);
        }catch(Exception ex){
            if(ex instanceof RuntimeException runtime)throw runtime;
            throw new IllegalStateException("Failed loading Drehmal fast travel catalog",ex);
        }
    }

    private static String string(JsonObject raw,String key){
        if(raw==null||!raw.has(key)||!raw.get(key).isJsonPrimitive())throw new IllegalStateException("Missing fast travel field "+key);
        String value=raw.get(key).getAsString().trim();
        if(value.isBlank())throw new IllegalStateException("Blank fast travel field "+key);
        return value;
    }
}

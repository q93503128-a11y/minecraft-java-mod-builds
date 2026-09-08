package kr.moonseungjun.riftfrontier.content;

import com.google.gson.*;
import java.io.Reader;
import java.util.*;

/** Strict decoder for Riftfrontier data-driven content definitions. */
public final class ContentDocumentCodec {
    public CoreDefinition decode(String json) { return decode(JsonParser.parseString(json)); }
    public CoreDefinition decode(Reader reader) { return decode(JsonParser.parseReader(reader)); }
    public CoreDefinition decode(JsonElement element) {
        JsonObject object = requireObject(element, "content definition");
        String kind = requireString(object, "kind");
        ContentId id = ContentId.parse(requireString(object, "id"));
        return switch (kind) {
            case "combat_archetype" -> new CoreDefinition.CombatArchetype(id, stringSet(object, "behaviours"));
            case "region" -> new CoreDefinition.Region(id, requireString(object, "gameplay_rule"), idSet(object, "archetypes"), optionalIdSet(object, "resources"), optionalIdSet(object, "contracts"));
            case "loot_profile" -> new CoreDefinition.LootProfile(id, stringList(object, "pools"));
            case "creature" -> new CoreDefinition.Creature(id, requireId(object, "region"), requireId(object, "archetype"), requireId(object, "loot_profile"), stringSet(object, "behaviours"));
            case "encounter" -> new CoreDefinition.Encounter(id, requireId(object, "region"), idList(object, "participants"), requireString(object, "objective"), requireString(object, "world_consequence"));
            case "expedition_resource" -> new CoreDefinition.ExpeditionResource(id, requireId(object, "region"), requireString(object, "category"), requireInt(object, "carry_weight"), requireInt(object, "field_value"));
            case "contract" -> new CoreDefinition.Contract(id, requireId(object, "region"), requireString(object, "objective"), idIntMap(object, "required_resources"), requireId(object, "reward_loot_profile"), requireId(object, "extraction_result"), requireString(object, "world_consequence"));
            case "extraction_result" -> new CoreDefinition.ExtractionResultProfile(id, requireString(object, "outcome"), requireInt(object, "retained_percent"), requireInt(object, "threat_delta"), requireString(object, "world_consequence"));
            case "attack_pattern" -> new CoreDefinition.AttackPattern(id, requireString(object, "delivery"), requireInt(object, "telegraph_ticks"), requireInt(object, "active_ticks"), requireInt(object, "recovery_ticks"), stringSet(object, "counterplay"), requireString(object, "presentation_cue"));
            case "boss_profile" -> new CoreDefinition.BossProfile(id, requireInt(object, "phase_count"), idSet(object, "attack_patterns"), requireString(object, "arena_rule"));
            default -> throw new IllegalArgumentException("Unknown content kind '" + kind + "' for " + id);
        };
    }
    private static ContentId requireId(JsonObject o,String k){return ContentId.parse(requireString(o,k));}
    private static String requireString(JsonObject o,String k){JsonElement v=o.get(k);if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Missing or non-string field '"+k+"'");String t=v.getAsString().trim();if(t.isEmpty())throw new IllegalArgumentException("Field '"+k+"' must not be blank");return t;}
    private static int requireInt(JsonObject o,String k){JsonElement v=o.get(k);if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Missing or non-numeric field '"+k+"'");try{return v.getAsInt();}catch(RuntimeException e){throw new IllegalArgumentException("Field '"+k+"' must be an integer",e);}}
    private static List<String> stringList(JsonObject o,String k){JsonArray a=requireArray(o,k);List<String> r=new ArrayList<>(a.size());for(JsonElement e:a){if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Field '"+k+"' must contain strings only");String v=e.getAsString().trim();if(v.isEmpty())throw new IllegalArgumentException("Field '"+k+"' contains a blank value");r.add(v);}return List.copyOf(r);}
    private static Set<String> stringSet(JsonObject o,String k){List<String> l=stringList(o,k);Set<String> s=new LinkedHashSet<>(l);if(s.size()!=l.size())throw new IllegalArgumentException("Field '"+k+"' contains duplicate values");return Set.copyOf(s);}
    private static List<ContentId> idList(JsonObject o,String k){return stringList(o,k).stream().map(ContentId::parse).toList();}
    private static Set<ContentId> idSet(JsonObject o,String k){List<ContentId> l=idList(o,k);Set<ContentId>s=new LinkedHashSet<>(l);if(s.size()!=l.size())throw new IllegalArgumentException("Field '"+k+"' contains duplicate content ids");return Set.copyOf(s);}
    private static Set<ContentId> optionalIdSet(JsonObject o,String k){return o.has(k)?idSet(o,k):Set.of();}
    private static Map<ContentId,Integer> idIntMap(JsonObject o,String k){JsonElement v=o.get(k);if(v==null||!v.isJsonObject())throw new IllegalArgumentException("Missing or non-object field '"+k+"'");Map<ContentId,Integer>r=new LinkedHashMap<>();for(Map.Entry<String,JsonElement>e:v.getAsJsonObject().entrySet()){ContentId id=ContentId.parse(e.getKey());JsonElement a=e.getValue();if(!a.isJsonPrimitive()||!a.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Field '"+k+"' amount for "+id+" must be numeric");int c=a.getAsInt();if(c<=0)throw new IllegalArgumentException("Field '"+k+"' amount for "+id+" must be > 0");if(r.putIfAbsent(id,c)!=null)throw new IllegalArgumentException("Field '"+k+"' contains duplicate id "+id);}return Map.copyOf(r);}
    private static JsonArray requireArray(JsonObject o,String k){JsonElement v=o.get(k);if(v==null||!v.isJsonArray())throw new IllegalArgumentException("Missing or non-array field '"+k+"'");return v.getAsJsonArray();}
    private static JsonObject requireObject(JsonElement e,String l){if(e==null||!e.isJsonObject())throw new IllegalArgumentException(l+" must be a JSON object");return e.getAsJsonObject();}
}

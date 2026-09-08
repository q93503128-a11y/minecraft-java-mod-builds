package kr.moonseungjun.riftfrontier.content;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ContentValidator {
    public enum Severity { ERROR, WARN }
    public enum Code {
        EMPTY_BEHAVIOUR_SET, MISSING_UNIQUE_GAMEPLAY_RULE, NO_REGION_ARCHETYPES, NO_REGION_RESOURCES,
        NO_REGION_CONTRACTS, EMPTY_LOOT_POOLS, MISSING_REFERENCE, EMPTY_ENCOUNTER_PARTICIPANTS,
        EMPTY_ENCOUNTER_OBJECTIVE, EMPTY_CONTRACT_OBJECTIVE, EMPTY_CONTRACT_REQUIREMENTS,
        INVALID_RESOURCE_CATEGORY, INVALID_EXTRACTION_OUTCOME, NO_WORLD_CONSEQUENCE,
        EMPTY_ATTACK_COUNTERPLAY, INVALID_ATTACK_DELIVERY, INVALID_ATTACK_PRESENTATION_CUE,
        NO_BOSS_ATTACK_PATTERNS, INVALID_BOSS_ARENA_RULE
    }
    public record Issue(Code code, Severity severity, ContentId source, String message) { }
    public record Report(int definitionCount, List<Issue> issues) {
        public Report { issues = List.copyOf(issues); }
        public boolean hasErrors(){return issues.stream().anyMatch(i->i.severity()==Severity.ERROR);}
        public List<Issue> warnings(){return issues.stream().filter(i->i.severity()==Severity.WARN).toList();}
        public List<Issue> byCode(Code code){return issues.stream().filter(i->i.code()==code).toList();}
        public String format(){return issues.stream().map(i->i.severity()+" ["+i.code()+"] "+i.source()+" - "+i.message()).reduce((a,b)->a+"\n"+b).orElse("OK");}
    }
    public Report validate(ContentRegistry registry) {
        List<Issue> issues=new ArrayList<>();
        for(CoreDefinition definition:registry.all()){
            switch(definition){
                case CoreDefinition.CombatArchetype a->{if(a.behaviours().isEmpty())error(issues,Code.EMPTY_BEHAVIOUR_SET,a.id(),"combat archetype has no behaviours");}
                case CoreDefinition.Region r->{if(r.gameplayRule().isBlank())error(issues,Code.MISSING_UNIQUE_GAMEPLAY_RULE,r.id(),"region has no unique gameplay rule");if(r.archetypes().isEmpty())warn(issues,Code.NO_REGION_ARCHETYPES,r.id(),"region has no combat archetypes yet");if(r.resources().isEmpty())warn(issues,Code.NO_REGION_RESOURCES,r.id(),"region has no expedition resources yet");if(r.contracts().isEmpty())warn(issues,Code.NO_REGION_CONTRACTS,r.id(),"region has no contracts yet");for(ContentId id:r.archetypes())require(registry,issues,r.id(),CoreDefinition.Kind.COMBAT_ARCHETYPE,id);for(ContentId id:r.resources())require(registry,issues,r.id(),CoreDefinition.Kind.EXPEDITION_RESOURCE,id);for(ContentId id:r.contracts())require(registry,issues,r.id(),CoreDefinition.Kind.CONTRACT,id);}
                case CoreDefinition.LootProfile l->{if(l.pools().isEmpty())warn(issues,Code.EMPTY_LOOT_POOLS,l.id(),"loot profile has no pools");}
                case CoreDefinition.Creature c->{require(registry,issues,c.id(),CoreDefinition.Kind.REGION,c.region());require(registry,issues,c.id(),CoreDefinition.Kind.COMBAT_ARCHETYPE,c.archetype());require(registry,issues,c.id(),CoreDefinition.Kind.LOOT_PROFILE,c.lootProfile());if(c.behaviours().isEmpty())error(issues,Code.EMPTY_BEHAVIOUR_SET,c.id(),"creature has no behaviours");}
                case CoreDefinition.Encounter e->{require(registry,issues,e.id(),CoreDefinition.Kind.REGION,e.region());if(e.participants().isEmpty())error(issues,Code.EMPTY_ENCOUNTER_PARTICIPANTS,e.id(),"encounter has no participants");for(ContentId id:e.participants())require(registry,issues,e.id(),CoreDefinition.Kind.CREATURE,id);if(e.objective().isBlank())error(issues,Code.EMPTY_ENCOUNTER_OBJECTIVE,e.id(),"encounter has no objective");if(e.worldConsequence().isBlank())warn(issues,Code.NO_WORLD_CONSEQUENCE,e.id(),"encounter has no world consequence");}
                case CoreDefinition.ExpeditionResource r->{require(registry,issues,r.id(),CoreDefinition.Kind.REGION,r.region());if(r.category().isBlank())error(issues,Code.INVALID_RESOURCE_CATEGORY,r.id(),"expedition resource category is blank");}
                case CoreDefinition.Contract c->{require(registry,issues,c.id(),CoreDefinition.Kind.REGION,c.region());require(registry,issues,c.id(),CoreDefinition.Kind.LOOT_PROFILE,c.rewardLootProfile());require(registry,issues,c.id(),CoreDefinition.Kind.EXTRACTION_RESULT,c.extractionResult());if(c.objective().isBlank())error(issues,Code.EMPTY_CONTRACT_OBJECTIVE,c.id(),"contract has no objective");if(c.requiredResources().isEmpty())warn(issues,Code.EMPTY_CONTRACT_REQUIREMENTS,c.id(),"contract has no resource requirements");for(ContentId id:c.requiredResources().keySet())require(registry,issues,c.id(),CoreDefinition.Kind.EXPEDITION_RESOURCE,id);if(c.worldConsequence().isBlank())warn(issues,Code.NO_WORLD_CONSEQUENCE,c.id(),"contract has no world consequence");}
                case CoreDefinition.ExtractionResultProfile e->{if(e.outcome().isBlank())error(issues,Code.INVALID_EXTRACTION_OUTCOME,e.id(),"extraction result outcome is blank");if(e.worldConsequence().isBlank())warn(issues,Code.NO_WORLD_CONSEQUENCE,e.id(),"extraction result has no world consequence");}
                case CoreDefinition.AttackPattern a->{if(a.delivery().isBlank())error(issues,Code.INVALID_ATTACK_DELIVERY,a.id(),"attack pattern delivery is blank");if(a.counterplay().isEmpty())error(issues,Code.EMPTY_ATTACK_COUNTERPLAY,a.id(),"attack pattern exposes no player counterplay");if(a.presentationCue().isBlank())error(issues,Code.INVALID_ATTACK_PRESENTATION_CUE,a.id(),"attack pattern has no presentation cue contract");}
                case CoreDefinition.BossProfile b->{if(b.attackPatterns().isEmpty())error(issues,Code.NO_BOSS_ATTACK_PATTERNS,b.id(),"boss profile has no attack patterns");for(ContentId id:b.attackPatterns())require(registry,issues,b.id(),CoreDefinition.Kind.ATTACK_PATTERN,id);if(b.arenaRule().isBlank())error(issues,Code.INVALID_BOSS_ARENA_RULE,b.id(),"boss profile has no arena rule");}
            }
        }
        issues.sort(Comparator.comparing((Issue i)->i.severity().ordinal()).thenComparing(i->i.source().toString()).thenComparing(i->i.code().name()).thenComparing(Issue::message));
        return new Report(registry.size(),issues);
    }
    private static void require(ContentRegistry registry,List<Issue> issues,ContentId source,CoreDefinition.Kind kind,ContentId target){if(!registry.contains(kind,target))error(issues,Code.MISSING_REFERENCE,source,"missing "+kind+" reference: "+target);}
    private static void error(List<Issue> issues,Code code,ContentId id,String message){issues.add(new Issue(code,Severity.ERROR,id,message));}
    private static void warn(List<Issue> issues,Code code,ContentId id,String message){issues.add(new Issue(code,Severity.WARN,id,message));}
}

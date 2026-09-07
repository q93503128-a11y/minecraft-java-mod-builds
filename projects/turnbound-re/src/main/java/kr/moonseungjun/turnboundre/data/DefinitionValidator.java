package kr.moonseungjun.turnboundre.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Fail-fast semantic validation shared by JSON reload and unit tests. */
public final class DefinitionValidator {
    private static final Set<String> ACTION_KINDS = Set.of("BASIC", "SKILL", "GUARD", "BURST", "PASSIVE");
    private static final Set<String> TARGET_TEAMS = Set.of("SELF", "ALLY", "ENEMY", "ANY");
    private static final Set<String> TARGET_SHAPES = Set.of("SINGLE", "MULTI");
    private static final Set<String> ROLES = Set.of("VANGUARD", "BREAKER", "STRIKER", "SUPPORT", "CONTROLLER");
    private static final Set<String> DAMAGE_TAGS = Set.of("MELEE", "PROJECTILE", "FIRE", "BLAST", "ARCANE", "VOID");
    private static final Set<String> AFFINITY_GRADES = Set.of("WEAK", "NORMAL", "RESIST", "IMMUNE");
    private static final Set<String> STATUS_POLARITIES = Set.of("POSITIVE", "NEGATIVE", "NEUTRAL");
    private static final Set<String> STATUS_DURATION_UNITS = Set.of("TURN", "CYCLE");
    private static final Set<String> REFRESH_RULES = Set.of("REFRESH_DURATION", "ADD_STACK", "REPLACE", "IGNORE");
    private static final Set<String> ACTION_EFFECTS = Set.of(
            "DAMAGE", "HEAL", "APPLY_STATUS", "REMOVE_STATUS", "ENERGY", "POISE_DAMAGE", "INTENT_DELAY", "NONE");
    private static final Set<String> STATUS_EFFECTS = Set.of(
            "DAMAGE_MAX_HP_PERCENT", "DAMAGE_FLAT", "HEAL_MAX_HP_PERCENT", "ATK_MULTIPLIER", "DEF_MULTIPLIER",
            "SPD_MULTIPLIER", "DAMAGE_TAKEN_MULTIPLIER", "POISE_TAKEN_MULTIPLIER", "NONE");
    private static final Set<String> HOOK_WHENS = Set.of("ON_APPLY", "TURN_START", "TURN_END", "CYCLE_START", "ON_ACTION", "ON_DAMAGE_TAKEN");
    private static final Set<String> REWARD_TYPES = Set.of("COIN", "ESSENCE", "CHARACTER_SHARD");

    private DefinitionValidator() {}

    public static List<String> validateActions(List<ActionDefinition> actions) {
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (actions == null) return List.of("actions must not be null");
        for (ActionDefinition action : actions) {
            if (action == null) { errors.add("action must not be null"); continue; }
            String id = action.id();
            if (!validId(id)) errors.add("invalid action id: " + id);
            else if (!seen.add(id)) errors.add("duplicate action id: " + id);
            if (!ACTION_KINDS.contains(action.kind())) errors.add(id + ": unknown action kind " + action.kind());
            if (action.energyDelta() < -100 || action.energyDelta() > 100) errors.add(id + ": energyDelta must be -100..100");
            if (action.hpPower() < 0) errors.add(id + ": hpPower must be >= 0");
            if (action.poisePower() < 0) errors.add(id + ": poisePower must be >= 0");
            if (!DAMAGE_TAGS.contains(action.damageTag())) errors.add(id + ": unknown damageTag " + action.damageTag());
            if (("SKILL".equals(action.kind()) || "BURST".equals(action.kind())) && action.energyDelta() > 0) {
                errors.add(id + ": " + action.kind() + " cannot generate Energy");
            }
            if (("BASIC".equals(action.kind()) || "GUARD".equals(action.kind())) && action.energyDelta() < 0) {
                errors.add(id + ": " + action.kind() + " cannot consume Energy");
            }
            if ("PASSIVE".equals(action.kind()) && action.energyDelta() != 0) errors.add(id + ": PASSIVE energyDelta must be 0");

            ActionDefinition.Targeting targeting = action.targeting();
            if (targeting == null) {
                errors.add(id + ": targeting must not be null");
            } else {
                if (!TARGET_TEAMS.contains(targeting.team())) errors.add(id + ": unknown targeting team " + targeting.team());
                if (!TARGET_SHAPES.contains(targeting.shape())) errors.add(id + ": unknown targeting shape " + targeting.shape());
                if (targeting.count() < 1) errors.add(id + ": targeting count must be >= 1");
                if ("SINGLE".equals(targeting.shape()) && targeting.count() != 1) errors.add(id + ": SINGLE targeting count must be 1");
            }

            for (ActionDefinition.Effect effect : action.effects()) {
                if (effect == null) { errors.add(id + ": null effect"); continue; }
                if (!ACTION_EFFECTS.contains(effect.type())) errors.add(id + ": unknown effect type " + effect.type());
                if (!Double.isFinite(effect.value())) errors.add(id + ": effect value must be finite");
                if (effect.duration() < 0) errors.add(id + ": effect duration must be >= 0");
                if (!Double.isFinite(effect.chance()) || effect.chance() < 0 || effect.chance() > 1) {
                    errors.add(id + ": effect chance must be finite in [0,1]");
                }
                if (("APPLY_STATUS".equals(effect.type()) || "REMOVE_STATUS".equals(effect.type())) && !validId(effect.status())) {
                    errors.add(id + ": status effect requires namespaced status id");
                }
            }
        }
        return List.copyOf(errors);
    }

    public static List<String> validateStatuses(List<StatusDefinition> statuses) {
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (statuses == null) return List.of("statuses must not be null");
        for (StatusDefinition status : statuses) {
            if (status == null) { errors.add("status must not be null"); continue; }
            String id = status.id();
            if (!validId(id)) errors.add("invalid status id: " + id);
            else if (!seen.add(id)) errors.add("duplicate status id: " + id);
            if (!STATUS_POLARITIES.contains(status.polarity())) errors.add(id + ": unknown polarity " + status.polarity());
            if (!STATUS_DURATION_UNITS.contains(status.durationUnit())) errors.add(id + ": unknown durationUnit " + status.durationUnit());
            if (status.baseDuration() < 1) errors.add(id + ": baseDuration must be >= 1");
            if (status.maxStacks() < 1) errors.add(id + ": maxStacks must be >= 1");
            if (!REFRESH_RULES.contains(status.refreshRule())) errors.add(id + ": unknown refreshRule " + status.refreshRule());
            for (String tag : status.dispelTags()) if (tag == null || tag.isBlank()) errors.add(id + ": blank dispelTag");
            for (StatusDefinition.Hook hook : status.hooks()) {
                if (hook == null || hook.effect() == null) { errors.add(id + ": null hook/effect"); continue; }
                if (!HOOK_WHENS.contains(hook.when())) errors.add(id + ": unknown hook timing " + hook.when());
                if (!STATUS_EFFECTS.contains(hook.effect().type())) errors.add(id + ": unknown status effect " + hook.effect().type());
                if (!Double.isFinite(hook.effect().value())) errors.add(id + ": status effect value must be finite");
            }
        }
        return List.copyOf(errors);
    }

    public static List<String> validateCharacters(List<CharacterDefinition> characters, Set<String> actionIds) {
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (characters == null) return List.of("characters must not be null");
        for (CharacterDefinition character : characters) {
            if (character == null) { errors.add("character must not be null"); continue; }
            String id = character.id();
            if (!validId(id)) errors.add("invalid character id: " + id);
            else if (!seen.add(id)) errors.add("duplicate character id: " + id);
            if (character.sourceEntity() != null && !character.sourceEntity().isBlank() && !validId(character.sourceEntity())) {
                errors.add(id + ": invalid sourceEntity " + character.sourceEntity());
            }
            if (character.originStar() < 1 || character.originStar() > 5) errors.add(id + ": originStar must be 1..5");
            if (character.squadCost() < 1 || character.squadCost() > 12) errors.add(id + ": squadCost must be 1..12");
            if (character.roles().isEmpty()) errors.add(id + ": roles must not be empty");
            for (String role : character.roles()) if (!ROLES.contains(role)) errors.add(id + ": unknown role " + role);
            validateStats(id + ": baseStats", character.baseStats(), false, errors);
            validateGrowth(id, character.growth(), errors);
            for (Map.Entry<String, CharacterDefinition.Stats> entry : character.ascensionFlat().entrySet()) {
                int star;
                try { star = Integer.parseInt(entry.getKey()); }
                catch (NumberFormatException ignored) { errors.add(id + ": ascensionFlat key must be star 2..6: " + entry.getKey()); continue; }
                if (star < 2 || star > 6) errors.add(id + ": ascensionFlat star must be 2..6: " + star);
                validateStats(id + ": ascensionFlat[" + star + "]", entry.getValue(), true, errors);
            }
            if (!character.affinities().keySet().equals(DAMAGE_TAGS)) {
                errors.add(id + ": affinities must define exactly " + DAMAGE_TAGS);
            }
            for (Map.Entry<String, String> affinity : character.affinities().entrySet()) {
                if (!DAMAGE_TAGS.contains(affinity.getKey())) errors.add(id + ": unknown affinity tag " + affinity.getKey());
                if (!AFFINITY_GRADES.contains(affinity.getValue())) errors.add(id + ": invalid affinity grade " + affinity.getValue());
            }
            for (String action : character.actions()) if (!actionIds.contains(action)) errors.add(id + ": missing action " + action);
            if (!validId(character.basicAction())) errors.add(id + ": invalid basicAction id");
            if (!validId(character.burst())) errors.add(id + ": invalid burst id");
            if (character.skills().isEmpty()) errors.add(id + ": at least one skill is required");
            if (character.availability() == null || character.availability().type() == null || character.availability().type().isBlank()) {
                errors.add(id + ": availability type required");
            }
            if (!validId(character.presentationKey())) errors.add(id + ": invalid presentationKey " + character.presentationKey());
        }
        return List.copyOf(errors);
    }

    public static List<String> validateEncounters(
            List<EncounterDefinition> encounters,
            Map<String, CharacterDefinition> characters,
            Set<String> rewardIds
    ) {
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (EncounterDefinition encounter : encounters) {
            String id = encounter.id();
            if (!validId(id)) errors.add("invalid encounter id: " + id);
            else if (!seen.add(id)) errors.add("duplicate encounter id: " + id);
            if (encounter.difficulty() < 1) errors.add(id + ": difficulty must be >= 1");
            if (encounter.enemies().isEmpty()) errors.add(id + ": enemies must not be empty");
            for (EncounterDefinition.EnemySlot slot : encounter.enemies()) {
                CharacterDefinition character = characters.get(slot.character());
                if (character == null) { errors.add(id + ": missing enemy character " + slot.character()); continue; }
                if (slot.currentStar() < character.originStar() || slot.currentStar() > 6) {
                    errors.add(id + ": currentStar invalid for " + slot.character());
                }
                int cap = levelCap(slot.currentStar());
                if (slot.level() < 1 || slot.level() > cap) errors.add(id + ": level must be 1.." + cap + " for " + slot.character());
            }
            if (!rewardIds.contains(encounter.rewardTable())) errors.add(id + ": missing reward table " + encounter.rewardTable());
            if (!validId(encounter.sceneKey())) errors.add(id + ": invalid sceneKey " + encounter.sceneKey());
        }
        return List.copyOf(errors);
    }

    public static List<String> validateRewards(List<RewardTableDefinition> rewards, Set<String> characterIds) {
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (RewardTableDefinition table : rewards) {
            String id = table.id();
            if (!validId(id)) errors.add("invalid reward id: " + id);
            else if (!seen.add(id)) errors.add("duplicate reward id: " + id);
            if (table.rolls().isEmpty()) errors.add(id + ": reward rolls must not be empty");
            for (RewardTableDefinition.Roll roll : table.rolls()) {
                if (!REWARD_TYPES.contains(roll.type())) errors.add(id + ": unknown reward type " + roll.type());
                if (roll.min() < 0 || roll.max() < roll.min()) errors.add(id + ": invalid reward range " + roll.min() + ".." + roll.max());
                if (roll.weight() < 1) errors.add(id + ": weight must be >= 1");
                if (!Double.isFinite(roll.chance()) || roll.chance() < 0 || roll.chance() > 1) errors.add(id + ": chance must be in [0,1]");
                if ("CHARACTER_SHARD".equals(roll.type()) && !characterIds.contains(roll.character())) {
                    errors.add(id + ": CHARACTER_SHARD requires known character " + roll.character());
                }
            }
        }
        return List.copyOf(errors);
    }

    private static void validateStats(String prefix, CharacterDefinition.Stats stats, boolean allowZeroHpPoise, List<String> errors) {
        if (stats == null) { errors.add(prefix + " required"); return; }
        if (allowZeroHpPoise ? stats.hp() < 0 : stats.hp() <= 0) errors.add(prefix + " hp invalid");
        if (stats.atk() < 0 || stats.def() < 0 || stats.spd() < 0) errors.add(prefix + " atk/def/spd must be >= 0");
        if (allowZeroHpPoise ? stats.poise() < 0 : stats.poise() <= 0) errors.add(prefix + " poise invalid");
    }

    private static void validateGrowth(String id, CharacterDefinition.Growth growth, List<String> errors) {
        if (growth == null) { errors.add(id + ": growth required"); return; }
        for (double value : List.of(growth.hp(), growth.atk(), growth.def(), growth.spd(), growth.poise())) {
            if (!Double.isFinite(value) || value < 0) { errors.add(id + ": growth values must be finite and >= 0"); break; }
        }
    }

    private static int levelCap(int star) {
        return switch (star) {
            case 1 -> 20; case 2 -> 30; case 3 -> 40; case 4 -> 50; case 5 -> 60; case 6 -> 70;
            default -> 0;
        };
    }

    private static boolean validId(String value) {
        if (value == null) return false;
        int colon = value.indexOf(':');
        if (colon <= 0 || colon == value.length() - 1 || colon != value.lastIndexOf(':')) return false;
        return validPart(value.substring(0, colon), false) && validPart(value.substring(colon + 1), true);
    }

    private static boolean validPart(String part, boolean path) {
        if (part.isBlank()) return false;
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.' || (path && c == '/');
            if (!ok) return false;
        }
        return true;
    }
}

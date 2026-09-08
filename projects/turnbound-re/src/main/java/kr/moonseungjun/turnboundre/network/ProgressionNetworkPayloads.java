package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.data.ActionDefinition;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.ProgressionDefinition;
import kr.moonseungjun.turnboundre.progression.CharacterProgress;
import kr.moonseungjun.turnboundre.progression.PlayerProgress;
import kr.moonseungjun.turnboundre.progression.PlayerProgressStore;
import kr.moonseungjun.turnboundre.progression.ProgressionRules;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Server-authored progression presentation and write-intent wire contracts. */
public final class ProgressionNetworkPayloads {
    private static final int MAX_WIRE_CHARS = 96_000;
    private static final Base64.Encoder B64E = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private ProgressionNetworkPayloads() {}

    public record StatsView(int hp, int atk, int def, int spd, int poise) {
        public StatsView {
            if (hp < 0 || atk < 0 || def < 0 || spd < 0 || poise < 0) {
                throw new IllegalArgumentException("stats must be >= 0");
            }
        }
    }

    public record CostView(long coin, long essence, int shards) {
        public static final CostView ZERO = new CostView(0, 0, 0);

        public CostView {
            if (coin < 0 || essence < 0 || shards < 0) throw new IllegalArgumentException("cost must be >= 0");
        }
    }

    public record EffectView(String type, String status, double value, int duration, double chance) {
        public EffectView {
            type = type == null ? "" : type;
            status = status == null ? "" : status;
            if (!Double.isFinite(value) || !Double.isFinite(chance)) throw new IllegalArgumentException("effect numbers must be finite");
        }
    }

    public record ActionView(
            String id,
            String kind,
            int energyDelta,
            int hpPower,
            int poisePower,
            String damageTag,
            String targetTeam,
            String targetShape,
            int targetCount,
            List<EffectView> effects
    ) {
        public ActionView {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("action id required");
            kind = kind == null ? "" : kind;
            damageTag = damageTag == null ? "" : damageTag;
            targetTeam = targetTeam == null ? "" : targetTeam;
            targetShape = targetShape == null ? "" : targetShape;
            if (targetCount < 0) throw new IllegalArgumentException("targetCount must be >= 0");
            effects = effects == null ? List.of() : List.copyOf(effects);
        }
    }

    /** All preview values below are authored by the server from the same progression formulas used for writes. */
    public record GrowthView(
            boolean owned,
            int shardBalance,
            int nextLevel,
            StatsView nextLevelStats,
            CostView levelCost,
            String levelBlockCode,
            int nextStar,
            int nextLevelCap,
            StatsView nextStarStats,
            CostView ascendCost,
            String ascendBlockCode
    ) {
        public GrowthView {
            if (shardBalance < 0 || nextLevel < 0 || nextStar < 1 || nextStar > 6 || nextLevelCap < 1) {
                throw new IllegalArgumentException("invalid growth preview");
            }
            if (nextLevelStats == null || levelCost == null || nextStarStats == null || ascendCost == null) {
                throw new IllegalArgumentException("growth preview values required");
            }
            levelBlockCode = levelBlockCode == null ? "" : levelBlockCode;
            ascendBlockCode = ascendBlockCode == null ? "" : ascendBlockCode;
        }

        public boolean canLevelUp() { return owned && levelBlockCode.isBlank(); }
        public boolean canAscend() { return owned && ascendBlockCode.isBlank(); }
    }

    public record CharacterView(
            String id,
            boolean owned,
            int originStar,
            int currentStar,
            int level,
            int levelCap,
            int squadCost,
            List<String> roles,
            int hp,
            int atk,
            int def,
            int spd,
            int poise,
            List<String> affinities,
            String basicAction,
            List<String> skills,
            String burst,
            List<String> passives,
            List<ActionView> actions,
            GrowthView growth
    ) {
        public CharacterView {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("character id required");
            if (originStar < 1 || originStar > 5) throw new IllegalArgumentException("originStar must be 1..5");
            if (currentStar < originStar || currentStar > 6) throw new IllegalArgumentException("currentStar invalid");
            if (level < 0 || level > levelCap) throw new IllegalArgumentException("level invalid");
            if (owned && level < 1) throw new IllegalArgumentException("owned character requires level >= 1");
            if (squadCost < 0) throw new IllegalArgumentException("squadCost must be >= 0");
            roles = roles == null ? List.of() : List.copyOf(roles);
            affinities = affinities == null ? List.of() : List.copyOf(affinities);
            skills = skills == null ? List.of() : List.copyOf(skills);
            passives = passives == null ? List.of() : List.copyOf(passives);
            actions = actions == null ? List.of() : List.copyOf(actions);
            basicAction = basicAction == null ? "" : basicAction;
            burst = burst == null ? "" : burst;
            if (growth == null) throw new IllegalArgumentException("growth view required");
        }

        public Optional<ActionView> action(String actionId) {
            return actions.stream().filter(action -> action.id().equals(actionId)).findFirst();
        }
    }

    public record Snapshot(
            long coin,
            long essence,
            int partyCapacity,
            List<String> party,
            List<CharacterView> characters,
            String resultCode,
            String resultDetail
    ) {
        public Snapshot {
            if (coin < 0 || essence < 0 || partyCapacity < 1) throw new IllegalArgumentException("invalid progression snapshot");
            party = party == null ? List.of() : List.copyOf(party);
            characters = characters == null ? List.of() : List.copyOf(characters);
            resultCode = resultCode == null ? "" : resultCode;
            resultDetail = resultDetail == null ? "" : resultDetail;
        }

        public Optional<CharacterView> character(String id) {
            return characters.stream().filter(character -> character.id().equals(id)).findFirst();
        }
    }

    public record DecodedSetParty(List<String> expectedParty, List<String> requestedParty) {
        public DecodedSetParty {
            expectedParty = expectedParty == null ? List.of() : List.copyOf(expectedParty);
            requestedParty = requestedParty == null ? List.of() : List.copyOf(requestedParty);
        }
    }

    public record DecodedGrowth(String operation, String characterId, int expectedStar, int expectedLevel) {
        public DecodedGrowth {
            operation = operation == null ? "" : operation;
            characterId = characterId == null ? "" : characterId;
        }
    }

    public record RequestProgressC2S(String wire) implements CustomPacketPayload {
        public static final Type<RequestProgressC2S> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "progress_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestProgressC2S> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, RequestProgressC2S::wire, RequestProgressC2S::new);

        public RequestProgressC2S {
            wire = checkedWire(wire);
            if (!"request".equals(wire)) throw new IllegalArgumentException("invalid progression request");
        }

        public RequestProgressC2S() { this("request"); }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SetPartyC2S(String wire) implements CustomPacketPayload {
        public static final Type<SetPartyC2S> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "set_party"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetPartyC2S> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SetPartyC2S::wire, SetPartyC2S::new);

        public SetPartyC2S { wire = checkedWire(wire); }

        public static SetPartyC2S of(List<String> expectedParty, List<String> requestedParty) {
            return new SetPartyC2S(packList(expectedParty) + "|" + packList(requestedParty));
        }

        public DecodedSetParty decode() {
            String[] parts = wire.split("\\|", -1);
            if (parts.length != 2) throw new IllegalArgumentException("invalid set-party wire");
            return new DecodedSetParty(unpackList(parts[0]), unpackList(parts[1]));
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record GrowthC2S(String wire) implements CustomPacketPayload {
        public static final Type<GrowthC2S> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "growth_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, GrowthC2S> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, GrowthC2S::wire, GrowthC2S::new);

        public GrowthC2S { wire = checkedWire(wire); }

        public static GrowthC2S of(String operation, String characterId, int expectedStar, int expectedLevel) {
            return new GrowthC2S(pack(operation) + "|" + pack(characterId) + "|" + expectedStar + "|" + expectedLevel);
        }

        public DecodedGrowth decode() {
            String[] parts = wire.split("\\|", -1);
            if (parts.length != 4) throw new IllegalArgumentException("invalid growth wire");
            return new DecodedGrowth(unpack(parts[0]), unpack(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ProgressSnapshotS2C(String wire) implements CustomPacketPayload {
        public static final Type<ProgressSnapshotS2C> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "progress_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ProgressSnapshotS2C> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ProgressSnapshotS2C::wire, ProgressSnapshotS2C::new);

        public ProgressSnapshotS2C { wire = checkedWire(wire); }

        public static ProgressSnapshotS2C from(
                PlayerProgress progress,
                DefinitionRegistry definitions,
                String resultCode,
                String resultDetail
        ) {
            if (progress == null || definitions == null) throw new IllegalArgumentException("progress/definitions required");
            ProgressionDefinition tuning = definitions.progressions().get(PlayerProgressStore.DEFAULT_PROGRESSION_ID);
            if (tuning == null) throw new IllegalStateException("missing progression definition " + PlayerProgressStore.DEFAULT_PROGRESSION_ID);

            List<CharacterView> characters = new ArrayList<>();
            for (CharacterDefinition definition : definitions.characters().values()) {
                CharacterProgress owned = progress.characters().get(definition.id());
                CharacterDefinition.Stats stats;
                int currentStar;
                int level;
                int cap;
                if (owned != null) {
                    stats = ProgressionRules.stats(definition, owned);
                    currentStar = owned.currentStar();
                    level = owned.level();
                    cap = ProgressionRules.levelCap(currentStar);
                } else {
                    stats = definition.baseStats();
                    currentStar = definition.originStar();
                    level = 0;
                    cap = ProgressionRules.levelCap(currentStar);
                }
                List<String> affinities = definition.affinities().entrySet().stream()
                        .sorted(java.util.Map.Entry.comparingByKey())
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .toList();
                List<ActionView> actionViews = definition.actions().stream()
                        .map(actionId -> actionView(definitions, actionId))
                        .toList();
                characters.add(new CharacterView(
                        definition.id(),
                        owned != null,
                        definition.originStar(),
                        currentStar,
                        level,
                        cap,
                        definition.squadCost(),
                        definition.roles(),
                        stats.hp(), stats.atk(), stats.def(), stats.spd(), stats.poise(),
                        affinities,
                        definition.basicAction(),
                        definition.skills(),
                        definition.burst(),
                        definition.passives(),
                        actionViews,
                        growthView(progress, definition, tuning, owned, stats)));
            }
            characters.sort(Comparator
                    .comparing(CharacterView::owned).reversed()
                    .thenComparing(CharacterView::originStar, Comparator.reverseOrder())
                    .thenComparing(CharacterView::id));
            return of(new Snapshot(progress.coin(), progress.essence(), progress.partyCapacity(), progress.party(),
                    characters, resultCode, resultDetail));
        }

        public static ProgressSnapshotS2C of(Snapshot snapshot) {
            String wire = snapshot.coin()
                    + "|" + snapshot.essence()
                    + "|" + snapshot.partyCapacity()
                    + "|" + packList(snapshot.party())
                    + "|" + packCharacters(snapshot.characters())
                    + "|" + pack(snapshot.resultCode())
                    + "|" + pack(snapshot.resultDetail());
            return new ProgressSnapshotS2C(wire);
        }

        public Snapshot decode() {
            String[] parts = wire.split("\\|", -1);
            if (parts.length != 7) throw new IllegalArgumentException("invalid progress snapshot wire");
            return new Snapshot(
                    Long.parseLong(parts[0]),
                    Long.parseLong(parts[1]),
                    Integer.parseInt(parts[2]),
                    unpackList(parts[3]),
                    unpackCharacters(parts[4]),
                    unpack(parts[5]),
                    unpack(parts[6]));
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static ActionView actionView(DefinitionRegistry definitions, String actionId) {
        ActionDefinition action = definitions.actions().get(actionId);
        if (action == null) throw new IllegalStateException("missing action definition " + actionId);
        return new ActionView(
                action.id(), action.kind(), action.energyDelta(), action.hpPower(), action.poisePower(), action.damageTag(),
                action.targeting().team(), action.targeting().shape(), action.targeting().count(),
                action.effects().stream().map(effect -> new EffectView(
                        effect.type(), effect.status(), effect.value(), effect.duration(), effect.chance())).toList());
    }

    private static GrowthView growthView(
            PlayerProgress progress,
            CharacterDefinition definition,
            ProgressionDefinition tuning,
            CharacterProgress owned,
            CharacterDefinition.Stats currentStats
    ) {
        int shardBalance = progress.shards().getOrDefault(definition.id(), 0);
        StatsView current = statsView(currentStats);
        if (owned == null) {
            return new GrowthView(false, shardBalance, 0, current, CostView.ZERO, "NOT_OWNED",
                    definition.originStar(), ProgressionRules.levelCap(definition.originStar()), current, CostView.ZERO, "NOT_OWNED");
        }

        int cap = ProgressionRules.levelCap(owned.currentStar());
        int nextLevel = owned.level();
        StatsView nextLevelStats = current;
        CostView levelCost = CostView.ZERO;
        String levelBlock = "";
        if (owned.level() >= cap) {
            levelBlock = "LEVEL_CAP";
        } else {
            nextLevel = owned.level() + 1;
            CharacterProgress preview = new CharacterProgress(owned.characterId(), owned.originStar(), owned.currentStar(), nextLevel);
            nextLevelStats = statsView(ProgressionRules.stats(definition, preview));
            levelCost = costView(ProgressionRules.levelUpCost(tuning, owned.currentStar(), owned.level()));
            levelBlock = affordability(progress, levelCost, false, shardBalance);
        }

        int nextStar = owned.currentStar();
        int nextLevelCap = cap;
        StatsView nextStarStats = current;
        CostView ascendCost = CostView.ZERO;
        String ascendBlock = "";
        if (owned.currentStar() >= 6) {
            ascendBlock = "MAX_STAR";
        } else {
            nextStar = owned.currentStar() + 1;
            nextLevelCap = ProgressionRules.levelCap(nextStar);
            CharacterProgress preview = new CharacterProgress(owned.characterId(), owned.originStar(), nextStar, owned.level());
            nextStarStats = statsView(ProgressionRules.stats(definition, preview));
            ascendCost = costView(ProgressionRules.ascensionCost(tuning, nextStar));
            if (owned.level() != cap) {
                ascendBlock = "NOT_AT_LEVEL_CAP";
            } else {
                ascendBlock = affordability(progress, ascendCost, true, shardBalance);
            }
        }

        return new GrowthView(true, shardBalance, nextLevel, nextLevelStats, levelCost, levelBlock,
                nextStar, nextLevelCap, nextStarStats, ascendCost, ascendBlock);
    }

    private static String affordability(PlayerProgress progress, CostView cost, boolean checkShards, int shardBalance) {
        if (progress.coin() < cost.coin()) return "INSUFFICIENT_COIN";
        if (progress.essence() < cost.essence()) return "INSUFFICIENT_ESSENCE";
        if (checkShards && shardBalance < cost.shards()) return "INSUFFICIENT_SHARDS";
        return "";
    }

    private static StatsView statsView(CharacterDefinition.Stats stats) {
        return new StatsView(stats.hp(), stats.atk(), stats.def(), stats.spd(), stats.poise());
    }

    private static CostView costView(ProgressionRules.Cost cost) {
        return new CostView(cost.coin(), cost.essence(), cost.shards());
    }

    private static String packCharacters(List<CharacterView> characters) {
        if (characters == null || characters.isEmpty()) return "";
        List<String> out = new ArrayList<>();
        for (CharacterView c : characters) {
            out.add(String.join(",",
                    pack(c.id()),
                    c.owned() ? "1" : "0",
                    Integer.toString(c.originStar()),
                    Integer.toString(c.currentStar()),
                    Integer.toString(c.level()),
                    Integer.toString(c.levelCap()),
                    Integer.toString(c.squadCost()),
                    packList(c.roles()),
                    Integer.toString(c.hp()),
                    Integer.toString(c.atk()),
                    Integer.toString(c.def()),
                    Integer.toString(c.spd()),
                    Integer.toString(c.poise()),
                    packList(c.affinities()),
                    pack(c.basicAction()),
                    packList(c.skills()),
                    pack(c.burst()),
                    packList(c.passives()),
                    pack(packActions(c.actions())),
                    pack(packGrowth(c.growth()))));
        }
        return String.join(";", out);
    }

    private static List<CharacterView> unpackCharacters(String wire) {
        if (wire == null || wire.isEmpty()) return List.of();
        List<CharacterView> out = new ArrayList<>();
        for (String packedCharacter : wire.split(";", -1)) {
            String[] p = packedCharacter.split(",", -1);
            if (p.length != 20) throw new IllegalArgumentException("invalid character view wire");
            out.add(new CharacterView(
                    unpack(p[0]),
                    "1".equals(p[1]),
                    Integer.parseInt(p[2]),
                    Integer.parseInt(p[3]),
                    Integer.parseInt(p[4]),
                    Integer.parseInt(p[5]),
                    Integer.parseInt(p[6]),
                    unpackList(p[7]),
                    Integer.parseInt(p[8]),
                    Integer.parseInt(p[9]),
                    Integer.parseInt(p[10]),
                    Integer.parseInt(p[11]),
                    Integer.parseInt(p[12]),
                    unpackList(p[13]),
                    unpack(p[14]),
                    unpackList(p[15]),
                    unpack(p[16]),
                    unpackList(p[17]),
                    unpackActions(unpack(p[18])),
                    unpackGrowth(unpack(p[19]))));
        }
        return List.copyOf(out);
    }

    private static String packActions(List<ActionView> actions) {
        if (actions == null || actions.isEmpty()) return "";
        List<String> out = new ArrayList<>();
        for (ActionView action : actions) {
            out.add(String.join(",",
                    pack(action.id()), pack(action.kind()), Integer.toString(action.energyDelta()),
                    Integer.toString(action.hpPower()), Integer.toString(action.poisePower()), pack(action.damageTag()),
                    pack(action.targetTeam()), pack(action.targetShape()), Integer.toString(action.targetCount()),
                    pack(packEffects(action.effects()))));
        }
        return String.join(";", out);
    }

    private static List<ActionView> unpackActions(String wire) {
        if (wire == null || wire.isEmpty()) return List.of();
        List<ActionView> out = new ArrayList<>();
        for (String packedAction : wire.split(";", -1)) {
            String[] p = packedAction.split(",", -1);
            if (p.length != 10) throw new IllegalArgumentException("invalid action view wire");
            out.add(new ActionView(unpack(p[0]), unpack(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                    Integer.parseInt(p[4]), unpack(p[5]), unpack(p[6]), unpack(p[7]), Integer.parseInt(p[8]),
                    unpackEffects(unpack(p[9]))));
        }
        return List.copyOf(out);
    }

    private static String packEffects(List<EffectView> effects) {
        if (effects == null || effects.isEmpty()) return "";
        List<String> out = new ArrayList<>();
        for (EffectView effect : effects) {
            out.add(String.join(":", pack(effect.type()), pack(effect.status()), Double.toString(effect.value()),
                    Integer.toString(effect.duration()), Double.toString(effect.chance())));
        }
        return String.join(";", out);
    }

    private static List<EffectView> unpackEffects(String wire) {
        if (wire == null || wire.isEmpty()) return List.of();
        List<EffectView> out = new ArrayList<>();
        for (String packedEffect : wire.split(";", -1)) {
            String[] p = packedEffect.split(":", -1);
            if (p.length != 5) throw new IllegalArgumentException("invalid effect view wire");
            out.add(new EffectView(unpack(p[0]), unpack(p[1]), Double.parseDouble(p[2]), Integer.parseInt(p[3]), Double.parseDouble(p[4])));
        }
        return List.copyOf(out);
    }

    private static String packGrowth(GrowthView growth) {
        return String.join("~",
                growth.owned() ? "1" : "0",
                Integer.toString(growth.shardBalance()),
                Integer.toString(growth.nextLevel()),
                packStats(growth.nextLevelStats()),
                packCost(growth.levelCost()),
                pack(growth.levelBlockCode()),
                Integer.toString(growth.nextStar()),
                Integer.toString(growth.nextLevelCap()),
                packStats(growth.nextStarStats()),
                packCost(growth.ascendCost()),
                pack(growth.ascendBlockCode()));
    }

    private static GrowthView unpackGrowth(String wire) {
        String[] p = wire.split("~", -1);
        if (p.length != 11) throw new IllegalArgumentException("invalid growth view wire");
        return new GrowthView("1".equals(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), unpackStats(p[3]),
                unpackCost(p[4]), unpack(p[5]), Integer.parseInt(p[6]), Integer.parseInt(p[7]), unpackStats(p[8]),
                unpackCost(p[9]), unpack(p[10]));
    }

    private static String packStats(StatsView stats) {
        return stats.hp() + ":" + stats.atk() + ":" + stats.def() + ":" + stats.spd() + ":" + stats.poise();
    }

    private static StatsView unpackStats(String wire) {
        String[] p = wire.split(":", -1);
        if (p.length != 5) throw new IllegalArgumentException("invalid stats wire");
        return new StatsView(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]));
    }

    private static String packCost(CostView cost) {
        return cost.coin() + ":" + cost.essence() + ":" + cost.shards();
    }

    private static CostView unpackCost(String wire) {
        String[] p = wire.split(":", -1);
        if (p.length != 3) throw new IllegalArgumentException("invalid cost wire");
        return new CostView(Long.parseLong(p[0]), Long.parseLong(p[1]), Integer.parseInt(p[2]));
    }

    private static String packList(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        return values.stream().map(ProgressionNetworkPayloads::pack).reduce((a, b) -> a + "~" + b).orElse("");
    }

    private static List<String> unpackList(String value) {
        if (value == null || value.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        for (String part : value.split("~", -1)) out.add(unpack(part));
        return List.copyOf(out);
    }

    private static String pack(String value) {
        String safe = value == null ? "" : value;
        return B64E.encodeToString(safe.getBytes(StandardCharsets.UTF_8));
    }

    private static String unpack(String value) {
        if (value == null || value.isEmpty()) return "";
        return new String(B64D.decode(value), StandardCharsets.UTF_8);
    }

    private static String checkedWire(String wire) {
        if (wire == null || wire.length() > MAX_WIRE_CHARS) throw new IllegalArgumentException("wire too large");
        return wire;
    }
}

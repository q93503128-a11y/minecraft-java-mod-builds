package kr.moonseungjun.arcanecircle.magic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MagicPlayerData extends SavedData {
    private record MasteryEntry(String spellId, int casts) {
        private static final Codec<MasteryEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("spell").forGetter(MasteryEntry::spellId),
                Codec.INT.optionalFieldOf("casts", 0).forGetter(MasteryEntry::casts)
        ).apply(instance, MasteryEntry::new));
    }

    private record PlayerEntry(
            String uuid,
            int circle,
            double mana,
            int insight,
            List<String> known,
            List<String> slots,
            int selected,
            String focus,
            String weave,
            List<MasteryEntry> fusionMastery
    ) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                Codec.INT.optionalFieldOf("circle", 1).forGetter(PlayerEntry::circle),
                Codec.DOUBLE.optionalFieldOf("mana", 100.0).forGetter(PlayerEntry::mana),
                Codec.INT.optionalFieldOf("insight", 0).forGetter(PlayerEntry::insight),
                Codec.STRING.listOf().optionalFieldOf("known", List.of()).forGetter(PlayerEntry::known),
                Codec.STRING.listOf().optionalFieldOf("slots", List.of()).forGetter(PlayerEntry::slots),
                Codec.INT.optionalFieldOf("selected", 0).forGetter(PlayerEntry::selected),
                Codec.STRING.optionalFieldOf("focus", "").forGetter(PlayerEntry::focus),
                Codec.STRING.optionalFieldOf("weave", "").forGetter(PlayerEntry::weave),
                MasteryEntry.CODEC.listOf().optionalFieldOf("fusion_mastery", List.of()).forGetter(PlayerEntry::fusionMastery)
        ).apply(instance, PlayerEntry::new));
    }

    public static final SavedDataType<MagicPlayerData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "mage_profiles_v2"),
            MagicPlayerData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of())
                            .forGetter(MagicPlayerData::entries)
            ).apply(instance, MagicPlayerData::new))
    );

    private final Map<String, MageState> players = new HashMap<>();

    public MagicPlayerData() {}

    private MagicPlayerData(List<PlayerEntry> entries) {
        for (PlayerEntry entry : entries) players.put(entry.uuid(), new MageState(entry));
    }

    public static MagicPlayerData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean ensureProfile(ServerPlayer player) {
        String key = player.getUUID().toString();
        if (players.containsKey(key)) return false;
        players.put(key, MageState.fresh());
        setDirty();
        return true;
    }

    public MageState state(ServerPlayer player) {
        ensureProfile(player);
        return players.get(player.getUUID().toString());
    }

    public void regenerate(ServerPlayer player) {
        MageState state = state(player);
        double before = state.mana;
        state.mana = Math.min(state.maxMana(), state.mana + state.regenPerHalfSecond());
        if (Math.abs(before - state.mana) > 0.001) setDirty();
    }

    public boolean selectSpell(ServerPlayer player, int socket, String spellId) {
        MageState state = state(player);
        SpellDefinition spell = SpellCatalog.spell(spellId).orElse(null);
        if ((socket != 0 && socket != 1) || spell == null || !state.known.contains(spellId)
                || spell.circle() > state.circle) {
            return false;
        }
        if (socket == 0) state.focus = spellId;
        else state.weave = spellId;
        setDirty();
        return true;
    }

    public String cycleSpell(ServerPlayer player, int socket, int delta) {
        MageState state = state(player);
        List<String> usable = SpellCatalog.spells().values().stream()
                .filter(spell -> state.known.contains(spell.id()) && spell.circle() <= state.circle)
                .map(SpellDefinition::id)
                .toList();
        if (usable.isEmpty()) return "";
        String current = socket == 1 ? state.weave : state.focus;
        int index = usable.indexOf(current);
        int next = Math.floorMod((index < 0 ? 0 : index) + delta, usable.size());
        if (socket == 1) state.weave = usable.get(next);
        else state.focus = usable.get(next);
        setDirty();
        return usable.get(next);
    }

    public CastPreparation prepareDirect(ServerPlayer player) {
        MageState state = state(player);
        return prepare(state, state.focus, false, "");
    }

    public CastPreparation prepareFusion(ServerPlayer player) {
        MageState state = state(player);
        if (state.focus.equals(state.weave)) {
            return CastPreparation.failure("같은 주문 두 개로는 융합 회로를 만들 수 없습니다.");
        }
        SpellCatalog.FusionFormula formula = SpellCatalog.fusionFor(state.focus, state.weave).orElse(null);
        if (formula == null) {
            return CastPreparation.failure("선택한 두 주문 사이에 안정된 융합식이 없습니다.");
        }
        if (!state.known.contains(formula.first()) || !state.known.contains(formula.second())) {
            return CastPreparation.failure("두 원본 주문을 모두 익혀야 융합할 수 있습니다.");
        }
        return prepare(state, formula.result(), true, formula.result());
    }

    private CastPreparation prepare(MageState state, String spellId, boolean fusion, String masteryId) {
        SpellDefinition spell = SpellCatalog.spell(spellId).orElse(null);
        if (spell == null || (!fusion && !state.known.contains(spellId))) {
            return CastPreparation.failure("선택한 주문을 사용할 수 없습니다.");
        }
        if (spell.circle() > state.circle) {
            return CastPreparation.failure(spell.circle() + "써클 주문은 현재 마력핵으로 안정화할 수 없습니다.");
        }
        int masteryGap = Math.max(0, state.circle - spell.circle());
        int manaCost = Math.max(1, (int) Math.ceil(spell.manaCost() * (1.0 - masteryGap * 0.10)));
        int cooldown = Math.max(10, (int) Math.round(spell.cooldownTicks() * (1.0 - masteryGap * 0.18)));
        double range = spell.range() * (1.0 + masteryGap * 0.10);
        double power = spell.power() * (1.0 + masteryGap * 0.12);
        if (state.mana + 0.0001 < manaCost) {
            return CastPreparation.failure("마력이 부족합니다. 필요 " + manaCost + " / 현재 " + (int) state.mana);
        }
        return CastPreparation.success(spell, manaCost, cooldown, range, power, fusion, masteryId);
    }

    public CastProgress completeCast(ServerPlayer player, CastPreparation cast) {
        MageState state = state(player);
        state.mana = Math.max(0.0, state.mana - cast.manaCost());
        state.insight += Math.max(1, cast.spell().circle());

        int previousCircle = state.circle;
        if (state.circle < 2 && state.insight >= 8) state.circle = 2;
        if (state.circle < 3 && state.insight >= 24) state.circle = 3;
        if (state.circle > previousCircle) state.mana = state.maxMana();

        MasteryProgress mastery = MasteryProgress.none();
        if (cast.fusion() && !cast.masteryId().isBlank()) {
            String resultId = cast.masteryId();
            int required = SpellCatalog.masteryRequired(resultId);
            int before = state.mastery.getOrDefault(resultId, 0);
            int after = Math.min(required, before + 1);
            state.mastery.put(resultId, after);
            boolean registered = after >= required && state.known.add(resultId);
            mastery = new MasteryProgress(true, registered, resultId, after, required);
        }

        setDirty();
        return new CastProgress(new CircleAdvance(previousCircle, state.circle), mastery);
    }

    private List<PlayerEntry> entries() {
        return players.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue().entry(entry.getKey()))
                .toList();
    }

    public record CircleAdvance(int previous, int current) {
        public boolean advanced() { return current > previous; }
    }

    public record MasteryProgress(
            boolean changed,
            boolean registered,
            String spellId,
            int casts,
            int required
    ) {
        static MasteryProgress none() { return new MasteryProgress(false, false, "", 0, 0); }
    }

    public record CastProgress(CircleAdvance circle, MasteryProgress mastery) {}

    public record CastPreparation(
            boolean accepted,
            String message,
            SpellDefinition spell,
            int manaCost,
            int cooldownTicks,
            double range,
            double power,
            boolean fusion,
            String masteryId
    ) {
        static CastPreparation failure(String message) {
            return new CastPreparation(false, message, null, 0, 0, 0.0, 0.0, false, "");
        }

        static CastPreparation success(SpellDefinition spell, int mana, int cooldown, double range, double power,
                                       boolean fusion, String masteryId) {
            return new CastPreparation(true, "", spell, mana, cooldown, range, power, fusion, masteryId);
        }
    }

    public static final class MageState {
        private int circle;
        private double mana;
        private int insight;
        private final Set<String> known;
        private final List<String> legacySlots;
        private int legacySelected;
        private String focus;
        private String weave;
        private final Map<String, Integer> mastery;

        private MageState(PlayerEntry entry) {
            this.circle = Math.max(1, Math.min(3, entry.circle()));
            this.insight = Math.max(0, entry.insight());
            this.known = new LinkedHashSet<>(entry.known());
            this.known.addAll(SpellCatalog.starterKnownSpells());
            this.legacySlots = normalizedSlots(entry.slots());
            this.legacySelected = Math.max(0, Math.min(4, entry.selected()));
            this.focus = validKnown(entry.focus()) ? entry.focus() : legacySlots.get(legacySelected);
            String fallbackWeave = legacySlots.stream().filter(id -> !id.equals(focus)).findFirst().orElse("ember");
            this.weave = validKnown(entry.weave()) && !entry.weave().equals(focus) ? entry.weave() : fallbackWeave;
            this.mastery = new LinkedHashMap<>();
            entry.fusionMastery().stream()
                    .sorted(Comparator.comparing(MasteryEntry::spellId))
                    .forEach(value -> {
                        if (SpellCatalog.isFusionResult(value.spellId()) && value.casts() > 0) {
                            mastery.put(value.spellId(), Math.min(SpellCatalog.masteryRequired(value.spellId()), value.casts()));
                        }
                    });
            for (SpellCatalog.FusionFormula formula : SpellCatalog.fusions()) {
                int required = SpellCatalog.masteryRequired(formula.result());
                if (known.contains(formula.result())) mastery.put(formula.result(), required);
                if (mastery.getOrDefault(formula.result(), 0) >= required) known.add(formula.result());
            }
            this.mana = Math.max(0.0, Math.min(maxMana(), entry.mana()));
        }

        private boolean validKnown(String spellId) {
            return !spellId.isBlank() && known.contains(spellId) && SpellCatalog.spell(spellId).isPresent();
        }

        private static MageState fresh() {
            return new MageState(new PlayerEntry("", 1, 100.0, 0,
                    SpellCatalog.starterKnownSpells(), SpellCatalog.starterSlots(), 0,
                    "arcane_dart", "ember", List.of()));
        }

        private static List<String> normalizedSlots(List<String> source) {
            List<String> result = new ArrayList<>(SpellCatalog.starterSlots());
            for (int index = 0; index < Math.min(5, source.size()); index++) {
                if (SpellCatalog.spell(source.get(index)).isPresent()) result.set(index, source.get(index));
            }
            return result;
        }

        private PlayerEntry entry(String uuid) {
            List<MasteryEntry> masteryEntries = mastery.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(value -> new MasteryEntry(value.getKey(), value.getValue()))
                    .toList();
            return new PlayerEntry(uuid, circle, mana, insight, List.copyOf(known), List.copyOf(legacySlots),
                    legacySelected, focus, weave, masteryEntries);
        }

        public int circle() { return circle; }
        public double mana() { return mana; }
        public int insight() { return insight; }
        public Set<String> known() { return Set.copyOf(known); }
        public String focus() { return focus; }
        public String weave() { return weave; }
        public int mastery(String spellId) {
            int value = mastery.getOrDefault(spellId, 0);
            return known.contains(spellId) && SpellCatalog.isFusionResult(spellId)
                    ? Math.max(value, SpellCatalog.masteryRequired(spellId)) : value;
        }
        public Map<String, Integer> mastery() { return Map.copyOf(mastery); }
        public int maxMana() { return switch (circle) { case 2 -> 170; case 3 -> 260; default -> 100; }; }
        public double regenPerHalfSecond() { return switch (circle) { case 2 -> 2.0; case 3 -> 3.0; default -> 1.0; }; }
        public int nextCircleInsight() { return circle >= 3 ? 24 : circle == 2 ? 24 : 8; }
    }
}

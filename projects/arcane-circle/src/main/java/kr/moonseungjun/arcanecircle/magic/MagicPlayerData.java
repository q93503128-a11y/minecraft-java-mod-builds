package kr.moonseungjun.arcanecircle.magic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.item.ArcaneStaffItem.StaffProfile;
import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
import java.util.stream.Collectors;

public final class MagicPlayerData extends SavedData {
    private record MasteryEntry(String spellId, int casts) {
        private static final Codec<MasteryEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("spell").forGetter(MasteryEntry::spellId),
                Codec.INT.optionalFieldOf("casts", 0).forGetter(MasteryEntry::casts)
        ).apply(instance, MasteryEntry::new));
    }

    private record CooldownEntry(String spellId, long readyAt, int totalTicks) {
        private static final Codec<CooldownEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("spell").forGetter(CooldownEntry::spellId),
                Codec.LONG.optionalFieldOf("ready_at", 0L).forGetter(CooldownEntry::readyAt),
                Codec.INT.optionalFieldOf("total", 0).forGetter(CooldownEntry::totalTicks)
        ).apply(instance, CooldownEntry::new));
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
            List<MasteryEntry> fusionMastery,
            List<CooldownEntry> cooldowns,
            boolean starterStaffGranted,
            boolean starterPrimerGranted
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
                MasteryEntry.CODEC.listOf().optionalFieldOf("fusion_mastery", List.of()).forGetter(PlayerEntry::fusionMastery),
                CooldownEntry.CODEC.listOf().optionalFieldOf("cooldowns", List.of()).forGetter(PlayerEntry::cooldowns),
                Codec.BOOL.optionalFieldOf("starter_staff_granted", false).forGetter(PlayerEntry::starterStaffGranted),
                Codec.BOOL.optionalFieldOf("starter_primer_granted", false).forGetter(PlayerEntry::starterPrimerGranted)
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

    public boolean claimStarterStaff(ServerPlayer player) {
        MageState state = state(player);
        if (state.starterStaffGranted) return false;
        state.starterStaffGranted = true;
        setDirty();
        return true;
    }

    public boolean claimStarterPrimer(ServerPlayer player) {
        MageState state = state(player);
        if (state.starterPrimerGranted) return false;
        state.starterPrimerGranted = true;
        setDirty();
        return true;
    }

    public LearnResult learnPrimer(ServerPlayer player) {
        MageState state = state(player);
        int learned = 0;
        for (SpellDefinition spell : SpellCatalog.primerSpells()) {
            if (state.known.add(spell.id())) {
                learned++;
                equipIntoFirstEmptySlot(state, spell.id());
            }
        }
        if (learned == 0) return new LearnResult(false, 0, "이미 모든 1써클 기초 주문을 익혔습니다.");
        state.mana = effectiveStats(player).maxMana();
        setDirty();
        return new LearnResult(true, learned, learned + "개의 기초 주문을 습득했습니다.");
    }

    public LearnResult learnSpell(ServerPlayer player, String spellId) {
        MageState state = state(player);
        SpellDefinition spell = SpellCatalog.spell(spellId).orElse(null);
        if (spell == null || spell.acquisition() != SpellDefinition.Acquisition.BOOK) {
            return new LearnResult(false, 0, "이 주문서는 안정된 전승 주문을 담고 있지 않습니다.");
        }
        if (spell.circle() > state.circle) {
            return new LearnResult(false, 0, spell.circle() + "써클 주문은 현재 " + state.circle
                    + "써클 마력핵으로 해독할 수 없습니다.");
        }
        if (!state.known.add(spellId)) {
            return new LearnResult(false, 0, "이미 " + spell.name() + " 주문을 익혔습니다.");
        }
        equipIntoFirstEmptySlot(state, spellId);
        setDirty();
        return new LearnResult(true, 1, spell.name() + "을 습득했습니다.");
    }

    private static void equipIntoFirstEmptySlot(MageState state, String spellId) {
        for (int index = 0; index < state.slots.size(); index++) {
            if (state.slots.get(index).isBlank()) {
                state.slots.set(index, spellId);
                return;
            }
        }
    }

    public EffectiveStats effectiveStats(ServerPlayer player) {
        MageState state = state(player);
        StaffProfile staff = ModItems.equipped(player);
        int maxMana = Math.max(1, state.baseMaxMana() + staff.maxManaBonus());
        if (state.mana > maxMana) {
            state.mana = maxMana;
            setDirty();
        }
        double regen = state.baseRegenPerHalfSecond() * staff.regenMultiplier();
        return new EffectiveStats(maxMana, regen, staff);
    }

    public void regenerate(ServerPlayer player) {
        MageState state = state(player);
        EffectiveStats stats = effectiveStats(player);
        double before = state.mana;
        state.mana = Math.min(stats.maxMana(), state.mana + stats.regenPerHalfSecond());
        if (Math.abs(before - state.mana) > 0.001) setDirty();
    }

    public boolean selectSpell(ServerPlayer player, int slot, String spellId) {
        MageState state = state(player);
        SpellDefinition spell = SpellCatalog.spell(spellId).orElse(null);
        if (slot < 0 || slot >= 5 || spell == null || !state.known.contains(spellId)
                || spell.circle() > state.circle) return false;
        state.slots.set(slot, spellId);
        setDirty();
        return true;
    }

    public CastPreparation prepareSlot(ServerPlayer player, int slot) {
        MageState state = state(player);
        if (slot < 0 || slot >= state.slots.size()) return CastPreparation.failure("존재하지 않는 주문 슬롯입니다.");
        String spellId = state.slots.get(slot);
        if (spellId.isBlank()) return CastPreparation.failure("비어 있는 주문 슬롯입니다. 마도서에서 주문을 장착하세요.");
        return prepare(player, state, spellId, false, "", List.of(spellId));
    }

    public CastPreparation prepareFusion(ServerPlayer player, List<String> ingredients) {
        MageState state = state(player);
        if (ingredients.size() < 2 || ingredients.size() > 3) {
            return CastPreparation.failure("융합은 2개 또는 3개의 주문 회로로 구성해야 합니다.");
        }
        SpellCatalog.FusionFormula formula = SpellCatalog.fusionFor(ingredients).orElse(null);
        if (formula == null) return CastPreparation.failure("선택한 순환에는 안정된 융합식이 없습니다.");
        for (String ingredient : formula.ingredients()) {
            if (!state.known.contains(ingredient)) {
                return CastPreparation.failure("원본 주문을 모두 익혀야 융합할 수 있습니다.");
            }
        }
        return prepare(player, state, formula.result(), true, formula.result(), formula.ingredients());
    }

    public CastPreparation preview(ServerPlayer player, String spellId) {
        MageState state = state(player);
        boolean fusion = SpellCatalog.isFusionResult(spellId) && !state.known.contains(spellId);
        return prepare(player, state, spellId, fusion, fusion ? spellId : "",
                SpellCatalog.spell(spellId).map(SpellDefinition::fusionSources).orElse(List.of(spellId)));
    }

    private CastPreparation prepare(ServerPlayer player, MageState state, String spellId, boolean fusion,
                                    String masteryId, List<String> ingredients) {
        SpellDefinition spell = SpellCatalog.spell(spellId).orElse(null);
        if (spell == null || (!fusion && !state.known.contains(spellId))) {
            return CastPreparation.failure("선택한 주문을 아직 습득하지 않았습니다.");
        }
        if (spell.circle() > state.circle) {
            return CastPreparation.failure(spell.circle() + "써클 주문은 현재 마력핵으로 안정화할 수 없습니다.");
        }

        StaffProfile staff = ModItems.equipped(player);
        kr.moonseungjun.arcanecircle.world.ArcaneWorldData world =
                kr.moonseungjun.arcanecircle.world.ArcaneWorldData.get(((ServerLevel) player.level()).getServer());
        kr.moonseungjun.arcanecircle.world.MagicTradition chosen = world.tradition(player);
        boolean facultyMatch = chosen != kr.moonseungjun.arcanecircle.world.MagicTradition.UNBOUND
                && SpellWorldLore.tradition(spell.id()) == chosen;
        double facultyMana = facultyMatch ? chosen.manaMultiplier() : 1.0;
        double facultyPower = facultyMatch ? chosen.powerMultiplier() : 1.0;
        double facultyRange = facultyMatch ? chosen.rangeMultiplier() : 1.0;
        double facultyCooldown = facultyMatch ? chosen.cooldownMultiplier() : 1.0;
        int masteryGap = Math.max(0, state.circle - spell.circle());
        int proficiency = SpellCatalog.masteryTier(state.mastery(spellId));
        double circleMana = Math.max(0.48, 1.0 - masteryGap * 0.09);
        double circleCooldown = Math.max(0.38, 1.0 - masteryGap * 0.14);
        double circleRange = 1.0 + masteryGap * 0.08;
        double circlePower = 1.0 + masteryGap * 0.10;
        double masteryMana = Math.max(0.80, 1.0 - proficiency * 0.02);
        double masteryCooldown = Math.max(0.70, 1.0 - proficiency * 0.03);
        double masteryRange = 1.0 + proficiency * 0.02;
        double masteryPower = 1.0 + proficiency * 0.04;

        int manaCost = Math.max(1, (int) Math.ceil(spell.manaCost() * circleMana * masteryMana
                * staff.manaCostMultiplier() * facultyMana));
        int cooldown = Math.max(8, (int) Math.round(spell.cooldownTicks() * circleCooldown * masteryCooldown
                * staff.cooldownMultiplier() * facultyCooldown));
        double range = spell.range() * circleRange * masteryRange * staff.rangeMultiplier() * facultyRange;
        double power = spell.power() * circlePower * masteryPower * staff.powerFor(spell.school()) * facultyPower;

        if (state.mana + 0.0001 < manaCost) {
            return CastPreparation.failure("마력이 부족합니다. 필요 " + manaCost + " / 현재 " + (int) state.mana);
        }
        return CastPreparation.success(spell, manaCost, cooldown, range, power, fusion, masteryId,
                List.copyOf(ingredients), staff);
    }

    public CastProgress completeCast(ServerPlayer player, CastPreparation cast,
                                     CombatGrowthService.Impact impact) {
        MageState state = state(player);
        CombatGrowthService.Impact result = impact == null ? CombatGrowthService.Impact.NONE : impact;
        state.mana = Math.max(0.0, state.mana - cast.manaCost());
        if ("wish".equals(cast.spell().id())) {
            state.mana = effectiveStats(player).maxMana();
            state.cooldowns.entrySet().removeIf(entry -> !"wish".equals(entry.getKey()));
        }

        int beforeMastery = state.mastery.getOrDefault(cast.spell().id(), 0);
        int masteryGain = Math.max(1, result.masteryGain());
        int afterMastery = Math.min(100000, beforeMastery + masteryGain);
        state.mastery.put(cast.spell().id(), afterMastery);
        state.insight += Math.max(1, cast.spell().circle() * 2) + Math.max(0, result.insightGain());

        int previousCircle = state.circle;
        while (state.circle < SpellCatalog.IMPLEMENTED_MAX_CIRCLE
                && state.insight >= SpellCatalog.circleInsightThreshold(state.circle + 1)) {
            state.circle++;
        }
        if (state.circle > previousCircle) state.mana = effectiveStats(player).maxMana();

        MasteryProgress mastery = MasteryProgress.none();
        if (cast.fusion() && !cast.masteryId().isBlank()) {
            String resultId = cast.masteryId();
            int required = SpellCatalog.masteryRequired(resultId);
            boolean registered = afterMastery >= required && state.known.add(resultId);
            if (registered) equipIntoFirstEmptySlot(state, resultId);
            mastery = new MasteryProgress(true, registered, resultId, afterMastery, required);
        }

        setDirty();
        return new CastProgress(new CircleAdvance(previousCircle, state.circle), mastery);
    }

    public CooldownStatus cooldownStatus(ServerPlayer player, String spellId) {
        MageState state = state(player);
        CooldownEntry cooldown = state.cooldowns.get(spellId);
        if (cooldown == null) return CooldownStatus.NONE;
        long remaining = cooldown.readyAt() - serverClock(player);
        if (remaining <= 0L) {
            state.cooldowns.remove(spellId);
            setDirty();
            return CooldownStatus.NONE;
        }
        return new CooldownStatus((int) Math.min(Integer.MAX_VALUE, remaining), Math.max(1, cooldown.totalTicks()));
    }

    public void startCooldown(ServerPlayer player, String spellId, int totalTicks) {
        MageState state = state(player);
        int total = Math.max(1, totalTicks);
        state.cooldowns.put(spellId, new CooldownEntry(spellId, serverClock(player) + total, total));
        setDirty();
    }

    public String cooldownSnapshot(ServerPlayer player) {
        MageState state = state(player);
        long now = serverClock(player);
        boolean removed = state.cooldowns.entrySet().removeIf(entry -> entry.getValue().readyAt() <= now
                || SpellCatalog.spell(entry.getKey()).isEmpty());
        if (removed) setDirty();
        return state.cooldowns.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + ":" + Math.max(0L, entry.getValue().readyAt() - now)
                        + ":" + Math.max(1, entry.getValue().totalTicks()))
                .collect(Collectors.joining("|"));
    }

    private static long serverClock(ServerPlayer player) {
        return ((ServerLevel) player.level()).getServer().overworld().getGameTime();
    }

    private List<PlayerEntry> entries() {
        return players.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue().entry(entry.getKey()))
                .toList();
    }

    public record EffectiveStats(int maxMana, double regenPerHalfSecond, StaffProfile staff) {}
    public record LearnResult(boolean learned, int count, String message) {}

    public record CooldownStatus(int remainingTicks, int totalTicks) {
        public static final CooldownStatus NONE = new CooldownStatus(0, 1);
        public boolean active() { return remainingTicks > 0; }
    }

    public record CircleAdvance(int previous, int current) {
        public boolean advanced() { return current > previous; }
    }

    public record MasteryProgress(boolean changed, boolean registered, String spellId, int casts, int required) {
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
            String masteryId,
            List<String> ingredients,
            StaffProfile staff
    ) {
        static CastPreparation failure(String message) {
            return new CastPreparation(false, message, null, 0, 0, 0.0, 0.0,
                    false, "", List.of(), StaffProfile.NONE);
        }

        static CastPreparation success(SpellDefinition spell, int mana, int cooldown, double range, double power,
                                       boolean fusion, String masteryId, List<String> ingredients, StaffProfile staff) {
            return new CastPreparation(true, "", spell, mana, cooldown, range, power,
                    fusion, masteryId, ingredients, staff);
        }
    }

    public static final class MageState {
        private int circle;
        private double mana;
        private int insight;
        private final Set<String> known;
        private final List<String> slots;
        private final Map<String, Integer> mastery;
        private final Map<String, CooldownEntry> cooldowns;
        private boolean starterStaffGranted;
        private boolean starterPrimerGranted;

        private MageState(PlayerEntry entry) {
            this.circle = Math.max(1, Math.min(SpellCatalog.IMPLEMENTED_MAX_CIRCLE, entry.circle()));
            this.insight = Math.max(0, entry.insight());
            this.known = new LinkedHashSet<>();
            for (String spellId : entry.known()) {
                if (SpellCatalog.spell(spellId).isPresent()) this.known.add(spellId);
            }
            this.slots = normalizedSlots(entry.slots(), entry.focus(), entry.weave());
            for (int index = 0; index < this.slots.size(); index++) {
                String spellId = this.slots.get(index);
                if (!spellId.isBlank() && !known.contains(spellId)) this.slots.set(index, "");
            }
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
            this.cooldowns = new LinkedHashMap<>();
            entry.cooldowns().stream()
                    .filter(value -> value.readyAt() > 0L && value.totalTicks() > 0)
                    .filter(value -> SpellCatalog.spell(value.spellId()).isPresent())
                    .sorted(Comparator.comparing(CooldownEntry::spellId))
                    .forEach(value -> cooldowns.put(value.spellId(), value));
            this.starterStaffGranted = entry.starterStaffGranted();
            this.starterPrimerGranted = entry.starterPrimerGranted();
            this.mana = Math.max(0.0, Math.min(20000.0, entry.mana()));
        }

        private static MageState fresh() {
            return new MageState(new PlayerEntry("", 1, 100.0, 0,
                    List.of(), SpellCatalog.starterSlots(), 0, "", "",
                    List.of(), List.of(), false, false));
        }

        private static List<String> normalizedSlots(List<String> source, String oldFocus, String oldWeave) {
            List<String> result = new ArrayList<>(SpellCatalog.starterSlots());
            for (int index = 0; index < Math.min(5, source.size()); index++) {
                String value = source.get(index);
                if (value == null || value.isBlank()) result.set(index, "");
                else if (SpellCatalog.spell(value).isPresent()) result.set(index, value);
            }
            if (source.isEmpty()) {
                if (SpellCatalog.spell(oldFocus).isPresent()) result.set(0, oldFocus);
                if (SpellCatalog.spell(oldWeave).isPresent()) result.set(1, oldWeave);
            }
            return result;
        }

        private PlayerEntry entry(String uuid) {
            List<MasteryEntry> masteryEntries = mastery.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(value -> new MasteryEntry(value.getKey(), value.getValue()))
                    .toList();
            List<CooldownEntry> cooldownEntries = cooldowns.values().stream()
                    .sorted(Comparator.comparing(CooldownEntry::spellId))
                    .toList();
            return new PlayerEntry(uuid, circle, mana, insight, List.copyOf(known), List.copyOf(slots),
                    0, "", "", masteryEntries, cooldownEntries, starterStaffGranted, starterPrimerGranted);
        }

        public int circle() { return circle; }
        public double mana() { return mana; }
        public int insight() { return insight; }
        public Set<String> known() { return Set.copyOf(known); }
        public List<String> slots() { return List.copyOf(slots); }
        public String slot(int index) { return index >= 0 && index < slots.size() ? slots.get(index) : ""; }
        public int mastery(String spellId) {
            int value = mastery.getOrDefault(spellId, 0);
            return known.contains(spellId) && SpellCatalog.isFusionResult(spellId)
                    ? Math.max(value, SpellCatalog.masteryRequired(spellId)) : value;
        }
        public Map<String, Integer> mastery() { return Map.copyOf(mastery); }
        public int baseMaxMana() {
            return switch (circle) {
                case 2 -> 180; case 3 -> 300; case 4 -> 480; case 5 -> 750;
                case 6 -> 1150; case 7 -> 1800; case 8 -> 2800; case 9 -> 4500;
                default -> 100;
            };
        }
        public double baseRegenPerHalfSecond() {
            return switch (circle) {
                case 2 -> 2.0; case 3 -> 3.2; case 4 -> 4.8; case 5 -> 7.0;
                case 6 -> 10.5; case 7 -> 15.5; case 8 -> 23.0; case 9 -> 34.0;
                default -> 1.0;
            };
        }
        public int nextCircleInsight() {
            return circle >= 5 ? 0 : SpellCatalog.circleInsightThreshold(circle + 1);
        }
    }
}

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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MagicPlayerData extends SavedData {
    private record PlayerEntry(
            String uuid,
            int circle,
            double mana,
            int insight,
            List<String> known,
            List<String> slots,
            int selected
    ) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                Codec.INT.optionalFieldOf("circle", 1).forGetter(PlayerEntry::circle),
                Codec.DOUBLE.optionalFieldOf("mana", 100.0).forGetter(PlayerEntry::mana),
                Codec.INT.optionalFieldOf("insight", 0).forGetter(PlayerEntry::insight),
                Codec.STRING.listOf().optionalFieldOf("known", List.of()).forGetter(PlayerEntry::known),
                Codec.STRING.listOf().optionalFieldOf("slots", List.of()).forGetter(PlayerEntry::slots),
                Codec.INT.optionalFieldOf("selected", 0).forGetter(PlayerEntry::selected)
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

    public boolean equip(ServerPlayer player, int slot, String spellId) {
        MageState state = state(player);
        if (slot < 0 || slot >= 5 || !state.known.contains(spellId) || SpellCatalog.spell(spellId).isEmpty()) return false;
        state.slots.set(slot, spellId);
        state.selected = slot;
        setDirty();
        return true;
    }

    public void select(ServerPlayer player, int slot) {
        MageState state = state(player);
        state.selected = Math.max(0, Math.min(4, slot));
        setDirty();
    }

    public FusionResult fuse(ServerPlayer player, String resultId) {
        MageState state = state(player);
        Optional<SpellDefinition> result = SpellCatalog.spell(resultId);
        if (result.isEmpty() || result.get().acquisition() != SpellDefinition.Acquisition.FUSION) {
            return new FusionResult(false, "존재하지 않는 융합식입니다.");
        }
        if (state.known.contains(resultId)) return new FusionResult(false, "이미 연구한 주문입니다.");
        if (!state.known.containsAll(result.get().fusionSources())) {
            return new FusionResult(false, "선행 주문을 모두 알고 있어야 합니다.");
        }
        state.known.add(resultId);
        setDirty();
        return new FusionResult(true, result.get().name() + " 연구 완료");
    }

    public CastPreparation prepareCast(ServerPlayer player, int requestedSlot) {
        MageState state = state(player);
        int slot = Math.max(0, Math.min(4, requestedSlot));
        state.selected = slot;
        String id = state.slots.get(slot);
        Optional<SpellDefinition> spell = SpellCatalog.spell(id);
        if (spell.isEmpty() || !state.known.contains(id)) {
            return CastPreparation.failure("해당 슬롯에 사용 가능한 주문이 없습니다.");
        }
        if (spell.get().circle() > state.circle) {
            return CastPreparation.failure(spell.get().circle() + "써클 주문은 현재 마력핵으로 안정화할 수 없습니다.");
        }
        int masteryGap = Math.max(0, state.circle - spell.get().circle());
        int manaCost = Math.max(1, (int) Math.ceil(spell.get().manaCost() * (1.0 - masteryGap * 0.10)));
        int cooldown = Math.max(10, (int) Math.round(spell.get().cooldownTicks() * (1.0 - masteryGap * 0.18)));
        double range = spell.get().range() * (1.0 + masteryGap * 0.10);
        double power = spell.get().power() * (1.0 + masteryGap * 0.12);
        if (state.mana + 0.0001 < manaCost) {
            return CastPreparation.failure("마력이 부족합니다. 필요 " + manaCost + " / 현재 " + (int) state.mana);
        }
        return CastPreparation.success(spell.get(), manaCost, cooldown, range, power);
    }

    public CircleAdvance completeCast(ServerPlayer player, int manaCost, int spellCircle) {
        MageState state = state(player);
        state.mana = Math.max(0.0, state.mana - manaCost);
        state.insight += Math.max(1, spellCircle);
        int previous = state.circle;
        if (state.circle < 2 && state.insight >= 8) state.circle = 2;
        if (state.circle < 3 && state.insight >= 24) state.circle = 3;
        if (state.circle > previous) state.mana = state.maxMana();
        setDirty();
        return new CircleAdvance(previous, state.circle);
    }

    private List<PlayerEntry> entries() {
        return players.entrySet().stream().map(entry -> entry.getValue().entry(entry.getKey())).toList();
    }

    public record FusionResult(boolean accepted, String message) {}
    public record CircleAdvance(int previous, int current) {
        public boolean advanced() { return current > previous; }
    }

    public record CastPreparation(
            boolean accepted,
            String message,
            SpellDefinition spell,
            int manaCost,
            int cooldownTicks,
            double range,
            double power
    ) {
        static CastPreparation failure(String message) {
            return new CastPreparation(false, message, null, 0, 0, 0.0, 0.0);
        }
        static CastPreparation success(SpellDefinition spell, int mana, int cooldown, double range, double power) {
            return new CastPreparation(true, "", spell, mana, cooldown, range, power);
        }
    }

    public static final class MageState {
        private int circle;
        private double mana;
        private int insight;
        private final Set<String> known;
        private final List<String> slots;
        private int selected;

        private MageState(PlayerEntry entry) {
            this.circle = Math.max(1, Math.min(3, entry.circle()));
            this.insight = Math.max(0, entry.insight());
            this.known = new LinkedHashSet<>(entry.known());
            this.known.addAll(SpellCatalog.starterKnownSpells());
            this.slots = normalizedSlots(entry.slots());
            this.selected = Math.max(0, Math.min(4, entry.selected()));
            this.mana = Math.max(0.0, Math.min(maxMana(), entry.mana()));
        }

        private static MageState fresh() {
            return new MageState(new PlayerEntry("", 1, 100.0, 0,
                    SpellCatalog.starterKnownSpells(), SpellCatalog.starterSlots(), 0));
        }

        private static List<String> normalizedSlots(List<String> source) {
            List<String> result = new ArrayList<>(SpellCatalog.starterSlots());
            for (int i = 0; i < Math.min(5, source.size()); i++) {
                if (SpellCatalog.spell(source.get(i)).isPresent()) result.set(i, source.get(i));
            }
            return result;
        }

        private PlayerEntry entry(String uuid) {
            return new PlayerEntry(uuid, circle, mana, insight, List.copyOf(known), List.copyOf(slots), selected);
        }

        public int circle() { return circle; }
        public double mana() { return mana; }
        public int insight() { return insight; }
        public Set<String> known() { return Set.copyOf(known); }
        public List<String> slots() { return List.copyOf(slots); }
        public int selected() { return selected; }
        public int maxMana() { return switch (circle) { case 2 -> 170; case 3 -> 260; default -> 100; }; }
        public double regenPerHalfSecond() { return switch (circle) { case 2 -> 2.0; case 3 -> 3.0; default -> 1.0; }; }
        public int nextCircleInsight() { return circle >= 3 ? 24 : circle == 2 ? 24 : 8; }
    }
}

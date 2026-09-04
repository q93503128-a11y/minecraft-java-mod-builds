package kr.moonseungjun.survivalascension.production;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Durable compensation ledger for paid runtime encounters that are canceled by an orderly
 * server shutdown. Long-lived expedition operations already persist their own state, while
 * Apex hunts, Ascension Trials and Outpost/Bastion sieges are intentionally runtime-only.
 * Their admission costs must therefore survive a shutdown even though the encounter does not.
 *
 * Cooldowns are deliberately not rewound: restarting the server cannot be used to bypass the
 * encounter retry timer. Outpost physical-supply refunds use canonical equivalent materials.
 */
public final class EncounterInterruptionData extends SavedData {
    private record PlayerEntry(
            String uuid,
            int echoShards,
            int amethystShards,
            int goldIngots,
            int dragonBreath,
            int supplyCharges,
            int wheat,
            int ironIngots,
            int oakLogs,
            int stoneBricks) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                Codec.INT.optionalFieldOf("echo_shards", 0).forGetter(PlayerEntry::echoShards),
                Codec.INT.optionalFieldOf("amethyst_shards", 0).forGetter(PlayerEntry::amethystShards),
                Codec.INT.optionalFieldOf("gold_ingots", 0).forGetter(PlayerEntry::goldIngots),
                Codec.INT.optionalFieldOf("dragon_breath", 0).forGetter(PlayerEntry::dragonBreath),
                Codec.INT.optionalFieldOf("supply_charges", 0).forGetter(PlayerEntry::supplyCharges),
                Codec.INT.optionalFieldOf("wheat", 0).forGetter(PlayerEntry::wheat),
                Codec.INT.optionalFieldOf("iron_ingots", 0).forGetter(PlayerEntry::ironIngots),
                Codec.INT.optionalFieldOf("oak_logs", 0).forGetter(PlayerEntry::oakLogs),
                Codec.INT.optionalFieldOf("stone_bricks", 0).forGetter(PlayerEntry::stoneBricks)
        ).apply(instance, PlayerEntry::new));
    }

    private static final class State {
        int echoShards;
        int amethystShards;
        int goldIngots;
        int dragonBreath;
        int supplyCharges;
        int wheat;
        int ironIngots;
        int oakLogs;
        int stoneBricks;

        State(PlayerEntry entry) {
            echoShards = sanitize(entry.echoShards());
            amethystShards = sanitize(entry.amethystShards());
            goldIngots = sanitize(entry.goldIngots());
            dragonBreath = sanitize(entry.dragonBreath());
            supplyCharges = sanitize(entry.supplyCharges());
            wheat = sanitize(entry.wheat());
            ironIngots = sanitize(entry.ironIngots());
            oakLogs = sanitize(entry.oakLogs());
            stoneBricks = sanitize(entry.stoneBricks());
        }

        State() {}

        boolean empty() {
            return echoShards <= 0 && amethystShards <= 0 && goldIngots <= 0 && dragonBreath <= 0
                    && supplyCharges <= 0 && wheat <= 0 && ironIngots <= 0 && oakLogs <= 0 && stoneBricks <= 0;
        }
    }

    public static final SavedDataType<EncounterInterruptionData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "encounter_interruption_v1"),
            EncounterInterruptionData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(EncounterInterruptionData::entries)
            ).apply(instance, EncounterInterruptionData::new))
    );

    private final Map<String, State> players = new HashMap<>();

    public EncounterInterruptionData() {}

    private EncounterInterruptionData(List<PlayerEntry> entries) {
        for (PlayerEntry entry : entries) {
            State state = new State(entry);
            if (!state.empty()) players.put(entry.uuid(), state);
        }
    }

    public static EncounterInterruptionData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) get(((ServerLevel) player.level()).getServer()).claim(player);
    }

    public void queueApex(UUID owner) {
        State state = state(owner);
        state.echoShards = add(state.echoShards, 8);
        state.amethystShards = add(state.amethystShards, 32);
        state.goldIngots = add(state.goldIngots, 32);
        setDirty();
    }

    public void queueTrial(UUID owner) {
        State state = state(owner);
        state.echoShards = add(state.echoShards, 32);
        state.amethystShards = add(state.amethystShards, 64);
        state.dragonBreath = add(state.dragonBreath, 8);
        setDirty();
    }

    public void queueSiege(UUID owner, boolean bastion) {
        State state = state(owner);
        if (bastion) {
            state.supplyCharges = add(state.supplyCharges, 2);
            state.wheat = add(state.wheat, 32);
            state.ironIngots = add(state.ironIngots, 8);
            state.stoneBricks = add(state.stoneBricks, 32);
        } else {
            state.supplyCharges = add(state.supplyCharges, 1);
            state.wheat = add(state.wheat, 16);
            state.ironIngots = add(state.ironIngots, 5);
            state.oakLogs = add(state.oakLogs, 12);
        }
        setDirty();
    }

    /** Retry only the abstract supply-ticket part after a slot becomes available. */
    public void retrySupplyRefund(ServerPlayer player) {
        String key = player.getUUID().toString();
        State state = players.get(key);
        if (state == null || state.supplyCharges <= 0) return;
        int restored = ProductionData.get(player).restoreSupplyCharges(player, state.supplyCharges);
        if (restored <= 0) return;
        state.supplyCharges -= restored;
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§b[서버 중단 보상] §f보류 중이던 현장 보급권 §e+" + restored + "§f 자동 복구"));
        finishState(key, state);
    }

    private void claim(ServerPlayer player) {
        String key = player.getUUID().toString();
        State state = players.get(key);
        if (state == null) return;

        int itemCount = state.echoShards + state.amethystShards + state.goldIngots + state.dragonBreath
                + state.wheat + state.ironIngots + state.oakLogs + state.stoneBricks;
        if (state.echoShards > 0) give(player, Items.ECHO_SHARD, state.echoShards);
        if (state.amethystShards > 0) give(player, Items.AMETHYST_SHARD, state.amethystShards);
        if (state.goldIngots > 0) give(player, Items.GOLD_INGOT, state.goldIngots);
        if (state.dragonBreath > 0) give(player, Items.DRAGON_BREATH, state.dragonBreath);
        if (state.wheat > 0) give(player, Items.WHEAT, state.wheat);
        if (state.ironIngots > 0) give(player, Items.IRON_INGOT, state.ironIngots);
        if (state.oakLogs > 0) give(player, Items.OAK_LOG, state.oakLogs);
        if (state.stoneBricks > 0) give(player, Items.STONE_BRICKS, state.stoneBricks);
        state.echoShards = state.amethystShards = state.goldIngots = state.dragonBreath = 0;
        state.wheat = state.ironIngots = state.oakLogs = state.stoneBricks = 0;

        int restored = 0;
        if (state.supplyCharges > 0) {
            restored = ProductionData.get(player).restoreSupplyCharges(player, state.supplyCharges);
            state.supplyCharges -= restored;
        }

        if (itemCount > 0 || restored > 0) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§b[서버 중단 보상] §f서버 종료로 취소된 유료 전투의 투입 재료를 반환했습니다."
                            + (restored > 0 ? " §7· 현장 보급권 +" + restored : "")
                            + " §7· 재사용 대기시간은 그대로 유지됩니다."));
        }
        if (state.supplyCharges > 0) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§7현장 보급권 " + state.supplyCharges + "개는 현재 보유 상한 때문에 보류 중입니다. 공간이 생기면 자동 복구됩니다."));
        }
        finishState(key, state);
    }

    private State state(UUID owner) {
        return players.computeIfAbsent(owner.toString(), ignored -> new State());
    }

    private void finishState(String key, State state) {
        if (state.empty()) players.remove(key);
        setDirty();
    }

    private List<PlayerEntry> entries() {
        List<PlayerEntry> out = new ArrayList<>(players.size());
        players.forEach((uuid, state) -> {
            if (!state.empty()) out.add(new PlayerEntry(uuid, state.echoShards, state.amethystShards, state.goldIngots,
                    state.dragonBreath, state.supplyCharges, state.wheat, state.ironIngots, state.oakLogs, state.stoneBricks));
        });
        return out;
    }

    private static void give(ServerPlayer player, Item item, int amount) {
        int remaining = Math.max(0, amount);
        int max = Math.max(1, new ItemStack(item).getMaxStackSize());
        while (remaining > 0) {
            int count = Math.min(max, remaining);
            ItemStack stack = new ItemStack(item, count);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            remaining -= count;
        }
        player.getInventory().setChanged();
    }

    private static int sanitize(int value) { return Math.max(0, Math.min(4096, value)); }
    private static int add(int current, int amount) { return sanitize(current + Math.max(0, amount)); }
}

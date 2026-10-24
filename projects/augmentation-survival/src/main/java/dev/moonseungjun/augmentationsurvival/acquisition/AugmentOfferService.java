package dev.moonseungjun.augmentationsurvival.acquisition;

import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import dev.moonseungjun.augmentationsurvival.threat.WorldThreatService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Server-authoritative augment draw/selection transaction.
 *
 * <p>Opening an offer never consumes a core. A core is consumed only after the server validates
 * the opaque token and the complete selection, then commits the whole state change atomically.</p>
 */
public final class AugmentOfferService {
    private static final int OFFER_LIFETIME_TICKS = 20 * 60 * 5;
    private static final int TARGET_CANDIDATE_COUNT = 3;
    private static final Map<UUID, PendingOffer> PENDING = new ConcurrentHashMap<>();

    private AugmentOfferService() {
    }

    public static BeginResult beginOffer(ServerPlayer player, AugmentTier tier) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(tier, "tier");

        MinecraftServer server = requireServer(player);
        if (!hasCore(player, tier)) {
            return BeginResult.noCore();
        }

        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        List<AugmentDefinition> eligible = new ArrayList<>();
        for (AugmentDefinition definition : AugmentCatalog.definitions(tier)) {
            if (!definition.isMaxed(state.level(definition.id()))) {
                eligible.add(definition);
            }
        }
        if (eligible.isEmpty()) {
            PENDING.remove(player.getUUID());
            return BeginResult.fullyMaxed();
        }

        shuffle(eligible, player.getRandom());
        int candidateCount = Math.min(TARGET_CANDIDATE_COUNT, eligible.size());
        List<String> candidateIds = eligible.subList(0, candidateCount).stream()
                .map(AugmentDefinition::id)
                .toList();
        int selectionCount = state.level(AugmentCatalog.SECOND_CHOICE_ID) > 0
                ? Math.min(2, candidateCount)
                : 1;
        String token = UUID.randomUUID().toString();
        int expiresAtTick = server.getTickCount() + OFFER_LIFETIME_TICKS;

        PENDING.put(
                player.getUUID(),
                new PendingOffer(token, tier, candidateIds, selectionCount, expiresAtTick)
        );
        return BeginResult.ready(token, tier, candidateIds, selectionCount, expiresAtTick);
    }

    public static CommitResult commitOffer(
            ServerPlayer player,
            String token,
            List<String> selectedIds
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(selectedIds, "selectedIds");

        MinecraftServer server = requireServer(player);
        PendingOffer pending = PENDING.get(player.getUUID());
        if (pending == null || !pending.token().equals(token)) {
            return CommitResult.invalid(CommitStatus.INVALID_TOKEN);
        }
        if (server.getTickCount() > pending.expiresAtTick()) {
            PENDING.remove(player.getUUID(), pending);
            return CommitResult.invalid(CommitStatus.EXPIRED);
        }
        if (selectedIds.size() != pending.selectionCount()) {
            return CommitResult.invalid(CommitStatus.INVALID_SELECTION_COUNT);
        }

        LinkedHashSet<String> distinct = new LinkedHashSet<>(selectedIds);
        if (distinct.size() != selectedIds.size()) {
            return CommitResult.invalid(CommitStatus.DUPLICATE_SELECTION);
        }
        Set<String> offered = new HashSet<>(pending.candidateIds());
        if (!offered.containsAll(distinct)) {
            return CommitResult.invalid(CommitStatus.NOT_OFFERED);
        }
        if (!hasCore(player, pending.tier())) {
            return CommitResult.invalid(CommitStatus.NO_CORE);
        }

        AugmentationPlayerState before = AugmentationPlayerStateService.state(player);
        AugmentationPlayerState next = before;
        for (String selectedId : selectedIds) {
            AugmentDefinition definition;
            try {
                definition = AugmentCatalog.require(selectedId);
            } catch (IllegalArgumentException exception) {
                return CommitResult.invalid(CommitStatus.UNKNOWN_AUGMENT);
            }
            if (definition.tier() != pending.tier()) {
                return CommitResult.invalid(CommitStatus.WRONG_TIER);
            }
            int currentLevel = next.level(selectedId);
            if (definition.isMaxed(currentLevel)) {
                return CommitResult.invalid(CommitStatus.ALREADY_MAXED);
            }
            next = next.withAugmentLevel(selectedId, currentLevel + 1);
        }

        if (!consumeCores(player, pending.tier(), 1)) {
            return CommitResult.invalid(CommitStatus.NO_CORE);
        }

        AugmentationPlayerStateService.replace(player, next);
        PENDING.remove(player.getUUID(), pending);
        WorldThreatService.observePowerScore(
                server,
                AugmentCatalog.powerScore(next.augmentLevels())
        );
        return new CommitResult(CommitStatus.SUCCESS, List.copyOf(selectedIds), next);
    }

    public static boolean cancelOffer(ServerPlayer player, String token) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(token, "token");
        PendingOffer pending = PENDING.get(player.getUUID());
        return pending != null
                && pending.token().equals(token)
                && PENDING.remove(player.getUUID(), pending);
    }

    public static void clearOffer(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        PENDING.remove(player.getUUID());
    }

    static boolean hasCore(ServerPlayer player, AugmentTier tier) {
        Item core = AugmentCoreItems.item(tier);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.is(core)) {
                return true;
            }
        }
        return false;
    }

    static int countCores(ServerPlayer player, AugmentTier tier) {
        Item core = AugmentCoreItems.item(tier);
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.is(core)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    static boolean consumeCores(ServerPlayer player, AugmentTier tier, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Core consumption amount must be positive");
        }
        if (countCores(player, tier) < amount) {
            return false;
        }
        Item core = AugmentCoreItems.item(tier);
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || !stack.is(core)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        if (remaining != 0) {
            throw new IllegalStateException("Core inventory changed during server transaction");
        }
        return true;
    }

    private static void shuffle(List<AugmentDefinition> definitions, RandomSource random) {
        for (int index = definitions.size() - 1; index > 0; index--) {
            int swapIndex = random.nextInt(index + 1);
            AugmentDefinition current = definitions.get(index);
            definitions.set(index, definitions.get(swapIndex));
            definitions.set(swapIndex, current);
        }
    }

    private static MinecraftServer requireServer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            throw new IllegalStateException("Augment selection requires a live server");
        }
        return server;
    }

    public enum BeginStatus {
        READY,
        NO_CORE,
        FULLY_MAXED
    }

    public enum CommitStatus {
        SUCCESS,
        INVALID_TOKEN,
        EXPIRED,
        INVALID_SELECTION_COUNT,
        DUPLICATE_SELECTION,
        NOT_OFFERED,
        NO_CORE,
        UNKNOWN_AUGMENT,
        WRONG_TIER,
        ALREADY_MAXED
    }

    public record BeginResult(
            BeginStatus status,
            String token,
            AugmentTier tier,
            List<String> candidateIds,
            int selectionCount,
            int expiresAtTick
    ) {
        private static BeginResult noCore() {
            return new BeginResult(BeginStatus.NO_CORE, null, null, List.of(), 0, 0);
        }

        private static BeginResult fullyMaxed() {
            return new BeginResult(BeginStatus.FULLY_MAXED, null, null, List.of(), 0, 0);
        }

        private static BeginResult ready(
                String token,
                AugmentTier tier,
                List<String> candidateIds,
                int selectionCount,
                int expiresAtTick
        ) {
            return new BeginResult(
                    BeginStatus.READY,
                    token,
                    tier,
                    List.copyOf(candidateIds),
                    selectionCount,
                    expiresAtTick
            );
        }
    }

    public record CommitResult(
            CommitStatus status,
            List<String> grantedIds,
            AugmentationPlayerState state
    ) {
        private static CommitResult invalid(CommitStatus status) {
            return new CommitResult(status, List.of(), null);
        }
    }

    private record PendingOffer(
            String token,
            AugmentTier tier,
            List<String> candidateIds,
            int selectionCount,
            int expiresAtTick
    ) {
        private PendingOffer {
            candidateIds = List.copyOf(candidateIds);
        }
    }
}

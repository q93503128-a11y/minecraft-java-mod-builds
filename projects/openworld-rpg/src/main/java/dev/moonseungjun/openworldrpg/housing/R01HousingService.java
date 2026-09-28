package dev.moonseungjun.openworldrpg.housing;

import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyService;
import dev.moonseungjun.openworldrpg.world.structure.R01AlderfordRuntimeBindingRegistry;
import dev.moonseungjun.openworldrpg.world.structure.R01StructureBindingData;
import dev.moonseungjun.openworldrpg.world.structure.R01StructureBindingLoader;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-authoritative Alderford property purchase/move transaction.
 *
 * <p>Physical ownership is shared-world state. Personal residence/storage and Gold are durable
 * player state. A target property is reserved before the idempotent Gold ledger and ownership
 * transfer, so reconnect can safely resume instead of duplicating currency or letting two players
 * buy one vacancy.</p>
 */
public final class R01HousingService {
    private R01HousingService() {
    }

    public static R01HousingPlayerState playerState(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                R01HousingAttachments.PLAYER_HOUSING,
                R01HousingPlayerState.initial()
        );
    }

    public static R01HousingWorldState worldState(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getAttachedOrSet(
                R01HousingAttachments.WORLD_HOUSING,
                R01HousingWorldState.initial()
        );
    }

    public static MoveResult requestMove(
            ServerPlayer player,
            String targetPropertyId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(targetPropertyId, "targetPropertyId");

        R01HousingPlayerState personal = playerState(player);
        if (personal.pendingMove().isPresent()) {
            var pending = personal.pendingMove().orElseThrow();
            if (!pending.newPropertyId().equals(targetPropertyId)) {
                return new MoveResult(
                        MoveStatus.TRANSACTION_PENDING,
                        pending.transactionId(),
                        pending.goldDelta()
                );
            }
            return reconcilePending(player);
        }

        R01StructureBindingData bindings =
                R01StructureBindingLoader.loadBundled();
        var runtimeTarget =
                R01AlderfordRuntimeBindingRegistry.productionProperty(
                        targetPropertyId
                );
        if (runtimeTarget.isEmpty()) {
            return new MoveResult(
                    MoveStatus.PROPERTY_NOT_PRODUCTION,
                    "",
                    0L
            );
        }
        if (personal.residencePropertyId()
                .filter(targetPropertyId::equals)
                .isPresent()) {
            return new MoveResult(
                    MoveStatus.ALREADY_OWNED,
                    "",
                    0L
            );
        }

        MinecraftServer server = requireServer(player);
        String playerUuid = player.getUUID().toString();
        R01HousingWorldState shared = worldState(server);

        var owner = shared.owner(targetPropertyId);
        if (owner.isPresent() && !owner.orElseThrow().equals(playerUuid)) {
            return new MoveResult(
                    MoveStatus.PROPERTY_OCCUPIED,
                    "",
                    0L
            );
        }
        var reservation = shared.reservation(targetPropertyId);
        if (reservation.isPresent()
                && !reservation.orElseThrow().playerUuid().equals(playerUuid)) {
            return new MoveResult(
                    MoveStatus.PROPERTY_RESERVED,
                    "",
                    0L
            );
        }

        long saleCredit = personal.residencePropertyId()
                .map(oldId -> {
                    R01StructureBindingData.PropertyBinding old =
                            bindings.property(oldId).orElseThrow(
                                    () -> new IllegalStateException(
                                            "Owned housing property missing from canon: "
                                                    + oldId
                                    )
                            );
                    return Math.round(
                            old.purchasePrice() * old.saleCreditRate()
                    );
                })
                .orElse(0L);
        R01StructureBindingData.PropertyBinding target =
                runtimeTarget.orElseThrow().property();
        long purchasePrice = target.purchasePrice();
        long goldDelta = purchasePrice - saleCredit;

        if (goldDelta > 0L
                && PlayerCurrencyService.state(player).gold() < goldDelta) {
            return new MoveResult(
                    MoveStatus.INSUFFICIENT_GOLD,
                    "",
                    goldDelta
            );
        }

        R01HousingPlayerState prepared = personal.prepareMove(
                playerUuid,
                targetPropertyId,
                target.storageCapacity(),
                purchasePrice,
                saleCredit,
                goldDelta
        );
        replacePlayer(player, prepared);

        R01HousingPlayerState.PendingMove pending =
                prepared.pendingMove().orElseThrow();
        var reserve = shared.reserve(
                targetPropertyId,
                playerUuid,
                pending.transactionId()
        );
        if (!reserve.success()) {
            replacePlayer(
                    player,
                    playerState(player).cancelPending(
                            pending.transactionId()
                    )
            );
            return new MoveResult(
                    reserve.status()
                            == R01HousingWorldState.ReserveStatus.OCCUPIED
                            ? MoveStatus.PROPERTY_OCCUPIED
                            : MoveStatus.PROPERTY_RESERVED,
                    "",
                    goldDelta
            );
        }
        replaceWorld(server, reserve.state());
        return reconcilePending(player);
    }

    public static MoveResult reconcilePending(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        R01HousingPlayerState personal = playerState(player);
        var pendingOptional = personal.pendingMove();
        if (pendingOptional.isEmpty()) {
            return new MoveResult(
                    MoveStatus.NOTHING_PENDING,
                    "",
                    0L
            );
        }

        R01HousingPlayerState.PendingMove pending =
                pendingOptional.orElseThrow();
        MinecraftServer server = requireServer(player);
        String playerUuid = player.getUUID().toString();
        R01HousingWorldState shared = worldState(server);

        boolean transferred = shared.completedTransfer(
                pending.transactionId()
        ).isPresent();

        if (!transferred) {
            var reserve = shared.reserve(
                    pending.newPropertyId(),
                    playerUuid,
                    pending.transactionId()
            );
            if (!reserve.success()) {
                if (pending.goldDelta() > 0L
                        && !PlayerCurrencyService.state(player)
                                .hasAppliedDebit(
                                        debitTransactionId(
                                                pending.transactionId()
                                        )
                                )) {
                    replacePlayer(
                            player,
                            personal.cancelPending(
                                    pending.transactionId()
                            )
                    );
                }
                return new MoveResult(
                        MoveStatus.PROPERTY_CONFLICT,
                        pending.transactionId(),
                        pending.goldDelta()
                );
            }
            replaceWorld(server, reserve.state());
        }

        if (pending.goldDelta() > 0L) {
            var debit = PlayerCurrencyService.debitOnce(
                    player,
                    debitTransactionId(pending.transactionId()),
                    pending.goldDelta()
            );
            if (!debit.success()) {
                R01HousingWorldState next = worldState(server)
                        .cancelReservation(
                                pending.newPropertyId(),
                                playerUuid,
                                pending.transactionId()
                        );
                replaceWorld(server, next);
                replacePlayer(
                        player,
                        playerState(player).cancelPending(
                                pending.transactionId()
                        )
                );
                return new MoveResult(
                        MoveStatus.INSUFFICIENT_GOLD,
                        "",
                        pending.goldDelta()
                );
            }
        } else if (pending.goldDelta() < 0L) {
            PlayerCurrencyService.creditOnce(
                    player,
                    creditTransactionId(pending.transactionId()),
                    -pending.goldDelta()
            );
        }

        shared = worldState(server);
        if (shared.completedTransfer(pending.transactionId()).isEmpty()) {
            shared = shared.transferReserved(pending, playerUuid);
            replaceWorld(server, shared);
        }

        replacePlayer(
                player,
                playerState(player).commitPending(
                        pending.transactionId()
                )
        );
        return new MoveResult(
                pending.oldPropertyId().isPresent()
                        ? MoveStatus.MOVED
                        : MoveStatus.PURCHASED,
                pending.transactionId(),
                pending.goldDelta()
        );
    }

    public static R01HousingWorldState.PropertyRole roleFor(
            MinecraftServer server,
            String propertyId,
            String playerUuid
    ) {
        return worldState(server).roleFor(propertyId, playerUuid);
    }

    public static boolean canFurnish(
            MinecraftServer server,
            String propertyId,
            String playerUuid
    ) {
        return worldState(server).canFurnish(propertyId, playerUuid);
    }

    public static boolean canAccessPrivateStorage(
            MinecraftServer server,
            String propertyId,
            String playerUuid
    ) {
        return worldState(server)
                .canAccessPrivateStorage(propertyId, playerUuid);
    }

    public static boolean canUseNonPrivateFurniture(
            MinecraftServer server,
            String propertyId,
            String playerUuid
    ) {
        return worldState(server)
                .canUseNonPrivateFurniture(propertyId, playerUuid);
    }

    public static void setTrustedDecorator(
            ServerPlayer owner,
            String targetPlayerUuid,
            boolean trusted
    ) {
        Objects.requireNonNull(owner, "owner");
        String propertyId = playerState(owner)
                .residencePropertyId()
                .orElseThrow(() -> new IllegalStateException(
                        "Player does not own a residence."
                ));
        MinecraftServer server = requireServer(owner);
        replaceWorld(
                server,
                worldState(server).setTrustedDecorator(
                        propertyId,
                        owner.getUUID().toString(),
                        targetPlayerUuid,
                        trusted
                )
        );
    }

    public static void setPrivateStorageAccess(
            ServerPlayer owner,
            String targetPlayerUuid,
            boolean allowed
    ) {
        Objects.requireNonNull(owner, "owner");
        String propertyId = playerState(owner)
                .residencePropertyId()
                .orElseThrow(() -> new IllegalStateException(
                        "Player does not own a residence."
                ));
        MinecraftServer server = requireServer(owner);
        replaceWorld(
                server,
                worldState(server).setPrivateStorageAccess(
                        propertyId,
                        owner.getUUID().toString(),
                        targetPlayerUuid,
                        allowed
                )
        );
    }

    public static Optional<String> owner(
            MinecraftServer server,
            String propertyId
    ) {
        return worldState(server).owner(propertyId);
    }

    private static String debitTransactionId(String housingTransactionId) {
        return housingTransactionId + "/debit";
    }

    private static String creditTransactionId(String housingTransactionId) {
        return housingTransactionId + "/credit";
    }

    private static MinecraftServer requireServer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            throw new IllegalStateException(
                    "Housing transaction requires a live server."
            );
        }
        return server;
    }

    private static R01HousingPlayerState replacePlayer(
            ServerPlayer player,
            R01HousingPlayerState next
    ) {
        R01HousingPlayerState current = playerState(player);
        if (!current.equals(next)) {
            player.setAttached(
                    R01HousingAttachments.PLAYER_HOUSING,
                    next
            );
        }
        return next;
    }

    private static R01HousingWorldState replaceWorld(
            MinecraftServer server,
            R01HousingWorldState next
    ) {
        ServerLevel overworld = server.overworld();
        R01HousingWorldState current = worldState(server);
        if (!current.equals(next)) {
            overworld.setAttached(
                    R01HousingAttachments.WORLD_HOUSING,
                    next
            );
        }
        return next;
    }

    public enum MoveStatus {
        PURCHASED,
        MOVED,
        PROPERTY_NOT_PRODUCTION,
        PROPERTY_OCCUPIED,
        PROPERTY_RESERVED,
        PROPERTY_CONFLICT,
        INSUFFICIENT_GOLD,
        ALREADY_OWNED,
        TRANSACTION_PENDING,
        NOTHING_PENDING
    }

    public record MoveResult(
            MoveStatus status,
            String transactionId,
            long goldDelta
    ) {
        public MoveResult {
            Objects.requireNonNull(status, "status");
            transactionId = Objects.requireNonNull(
                    transactionId,
                    "transactionId"
            );
        }

        public boolean completed() {
            return status == MoveStatus.PURCHASED
                    || status == MoveStatus.MOVED;
        }
    }
}

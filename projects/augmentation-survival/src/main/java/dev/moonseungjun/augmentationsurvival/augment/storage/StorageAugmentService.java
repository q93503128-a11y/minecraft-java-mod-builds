package dev.moonseungjun.augmentationsurvival.augment.storage;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.survival.DeathPreservationAugmentService;
import dev.moonseungjun.augmentationsurvival.menu.AugmentStorageMenu;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

/** Server-owned inventory/storage runtime for Pockets and Remote Storage. */
public final class StorageAugmentService {
    private static final String POCKETS_ID = id("pockets");
    private static final String REMOTE_STORAGE_ID = id("remote_storage");
    private static final int INVITE_LIFETIME_TICKS = 60 * 20;

    private static final Map<StorageKey, PersistentSegmentContainer> SEGMENTS = new HashMap<>();
    private static final Set<AugmentStorageMenu> OPEN_MENUS = new HashSet<>();
    private static final Map<UUID, PendingInvite> PENDING_INVITES = new HashMap<>();

    private StorageAugmentService() {
    }

    public static void initialize() {
        AugmentCatalog.require(POCKETS_ID);
        AugmentCatalog.require(REMOTE_STORAGE_ID);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) handleActualPlayerDeath(player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> {
            UUID leaving = listener.player.getUUID();
            PENDING_INVITES.remove(leaving);
            PENDING_INVITES.entrySet().removeIf(entry -> entry.getValue().inviterId.equals(leaving));
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            SEGMENTS.clear();
            OPEN_MENUS.clear();
            PENDING_INVITES.clear();
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("augmentstorage")
                        .then(Commands.literal("open").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            return openStorage(player) ? 1 : 0;
                        }))
                        .then(Commands.literal("invite")
                                .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    return invite(player, target) ? 1 : 0;
                                })))
                        .then(Commands.literal("accept").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            return acceptInvite(player) ? 1 : 0;
                        }))
                        .then(Commands.literal("leave").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            return leaveGroup(player) ? 1 : 0;
                        }))
        ));
    }

    public static int unlockedPocketSlots(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, POCKETS_ID);
        return Math.min(StorageWorldState.POCKET_SIZE, level * 3);
    }

    public static boolean hasRemoteStorage(ServerPlayer player) {
        return boundedLevel(AugmentationPlayerStateService.state(player), REMOTE_STORAGE_ID) > 0;
    }

    public static boolean hasAnyStorage(ServerPlayer player) {
        return unlockedPocketSlots(player) > 0 || hasRemoteStorage(player);
    }

    /** Called after vanilla inventory insertion. Mutates {@code incoming} exactly like an inventory insert. */
    public static boolean tryInsertPocketOverflow(ServerPlayer player, ItemStack incoming) {
        int unlocked = unlockedPocketSlots(player);
        if (unlocked <= 0 || incoming.isEmpty()) return incoming.isEmpty();
        PersistentSegmentContainer pocket = segment(player.level().getServer(), player.getUUID(), SegmentKind.POCKET);
        boolean changed = false;

        for (int slot = 0; slot < unlocked && !incoming.isEmpty(); slot++) {
            ItemStack existing = pocket.getItem(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, incoming)) continue;
            int capacity = Math.max(0, existing.getMaxStackSize() - existing.getCount());
            if (capacity <= 0) continue;
            int moved = Math.min(capacity, incoming.getCount());
            existing.grow(moved);
            incoming.shrink(moved);
            changed = true;
        }
        for (int slot = 0; slot < unlocked && !incoming.isEmpty(); slot++) {
            if (!pocket.getItem(slot).isEmpty()) continue;
            int moved = Math.min(incoming.getMaxStackSize(), incoming.getCount());
            pocket.setItem(slot, incoming.copyWithCount(moved));
            incoming.shrink(moved);
            changed = true;
        }
        if (changed) pocket.setChanged();
        return incoming.isEmpty();
    }

    public static Container pocketContainer(ServerPlayer player) {
        return segment(player.level().getServer(), player.getUUID(), SegmentKind.POCKET);
    }

    public static RemoteView remoteView(ServerPlayer player) {
        return new RemoteView(player);
    }

    public static boolean openStorage(ServerPlayer player) {
        if (!hasAnyStorage(player) || player.isDeadOrDying()) {
            player.sendSystemMessage(Component.translatable("message.augmentation_survival.storage.no_access"));
            return false;
        }
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new AugmentStorageMenu(containerId, inventory, player),
                Component.translatable("container.augmentation_survival.storage")
        ));
        return true;
    }

    public static void registerOpenMenu(AugmentStorageMenu menu) {
        OPEN_MENUS.add(menu);
    }

    public static void unregisterOpenMenu(AugmentStorageMenu menu) {
        OPEN_MENUS.remove(menu);
    }

    public static List<UUID> remoteMembers(ServerPlayer player) {
        if (!hasRemoteStorage(player)) return List.of();
        return worldState(player.level().getServer()).members(player.getUUID());
    }

    private static boolean invite(ServerPlayer player, ServerPlayer target) {
        if (player == target) return fail(player, "message.augmentation_survival.storage.invite_self");
        if (!hasRemoteStorage(player) || !hasRemoteStorage(target)) {
            return fail(player, "message.augmentation_survival.storage.remote_required");
        }
        StorageWorldState state = worldState(player.level().getServer());
        if (state.groupId(player.getUUID()).isPresent()
                && state.groupId(player.getUUID()).equals(state.groupId(target.getUUID()))) {
            return fail(player, "message.augmentation_survival.storage.already_shared");
        }
        if (state.members(player.getUUID()).size() > 1 && state.members(target.getUUID()).size() > 1) {
            return fail(player, "message.augmentation_survival.storage.group_merge_blocked");
        }
        long expires = player.level().getServer().overworld().getGameTime() + INVITE_LIFETIME_TICKS;
        PENDING_INVITES.put(target.getUUID(), new PendingInvite(player.getUUID(), expires));
        player.sendSystemMessage(Component.translatable(
                "message.augmentation_survival.storage.invite_sent", target.getScoreboardName()));
        target.sendSystemMessage(Component.translatable(
                "message.augmentation_survival.storage.invite_received", player.getScoreboardName()));
        return true;
    }

    private static boolean acceptInvite(ServerPlayer player) {
        PendingInvite invite = PENDING_INVITES.remove(player.getUUID());
        long now = player.level().getServer().overworld().getGameTime();
        if (invite == null || invite.expiresAtTick < now) {
            return fail(player, "message.augmentation_survival.storage.no_invite");
        }
        MinecraftServer server = player.level().getServer();
        StorageWorldState state = worldState(server);
        if (!hasRemoteStorage(player)) return fail(player, "message.augmentation_survival.storage.remote_required");
        ServerPlayer inviter = server.getPlayerList().getPlayer(invite.inviterId);
        if (inviter != null && !hasRemoteStorage(inviter)) {
            return fail(player, "message.augmentation_survival.storage.remote_required");
        }
        if (state.members(player.getUUID()).size() > 1 && state.members(invite.inviterId).size() > 1) {
            return fail(player, "message.augmentation_survival.storage.group_merge_blocked");
        }

        Set<UUID> affected = new HashSet<>(state.members(player.getUUID()));
        affected.addAll(state.members(invite.inviterId));
        String groupId = state.groupId(invite.inviterId)
                .or(() -> state.groupId(player.getUUID()))
                .orElseGet(() -> UUID.randomUUID().toString());
        setWorldState(server, state.mergeGroups(invite.inviterId, player.getUUID(), groupId));
        closeMenusFor(affected);
        player.sendSystemMessage(Component.translatable("message.augmentation_survival.storage.shared"));
        if (inviter != null) inviter.sendSystemMessage(Component.translatable("message.augmentation_survival.storage.shared"));
        return true;
    }

    private static boolean leaveGroup(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        StorageWorldState state = worldState(server);
        if (state.groupId(player.getUUID()).isEmpty()) {
            return fail(player, "message.augmentation_survival.storage.not_shared");
        }
        Set<UUID> affected = new HashSet<>(state.members(player.getUUID()));
        setWorldState(server, state.leaveGroup(player.getUUID()));
        closeMenusFor(affected);
        player.sendSystemMessage(Component.translatable("message.augmentation_survival.storage.left_group"));
        return true;
    }

    private static boolean fail(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key));
        return false;
    }

    private static void closeMenusFor(Set<UUID> owners) {
        for (AugmentStorageMenu menu : List.copyOf(OPEN_MENUS)) {
            if (!owners.contains(menu.ownerId())) continue;
            ServerPlayer owner = menu.serverOwner();
            if (owner != null && owner.containerMenu == menu) owner.closeContainer();
        }
    }

    private static void handleActualPlayerDeath(ServerPlayer player) {
        if (player.level().getGameRules().get(GameRules.KEEP_INVENTORY)
                || DeathPreservationAugmentService.shouldKeepInventory(player)) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        PersistentSegmentContainer pocket = segment(server, player.getUUID(), SegmentKind.POCKET);
        if (!(player.level() instanceof ServerLevel level)) return;
        boolean changed = false;
        for (int slot = 0; slot < pocket.getContainerSize(); slot++) {
            ItemStack stack = pocket.removeItemNoUpdate(slot);
            if (stack.isEmpty()) continue;
            ItemEntity dropped = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), stack);
            dropped.setDefaultPickUpDelay();
            level.addFreshEntity(dropped);
            changed = true;
        }
        if (changed) pocket.setChanged();
    }

    private static PersistentSegmentContainer segment(MinecraftServer server, UUID owner, SegmentKind kind) {
        StorageKey key = new StorageKey(owner, kind);
        return SEGMENTS.computeIfAbsent(key, ignored -> new PersistentSegmentContainer(
                server,
                owner,
                kind,
                kind == SegmentKind.POCKET
                        ? worldState(server).pocketSegment(owner)
                        : worldState(server).remoteSegment(owner)
        ));
    }

    private static void persist(PersistentSegmentContainer container) {
        MinecraftServer server = container.server;
        ArrayList<ItemStack> snapshot = new ArrayList<>(container.getContainerSize());
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            snapshot.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        StorageWorldState state = worldState(server);
        state = container.kind == SegmentKind.POCKET
                ? state.withPocketSegment(container.owner, snapshot)
                : state.withRemoteSegment(container.owner, snapshot);
        setWorldState(server, state);
        for (AugmentStorageMenu menu : List.copyOf(OPEN_MENUS)) {
            if (menu.referencesSegment(container.kind, container.owner)) menu.broadcastChanges();
        }
    }

    private static StorageWorldState worldState(MinecraftServer server) {
        return server.overworld().getAttachedOrSet(StorageWorldAttachments.STATE, StorageWorldState.initial());
    }

    private static void setWorldState(MinecraftServer server, StorageWorldState state) {
        server.overworld().setAttached(StorageWorldAttachments.STATE, state);
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    public enum SegmentKind { POCKET, REMOTE }

    private record StorageKey(UUID owner, SegmentKind kind) { }
    private record PendingInvite(UUID inviterId, long expiresAtTick) { }

    private static final class PersistentSegmentContainer extends SimpleContainer {
        private final MinecraftServer server;
        private final UUID owner;
        private final SegmentKind kind;
        private boolean loading = true;

        private PersistentSegmentContainer(
                MinecraftServer server,
                UUID owner,
                SegmentKind kind,
                List<ItemStack> initial
        ) {
            super(kind == SegmentKind.POCKET ? StorageWorldState.POCKET_SIZE : StorageWorldState.REMOTE_SEGMENT_SIZE);
            this.server = server;
            this.owner = owner;
            this.kind = kind;
            for (int slot = 0; slot < getContainerSize() && slot < initial.size(); slot++) {
                ItemStack stack = initial.get(slot);
                super.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            }
            loading = false;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!loading) persist(this);
        }
    }

    /** 54-slot paged view that delegates every mutation to the owners' canonical 27-slot segments. */
    public static final class RemoteView implements Container {
        private static final int PAGE_SIZE = 54;
        private final ServerPlayer viewer;
        private int page;

        private RemoteView(ServerPlayer viewer) {
            this.viewer = viewer;
        }

        public int page() { return page; }
        public int memberCount() { return remoteMembers(viewer).size(); }
        public int pageCount() {
            int members = memberCount();
            return members <= 0 ? 0 : Math.max(1, (members + 1) / 2);
        }
        public void setPage(int page) {
            int pages = pageCount();
            this.page = pages <= 0 ? 0 : Math.max(0, Math.min(page, pages - 1));
        }
        public boolean slotActive(int slot) { return resolve(slot) != null; }
        public boolean references(UUID owner) { return remoteMembers(viewer).contains(owner); }

        @Override public int getContainerSize() { return PAGE_SIZE; }
        @Override public boolean isEmpty() {
            for (int slot = 0; slot < PAGE_SIZE; slot++) if (!getItem(slot).isEmpty()) return false;
            return true;
        }
        @Override public ItemStack getItem(int slot) {
            ResolvedSlot resolved = resolve(slot);
            return resolved == null ? ItemStack.EMPTY : resolved.container.getItem(resolved.slot);
        }
        @Override public ItemStack removeItem(int slot, int amount) {
            ResolvedSlot resolved = resolve(slot);
            return resolved == null ? ItemStack.EMPTY : resolved.container.removeItem(resolved.slot, amount);
        }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            ResolvedSlot resolved = resolve(slot);
            return resolved == null ? ItemStack.EMPTY : resolved.container.removeItemNoUpdate(resolved.slot);
        }
        @Override public void setItem(int slot, ItemStack stack) {
            ResolvedSlot resolved = resolve(slot);
            if (resolved != null) resolved.container.setItem(resolved.slot, stack);
        }
        @Override public void setChanged() { }
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) {
            return player == viewer && !viewer.isDeadOrDying() && hasRemoteStorage(viewer);
        }
        @Override public void clearContent() {
            for (int slot = 0; slot < PAGE_SIZE; slot++) {
                ResolvedSlot resolved = resolve(slot);
                if (resolved != null) resolved.container.setItem(resolved.slot, ItemStack.EMPTY);
            }
        }

        private ResolvedSlot resolve(int slot) {
            if (slot < 0 || slot >= PAGE_SIZE || !hasRemoteStorage(viewer)) return null;
            List<UUID> members = remoteMembers(viewer);
            int global = page * PAGE_SIZE + slot;
            int memberIndex = global / StorageWorldState.REMOTE_SEGMENT_SIZE;
            if (memberIndex < 0 || memberIndex >= members.size()) return null;
            int inner = global % StorageWorldState.REMOTE_SEGMENT_SIZE;
            PersistentSegmentContainer container = segment(
                    viewer.level().getServer(), members.get(memberIndex), SegmentKind.REMOTE);
            return new ResolvedSlot(container, inner);
        }

        private record ResolvedSlot(PersistentSegmentContainer container, int slot) { }
    }
}

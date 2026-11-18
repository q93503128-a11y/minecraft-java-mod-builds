package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Explicit two-step server confirmation; no offerings are charged for inspection. */
public final class PactService {
    private static final long OFFER_DURATION_MS = 60_000L;
    private enum Action { SIGN, RELEASE }
    private record Pending(String id, Action action, int count, int revision, long expires) {}
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private PactService() {}

    public static boolean isPact(ServerPlayer player) {
        return player.getServer() != null && player.isAlive()
                && ClassProfileSavedData.get(player.getServer()).profile(player.getUUID())
                .currentClass().filter(id -> id == ClassId.PACT_CASTER).isPresent();
    }
    public static void status(ServerPlayer player) {
        if (!isPact(player)) {
            say(player, "message.anomalousclasses.pact.wrong_class");
            return;
        }
        var state = PactSavedData.get(player.getServer()).player(player.getUUID());
        if (state.count() == 0) {
            say(player, "message.anomalousclasses.pact.no_contract");
        } else for (var bond : state.bonds()) {
            PactCatalog.Patron patron = PactCatalog.get(bond.id());
            player.sendSystemMessage(Component.translatable("message.anomalousclasses.pact.status",
                    patron.name(), bond.relation(), bond.debt()));
        }
        for (var patron : PactCatalog.introductory())
            player.sendSystemMessage(Component.translatable("message.anomalousclasses.pact.available",
                    patron.name(), patron.id()));
        say(player, "message.anomalousclasses.pact.contact_hint");
    }
    public static void previewSign(ServerPlayer player, String id) {
        PENDING.remove(player.getUUID()); // Invalid second previews must invalidate any older consent.
        if (!isPact(player) || CombatTracker.isLocked(player)) {
            say(player, "message.anomalousclasses.pact.unavailable"); return;
        }
        PactCatalog.Patron patron = PactCatalog.get(id);
        if (patron == null || !patron.accessible()) {
            say(player, "message.anomalousclasses.pact.altar_required"); return;
        }
        PactSavedData.Player state = PactSavedData.get(player.getServer()).player(player.getUUID());
        if (state.has(id)) {
            say(player, "message.anomalousclasses.pact.already_signed"); return;
        }
        quote(player, patron, state.count(), false);
        PENDING.put(player.getUUID(), new Pending(id, Action.SIGN, state.count(),
                state.revision(), System.currentTimeMillis() + OFFER_DURATION_MS));
    }
    public static void previewRelease(ServerPlayer player, String id) {
        PENDING.remove(player.getUUID());
        if (!isPact(player) || CombatTracker.isLocked(player)) {
            say(player, "message.anomalousclasses.pact.unavailable"); return;
        }
        PactCatalog.Patron patron = PactCatalog.get(id);
        var state = PactSavedData.get(player.getServer()).player(player.getUUID());
        if (patron == null || !state.has(id)) {
            say(player, "message.anomalousclasses.pact.not_signed"); return;
        }
        if (state.bond(id).debt() > 0) {
            say(player, "message.anomalousclasses.pact.debt_due", state.bond(id).debt());
            return;
        }
        quote(player, patron, state.count(), true);
        PENDING.put(player.getUUID(), new Pending(id, Action.RELEASE, state.count(),
                state.revision(), System.currentTimeMillis() + OFFER_DURATION_MS));
    }
    private static void quote(ServerPlayer player, PactCatalog.Patron patron,
                              int active, boolean release) {
        say(player, release ? "message.anomalousclasses.pact.release_preview"
                : "message.anomalousclasses.pact.sign_preview",
                patron.name(), release ? 1 : PactCatalog.multiplier(active));
        for (var item : release ? patron.release() : patron.offering()) {
            long amount = release ? item.count() : PactCatalog.required(item, active);
            player.sendSystemMessage(Component.translatable("message.anomalousclasses.pact.material",
                    amount, Component.translatable(item.item().getDescriptionId())));
        }
        if (!release) {
            if (patron.hpSacrifice() > 0)
                say(player, "message.anomalousclasses.pact.health_sacrifice", patron.hpSacrifice());
            say(player, "message.anomalousclasses.pact.blessing",
                    (int) Math.round(patron.health() * 100), (int) Math.round(patron.attack() * 100),
                    (int) Math.round(patron.speed() * 100), (int) Math.round(patron.mining() * 100));
            for (var ability : patron.abilities())
                player.sendSystemMessage(Component.translatable(
                        "pact_ability.anomalousclasses." + ability.id()));
            say(player, "pact_terms.anomalousclasses." + patron.id());
            say(player, "message.anomalousclasses.pact.release_cost");
            for (var material : patron.release())
                player.sendSystemMessage(Component.translatable(
                        "message.anomalousclasses.pact.material",
                        material.count(), Component.translatable(material.item().getDescriptionId())));
            if ((patron.id().equals("isael") && PactSavedData.get(player.getServer()).player(player.getUUID()).has("velkan"))
                    || (patron.id().equals("velkan")
                    && PactSavedData.get(player.getServer()).player(player.getUUID()).has("isael")))
                say(player, "message.anomalousclasses.pact.conflict");
        }
        say(player, "message.anomalousclasses.pact.confirm_hint");
    }
    public static void confirm(ServerPlayer player) {
        Pending pending = PENDING.remove(player.getUUID());
        if (pending == null || !isPact(player) || CombatTracker.isLocked(player)
                || System.currentTimeMillis() > pending.expires()) {
            say(player, "message.anomalousclasses.pact.expired"); return;
        }
        PactCatalog.Patron patron = PactCatalog.get(pending.id());
        if (patron == null) return;
        PactSavedData saved = PactSavedData.get(player.getServer());
        PactSavedData.Player state = saved.player(player.getUUID());
        if (pending.revision() != state.revision() || pending.count() != state.count()) {
            say(player, "message.anomalousclasses.pact.changed"); return;
        }
        boolean release = pending.action() == Action.RELEASE;
        if (release ? (!state.has(patron.id()) || state.bond(patron.id()).debt() > 0)
                : (!patron.accessible() || state.has(patron.id()))) {
            say(player, "message.anomalousclasses.pact.changed"); return;
        }
        long hpCost = release ? 0 : (long) Math.ceil(patron.hpSacrifice());
        if (hpCost > 0 && player.getHealth() <= hpCost + 1.0) {
            say(player, "message.anomalousclasses.pact.need_health"); return;
        }
        var offering = release ? patron.release() : patron.offering();
        int multiplierCount = release ? 0 : state.count();
        if (!canPay(player.getInventory(), offering, multiplierCount)) {
            say(player, "message.anomalousclasses.pact.need_material"); return;
        }
        // Inventory scan and consume run as one server-thread operation, then mutate SavedData.
        pay(player.getInventory(), offering, multiplierCount);
        if (hpCost > 0) player.setHealth(player.getHealth() - hpCost);
        if (release) {
            saved.breakBond(player.getUUID(), patron.id());
            PactCombatService.removeBond(player, patron.id());
        } else {
            saved.sign(player.getUUID(), patron.id());
            if ((patron.id().equals("isael") && state.has("velkan"))
                    || (patron.id().equals("velkan") && state.has("isael"))) {
                saved.relation(player.getUUID(),"isael",-10);
                say(player,"message.anomalousclasses.pact.conflict");
            }
        }
        PactBlessingService.apply(player);
        say(player, release ? "message.anomalousclasses.pact.released"
                : "message.anomalousclasses.pact.signed", patron.name());
        if (!release) RpgExperienceService.awardFirst(player, "pact_signed:" + patron.id(), 1_200);
    }
    private static boolean canPay(Inventory inventory, java.util.List<PactCatalog.Material> offering,
                                   int count) {
        for (var material : offering) {
            long required = PactCatalog.required(material, count), found = 0L;
            Item item = material.item();
            for (ItemStack stack : inventory.items) if (stack.is(item)) found += stack.getCount();
            if (found < required) return false;
        }
        return true;
    }
    private static void pay(Inventory inventory, java.util.List<PactCatalog.Material> offering,
                             int count) {
        for (var material : offering) {
            long required = PactCatalog.required(material, count);
            for (int i=0; i<inventory.items.size() && required>0; i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.is(material.item())) continue;
                int taken = (int) Math.min(stack.getCount(), required);
                stack.shrink(taken);
                required -= taken;
            }
        }
        inventory.setChanged();
    }
    public static void expire(ServerPlayer player) { PENDING.remove(player.getUUID()); }
    private static void say(ServerPlayer player, String key, Object... arguments) {
        player.sendSystemMessage(Component.translatable(key, arguments));
    }
}

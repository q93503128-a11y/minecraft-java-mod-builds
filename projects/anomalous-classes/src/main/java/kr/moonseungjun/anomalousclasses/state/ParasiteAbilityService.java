package kr.moonseungjun.anomalousclasses.state;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/** Three separate common skills; no client-provided heal/damage/cooldown values. */
public final class ParasiteAbilityService {
    private ParasiteAbilityService() {}

    public static void use(ServerPlayer owner, int slot) {
        if (!ParasiteService.current(owner) || !owner.isAlive() || slot < 0 || slot > 2) return;
        ParasiteAbilitySavedData storage = ParasiteAbilitySavedData.get(owner.getServer());
        long now = owner.serverLevel().getGameTime();
        if (!storage.ready(owner.getUUID(), slot, now)) {
            long remaining = storage.player(owner.getUUID()).readyAt(slot) - now;
            owner.displayClientMessage(Component.translatable(
                    "message.anomalousclasses.parasite_ability.cooldown",
                    Math.max(1L, (remaining + 19L) / 20L)), true);
            return;
        }

        if (slot == 0) {
            if (!ParasiteService.canPrepareIntrusion(owner)) {
                owner.displayClientMessage(Component.translatable(
                        "message.anomalousclasses.parasite_ability.failed_0"), true);
                return;
            }
            if (storage.anchor(owner.getUUID(), now))
                success(owner, 0);
            return;
        }
        Mob host = ParasiteService.activeHost(owner);
        if (host == null || !host.isAlive()) {
            owner.displayClientMessage(Component.translatable(
                    "message.anomalousclasses.parasite_ability.no_host"), true);
            return;
        }
        if (slot == 1) {
            float deficit = host.getMaxHealth() - host.getHealth();
            if (deficit <= 0.0F) {
                owner.displayClientMessage(Component.translatable(
                        "message.anomalousclasses.parasite_ability.full_health"), true);
                return;
            }
            var state = ParasiteSavedData.get(owner.getServer()).player(owner.getUUID());
            PlayerClassProfile profile = ClassProfileSavedData.get(owner.getServer()).profile(owner.getUUID());
            // Gradual repair: capped by both host size and the permanent BD growth curve.
            float total = (float) Math.min(0.25 * host.getMaxHealth(),
                    4.0 + .5 * Math.sqrt(state.bd()));
            total *= 1.0F + 0.005F * ParasiteRules.rank(profile, "self_repair");
            if (storage.heal(owner.getUUID(), host.getUUID(), Math.min(deficit, total), now))
                success(owner, 1);
            return;
        }
        if (storage.awaken(owner.getUUID(), host.getUUID(), now)) success(owner, 2);
    }

    public static void tick(ServerPlayer owner, Mob host) {
        var storage = ParasiteAbilitySavedData.get(owner.getServer());
        float healed = storage.healTick(owner.getUUID(), host.getUUID(),
                owner.serverLevel().getGameTime(), Math.max(0, host.getMaxHealth() - host.getHealth()));
        if (healed > 0.0F && host.isAlive()) host.heal(healed);
    }
    public static boolean awakened(ServerPlayer owner, Mob host) {
        return owner != null && host != null
                && ParasiteAbilitySavedData.get(owner.getServer()).isAwakened(
                        owner.getUUID(), host.getUUID(), owner.serverLevel().getGameTime());
    }
    public static void interruptOnDamage(ServerPlayer owner, Mob host, float damage) {
        if (damage <= 2.0F) return;
        ParasiteAbilitySavedData.get(owner.getServer()).stopHealing(owner.getUUID(), host.getUUID());
    }
    public static void endHost(ServerPlayer owner, Mob host) {
        if (owner == null || host == null) return;
        ParasiteAbilitySavedData.get(owner.getServer()).endHost(owner.getUUID(), host.getUUID());
    }
    public static boolean takeAnchoring(ServerPlayer owner) {
        return ParasiteAbilitySavedData.get(owner.getServer())
                .consumeAnchor(owner.getUUID(), owner.serverLevel().getGameTime());
    }
    private static void success(ServerPlayer owner, int slot) {
        owner.displayClientMessage(Component.translatable(
                "message.anomalousclasses.parasite_ability.success_" + slot), true);
    }
}

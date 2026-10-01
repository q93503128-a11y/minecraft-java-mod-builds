package io.github.q93503128.turnbound.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.HashSet;
import java.util.Set;

/** Applies server-authorized quest outlines locally so another player's tracked target never leaks into this client. */
public final class ClientQuestTargetOutlineState {
    private static final Set<Integer> DESIRED = new HashSet<>();
    private static final Set<Integer> APPLIED = new HashSet<>();
    private static ClientLevel lastLevel;

    private ClientQuestTargetOutlineState() {}

    public static void update(String encoded) {
        DESIRED.clear();
        if (encoded == null || encoded.isBlank()) return;
        for (String token : encoded.split(",")) {
            try {
                int id = Integer.parseInt(token.trim());
                if (id > 0) DESIRED.add(id);
            } catch (NumberFormatException ignored) { }
        }
    }

    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != lastLevel) {
            clearApplied(lastLevel);
            APPLIED.clear();
            DESIRED.clear();
            lastLevel = level;
        }
        if (level == null || minecraft.player == null) return;

        for (int id : Set.copyOf(APPLIED)) {
            if (DESIRED.contains(id)) continue;
            Entity entity = level.getEntity(id);
            if (entity != null) entity.setGlowingTag(false);
            APPLIED.remove(id);
        }
        for (int id : DESIRED) {
            Entity entity = level.getEntity(id);
            if (entity == null || entity == minecraft.player) continue;
            entity.setGlowingTag(true);
            APPLIED.add(id);
        }
    }

    private static void clearApplied(ClientLevel level) {
        if (level == null) return;
        for (int id : APPLIED) {
            Entity entity = level.getEntity(id);
            if (entity != null) entity.setGlowingTag(false);
        }
    }
}

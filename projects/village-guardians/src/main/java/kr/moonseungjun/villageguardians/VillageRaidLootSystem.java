package kr.moonseungjun.villageguardians;

import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

public final class VillageRaidLootSystem {
    private VillageRaidLootSystem() {}

    public static void handleDrops(LivingDropsEvent event) {
        if (!VillageRaidSystem.isRaidEnemy(event.getEntity())) return;
        event.getDrops().clear();
    }
}

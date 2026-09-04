from pathlib import Path

pkg = Path('projects/survival-ascension/src/main/java/kr/moonseungjun/survivalascension')

p = pkg / 'production/EncounterInterruptionData.java'
s = p.read_text(encoding='utf-8')
marker = 'import net.minecraft.server.MinecraftServer;\n'
assert marker in s
s = s.replace(marker, marker + 'import net.minecraft.server.level.ServerLevel;\n', 1)
old = '        if (event.getEntity() instanceof ServerPlayer player) get(player.getServer()).claim(player);\n'
new = '        if (event.getEntity() instanceof ServerPlayer player) get(((ServerLevel) player.level()).getServer()).claim(player);\n'
assert old in s
p.write_text(s.replace(old, new, 1), encoding='utf-8')

# Keep an active defense for the existing owner-grace window on ordinary disconnect.
# Integrated Save & Quit then reaches ServerStoppingEvent with the paid defense still registered,
# while a dedicated-server disconnect simply times out as a normal failure with no refund.
p = pkg / 'production/OutpostSiegeSystem.java'
s = p.read_text(encoding='utf-8')
old = """    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Siege siege = ACTIVE.remove(event.getEntity().getUUID());
        if (siege != null) {
            // Normal disconnects remain a failed defense. During an actual server shutdown the
            // stopping event normally queues this first; this fallback covers alternate shutdown order.
            if (!siege.level.getServer().isRunning()) {
                EncounterInterruptionData.get(siege.level.getServer()).queueSiege(
                        siege.owner, siege.mode == SiegeMode.BASTION);
            }
            cleanupMobs(siege);
            closeBossBar(siege);
        }
    }
"""
new = """    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Siege siege = ACTIVE.get(event.getEntity().getUUID());
        if (siege != null && event.getEntity() instanceof ServerPlayer player) {
            siege.bossBar.removePlayer(player);
        }
    }
"""
assert old in s
p.write_text(s.replace(old, new, 1), encoding='utf-8')

print('SURVIVAL_AUDIT5_MAPPING_AND_LOGOUT_FIX_APPLIED')

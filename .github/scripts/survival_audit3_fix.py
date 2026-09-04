from pathlib import Path
import re

root = Path('projects/survival-ascension')
mobility = root / 'src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java'
main = root / 'src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java'
tbs = root / 'src/main/java/kr/moonseungjun/survivalascension/compat/TbsJournalRestorationService.java'
props = root / 'gradle.properties'

text = mobility.read_text(encoding='utf-8')
old = 'import net.neoforged.neoforge.event.entity.player.PlayerEvent;\nimport net.neoforged.neoforge.event.tick.PlayerTickEvent;'
new = 'import net.neoforged.neoforge.event.entity.player.PlayerEvent;\nimport net.neoforged.neoforge.event.server.ServerStoppingEvent;\nimport net.neoforged.neoforge.event.tick.PlayerTickEvent;'
assert old in text
text = text.replace(old, new, 1)
old = '''    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Client state can survive an integrated-server/world switch; explicitly clear it on join.
            SkillNetwork.sendMobilityCooldown(player, new MobilityCooldownPayload(0));
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        TRAVERSAL.remove(uuid);
        DASH_READY_TICK.remove(uuid);
        AIR_DASH_COUNT.remove(uuid);
    }
'''
new = '''    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // A dedicated-server relog must not reset a live dash cooldown. Client state can survive
        // world switches, so always send the authoritative server value on join.
        if (DASH_READY_TICK.containsKey(player.getUUID())) syncDashCooldown(player);
        else SkillNetwork.sendMobilityCooldown(player, new MobilityCooldownPayload(0));
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        // Traversal distance is session-local, but cooldown and airborne quota are gameplay state.
        // Keep those two until landing or actual server shutdown so relogging cannot refresh a dash.
        TRAVERSAL.remove(uuid);
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        TRAVERSAL.clear();
        DASH_READY_TICK.clear();
        AIR_DASH_COUNT.clear();
    }
'''
assert old in text
mobility.write_text(text.replace(old, new, 1), encoding='utf-8')

text = tbs.read_text(encoding='utf-8')
old = 'import net.neoforged.neoforge.event.entity.player.PlayerEvent;\nimport net.neoforged.neoforge.event.tick.PlayerTickEvent;'
new = 'import net.neoforged.neoforge.event.entity.player.PlayerEvent;\nimport net.neoforged.neoforge.event.server.ServerStoppingEvent;\nimport net.neoforged.neoforge.event.tick.PlayerTickEvent;'
assert old in text
text = text.replace(old, new, 1)
marker = '    public static void onPlayerTick(PlayerTickEvent.Post event) {\n'
insert = '    public static void onServerStopping(ServerStoppingEvent event) {\n        READY_AT.clear();\n    }\n\n'
assert marker in text and insert not in text
tbs.write_text(text.replace(marker, insert + marker, 1), encoding='utf-8')

text = main.read_text(encoding='utf-8')
assert 'public static final String VERSION = "0.61.1-alpha.1";' in text
text = text.replace('public static final String VERSION = "0.61.1-alpha.1";', 'public static final String VERSION = "0.61.2-alpha.1";', 1)
marker = '        NeoForge.EVENT_BUS.addListener(MobilityProgression::onPlayerLoggedOut);\n'
assert marker in text
text = text.replace(marker, marker + '        NeoForge.EVENT_BUS.addListener(MobilityProgression::onServerStopping);\n', 1)
marker = '        NeoForge.EVENT_BUS.addListener(TbsJournalRestorationService::onPlayerTick);\n'
assert marker in text
text = text.replace(marker, marker + '        NeoForge.EVENT_BUS.addListener(TbsJournalRestorationService::onServerStopping);\n', 1)
main.write_text(text, encoding='utf-8')

text = props.read_text(encoding='utf-8')
assert 'mod_version=0.61.1-alpha.1' in text
props.write_text(text.replace('mod_version=0.61.1-alpha.1', 'mod_version=0.61.2-alpha.1', 1), encoding='utf-8')

print('SURVIVAL_AUDIT3_FIX_APPLIED')
print('MUTABLE_RUNTIME_STATE_INVENTORY')
pat = re.compile(r'private\s+static\s+final\s+(?:Map|Set|List)<')
for java_root in (Path('projects/survival-ascension/src/main/java'), Path('projects/frontier-settlement/src/main/java')):
    for p in sorted(java_root.rglob('*.java')):
        s = p.read_text(encoding='utf-8')
        hits = len(pat.findall(s))
        if hits:
            lifecycle = ('onServerStopping' in s) or ('onPlayerLoggedOut' in s)
            print(f'{p}: containers={hits} lifecycle_cleanup={lifecycle}')

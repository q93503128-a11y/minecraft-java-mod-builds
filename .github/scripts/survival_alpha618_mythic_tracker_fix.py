from pathlib import Path

root = Path('projects/survival-ascension')

# Version bump.
gp = root / 'gradle.properties'
s = gp.read_text(encoding='utf-8')
if 'mod_version=0.61.7-alpha.1' not in s:
    raise SystemExit('unexpected Survival version')
s = s.replace('mod_version=0.61.7-alpha.1', 'mod_version=0.61.8-alpha.1', 1)
gp.write_text(s, encoding='utf-8')

main = root / 'src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java'
s = main.read_text(encoding='utf-8')
s = s.replace('public static final String VERSION = "0.61.7-alpha.1";', 'public static final String VERSION = "0.61.8-alpha.1";', 1)
anchor = '        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onServerTick);\n'
if anchor not in s:
    raise SystemExit('Elite listener anchor missing')
s = s.replace(anchor, anchor + '        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onPlayerLoggedIn);\n        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onPlayerRespawn);\n        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onPlayerChangedDimension);\n', 1)
main.write_text(s, encoding='utf-8')

payload = root / 'src/main/java/kr/moonseungjun/survivalascension/network/MythicTargetPayload.java'
payload.write_text('''package kr.moonseungjun.survivalascension.network;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record MythicTargetPayload(boolean active, UUID targetId, double x, double z) implements CustomPacketPayload {
    private static final UUID NONE = new UUID(0L, 0L);
    public static final Type<MythicTargetPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "mythic_target"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MythicTargetPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.active());
                UUID id = payload.targetId() == null ? NONE : payload.targetId();
                buf.writeLong(id.getMostSignificantBits());
                buf.writeLong(id.getLeastSignificantBits());
                buf.writeDouble(payload.x());
                buf.writeDouble(payload.z());
            },
            buf -> new MythicTargetPayload(
                    buf.readBoolean(),
                    new UUID(buf.readLong(), buf.readLong()),
                    buf.readDouble(),
                    buf.readDouble()));

    public static MythicTargetPayload target(UUID targetId, double x, double z) {
        return new MythicTargetPayload(true, targetId, x, z);
    }

    public static MythicTargetPayload clear() {
        return new MythicTargetPayload(false, NONE, 0.0D, 0.0D);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
''', encoding='utf-8')

client_state = root / 'src/main/java/kr/moonseungjun/survivalascension/client/ClientMythicState.java'
client_state.write_text('''package kr.moonseungjun.survivalascension.client;

import kr.moonseungjun.survivalascension.network.MythicTargetPayload;

import java.util.UUID;

public final class ClientMythicState {
    // Explicit server clear packets are authoritative. This is only a failsafe for a severed connection.
    private static final long FAILSAFE_STALE_MILLIS = 10_000L;
    private static volatile Target target;

    private ClientMythicState() {}

    public static void onTarget(MythicTargetPayload payload) {
        if (!payload.active()) {
            target = null;
            return;
        }
        target = new Target(payload.targetId(), payload.x(), payload.z(), System.currentTimeMillis());
    }

    public static void clear() {
        target = null;
    }

    public static Target current() {
        Target value = target;
        if (value == null) return null;
        if (System.currentTimeMillis() - value.updatedAtMillis() > FAILSAFE_STALE_MILLIS) {
            target = null;
            return null;
        }
        return value;
    }

    public record Target(UUID targetId, double x, double z, long updatedAtMillis) {}
}
''', encoding='utf-8')

hud = root / 'src/main/java/kr/moonseungjun/survivalascension/client/SkillHudOverlay.java'
s = hud.read_text(encoding='utf-8')
old = '''        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double relative = Mth.wrapDegrees(targetYaw - minecraft.player.getYRot());
        String arrow = relativeArrow(relative);
        String label = "신화 III   " + arrow + "   " + distance + "m";
'''
new = '''        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        // Track what the player is actually looking at, not the body yaw used by third-person movement.
        double cameraYaw = minecraft.gameRenderer.getMainCamera().getYRot();
        double relative = Mth.wrapDegrees(targetYaw - cameraYaw);
        String arrow = relativeArrow(relative);
        String label = "신화 III  " + arrow + "  약 " + distance + "m";
'''
if old not in s:
    raise SystemExit('HUD tracker anchor missing')
s = s.replace(old, new, 1)
hud.write_text(s, encoding='utf-8')

client = root / 'src/main/java/kr/moonseungjun/survivalascension/client/SurvivalAscensionClient.java'
s = client.read_text(encoding='utf-8')
old = '''    private static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
'''
new = '''    private static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) ClientMythicState.clear();
'''
if old not in s:
    raise SystemExit('client tick anchor missing')
s = s.replace(old, new, 1)
client.write_text(s, encoding='utf-8')

elite = root / 'src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java'
s = elite.read_text(encoding='utf-8')
s = s.replace('import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;\n', 'import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;\nimport net.neoforged.neoforge.event.entity.player.PlayerEvent;\n', 1)
old = '''    public static void onServerTick(ServerTickEvent.Pre event) {
        if (++mythicTicker < 10) return;
        mythicTicker = 0;
        if (MYTHICS.isEmpty()) return;
        List<UUID> remove = new ArrayList<>();
'''
new = '''    public static void onServerTick(ServerTickEvent.Pre event) {
        if (++mythicTicker < 10) return;
        mythicTicker = 0;
        List<UUID> remove = new ArrayList<>();
'''
if old not in s:
    raise SystemExit('server tick anchor missing')
s = s.replace(old, new, 1)
old = '''            syncMythicBossBar(runtime, mob);
            if (runtime.level.getGameTime() % 20L == 0L) syncMythicTracker(runtime, mob);
            int phase = mob.getPersistentData().getIntOr(MYTHIC_PHASE_KEY, 0);
'''
new = '''            syncMythicBossBar(runtime, mob);
            int phase = mob.getPersistentData().getIntOr(MYTHIC_PHASE_KEY, 0);
'''
if old not in s:
    raise SystemExit('per-runtime tracker anchor missing')
s = s.replace(old, new, 1)
old = '''        for (UUID id : remove) MYTHICS.remove(id);
    }

    public static void onServerStopping(ServerStoppingEvent event) {
'''
new = '''        for (UUID id : remove) MYTHICS.remove(id);
        if (event.getServer().getTickCount() % 20 == 0) {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) syncMythicTracker(player);
        }
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncMythicTracker(player);
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncMythicTracker(player);
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncMythicTracker(player);
    }

    public static void onServerStopping(ServerStoppingEvent event) {
'''
if old not in s:
    raise SystemExit('post tick anchor missing')
s = s.replace(old, new, 1)
old = '''    private static void syncMythicTracker(MythicRuntime runtime, Mob mob) {
        MythicTargetPayload payload = new MythicTargetPayload(mob.getX(), mob.getZ());
        for (ServerPlayer player : playersNear(runtime.level, mob, MYTHIC_ALERT_RADIUS)) {
            SkillNetwork.sendMythicTarget(player, payload);
        }
    }
'''
new = '''    private static void syncMythicTracker(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !(player.level() instanceof ServerLevel level)) {
            SkillNetwork.sendMythicTarget(player, MythicTargetPayload.clear());
            return;
        }
        double maxDistanceSqr = MYTHIC_ALERT_RADIUS * MYTHIC_ALERT_RADIUS;
        Mob nearest = null;
        double nearestDistanceSqr = Double.MAX_VALUE;
        for (Map.Entry<UUID, MythicRuntime> entry : MYTHICS.entrySet()) {
            MythicRuntime runtime = entry.getValue();
            if (runtime.level != level) continue;
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Mob mob) || !mob.isAlive() || rank(mob) != Rank.MYTHIC_III) continue;
            double distanceSqr = player.distanceToSqr(mob);
            if (distanceSqr > maxDistanceSqr) continue;
            if (nearest == null || distanceSqr < nearestDistanceSqr
                    || (distanceSqr == nearestDistanceSqr && mob.getUUID().compareTo(nearest.getUUID()) < 0)) {
                nearest = mob;
                nearestDistanceSqr = distanceSqr;
            }
        }
        SkillNetwork.sendMythicTarget(player, nearest == null
                ? MythicTargetPayload.clear()
                : MythicTargetPayload.target(nearest.getUUID(), nearest.getX(), nearest.getZ()));
    }
'''
if old not in s:
    raise SystemExit('sync tracker method anchor missing')
s = s.replace(old, new, 1)
elite.write_text(s, encoding='utf-8')

network = root / 'src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java'
s = network.read_text(encoding='utf-8').replace('private static final String PROTOCOL = "12";', 'private static final String PROTOCOL = "13";', 1)
network.write_text(s, encoding='utf-8')

# Admin-only deterministic Mythic test command: spawn via the existing vanilla registry spawn API and promote it through the same elite path.
cmd = root / 'src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java'
s = cmd.read_text(encoding='utf-8')
s = s.replace('import kr.moonseungjun.survivalascension.expedition.ExpeditionData;\n', 'import kr.moonseungjun.survivalascension.expedition.ExpeditionData;\nimport kr.moonseungjun.survivalascension.elite.EliteMobSystem;\n', 1)
anchor = '''                .then(Commands.literal("content").executes(context -> showContent(context.getSource().getPlayerOrException())))
'''
if anchor not in s:
    raise SystemExit('command root anchor missing')
s = s.replace(anchor, anchor + '''                .then(Commands.literal("mythic")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("spawn").executes(context -> EliteMobSystem.spawnTestMythic(context.getSource().getPlayerOrException()))))
''', 1)
cmd.write_text(s, encoding='utf-8')

# Add test helper using the same applyElite authority.
s = elite.read_text(encoding='utf-8')
s = s.replace('import net.minecraft.core.particles.ParticleTypes;\n', 'import net.minecraft.core.BlockPos;\nimport net.minecraft.core.particles.ParticleTypes;\nimport net.minecraft.world.entity.EntitySpawnReason;\nimport net.minecraft.world.entity.EntityType;\n', 1)
anchor = '''    public static int rankId(net.minecraft.world.entity.LivingEntity entity) {
        return entity.getPersistentData().getIntOr(RANK_KEY, 0);
    }
'''
helper = '''    public static int rankId(net.minecraft.world.entity.LivingEntity entity) {
        return entity.getPersistentData().getIntOr(RANK_KEY, 0);
    }

    public static int spawnTestMythic(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return 0;
        BlockPos pos = player.blockPosition().relative(player.getDirection(), 10);
        Entity entity = EntityType.ZOMBIE.spawn(level, pos, EntitySpawnReason.COMMAND);
        if (!(entity instanceof Mob mob)) {
            if (entity != null) entity.discard();
            player.sendSystemMessage(Component.literal("§c[신화 테스트] §f좀비 생성에 실패했습니다."));
            return 0;
        }
        applyElite(mob, Rank.MYTHIC_III, Trait.SWIFT, 1);
        syncMythicTracker(player);
        player.sendSystemMessage(Component.literal("§6[신화 테스트] §f정면 약 10블록에 신화 III 좀비를 소환했습니다."));
        return 1;
    }
'''
if anchor not in s:
    raise SystemExit('rank helper anchor missing')
s = s.replace(anchor, helper, 1)
elite.write_text(s, encoding='utf-8')

# Top-of-file changelog entry.
ch = root / 'CHANGELOG.md'
s = ch.read_text(encoding='utf-8')
header = '# Changelog\n\n'
entry = '''## 0.61.8-alpha.1
- Rebuilt the Mythic III tracker as a deterministic server-authoritative per-player target: nearest alive same-dimension Mythic within 192 blocks wins, and explicit clear packets remove dead/out-of-range/dimension-stale targets.
- Mythic target sync now runs periodically and immediately on login, respawn and dimension change instead of depending on a 1.6-second client wall-clock timeout.
- Mythic target packets now carry active state and target UUID in addition to coordinates; protocol bumped from 12 to 13.
- The compact top HUD now uses camera yaw rather than body yaw and shows Mythic identity, eight-way direction and approximate distance.
- Added operator test command `/ascension mythic spawn`, which creates a deterministic Mythic III zombie through the same rank/attribute/runtime path used by normal Mythics.
- No Frontier Settlement construction or residential-integrity behavior changed.

'''
if not s.startswith(header):
    raise SystemExit('changelog header unexpected')
s = header + entry + s[len(header):]
ch.write_text(s, encoding='utf-8')

print('SURVIVAL_ALPHA618_MYTHIC_TRACKER_PATCHED')

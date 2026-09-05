from pathlib import Path
import json

REPO = Path(__file__).resolve().parent
if REPO.name == "tools":
    REPO = REPO.parent


def r(path): return path.read_text(encoding="utf-8")
def w(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")

def one(text, old, new, label):
    n = text.count(old)
    if n != 1: raise RuntimeError(f"{label}: expected 1 match, got {n}")
    return text.replace(old, new, 1)

# Frontier alpha112 -----------------------------------------------------------
F = REPO / "projects/frontier-settlement"
cp = F / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java"
c = r(cp)
# The exterior ring is an optional worker approach lane, not placement authority.
for old, new, label in [
    ("for (int x = -1; x <= width; x++)", "for (int x = 0; x < width; x++)", "x footprint loops"),
    ("for (int z = -1; z <= depth; z++)", "for (int z = 0; z < depth; z++)", "z footprint loops"),
]:
    n = c.count(old)
    if n != 2: raise RuntimeError(f"{label}: expected 2 matches, got {n}")
    c = c.replace(old, new)
c = one(c, "List<GradeCell> result = new ArrayList<>((width + 2) * (depth + 2));",
        "List<GradeCell> result = new ArrayList<>(width * depth);", "grade capacity")
c = one(c, "        if (!isSafeSupplyPosition(level, supplyPosition(origin, type, rotation))) return null;\n        return new Site(origin, terrainSpan, terrainStoneCost);",
        "        return new Site(origin, terrainSpan, terrainStoneCost);", "remove hidden supply veto")
c = one(c,
'''        if (overlapsInfrastructure(data, site.origin(), type, rotation)) {
            return invalidPlacement("선택한 부지가 기존 건물·도로·전초기지 또는 공동 창고와 겹칩니다.");
        }
        ConstructionState gradingPreview = new ConstructionState(''',
'''        if (overlapsInfrastructure(data, site.origin(), type, rotation)) {
            return invalidPlacement("선택한 부지가 기존 건물·도로·전초기지 또는 공동 창고와 겹칩니다.");
        }
        BlockPos supply = supplyPosition(site.origin(), type, rotation);
        if (!isSafeSupplyPosition(level, supply)) {
            return invalidPlacement("현장 자재통 위치가 막혀 있습니다 · "
                    + supply.getX() + ", " + supply.getY() + ", " + supply.getZ()
                    + " · 위치를 조금 옮기거나 R로 회전해 주세요.");
        }
        ConstructionState gradingPreview = new ConstructionState(''', "explicit supply reason")
c = one(c,
'''                return invalidPlacement("건물 주변 1블록까지 부지 정리가 가능한 공간이 필요합니다. 물·보호된 블록·깊은 절벽·미로드 경계를 피해 다시 지정해 주세요.");''',
'''                BlockPos blocked = cell.floor().above();
                return invalidPlacement("실제 건물 부지 안의 정리 칸이 막혀 있습니다 · "
                        + blocked.getX() + ", " + blocked.getY() + ", " + blocked.getZ()
                        + " · 물·보호 블록·깊은 절벽을 확인해 주세요.");''', "grade blocker reason")
c = one(c,
'''        BlockPos origin = new BlockPos(originX, baseY, originZ);

        for (int x = 0; x < width; x++) {''',
'''        BlockPos origin = new BlockPos(originX, baseY, originZ);

        // Only the actual rotated blueprint footprint is placement authority. The exterior
        // one-block ring remains optional worker approach space and must not veto a legal lot.
        for (int x = 0; x < width; x++) {''', "footprint rationale")
w(cp, c)

p = F / "gradle.properties"
s = one(r(p), "mod_version=0.1.0-alpha.111", "mod_version=0.1.0-alpha.112", "Frontier version")
s = s.rstrip() + "\n\n# Alpha.112 placement footprint: validation/grading use only the real rotated footprint; exterior worker approach cells no longer veto placement, and supply/grading blockers are reported explicitly.\n"
w(p, s)

lp = F / "COMPANION_LOCK.json"
lock = json.loads(r(lp)); lock["target"]["frontier_settlement"] = "0.1.0-alpha.112"
note = "Alpha.112 keeps companion pins unchanged. Building site validation/grading now uses only the real rotated footprint; the exterior worker approach ring no longer vetoes legal lots, while the external supply barrel and actual footprint blockers report explicit placement reasons."
if note not in lock["notes"]: lock["notes"].append(note)
w(lp, json.dumps(lock, ensure_ascii=False, indent=2) + "\n")

tp = F / "tools/test_current_source.py"
t = one(r(tp), "mod_version=0.1.0-alpha.111", "mod_version=0.1.0-alpha.112", "Frontier verifier version")
a = 'require("TREE_CANOPY_SEARCH_HEIGHT = 10" in construction and "TREE_CANOPY_SEARCH_RADIUS = 2" in construction, "bounded tree evidence envelope drifted")\n'
t = one(t, a, a + '''require("for (int x = -1; x <= width; x++)" not in construction and "for (int z = -1; z <= depth; z++)" not in construction,
        "hidden exterior site/grading veto ring returned")
require("List<GradeCell> result = new ArrayList<>(width * depth);" in construction, "grading plan is not footprint-only")
require("현장 자재통 위치가 막혀 있습니다" in construction and "실제 건물 부지 안의 정리 칸이 막혀 있습니다" in construction,
        "placement blocker diagnostics missing")
''', "Frontier verifier assertions")
t = one(t, 'print("CURRENT SOURCE CHECK PASS: alpha111 explicit settlement location UX + alpha110 scalable parallel construction crews + prior authority invariants")',
        'print("CURRENT SOURCE CHECK PASS: alpha112 footprint-only placement + blocker diagnostics + alpha111 location UX + prior invariants")', "Frontier verifier print")
w(tp, t)

# Survival Ascension 0.61.16 --------------------------------------------------
S = REPO / "projects/survival-ascension"
J = S / "src/main/java/kr/moonseungjun/survivalascension"

w(J / "network/FractureShrineTargetPayload.java", '''package kr.moonseungjun.survivalascension.network;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FractureShrineTargetPayload(boolean active, boolean exact, int x, int z) implements CustomPacketPayload {
    public static final Type<FractureShrineTargetPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "fracture_shrine_target"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FractureShrineTargetPayload> CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeBoolean(value.active()); buf.writeBoolean(value.exact()); buf.writeInt(value.x()); buf.writeInt(value.z()); },
            buf -> new FractureShrineTargetPayload(buf.readBoolean(), buf.readBoolean(), buf.readInt(), buf.readInt()));
    public static FractureShrineTargetPayload target(boolean exact, int x, int z) { return new FractureShrineTargetPayload(true, exact, x, z); }
    public static FractureShrineTargetPayload clear() { return new FractureShrineTargetPayload(false, false, 0, 0); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
''')

w(J / "client/ClientFractureShrineState.java", '''package kr.moonseungjun.survivalascension.client;

import kr.moonseungjun.survivalascension.network.FractureShrineTargetPayload;

public final class ClientFractureShrineState {
    private static final long STALE_MILLIS = 5_000L;
    private static volatile Target target;
    private ClientFractureShrineState() {}
    public static void onTarget(FractureShrineTargetPayload p) { target = p.active() ? new Target(p.exact(), p.x(), p.z(), System.currentTimeMillis()) : null; }
    public static void clear() { target = null; }
    public static Target current() {
        Target value = target;
        if (value != null && System.currentTimeMillis() - value.updatedAtMillis() > STALE_MILLIS) { target = null; return null; }
        return value;
    }
    public record Target(boolean exact, int x, int z, long updatedAtMillis) {}
}
''')

w(J / "compat/TbosFractureShrineLocator.java", '''package kr.moonseungjun.survivalascension.compat;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.network.FractureShrineTargetPayload;
import kr.moonseungjun.survivalascension.network.SkillNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Soft optional TBOS bridge: no implementation class is linked at compile time and no chunk is force-loaded. */
public final class TbosFractureShrineLocator {
    private static final String TBOS_MOD_ID = "tbos";
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private TbosFractureShrineLocator() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        if (!ModList.get().isLoaded(TBOS_MOD_ID) || player.level() != player.level().getServer().overworld()) {
            SkillNetwork.sendFractureShrineTarget(player, FractureShrineTargetPayload.clear()); return;
        }
        Target target = nearest(player);
        SkillNetwork.sendFractureShrineTarget(player, target == null ? FractureShrineTargetPayload.clear()
                : FractureShrineTargetPayload.target(target.exact(), target.pos().getX(), target.pos().getZ()));
    }

    private static Target nearest(ServerPlayer player) {
        try {
            ServerLevel level = player.level();
            Map<String, Target> targets = new LinkedHashMap<>();
            Class<?> world = Class.forName("com.nightbeam.tbos.world.AdventureWorldManager");
            Object planned = world.getMethod("plannedShrines", ServerLevel.class).invoke(null, level);
            if (planned instanceof List<?> list) for (Object plan : list) put(targets, plan, "target", false);
            Class<?> sites = Class.forName("com.nightbeam.tbos.site.TemporalSiteManager");
            Object data = sites.getMethod("data", ServerLevel.class).invoke(null, level);
            Object built = data.getClass().getMethod("fractureShrines").invoke(data);
            if (built instanceof List<?> list) for (Object placement : list) put(targets, placement, "origin", true);
            BlockPos here = player.blockPosition(); Target best = null; long bestSq = Long.MAX_VALUE;
            for (Target t : targets.values()) {
                long dx = (long)t.pos().getX() - here.getX(), dz = (long)t.pos().getZ() - here.getZ(), sq = dx * dx + dz * dz;
                if (sq < bestSq) { bestSq = sq; best = t; }
            }
            return best;
        } catch (ReflectiveOperationException | LinkageError e) {
            if (WARNED.compareAndSet(false, true)) SurvivalAscension.LOGGER.warn("TBOS Fracture Shrine locator disabled safely", e);
            return null;
        }
    }

    private static void put(Map<String, Target> targets, Object value, String posMethod, boolean exact) throws ReflectiveOperationException {
        Method variantMethod = value.getClass().getMethod("variant");
        Object variant = variantMethod.invoke(value), pos = value.getClass().getMethod(posMethod).invoke(value);
        if (variant != null && pos instanceof BlockPos blockPos) targets.put(variant.toString(), new Target(blockPos.immutable(), exact));
    }
    private record Target(BlockPos pos, boolean exact) {}
}
''')

np = J / "network/SkillNetwork.java"; n = r(np)
n = one(n, 'private static final String PROTOCOL = "14";', 'private static final String PROTOCOL = "15";', "protocol")
n = one(n, '    private static volatile Consumer<MythicTargetPayload> mythicSink = payload -> {};\n',
        '    private static volatile Consumer<MythicTargetPayload> mythicSink = payload -> {};\n    private static volatile Consumer<FractureShrineTargetPayload> fractureShrineSink = payload -> {};\n', "network sink")
n = one(n, '        registrar.playToClient(MythicTargetPayload.TYPE, MythicTargetPayload.CODEC, (payload, context) -> mythicSink.accept(payload));\n',
        '        registrar.playToClient(MythicTargetPayload.TYPE, MythicTargetPayload.CODEC, (payload, context) -> mythicSink.accept(payload));\n        registrar.playToClient(FractureShrineTargetPayload.TYPE, FractureShrineTargetPayload.CODEC, (payload, context) -> fractureShrineSink.accept(payload));\n', "network registration")
n = one(n, '''    public static void installMobilityReceiver(Consumer<MobilityCooldownPayload> cooldowns) {''',
'''    public static void installFractureShrineReceiver(Consumer<FractureShrineTargetPayload> targets) {
        fractureShrineSink = Objects.requireNonNull(targets);
    }

    public static void installMobilityReceiver(Consumer<MobilityCooldownPayload> cooldowns) {''', "receiver")
n = one(n, '    public static void sendMobilityCooldown(ServerPlayer player, MobilityCooldownPayload payload) {',
        '    public static void sendFractureShrineTarget(ServerPlayer player, FractureShrineTargetPayload payload) { PacketDistributor.sendToPlayer(player, payload); }\n    public static void sendMobilityCooldown(ServerPlayer player, MobilityCooldownPayload payload) {', "sender")
w(np, n)

mp = J / "SurvivalAscension.java"; m = r(mp)
m = one(m, 'import kr.moonseungjun.survivalascension.compat.TbsJournalRestorationService;\n',
        'import kr.moonseungjun.survivalascension.compat.TbsJournalRestorationService;\nimport kr.moonseungjun.survivalascension.compat.TbosFractureShrineLocator;\n', "locator import")
m = one(m, 'public static final String VERSION = "0.61.15-alpha.1";', 'public static final String VERSION = "0.61.16-alpha.1";', "source version")
m = one(m, '        NeoForge.EVENT_BUS.addListener(TbsJournalRestorationService::onPlayerTick);\n',
        '        NeoForge.EVENT_BUS.addListener(TbsJournalRestorationService::onPlayerTick);\n        NeoForge.EVENT_BUS.addListener(TbosFractureShrineLocator::onPlayerTick);\n', "locator event")
w(mp, m)

clp = J / "client/SurvivalAscensionClient.java"; cl = r(clp)
cl = one(cl, '        SkillNetwork.installMythicReceiver(ClientMythicState::onTarget);\n',
         '        SkillNetwork.installMythicReceiver(ClientMythicState::onTarget);\n        SkillNetwork.installFractureShrineReceiver(ClientFractureShrineState::onTarget);\n', "client receiver")
cl = one(cl, '            ClientMythicState.clear();\n', '            ClientMythicState.clear();\n            ClientFractureShrineState.clear();\n', "client clear")
w(clp, cl)

hp = J / "client/SkillHudOverlay.java"; h = r(hp)
h = one(h, '        renderMythicTracker(graphics, minecraft);\n', '        renderMythicTracker(graphics, minecraft);\n        renderFractureShrineTracker(graphics, minecraft);\n', "HUD call")
marker = '    private static String relativeArrow(double degrees) {\n'
method = '''    private static void renderFractureShrineTracker(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        ClientFractureShrineState.Target target = ClientFractureShrineState.current();
        if (target == null || minecraft.player == null) return;
        double dx = target.x() - minecraft.player.getX(), dz = target.z() - minecraft.player.getZ();
        int distance = (int)Math.round(Math.sqrt(dx * dx + dz * dz));
        double relative = Mth.wrapDegrees(Math.toDegrees(Math.atan2(-dx, dz)) - minecraft.player.getYRot());
        String label = "균열 성소" + (target.exact() ? "" : " 예상") + "  " + relativeArrow(relative) + "  약 " + distance + "m";
        int width = Math.max(132, minecraft.font.width(label) + 16), left, top;
        boolean mythic = ClientMythicState.current() != null;
        if (graphics.guiWidth() >= 420) { left = Math.max(6, graphics.guiWidth() - width - 8); top = mythic ? 27 : 8; }
        else { left = Math.max(6, (graphics.guiWidth() - width) / 2); top = mythic ? 97 : 78; }
        graphics.fill(left - 1, top - 1, left + width + 1, top + 14, target.exact() ? 0xFF6BA8A0 : 0xC0607774);
        graphics.fill(left, top, left + width, top + 13, 0xD4101717);
        graphics.text(minecraft.font, label, left + (width - minecraft.font.width(label)) / 2, top + 2, 0xFFE2FFF8, true);
    }

'''
h = one(h, marker, method + marker, "HUD method"); w(hp, h)

sp = S / "gradle.properties"; w(sp, one(r(sp), "mod_version=0.61.15-alpha.1", "mod_version=0.61.16-alpha.1", "SA version"))
chp = S / "CHANGELOG.md"; ch = r(chp)
entry = '''## 0.61.16-alpha.1
- Added a soft TBOS Fracture Shrine direction/distance tracker. Built shrine SavedData gives exact coordinates; unopened shrine plans are labeled `예상` until TBOS resolves the final dry-surface placement.
- The bridge is reflection-only, never force-loads or materializes shrine chunks, and clears itself when `tbos` is absent. Network protocol is now 15; combat/progression rules are unchanged.

'''
if "## 0.61.16-alpha.1" not in ch: ch = one(ch, "# Changelog\n\n", "# Changelog\n\n" + entry, "changelog")
w(chp, ch)

stp = S / "tools/test_current_source.py"; st = r(stp)
st = one(st, "mod_version=0.61.15-alpha.1", "mod_version=0.61.16-alpha.1", "SA verifier version")
st = one(st, 'VERSION = "0.61.15-alpha.1"', 'VERSION = "0.61.16-alpha.1"', "SA verifier source")
st = one(st, 'require(\'PROTOCOL = "14"\' in network, "expedition current-region packet protocol must be 14")',
         'require(\'PROTOCOL = "15"\' in network, "TBOS shrine locator packet protocol must be 15")', "SA verifier protocol")
a = 'require("top = 78" in hud, "Mythic tracker narrow-screen boss-bar fallback missing")\n'
st = one(st, a, a + '''locator = text(JAVA / "compat/TbosFractureShrineLocator.java")
require("TbosFractureShrineLocator::onPlayerTick" in main, "TBOS shrine locator event missing")
require("AdventureWorldManager" in locator and "TemporalSiteManager" in locator and "fractureShrines" in locator,
        "TBOS planned/exact shrine bridge missing")
require("setChunkForced" not in locator and "addRegionTicket" not in locator, "TBOS locator may force-load chunks")
require("FractureShrineTargetPayload.TYPE" in network and "installFractureShrineReceiver" in network, "TBOS locator packet missing")
require("ClientFractureShrineState" in hud and "균열 성소" in hud and "예상" in hud, "TBOS locator HUD missing")
''', "SA verifier locator")
st = one(st, 'print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.15 deterministic fishing + Angler Harbor + current expedition UI + runtime invariants")',
         'print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.16 soft TBOS shrine locator + protocol15 + prior runtime invariants")', "SA verifier print")
w(stp, st)

print("PATCH APPLIED: Frontier alpha112 + Survival Ascension 0.61.16")

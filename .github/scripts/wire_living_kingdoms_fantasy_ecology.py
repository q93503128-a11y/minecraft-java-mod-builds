from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms"
MOD = BASE / "LivingKingdoms.java"
CLIENT = BASE / "client/LivingKingdomsClient.java"


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(message)


m = MOD.read_text(encoding="utf-8")
if "import kr.moonseungjun.livingkingdoms.entity.FantasyEntityTypes;" not in m:
    anchor = "import kr.moonseungjun.livingkingdoms.economy.RealmEconomyManager;\n"
    require(anchor in m, "main entity import anchor missing")
    m = m.replace(anchor, anchor + "import kr.moonseungjun.livingkingdoms.entity.FantasyEntityTypes;\n", 1)
if "import kr.moonseungjun.livingkingdoms.world.ErdenFantasyEcologyManager;" not in m:
    anchor = "import kr.moonseungjun.livingkingdoms.world.ErdenFireResponseManager;\n"
    require(anchor in m, "main ecology import anchor missing")
    m = m.replace(anchor, anchor + "import kr.moonseungjun.livingkingdoms.world.ErdenFantasyEcologyManager;\n", 1)
if "FantasyEntityTypes.register(modEventBus);" not in m:
    anchor = "        LivingWorldgenTypes.register(modEventBus);\n"
    require(anchor in m, "main registry constructor anchor missing")
    m = m.replace(anchor, anchor + "        FantasyEntityTypes.register(modEventBus);\n", 1)
if "ErdenFantasyEcologyManager.onServerTick(event);" not in m:
    anchor = "        RegionalEcologyManager.onServerTick(event);\n"
    require(anchor in m, "regional ecology tick anchor missing")
    m = m.replace(anchor, "        ErdenFantasyEcologyManager.onServerTick(event);\n" + anchor, 1)
MOD.write_text(m, encoding="utf-8")

c = CLIENT.read_text(encoding="utf-8")
if "modEventBus.addListener(FantasyEntityRenderers::register);" not in c:
    anchor = "        modEventBus.addListener(ClientNetworkHandlers::register);\n"
    require(anchor in c, "client renderer anchor missing")
    c = c.replace(anchor, anchor + "        modEventBus.addListener(FantasyEntityRenderers::register);\n", 1)
CLIENT.write_text(c, encoding="utf-8")

for path, tokens in {
    MOD: ["FantasyEntityTypes.register(modEventBus)", "ErdenFantasyEcologyManager.onServerTick(event)"],
    CLIENT: ["FantasyEntityRenderers::register"],
}.items():
    text = path.read_text(encoding="utf-8")
    for token in tokens:
        require(token in text, f"missing ecology wiring token {token} in {path.name}")

print("Wired fantasy entity registry, regional spawn tick and client render registration.")

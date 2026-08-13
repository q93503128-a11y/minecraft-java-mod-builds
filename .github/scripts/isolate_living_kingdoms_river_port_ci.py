from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
EXTERIOR = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java"
FIRE = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenFireResponseManager.java"
PORT = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenRiverPortManager.java"
JUSTICE = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/crime/ErdenJusticeManager.java"
CRIME = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/crime/CrimeManager.java"
MAIN = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/LivingKingdoms.java"
STATUS = ROOT / "projects/living-kingdoms/docs/ERDEN_IMPLEMENTATION_STATUS.md"


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(message)


port = PORT.read_text(encoding="utf-8")
require("LIVING_KINGDOMS_CI_RIVER_PORT_TEST" in port, "river-port CI fixture is not isolated")
require("ci_corridor_retained_until_pass=true" in port, "river-port ticket lifecycle proof missing")
fire = FIRE.read_text(encoding="utf-8")
require("LIVING_KINGDOMS_CI_FIRE_RESPONSE_TEST" in fire, "fire CI fixture is not isolated")
require("Selected bounded Erden fire CI support=" in fire, "bounded fire fixture search missing")
justice = JUSTICE.read_text(encoding="utf-8")
require("event_time_witness=true" in justice and "retroactive_witness=false" in justice,
        "Erden justice witness invariants missing")
require("ErdenJusticeManager.observeCrime(" in CRIME.read_text(encoding="utf-8"), "Erden crime routing missing")
require("ErdenJusticeManager.onServerTick(event);" in MAIN.read_text(encoding="utf-8"), "justice tick wiring missing")
require("범행 순간 실제 로드된 주민만 목격자로 확정" in STATUS.read_text(encoding="utf-8"),
        "Erden status lost justice implementation")

exterior = EXTERIOR.read_text(encoding="utf-8")
old = '''    private static boolean isCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }'''
new = '''    private static boolean isCi() {
        if (!"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return false;
        // Focused subsystem audits still bootstrap the authored realm, but must not also request
        // the 178-chunk exterior regression sweep. Naturally loaded exterior chunks continue to
        // use the normal streaming path, so production behavior is unchanged.
        return !"1".equals(System.getenv("LIVING_KINGDOMS_CI_RIVER_PORT_TEST"))
                && !"1".equals(System.getenv("LIVING_KINGDOMS_CI_FIRE_RESPONSE_TEST"));
    }'''
if "Focused subsystem audits still bootstrap the authored realm" not in exterior:
    require(old in exterior, "could not locate exterior CI gate")
    exterior = exterior.replace(old, new, 1)
require("LIVING_KINGDOMS_CI_RIVER_PORT_TEST" in exterior, "port focused-CI exclusion missing")
require("LIVING_KINGDOMS_CI_FIRE_RESPONSE_TEST" in exterior, "fire focused-CI exclusion missing")
EXTERIOR.write_text(exterior, encoding="utf-8")

print("Isolated focused port/fire audits from the 178-chunk exterior CI sweep without changing production streaming.")

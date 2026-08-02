#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
SELF = Path(__file__).resolve()
OLD_VERSION = "0.12.1-alpha.4"
NEW_VERSION = "0.12.1-alpha.5"


def replace_once(relative: str, old: str, new: str) -> None:
    path = ROOT / relative
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"anchor mismatch in {relative}: expected 1, got {count}: {old[:120]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def replace_all_versions() -> None:
    suffixes = {".java", ".py", ".json", ".properties", ".toml", ".md", ".txt"}
    for path in ROOT.rglob("*"):
        if not path.is_file() or path.resolve() == SELF or path.suffix.lower() not in suffixes:
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        if OLD_VERSION in text:
            path.write_text(text.replace(OLD_VERSION, NEW_VERSION), encoding="utf-8")


def patch_casting() -> None:
    relative = "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java"
    replace_once(relative,
        "    private static final long QUEUE_TIMEOUT_TICKS = 200L;\n"
        "    private static final long CHARGE_TIMEOUT_TICKS = 400L;",
        "    private static final long QUEUE_TIMEOUT_TICKS = 2400L;\n"
        "    private static final long CHARGE_TIMEOUT_TICKS = 1600L;")

    replace_once(relative,
'''    public static int requiredCastTicks(ServerPlayer player, SpellDefinition spell) {
        MagicPlayerData.MageState state = data(player).state(player);
        int base = 4 + spell.circle() * 4;
        int circleGap = Math.max(0, state.circle() - spell.circle());
        int circleGapReduction = circleGap * 4;
        int masteryReduction = SpellCatalog.masteryTier(state.mastery(spell.id())) * 2;
        return Math.max(2, base - circleGapReduction - masteryReduction);
    }
''',
'''    public static int requiredCastTicks(ServerPlayer player, SpellDefinition spell) {
        MagicPlayerData.MageState state = data(player).state(player);
        int circle = Math.max(1, Math.min(9, spell.circle()));
        int[] sameCircleTicks = {0, 10, 18, 30, 50, 84, 140, 230, 380, 620};
        int[] minimumTicks = {0, 3, 5, 8, 14, 24, 50, 90, 170, 360};
        int circleGap = Math.max(0, state.circle() - circle);
        int masteryTier = SpellCatalog.masteryTier(state.mastery(spell.id()));
        double gapScale = Math.pow(0.78, circleGap);
        double masteryScale = Math.max(0.72, 1.0 - masteryTier * 0.028);
        int calculated = (int) Math.round(sameCircleTicks[circle] * gapScale * masteryScale);
        return Math.max(minimumTicks[circle], calculated);
    }
''')

    replace_once(relative,
'''    public static int requiredFusionCastTicks(ServerPlayer player, SpellDefinition result, int ingredientCount) {
        MagicPlayerData.MageState state = data(player).state(player);
        int direct = requiredCastTicks(player, result);
        int masteryTier = SpellCatalog.masteryTier(state.mastery(result.id()));
        boolean registered = state.known().contains(result.id());
        int unfamiliarPenalty = registered ? 7 : 18 + ingredientCount * 5 + result.circle() * 2;
        return Math.max(direct + 5, direct + unfamiliarPenalty - masteryTier * 2);
    }
''',
'''    public static int requiredFusionCastTicks(ServerPlayer player, SpellDefinition result, int ingredientCount) {
        MagicPlayerData.MageState state = data(player).state(player);
        int circle = Math.max(1, Math.min(9, result.circle()));
        int direct = requiredCastTicks(player, result);
        int masteryTier = SpellCatalog.masteryTier(state.mastery(result.id()));
        boolean registered = state.known().contains(result.id());
        double complexity = 1.35 + Math.max(0, ingredientCount - 2) * 0.18 + circle * 0.055;
        int unfamiliarPenalty = registered ? 8 : 18 + ingredientCount * 7 + circle * 3;
        int calculated = (int) Math.ceil(direct * complexity) + unfamiliarPenalty - masteryTier * 3;
        int minimum = switch (circle) {
            case 1 -> 20;
            case 2 -> 34;
            case 3 -> 56;
            case 4 -> 90;
            case 5 -> 150;
            case 6 -> 240;
            case 7 -> 380;
            case 8 -> 620;
            default -> 960;
        };
        return Math.max(minimum, calculated);
    }
''')


def patch_affiliation_stats() -> None:
    relative = "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java"
    replace_once(relative,
'''        kr.moonseungjun.arcanecircle.world.MagicTradition chosen = world.tradition(player);
        boolean facultyMatch = chosen != kr.moonseungjun.arcanecircle.world.MagicTradition.UNBOUND
                && SpellWorldLore.tradition(spell.id()) == chosen;
        double facultyMana = facultyMatch ? chosen.manaMultiplier() : 1.0;
        double facultyPower = facultyMatch ? chosen.powerMultiplier() : 1.0;
        double facultyRange = facultyMatch ? chosen.rangeMultiplier() : 1.0;
        double facultyCooldown = facultyMatch ? chosen.cooldownMultiplier() : 1.0;
''',
'''        kr.moonseungjun.arcanecircle.world.MagicTradition chosen = world.tradition(player);
        // Affiliation is a social team, not a spell school. It never locks or buffs a school directly.
        double facultyMana = 1.0;
        double facultyPower = 1.0;
        double facultyRange = 1.0;
        double facultyCooldown = 1.0;
''')


def patch_economy() -> None:
    relative = "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEconomyService.java"
    replace_once(relative,
'''    public static long priceFor(ServerPlayer player, AcademyOfferCatalog.Offer offer) {
        long price = offer.basePrice();
        MagicTradition chosen = data(player).tradition(player);
        if (offer.kind() == AcademyOfferCatalog.Kind.SPELLBOOK
                && chosen != MagicTradition.UNBOUND
                && SpellWorldLore.tradition(offer.targetId()) == chosen) {
            price = Math.max(1L, Math.round(price * 0.82));
        }
        return price;
    }
''',
'''    public static long priceFor(ServerPlayer player, AcademyOfferCatalog.Offer offer) {
        // Social affiliation does not discount a magical school. All affiliations use one Arcana market.
        return offer.basePrice();
    }
''')
    path = ROOT / relative
    text = path.read_text(encoding="utf-8")
    text = text.replace("[학부 변경]", "[소속 변경]")
    text = text.replace("[학부 조율]", "[소속 등록]")
    text = text.replace("잘못된 학부", "잘못된 소속")
    text = text.replace("에 마력핵을 조율했습니다.", " 소속으로 등록되었습니다.")
    path.write_text(text, encoding="utf-8")


def patch_grimoire() -> None:
    relative = "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java"
    replace_once(relative,
        '            new Tab("academy", "학원"), new Tab("core", "마력핵"));',
        '            new Tab("academy", "마도회"), new Tab("core", "마력핵"));')
    replace_once(relative,
        "    private static int atlasCircle;\n    private static int academyCircle;",
        "    private static int atlasCircle;\n    private static int fusionCircle;\n    private static int academyCircle;")
    replace_once(relative,
        '        if ("atlas".equals(page)) return clickAtlas(event, l) || super.mouseClicked(event, doubleClick);\n'
        '        if ("academy".equals(page)) return clickAcademy(event, l) || super.mouseClicked(event, doubleClick);',
        '        if ("atlas".equals(page)) return clickAtlas(event, l) || super.mouseClicked(event, doubleClick);\n'
        '        if ("recipes".equals(page)) return clickRecipes(event, l) || super.mouseClicked(event, doubleClick);\n'
        '        if ("academy".equals(page)) return clickAcademy(event, l) || super.mouseClicked(event, doubleClick);')

    anchor = "    private boolean clickAcademy(MouseButtonEvent event, Layout l) {"
    path = ROOT / relative
    text = path.read_text(encoding="utf-8")
    if text.count(anchor) != 1:
        raise RuntimeError("clickAcademy insertion anchor mismatch")
    click_recipes = '''    private boolean clickRecipes(MouseButtonEvent event, Layout l) {
        if (fusionCircle == 0) {
            for (int circle = 1; circle <= 9; circle++) {
                if (inside(event.x(), event.y(), l.circleCard(circle))) {
                    fusionCircle = circle;
                    scroll = 0;
                    saveScroll();
                    return true;
                }
            }
            return false;
        }
        if (inside(event.x(), event.y(), l.back())) {
            fusionCircle = 0;
            scroll = 0;
            saveScroll();
            return true;
        }
        return false;
    }

'''
    text = text.replace(anchor, click_recipes + anchor, 1)
    path.write_text(text, encoding="utf-8")

    replace_once(relative,
        '                notice(traditions[i].displayName() + " 조율 요청"); return true;',
        '                notice(traditions[i].displayName() + " 소속 등록 요청"); return true;')

    old_recipes = '''    private void recipes(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        sectionTitle(g, l, "융합", "");
        Rect viewport = l.listViewport(24);
        g.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        List<SpellCatalog.FusionFormula> formulas = SpellCatalog.fusions();
        for (int i = 0; i < formulas.size(); i++) {
            Rect r = l.wideCard(i, scroll, 54, 24);
            SpellCatalog.FusionFormula formula = formulas.get(i);
            SpellDefinition result = SpellCatalog.spell(formula.result()).orElseThrow();
            int accent = ArcaneRenderUtil.schoolColor(result.school());
            boolean ready = formula.ingredients().stream().allMatch(id -> ArcaneClientState.cooldownRemainingTicks(id) <= 0);
            g.fill(r.x(), r.y(), r.right(), r.bottom(), inside(mouseX, mouseY, r) ? 0xFF25344B : 0xFF111927);
            g.fill(r.x(), r.y(), r.x() + 2, r.bottom(), ready ? accent : 0xFFB75B68);
            g.text(font, Component.literal(fit(result.circle() + "C  " + result.name(), r.w() - 10)), r.x() + 6, r.y() + 5, 0xFFF0E7FA);
            String chain = formula.ingredients().stream().map(id -> SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id))
                    .reduce((a, b) -> a + " + " + b).orElse("");
            g.text(font, Component.literal(fit(chain, r.w() - 10)), r.x() + 6, r.y() + 18, 0xFF93A2B8);
            String meta = "MP " + result.manaCost() + " · 쿨 " + String.format("%.1fs", result.cooldownTicks() / 20.0)
                    + " · 숙련 " + ArcaneClientState.mastery(result.id()) + "/" + SpellCatalog.masteryRequired(result.id());
            g.text(font, Component.literal(fit(meta, r.w() - 10)), r.x() + 6, r.y() + 31, 0xFF9FB6D2);
            String readiness = ready ? "재료 주문 쿨타임 준비 완료" : "재료 주문 쿨타임 대기 중";
            g.text(font, Component.literal(fit(readiness, r.w() - 10)), r.x() + 6, r.y() + 43,
                    ready ? 0xFF76D5A5 : 0xFFE07882);
        }
        g.disableScissor();
    }
'''
    new_recipes = '''    private void recipes(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        if (fusionCircle == 0) {
            sectionTitle(g, l, "융합 써클", "결과 주문의 써클별로 분류합니다");
            for (int circle = 1; circle <= 9; circle++) drawFusionCircleCard(g, l.circleCard(circle), circle, mouseX, mouseY);
            return;
        }

        Rect back = l.back();
        button(g, back, "‹ 써클", inside(mouseX, mouseY, back), true);
        g.text(font, Component.literal(fusionCircle + "C 융합"), back.right() + 8, back.y() + 5, 0xFFF1E8FA);
        Rect viewport = l.listViewport(49);
        g.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        List<SpellCatalog.FusionFormula> formulas = fusionFormulasInCircle(fusionCircle);
        for (int i = 0; i < formulas.size(); i++) {
            Rect r = l.wideCard(i, scroll, 54, 49);
            SpellCatalog.FusionFormula formula = formulas.get(i);
            SpellDefinition result = SpellCatalog.spell(formula.result()).orElseThrow();
            int accent = ArcaneRenderUtil.schoolColor(result.school());
            boolean ready = formula.ingredients().stream().allMatch(id -> ArcaneClientState.cooldownRemainingTicks(id) <= 0);
            g.fill(r.x(), r.y(), r.right(), r.bottom(), inside(mouseX, mouseY, r) ? 0xFF25344B : 0xFF111927);
            g.fill(r.x(), r.y(), r.x() + 2, r.bottom(), ready ? accent : 0xFFB75B68);
            g.text(font, Component.literal(fit(result.circle() + "C  " + result.name(), r.w() - 10)), r.x() + 6, r.y() + 5, 0xFFF0E7FA);
            String chain = formula.ingredients().stream().map(id -> SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id))
                    .reduce((a, b) -> a + " + " + b).orElse("");
            g.text(font, Component.literal(fit(chain, r.w() - 10)), r.x() + 6, r.y() + 18, 0xFF93A2B8);
            String meta = "MP " + result.manaCost() + " · 쿨 " + String.format("%.1fs", result.cooldownTicks() / 20.0)
                    + " · 숙련 " + ArcaneClientState.mastery(result.id()) + "/" + SpellCatalog.masteryRequired(result.id());
            g.text(font, Component.literal(fit(meta, r.w() - 10)), r.x() + 6, r.y() + 31, 0xFF9FB6D2);
            String readiness = ready ? "재료 주문 쿨타임 준비 완료" : "재료 주문 쿨타임 대기 중";
            g.text(font, Component.literal(fit(readiness, r.w() - 10)), r.x() + 6, r.y() + 43,
                    ready ? 0xFF76D5A5 : 0xFFE07882);
        }
        g.disableScissor();
    }

    private void drawFusionCircleCard(GuiGraphicsExtractor g, Rect r, int circle, int mouseX, int mouseY) {
        int count = fusionFormulasInCircle(circle).size();
        boolean unlocked = circle <= ArcaneClientState.integer("circle", 1);
        boolean hover = inside(mouseX, mouseY, r);
        int accent = count == 0 ? 0xFF4D4F59 : unlocked ? circleColor(circle) : 0xFF5B5364;
        g.fill(r.x(), r.y(), r.right(), r.bottom(), hover ? 0xFF26344A : 0xFF121A29);
        g.fill(r.x(), r.bottom() - 2, r.right(), r.bottom(), accent);
        ArcaneRenderUtil.ring(g, r.x() + 15, r.y() + r.h() / 2, 8, accent);
        g.text(font, Component.literal(circle + "C"), r.x() + 28, r.y() + 6, count > 0 ? 0xFFF5EDFF : 0xFF777881);
        g.text(font, Component.literal(Integer.toString(count)), r.x() + 29, r.y() + 18, accent);
    }
'''
    replace_once(relative, old_recipes, new_recipes)

    path = ROOT / relative
    text = path.read_text(encoding="utf-8")
    text = text.replace("학부 ", "소속 ")
    text = text.replace("학부 조율", "소속 등록")
    text = text.replace("학부 변경", "소속 변경")
    path.write_text(text, encoding="utf-8")

    replace_once(relative,
        '    private String scrollKey() { return page + ":" + ("atlas".equals(page) ? atlasCircle : "academy".equals(page) ? academyCircle : 0); }',
        '    private String scrollKey() { return page + ":" + ("atlas".equals(page) ? atlasCircle : "recipes".equals(page) ? fusionCircle : "academy".equals(page) ? academyCircle : 0); }')
    replace_once(relative,
        '            case "recipes" -> l.maxWideScroll(SpellCatalog.fusions().size(), 54, 24);',
        '            case "recipes" -> fusionCircle == 0 ? 0 : l.maxWideScroll(fusionFormulasInCircle(fusionCircle).size(), 54, 49);')
    replace_once(relative,
        '    private static MagicTradition[] traditions() { return new MagicTradition[]{MagicTradition.ARCANE, MagicTradition.DIVINE, MagicTradition.OCCULT, MagicTradition.PRIMAL}; }',
        '    private static List<SpellCatalog.FusionFormula> fusionFormulasInCircle(int circle) {\n'
        '        return SpellCatalog.fusions().stream().filter(formula -> SpellCatalog.spell(formula.result())\n'
        '                .map(spell -> spell.circle() == circle).orElse(false)).toList();\n'
        '    }\n'
        '    private static MagicTradition[] traditions() { return new MagicTradition[]{MagicTradition.ARCANE, MagicTradition.DIVINE, MagicTradition.OCCULT, MagicTradition.PRIMAL}; }')
    path = ROOT / relative
    text = path.read_text(encoding="utf-8")
    text = text.replace('case 6->"대마법";case 7->"차원 마법";case 8->"현실 간섭";default->"궁극 마법";',
                        'case 6->"대마법사";case 7->"초월 마법";case 8->"신화 마법";default->"세계급 의식";')
    path.write_text(text, encoding="utf-8")


def patch_damage_attribution() -> None:
    magic_root = ROOT / "src/main/java/kr/moonseungjun/arcanecircle/magic"
    pattern = re.compile(
        r"\b([A-Za-z_][A-Za-z0-9_]*)\.hurtServer\(level,\s*level\.damageSources\(\)\.magic\(\),\s*([^;]+)\);"
    )
    replaced = 0
    for path in magic_root.glob("*.java"):
        if path.name == "ArcaneDamage.java":
            continue
        text = path.read_text(encoding="utf-8")
        updated, count = pattern.subn(r"ArcaneDamage.hurt(level, player, \1, \2);", text)
        if count:
            path.write_text(updated, encoding="utf-8")
            replaced += count
    if replaced < 9:
        raise RuntimeError(f"expected at least 9 attributed magic damage replacements, got {replaced}")
    offenders = []
    for path in magic_root.glob("*.java"):
        if path.name == "ArcaneDamage.java":
            continue
        text = path.read_text(encoding="utf-8")
        if "damageSources().magic()" in text:
            offenders.append(path.name)
    if offenders:
        raise RuntimeError(f"unattributed spell damage remains: {offenders}")


def patch_mage_compatibility() -> None:
    relative = "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java"
    path = ROOT / relative
    text = path.read_text(encoding="utf-8")
    text = text.replace("castNaturalSpell(level, mob)", "castHostileSpell(level, mob)")
    text = text.replace("private static void castNaturalSpell(ServerLevel level, Mob caster)",
                        "private static void castHostileSpell(ServerLevel level, Mob caster)")
    path.write_text(text, encoding="utf-8")


def patch_index() -> None:
    path = ROOT / "src/main/resources/data/arcanecircle/spell_catalog/index.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data["version"] = NEW_VERSION
    data["circle_hierarchy"] = {
        "6": "grand_archmage",
        "7": "transcendent",
        "8": "mythic",
        "9": "world_unique_final_boss"
    }
    data["natural_circle_weights"] = {
        "1": 59000, "2": 25000, "3": 10000, "4": 4000,
        "5": 1500, "6": 400, "7": 80, "8": 20, "9": 0
    }
    data["affiliations"] = ["UNBOUND", "ARCANE", "DIVINE", "OCCULT", "PRIMAL"]
    data["affiliation_display"] = {
        "ARCANE": "왕국 마도연맹",
        "DIVINE": "백은 성약",
        "OCCULT": "녹월 결사",
        "PRIMAL": "재의 밀약"
    }
    data["mage_roles"] = ["WANDERER", "HOUSEHOLD", "LICENSED", "WARDEN", "SCHOLAR", "VILLAIN"]
    data["spell_damage_attribution"] = "caster_bound"
    data["fusion_ui"] = "circle_hierarchy"
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    if NEW_VERSION in properties:
        print("Arcane Circle alpha.5 hierarchy already installed")
        return
    if OLD_VERSION not in properties:
        raise RuntimeError("unexpected Arcane Circle source version")
    patch_casting()
    patch_affiliation_stats()
    patch_economy()
    patch_grimoire()
    patch_damage_attribution()
    patch_mage_compatibility()
    patch_index()
    replace_all_versions()
    print("Arcane Circle v0.12.1-alpha.5 hierarchy, affiliations and aggro installed")


if __name__ == "__main__":
    main()

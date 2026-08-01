#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"
RES = ROOT / "src/main/resources"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def replace_region(text: str, start_marker: str, end_marker: str, replacement: str, label: str) -> str:
    start = text.find(start_marker)
    if start < 0:
        if replacement.strip() in text:
            return text
        raise SystemExit(f"{label}: start marker missing")
    end = text.find(end_marker, start)
    if end < 0:
        raise SystemExit(f"{label}: end marker missing")
    return text[:start] + replacement + "\n\n" + text[end:]


# ---------------------------------------------------------------------------
# Spell casting: fixed readable sigils, true range scaling and combat result capture.
# ---------------------------------------------------------------------------
path = JAVA / "magic/SpellCastingService.java"
text = path.read_text(encoding="utf-8")
text = replace_once(text,
    "        if ((player.tickCount & 1) != 0) return;",
    "        if (Math.floorMod(player.tickCount, 4) != 0) return;",
    "charge render cadence")
text = replace_once(text,
'''        releasePrelude(player, cast);
        if (!execute(player, spell.id(), cast.range(), cast.power())) {
            fail(player, "시전 조건이 사라져 주문이 중단되었습니다.");
            return;
        }

        data.startCooldown(player, spell.id(), cast.cooldownTicks());
        MagicPlayerData.CastProgress progress = data.completeCast(player, cast);''',
'''        CombatGrowthService.Snapshot combatSnapshot = CombatGrowthService.capture(player, cast.range());
        releasePrelude(player, cast);
        if (!execute(player, spell.id(), cast.range(), cast.power())) {
            fail(player, "시전 조건이 사라져 주문이 중단되었습니다.");
            return;
        }
        CombatGrowthService.Impact impact = CombatGrowthService.measure(combatSnapshot, spell.circle());

        data.startCooldown(player, spell.id(), cast.cooldownTicks());
        MagicPlayerData.CastProgress progress = data.completeCast(player, cast, impact);''',
    "combat impact integration")
text = replace_once(text,
'''        ServerLevel level = (ServerLevel) player.level();
        if (progress.mastery().registered()) {''',
'''        if (impact.meaningful()) {
            String threat = impact.strongKills() > 0 ? " §6강적 처치 " + impact.strongKills()
                    : impact.strongHits() > 0 ? " §e강적 적중 " + impact.strongHits() : "";
            player.sendSystemMessage(Component.literal("§5[주문 숙련] §f적중 " + impact.hits()
                    + " · 처치 " + impact.kills() + threat + " §7· 숙련 +" + impact.masteryGain()
                    + " · 통찰 +" + impact.insightGain()));
        }

        ServerLevel level = (ServerLevel) player.level();
        if (progress.mastery().registered()) {''',
    "combat feedback")
text = replace_once(text,
    "            default -> false;\n        };\n    }",
    "            default -> ExpandedSpellEffects.execute(player, id, range, power);\n        };\n    }",
    "expanded spell dispatch")

new_charge = '''    private static void renderCharge(ServerPlayer player, SpellDefinition spell, long elapsed, double range) {
        ServerLevel level = (ServerLevel) player.level();
        double baseRange = Math.max(1.0, spell.range());
        double rangeRatio = Math.max(0.75, Math.min(2.6, range / baseRange));
        double radius = (0.76 + spell.circle() * 0.18) * Math.sqrt(rangeRatio);
        renderAnchoredSigil(level, player, spell, range, radius, 2);
        if (elapsed == 0L) {
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.42F, 1.58F - spell.circle() * 0.07F);
        }
    }'''
text = replace_region(text,
    "    private static void renderCharge(ServerPlayer player, SpellDefinition spell, long elapsed, double range) {",
    "    private static void renderAnchoredSigil",
    new_charge,
    "fixed charge sigil")

new_vertical = '''    private static void verticalSigil(ServerLevel level, Vec3 center, Vec3 normal, SpellDefinition spell,
                                      double radius, int density) {
        Vec3 upReference = Math.abs(normal.y) > 0.92 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = normal.cross(upReference).normalize();
        Vec3 up = right.cross(normal).normalize();
        int outer = Math.max(36, 34 + spell.circle() * 6 + density * 4);
        planeRing(level, center, right, up, radius, ParticleTypes.END_ROD, outer);
        planeRing(level, center, right, up, radius * 0.72, schoolParticle(spell), Math.max(28, outer - 10));
        planeRing(level, center, right, up, radius * 0.34, ParticleTypes.END_ROD, 24);
        int sides = 4 + Math.min(5, spell.circle());
        planePolygon(level, center, right, up, radius * 0.82, sides, ParticleTypes.END_ROD, 8 + density * 2);
        planeLine(level, center.add(right.scale(-radius * 0.55)), center.add(right.scale(radius * 0.55)),
                schoolParticle(spell), 12 + density * 2);
        planeLine(level, center.add(up.scale(-radius * 0.55)), center.add(up.scale(radius * 0.55)),
                schoolParticle(spell), 12 + density * 2);
        level.sendParticles(schoolParticle(spell), center.x, center.y, center.z,
                3 + spell.circle(), 0.025, 0.025, 0.025, 0.0);
    }

    private static void planeRing(ServerLevel level, Vec3 center, Vec3 right, Vec3 up, double radius,
                                  ParticleOptions particle, int points) {
        for (int index = 0; index < points; index++) {
            double angle = Math.PI * 2.0 * index / points;
            Vec3 point = center.add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    private static void planePolygon(ServerLevel level, Vec3 center, Vec3 right, Vec3 up, double radius,
                                     int sides, ParticleOptions particle, int pointsPerEdge) {
        List<Vec3> vertices = new ArrayList<>();
        for (int index = 0; index < sides; index++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * index / sides;
            vertices.add(center.add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius)));
        }
        for (int index = 0; index < vertices.size(); index++) {
            planeLine(level, vertices.get(index), vertices.get((index + 1) % vertices.size()), particle, pointsPerEdge);
        }
    }

    private static void planeLine(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int points) {
        particleLine(level, start, end, particle, Math.max(2, points));
    }'''
text = replace_region(text,
    "    private static void verticalSigil(ServerLevel level, Vec3 center, Vec3 normal, SpellDefinition spell,",
    "    private static void horizontalSigil",
    new_vertical,
    "readable vertical sigil")

new_horizontal = '''    private static void horizontalSigil(ServerLevel level, Vec3 center, SpellDefinition spell,
                                        double radius, int density) {
        int outer = Math.max(36, 34 + spell.circle() * 6 + density * 4);
        ring(level, center, radius, ParticleTypes.END_ROD, outer);
        ring(level, center.add(0.0, 0.025, 0.0), radius * 0.72, schoolParticle(spell), Math.max(28, outer - 10));
        ring(level, center.add(0.0, 0.05, 0.0), radius * 0.34, ParticleTypes.END_ROD, 24);
        int sides = 4 + Math.min(5, spell.circle());
        List<Vec3> vertices = new ArrayList<>();
        for (int index = 0; index < sides; index++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * index / sides;
            vertices.add(center.add(Math.cos(angle) * radius * 0.82, 0.07,
                    Math.sin(angle) * radius * 0.82));
        }
        for (int index = 0; index < vertices.size(); index++) {
            particleLine(level, vertices.get(index), vertices.get((index + 1) % vertices.size()),
                    ParticleTypes.END_ROD, 8 + density * 2);
        }
        particleLine(level, center.add(-radius * 0.55, 0.08, 0.0), center.add(radius * 0.55, 0.08, 0.0),
                schoolParticle(spell), 12 + density * 2);
        particleLine(level, center.add(0.0, 0.08, -radius * 0.55), center.add(0.0, 0.08, radius * 0.55),
                schoolParticle(spell), 12 + density * 2);
    }'''
text = replace_region(text,
    "    private static void horizontalSigil(ServerLevel level, Vec3 center, SpellDefinition spell,",
    "    private static ParticleOptions schoolParticle",
    new_horizontal,
    "readable horizontal sigil")
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Persistent mastery: real combat gives more than empty casts; mastery affects stats.
# ---------------------------------------------------------------------------
path = JAVA / "magic/MagicPlayerData.java"
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''        int masteryGap = Math.max(0, state.circle - spell.circle());
        double circleMana = Math.max(0.48, 1.0 - masteryGap * 0.09);
        double circleCooldown = Math.max(0.38, 1.0 - masteryGap * 0.14);
        double circleRange = 1.0 + masteryGap * 0.08;
        double circlePower = 1.0 + masteryGap * 0.10;

        int manaCost = Math.max(1, (int) Math.ceil(spell.manaCost() * circleMana * staff.manaCostMultiplier()));
        int cooldown = Math.max(8, (int) Math.round(spell.cooldownTicks() * circleCooldown * staff.cooldownMultiplier()));
        double range = spell.range() * circleRange * staff.rangeMultiplier();
        double power = spell.power() * circlePower * staff.powerFor(spell.school());''',
'''        int masteryGap = Math.max(0, state.circle - spell.circle());
        int proficiency = SpellCatalog.masteryTier(state.mastery(spellId));
        double circleMana = Math.max(0.48, 1.0 - masteryGap * 0.09);
        double circleCooldown = Math.max(0.38, 1.0 - masteryGap * 0.14);
        double circleRange = 1.0 + masteryGap * 0.08;
        double circlePower = 1.0 + masteryGap * 0.10;
        double masteryMana = Math.max(0.80, 1.0 - proficiency * 0.02);
        double masteryCooldown = Math.max(0.70, 1.0 - proficiency * 0.03);
        double masteryRange = 1.0 + proficiency * 0.02;
        double masteryPower = 1.0 + proficiency * 0.04;

        int manaCost = Math.max(1, (int) Math.ceil(spell.manaCost() * circleMana * masteryMana
                * staff.manaCostMultiplier()));
        int cooldown = Math.max(8, (int) Math.round(spell.cooldownTicks() * circleCooldown * masteryCooldown
                * staff.cooldownMultiplier()));
        double range = spell.range() * circleRange * masteryRange * staff.rangeMultiplier();
        double power = spell.power() * circlePower * masteryPower * staff.powerFor(spell.school());''',
    "mastery stat scaling")

new_complete = '''    public CastProgress completeCast(ServerPlayer player, CastPreparation cast,
                                     CombatGrowthService.Impact impact) {
        MageState state = state(player);
        CombatGrowthService.Impact result = impact == null ? CombatGrowthService.Impact.NONE : impact;
        state.mana = Math.max(0.0, state.mana - cast.manaCost());

        int beforeMastery = state.mastery.getOrDefault(cast.spell().id(), 0);
        int masteryGain = Math.max(1, result.masteryGain());
        int afterMastery = Math.min(100000, beforeMastery + masteryGain);
        state.mastery.put(cast.spell().id(), afterMastery);
        state.insight += Math.max(1, cast.spell().circle() * 2) + Math.max(0, result.insightGain());

        int previousCircle = state.circle;
        while (state.circle < SpellCatalog.IMPLEMENTED_MAX_CIRCLE
                && state.insight >= SpellCatalog.circleInsightThreshold(state.circle + 1)) {
            state.circle++;
        }
        if (state.circle > previousCircle) state.mana = effectiveStats(player).maxMana();

        MasteryProgress mastery = MasteryProgress.none();
        if (cast.fusion() && !cast.masteryId().isBlank()) {
            String resultId = cast.masteryId();
            int required = SpellCatalog.masteryRequired(resultId);
            boolean registered = afterMastery >= required && state.known.add(resultId);
            if (registered) equipIntoFirstEmptySlot(state, resultId);
            mastery = new MasteryProgress(true, registered, resultId, afterMastery, required);
        }

        setDirty();
        return new CastProgress(new CircleAdvance(previousCircle, state.circle), mastery);
    }'''
text = replace_region(text,
    "    public CastProgress completeCast(ServerPlayer player, CastPreparation cast) {",
    "    public CooldownStatus cooldownStatus",
    new_complete,
    "combat-weighted completeCast")
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Network all-spell mastery synchronization.
# ---------------------------------------------------------------------------
path = JAVA / "network/ArcaneNetwork.java"
text = path.read_text(encoding="utf-8")
text = text.replace('PROTOCOL_VERSION = "ninefold-arcana-6"', 'PROTOCOL_VERSION = "ninefold-arcana-7"')
text = replace_once(text,
'''        String mastery = SpellCatalog.fusions().stream()
                .map(formula -> formula.result() + ":" + state.mastery(formula.result()))
                .collect(Collectors.joining("|"));''',
'''        String mastery = SpellCatalog.spells().values().stream()
                .map(spell -> spell.id() + ":" + state.mastery(spell.id()))
                .collect(Collectors.joining("|"));''',
    "all spell mastery snapshot")
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Grimoire circle categories and stronger card readability.
# ---------------------------------------------------------------------------
path = JAVA / "client/GrimoireScreen.java"
text = path.read_text(encoding="utf-8")
text = replace_once(text,
    "    private static int savedActiveSlot;",
    "    private static int savedActiveSlot;\n    private static int savedCircleFilter;",
    "circle filter field")
text = replace_once(text,
'''        if ("atlas".equals(page)) {
            for (int i = 0; i < 5; i++) {''',
'''        if ("atlas".equals(page)) {
            for (int circle = 0; circle <= SpellCatalog.IMPLEMENTED_MAX_CIRCLE; circle++) {
                if (inside(event.x(), event.y(), l.circleFilter(circle))) {
                    savedCircleFilter = circle;
                    contentScroll = 0;
                    SAVED_SCROLL.put(page, 0);
                    notice(circle == 0 ? "전체 주문 표시" : circle + "써클 주문만 표시");
                    return true;
                }
            }
            for (int i = 0; i < 5; i++) {''',
    "circle filter click")
text = text.replace("            List<SpellDefinition> spells = new ArrayList<>(SpellCatalog.spells().values());",
                    "            List<SpellDefinition> spells = visibleSpells();", 1)
text = replace_once(text,
'''        drawManaStrip(g, l);
        for (int i = 0; i < 5; i++) drawLoadoutSlot(g, l.slot(i), i, savedActiveSlot == i, time + i * 140L);

        int gridTop = l.gridTop();''',
'''        drawManaStrip(g, l);
        for (int i = 0; i < 5; i++) drawLoadoutSlot(g, l.slot(i), i, savedActiveSlot == i, time + i * 140L);
        drawCircleFilters(g, l, mouseX, mouseY);

        int gridTop = l.gridTop();''',
    "circle filter rendering")
text = text.replace("        List<SpellDefinition> spells = new ArrayList<>(SpellCatalog.spells().values());",
                    "        List<SpellDefinition> spells = visibleSpells();", 1)
text = replace_once(text,
'''        g.text(font, Component.literal(spell.school().displayName() + " · MP " + spell.manaCost()),
                textX, r.y() + 21, usable ? 0xFF9CB1CE : 0xFF5E5B64);''',
'''        int points = ArcaneClientState.mastery(spell.id());
        int tier = SpellCatalog.masteryTier(points);
        String status = !ArcaneClientState.known().contains(spell.id()) ? "미습득"
                : spell.circle() > ArcaneClientState.integer("circle", 1) ? "써클 부족"
                : equipped ? "장착 · 숙련 " + tier : "사용 가능 · 숙련 " + tier;
        g.text(font, Component.literal(spell.school().displayName() + " · MP " + spell.manaCost() + " · " + status),
                textX, r.y() + 21, usable ? (equipped ? 0xFFFFD36B : 0xFFB8D4F4) : 0xFF77727D);''',
    "spell card state readability")
text = replace_once(text,
'''    private void drawManaStrip(GuiGraphicsExtractor g, Layout l) {''',
'''    private List<SpellDefinition> visibleSpells() {
        return SpellCatalog.spells().values().stream()
                .filter(spell -> savedCircleFilter == 0 || spell.circle() == savedCircleFilter)
                .toList();
    }

    private void drawCircleFilters(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        for (int circle = 0; circle <= SpellCatalog.IMPLEMENTED_MAX_CIRCLE; circle++) {
            Rect tab = l.circleFilter(circle);
            boolean active = savedCircleFilter == circle;
            boolean hover = inside(mouseX, mouseY, tab);
            int background = active ? 0xFF5B367C : hover ? 0xFF293754 : 0xFF111827;
            int border = active ? 0xFFE4B8FF : circle <= ArcaneClientState.integer("circle", 1)
                    ? 0xFF6E8FC7 : 0xFF4A4652;
            g.fill(tab.x(), tab.y(), tab.right(), tab.bottom(), background);
            g.fill(tab.x(), tab.bottom() - 2, tab.right(), tab.bottom(), border);
            g.centeredText(font, Component.literal(circle == 0 ? "전체" : circle + "C"),
                    tab.x() + tab.w() / 2, tab.y() + 5, active ? 0xFFFFFFFF : 0xFFD0C8DC);
        }
    }

    private void drawManaStrip(GuiGraphicsExtractor g, Layout l) {''',
    "visible spell helper")
text = replace_once(text,
    '            case "atlas" -> l.maxAtlasScroll(SpellCatalog.spells().size());',
    '            case "atlas" -> l.maxAtlasScroll(visibleSpells().size());',
    "filtered atlas scroll")
text = replace_once(text,
'''        int gridTop() { return slotTop() + slotSize() + 9; }''',
'''        int filterTop() { return slotTop() + slotSize() + 7; }
        int gridTop() { return filterTop() + 25; }
        Rect circleFilter(int circle) {
            Rect c = content();
            int count = SpellCatalog.IMPLEMENTED_MAX_CIRCLE + 1;
            int gap = c.w() < 420 ? 2 : 5;
            int width = Math.max(34, Math.min(62, (c.w() - gap * (count - 1)) / count));
            int total = width * count + gap * (count - 1);
            return new Rect(cx() - total / 2 + circle * (width + gap), filterTop(), width, 20);
        }''',
    "circle filter layout")
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Generate all spellbook resources and localization from the new catalogue IDs.
# ---------------------------------------------------------------------------
spellbooks = [
    ("light", 1, "minecraft:glowstone_dust"), ("grease", 1, "minecraft:slime_ball"),
    ("sleep", 1, "minecraft:poppy"), ("thunderwave", 1, "minecraft:goat_horn"),
    ("mage_armor", 1, "minecraft:chainmail_chestplate"),
    ("scorching_ray", 2, "minecraft:blaze_powder"), ("misty_step", 2, "minecraft:ender_pearl"),
    ("web", 2, "minecraft:cobweb"), ("mirror_image", 2, "minecraft:glass"),
    ("invisibility", 2, "minecraft:fermented_spider_eye"), ("gust_of_wind", 2, "minecraft:breeze_rod"),
    ("hold_person", 2, "minecraft:lead"), ("shatter", 2, "minecraft:amethyst_shard"),
    ("blur", 2, "minecraft:phantom_membrane"), ("levitate", 2, "minecraft:feather"),
    ("fireball", 3, "minecraft:fire_charge"), ("lightning_bolt", 3, "minecraft:lightning_rod"),
    ("fly", 3, "minecraft:elytra"), ("haste", 3, "minecraft:sugar"),
    ("dispel_magic", 3, "minecraft:milk_bucket"), ("vampiric_touch", 3, "minecraft:ghast_tear"),
    ("slow", 3, "minecraft:soul_sand"), ("protection_from_energy", 3, "minecraft:magma_cream"),
    ("sleet_storm", 3, "minecraft:packed_ice"), ("blink", 3, "minecraft:ender_eye"),
    ("wall_of_fire", 4, "minecraft:blaze_rod"), ("ice_storm", 4, "minecraft:blue_ice"),
    ("greater_invisibility", 4, "minecraft:echo_shard"), ("resilient_sphere", 4, "minecraft:beacon"),
    ("dimension_door", 4, "minecraft:crying_obsidian"), ("stoneskin", 4, "minecraft:obsidian"),
    ("confusion", 4, "minecraft:chorus_fruit"), ("blight", 4, "minecraft:wither_rose"),
    ("freedom_of_movement", 4, "minecraft:rabbit_foot"), ("phantasmal_killer", 4, "minecraft:wither_skeleton_skull"),
    ("cone_of_cold", 5, "minecraft:blue_ice"), ("wall_of_force", 5, "minecraft:beacon"),
    ("cloudkill", 5, "minecraft:dragon_breath"), ("telekinesis", 5, "minecraft:shulker_shell"),
    ("flame_strike", 5, "minecraft:nether_star"), ("hold_monster", 5, "minecraft:heavy_core"),
    ("mass_cure_wounds", 5, "minecraft:enchanted_golden_apple"), ("passwall", 5, "minecraft:reinforced_deepslate"),
    ("dominate_person", 5, "minecraft:ominous_bottle"), ("insect_plague", 5, "minecraft:bee_nest"),
]
materials = {
    1: ("minecraft:amethyst_shard", "minecraft:lapis_lazuli", "minecraft:gold_ingot", "minecraft:emerald", 18, None, 0),
    2: ("minecraft:amethyst_block", "minecraft:lapis_block", "minecraft:diamond", "minecraft:emerald", 45, None, 0),
    3: ("minecraft:diamond", "minecraft:amethyst_block", "minecraft:echo_shard", "minecraft:emerald_block", 14, None, 0),
    4: ("minecraft:diamond_block", "minecraft:echo_shard", "minecraft:netherite_ingot", "minecraft:emerald_block", 40, "minecraft:diamond", 4),
    5: ("minecraft:netherite_ingot", "minecraft:echo_shard", "minecraft:nether_star", "minecraft:emerald_block", 64, "minecraft:nether_star", 1),
}
entries = []
for spell_id, circle, catalyst in spellbooks:
    ring, seal, core, price_item, price_count, second_item, second_count = materials[circle]
    entry = {
        "id": spell_id, "circle": circle, "catalyst": catalyst,
        "ring": ring, "seal": seal, "core": core, "level": circle,
        "price_item": price_item, "price_count": price_count,
    }
    if second_item:
        entry["second_item"] = second_item
        entry["second_count"] = second_count
    entries.append(entry)
(ROOT / "src/main/spellbooks.json").write_text(json.dumps(entries, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

names = {
    "light":"빛", "grease":"기름막", "sleep":"수면", "thunderwave":"천둥파동", "mage_armor":"마법 갑주",
    "scorching_ray":"작열 광선", "misty_step":"안개 걸음", "web":"거미줄", "mirror_image":"거울상",
    "invisibility":"투명화", "gust_of_wind":"돌풍", "hold_person":"인간형 속박", "shatter":"분쇄",
    "blur":"흐릿함", "levitate":"부유", "fireball":"화염구", "lightning_bolt":"번개 줄기",
    "fly":"비행", "haste":"가속", "dispel_magic":"마법 해제", "vampiric_touch":"흡혈의 손길",
    "slow":"둔화", "protection_from_energy":"에너지 보호", "sleet_storm":"진눈깨비 폭풍", "blink":"점멸",
    "wall_of_fire":"화염벽", "ice_storm":"얼음 폭풍", "greater_invisibility":"상급 투명화",
    "resilient_sphere":"탄성 구체", "dimension_door":"차원문", "stoneskin":"돌가죽", "confusion":"혼란",
    "blight":"황폐", "freedom_of_movement":"이동의 자유", "phantasmal_killer":"환영 살해자",
    "cone_of_cold":"냉기 원뿔", "wall_of_force":"역장벽", "cloudkill":"독구름", "telekinesis":"염동력",
    "flame_strike":"화염 기둥", "hold_monster":"괴물 속박", "mass_cure_wounds":"광역 치유",
    "passwall":"통과문", "dominate_person":"인간형 지배", "insect_plague":"곤충 떼",
}
lang_path = RES / "assets/arcanecircle/lang/ko_kr.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
for key in list(lang):
    if key.startswith("item.arcanecircle.spellbook_"):
        del lang[key]
for spell_id, display in names.items():
    lang[f"item.arcanecircle.spellbook_{spell_id}"] = f"주문서: {display}"
lang_path.write_text(json.dumps(lang, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

print("Arcane Circle v0.7 source upgrade applied")

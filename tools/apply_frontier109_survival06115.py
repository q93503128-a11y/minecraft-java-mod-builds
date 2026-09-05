#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
F = ROOT / "projects/frontier-settlement"
S = ROOT / "projects/survival-ascension"


def read(path):
    return path.read_text(encoding="utf-8")


def write(path, text):
    path.write_text(text, encoding="utf-8")


def replace_once(path, old, new):
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected exactly one replacement target, found {count}: {old[:80]!r}")
    write(path, text.replace(old, new, 1))


# ---------------------------------------------------------------------------
# Frontier Settlement alpha.109 — persistent settlement/outpost navigation UI
# ---------------------------------------------------------------------------
fg = F / "gradle.properties"
replace_once(fg, "mod_version=0.1.0-alpha.108", "mod_version=0.1.0-alpha.109")
with fg.open("a", encoding="utf-8") as out:
    out.write("\n# Alpha.109 settlement navigation: main settlement center and nearest outposts are exposed in the M > infrastructure screen with overworld coordinates, distance and compass direction.\n")

context = F / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementContextService.java"
replace_once(context,
'''        BlockPos stock = data.stockpilePos();
        SettlementResources resources = data.resources();''',
'''        BlockPos center = data.centerPos();
        targets.add(new SettlementContextTarget(
                "settlement", "settlement",
                center.getX(), center.getY(), center.getZ(),
                center.getX(), center.getY(), center.getZ(),
                center.getX(), center.getY(), center.getZ(),
                "본진", "공동 마을 중심 · M → 인프라에서 좌표/방향 확인", -1));

        BlockPos stock = data.stockpilePos();
        SettlementResources resources = data.resources();''')

palette = F / "src/main/java/kr/moonseungjun/frontiersettlement/client/BuildingPaletteScreen.java"
replace_once(palette,
'''import kr.moonseungjun.frontiersettlement.network.SettlementSnapshotPayload;''',
'''import kr.moonseungjun.frontiersettlement.network.SettlementSnapshotPayload;
import kr.moonseungjun.frontiersettlement.network.SettlementContextTarget;
import net.minecraft.world.level.Level;''')
replace_once(palette,
'''            g.text(this.font, Component.literal("메뉴에서 작업을 고른 뒤 월드 프리뷰로 위치와 범위를 확인하세요."),
                    contentX, infoY + 22, TEXT_MUTED, false);''',
'''            g.text(this.font, Component.literal("메뉴에서 작업을 고른 뒤 월드 프리뷰로 위치와 범위를 확인하세요."),
                    contentX, infoY + 22, TEXT_MUTED, false);
            drawSettlementLocations(g, infoY + 39);''')
replace_once(palette,
'''    private void drawBuildingDetail(GuiGraphicsExtractor g, SettlementSnapshotPayload data, BuildingType type) {''',
'''    private void drawSettlementLocations(GuiGraphicsExtractor g, int startY) {
        List<SettlementContextTarget> bases = new ArrayList<>();
        for (SettlementContextTarget target : ClientSettlementState.context().targets()) {
            if ("settlement".equals(target.kind()) || "outpost".equals(target.kind())) bases.add(target);
        }
        if (bases.isEmpty()) {
            g.text(this.font, Component.literal("거점 위치 동기화 대기 중…"), contentX, startY, TEXT_MUTED, false);
            return;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        boolean overworld = minecraft.level != null && minecraft.level.dimension().equals(Level.OVERWORLD);
        if (player != null && overworld) {
            bases.sort((a, b) -> Long.compare(distanceSq(player.getX(), player.getZ(), a), distanceSq(player.getX(), player.getZ(), b)));
        } else {
            bases.sort((a, b) -> {
                if ("settlement".equals(a.kind())) return -1;
                if ("settlement".equals(b.kind())) return 1;
                return Integer.compare(a.markerX(), b.markerX());
            });
        }

        int bottom = panelY + panelHeight - 27;
        int maxRows = Math.max(1, (bottom - startY) / 11);
        int visible = Math.min(bases.size(), maxRows);
        for (int i = 0; i < visible; i++) {
            SettlementContextTarget target = bases.get(i);
            String label = "settlement".equals(target.kind()) ? "본진" : target.title();
            String line = label + " · X " + target.markerX() + " Y " + target.markerY() + " Z " + target.markerZ();
            if (player != null && overworld) {
                long dx = Math.round(target.markerX() + 0.5D - player.getX());
                long dz = Math.round(target.markerZ() + 0.5D - player.getZ());
                long distance = Math.round(Math.sqrt((double) dx * dx + (double) dz * dz));
                line += " · " + distance + "블록 " + directionName(dx, dz);
            } else {
                line += " · 오버월드";
            }
            if (i == visible - 1 && bases.size() > visible) {
                line += " · 외 " + (bases.size() - visible) + "곳";
            }
            line = trimToWidth(line, contentWidth);
            int color = "settlement".equals(target.kind()) ? TEXT_ACCENT : TEXT_SECONDARY;
            g.text(this.font, Component.literal(line), contentX, startY + i * 11, color, false);
        }
    }

    private static long distanceSq(double x, double z, SettlementContextTarget target) {
        long dx = Math.round(target.markerX() + 0.5D - x);
        long dz = Math.round(target.markerZ() + 0.5D - z);
        return dx * dx + dz * dz;
    }

    private static String directionName(long dx, long dz) {
        if (dx == 0L && dz == 0L) return "현재 위치";
        String[] names = {"북", "북동", "동", "남동", "남", "남서", "서", "북서"};
        double angle = Math.atan2((double) dx, (double) -dz);
        int index = (int) Math.round(angle / (Math.PI / 4.0D));
        index = Math.floorMod(index, 8);
        return names[index];
    }

    private String trimToWidth(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) return text;
        String suffix = "…";
        String out = text;
        while (!out.isEmpty() && this.font.width(out + suffix) > maxWidth) out = out.substring(0, out.length() - 1);
        return out + suffix;
    }

    private void drawBuildingDetail(GuiGraphicsExtractor g, SettlementSnapshotPayload data, BuildingType type) {''')

fv = F / "tools/test_current_source.py"
replace_once(fv, 'require("mod_version=0.1.0-alpha.108" in gradle, "current verifier/version drift")',
                  'require("mod_version=0.1.0-alpha.109" in gradle, "current verifier/version drift")')
replace_once(fv,
'''require("기존 시설 자동 개량" in palette, "production vertical progression is hidden from the build palette")''',
'''require("기존 시설 자동 개량" in palette, "production vertical progression is hidden from the build palette")
context_ui = text(SETTLEMENT / "SettlementContextService.java")
require('"settlement", "settlement"' in context_ui and '"본진"' in context_ui and "data.centerPos()" in context_ui,
        "main settlement navigation target missing")
require("drawSettlementLocations" in palette and "directionName" in palette and '"outpost".equals(target.kind())' in palette,
        "settlement/outpost coordinate navigation UI missing")''')
replace_once(fv,
'''print("CURRENT SOURCE CHECK PASS: alpha108 tree-aware placement + alpha107 worker/repair + prior authority invariants")''',
'''print("CURRENT SOURCE CHECK PASS: alpha109 settlement navigation + alpha108 tree-aware placement + prior authority invariants")''')


# ---------------------------------------------------------------------------
# Survival Ascension 0.61.15 — deterministic fishing progression + fishery infra
# ---------------------------------------------------------------------------
sg = S / "gradle.properties"
replace_once(sg, "mod_version=0.61.14-alpha.1", "mod_version=0.61.15-alpha.1")
smain = S / "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java"
replace_once(smain, 'VERSION = "0.61.14-alpha.1"', 'VERSION = "0.61.15-alpha.1"')

infra = S / "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java"
replace_once(infra,
'''    BUILDER_FOUNDRY(
            "builder_foundry", "건축 공방",''',
'''    ANGLER_HARBOR(
            "angler_harbor", "어업 부두", "낚시 추가 어획·마모 절약을 확률 대신 누적 보장식으로 운용 · 추가 어획 +35%p · 마모 절약 +15%p · 숙련 XP +25%", 0,
            List.of(
                    shared("wood", SharedEconomyCompat.ResourceCategory.WOOD, 96),
                    shared("stone", SharedEconomyCompat.ResourceCategory.STONE, 48),
                    shared("metal", SharedEconomyCompat.ResourceCategory.METAL, 24),
                    exact("prismarine", Items.PRISMARINE_SHARD, "프리즈머린 조각", 8),
                    exact("nautilus", Items.NAUTILUS_SHELL, "앵무조개 껍데기", 2)
            )),
    BUILDER_FOUNDRY(
            "builder_foundry", "건축 공방",''')

site = S / "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureSiteService.java"
replace_once(site,
'''    private static final SiteProfile INDUSTRIAL_SITE = new SiteProfile(false, List.of(''',
'''    private static final SiteProfile ANGLER_SITE = new SiteProfile(false, List.of(
            new SiteRequirement(Blocks.WATER, "물", 12),
            new SiteRequirement(Blocks.BARREL, "배럴", 1),
            new SiteRequirement(Blocks.SMOKER, "훈연기", 1),
            new SiteRequirement(Blocks.CAMPFIRE, "모닥불", 1),
            new SiteRequirement(Blocks.LANTERN, "랜턴", 2)
    ));
    private static final SiteProfile INDUSTRIAL_SITE = new SiteProfile(false, List.of(''')
replace_once(site,
'''            case CIVIL_WORKS -> CIVIL_SITE;
            case INDUSTRIAL_WORKS -> INDUSTRIAL_SITE;''',
'''            case ANGLER_HARBOR -> ANGLER_SITE;
            case CIVIL_WORKS -> CIVIL_SITE;
            case INDUSTRIAL_WORKS -> INDUSTRIAL_SITE;''')

radial = S / "src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java"
replace_once(radial,
'''            new Entry("관개 시설", "Lv.30 자동 재파종", new ItemStack(Items.WATER_BUCKET), InfrastructureProject.IRRIGATION_WORKS, Action.FUND),''',
'''            new Entry("관개 시설", "Lv.30 자동 재파종", new ItemStack(Items.WATER_BUCKET), InfrastructureProject.IRRIGATION_WORKS, Action.FUND),
            new Entry("어업 부두", "추가 어획 누적보장 · 마모 절약 · XP", new ItemStack(Items.FISHING_ROD), InfrastructureProject.ANGLER_HARBOR, Action.FUND),''')

progress = S / "src/main/java/kr/moonseungjun/survivalascension/progress/SkillProgressData.java"
replace_once(progress,
'''    private record PlayerEntry(String uuid, Map<String, Long> skills, long legacyMiningXp, boolean introduced,
                               int constructionLength) {''',
'''    private record PlayerEntry(String uuid, Map<String, Long> skills, long legacyMiningXp, boolean introduced,
                               int constructionLength, int fishingBonusMilli, int fishingPreserveMilli) {''')
replace_once(progress,
'''                Codec.BOOL.optionalFieldOf("introduced", false).forGetter(PlayerEntry::introduced),
                Codec.INT.optionalFieldOf("construction_length", 0).forGetter(PlayerEntry::constructionLength)
        ).apply(instance, PlayerEntry::new));''',
'''                Codec.BOOL.optionalFieldOf("introduced", false).forGetter(PlayerEntry::introduced),
                Codec.INT.optionalFieldOf("construction_length", 0).forGetter(PlayerEntry::constructionLength),
                Codec.INT.optionalFieldOf("fishing_bonus_milli", 0).forGetter(PlayerEntry::fishingBonusMilli),
                Codec.INT.optionalFieldOf("fishing_preserve_milli", 0).forGetter(PlayerEntry::fishingPreserveMilli)
        ).apply(instance, PlayerEntry::new));''')
replace_once(progress,
'''        private boolean introduced;
        private int constructionLength;

        private PlayerState(Map<String, Long> xp, long legacyMiningXp, boolean introduced, int constructionLength) {''',
'''        private boolean introduced;
        private int constructionLength;
        private int fishingBonusMilli;
        private int fishingPreserveMilli;

        private PlayerState(Map<String, Long> xp, long legacyMiningXp, boolean introduced, int constructionLength,
                            int fishingBonusMilli, int fishingPreserveMilli) {''')
replace_once(progress,
'''            this.introduced = introduced;
            this.constructionLength = Math.max(0, constructionLength);''',
'''            this.introduced = introduced;
            this.constructionLength = Math.max(0, constructionLength);
            this.fishingBonusMilli = Math.floorMod(fishingBonusMilli, 1000);
            this.fishingPreserveMilli = Math.floorMod(fishingPreserveMilli, 1000);''')
replace_once(progress,
'''            players.put(entry.uuid(), new PlayerState(entry.skills(), entry.legacyMiningXp(), entry.introduced(), entry.constructionLength()));''',
'''            players.put(entry.uuid(), new PlayerState(entry.skills(), entry.legacyMiningXp(), entry.introduced(), entry.constructionLength(),
                    entry.fishingBonusMilli(), entry.fishingPreserveMilli()));''')
replace_once(progress,
'''                uuid, Map.copyOf(state.xp), 0L, state.introduced, state.constructionLength)));''',
'''                uuid, Map.copyOf(state.xp), 0L, state.introduced, state.constructionLength,
                state.fishingBonusMilli, state.fishingPreserveMilli)));''')
replace_once(progress,
'''        players.put(key, new PlayerState(Map.of(), 0L, false, 0));''',
'''        players.put(key, new PlayerState(Map.of(), 0L, false, 0, 0, 0));''')
replace_once(progress,
'''    public int constructionLengthSelection(ServerPlayer player) { return state(player).constructionLength; }

    public void setConstructionLengthSelection''',
'''    public int constructionLengthSelection(ServerPlayer player) { return state(player).constructionLength; }
    public int fishingBonusMilli(ServerPlayer player) { return state(player).fishingBonusMilli; }
    public int fishingPreserveMilli(ServerPlayer player) { return state(player).fishingPreserveMilli; }

    public void setFishingBonusMilli(ServerPlayer player, int milli) {
        PlayerState state = state(player);
        int normalized = Math.floorMod(milli, 1000);
        if (state.fishingBonusMilli == normalized) return;
        state.fishingBonusMilli = normalized;
        setDirty();
    }

    public void setFishingPreserveMilli(ServerPlayer player, int milli) {
        PlayerState state = state(player);
        int normalized = Math.floorMod(milli, 1000);
        if (state.fishingPreserveMilli == normalized) return;
        state.fishingPreserveMilli = normalized;
        setDirty();
    }

    public void setConstructionLengthSelection''')

fishing = S / "src/main/java/kr/moonseungjun/survivalascension/fishing/FishingProgression.java"
replace_once(fishing,
'''import kr.moonseungjun.survivalascension.progress.SkillProgressData;''',
'''import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import kr.moonseungjun.survivalascension.progress.SkillProgressData;''')
replace_once(fishing,
'''public final class FishingProgression {
    private FishingProgression() {}''',
'''public final class FishingProgression {
    private static final int METER = 1000;
    private static final int HARBOR_BONUS_CATCH_MILLI = 350;
    private static final int HARBOR_PRESERVATION_MILLI = 150;
    private static final double HARBOR_XP_MULTIPLIER = 1.25D;

    private FishingProgression() {}''')
replace_once(fishing,
'''        int oldLevel = SkillProgressData.get(player).level(player, SkillType.FISHING);
        int rawXp = xpForCatch(event);
        SkillProgressData.AddXpResult result = SkillProgressionService.award(player, SkillType.FISHING, rawXp);
        applyBonusCatch(player, event, oldLevel);
        preserveRod(player, event, oldLevel);''',
'''        int oldLevel = SkillProgressData.get(player).level(player, SkillType.FISHING);
        boolean harbor = InfrastructureData.get(player).isComplete(InfrastructureProject.ANGLER_HARBOR);
        int rawXp = xpForCatch(event);
        if (harbor) rawXp = Math.max(1, (int) Math.round(rawXp * HARBOR_XP_MULTIPLIER));
        SkillProgressData.AddXpResult result = SkillProgressionService.award(player, SkillType.FISHING, rawXp);
        applyBonusCatch(player, event, oldLevel, harbor);
        preserveRod(player, event, oldLevel, harbor);''')
replace_once(fishing,
'''    private static void applyBonusCatch(ServerPlayer player, ItemFishedEvent event, int level) {
        double chance = SkillTuning.fishingBonusCatchChance(level);
        if (chance <= 0.0D) return;
        for (ItemStack stack : event.getDrops()) {
            if (!isFishCatch(stack)) continue;
            if (player.getRandom().nextDouble() < chance) stack.grow(1);
        }
    }''',
'''    private static void applyBonusCatch(ServerPlayer player, ItemFishedEvent event, int level, boolean harbor) {
        ItemStack fish = ItemStack.EMPTY;
        for (ItemStack stack : event.getDrops()) {
            if (isFishCatch(stack)) { fish = stack; break; }
        }
        if (fish.isEmpty()) return;

        int gain = (int) Math.round(SkillTuning.fishingBonusCatchChance(level) * METER)
                + (harbor ? HARBOR_BONUS_CATCH_MILLI : 0);
        if (gain <= 0) return;
        SkillProgressData data = SkillProgressData.get(player);
        int meter = data.fishingBonusMilli(player) + gain;
        int extra = meter / METER;
        data.setFishingBonusMilli(player, meter % METER);
        if (extra > 0) fish.grow(extra);
    }''')
replace_once(fishing,
'''    private static void preserveRod(ServerPlayer player, ItemFishedEvent event, int level) {
        int damage = event.getRodDamage();
        if (damage <= 0) return;
        double chance = SkillTuning.fishingRodPreservationChance(level);
        if (chance > 0.0D && player.getRandom().nextDouble() < chance) {
            event.damageRodBy(Math.max(0, damage - 1));
        }
    }''',
'''    private static void preserveRod(ServerPlayer player, ItemFishedEvent event, int level, boolean harbor) {
        int damage = event.getRodDamage();
        if (damage <= 0) return;
        int gain = (int) Math.round(SkillTuning.fishingRodPreservationChance(level) * METER)
                + (harbor ? HARBOR_PRESERVATION_MILLI : 0);
        gain = Math.max(0, Math.min(METER, gain));
        if (gain <= 0) return;
        SkillProgressData data = SkillProgressData.get(player);
        int meter = data.fishingPreserveMilli(player) + gain;
        if (meter >= METER) {
            event.damageRodBy(Math.max(0, damage - 1));
            meter -= METER;
        }
        data.setFishingPreserveMilli(player, meter);
    }''')
replace_once(fishing,
'''        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 방지 10% · 물고기 추가 어획 10%"));
        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 방지 25% · 추가 어획 25% · 이후 레벨마다 두 효과 증가"));
        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 방지 45% · 추가 어획 50%"));
        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 방지 65% · 추가 어획 75%"));
        if (oldLevel < 100 && newLevel >= 100) player.sendSystemMessage(Component.literal("§3[낚시 숙련 VI] §f마모 방지 80% · 물고기 추가 어획 100%"));''',
'''        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 절약 10% 누적 · 물고기 추가 어획 10% 누적"));
        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 절약 25% · 추가 어획 25% · 확률 추첨 대신 누적 보장"));
        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 절약 45% · 추가 어획 50% 누적"));
        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 절약 65% · 추가 어획 75% 누적"));
        if (oldLevel < 100 && newLevel >= 100) player.sendSystemMessage(Component.literal("§3[낚시 숙련 VI] §f마모 절약 80% · 물고기 추가 어획 매회 +1 보장"));''')

skills = S / "src/main/java/kr/moonseungjun/survivalascension/client/SkillsScreen.java"
replace_once(skills,
'''            case FISHING -> "낚싯대 마모 방지 " + Math.round(SkillTuning.fishingRodPreservationChance(level) * 100.0D) + "% · 어획 성공으로 숙련";''',
'''            case FISHING -> "마모 절약 " + Math.round(SkillTuning.fishingRodPreservationChance(level) * 100.0D)
                    + "% 누적 · 추가 어획 " + Math.round(SkillTuning.fishingBonusCatchChance(level) * 100.0D) + "% 누적";''')

sv = S / "tools/test_current_source.py"
replace_once(sv, 'require("mod_version=0.61.14-alpha.1" in props, "Survival Ascension version drift")',
                 'require("mod_version=0.61.15-alpha.1" in props, "Survival Ascension version drift")')
replace_once(sv, 'require(\'VERSION = "0.61.14-alpha.1"\' in main, "source version drift")',
                 'require(\'VERSION = "0.61.15-alpha.1"\' in main, "source version drift")')
replace_once(sv,
'''require("applyBonusCatch" in fishing and "stack.grow(1)" in fishing, "fishing bonus catch is not applied to real fish drops")''',
'''require("applyBonusCatch" in fishing and "fish.grow(extra)" in fishing, "fishing deterministic bonus catch is not applied to real fish drops")
require("player.getRandom().nextDouble() < chance" not in fishing, "fishing mastery returned to streaky RNG")
require("ANGLER_HARBOR" in infra and "어업 부두" in infra, "fishing infrastructure project missing")
require("HARBOR_BONUS_CATCH_MILLI = 350" in fishing and "HARBOR_PRESERVATION_MILLI = 150" in fishing,
        "angler harbor fishing bonus drift")
require("HARBOR_XP_MULTIPLIER = 1.25D" in fishing, "angler harbor XP acceleration drift")
progress_data = text(JAVA / "progress/SkillProgressData.java")
require("fishing_bonus_milli" in progress_data and "fishing_preserve_milli" in progress_data,
        "persistent deterministic fishing meters missing")
site = text(JAVA / "infrastructure/InfrastructureSiteService.java")
require("ANGLER_SITE" in site and "Blocks.WATER" in site and "Blocks.SMOKER" in site,
        "physical waterside angler harbor commissioning site missing")
infra_ui = text(JAVA / "client/InfrastructureRadialMenuScreen.java")
require("어업 부두" in infra_ui and "Items.FISHING_ROD" in infra_ui, "angler harbor missing from infrastructure menu")''')
replace_once(sv,
'''print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.14 current expedition UI + mobility/fishing + Mythic HUD + runtime invariants")''',
'''print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.15 deterministic fishing + Angler Harbor + current expedition UI + runtime invariants")''')

changelog = S / "CHANGELOG.md"
replace_once(changelog,
'''# Changelog

## 0.61.14-alpha.1''',
'''# Changelog

## 0.61.15-alpha.1
- Replaced Fishing mastery bonus-catch and rod-preservation coin flips with persistent deterministic accumulation. The same percentage now becomes guaranteed progress toward the next extra fish or saved durability point, removing long unlucky streaks without duplicating treasure loot.
- Added the stage-0 `어업 부두` infrastructure project. It adds +35 percentage points to deterministic bonus catch, +15 points to rod preservation, and +25% Fishing mastery XP so fewer repeated casts are needed.
- `어업 부두` has a physical waterside commissioning check around a real barrel: nearby water, barrel, smoker, campfire and lanterns are required before the final funding step. No chunks are force-loaded.
- Kept vanilla FishingHook bite timing untouched; the reduced grind comes from deterministic yield, durability and XP density rather than brittle private-timer manipulation.
- Network protocol remains 14 and expedition state is unchanged.

## 0.61.14-alpha.1''')

print("PATCH APPLIED: Frontier alpha109 navigation + Survival 0.61.15 deterministic fishing/angler harbor")

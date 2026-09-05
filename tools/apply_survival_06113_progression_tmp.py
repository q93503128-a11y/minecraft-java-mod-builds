from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT / "projects/survival-ascension"
JAVA = PROJECT / "src/main/java/kr/moonseungjun/survivalascension"


def replace_once(path: Path, old: str, new: str, label: str):
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, got {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


gradle = PROJECT / "gradle.properties"
replace_once(gradle, "mod_version=0.61.12-alpha.1", "mod_version=0.61.13-alpha.1", "gradle version")

main = JAVA / "SurvivalAscension.java"
replace_once(main, 'public static final String VERSION = "0.61.12-alpha.1";', 'public static final String VERSION = "0.61.13-alpha.1";', "source version")
replace_once(
    main,
    "        NeoForge.EVENT_BUS.addListener(MobilityProgression::onPlayerLoggedIn);\n        NeoForge.EVENT_BUS.addListener(MobilityProgression::onPlayerLoggedOut);\n",
    "        NeoForge.EVENT_BUS.addListener(MobilityProgression::onPlayerLoggedIn);\n        NeoForge.EVENT_BUS.addListener(MobilityProgression::onPlayerRespawn);\n        NeoForge.EVENT_BUS.addListener(MobilityProgression::onPlayerChangedDimension);\n        NeoForge.EVENT_BUS.addListener(MobilityProgression::onPlayerLoggedOut);\n",
    "mobility lifecycle registration",
)

mobility = JAVA / "mobility/MobilityProgression.java"
replace_once(
    mobility,
    "    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {\n",
    "    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {\n        if (!(event.getEntity() instanceof ServerPlayer player)) return;\n        APPLIED_ATTRIBUTE_LEVEL.remove(player.getUUID());\n        refreshAttributesIfNeeded(player);\n    }\n\n    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {\n        if (!(event.getEntity() instanceof ServerPlayer player)) return;\n        APPLIED_ATTRIBUTE_LEVEL.remove(player.getUUID());\n        refreshAttributesIfNeeded(player);\n    }\n\n    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {\n",
    "mobility lifecycle recovery",
)
replace_once(
    mobility,
    '        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§d[기동 해금] §f1블록 단차 자동 넘기기 + 낙하 안전 강화"));\n        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§d[기동 해금] §fV · 지상 돌진"));\n        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§d[기동 해금] §f공중에서 V를 한 번 더 사용할 수 있습니다."));\n        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§d[기동 해금] §f극한 돌진 · 종말 단계 승천 중추 완공 시 공중 돌진 2회"));\n',
    '        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§d[기동 해금] §f1블록 단차 · 이동 속도 +2%대 · 안전 낙하 4블록"));\n        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§d[기동 해금] §f1.25블록 단차 · 지상 돌진 · 이후 레벨마다 돌진 성능 증가"));\n        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§d[기동 해금] §f1.5블록 단차 · 공중 돌진 1회 · 이동 속도 약 +16%"));\n        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§d[기동 해금] §f1.75블록 단차 · 극한 돌진 · 조건 충족 시 공중 돌진 2회"));\n',
    "mobility milestone text",
)

skill = JAVA / "progress/SkillTuning.java"
replace_once(
    skill,
    "    public static double fishingRodPreservationChance(int level) {\n        if (level >= 100) return 0.65D;\n        if (level >= 90) return 0.50D;\n        if (level >= 60) return 0.35D;\n        if (level >= 30) return 0.20D;\n        if (level >= 10) return 0.10D;\n        return 0.0D;\n    }\n",
    "    public static double fishingRodPreservationChance(int level) {\n        int clamped = clamp(level);\n        if (clamped < 10) return 0.0D;\n        if (clamped < 30) return 0.10D + 0.15D * (clamped - 10) / 20.0D;\n        if (clamped < 60) return 0.25D + 0.20D * (clamped - 30) / 30.0D;\n        if (clamped < 90) return 0.45D + 0.20D * (clamped - 60) / 30.0D;\n        return 0.65D + 0.15D * (clamped - 90) / 10.0D;\n    }\n\n    public static double fishingBonusCatchChance(int level) {\n        int clamped = clamp(level);\n        if (clamped < 10) return 0.0D;\n        if (clamped < 30) return 0.10D + 0.15D * (clamped - 10) / 20.0D;\n        if (clamped < 60) return 0.25D + 0.25D * (clamped - 30) / 30.0D;\n        if (clamped < 90) return 0.50D + 0.25D * (clamped - 60) / 30.0D;\n        return 0.75D + 0.25D * (clamped - 90) / 10.0D;\n    }\n",
    "fishing scaling",
)
replace_once(
    skill,
    "    public static double mobilitySpeedMultiplier(int level) {\n        int clamped = clamp(level);\n        return 1.0D + 0.0015D * clamped + 0.000005D * clamped * clamped;\n    }\n    public static double mobilityStepHeight(int level) {\n        if (level >= 100) return 2.0D;\n        if (level >= 90) return 1.5D;\n        if (level >= 60) return 1.25D;\n        if (level >= 10) return 1.0D;\n        return 0.6D;\n    }\n    public static double mobilitySafeFallDistance(int level) {\n        if (level >= 100) return 16.0D;\n        if (level >= 90) return 12.0D;\n        if (level >= 60) return 8.0D;\n        if (level >= 30) return 6.0D;\n        if (level >= 10) return 4.0D;\n        return 3.0D;\n    }\n    public static double mobilityDashPower(int level) {\n        if (level >= 100) return 1.80D;\n        if (level >= 90) return 1.55D;\n        if (level >= 60) return 1.25D;\n        if (level >= 30) return 0.95D;\n        return 0.0D;\n    }\n    public static int mobilityDashCooldownTicks(int level) {\n        if (level >= 100) return 16;\n        if (level >= 90) return 24;\n        if (level >= 60) return 40;\n        if (level >= 30) return 60;\n        return Integer.MAX_VALUE;\n    }\n",
    "    public static double mobilitySpeedMultiplier(int level) {\n        int clamped = clamp(level);\n        return 1.0D + 0.0020D * clamped + 0.000010D * clamped * clamped;\n    }\n    public static double mobilityStepHeight(int level) {\n        if (level >= 100) return 2.0D;\n        if (level >= 90) return 1.75D;\n        if (level >= 60) return 1.50D;\n        if (level >= 30) return 1.25D;\n        if (level >= 10) return 1.0D;\n        return 0.6D;\n    }\n    public static double mobilitySafeFallDistance(int level) {\n        int clamped = clamp(level);\n        if (clamped < 10) return 3.0D;\n        if (clamped < 30) return 4.0D + 2.0D * (clamped - 10) / 20.0D;\n        if (clamped < 60) return 6.0D + 2.0D * (clamped - 30) / 30.0D;\n        if (clamped < 90) return 8.0D + 4.0D * (clamped - 60) / 30.0D;\n        return 12.0D + 4.0D * (clamped - 90) / 10.0D;\n    }\n    public static double mobilityDashPower(int level) {\n        int clamped = clamp(level);\n        if (clamped < 30) return 0.0D;\n        if (clamped < 60) return 0.95D + 0.30D * (clamped - 30) / 30.0D;\n        if (clamped < 90) return 1.25D + 0.30D * (clamped - 60) / 30.0D;\n        return 1.55D + 0.25D * (clamped - 90) / 10.0D;\n    }\n    public static int mobilityDashCooldownTicks(int level) {\n        int clamped = clamp(level);\n        if (clamped < 30) return Integer.MAX_VALUE;\n        if (clamped < 60) return (int)Math.round(60.0D - 20.0D * (clamped - 30) / 30.0D);\n        if (clamped < 90) return (int)Math.round(40.0D - 16.0D * (clamped - 60) / 30.0D);\n        return (int)Math.round(24.0D - 8.0D * (clamped - 90) / 10.0D);\n    }\n",
    "mobility scaling",
)

fishing = JAVA / "fishing/FishingProgression.java"
replace_once(
    fishing,
    "        SkillProgressData.AddXpResult result = SkillProgressionService.award(player, SkillType.FISHING, rawXp);\n        preserveRod(player, event, oldLevel);\n        announceMilestones(player, result);\n",
    "        SkillProgressData.AddXpResult result = SkillProgressionService.award(player, SkillType.FISHING, rawXp);\n        applyBonusCatch(player, event, oldLevel);\n        preserveRod(player, event, oldLevel);\n        announceMilestones(player, result);\n",
    "fishing bonus call",
)
replace_once(
    fishing,
    "    private static void preserveRod(ServerPlayer player, ItemFishedEvent event, int level) {\n",
    "    private static void applyBonusCatch(ServerPlayer player, ItemFishedEvent event, int level) {\n        double chance = SkillTuning.fishingBonusCatchChance(level);\n        if (chance <= 0.0D) return;\n        for (ItemStack stack : event.getDrops()) {\n            if (!isFishCatch(stack)) continue;\n            if (player.getRandom().nextDouble() < chance) stack.grow(1);\n        }\n    }\n\n    private static boolean isFishCatch(ItemStack stack) {\n        return stack.is(Items.COD) || stack.is(Items.SALMON)\n                || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH);\n    }\n\n    private static void preserveRod(ServerPlayer player, ItemFishedEvent event, int level) {\n",
    "fishing bonus implementation",
)
replace_once(
    fishing,
    '        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f낚싯대 마모 방지 10%"));\n        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f낚싯대 마모 방지 20%"));\n        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f낚싯대 마모 방지 35%"));\n        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f낚싯대 마모 방지 50%"));\n        if (oldLevel < 100 && newLevel >= 100) player.sendSystemMessage(Component.literal("§3[낚시 숙련 VI] §f낚싯대 마모 방지 65%"));\n',
    '        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 방지 10% · 물고기 추가 어획 10%"));\n        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 방지 25% · 추가 어획 25% · 이후 레벨마다 두 효과 증가"));\n        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 방지 45% · 추가 어획 50%"));\n        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 방지 65% · 추가 어획 75%"));\n        if (oldLevel < 100 && newLevel >= 100) player.sendSystemMessage(Component.literal("§3[낚시 숙련 VI] §f마모 방지 80% · 물고기 추가 어획 100%"));\n',
    "fishing milestone text",
)

verify = PROJECT / "tools/test_current_source.py"
replace_once(verify, 'require("mod_version=0.61.12-alpha.1" in props, "Survival Ascension version drift")', 'require("mod_version=0.61.13-alpha.1" in props, "Survival Ascension version drift")', "verify version")
replace_once(verify, 'require(\'VERSION = "0.61.12-alpha.1"\' in main, "source version drift")', 'require(\'VERSION = "0.61.13-alpha.1"\' in main, "source version drift")', "verify source version")
anchor = 'require("mobility_action\\\", InputConstants.KEY_V" not in client, "old V dash default returned")\n'
addition = anchor + '\nrequire("MobilityProgression::onPlayerRespawn" in main and "MobilityProgression::onPlayerChangedDimension" in main, "mobility transient attributes are not restored across lifecycle boundaries")\nrequire("return 1.0D + 0.0020D * clamped + 0.000010D * clamped * clamped;" in tuning, "mobility per-level speed scaling drift")\nrequire("if (level >= 30) return 1.25D;" in tuning and "if (level >= 60) return 1.50D;" in tuning, "mobility step progression drift")\nrequire("fishingBonusCatchChance" in tuning, "fishing bonus-yield progression missing")\nfishing = text(JAVA / "fishing/FishingProgression.java")\nrequire("applyBonusCatch" in fishing and "stack.grow(1)" in fishing, "fishing bonus catch is not applied to real fish drops")\n'
replace_once(verify, anchor, addition, "verify progression additions")
replace_once(verify, 'print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.12 Mythic HUD + X dash + pacing/runtime invariants")', 'print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.13 mobility/fishing progression + Mythic HUD + runtime invariants")', "verify pass text")

changelog = PROJECT / "CHANGELOG.md"
text = changelog.read_text(encoding="utf-8")
entry = """## 0.61.13-alpha.1
- Restored Mobility transient movement attributes immediately after respawn and dimension changes, fixing the step-height/safe-fall/speed perks disappearing after transitions such as entering the Nether.
- Rebalanced Mobility so ordinary levels matter: movement speed now reaches +30% at Lv100, safe-fall and dash power scale continuously between milestones, dash cooldown improves continuously from Lv30 onward, and step height now progresses 1.0/1.25/1.5/1.75/2.0 blocks at Lv10/30/60/90/100.
- Expanded Fishing beyond rod durability: rod preservation now scales continuously between milestones and real fish catches gain an extra-fish chance that scales 10%/25%/50%/75%/100% at Lv10/30/60/90/100. Treasure and non-fish drops are not duplicated.
- Mining, Woodcutting, Harvesting, Combat and Construction balance is unchanged in this patch.

"""
if entry not in text:
    text = text.replace("# Changelog\n\n", "# Changelog\n\n" + entry, 1)
    changelog.write_text(text, encoding="utf-8")

print("SURVIVAL 0.61.13 PROGRESSION PATCH APPLIED")

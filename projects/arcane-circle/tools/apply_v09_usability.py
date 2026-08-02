#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        if new in text:
            return
        raise RuntimeError(f"missing patch anchor in {path}: {old[:120]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def replace_method(path: Path, signature: str, replacement: str) -> None:
    text = path.read_text(encoding="utf-8")
    start = text.find(signature)
    if start < 0:
        if replacement.strip() in text:
            return
        raise RuntimeError(f"missing method in {path.name}: {signature}")
    brace = text.find("{", start)
    if brace < 0:
        raise RuntimeError(f"missing opening brace in {path.name}: {signature}")
    depth = 0
    end = -1
    for i in range(brace, len(text)):
        if text[i] == "{": depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                end = i + 1
                break
    if end < 0:
        raise RuntimeError(f"unclosed method in {path.name}: {signature}")
    path.write_text(text[:start] + replacement.rstrip() + text[end:], encoding="utf-8")

props = ROOT / "gradle.properties"
replace_once(props, "mod_version=0.8.0-alpha.1", "mod_version=0.9.0-alpha.1")

network = JAVA / "network/ArcaneNetwork.java"
replace_once(network, 'public static final String PROTOCOL_VERSION = "ninefold-arcana-8";',
             'public static final String PROTOCOL_VERSION = "ninefold-arcana-9";')
replace_once(network,
'''                + ";charge_ticks=" + SpellCastingService.chargingTicks(player)
                + ";queue=" + queued''',
'''                + ";charge_ticks=" + SpellCastingService.chargingTicks(player)
                + ";charge_required=" + SpellCastingService.chargingRequiredTicks(player)
                + ";queue=" + queued''')

client = JAVA / "client/ArcaneClientState.java"
replace_once(client,
'''    public static boolean isChargingSlot(int slot) {
        return chargingSlot() == slot && !chargingSpell().isBlank();
    }
''',
'''    public static int chargingRequiredTicks() {
        return Math.max(0, integer("charge_required", 0));
    }

    public static double chargingFraction() {
        int required = chargingRequiredTicks();
        if (required <= 0 || chargingSpell().isBlank()) return 0.0;
        return Math.min(1.0, chargingTicks() / (double) required);
    }

    public static boolean chargingReady() {
        return !chargingSpell().isBlank() && chargingTicks() >= chargingRequiredTicks();
    }

    public static boolean isChargingSlot(int slot) {
        return chargingSlot() == slot && !chargingSpell().isBlank();
    }
''')

casting = JAVA / "magic/SpellCastingService.java"
replace_once(casting, "private static final long CHARGE_TIMEOUT_TICKS = 240L;",
             "private static final long CHARGE_TIMEOUT_TICKS = 400L;")
replace_once(casting,
'''    private static final class ChargeState {
        private final int slot;
        private final String spellId;
        private final long startedAt;

        private ChargeState(int slot, String spellId, long startedAt) {
            this.slot = slot;
            this.spellId = spellId;
            this.startedAt = startedAt;
        }
    }
''',
'''    private static final class ChargeState {
        private final int slot;
        private final String spellId;
        private final long startedAt;
        private final int requiredTicks;
        private int lastStage = -1;
        private long lastReadyPulse;

        private ChargeState(int slot, String spellId, long startedAt, int requiredTicks) {
            this.slot = slot;
            this.spellId = spellId;
            this.startedAt = startedAt;
            this.requiredTicks = requiredTicks;
        }
    }
''')

replace_method(casting, "    public static void beginSlotCharge(ServerPlayer player, int slot)", r'''    public static void beginSlotCharge(ServerPlayer player, int slot) {
        MagicPlayerData data = data(player);
        MagicPlayerData.CastPreparation cast = data.prepareSlot(player, slot);
        if (!cast.accepted()) {
            fail(player, cast.message());
            return;
        }
        MagicPlayerData.CooldownStatus cooldown = data.cooldownStatus(player, cast.spell().id());
        if (cooldown.active()) {
            fail(player, String.format("%s 재사용까지 %.1f초", cast.spell().name(), cooldown.remainingTicks() / 20.0));
            return;
        }

        ChargeState existing = CHARGES.get(player.getUUID());
        if (existing != null && existing.slot == slot && existing.spellId.equals(cast.spell().id())) {
            return;
        }

        clearFusion(player, false);
        int required = requiredCastTicks(player, cast.spell());
        if (required <= 0) {
            CHARGES.remove(player.getUUID());
            castPrepared(player, data, cast);
            return;
        }

        ChargeState charge = new ChargeState(slot, cast.spell().id(), serverClock(player), required);
        CHARGES.put(player.getUUID(), charge);
        player.sendOverlayMessage(Component.literal("§5[회로 전개] §f" + cast.spell().name()
                + " §7· " + String.format("%.1f", required / 20.0) + "초"));
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 0.45F, 1.18F);
        SpellSigilService.renderChargeStep(player, cast.spell(), cast.range(), 0);
        charge.lastStage = 0;
    }''')

replace_method(casting, "    public static void releaseSlotCharge(ServerPlayer player, int slot)", r'''    public static void releaseSlotCharge(ServerPlayer player, int slot) {
        ChargeState charge = CHARGES.get(player.getUUID());
        if (charge == null || charge.slot != slot) return;
        long elapsed = serverClock(player) - charge.startedAt;
        CHARGES.remove(player.getUUID());
        if (elapsed > CHARGE_TIMEOUT_TICKS) {
            player.sendOverlayMessage(Component.literal("§7[시전 취소] 유지 한계를 넘어 마법진이 해제되었습니다."));
            return;
        }
        if (elapsed < charge.requiredTicks) {
            int percent = (int) Math.round(100.0 * elapsed / Math.max(1, charge.requiredTicks));
            player.sendOverlayMessage(Component.literal("§7[시전 취소] 회로 전개 " + percent + "% · 완성 전에 키를 놓았습니다."));
            return;
        }
        MagicPlayerData data = data(player);
        MagicPlayerData.CastPreparation cast = data.prepareSlot(player, slot);
        if (!cast.accepted() || !charge.spellId.equals(cast.spell().id())) {
            fail(player, cast.accepted() ? "충전 중 주문 슬롯이 변경되었습니다." : cast.message());
            return;
        }
        castPrepared(player, data, cast);
    }''')

replace_method(casting, "    public static void tickCharge(ServerPlayer player)", r'''    public static void tickCharge(ServerPlayer player) {
        ChargeState charge = CHARGES.get(player.getUUID());
        if (charge == null) return;
        long now = serverClock(player);
        long elapsed = now - charge.startedAt;
        if (!player.isAlive() || player.isSpectator() || elapsed > CHARGE_TIMEOUT_TICKS) {
            CHARGES.remove(player.getUUID());
            if (elapsed > CHARGE_TIMEOUT_TICKS) {
                player.sendOverlayMessage(Component.literal("§7[시전 취소] 마법진 유지 시간이 끝났습니다."));
            }
            return;
        }
        SpellDefinition spell = SpellCatalog.spell(charge.spellId).orElse(null);
        if (spell == null || !data(player).state(player).known().contains(spell.id())) {
            CHARGES.remove(player.getUUID());
            return;
        }
        MagicPlayerData.CastPreparation cast = data(player).prepareSlot(player, charge.slot);
        if (!cast.accepted()) {
            CHARGES.remove(player.getUUID());
            return;
        }

        int stage = Math.min(SpellSigilService.CHARGE_STAGES - 1,
                (int) (elapsed * SpellSigilService.CHARGE_STAGES / Math.max(1, charge.requiredTicks)));
        if (stage > charge.lastStage) {
            for (int next = charge.lastStage + 1; next <= stage; next++) {
                SpellSigilService.renderChargeStep(player, spell, cast.range(), next);
            }
            charge.lastStage = stage;
        }
        if (elapsed >= charge.requiredTicks && now - charge.lastReadyPulse >= 16L) {
            SpellSigilService.renderReadyPulse(player, spell, cast.range());
            charge.lastReadyPulse = now;
        }
    }''')

replace_once(casting,
'''    public static int chargingTicks(ServerPlayer player) {
        ChargeState state = CHARGES.get(player.getUUID());
        if (state == null) return 0;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, serverClock(player) - state.startedAt));
    }
''',
'''    public static int chargingTicks(ServerPlayer player) {
        ChargeState state = CHARGES.get(player.getUUID());
        if (state == null) return 0;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, serverClock(player) - state.startedAt));
    }

    public static int chargingRequiredTicks(ServerPlayer player) {
        ChargeState state = CHARGES.get(player.getUUID());
        return state == null ? 0 : state.requiredTicks;
    }

    public static int requiredCastTicks(ServerPlayer player, SpellDefinition spell) {
        MagicPlayerData.MageState state = data(player).state(player);
        int base = 10 + spell.circle() * 8;
        int circleGapReduction = Math.max(0, state.circle() - spell.circle()) * 6;
        int masteryReduction = SpellCatalog.masteryTier(state.mastery(spell.id())) * 2;
        return Math.max(0, base - circleGapReduction - masteryReduction);
    }
''')

replace_method(casting, "    private static void renderCharge(ServerPlayer player, SpellDefinition spell, long elapsed, double range)", r'''    private static void renderCharge(ServerPlayer player, SpellDefinition spell, long elapsed, double range) {
        int required = Math.max(1, requiredCastTicks(player, spell));
        int stage = Math.min(SpellSigilService.CHARGE_STAGES - 1,
                (int) (elapsed * SpellSigilService.CHARGE_STAGES / required));
        SpellSigilService.renderChargeStep(player, spell, range, stage);
    }''')

hud = JAVA / "client/ArcaneHud.java"
replace_once(hud,
'''        int gap = width < 360 ? 2 : 3;
        int slotSize = Math.max(20, Math.min(27, (width - 18 - gap * 4) / 5));''',
'''        int gap = width < 360 ? 2 : 4;
        int desired = width >= 600 ? 58 : width >= 430 ? 46 : 32;
        int slotSize = Math.max(24, Math.min(desired, (width - 18 - gap * 4) / 5));''')
replace_once(hud,
'''        // Preserve the old left-side mana/status scale and position on wide displays.
        if (width >= 500) {
            int legacySlot = 38;
            int legacyGap = 4;
            int legacyStart = Math.max(4, (width - legacySlot * 5 - legacyGap * 4) / 2);
            int legacyY = Math.max(8, height - legacySlot - 29);
            drawManaSide(g, font, legacyStart, legacyY, legacySlot);
        } else {
            drawManaTop(g, font, width, y - 13);
        }
''',
'''        if (width >= 500) drawManaSide(g, font, startX, y, slotSize);
        else drawManaTop(g, font, width, y - 13);
''')
replace_once(hud,
'''                String name = compactName(spell.name(), 3);
                g.centeredText(font, Component.literal(name), x + size / 2, y + size - 9,
                        remaining > 0 ? 0xFF8B8492 : charging ? 0xFFFFE0A2 : 0xFFDCD4E9);''',
'''                String name = fitName(font, spell.name(), size - 5);
                g.centeredText(font, Component.literal(name), x + size / 2, y + size - 10,
                        remaining > 0 ? 0xFF8B8492 : charging ? 0xFFFFE0A2 : 0xFFDCD4E9);''')
replace_once(hud,
'''        } else if (charging) {
            String charge = ArcaneClientState.chargingTicks() >= 20 ? "READY" : "...";
            g.centeredText(font, Component.literal(charge), x + size / 2, y - 10, 0xFFFFD36B);
        }
''',
'''        } else if (charging) {
            int progress = (int) Math.round((size - 4) * ArcaneClientState.chargingFraction());
            g.fill(x + 2, y + size - 4, x + size - 2, y + size - 2, 0xFF282D38);
            g.fill(x + 2, y + size - 4, x + 2 + progress, y + size - 2,
                    ArcaneClientState.chargingReady() ? 0xFFFFD36B : color);
        }
''')
replace_once(hud,
'''    private static String compactName(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }
''',
'''    private static String fitName(Font font, String value, int pixels) {
        if (value == null || pixels <= 0) return "";
        if (font.width(value) <= pixels) return value;
        String suffix = "…";
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + suffix) > pixels) end--;
        return end <= 0 ? suffix : value.substring(0, end) + suffix;
    }

    private static String compactName(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }
''')

world = JAVA / "world/MagicWorldService.java"
replace_once(world, "            player.setGameMode(GameType.ADVENTURE);", "            player.setGameMode(GameType.SURVIVAL);")
replace_once(world,
'''    public static void onRespawn(ServerPlayer player) {
        ArcaneWorldData data = ArcaneWorldData.get(((ServerLevel) player.level()).getServer());''',
'''    public static void onRespawn(ServerPlayer player) {
        if (!player.isCreative() && !player.isSpectator()) player.setGameMode(GameType.SURVIVAL);
        ArcaneWorldData data = ArcaneWorldData.get(((ServerLevel) player.level()).getServer());''')
replace_once(world,
'''    public static void tick(ServerPlayer player) {
        player.getFoodData().setFoodLevel(20);''',
'''    public static void tick(ServerPlayer player) {
        if (player.tickCount % 100 == 0 && !player.isCreative() && !player.isSpectator()) {
            player.setGameMode(GameType.SURVIVAL);
        }
        player.getFoodData().setFoodLevel(20);''')

catalog = JAVA / "magic/SpellCatalog.java"
name_changes = {
    '"마법 화살"': '"매직 미사일"', '"화염 화살"': '"파이어 볼트"', '"냉기 광선"': '"프로스트 레이"',
    '"방패"': '"아케인 실드"', '"깃털 낙하"': '"페더 폴"', '"빛"': '"라이트"',
    '"기름막"': '"그리스"', '"수면"': '"슬립"', '"천둥파동"': '"썬더 웨이브"',
    '"마법 갑주"': '"메이지 아머"', '"작열 광선"': '"스코칭 레이"', '"안개 걸음"': '"미스티 스텝"',
    '"거미줄"': '"웹"', '"거울상"': '"미러 이미지"', '"투명화"': '"인비저빌리티"',
    '"돌풍"': '"거스트"', '"인간형 속박"': '"홀드"', '"분쇄"': '"섀터"',
    '"흐릿함"': '"블러"', '"부유"': '"레비테이트"', '"화염구"': '"파이어볼"',
    '"번개 줄기"': '"라이트닝 볼트"', '"비행"': '"플라이"', '"가속"': '"헤이스트"',
    '"마법 해제"': '"디스펠"', '"흡혈의 손길"': '"뱀파릭 터치"', '"둔화"': '"슬로우"',
    '"지연 폭발 화염구"': '"딜레이드 파이어볼"', '"유성우"': '"메테오 스트라이크"',
    '"시간 정지"': '"타임 스톱"', '"게이트"': '"월드 게이트"',
    '"불타는 손"': '"플레임 버스트"', '"얼음 칼"': '"프로스트 랜스"',
    '"색채 구체"': '"엘리멘탈 오브"', '"바람벽"': '"스톰 배리어"',
    '"주문 반사"': '"스펠 브레이커"', '"화염 방패"': '"블레이징 이지스"',
    '"얼음벽"': '"글레이셜 월"', '"연쇄 번개"': '"체인 라이트닝"',
    '"비전의 손"': '"아스트랄 핸드"', '"순간이동진"': '"아케인 게이트"',
}
text = catalog.read_text(encoding="utf-8")
for old, new in name_changes.items():
    text = text.replace(old, new)
catalog.write_text(text, encoding="utf-8")

verify = ROOT / "tools/verify_jar.py"
replace_once(verify, "Arcane Circle v0.8 JAR verification", "Arcane Circle v0.9 JAR verification")
index = ROOT / "src/main/resources/data/arcanecircle/spell_catalog/index.json"
if index.exists():
    replace_once(index, '"version": "0.8.0-alpha.1"', '"version": "0.9.0-alpha.1"')

print("Arcane Circle v0.9 fixed UI, staged casting, HUD, naming and builder-world migration: PASS")

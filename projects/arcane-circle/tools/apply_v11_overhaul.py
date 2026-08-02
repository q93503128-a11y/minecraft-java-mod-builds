#!/usr/bin/env python3
from pathlib import Path
import json, subprocess, sys

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"
subprocess.run([sys.executable, str(ROOT / "tools/apply_v10_overhaul.py")], check=True)


def patch(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"missing patch token in {path}: {old[:90]!r}")
    path.write_text(text.replace(old, new), encoding="utf-8")


# Version and protocol.
patch(ROOT / "gradle.properties", "mod_version=0.10.0-alpha.1", "mod_version=0.11.0-alpha.1")
patch(JAVA / "ArcaneCircle.java", 'VERSION = "0.10.0-alpha.1"', 'VERSION = "0.11.0-alpha.1"')
patch(JAVA / "network/ArcaneNetwork.java", 'PROTOCOL_VERSION = "ninefold-arcana-10"',
      'PROTOCOL_VERSION = "ninefold-arcana-11"')
index_path = ROOT / "src/main/resources/data/arcanecircle/spell_catalog/index.json"
index = json.loads(index_path.read_text(encoding="utf-8"))
index["version"] = "0.11.0-alpha.1"
index_path.write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# Compact square world HUD and a vector-only charging seal. No particle is involved in seal construction.
(JAVA / "client/ArcaneHud.java").write_text(r'''package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

public final class ArcaneHud {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "spell_hotbar");

    private ArcaneHud() {}

    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, ArcaneHud::renderWorldHud);
    }

    private static void renderWorldHud(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null || !ArcaneClientState.ready()) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Font font = minecraft.font;

        drawCastingSigil(g, width, height);

        int gap = 2;
        int slotSize = width >= 520 ? 36 : width >= 360 ? 32 : 28;
        int total = slotSize * 5 + gap * 4;
        int startX = Math.max(4, (width - total) / 2);
        int y = Math.max(8, height - slotSize - 58);
        drawMana(g, font, startX, y, slotSize);
        for (int slot = 0; slot < 5; slot++) {
            drawSlot(g, font, startX + slot * (slotSize + gap), y, slotSize, slot);
        }
        drawFusionQueue(g, font, width, y - 21);
    }

    private static void drawMana(GuiGraphicsExtractor g, Font font, int startX, int y, int size) {
        int mana = ArcaneClientState.integer("mana", 0);
        int max = Math.max(1, ArcaneClientState.integer("max", 100));
        int w = Math.min(72, Math.max(48, startX - 10));
        int x = Math.max(4, startX - w - 6);
        int fill = (int) Math.round((w - 2) * Math.min(1.0, mana / (double) max));
        g.text(font, Component.literal(ArcaneClientState.integer("circle", 1) + "C " + mana + "/" + max),
                x, y + 5, 0xFFE7DDF7);
        g.fill(x, y + 19, x + w, y + 24, 0xD9050912);
        g.fill(x + 1, y + 20, x + 1 + fill, y + 23, 0xEF5E8EEB);
    }

    private static void drawSlot(GuiGraphicsExtractor g, Font font, int x, int y, int size, int slot) {
        SpellDefinition spell = SpellCatalog.spell(ArcaneClientState.slot(slot)).orElse(null);
        int color = spell == null ? 0xFF606475 : ArcaneRenderUtil.schoolColor(spell.school());
        int dark = spell == null ? 0xFF121620 : ArcaneRenderUtil.schoolDark(spell.school());
        int remaining = ArcaneClientState.cooldownRemainingTicks(slot);
        boolean charging = ArcaneClientState.isChargingSlot(slot);

        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, charging ? 0xFFFFD36B : 0xD9040610);
        g.fill(x, y, x + size, y + size, remaining > 0 ? dark : 0xEB101827);
        g.fill(x, y + size - 2, x + size, y + size, color);
        g.text(font, Component.literal(Integer.toString(slot + 1)), x + 2, y + 1, 0xFF98A3B7);

        if (spell == null) return;
        int iconY = y + 13;
        ArcaneRenderUtil.ring(g, x + size / 2, iconY, Math.max(6, size / 5), remaining > 0 ? 0xFF686A74 : color);
        ArcaneRenderUtil.spellRune(g, x + size / 2, iconY, spell, Math.max(4, size / 7),
                remaining > 0 ? 0xFF777A84 : 0xFFF8F2FF);
        String name = fitName(font, spell.name(), size - 3);
        g.centeredText(font, Component.literal(name), x + size / 2, y + size - 10,
                remaining > 0 ? 0xFF7E7F88 : charging ? 0xFFFFE0A2 : 0xFFD8D1E1);

        if (remaining > 0) {
            String seconds = remaining >= 200 ? Integer.toString((int) Math.ceil(remaining / 20.0))
                    : String.format("%.1f", remaining / 20.0);
            g.centeredText(font, Component.literal(seconds), x + size / 2, iconY - 4, 0xFFFFFFFF);
            int fill = (int) Math.round((size - 2) * ArcaneClientState.cooldownFraction(slot));
            g.fill(x + 1, y + size - 3, x + 1 + fill, y + size - 1, 0xFFE46D78);
        } else if (charging) {
            int fill = (int) Math.round((size - 2) * ArcaneClientState.chargingFraction());
            g.fill(x + 1, y + size - 3, x + 1 + fill, y + size - 1,
                    ArcaneClientState.chargingReady() ? 0xFFFFD36B : color);
        }
    }

    /** Screen-space vector seal: exact circle count, progressive line construction, zero particles. */
    private static void drawCastingSigil(GuiGraphicsExtractor g, int width, int height) {
        String id = ArcaneClientState.chargingSpell();
        SpellDefinition spell = SpellCatalog.spell(id).orElse(null);
        if (spell == null) return;
        double progress = Math.max(0.0, Math.min(1.0, ArcaneClientState.chargingFraction()));
        int cx = width / 2;
        int cy = height / 2 + 18;
        int radius = Math.min(34, 13 + spell.circle() * 2);
        int color = ArcaneRenderUtil.schoolColor(spell.school());
        int faint = (color & 0x00FFFFFF) | 0x88000000;

        // 1C = one concentric boundary, 2C = two, and so on through 9C.
        for (int ring = 0; ring < spell.circle(); ring++) {
            int r = Math.max(5, radius - ring * Math.max(2, radius / 12));
            double local = Math.max(0.0, Math.min(1.0, progress * spell.circle() - ring));
            partialRing(g, cx, cy, r, local, ring == 0 ? color : faint);
        }
        if (progress < 0.18) return;

        int inner = Math.max(5, radius / 2);
        ArcaneRenderUtil.spellRune(g, cx, cy, spell, inner, progress >= 1.0 ? 0xFFFFFFFF : color);
        int spokes = Math.min(12, 3 + spell.circle());
        int completed = (int) Math.floor(spokes * Math.min(1.0, Math.max(0.0, (progress - 0.22) / 0.58)));
        for (int i = 0; i < completed; i++) {
            double a = Math.PI * 2.0 * i / spokes - Math.PI / 2.0;
            int x1 = cx + (int) Math.round(Math.cos(a) * (inner + 2));
            int y1 = cy + (int) Math.round(Math.sin(a) * (inner + 2));
            int x2 = cx + (int) Math.round(Math.cos(a) * (radius - 1));
            int y2 = cy + (int) Math.round(Math.sin(a) * (radius - 1));
            ArcaneRenderUtil.line(g, x1, y1, x2, y2, faint);
        }

        if (spell.circle() >= 3 && progress >= 0.62) {
            int satellites = Math.min(6, spell.circle() - 1);
            for (int i = 0; i < satellites; i++) {
                double a = Math.PI * 2.0 * i / satellites - Math.PI / 2.0;
                int sx = cx + (int) Math.round(Math.cos(a) * (radius + 6));
                int sy = cy + (int) Math.round(Math.sin(a) * (radius + 6));
                ArcaneRenderUtil.ring(g, sx, sy, 3 + spell.circle() / 4, faint);
                ArcaneRenderUtil.diamond(g, sx, sy, 2, color);
            }
        }
        if (progress >= 1.0) ArcaneRenderUtil.ring(g, cx, cy, radius + 2, 0xFFFFE7A3);
    }

    private static void partialRing(GuiGraphicsExtractor g, int cx, int cy, int radius, double progress, int color) {
        int points = Math.max(32, radius * 7);
        int shown = (int) Math.ceil(points * progress);
        for (int i = 0; i < shown; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / points;
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int y = cy + (int) Math.round(Math.sin(angle) * radius);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void drawFusionQueue(GuiGraphicsExtractor g, Font font, int width, int y) {
        List<String> queue = ArcaneClientState.queue();
        if (queue.isEmpty()) return;
        String result = ArcaneClientState.queueResult();
        int boxWidth = Math.min(width - 12, 230);
        int x = (width - boxWidth) / 2;
        g.fill(x, y, x + boxWidth, y + 16, 0xED080B16);
        g.fill(x, y, x + boxWidth, y + 2, result.isBlank() ? 0xFF7E67AD : 0xFFFFC861);
        String chain = queue.stream().map(id -> SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id))
                .reduce((a, b) -> a + "+" + b).orElse("");
        String suffix = result.isBlank() ? "" : "→" + SpellCatalog.spell(result).map(SpellDefinition::name).orElse(result);
        g.centeredText(font, Component.literal(compactName("X " + chain + suffix, 28)), width / 2, y + 4,
                result.isBlank() ? 0xFFD4B8F1 : 0xFFFFD889);
    }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen) || !ArcaneClientState.ready()) return;
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        GuiGraphicsExtractor g = event.getGuiGraphics();
        Font font = minecraft.font;
        int inventoryRight = width / 2 + 88;
        int inventoryLeft = width / 2 - 88;
        int sideSpace = Math.max(inventoryLeft, width - inventoryRight);
        if (sideSpace < 142) return;
        int panelW = Math.min(154, sideSpace - 12);
        int x = inventoryRight + 7;
        if (x + panelW > width - 5) x = inventoryLeft - panelW - 7;
        int y = Math.max(5, (height - 104) / 2);
        panel(g, x, y, panelW, 104, "마력핵");
        int lineY = y + 27;
        g.text(font, Component.literal(ArcaneClientState.integer("circle", 1) + "C  MP "
                + ArcaneClientState.integer("mana", 0) + "/" + ArcaneClientState.integer("max", 100)),
                x + 7, lineY, 0xFFC9D8F2);
        g.text(font, Component.literal("회복 " + String.format("%.1f", ArcaneClientState.regenPerSecond()) + "/초"),
                x + 7, lineY + 14, 0xFF8ED6C0);
        g.text(font, Component.literal(compactName(ArcaneClientState.text("staff", "맨손"), 18)),
                x + 7, lineY + 30, 0xFFFFD58D);
        g.text(font, Component.literal("C 마도서 · 1~5 시전 · X 융합"), x + 7, y + 85, 0xFF81778F);
    }

    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, String title) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF604779);
        g.fill(x, y, x + w, y + h, 0xF20A0F1D);
        g.fill(x + 3, y + 3, x + w - 3, y + 21, 0xD1241A38);
        g.centeredText(Minecraft.getInstance().font, Component.literal(title), x + w / 2, y + 8, 0xFFEAD9FF);
    }

    private static String fitName(Font font, String value, int pixels) {
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
}
''', encoding="utf-8")

# Number keys never move the vanilla hotbar. 1-5 remain spell inputs; 6-9 are inert number keys.
client = JAVA / "client/ArcaneClient.java"
patch(client,
'''        for (int slot = 0; slot < SLOT_KEYS.length; slot++) {
            if (!SLOT_KEYS[slot].isDown()) continue;
            while (minecraft.options.keyHotbarSlots[slot].consumeClick()) {}
        }''',
'''        for (int slot = 0; slot < minecraft.options.keyHotbarSlots.length; slot++) {
            while (minecraft.options.keyHotbarSlots[slot].consumeClick()) {}
        }''')
patch(client, "same physical 1-5 press is consumed", "all physical 1-9 presses are consumed")

# Charging remains active after completion and fires only when the player releases the key.
casting = JAVA / "magic/SpellCastingService.java"
patch(casting, "        private int lastStage = -1;\n        private long lastReadyPulse;\n", "")
patch(casting,
'''        SpellSigilService.renderChargeStep(player, cast.spell(), cast.range(), 0);
        charge.lastStage = 0;
''', "")
patch(casting,
'''        int stage = Math.min(SpellSigilService.CHARGE_STAGES - 1,
                (int) (elapsed * SpellSigilService.CHARGE_STAGES / Math.max(1, charge.requiredTicks)));
        if (stage > charge.lastStage) {
            for (int next = charge.lastStage + 1; next <= stage; next++) {
                SpellSigilService.renderChargeStep(player, spell, cast.range(), next);
            }
            charge.lastStage = stage;
        }

        if (elapsed >= charge.requiredTicks) {
            CHARGES.remove(player.getUUID());
            castPrepared(player, data, cast);
        }
''',
'''        // The client draws one persistent vector seal. Completion only arms the spell;
        // releaseSlotCharge performs the cast when the player lets go.
''')
patch(casting, "return Math.max(0, base - circleGapReduction - masteryReduction);",
      "return Math.max(2, base - circleGapReduction - masteryReduction);")
patch(casting, "        SpellSigilService.renderRelease(player, spell, cast.range());\n", "")

# Add distinct first-circle gameplay details instead of three near-identical bolts.
patch(casting,
'''            case "arcane_dart" -> bolt(player, range, power, ParticleTypes.ENCHANT, 0, 0);
            case "ember" -> bolt(player, range, power, ParticleTypes.FLAME, 100, 0);
            case "frost_needle" -> bolt(player, range, power, ParticleTypes.SNOWFLAKE, 0, 90);''',
'''            case "arcane_dart" -> arcaneDart(player, range, power);
            case "ember" -> emberShot(player, range, power);
            case "frost_needle" -> frostNeedle(player, range, power);''')
patch(casting,
'''    private static boolean bolt(ServerPlayer player, double range, double power, ParticleOptions particle,
''',
'''    private static boolean arcaneDart(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> target = lookTarget(player, range);
        Vec3 start = frontOrigin(player, 1.25);
        Vec3 end = target.map(Mob::getEyePosition).orElse(start.add(player.getLookAngle().normalize().scale(range)));
        Vec3 side = new Vec3(-player.getLookAngle().z, 0.0, player.getLookAngle().x).normalize().scale(0.16);
        particleLine(level, start.add(side), end, ParticleTypes.ENCHANT, 22);
        particleLine(level, start.subtract(side), end, ParticleTypes.END_ROD, 22);
        target.ifPresent(mob -> {
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 50, 0));
            burst(level, mob.getEyePosition(), ParticleTypes.ENCHANT, 14, 0.32);
        });
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.45F, 1.65F);
        return true;
    }

    private static boolean emberShot(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> target = lookTarget(player, range);
        boolean result = bolt(player, range, power, ParticleTypes.FLAME, 120, 0);
        target.ifPresent(primary -> {
            for (Mob mob : nearbyTargets(player, primary.position(), 1.8, 1.5)) {
                if (mob == primary) continue;
                mob.hurtServer(level, level.damageSources().magic(), (float) (power * 0.35));
                mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 60));
            }
            burst(level, primary.position().add(0.0, 0.7, 0.0), ParticleTypes.FLAME, 24, 0.65);
        });
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS, 0.55F, 1.25F);
        return result;
    }

    private static boolean frostNeedle(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> target = lookTarget(player, range);
        boolean result = bolt(player, range, power * 0.92, ParticleTypes.SNOWFLAKE, 0, 140);
        target.ifPresent(mob -> {
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 75, 2));
            mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 150));
            burst(level, mob.getEyePosition(), ParticleTypes.SNOWFLAKE, 20, 0.42);
        });
        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 0.38F, 1.75F);
        return result;
    }

    private static boolean bolt(ServerPlayer player, double range, double power, ParticleOptions particle,
''')

# Restore the full-size grimoire window; compact controls remain compact within it.
screen = JAVA / "client/GrimoireScreen.java"
start = '''    private Layout layout() {
        int targetH = switch (page) {
            case "atlas" -> atlasCircle == 0 ? 150 : 238;
            case "academy" -> academyCircle == 0 ? 188 : 278;
            case "core" -> 210;
            default -> 300;
        };
        int panelW = Math.min(620, Math.max(300, width - 28));
        int panelH = Math.min(targetH, Math.max(180, height - 24));
        panelW = Math.min(panelW, Math.max(1, width - 10));
        panelH = Math.min(panelH, Math.max(1, height - 10));
        return new Layout((width - panelW) / 2, (height - panelH) / 2, panelW, panelH);
    }
'''
replacement = '''    private Layout layout() {
        int panelW = Math.min(720, Math.max(360, width - 40));
        int panelH = Math.min(410, Math.max(260, height - 36));
        panelW = Math.min(panelW, Math.max(1, width - 12));
        panelH = Math.min(panelH, Math.max(1, height - 12));
        return new Layout((width - panelW) / 2, (height - panelH) / 2, panelW, panelH);
    }
'''
patch(screen, start, replacement)
patch(screen, "int cols=c.w()>=520?9:3", "int cols=c.w()>=420?9:3")
patch(screen, "int cols=c.w()>=520?9:3,gap=4", "int cols=c.w()>=420?9:3,gap=4")

# Bring the broad contract forward after v0.10 regenerated it.
contract = ROOT / "tools/test_magic_contract.py"
text = contract.read_text(encoding="utf-8")
text = text.replace("0.10.0-alpha.1", "0.11.0-alpha.1")
text = text.replace("apply_v10_overhaul.py", "apply_v11_overhaul.py")
text = text.replace("single-pass casting", "release-cast vector sigils")
text = text.replace('"SpellSigilService.renderChargeStep", "SpellSigilService.renderRelease",', '')
text = text.replace('need(sigils, [\n    "renderChargeStep", "renderRelease", "CHARGE_STAGES",',
                    'need(sigils, [\n    "renderChargeStep", "renderRelease", "CHARGE_STAGES",')
text = text.replace('need(hud, ["fitName", "chargingFraction"], "compact spell HUD")',
                    'need(hud, ["fitName", "chargingFraction", "drawCastingSigil", "partialRing"], "compact spell HUD")')
contract.write_text(text, encoding="utf-8")

print("Arcane Circle v0.11 square HUD, vector sigils, release casting and detailed starter spells: PASS")

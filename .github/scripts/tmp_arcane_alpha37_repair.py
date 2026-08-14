from pathlib import Path
import re
root=Path('projects/arcane-circle')
client=root/'src/main/java/kr/moonseungjun/arcanecircle/client'
magic=root/'src/main/java/kr/moonseungjun/arcanecircle/magic'

def rep(path,old,new,count=1):
    s=path.read_text(encoding='utf-8'); n=s.count(old)
    if n!=count: raise SystemExit(f'{path}: expected {count}, got {n}: {old[:90]!r}')
    path.write_text(s.replace(old,new,count),encoding='utf-8')

# Charge recharge must begin after a charge is actually spent, not from initial cast time.
buff=magic/'ArcaneBuffRuntime.java'
rep(buff,
'''        State armor = active(player, "mage_armor", now);
        if (armor != null) {
            multiplier = Math.min(multiplier, armor.charges > 0 ? .68 : .84);
            flat = Math.max(flat, .8);
            if (armor.charges > 0) armor.charges--;
        }
''',
'''        State armor = active(player, "mage_armor", now);
        if (armor != null) {
            multiplier = Math.min(multiplier, armor.charges > 0 ? .68 : .84);
            flat = Math.max(flat, .8);
            if (armor.charges > 0) {
                armor.charges--;
                if (armor.nextChargeAt <= now) armor.nextChargeAt = now + rechargeInterval("mage_armor");
            }
        }
''')
rep(buff,
'''        State solar = active(player, "solar_guard", now);
        if (solar != null && solar.charges > 0) {
            solar.charges--;
            multiplier = Math.min(multiplier, .58);
''',
'''        State solar = active(player, "solar_guard", now);
        if (solar != null && solar.charges > 0) {
            solar.charges--;
            if (solar.nextChargeAt <= now) solar.nextChargeAt = now + rechargeInterval("solar_guard");
            multiplier = Math.min(multiplier, .58);
''')

# Existing unique buffs also need visual lifetime parity.
gameplay=magic/'SpellGameplayService.java'
rep(gameplay,
'''            case "time_stop" -> ArcaneFieldService.TIME_STOP_TICKS;
            case "antimagic_field" -> ArcaneFieldService.ANTIMAGIC_TICKS;
            case "grease" -> 160;
''',
'''            case "time_stop" -> ArcaneFieldService.TIME_STOP_TICKS;
            case "antimagic_field" -> ArcaneFieldService.ANTIMAGIC_TICKS;
            case "feather_fall" -> 120;
            case "mirror_image" -> 260;
            case "blur" -> 360;
            case "fly" -> 600;
            case "simulacrum" -> 1200;
            case "clone" -> 1800;
            case "grease" -> 160;
''')
rep(gameplay,'            case "globe_of_invulnerability" -> 360;\n','            case "globe_of_invulnerability" -> 520;\n')
rep(gameplay,'            case "fire_shield" -> 300;\n','            case "fire_shield" -> 620;\n')

# Persistent silhouettes for unique existing buffs, including flight/death substitutes.
over=client/'ArcaneSpellVisualOverhaul.java'
rep(over,
'''    private static final Set<String> BUFFS = Set.of(
            "shield", "mage_armor", "mirror_image", "invisibility", "blur", "haste",
            "protection_from_energy", "greater_invisibility", "resilient_sphere", "stoneskin",
            "freedom_of_movement", "true_seeing", "globe_of_invulnerability", "fire_shield",
            "solar_guard", "shapechange", "foresight");
''',
'''    private static final Set<String> BUFFS = Set.of(
            "shield", "feather_fall", "mage_armor", "mirror_image", "invisibility", "blur", "fly", "haste",
            "protection_from_energy", "greater_invisibility", "resilient_sphere", "stoneskin",
            "freedom_of_movement", "true_seeing", "globe_of_invulnerability", "simulacrum", "clone",
            "fire_shield", "solar_guard", "shapechange", "foresight");
''')
rep(over,
'''        SpellPresentationProfile.MotionStyle motion = SpellPresentationProfile.profile(spell).motion();
        switch (motion) {
''',
'''        SpellPresentationProfile.MotionStyle motion = SpellPresentationProfile.profile(spell).motion();
        boolean persistentBuff = BUFFS.contains(spell.id());
        if (persistentBuff) buffMantle(m, spell, targetOffset, rise, elapsedSeconds);
        switch (motion) {
''')
rep(over,
'''            case AURA -> {
                if (BUFFS.contains(spell.id())) buffMantle(m, spell, targetOffset, rise, elapsedSeconds);
                else auraMantle(m, spell, targetOffset, rise, elapsedSeconds);
            }
''',
'''            case AURA -> {
                if (!persistentBuff) auraMantle(m, spell, targetOffset, rise, elapsedSeconds);
            }
''')
# Add three physically different maintained signatures.
needle='''            case "shield" -> {
                Vec3 c = center.add(0, 1.15, .62);
'''
replacement='''            case "feather_fall" -> {
                for (int sideSign : new int[]{-1, 1}) {
                    for (int i = 0; i < 3; i++) {
                        double y = .45 + i * .42;
                        Vec3 root = center.add(sideSign * .20, y, 0);
                        Vec3 tip = center.add(sideSign * (.62 + i * .14), y + .18, .08 * Math.sin(time * 1.4 + i));
                        m.line(root, tip, i == 0 ? .64F : .38F);
                        m.line(tip, tip.add(sideSign * .18, -.12, 0), .30F);
                    }
                }
                m.arc(g, center.add(0, .08, 0), .72, time * .18, Math.PI * 1.25, 28, .42F);
            }
            case "fly" -> {
                for (int sideSign : new int[]{-1, 1}) {
                    Vec3 root = center.add(sideSign * .18, 1.15, 0);
                    for (int i = 0; i < 4; i++) {
                        Vec3 elbow = center.add(sideSign * (.55 + i * .22), 1.55 - i * .13, .08 * Math.sin(time * 1.7 + i));
                        Vec3 tip = center.add(sideSign * (1.05 + i * .18), 1.20 - i * .20, .12 * Math.cos(time * 1.3 + i));
                        m.line(root, elbow, i == 0 ? .72F : .46F);
                        m.line(elbow, tip, .38F);
                    }
                }
                m.helix(center.add(0, .05, 0), new Vec3(0, 1, 0), front, 1.55, .38, 2, 34, .34F, true);
            }
            case "simulacrum" -> {
                Vec3 echo = center.add(-.92, .98, .12);
                m.diamond(front, echo, .52, time * .035, 1.08F, .18F);
                m.runeGlyph(front, echo, .23, seed ^ 0x51A0, -time * .05, .48F);
                m.line(center.add(0, .80, 0), echo, .30F);
                m.circle(g, center.add(0, .04, 0), .82, 40, .38F);
            }
            case "clone" -> {
                Vec3 core = center.add(0, .52, -.72);
                m.circle(front, core, .58, 38, .58F);
                m.polygon(front, core, .43, 6, time * .045, .42F);
                m.runeGlyph(front, core, .20, seed ^ 0xC10E, -time * .035, .46F);
                m.brokenBand(g, center.add(0, .04, 0), .96, 1.08, 48, 7, 1.02F, .10F);
            }
            case "shield" -> {
                Vec3 c = center.add(0, 1.15, .62);
'''
rep(over,needle,replacement)

# Audit parity regressions.
audit=root/'tools/test_current_source.py'
text=audit.read_text(encoding='utf-8')
marker='# Alpha.37 non-potion buff identity + 3D high-circle authority.\n'
if marker not in text: raise SystemExit('audit alpha37 marker missing')
extra='''# Maintained-buff visual lifetime parity.\nfor token in ['case "feather_fall" -> 120','case "mirror_image" -> 260','case "blur" -> 360',\n              'case "fly" -> 600','case "simulacrum" -> 1200','case "clone" -> 1800',\n              'case "globe_of_invulnerability" -> 520','case "fire_shield" -> 620']:\n    assert token in gameplay, token\nfor token in ['"feather_fall"','"fly"','"simulacrum"','"clone"','persistentBuff']:\n    assert token in overhaul, token\nassert 'armor.nextChargeAt = now + rechargeInterval("mage_armor")' in buff\nassert 'solar.nextChargeAt = now + rechargeInterval("solar_guard")' in buff\n\n'''
text=text.replace(marker,extra+marker,1)
audit.write_text(text,encoding='utf-8')
print('alpha37 repair applied')

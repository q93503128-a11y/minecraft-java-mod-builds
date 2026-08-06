from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def write(relative: str, text: str) -> None:
    (ROOT / relative).write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"missing alpha.14 marker: {label}")
    if text.count(old) != 1:
        raise SystemExit(f"ambiguous alpha.14 marker: {label} ({text.count(old)})")
    return text.replace(old, new, 1)


def replace_between(text: str, start: str, end: str, replacement: str, label: str) -> str:
    begin = text.find(start)
    if begin < 0:
        raise SystemExit(f"missing alpha.14 method start: {label}")
    finish = text.find(end, begin)
    if finish < 0:
        raise SystemExit(f"missing alpha.14 method end: {label}")
    return text[:begin] + replacement.rstrip() + "\n\n" + text[finish:]


# Version bump.
props = read("gradle.properties")
if "mod_version=0.12.1-alpha.14" not in props:
    if "mod_version=0.12.1-alpha.13" in props:
        props = props.replace("mod_version=0.12.1-alpha.13", "mod_version=0.12.1-alpha.14", 1)
    elif "mod_version=0.12.1-alpha.12" in props:
        props = props.replace("mod_version=0.12.1-alpha.12", "mod_version=0.12.1-alpha.14", 1)
    else:
        raise SystemExit("unsupported Arcane Circle source version")
write("gradle.properties", props)


# Equipment floor and progression layers are deliberately separate.
magic_path = "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java"
magic = read(magic_path)
old_reduction = '''        double circleMana = Math.max(0.10, Math.pow(0.72, masteryGap));
        double circleCooldown = Math.max(0.10, Math.pow(0.62, masteryGap));
        double circleRange = 1.0 + masteryGap * 0.07;
        double circlePower = 1.0 + masteryGap * 0.04;
        double masteryMana = Math.max(0.80, 1.0 - proficiency * 0.02);
        double masteryCooldown = Math.max(0.70, 1.0 - proficiency * 0.03);
        double masteryRange = 1.0 + proficiency * 0.02;
        double masteryPower = 1.0 + proficiency * 0.04;

        // Cost and cooldown reductions may never go below 10% of the spell's base value.
        double totalCostMultiplier = Math.max(0.10, circleMana * masteryMana
                * staff.manaCostMultiplier() * gear.manaCostMultiplier() * facultyMana);
        int manaCost = spell.manaCost() <= 0 ? 0
                : Math.max(1, (int) Math.ceil(spell.manaCost() * totalCostMultiplier));
        double totalCooldownMultiplier = Math.max(0.10, circleCooldown * masteryCooldown
                * staff.cooldownMultiplier() * gear.cooldownMultiplier() * facultyCooldown);
        double rawCooldown = spell.cooldownTicks() * totalCooldownMultiplier;
        // Values below 0.1 seconds are treated as absent; otherwise preserve a 2-tick minimum.
        int cooldown = spell.cooldownTicks() <= 0 || rawCooldown < 2.0
                ? 0 : Math.max(2, (int) Math.round(rawCooldown));'''
new_reduction = '''        // Circle gap and mastery are progression layers, not equipment. They remain free to
        // push old low-circle spells below the equipment floor and eventually to zero cooldown.
        double circleMana = Math.pow(0.72, masteryGap);
        double circleCooldown = Math.pow(0.62, masteryGap);
        double circleRange = 1.0 + masteryGap * 0.07;
        double circlePower = 1.0 + masteryGap * 0.04;
        double masteryMana = Math.max(0.80, 1.0 - proficiency * 0.02);
        double masteryCooldown = Math.max(0.70, 1.0 - proficiency * 0.03);
        double masteryRange = 1.0 + proficiency * 0.02;
        double masteryPower = 1.0 + proficiency * 0.04;

        // Only staff and wearable equipment share the 10% floor. Circle, mastery and
        // affiliation modifiers are multiplied afterwards as additional progression.
        double equipmentCostMultiplier = Math.max(0.10,
                staff.manaCostMultiplier() * gear.manaCostMultiplier());
        double progressionCostMultiplier = circleMana * masteryMana * facultyMana;
        double totalCostMultiplier = equipmentCostMultiplier * progressionCostMultiplier;
        int manaCost = spell.manaCost() <= 0 ? 0
                : Math.max(1, (int) Math.ceil(spell.manaCost() * totalCostMultiplier));

        double equipmentCooldownMultiplier = Math.max(0.10,
                staff.cooldownMultiplier() * gear.cooldownMultiplier());
        double progressionCooldownMultiplier = circleCooldown * masteryCooldown * facultyCooldown;
        double rawCooldown = spell.cooldownTicks()
                * equipmentCooldownMultiplier * progressionCooldownMultiplier;
        // A final value below 0.1 seconds is treated as no cooldown.
        int cooldown = spell.cooldownTicks() <= 0 || rawCooldown < 2.0
                ? 0 : Math.max(2, (int) Math.round(rawCooldown));'''
if old_reduction in magic:
    magic = replace_once(magic, old_reduction, new_reduction, "reduction layering")
elif "double equipmentCostMultiplier = Math.max(0.10," not in magic:
    raise SystemExit("neither old nor corrected reduction block found")
write(magic_path, magic)


tracker_path = "src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java"
tracker = read(tracker_path)
tracker = tracker.replace("private static final int MAX_CHARGE_GEOMETRY = 520;",
                          "private static final int MAX_CHARGE_GEOMETRY = 920;", 1)
tracker = tracker.replace("private static final int MAX_RELEASE_GEOMETRY = 760;",
                          "private static final int MAX_RELEASE_GEOMETRY = 1280;", 1)
tracker = tracker.replace("private static final int MAX_FRAME = 2800;",
                          "private static final int MAX_FRAME = 4200;", 1)

old_sets = '''    private static final Set<String> WAVES = Set.of(
            "thunderwave", "gust_of_wind", "burning_hands", "cone_of_cold",
            "shatter", "steam_burst", "world_sunder", "flame_wave", "wind_blade");'''
new_sets = '''    private static final Set<String> WAVES = Set.of(
            "thunderwave", "gust_of_wind", "burning_hands", "cone_of_cold",
            "shatter", "steam_burst", "world_sunder", "flame_wave", "wind_blade");
    private static final Set<String> METEOR_FORMS = Set.of(
            "flame_strike", "delayed_blast_fireball", "fire_storm", "meteor_swarm", "sunburst");
    private static final Set<String> PORTAL_FORMS = Set.of(
            "misty_step", "blink", "dimension_door", "passwall", "plane_shift",
            "teleport", "demiplane", "gate");
    private static final Set<String> PRISON_FORMS = Set.of(
            "hold_person", "hold_monster", "resilient_sphere", "forcecage",
            "astral_prison", "maze");
    private static final Set<String> WALL_FORMS = Set.of(
            "wall_of_fire", "wind_wall", "wall_of_ice", "wall_of_force", "prismatic_wall");
    private static final Set<String> STORM_FORMS = Set.of(
            "sleet_storm", "ice_storm", "cloudkill", "insect_plague", "control_weather",
            "incendiary_cloud", "winter_domain");
    private static final Set<String> ORB_FORMS = Set.of(
            "fireball", "chromatic_orb", "ice_knife", "freezing_sphere",
            "delayed_blast_fireball", "solar_guard");
    private static final Set<String> LANCE_FORMS = Set.of(
            "fire_bolt", "void_lance", "finger_of_death", "arcane_hand");'''
if old_sets in tracker:
    tracker = replace_once(tracker, old_sets, new_sets, "visual family sets")
elif "private static final Set<String> METEOR_FORMS" not in tracker:
    raise SystemExit("visual family insertion marker missing")

new_charge = '''    private static ArcaneWorldMesh buildCharge(Visual visual) {
        SpellDefinition spell = visual.spell;
        ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.builder(MAX_CHARGE_GEOMETRY);
        ArcaneWorldMesh.Basis basis = basis(spell, visual.direction);
        int circle = clampCircle(spell.circle());
        double p = Math.max(0.06, visual.progress);
        double scale = clamp(Math.pow(Math.max(0.2,
                visual.range / Math.max(4.0, spell.range())), 0.22), 0.86, 1.62);
        double outer = (0.46 + circle * 0.125 + circle * circle * 0.008
                + (visual.fusion ? 0.24 : 0.0)) * scale;
        double rotation = Math.floorMod(spell.id().hashCode(), 360) * Math.PI / 180.0
                + p * (0.18 + circle * 0.018);

        // The plate gives the glyphs a luminous surface. It is deliberately faint so terrain
        // remains readable while the filled geometry avoids the old pixel-wire appearance.
        mesh.disc(basis, Vec3.ZERO, outer * (0.90 + p * 0.08), 48 + circle * 4,
                0.42F, (float) (0.026 + p * 0.052));

        // Exactly one complete concentric band per spell circle. Width and opacity vary by layer,
        // instead of drawing every circle with the same debug-line thickness.
        for (int layer = 1; layer <= circle && !mesh.full(); layer++) {
            double t = layer / (double) circle;
            double radius = outer * (0.18 + 0.80 * t);
            double thickness = outer * (0.014 + (layer % 3) * 0.006 + circle * 0.0012);
            float brightness = (float) (0.76 + 0.34 * t + (layer % 2) * 0.08);
            float alpha = (float) ((0.11 + 0.23 * p) * (0.72 + 0.28 * t));
            mesh.band(basis, Vec3.ZERO, Math.max(0.01, radius - thickness), radius,
                    44 + layer * 4, brightness, alpha);
        }

        double sealRadius = outer * (0.34 + Math.min(0.12, circle * 0.012));
        schoolSeal(mesh, spell, basis, sealRadius, rotation, p);
        if (circle >= 2) {
            mesh.runeChords(basis, Vec3.ZERO, outer * 0.50,
                    5 + circle, 2 + circle % 3, rotation,
                    (float) (0.48 + circle * 0.035));
        }
        if (circle >= 4) {
            mesh.starPlate(basis, Vec3.ZERO, outer * 0.63, outer * 0.43,
                    4 + circle / 2, -rotation * 0.72, 0.62F,
                    (float) (0.050 + p * 0.095));
        }

        int glyphs = Math.min(18, 4 + circle * 2);
        int visible = Math.max(1, (int) Math.ceil(glyphs * p));
        for (int i = 0; i < visible && !mesh.full(); i++) {
            double angle = rotation + Math.PI * 2.0 * i / glyphs;
            Vec3 glyph = basis.point(angle, outer * (0.70 + (i % 2) * 0.045));
            double size = outer * (0.026 + (i % 4) * 0.006);
            if ((i % 3) == 0) mesh.diamond(basis, glyph, size * 1.35, -angle, 1.18F, 0.38F);
            else if ((i % 3) == 1) mesh.disc(basis, glyph, size, 14, 0.96F, 0.26F);
            else mesh.polygonPlate(basis, glyph, size * 1.15, 3, angle, 1.08F, 0.31F);
        }

        // Sixth circle and above gain gyroscopic, non-coplanar circuit layers. These are broken
        // luminous bands, not extra complete concentric circles, so the 1C-9C ring count stays exact.
        if (circle >= 6) {
            Vec3 normal = basis.normal();
            ArcaneWorldMesh.Basis tiltA = ArcaneWorldMesh.Basis.fromNormal(
                    basis.right().add(normal.scale(0.58)), basis.up());
            ArcaneWorldMesh.Basis tiltB = ArcaneWorldMesh.Basis.fromNormal(
                    basis.up().add(normal.scale(0.46)), basis.right());
            mesh.brokenBand(tiltA, Vec3.ZERO, outer * 0.84, outer * 0.89,
                    60 + circle * 3, 5, 1.10F, (float) (0.14 + p * 0.16));
            mesh.brokenBand(tiltB, Vec3.ZERO, outer * 0.67, outer * 0.72,
                    54 + circle * 3, 6, 0.92F, (float) (0.11 + p * 0.14));
        }
        if (circle >= 8) {
            mesh.orb(Vec3.ZERO, outer * (circle == 9 ? 0.46 : 0.38),
                    28 + circle * 2, 0.64F, (float) (0.040 + p * 0.050));
            mesh.starPlate(basis, Vec3.ZERO, outer * 0.27, outer * 0.10,
                    circle == 9 ? 9 : 8, rotation * 1.35, 1.28F,
                    (float) (0.18 + p * 0.20));
        }

        if (visual.fusion && visual.ingredients >= 2) {
            int count = Math.min(3, visual.ingredients);
            for (int i = 0; i < count && !mesh.full(); i++) {
                double angle = rotation + Math.PI * 2.0 * i / count;
                Vec3 node = basis.point(angle, outer * 1.20);
                double radius = outer * (0.12 + count * 0.012);
                mesh.disc(basis, node, radius, 24, 0.72F, 0.10F);
                mesh.band(basis, node, radius * 0.66, radius, 30, 1.22F, 0.38F);
                mesh.starPlate(basis, node, radius * 0.62, radius * 0.24,
                        3 + i, rotation + i, 1.08F, 0.34F);
            }
            mesh.brokenBand(basis, Vec3.ZERO, outer * 1.31, outer * 1.37,
                    72, 7, 1.08F, 0.28F);
        }
        return mesh.build();
    }'''
tracker = replace_between(tracker,
                          "    private static ArcaneWorldMesh buildCharge(Visual visual) {",
                          "    private static ArcaneWorldMesh buildRelease(Visual visual, double age) {",
                          new_charge, "buildCharge")

new_release = '''    private static ArcaneWorldMesh buildRelease(Visual visual, double age) {
        ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.builder(MAX_RELEASE_GEOMETRY);
        SpellDefinition spell = visual.spell;
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(visual.direction);
        int circle = clampCircle(spell.circle());
        double powerFactor = clamp(Math.pow(Math.max(0.08,
                visual.power / Math.max(1.0, spell.power())), 0.18), 0.82, 2.0);
        String id = spell.id();

        // Spell identity decides the body first. Anchor is only a fallback, so a portal, wall,
        // meteor and prison can no longer collapse into the same generic ground burst.
        if (METEOR_FORMS.contains(id)) buildMeteor(mesh, visual, age, powerFactor);
        else if (PORTAL_FORMS.contains(id)) buildPortal(mesh, visual, age, powerFactor);
        else if (PRISON_FORMS.contains(id)) buildPrison(mesh, visual, age, powerFactor);
        else if (WALL_FORMS.contains(id)) buildWall(mesh, visual, age, powerFactor);
        else if (STORM_FORMS.contains(id)) buildStorm(mesh, visual, age, powerFactor);
        else if (TRUE_BEAMS.contains(id)) buildBeam(mesh, visual, facing, age, powerFactor);
        else if (WAVES.contains(id)) buildWave(mesh, visual, facing, age, powerFactor);
        else if ("magic_missile".equals(id)) buildMissileSwarm(mesh, visual, facing, age, powerFactor);
        else if (ORB_FORMS.contains(id)) buildElementalOrb(mesh, visual, facing, age, powerFactor);
        else if (LANCE_FORMS.contains(id)) buildLance(mesh, visual, facing, age, powerFactor);
        else {
            switch (spell.sigilAnchor()) {
                case FEET, GROUND_SELF, GROUND_TARGET -> buildField(mesh, visual, age, powerFactor);
                case BODY -> buildAura(mesh, visual, age, powerFactor);
                case TARGET -> buildTargetBurst(mesh, visual, age, powerFactor);
                case FRONT -> buildProjectile(mesh, visual, facing, age, powerFactor);
            }
        }

        if (visual.fusion) {
            double ring = 0.48 + circle * 0.07;
            mesh.brokenBand(facing, Vec3.ZERO, ring, ring + 0.055,
                    48, 5, 1.20F, (float) (0.18 * (1.0 - age)));
        }
        return mesh.build();
    }'''
tracker = replace_between(tracker,
                          "    private static ArcaneWorldMesh buildRelease(Visual visual, double age) {",
                          "    private static void buildProjectile(ArcaneWorldMesh.Builder mesh, Visual visual,",
                          new_release, "buildRelease")

new_projectile = '''    private static void buildProjectile(ArcaneWorldMesh.Builder mesh, Visual visual,
                                        ArcaneWorldMesh.Basis facing, double age, double powerFactor) {
        SpellDefinition spell = visual.spell;
        int circle = clampCircle(spell.circle());
        double travel = Math.min(72.0, Math.max(3.0, visual.range));
        double eased = 1.0 - Math.pow(1.0 - age, 1.35);
        Vec3 position = visual.direction.scale(travel * eased);
        double core = (0.17 + circle * 0.052) * powerFactor;

        // Detached echoes communicate speed without turning every projectile into a continuous line.
        int echoes = Math.min(4, 2 + circle / 3);
        for (int i = 1; i <= echoes; i++) {
            double back = core * (1.1 + i * 1.18) + travel * 0.018 * i;
            Vec3 echo = position.subtract(visual.direction.scale(back))
                    .add(facing.point(age * 5.2 + i * 2.17, core * 0.16 * i));
            double radius = core * (0.42 - i * 0.055);
            if (radius > 0.04) mesh.orb(echo, radius, 12, 0.68F, 0.13F);
        }

        switch (spell.school()) {
            case FIRE -> {
                mesh.shard(position, visual.direction, facing, core * 3.5, core * 0.52,
                        1.20F, 0.52F);
                mesh.orb(position.add(visual.direction.scale(core * 0.45)), core * 0.72,
                        22, 1.28F, 0.42F);
                mesh.ribbon(position.subtract(visual.direction.scale(core * 2.8)), visual.direction,
                        facing, core * 3.2, core * 0.92, 2, 18, 1.08F, 0.24F);
            }
            case FROST -> {
                mesh.shard(position, visual.direction, facing, core * 4.8, core * 0.68,
                        1.18F, 0.60F);
                for (int i = 0; i < 4; i++) {
                    Vec3 crystal = position.add(facing.point(i * Math.PI / 2.0 + age,
                            core * 0.82));
                    mesh.shard(crystal, visual.direction, facing, core * 1.30,
                            core * 0.17, 1.05F, 0.36F);
                }
            }
            case WIND -> {
                mesh.ribbon(position.subtract(visual.direction.scale(core * 2.2)), visual.direction,
                        facing, core * 4.0, core * 1.4, 3, 24, 0.94F, 0.25F);
                mesh.brokenBand(facing, position, core * 0.82, core * 1.24,
                        38, 5, 1.12F, 0.34F);
            }
            case SPACE -> {
                mesh.orb(position, core * 0.76, 26, 0.66F, 0.48F);
                mesh.brokenBand(facing, position, core * 1.05, core * 1.28,
                        40, 4, 1.22F, 0.42F);
                ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                        facing.right().add(visual.direction.scale(0.6)), facing.up());
                mesh.brokenBand(tilt, position, core * 1.34, core * 1.48,
                        42, 6, 0.84F, 0.30F);
            }
            case WARD -> {
                mesh.polygonPlate(facing, position, core * 1.38, 6 + circle / 3,
                        age * 1.8, 1.08F, 0.40F);
                mesh.band(facing, position, core * 0.74, core * 1.10,
                        34, 1.24F, 0.30F);
            }
            case LIFE -> {
                mesh.orb(position, core, 24, 1.18F, 0.44F);
                mesh.starPlate(facing, position, core * 1.10, core * 0.38,
                        4, age * 1.4, 1.30F, 0.38F);
            }
            default -> {
                mesh.shard(position, visual.direction, facing, core * 3.5, core * 0.50,
                        1.18F, 0.48F);
                mesh.starPlate(facing, position, core * 1.10, core * 0.38,
                        5 + circle / 3, age * 2.0, 1.08F, 0.34F);
            }
        }

        if (age > 0.78) {
            double burst = clamp((age - 0.78) / 0.22, 0.0, 1.0);
            Vec3 end = visual.direction.scale(travel);
            mesh.orb(end, core * (0.72 + burst * 1.55), 28,
                    1.20F, (float) (0.34 * (1.0 - burst)));
            mesh.brokenBand(facing, end, core * (0.82 + burst * 1.85),
                    core * (1.02 + burst * 2.05), 44, 5, 1.18F,
                    (float) (0.42 * (1.0 - burst)));
        }
    }'''
tracker = replace_between(tracker,
                          "    private static void buildProjectile(ArcaneWorldMesh.Builder mesh, Visual visual,",
                          "    private static void buildBeam(ArcaneWorldMesh.Builder mesh, Visual visual,",
                          new_projectile, "buildProjectile")

family_methods = '''    private static void buildMissileSwarm(ArcaneWorldMesh.Builder mesh, Visual visual,
                                          ArcaneWorldMesh.Basis facing, double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        int count = Math.min(7, 3 + circle / 2);
        double travel = Math.min(72.0, Math.max(4.0, visual.range));
        double eased = 1.0 - Math.pow(1.0 - age, 1.45);
        double body = (0.12 + circle * 0.028) * powerFactor;
        for (int i = 0; i < count && !mesh.full(); i++) {
            double phase = Math.PI * 2.0 * i / count + age * (2.8 + i * 0.08);
            double spread = body * (1.3 + (i % 2) * 0.38) * Math.sin(Math.PI * age);
            Vec3 position = visual.direction.scale(travel * eased - i * body * 0.55)
                    .add(facing.point(phase, spread));
            mesh.shard(position, visual.direction, facing, body * 3.4, body * 0.32,
                    1.22F, 0.54F);
            mesh.orb(position, body * 0.52, 16, 1.32F, 0.42F);
        }
        if (age > 0.80) {
            double burst = clamp((age - 0.80) / 0.20, 0.0, 1.0);
            Vec3 end = visual.direction.scale(travel);
            mesh.starPlate(facing, end, body * (2.2 + burst * 3.8),
                    body * (0.8 + burst), 7, age * 2.0, 1.28F,
                    (float) (0.44 * (1.0 - burst)));
        }
    }

    private static void buildElementalOrb(ArcaneWorldMesh.Builder mesh, Visual visual,
                                          ArcaneWorldMesh.Basis facing, double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        double travel = Math.min(72.0, Math.max(4.0, visual.range));
        double eased = 1.0 - Math.pow(1.0 - age, 1.28);
        Vec3 position = visual.direction.scale(travel * eased);
        double radius = (0.26 + circle * 0.075) * powerFactor;
        double pulse = 0.92 + Math.sin(age * Math.PI * 8.0) * 0.08;
        mesh.orb(position, radius * pulse, 30 + circle * 2, 1.18F, 0.52F);
        mesh.brokenBand(facing, position, radius * 1.08, radius * 1.28,
                46, 5, 1.26F, 0.42F);
        ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                facing.up().add(visual.direction.scale(0.58)), facing.right());
        mesh.brokenBand(tilt, position, radius * 1.30, radius * 1.47,
                48, 6, 0.88F, 0.30F);
        for (int i = 1; i <= 3; i++) {
            Vec3 echo = position.subtract(visual.direction.scale(radius * (1.8 + i * 1.45)));
            mesh.orb(echo, radius * (0.42 - i * 0.07), 14, 0.72F, 0.14F);
        }
        if (age > 0.72) {
            double burst = clamp((age - 0.72) / 0.28, 0.0, 1.0);
            Vec3 end = visual.direction.scale(travel);
            mesh.orb(end, radius * (1.0 + burst * 2.8), 34, 1.20F,
                    (float) (0.48 * (1.0 - burst)));
            mesh.band(facing, end, radius * (1.2 + burst * 2.4),
                    radius * (1.5 + burst * 2.9), 58, 1.24F,
                    (float) (0.40 * (1.0 - burst)));
        }
    }

    private static void buildLance(ArcaneWorldMesh.Builder mesh, Visual visual,
                                   ArcaneWorldMesh.Basis facing, double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        double travel = Math.min(72.0, Math.max(4.0, visual.range));
        double eased = 1.0 - Math.pow(1.0 - age, 1.45);
        Vec3 position = visual.direction.scale(travel * eased);
        double width = (0.10 + circle * 0.026) * powerFactor;
        double length = width * (6.0 + circle * 0.45);
        mesh.shard(position, visual.direction, facing, length, width, 1.24F, 0.62F);
        for (int i = 0; i < 3; i++) {
            Vec3 fin = position.subtract(visual.direction.scale(length * 0.18))
                    .add(facing.point(age * 5.0 + i * Math.PI * 2.0 / 3.0, width * 1.7));
            mesh.shard(fin, visual.direction, facing, length * 0.34, width * 0.24,
                    0.92F, 0.34F);
        }
        mesh.brokenBand(facing, position, width * 1.45, width * 1.86,
                32, 5, 1.18F, 0.32F);
        if (age > 0.82) {
            double burst = clamp((age - 0.82) / 0.18, 0.0, 1.0);
            Vec3 end = visual.direction.scale(travel);
            mesh.starPlate(facing, end, width * (3.0 + burst * 4.0),
                    width * (1.0 + burst), 5 + circle / 2, age * 2.2,
                    1.24F, (float) (0.46 * (1.0 - burst)));
        }
    }

    private static void buildMeteor(ArcaneWorldMesh.Builder mesh, Visual visual,
                                    double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        int count = "meteor_swarm".equals(visual.spell.id()) ? 4
                : "fire_storm".equals(visual.spell.id()) ? Math.min(6, 2 + circle / 2) : 1;
        double radius = (0.50 + circle * 0.15) * powerFactor;
        double fall = clamp(age / 0.72, 0.0, 1.0);
        for (int i = 0; i < count && !mesh.full(); i++) {
            double angle = Math.PI * 2.0 * i / count + 0.45;
            double spread = count == 1 ? 0.0 : radius * (2.4 + (i % 2) * 0.7);
            Vec3 target = ground.point(angle, spread);
            Vec3 meteor = target.add(ground.point(angle + 1.1, radius * (1.5 - fall)))
                    .add(0.0, (11.0 + circle * 1.2) * (1.0 - fall), 0.0);
            Vec3 descent = target.subtract(meteor).normalize();
            ArcaneWorldMesh.Basis bodyBasis = ArcaneWorldMesh.Basis.facing(descent);
            mesh.orb(meteor, radius * (0.66 + i * 0.05), 28, 1.24F, 0.54F);
            mesh.shard(meteor.subtract(descent.scale(radius * 0.65)), descent, bodyBasis,
                    radius * 3.6, radius * 0.58, 1.12F, 0.42F);
        }
        if (age >= 0.62) {
            double impact = clamp((age - 0.62) / 0.38, 0.0, 1.0);
            double effect = Math.min(48.0,
                    SpellMetrics.effectRadius(visual.spell.id(), visual.range, circle));
            double ring = Math.max(radius, effect * impact);
            mesh.disc(ground, Vec3.ZERO, ring, 64, 0.82F,
                    (float) (0.15 * (1.0 - impact)));
            mesh.band(ground, Vec3.ZERO, ring * 0.82, ring, 72, 1.30F,
                    (float) (0.52 * (1.0 - impact)));
            mesh.orb(new Vec3(0.0, radius * 0.32, 0.0),
                    radius * (0.8 + impact * 3.2), 34, 1.22F,
                    (float) (0.44 * (1.0 - impact)));
        }
    }

    private static void buildStorm(ArcaneWorldMesh.Builder mesh, Visual visual,
                                   double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double effect = Math.min(48.0,
                SpellMetrics.effectRadius(visual.spell.id(), visual.range, circle));
        double radius = Math.max(1.2, effect * (0.35 + 0.65 * clamp(age / 0.24, 0.0, 1.0)));
        double fade = clamp((1.0 - age) / 0.16, 0.0, 1.0);
        mesh.disc(ground, Vec3.ZERO, radius, 64, 0.58F, (float) (0.08 * fade));
        for (int layer = 0; layer < 4 + circle / 2 && !mesh.full(); layer++) {
            double y = 0.18 + layer * (0.48 + circle * 0.035);
            double local = radius * (0.92 - layer * 0.055);
            mesh.brokenBand(ground, new Vec3(0.0, y, 0.0), local * 0.84, local,
                    54, 5 + layer % 3, 1.06F,
                    (float) ((0.19 + layer * 0.018) * fade));
        }
        Vec3 vertical = new Vec3(0.0, 1.0, 0.0);
        mesh.helix(Vec3.ZERO, vertical, ground, 2.8 + circle * 0.38,
                radius * 0.82, 2 + circle / 3, 52 + circle * 4,
                0.76F, true);
        for (int i = 0; i < Math.min(8, 3 + circle / 2); i++) {
            double a = age * 5.0 + Math.PI * 2.0 * i / Math.min(8, 3 + circle / 2);
            Vec3 mote = ground.point(a, radius * (0.35 + (i % 3) * 0.16))
                    .add(0.0, 0.5 + (i % 4) * 0.62, 0.0);
            mesh.orb(mote, 0.12 + circle * 0.018, 12, 1.16F, (float) (0.30 * fade));
        }
    }

    private static void buildPortal(ArcaneWorldMesh.Builder mesh, Visual visual,
                                    double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        ArcaneWorldMesh.Basis vertical = ArcaneWorldMesh.Basis.facing(new Vec3(0.0, 0.0, 1.0));
        double size = (0.86 + circle * 0.24) * powerFactor;
        if ("gate".equals(visual.spell.id())) size *= 2.0;
        double open = Math.sin(Math.min(1.0, age / 0.30) * Math.PI / 2.0);
        double fade = clamp((1.0 - age) / 0.18, 0.0, 1.0);
        Vec3 center = new Vec3(0.0, size * 0.95, 0.0);
        mesh.disc(vertical, center, size * open, 64, 0.48F,
                (float) (0.20 * fade));
        mesh.band(vertical, center, size * 0.78 * open, size * open,
                72, 1.26F, (float) (0.56 * fade));
        mesh.brokenBand(vertical, center, size * 1.08 * open, size * 1.20 * open,
                72, 6, 0.96F, (float) (0.34 * fade));
        ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                new Vec3(0.58, 0.0, 1.0), new Vec3(0.0, 1.0, 0.0));
        mesh.brokenBand(tilt, center, size * 0.88 * open, size * 0.98 * open,
                58, 5, 1.12F, (float) (0.26 * fade));
        mesh.starPlate(vertical, center, size * 0.42 * open, size * 0.16 * open,
                6 + circle / 2, age * 1.8, 1.18F, (float) (0.32 * fade));
    }

    private static void buildPrison(ArcaneWorldMesh.Builder mesh, Visual visual,
                                    double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = (0.90 + circle * 0.18) * powerFactor;
        double height = 1.8 + circle * 0.24;
        double appear = clamp(age / 0.22, 0.0, 1.0);
        double fade = clamp((1.0 - age) / 0.18, 0.0, 1.0);
        int levels = 4 + circle / 2;
        for (int level = 0; level < levels && !mesh.full(); level++) {
            double y = height * level / Math.max(1.0, levels - 1.0);
            double local = radius * (0.92 + 0.08 * Math.sin(Math.PI * level / levels));
            mesh.band(ground, new Vec3(0.0, y, 0.0), local * 0.88 * appear,
                    local * appear, 52, 1.14F, (float) (0.36 * fade));
        }
        int sides = 6 + circle / 2;
        for (int side = 0; side < sides && !mesh.full(); side++) {
            double a = Math.PI * 2.0 * side / sides;
            Vec3 lower = ground.point(a, radius * appear);
            Vec3 tangent = ground.point(a + Math.PI / 2.0, radius * 0.11);
            Vec3 upper = lower.add(0.0, height, 0.0);
            mesh.face(lower.subtract(tangent), lower.add(tangent),
                    upper.add(tangent), upper.subtract(tangent),
                    side % 2 == 0 ? 1.16F : 0.88F, (float) (0.20 * fade));
        }
        mesh.polygonPlate(ground, new Vec3(0.0, height, 0.0), radius * appear,
                sides, age * 0.8, 0.82F, (float) (0.18 * fade));
    }

    private static void buildWall(ArcaneWorldMesh.Builder mesh, Visual visual,
                                  double age, double powerFactor) {
        int circle = clampCircle(visual.spell.circle());
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(visual.direction);
        Vec3 right = facing.right();
        double width = Math.min(40.0,
                Math.max(4.0, SpellMetrics.effectRadius(visual.spell.id(), visual.range, circle) * 2.0));
        double height = (1.7 + circle * 0.34) * powerFactor;
        double appear = clamp(age / 0.24, 0.0, 1.0);
        double fade = clamp((1.0 - age) / 0.18, 0.0, 1.0);
        int panels = Math.min(18, 6 + circle * 2);
        for (int i = 0; i < panels && !mesh.full(); i++) {
            double a = (i / (double) panels - 0.5) * width;
            double b = ((i + 1) / (double) panels - 0.5) * width;
            Vec3 p0 = right.scale(a);
            Vec3 p1 = right.scale(b);
            double crestA = height * appear * (0.88 + 0.12 * Math.sin(i * 1.7));
            double crestB = height * appear * (0.88 + 0.12 * Math.sin((i + 1) * 1.7));
            mesh.face(p0, p1, p1.add(0.0, crestB, 0.0), p0.add(0.0, crestA, 0.0),
                    i % 2 == 0 ? 1.18F : 0.84F, (float) (0.30 * fade));
            if (i % 2 == 0) {
                Vec3 center = right.scale((a + b) * 0.5).add(0.0, (crestA + crestB) * 0.28, 0.0);
                mesh.orb(center, 0.12 + circle * 0.018, 12, 1.24F,
                        (float) (0.30 * fade));
            }
        }
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        mesh.brokenBand(ground, Vec3.ZERO, width * 0.44, width * 0.50,
                64, 7, 1.10F, (float) (0.24 * fade));
    }

'''
beam_marker = "    private static void buildBeam(ArcaneWorldMesh.Builder mesh, Visual visual,"
if "    private static void buildMeteor(" not in tracker:
    index = tracker.find(beam_marker)
    if index < 0:
        raise SystemExit("buildBeam insertion marker missing")
    tracker = tracker[:index] + family_methods + tracker[index:]

write(tracker_path, tracker)


# Static audit intentionally checks design contracts rather than screenshots.
audit = ROOT / "tools/test_v0121_alpha14_visual_architecture.py"
audit.write_text('''from pathlib import Path
import math

root = Path(__file__).resolve().parents[1]
magic = (root / "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java").read_text(encoding="utf-8")
casting = (root / "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java").read_text(encoding="utf-8")
tracker = (root / "src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java").read_text(encoding="utf-8")
props = (root / "gradle.properties").read_text(encoding="utf-8")

assert "mod_version=0.12.1-alpha.14" in props
for marker in (
    "double equipmentCostMultiplier = Math.max(0.10,",
    "double progressionCostMultiplier = circleMana * masteryMana * facultyMana;",
    "double equipmentCooldownMultiplier = Math.max(0.10,",
    "double progressionCooldownMultiplier = circleCooldown * masteryCooldown * facultyCooldown;",
    "rawCooldown < 2.0",
):
    assert marker in magic, marker
for obsolete in (
    "Math.max(0.10, circleMana * masteryMana",
    "Math.max(0.10, circleCooldown * masteryCooldown",
    "double circleMana = Math.max(0.10",
    "double circleCooldown = Math.max(0.10",
):
    assert obsolete not in magic, obsolete
assert "return raw < 2.0 ? 0" in casting

for marker in (
    "METEOR_FORMS", "PORTAL_FORMS", "PRISON_FORMS", "WALL_FORMS", "STORM_FORMS",
    "buildMeteor", "buildPortal", "buildPrison", "buildWall", "buildStorm",
    "buildMissileSwarm", "buildElementalOrb", "buildLance",
    "for (int layer = 1; layer <= circle", "Exactly one complete concentric band per spell circle",
):
    assert marker in tracker, marker

# Equipment alone bottoms at ten percent. Progression remains multiplicative afterwards.
equipment = max(0.10, 0.22 * 0.30)
progression = math.pow(0.62, 7) * 0.70 * 0.90
assert equipment == 0.10
assert equipment * progression < 0.10
assert 40 * equipment * progression < 2.0
assert 10 * math.pow(0.78, 7) * 0.72 < 2.0

# Generic continuous beams remain restricted to the explicit beam family.
assert 'else if (TRUE_BEAMS.contains(id)) buildBeam' in tracker
assert 'else if ("magic_missile".equals(id)) buildMissileSwarm' in tracker
print("Arcane Circle alpha.14 visual architecture audit: PASS")
''', encoding="utf-8")

print("Arcane Circle alpha.14 source migration: PASS")

package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.world.phys.Vec3;

/**
 * Explicit authored presentation registry.
 *
 * Every spell id is named here on purpose.  No school/form/circle style generator is allowed to
 * decide what a spell looks like.  The circle files are hand-authored compositions made only from
 * ArcaneWorldMesh drawing primitives.  Adding a spell without adding a case is a source-audit
 * failure, not an invitation to fall back to a generic magic circle.
 */
final class ManualSpellVisuals {
    static final int CAST_SNAP=1, CAST_AIM=2, CAST_HEAVY=3, CAST_GROUND=4,
            CAST_WARD=5, CAST_PORTAL=6, CAST_RITUAL=7;
    private static final int CHARGE_BUDGET=2500;
    private static final int RELEASE_BUDGET=3600;

    private ManualSpellVisuals() {}

    record Context(Vec3 direction, Vec3 target, double range, double power,
                   double progress, double age, double elapsed, double duration,
                   long seed, boolean release) {
        double reveal(){return smooth(clamp(release ? Math.min(1.0, elapsed/.32) : progress,0,1));}
        double pulse(double speed,double amount){return 1.0+Math.sin(elapsed*speed+(seed&255)*.013)*amount;}
    }

    static ArcaneWorldMesh charge(SpellDefinition spell, Vec3 direction, Vec3 target, double range,
                                  double power, double progress, long startedAtNanos, long seed) {
        double elapsed=Math.max(0.0,(System.nanoTime()-startedAtNanos)/1_000_000_000.0);
        Context c=new Context(safe(direction),target,range,power,progress,0,elapsed,0,seed,false);
        ArcaneWorldMesh.Builder m=ArcaneWorldMesh.fineBuilder(CHARGE_BUDGET);
        draw(spell.id(),m,c);
        return m.build();
    }

    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction, Vec3 target, double range,
                                   double power, double age, double elapsed, double duration, long seed) {
        Context c=new Context(safe(direction),target,range,power,1,age,elapsed,duration,seed,true);
        ArcaneWorldMesh.Builder m=ArcaneWorldMesh.builder(RELEASE_BUDGET);
        draw(spell.id(),m,c);
        return m.build();
    }

    private static void draw(String id, ArcaneWorldMesh.Builder m, Context c) {
        switch (id) {
            case "magic_missile", "fire_bolt", "ray_of_frost", "shield", "feather_fall",
                    "light", "grease", "sleep", "thunderwave", "mage_armor" -> ManualCircle1Visuals.draw(id,m,c);
            case "scorching_ray", "misty_step", "web", "mirror_image", "invisibility",
                    "gust_of_wind", "hold_person", "shatter", "blur", "levitate" -> ManualCircle2Visuals.draw(id,m,c);
            case "fireball", "lightning_bolt", "fly", "haste", "dispel_magic", "vampiric_touch",
                    "slow", "protection_from_energy", "sleet_storm", "blink" -> ManualCircle3Visuals.draw(id,m,c);
            case "wall_of_fire", "ice_storm", "greater_invisibility", "resilient_sphere", "dimension_door",
                    "stoneskin", "confusion", "blight", "freedom_of_movement", "phantasmal_killer" -> ManualCircle4Visuals.draw(id,m,c);
            case "cone_of_cold", "wall_of_force", "cloudkill", "telekinesis", "flame_strike",
                    "hold_monster", "mass_cure_wounds", "passwall", "dominate_person", "insect_plague" -> ManualCircle5Visuals.draw(id,m,c);
            case "disintegrate", "globe_of_invulnerability", "mass_suggestion", "move_earth", "sunbeam",
                    "true_seeing", "freezing_sphere", "eyebite", "flesh_to_stone", "circle_of_death" -> ManualCircle6Visuals.draw(id,m,c);
            case "delayed_blast_fireball", "etherealness", "finger_of_death", "fire_storm", "forcecage",
                    "plane_shift", "prismatic_spray", "reverse_gravity", "simulacrum", "teleport" -> ManualCircle7Visuals.draw(id,m,c);
            case "antimagic_field", "clone", "control_weather", "demiplane", "dominate_monster",
                    "earthquake", "feeblemind", "incendiary_cloud", "maze", "sunburst" -> ManualCircle8Visuals.draw(id,m,c);
            case "meteor_swarm", "power_word_kill", "prismatic_wall", "shapechange", "time_stop",
                    "true_polymorph", "weird", "wish", "gate", "foresight" -> ManualCircle9Visuals.draw(id,m,c);
            case "burning_hands", "ice_knife", "chromatic_orb", "wind_wall", "counterspell", "fire_shield",
                    "wall_of_ice", "chain_lightning", "arcane_hand", "teleportation_circle", "steam_burst",
                    "frost_step", "thunder_cage", "solar_guard", "void_lance", "winter_domain",
                    "astral_prison", "phoenix_requiem", "world_sunder" -> ManualFusionVisuals.draw(id,m,c);
            default -> throw new IllegalStateException("Un-authored spell visual: "+id);
        }
    }

    static int castingFamily(String id) {
        return switch (id) {
            case "magic_missile" -> CAST_AIM; case "fire_bolt" -> CAST_AIM; case "ray_of_frost" -> CAST_AIM;
            case "shield" -> CAST_WARD; case "feather_fall" -> CAST_WARD; case "light" -> CAST_SNAP;
            case "grease" -> CAST_GROUND; case "sleep" -> CAST_GROUND; case "thunderwave" -> CAST_GROUND; case "mage_armor" -> CAST_WARD;
            case "scorching_ray" -> CAST_AIM; case "misty_step" -> CAST_PORTAL; case "web" -> CAST_GROUND;
            case "mirror_image" -> CAST_WARD; case "invisibility" -> CAST_WARD; case "gust_of_wind" -> CAST_GROUND;
            case "hold_person" -> CAST_WARD; case "shatter" -> CAST_HEAVY; case "blur" -> CAST_WARD; case "levitate" -> CAST_WARD;
            case "fireball" -> CAST_HEAVY; case "lightning_bolt" -> CAST_AIM; case "fly" -> CAST_WARD; case "haste" -> CAST_WARD;
            case "dispel_magic" -> CAST_AIM; case "vampiric_touch" -> CAST_AIM; case "slow" -> CAST_GROUND;
            case "protection_from_energy" -> CAST_WARD; case "sleet_storm" -> CAST_GROUND; case "blink" -> CAST_PORTAL;
            case "wall_of_fire" -> CAST_GROUND; case "ice_storm" -> CAST_RITUAL; case "greater_invisibility" -> CAST_WARD;
            case "resilient_sphere" -> CAST_WARD; case "dimension_door" -> CAST_PORTAL; case "stoneskin" -> CAST_WARD;
            case "confusion" -> CAST_GROUND; case "blight" -> CAST_AIM; case "freedom_of_movement" -> CAST_WARD; case "phantasmal_killer" -> CAST_RITUAL;
            case "cone_of_cold" -> CAST_GROUND; case "wall_of_force" -> CAST_GROUND; case "cloudkill" -> CAST_RITUAL;
            case "telekinesis" -> CAST_AIM; case "flame_strike" -> CAST_RITUAL; case "hold_monster" -> CAST_WARD;
            case "mass_cure_wounds" -> CAST_RITUAL; case "passwall" -> CAST_PORTAL; case "dominate_person" -> CAST_RITUAL; case "insect_plague" -> CAST_RITUAL;
            case "disintegrate" -> CAST_AIM; case "globe_of_invulnerability" -> CAST_RITUAL; case "mass_suggestion" -> CAST_RITUAL;
            case "move_earth" -> CAST_GROUND; case "sunbeam" -> CAST_AIM; case "true_seeing" -> CAST_RITUAL;
            case "freezing_sphere" -> CAST_HEAVY; case "eyebite" -> CAST_RITUAL; case "flesh_to_stone" -> CAST_RITUAL; case "circle_of_death" -> CAST_RITUAL;
            case "delayed_blast_fireball" -> CAST_RITUAL; case "etherealness" -> CAST_RITUAL; case "finger_of_death" -> CAST_RITUAL;
            case "fire_storm" -> CAST_RITUAL; case "forcecage" -> CAST_RITUAL; case "plane_shift" -> CAST_PORTAL;
            case "prismatic_spray" -> CAST_RITUAL; case "reverse_gravity" -> CAST_RITUAL; case "simulacrum" -> CAST_RITUAL; case "teleport" -> CAST_PORTAL;
            case "antimagic_field" -> CAST_RITUAL; case "clone" -> CAST_RITUAL; case "control_weather" -> CAST_RITUAL;
            case "demiplane" -> CAST_PORTAL; case "dominate_monster" -> CAST_RITUAL; case "earthquake" -> CAST_RITUAL;
            case "feeblemind" -> CAST_RITUAL; case "incendiary_cloud" -> CAST_RITUAL; case "maze" -> CAST_RITUAL; case "sunburst" -> CAST_RITUAL;
            case "meteor_swarm" -> CAST_RITUAL; case "power_word_kill" -> CAST_RITUAL; case "prismatic_wall" -> CAST_RITUAL;
            case "shapechange" -> CAST_RITUAL; case "time_stop" -> CAST_RITUAL; case "true_polymorph" -> CAST_RITUAL;
            case "weird" -> CAST_RITUAL; case "wish" -> CAST_RITUAL; case "gate" -> CAST_PORTAL; case "foresight" -> CAST_RITUAL;
            case "burning_hands" -> CAST_GROUND; case "ice_knife" -> CAST_AIM; case "chromatic_orb" -> CAST_HEAVY;
            case "wind_wall" -> CAST_GROUND; case "counterspell" -> CAST_AIM; case "fire_shield" -> CAST_WARD;
            case "wall_of_ice" -> CAST_GROUND; case "chain_lightning" -> CAST_AIM; case "arcane_hand" -> CAST_HEAVY;
            case "teleportation_circle" -> CAST_PORTAL; case "steam_burst" -> CAST_GROUND; case "frost_step" -> CAST_PORTAL;
            case "thunder_cage" -> CAST_RITUAL; case "solar_guard" -> CAST_RITUAL; case "void_lance" -> CAST_RITUAL;
            case "winter_domain" -> CAST_RITUAL; case "astral_prison" -> CAST_RITUAL; case "phoenix_requiem" -> CAST_RITUAL; case "world_sunder" -> CAST_RITUAL;
            default -> throw new IllegalStateException("Un-authored casting pose: "+id);
        };
    }

    static int color(String id) {
    return switch (id) {
        case "fire_bolt","scorching_ray","fireball","wall_of_fire","flame_strike","delayed_blast_fireball","fire_storm","incendiary_cloud","meteor_swarm","burning_hands","fire_shield","steam_burst","phoenix_requiem" -> 0xFFFF321A;
        case "ray_of_frost","sleet_storm","ice_storm","cone_of_cold","freezing_sphere","simulacrum","ice_knife","wall_of_ice","frost_step","winter_domain" -> 0xFF31D9FF;
        case "feather_fall","thunderwave","gust_of_wind","levitate","fly","control_weather","wind_wall" -> 0xFF3DFFC4;
        case "shield","mage_armor","web","hold_person","protection_from_energy","resilient_sphere","stoneskin","wall_of_force","hold_monster","globe_of_invulnerability","forcecage","antimagic_field","prismatic_wall","thunder_cage","astral_prison" -> 0xFFAA5CFF;
        case "vampiric_touch","blight","freedom_of_movement","cloudkill","mass_cure_wounds","insect_plague","clone","shapechange","true_polymorph" -> 0xFF4AFF72;
        case "misty_step","blink","dimension_door","passwall","etherealness","plane_shift","reverse_gravity","teleport","demiplane","maze","gate","teleportation_circle","void_lance" -> 0xFFE052FF;
        case "magic_missile","light","grease","sleep","mirror_image","invisibility","shatter","blur","haste","dispel_magic","slow","greater_invisibility","confusion","telekinesis","dominate_person","mass_suggestion","dominate_monster","counterspell","arcane_hand" -> 0xFF5B78FF;
        case "lightning_bolt","chain_lightning" -> 0xFF78B8FF;
        case "prismatic_spray","chromatic_orb" -> 0xFFFFFFFF;
        case "disintegrate" -> 0xFF6BFF22;
        case "sunbeam","sunburst","true_seeing","foresight","solar_guard" -> 0xFFFFE55A;
        case "circle_of_death","finger_of_death","power_word_kill","eyebite" -> 0xFFFF224E;
        case "phantasmal_killer","weird","feeblemind" -> 0xFFFF36D7;
        case "flesh_to_stone" -> 0xFFD4DAE8;
        case "time_stop" -> 0xFF64EFFF;
        case "wish" -> 0xFFF4A8FF;
        case "move_earth","earthquake","world_sunder" -> 0xFFFFA43B;
        default -> throw new IllegalStateException("Un-authored spell color: "+id);
    };
}

    static int prismaticColor(int layer){int[] c={0xFFFF2348,0xFFFF8A24,0xFFFFE63B,0xFF39EE77,0xFF35A9FF,0xFF7657FF,0xFFE055FF};return c[Math.floorMod(layer,c.length)];}

    static ArcaneWorldMesh prismaticWallLayer(Vec3 direction,Vec3 target,double range,double age,double elapsed,int layer){
        ArcaneWorldMesh.Builder m=ArcaneWorldMesh.builder(340);
        ArcaneWorldMesh.Basis face=ArcaneWorldMesh.Basis.facing(flat(direction));
        double width=Math.max(15.0,range*.54), panel=width/7.0;
        double x0=-width*.5+layer*panel,x1=x0+panel;
        double rise=smooth(clamp(elapsed/.30,0,1)),height=6.6*rise;
        double fade=age<.90?1.0:clamp((1-age)/.10,0,1);
        Vec3 a=target.add(face.right().scale(x0)),b=target.add(face.right().scale(x1)),up=new Vec3(0,height,0);
        m.face(a,b,b.add(up),a.add(up),1.12F,(float)(.34*fade));
        m.line(a,a.add(up),1.04F);m.line(b,b.add(up),1.04F);m.line(a.add(up),b.add(up),.82F);
        Vec3 center=a.add(b).scale(.5).add(0,height*.48,0),low=center.add(0,-height*.30,0),high=center.add(0,height*.32,0);
        m.diamond(face,center,panel*.22,elapsed*(layer%2==0?.22:-.18),1.16F,(float)(.24*fade));m.runeGlyph(face,center,panel*.12,0x901+layer*131,-elapsed*.07,.48F);
        m.polygon(face,low,panel*.18,3+(layer%3),layer*.31+elapsed*.04,.34F);m.polygon(face,high,panel*.16,4+(layer%2),-layer*.27-elapsed*.035,.32F);
        m.runeGlyph(face,low,panel*.075,0xA01+layer*173,layer*.4,.30F);m.runeGlyph(face,high,panel*.070,0xB01+layer*179,-layer*.3,.28F);
        m.line(a.add(0,height*.16,0),b.add(0,height*.84,0),.22F);m.line(b.add(0,height*.16,0),a.add(0,height*.84,0),.22F);
        return m.build();
    }

    static ArcaneWorldMesh prismaticSprayLayer(Vec3 direction,double range,double age,int layer){
        ArcaneWorldMesh.Builder m=ArcaneWorldMesh.builder(80);
        ArcaneWorldMesh.Basis face=ArcaneWorldMesh.Basis.facing(direction);
        double offset=(layer-3)*.135;
        Vec3 dir=direction.add(face.right().scale(offset)).normalize();
        m.beamPrism(Vec3.ZERO,dir,ArcaneWorldMesh.Basis.facing(dir),Math.max(4,range*.88),.040+layer*.004,1.16F,(float)(.34*(1-age*.55)));
        return m.build();
    }

    static ArcaneWorldMesh.Basis ground(){return ArcaneWorldMesh.Basis.ground();}
    static ArcaneWorldMesh.Basis face(Context c){return ArcaneWorldMesh.Basis.facing(c.direction);}
    static ArcaneWorldMesh.Basis sideX(){return ArcaneWorldMesh.Basis.facing(new Vec3(1,0,0));}
    static ArcaneWorldMesh.Basis sideZ(){return ArcaneWorldMesh.Basis.facing(new Vec3(0,0,1));}
    static Vec3 flat(Vec3 v){Vec3 f=new Vec3(v.x,0,v.z);return f.lengthSqr()<1e-8?new Vec3(0,0,1):f.normalize();}
    static Vec3 safe(Vec3 v){return v==null||v.lengthSqr()<1e-8?new Vec3(0,0,1):v.normalize();}
    static double smooth(double t){return t*t*(3-2*t);}
    static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
}

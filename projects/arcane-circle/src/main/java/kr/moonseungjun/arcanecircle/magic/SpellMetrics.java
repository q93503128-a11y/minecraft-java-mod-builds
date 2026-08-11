package kr.moonseungjun.arcanecircle.magic;

/** Exact shared geometry metrics. Gameplay hitboxes and client boundary bands use this table. */
public final class SpellMetrics {
    private SpellMetrics() {}

    public static double effectRadius(String spellId,double range,int circle){
        return switch(spellId){
            case "grease"->Math.max(4.0,range/3.0);
            case "sleep"->Math.max(4.5,range*.375);
            case "web"->Math.max(5.0,range*(5.0/14.0));
            case "slow"->Math.max(5.0,range*(5.0/17.0));
            case "confusion"->Math.max(5.8,range*.30);
            case "shatter"->Math.max(3.5,range*.28);
            case "fireball"->Math.max(3.0,range*.32);
            case "sleet_storm"->Math.max(5.0,range*.34);
            case "ice_storm"->Math.max(5.8,range*.34);
            case "cloudkill"->Math.max(6.0,range*.34);
            case "insect_plague"->Math.max(6.0,range*.32);
            case "ice_knife","chromatic_orb"->Math.max(3.0,range*.22);
            case "frost_nova","phoenix_field","inferno_domain","absolute_zero","tempest_domain","aegis_citadel"->Math.max(3.0,range);
            case "tempest_aegis"->Math.max(2.5,range*.45);
            case "meteor_shard"->5.0;case "blizzard_field"->6.0;case "thunder_prison"->4.5;
            case "wall_of_fire","wall_of_ice","wind_wall","wall_of_force","prismatic_wall"->Math.max(5.0,range*.38);
            case "antimagic_field","incendiary_cloud","meteor_swarm","storm_of_vengeance"->Math.max(7.0,range*.36);
            default->Math.max(2.4,range*(.20+Math.min(9,circle)*.012));
        };
    }

    /** Full lateral wall width. It deliberately has no low legacy hard-cap, so range bonuses remain visible. */
    public static double wallWidth(String spellId,double range,int circle){
        double multiplier=switch(spellId){
            case "wall_of_force"->.82;case "wall_of_ice"->.76;case "wind_wall"->.84;default->.72;
        };
        return Math.max(9.0,range*multiplier);
    }

    public static double waveLength(double range){return Math.max(4.0,range);}
    public static double waveEndRadius(String spellId,double range,int circle){
        double spread=switch(spellId){case"burning_hands"->.34;case"steam_burst"->.42;case"cone_of_cold"->.46;default->.40;};
        return Math.max(1.4,range*spread+circle*.10);
    }
}

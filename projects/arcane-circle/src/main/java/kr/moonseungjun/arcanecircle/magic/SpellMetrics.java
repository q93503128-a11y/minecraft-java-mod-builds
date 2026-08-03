package kr.moonseungjun.arcanecircle.magic;

/** Exact shared geometry metrics. Gameplay hitboxes and client boundary bands use this table. */
public final class SpellMetrics {
    private SpellMetrics() {}

    public static double effectRadius(String spellId,double range,int circle){
        return switch(spellId){
            case "grease"->4.0;case "sleep"->4.5;case "web","slow"->5.0;
            case "shatter"->Math.max(3.5,range*.28);
            case "fireball"->Math.max(3.0,range*.32);
            case "sleet_storm"->Math.max(5.0,range*.34);case "cloudkill"->Math.max(6.0,range*.34);case "insect_plague"->Math.max(6.0,range*.32);
            case "ice_knife","chromatic_orb"->Math.max(3.0,range*.22);
            case "frost_nova","phoenix_field","inferno_domain","absolute_zero","tempest_domain","aegis_citadel"->Math.max(3.0,range);
            case "tempest_aegis"->Math.max(2.5,range*.45);
            case "meteor_shard"->5.0;case "blizzard_field"->6.0;case "thunder_prison"->4.5;
            case "wall_of_fire","wall_of_ice","wind_wall","prismatic_wall"->Math.max(5.0,range*.38);
            case "antimagic_field","incendiary_cloud","meteor_swarm","storm_of_vengeance"->Math.max(7.0,range*.36);
            default->Math.max(2.4,range*(.20+Math.min(9,circle)*.012));
        };
    }
    public static double waveLength(double range){return Math.max(4.0,range);}
    public static double waveEndRadius(String spellId,double range,int circle){double spread=switch(spellId){case"burning_hands"->.34;case"cone_of_cold"->.46;default->.40;};return Math.max(1.4,range*spread+circle*.10);}
}

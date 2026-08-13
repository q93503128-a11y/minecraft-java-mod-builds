package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Server-authoritative terrain rupture for spells whose fiction is explicitly destructive.
 * Weak materials fail farther from the impact; hard/blast-resistant materials require a much
 * stronger local impulse. Unbreakable blocks and block entities are never removed. Every call is
 * bounded so a cinematic spell cannot turn into an unbounded world-edit or item-entity storm.
 */
public final class DestructiveMagicService {
    private record Candidate(BlockPos pos, double overload) {}
    private record Profile(double radiusScale, double baseEnergy, int maxBlocks, boolean drops) {}

    private DestructiveMagicService() {}

    public static int impact(ServerPlayer player, String spellId, Vec3 center,
                             double requestedRadius, double power) {
        Profile profile=profile(spellId);
        if(profile==null||center==null)return 0;
        ServerLevel level=(ServerLevel)player.level();
        double radius=Math.max(.75,Math.min(10.5,requestedRadius*profile.radiusScale()));
        double energy=profile.baseEnergy()*(.84+.16*Math.sqrt(Math.max(.1,power)));
        int bound=(int)Math.ceil(radius);
        List<Candidate> candidates=new ArrayList<>();
        BlockPos origin=BlockPos.containing(center);
        double vertical=Math.max(1.25,radius*.62);
        for(int x=-bound;x<=bound;x++)for(int z=-bound;z<=bound;z++)for(int y=-(int)Math.ceil(vertical);y<=Math.ceil(vertical);y++){
            double nx=x/radius,nz=z/radius,ny=y/vertical;
            double normalized=Math.sqrt(nx*nx+nz*nz+ny*ny);
            if(normalized>1.0)continue;
            BlockPos pos=origin.offset(x,y,z);
            BlockState state=level.getBlockState(pos);
            if(state.isAir()||!state.getFluidState().isEmpty()||state.hasBlockEntity())continue;
            float hardness=state.getDestroySpeed(level,pos);
            if(hardness<0)continue;
            float blast=Math.max(0F,state.getBlock().getExplosionResistance());
            if(blast>=1000F)continue;
            double strength=1.0+Math.max(0,hardness)*2.6+Math.sqrt(blast)*1.45;
            double falloff=Math.pow(Math.max(0.0,1.0-normalized),.78);
            double local=energy*(.20+.80*falloff);
            if(local<strength)continue;
            candidates.add(new Candidate(pos,local/Math.max(.25,strength)));
        }
        candidates.sort(Comparator.comparingDouble(Candidate::overload).reversed());
        int changed=0;
        for(Candidate candidate:candidates){
            if(changed>=profile.maxBlocks())break;
            if(level.destroyBlock(candidate.pos(),profile.drops(),player))changed++;
        }
        return changed;
    }

    public static int ray(ServerPlayer player, String spellId, Vec3 start, Vec3 end, double power) {
        if(start==null||end==null)return 0;
        Vec3 delta=end.subtract(start); double length=delta.length();
        if(length<.05)return 0;
        Vec3 unit=delta.scale(1.0/length); int changed=0;
        int samples=Math.min(72,Math.max(1,(int)Math.ceil(length/.75)));
        for(int i=1;i<=samples;i++){
            Vec3 at=start.add(unit.scale(length*i/(double)samples));
            changed+=impact(player,spellId,at,1.15,power);
            if(changed>=72)break;
        }
        return changed;
    }

    private static Profile profile(String id){
        return switch(id){
            case "fireball" -> new Profile(.72,7.6,72,false);
            case "shatter" -> new Profile(.74,9.0,88,true);
            case "flame_strike" -> new Profile(.62,10.5,96,false);
            case "meteor_shard" -> new Profile(.88,12.5,112,false);
            case "disintegrate" -> new Profile(.78,24.0,18,false);
            case "delayed_blast_fireball" -> new Profile(.92,15.5,150,false);
            case "fire_storm" -> new Profile(.66,11.5,52,false);
            case "move_earth" -> new Profile(.54,11.0,170,true);
            case "earthquake" -> new Profile(.58,14.5,240,true);
            case "meteor_swarm" -> new Profile(.92,16.5,34,false);
            case "world_sunder" -> new Profile(.62,28.0,320,true);
            case "arcane_annihilation" -> new Profile(.70,22.0,48,false);
            default -> null;
        };
    }
}

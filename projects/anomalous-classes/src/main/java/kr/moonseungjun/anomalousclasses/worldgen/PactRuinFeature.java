package kr.moonseungjun.anomalousclasses.worldgen;

import com.mojang.serialization.Codec;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.block.PactAltarBlock;
import kr.moonseungjun.anomalousclasses.block.PactBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Functional, recognizable ruins for tier-two through tier-four patrons.
 * One shared 40-chunk cell: adding patrons never multiplies structure attempts.
 * The full-scale distinct altar architecture is a later visual/worldbuilding pass.
 */
public final class PactRuinFeature extends Feature<NoneFeatureConfiguration> {
    public PactRuinFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos origin=context.origin();
        int cx=origin.getX()>>4, cz=origin.getZ()>>4;
        int[] goal=PactAltarPlacement.targetChunk(Math.floorDiv(cx,40),Math.floorDiv(cz,40));
        if (cx!=goal[0]||cz!=goal[1]) return false;
        WorldGenLevel level=context.level();
        int x=(cx<<4)+8, z=(cz<<4)+8;
        int terrain=level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z);
        BlockPos reference=new BlockPos(x,terrain,z);
        boolean ocean=level.getBiome(reference).is(BiomeTags.IS_OCEAN);
        String patron;
        if(ocean) patron="nerea";
        else {
            patron=PactAltarPlacement.selectLandPatron(cx,cz);
        }
        int height=ocean?level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z):terrain;
        if(height<=level.getMinBuildHeight()+4||height>=level.getMaxBuildHeight()-10) return false;
        boolean topTier= switch(patron) {
            case "nahar","rakiel","ormas","viar" -> true;
            default -> false;
        };
        boolean highTier= switch(patron){
            case "sevenwing","merzad","taron","arken",
                 "nahar","rakiel","ormas","viar" -> true;
            default -> false;
        };
        int radius=topTier?7:highTier?6:4;
        int tolerance=ocean?5:topTier?5:highTier?4:3;
        for(int dx=-radius;dx<=radius;dx+=radius) for(int dz=-radius;dz<=radius;dz+=radius) {
            int neighbor=level.getHeight(ocean?Heightmap.Types.OCEAN_FLOOR_WG
                    : Heightmap.Types.WORLD_SURFACE_WG,x+dx,z+dz);
            if(Math.abs(neighbor-height)>tolerance) return false;
        }
        BlockPos floor=new BlockPos(x,height-1,z);
        if(!ocean && !level.getFluidState(floor.above()).isEmpty()) return false;
        int variant=PactAltarBlock.index(patron);
        if(variant<0) return false;
        Block base= switch(patron){
            case "karda"->Blocks.CUT_COPPER;
            case "nerea"->Blocks.PRISMARINE_BRICKS;
            case "shero"->Blocks.END_STONE_BRICKS;
            case "sevenwing"->Blocks.QUARTZ_BRICKS;
            case "merzad"->Blocks.NETHER_BRICKS;
            case "taron"->Blocks.DEEPSLATE_BRICKS;
            case "arken"->Blocks.STONE_BRICKS;
            case "nahar"->Blocks.POLISHED_BLACKSTONE_BRICKS;
            case "rakiel"->Blocks.QUARTZ_BRICKS;
            case "ormas"->Blocks.DEEPSLATE_BRICKS;
            case "viar"->Blocks.MOSSY_STONE_BRICKS;
            default->Blocks.POLISHED_BLACKSTONE_BRICKS;
        };
        Block accent= switch(patron){
            case "karda"->Blocks.OXIDIZED_CUT_COPPER;
            case "nerea"->Blocks.DARK_PRISMARINE;
            case "shero"->Blocks.CALCITE;
            case "sevenwing"->Blocks.CHISELED_QUARTZ_BLOCK;
            case "merzad"->Blocks.POLISHED_BLACKSTONE_BRICKS;
            case "taron"->Blocks.TUFF_BRICKS;
            case "arken"->Blocks.CHISELED_STONE_BRICKS;
            case "nahar"->Blocks.CRYING_OBSIDIAN;
            case "rakiel"->Blocks.CHISELED_QUARTZ_BLOCK;
            case "ormas"->Blocks.POLISHED_BASALT;
            case "viar"->Blocks.MOSS_BLOCK;
            default->Blocks.GILDED_BLACKSTONE;
        };
        for(int dx=-radius;dx<=radius;dx++) for(int dz=-radius;dz<=radius;dz++) {
            // Circular broken plinth and walkable interior; no broad terrain flattening.
            int squared=dx*dx+dz*dz;
            int squaredLimit=radius*radius;
            if(squared>squaredLimit || (squared>squaredLimit-6 && ((dx*31+dz*17)&3)==0)) continue;
            Block platform=squared>=9?accent:base;
            level.setBlock(floor.offset(dx,0,dz),platform.defaultBlockState(),2);
        }
        level.setBlock(floor.above(),PactBlocks.ALTAR_CORE.get().defaultBlockState()
                .setValue(PactAltarBlock.PATRON,variant),2);
        switch(patron){
            case "karda" -> {
                for(int heightOffset=0;heightOffset<6;heightOffset++){
                    level.setBlock(floor.offset(-4,heightOffset,0),
                            (heightOffset%2==0?base:accent).defaultBlockState(),2);
                    level.setBlock(floor.offset(4,heightOffset,0),
                            (heightOffset%2==0?accent:base).defaultBlockState(),2);
                }
                level.setBlock(floor.offset(-4,6,0),Blocks.LIGHTNING_ROD.defaultBlockState(),2);
                level.setBlock(floor.offset(4,6,0),Blocks.LIGHTNING_ROD.defaultBlockState(),2);
            }
            case "nerea" -> {
                for(int sx:new int[]{-3,3}) for(int sz:new int[]{-3,3})
                    for(int rise=0;rise<3;rise++)
                        level.setBlock(floor.offset(sx,rise,sz),accent.defaultBlockState(),2);
                level.setBlock(floor.offset(3,0,0),Blocks.MAGMA_BLOCK.defaultBlockState(),2);
                for(int rise=1;rise<4;rise++)
                    level.setBlock(floor.offset(3,rise,0),Blocks.WATER.defaultBlockState(),2);
            }
            case "shero" -> {
                for(int rise=0;rise<4;rise++)
                    level.setBlock(floor.offset(-3,rise,-2),accent.defaultBlockState(),2);
                for(int rise=0;rise<6;rise++)
                    level.setBlock(floor.offset(3,rise,2),accent.defaultBlockState(),2);
                level.setBlock(floor.offset(3,6,2),Blocks.AMETHYST_BLOCK.defaultBlockState(),2);
            }
            case "hastel" -> {
                for(int dx=-3;dx<=3;dx+=6)
                    for(int rise=0;rise<4;rise++)
                        level.setBlock(floor.offset(dx,rise,-2),base.defaultBlockState(),2);
                for(int dx=-3;dx<=3;dx++)
                    level.setBlock(floor.offset(dx,4,-2),accent.defaultBlockState(),2);
            }
            case "sevenwing" -> {
                // Seven open pillars around a circular court.
                for(int i=0;i<7;i++){
                    double angle=i*2*Math.PI/7;
                    int ox=(int)Math.round(Math.cos(angle)*5);
                    int oz=(int)Math.round(Math.sin(angle)*5);
                    for(int rise=0;rise<5;rise++)
                        level.setBlock(floor.offset(ox,rise,oz),
                                (rise==4?accent:base).defaultBlockState(),2);
                }
            }
            case "merzad" -> {
                // A split throne and a path to the bargaining core, without lava or fire grief.
                for(int dx=-4;dx<=4;dx+=8)
                    for(int rise=0;rise<5;rise++)
                        level.setBlock(floor.offset(dx,rise,-3),
                                (rise>=3?accent:base).defaultBlockState(),2);
                for(int dx=-2;dx<=2;dx++)
                    level.setBlock(floor.offset(dx,1,-4),accent.defaultBlockState(),2);
                level.setBlock(floor.offset(0,2,-4),Blocks.SHROOMLIGHT.defaultBlockState(),2);
            }
            case "taron" -> {
                // The stone king's gate uses heavy asymmetrical uprights.
                for(int dx=-4;dx<=4;dx+=8)
                    for(int rise=0;rise<6;rise++)
                        level.setBlock(floor.offset(dx,rise,-2),
                                (rise%3==0?accent:base).defaultBlockState(),2);
                for(int dx=-4;dx<=4;dx++)
                    level.setBlock(floor.offset(dx,6,-2),accent.defaultBlockState(),2);
            }
            case "arken" -> {
                // A broken reading aisle with an open archive lectern.
                for(int dz=-4;dz<=4;dz++)
                    if(dz!=-1 && dz!=1) {
                        level.setBlock(floor.offset(-4,1,dz),base.defaultBlockState(),2);
                        level.setBlock(floor.offset(4,1,dz),accent.defaultBlockState(),2);
                    }
                level.setBlock(floor.offset(0,1,-4),Blocks.LECTERN.defaultBlockState(),2);
            }
            case "nahar" -> {
                // Fragmented impact ring: a short, open path to the central core.
                for(int i=0;i<8;i++) {
                    double angle=i*Math.PI/4;
                    int dx=(int)Math.round(Math.cos(angle)*6);
                    int dz=(int)Math.round(Math.sin(angle)*6);
                    for(int rise=0;rise<(i%2==0?5:3);rise++)
                        level.setBlock(floor.offset(dx,rise,dz),
                                (rise==0?accent:base).defaultBlockState(),2);
                }
            }
            case "rakiel" -> {
                // Paired open wings around an unobstructed oath dais.
                for(int dx:new int[]{-5,5})for(int dz:new int[]{-4,4}) {
                    for(int rise=0;rise<6;rise++)
                        level.setBlock(floor.offset(dx,rise,dz),
                                (rise==5?accent:base).defaultBlockState(),2);
                }
                level.setBlock(floor.offset(-5,6,-4),Blocks.GLOWSTONE.defaultBlockState(),2);
                level.setBlock(floor.offset(5,6,-4),Blocks.GLOWSTONE.defaultBlockState(),2);
            }
            case "ormas" -> {
                // Four towering fingers, with a clear center and reachable loot.
                for(int dx:new int[]{-5,5})for(int dz:new int[]{-4,4}) {
                    int tall=dz==-4?7:4;
                    for(int rise=0;rise<tall;rise++)
                        level.setBlock(floor.offset(dx,rise,dz),
                                (rise%3==0?accent:base).defaultBlockState(),2);
                }
                for(int rise=1;rise<4;rise++)
                    level.setBlock(floor.offset(0,rise,-6),accent.defaultBlockState(),2);
            }
            case "viar" -> {
                // Open forest roots and canopy; the contract core stays walkable.
                for(int dx:new int[]{-5,5})for(int dz:new int[]{-5,5}) {
                    for(int rise=0;rise<5;rise++)
                        level.setBlock(floor.offset(dx,rise,dz),Blocks.OAK_LOG.defaultBlockState(),2);
                    level.setBlock(floor.offset(dx,5,dz),Blocks.OAK_LEAVES.defaultBlockState(),2);
                }
                for(int dx=-2;dx<=2;dx++)
                    level.setBlock(floor.offset(dx,1,-6),accent.defaultBlockState(),2);
            }
        }
        BlockPos chest=floor.offset(highTier?3:2,1,highTier?3:2);
        if(ocean) chest=floor.offset(-2,1,2);
        level.setBlock(chest,Blocks.CHEST.defaultBlockState(),2);
        if(level.getBlockEntity(chest) instanceof RandomizableContainerBlockEntity loot) {
            ResourceLocation location=ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID,
                    "chests/pact_"+patron);
            loot.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,location));
        }
        return true;
    }
}

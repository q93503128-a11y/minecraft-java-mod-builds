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
 * Functional 4-altar world placement with four different vanilla-block structures.
 * Shared 40-chunk cell: never place four independent full-frequency structure sets.
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
            long h=PactAltarPlacement.mix(cx*0x632be59bd9b4e019L ^ cz*0x85157af5L);
            patron=switch((int)Math.floorMod(h,3)){case 0->"karda";case 1->"shero";default->"hastel";};
        }
        int height=ocean?level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z):terrain;
        if(height<=level.getMinBuildHeight()+4||height>=level.getMaxBuildHeight()-10) return false;
        int tolerance=ocean?5:3;
        for(int dx=-4;dx<=4;dx+=4) for(int dz=-4;dz<=4;dz+=4) {
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
            default->Blocks.POLISHED_BLACKSTONE_BRICKS;
        };
        Block accent= switch(patron){
            case "karda"->Blocks.OXIDIZED_CUT_COPPER;
            case "nerea"->Blocks.DARK_PRISMARINE;
            case "shero"->Blocks.CALCITE;
            default->Blocks.GILDED_BLACKSTONE;
        };
        for(int dx=-4;dx<=4;dx++) for(int dz=-4;dz<=4;dz++) {
            // Circular broken plinth and a walkable interior. Leave outside terrain untouched.
            int squared=dx*dx+dz*dz;
            if(squared>20 || (squared>14 && ((dx*31+dz*17)&3)==0)) continue;
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
        }
        BlockPos chest=floor.offset(2,1,2);
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

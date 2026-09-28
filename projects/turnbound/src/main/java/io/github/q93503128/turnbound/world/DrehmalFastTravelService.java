package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.*;

final class DrehmalFastTravelService {
    private static final String FLAG_PREFIX="FAST_TRAVEL_DISCOVERED:";
    private static final double CURRENT_RADIUS_SQR=28.0D*28.0D;

    private DrehmalFastTravelService(){}

    static void recordDiscovery(ServerPlayer player){
        if(player==null||player.level().getServer()==null)return;
        ExternalWorldSavedData data=ExternalWorldSavedData.get(player.level().getServer());
        for(var node:DrehmalFastTravelCatalog.nodes()){
            double dx=player.getX()-(node.mapX()+0.5D),dz=player.getZ()-(node.mapZ()+0.5D);
            if(dx*dx+dz*dz<=node.unlockRadius()*node.unlockRadius()){
                data.markOnboardingFlag(player.getUUID(),flag(node.id()));
            }
        }
    }

    static List<FieldUiSnapshot.Travel> travels(ServerPlayer player){
        if(player==null||player.level().getServer()==null)return List.of();
        ExternalWorldSavedData data=ExternalWorldSavedData.get(player.level().getServer());
        List<FieldUiSnapshot.Travel> out=new ArrayList<>();
        for(var node:DrehmalFastTravelCatalog.nodes()){
            double dx=player.getX()-(node.mapX()+0.5D),dz=player.getZ()-(node.mapZ()+0.5D);
            boolean current=dx*dx+dz*dz<=CURRENT_RADIUS_SQR;
            out.add(new FieldUiSnapshot.Travel(node.id(),node.label(),
                    data.onboardingFlag(player.getUUID(),flag(node.id())),current));
        }
        return List.copyOf(out);
    }

    static boolean handle(ServerPlayer player,String destinationId){
        if(player==null||destinationId==null)return false;
        var node=DrehmalFastTravelCatalog.node(destinationId);
        if(node==null)return false;
        if(!ExternalWorldBootstrap.active(player))return true;
        if(BattleSessionManager.exists(player)){
            deny(player,"전투 중에는 빠른 이동을 사용할 수 없습니다.");
            return true;
        }
        if(DrehmalVisibleEncounterService.fastTravelBlocked(player)){
            deny(player,"적의 추격을 벗어난 뒤 빠른 이동을 사용할 수 있습니다.");
            return true;
        }
        var server=player.level().getServer();
        if(server==null)return true;
        ExternalWorldSavedData data=ExternalWorldSavedData.get(server);
        if(!data.onboardingFlag(player.getUUID(),flag(node.id()))){
            deny(player,"직접 발견한 이동 거점만 사용할 수 있습니다.");
            return true;
        }
        if(!(player.level() instanceof ServerLevel level))return true;
        BlockPos destination=findSafeArrival(level,node);
        if(destination==null){
            deny(player,"이동 지점의 안전한 착지 공간을 찾지 못했습니다.");
            return true;
        }

        boolean teleported=player.teleportTo(level,destination.getX()+0.5D,destination.getY(),destination.getZ()+0.5D,
                Set.of(),node.yaw(),0.0F,true);
        if(!teleported){
            deny(player,"빠른 이동을 완료하지 못했습니다.");
            return true;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.setOnGround(true);
        player.sendSystemMessage(Component.literal("빠른 이동 · "+node.label()).withStyle(ChatFormatting.AQUA));
        ExternalWorldBootstrap.refreshFieldContext(player);
        return true;
    }

    private static BlockPos findSafeArrival(ServerLevel level,DrehmalFastTravelCatalog.Node node){
        BlockPos best=null;
        long bestScore=Long.MAX_VALUE;
        int radius=node.arrivalRadius();
        for(int dz=-radius;dz<=radius;dz++)for(int dx=-radius;dx<=radius;dx++){
            int distanceSq=dx*dx+dz*dz;
            if(distanceSq>radius*radius)continue;
            int x=node.mapX()+dx,z=node.mapZ()+dz;
            for(int dy=-12;dy<=12;dy++){
                BlockPos feet=new BlockPos(x,node.preferredY()+dy,z);
                if(!safe(level,feet))continue;
                long score=surfacePriority(level.getBlockState(feet.below()))*1_000_000L+Math.abs(dy)*3000L+distanceSq;
                if(score<bestScore){bestScore=score;best=feet;}
            }
        }
        if(best!=null)return best;

        for(int dz=-radius;dz<=radius;dz++)for(int dx=-radius;dx<=radius;dx++){
            int distanceSq=dx*dx+dz*dz;
            if(distanceSq>radius*radius)continue;
            int x=node.mapX()+dx,z=node.mapZ()+dz;
            int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            BlockPos feet=new BlockPos(x,y,z);
            if(!safe(level,feet))continue;
            long score=surfacePriority(level.getBlockState(feet.below()))*1_000_000L+distanceSq;
            if(score<bestScore){bestScore=score;best=feet;}
        }
        return best;
    }

    private static boolean safe(ServerLevel level,BlockPos feet){
        BlockState ground=level.getBlockState(feet.below());
        if(ground.isAir()||ground.is(BlockTags.LEAVES)||!ground.getFluidState().isEmpty())return false;
        for(int i=0;i<=2;i++){
            BlockPos p=feet.above(i);
            if(!level.getFluidState(p).isEmpty()||!level.getBlockState(p).getCollisionShape(level,p).isEmpty())return false;
        }
        return true;
    }

    private static int surfacePriority(BlockState state){
        if(state.is(Blocks.DIRT_PATH))return 0;
        if(state.is(Blocks.GRAVEL)||state.is(Blocks.COBBLESTONE)||state.is(Blocks.STONE_BRICKS)
                ||state.is(Blocks.ANDESITE)||state.is(Blocks.POLISHED_ANDESITE)
                ||state.is(Blocks.OAK_PLANKS)||state.is(Blocks.SPRUCE_PLANKS))return 1;
        if(state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.PODZOL)||state.is(Blocks.MOSS_BLOCK)||state.is(Blocks.STONE))return 2;
        return 3;
    }

    private static String flag(String id){return FLAG_PREFIX+id;}
    private static void deny(ServerPlayer player,String text){
        player.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.GRAY));
        ExternalWorldBootstrap.refreshFieldContext(player);
    }
}

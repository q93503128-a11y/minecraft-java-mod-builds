package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class BattlePresentation {
    private final Map<String,UUID> actors=new LinkedHashMap<>();
    private final Map<String,Vec3> homes=new LinkedHashMap<>();
    private String moving; private int returnTicks;

    void spawn(ServerPlayer player, Iterable<CombatantState> combatants){
        ServerLevel level=(ServerLevel)player.level(); Vec3 anchor=player.position(); Vec3 forward=horizontal(player.getLookAngle()); Vec3 right=new Vec3(-forward.z,0,forward.x);
        int ai=0,ei=0;
        for(CombatantState c:combatants){
            double lane; double distance;
            if(c.side()==CombatantSide.ALLY){ lane=-4.6+ai*1.25; distance=4.2; ai++; }
            else { lane=-3.0+ei*1.5; distance=8.0; ei++; }
            Vec3 pos=anchor.add(forward.scale(distance)).add(right.scale(lane));
            ArmorStand stand=new ArmorStand(level,pos.x,pos.y,pos.z);
            stand.setCustomName(Component.literal(c.definition().name())); stand.setCustomNameVisible(true); stand.setInvulnerable(true); stand.setNoGravity(true); stand.setShowArms(true);
            stand.setItemSlot(EquipmentSlot.HEAD,new ItemStack(c.side()==CombatantSide.ALLY?Items.LIGHT_BLUE_WOOL:Items.RED_WOOL));
            level.addFreshEntity(stand); actors.put(c.instanceId(),stand.getUUID()); homes.put(c.instanceId(),pos);
        }
    }
    void lunge(ServerLevel level,String actorId,String targetId){
        var actor=entity(level,actorId); Vec3 target=homes.get(targetId); if(actor==null||target==null)return; Vec3 home=homes.get(actorId); Vec3 delta=target.subtract(home); if(delta.lengthSqr()>0.001) actor.setPos(target.subtract(delta.normalize().scale(1.4))); moving=actorId; returnTicks=5;
    }
    void tick(ServerLevel level){ if(moving==null)return; if(--returnTicks<=0){ var e=entity(level,moving); Vec3 home=homes.get(moving); if(e!=null&&home!=null)e.setPos(home); moving=null; } }
    void cleanup(ServerLevel level){ for(UUID id:actors.values()){ var e=level.getEntity(id); if(e!=null)e.discard(); } actors.clear(); homes.clear(); moving=null; }
    private ArmorStand entity(ServerLevel level,String id){ UUID uuid=actors.get(id); if(uuid==null)return null; var e=level.getEntity(uuid); return e instanceof ArmorStand a?a:null; }
    private static Vec3 horizontal(Vec3 v){ Vec3 h=new Vec3(v.x,0,v.z); return h.lengthSqr()<0.001?new Vec3(0,0,1):h.normalize(); }
}

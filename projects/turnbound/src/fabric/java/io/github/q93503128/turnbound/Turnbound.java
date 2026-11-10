package io.github.q93503128.turnbound;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

/**
 * Server-authoritative overworld exploration policy. This does not enable old Drehmal
 * quest/advancement/datapack logic and does not create a playable TURNBOUND party.
 */
public final class Turnbound implements ModInitializer {
    public static final Identifier WORLD_MODE_PACKET = new Identifier("turnbound", "world_mode");
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(TurnboundWorldGate::open);
        TurnboundBattleBridge.register();
        TurnboundCheckpointService.register();
        ServerLifecycleEvents.SERVER_STOPPED.register(TurnboundWorldGate::close);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler.player, WORLD_MODE_PACKET)) {
                var payload = PacketByteBufs.create();
                payload.writeBoolean(TurnboundWorldGate.active(server));
                ServerPlayNetworking.send(handler.player, WORLD_MODE_PACKET, payload);
            }
        });

        // Vanilla health is not the party's battle HP. Prevent damage before lethal effects.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof ServerPlayerEntity player && TurnboundWorldGate.active(player.getServer())));

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                !TurnboundWorldGate.active(player.getServer()));

        // Adventure mode normally blocks construction; also deny explicit placement-capable items.
        // Ordinary doors, levers, buttons and puzzle interactions stay usable.
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient() || !TurnboundWorldGate.active(player.getServer())) {
                return ActionResult.PASS;
            }
            ItemStack held = player.getStackInHand(hand);
            if (held.getItem() instanceof BlockItem
                    || held.isOf(Items.WATER_BUCKET) || held.isOf(Items.LAVA_BUCKET)
                    || held.isOf(Items.FLINT_AND_STEEL) || held.isOf(Items.FIRE_CHARGE)
                    || held.isOf(Items.BONE_MEAL)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!TurnboundWorldGate.active(server)) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.interactionManager.getGameMode() != GameMode.ADVENTURE) {
                    player.changeGameMode(GameMode.ADVENTURE);
                }
                player.getHungerManager().setFoodLevel(20);
                player.getHungerManager().setSaturationLevel(20.0F);
                if (player.getHealth() < player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        });
    }
}

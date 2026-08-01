package kr.moonseungjun.arcanecircle;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.network.ArcaneNetwork;
import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

@Mod(ArcaneCircle.MOD_ID)
public final class ArcaneCircle {
    public static final String MOD_ID = "arcanecircle";
    public static final String VERSION = "0.4.0-alpha.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArcaneCircle(IEventBus modEventBus) {
        SpellCatalog.bootstrap();
        ModItems.register(modEventBus);
        modEventBus.addListener(ArcaneNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        LOGGER.info("Arcane Circle {} loaded with {} spells, {} fusion formulae and {} staves",
                VERSION, SpellCatalog.spells().size(), SpellCatalog.fusions().size(), ModItems.all().size());
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MagicPlayerData data = MagicPlayerData.get(((ServerLevel) player.level()).getServer());
        boolean firstAwakening = data.ensureProfile(player);
        if (firstAwakening) {
            player.getInventory().add(new ItemStack(ModItems.NOVICE_STAFF.get()));
            player.sendSystemMessage(Component.literal(
                    "§5[구중 마법학] §f마력핵이 각성했습니다. §dC§f로 마도서를 여세요."));
            player.sendSystemMessage(Component.literal(
                    "§71~5: 주문 시전 · X를 누른 채 2~3개 숫자 주문 선택 후 X를 놓으면 융합 시전"));
            player.sendSystemMessage(Component.literal(
                    "§7성공한 융합은 숙련도가 오르며, 완성되면 일반 슬롯에 장착할 수 있습니다."));
        }
        ArcaneNetwork.sync(player);
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MagicPlayerData data = MagicPlayerData.get(((ServerLevel) player.level()).getServer());
        if (player.tickCount % 10 == 0) data.regenerate(player);
        if (player.tickCount % 20 == 0) ArcaneNetwork.sync(player);
    }
}

package kr.moonseungjun.arcanecircle;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.network.ArcaneNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

@Mod(ArcaneCircle.MOD_ID)
public final class ArcaneCircle {
    public static final String MOD_ID = "arcanecircle";
    public static final String VERSION = "0.2.0-alpha.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArcaneCircle(IEventBus modEventBus) {
        SpellCatalog.bootstrap();
        modEventBus.addListener(ArcaneNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        LOGGER.info("Arcane Circle {} loaded with {} spells and {} fusion formulae",
                VERSION, SpellCatalog.spells().size(), SpellCatalog.fusions().size());
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MagicPlayerData data = MagicPlayerData.get(player.getServer());
        boolean firstAwakening = data.ensureProfile(player);
        ArcaneNetwork.sync(player);
        if (firstAwakening) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§5[구중 마법학] §f마력핵이 각성했습니다. §dC§f로 마도서를 열고, §dR§f로 선택 주문을 시전하세요."));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§7Z/X: 주문 슬롯 변경 · 융합식과 전승 주문은 마도서에서 확인"));
        }
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 10 == 0) {
            MagicPlayerData.get(player.getServer()).regenerate(player);
        }
    }
}

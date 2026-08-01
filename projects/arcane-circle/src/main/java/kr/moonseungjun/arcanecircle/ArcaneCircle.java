package kr.moonseungjun.arcanecircle;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.network.ArcaneNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
    public static final String VERSION = "0.3.0-alpha.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArcaneCircle(IEventBus modEventBus) {
        SpellCatalog.bootstrap();
        modEventBus.addListener(ArcaneNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        LOGGER.info("Arcane Circle {} loaded with {} spells and {} live fusion formulae",
                VERSION, SpellCatalog.spells().size(), SpellCatalog.fusions().size());
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MagicPlayerData data = MagicPlayerData.get(((ServerLevel) player.level()).getServer());
        boolean firstAwakening = data.ensureProfile(player);
        ArcaneNetwork.sync(player);
        if (firstAwakening) {
            player.sendSystemMessage(Component.literal(
                    "§5[구중 마법학] §f마력핵이 각성했습니다. §dC§f로 성좌 마도서를 여세요."));
            player.sendSystemMessage(Component.literal(
                    "§7R: 주력 주문 · G: 주력+직조 즉석 융합 · Z/X: 주력 변경 · V/B: 직조 변경"));
            player.sendSystemMessage(Component.literal(
                    "§7융합 주문은 전투에서 반복 성공하면 숙련되어 단독 시전 주문으로 영구 등록됩니다."));
        }
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 10 == 0) {
            MagicPlayerData.get(((ServerLevel) player.level()).getServer()).regenerate(player);
        }
    }
}

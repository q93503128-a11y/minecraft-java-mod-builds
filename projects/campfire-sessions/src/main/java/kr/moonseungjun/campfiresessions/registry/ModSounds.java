package kr.moonseungjun.campfiresessions.registry;

import kr.moonseungjun.campfiresessions.CampfireSessions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, CampfireSessions.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ETIRWER = register("etirwer");
    public static final DeferredHolder<SoundEvent, SoundEvent> COZY_PUZZLE = register("cozy_puzzle");
    public static final DeferredHolder<SoundEvent, SoundEvent> NEON_CIRCUIT = register("neon_circuit");
    public static final DeferredHolder<SoundEvent, SoundEvent> UNDERWATER_PAD = register("underwater_pad");

    private ModSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> register(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(
                Identifier.fromNamespaceAndPath(CampfireSessions.MOD_ID, id)));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}

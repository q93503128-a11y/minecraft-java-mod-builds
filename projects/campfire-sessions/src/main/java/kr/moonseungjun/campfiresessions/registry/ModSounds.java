package kr.moonseungjun.campfiresessions.registry;

import kr.moonseungjun.campfiresessions.CampfireSessions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, CampfireSessions.MOD_ID);
    public static final DeferredHolder<SoundEvent, SoundEvent> ETIRWER = SOUNDS.register(
        "etirwer",
        () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(CampfireSessions.MOD_ID, "etirwer"))
    );
    private ModSounds() {}
    public static void register(IEventBus bus) { SOUNDS.register(bus); }
}

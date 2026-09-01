package io.github.q93503128.turnbound;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** TURNBOUND-owned music and battle SFX event registry. */
public final class TurnboundSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Turnbound.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_HUB = register("music.hub");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_REGION_EXPLORE = register("music.region_explore");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BATTLE_NORMAL = register("music.battle_normal");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BATTLE_ELITE = register("music.battle_elite");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BATTLE_BOSS = register("music.battle_boss");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_BATTLE_FINAL = register("music.battle_final");

    public static final DeferredHolder<SoundEvent, SoundEvent> SFX_SKILL = register("sfx.skill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SFX_HIT_LIGHT = register("sfx.hit_light");
    public static final DeferredHolder<SoundEvent, SoundEvent> SFX_HIT_HEAVY = register("sfx.hit_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> SFX_REACTION_HIT = register("sfx.reaction_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SFX_DOT_TICK = register("sfx.dot_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> SFX_HEAL = register("sfx.heal");
    public static final DeferredHolder<SoundEvent, SoundEvent> SFX_BARRIER = register("sfx.barrier");
    public static final DeferredHolder<SoundEvent, SoundEvent> SFX_REVIVE = register("sfx.revive");
    public static final DeferredHolder<SoundEvent, SoundEvent> SFX_DOWN = register("sfx.down");
    public static final DeferredHolder<SoundEvent, SoundEvent> SFX_BOSS_PHASE = register("sfx.boss_phase");
    public static final DeferredHolder<SoundEvent, SoundEvent> SFX_SPAWN = register("sfx.spawn");

    private TurnboundSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, path);
        return SOUND_EVENTS.register(path, () -> SoundEvent.createVariableRangeEvent(id));
    }
}

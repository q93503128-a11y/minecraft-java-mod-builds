from pathlib import Path

root = Path('projects/survival-ascension')
pkg = root / 'src/main/java/kr/moonseungjun/survivalascension'
lifecycle = pkg / 'progress/PlayerLifecycleState.java'
main = pkg / 'SurvivalAscension.java'
props = root / 'gradle.properties'

lifecycle.write_text('''package kr.moonseungjun.survivalascension.progress;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Preserves Survival Ascension player-local runtime preferences/cooldowns across the
 * ServerPlayer clone created by death or End return. NeoForge only copies the
 * PlayerPersisted subtag automatically; these legacy root keys predate that convention.
 */
public final class PlayerLifecycleState {
    private static final String[] LONG_KEYS = {
            "survivalascension_apex_content_mark_tick",
            "survivalascension_apex_hunt_ready",
            "survivalascension_ascension_trial_ready",
            "survivalascension_combat_shockwave_ready",
            "survivalascension_expedition_incident_ready",
            "survivalascension_next_warband_formation",
            "survivalascension_operation_interdiction_session",
            "survivalascension_outpost_breach_warning_ready",
            "survivalascension_outpost_siege_ready",
            "survivalascension_shield_wave_ready"
    };
    private static final String[] INT_KEYS = {
            "survivalascension_bulk_tool_wear_bank",
            "survivalascension_operation_interdiction_stage"
    };
    private static final String[] STRING_KEYS = {
            "survivalascension_apex_content_mark_type",
            "survivalascension_bulk_tool_wear_tool",
            "survivalascension_mining_mode"
    };

    private PlayerLifecycleState() {}

    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag source = event.getOriginal().getPersistentData();
        CompoundTag target = event.getEntity().getPersistentData();
        for (String key : LONG_KEYS) copyLong(source, target, key);
        for (String key : INT_KEYS) copyInt(source, target, key);
        for (String key : STRING_KEYS) copyString(source, target, key);
    }

    private static void copyLong(CompoundTag source, CompoundTag target, String key) {
        if (source.contains(key)) target.putLong(key, source.getLongOr(key, 0L));
    }

    private static void copyInt(CompoundTag source, CompoundTag target, String key) {
        if (source.contains(key)) target.putInt(key, source.getIntOr(key, 0));
    }

    private static void copyString(CompoundTag source, CompoundTag target, String key) {
        if (source.contains(key)) target.putString(key, source.getStringOr(key, ""));
    }
}
''', encoding='utf-8')

text = main.read_text(encoding='utf-8')
assert 'public static final String VERSION = "0.61.2-alpha.1";' in text
text = text.replace('public static final String VERSION = "0.61.2-alpha.1";',
                    'public static final String VERSION = "0.61.3-alpha.1";', 1)
marker = 'import kr.moonseungjun.survivalascension.network.SkillNetwork;\n'
assert marker in text
assert 'import kr.moonseungjun.survivalascension.progress.PlayerLifecycleState;' not in text
text = text.replace(marker, marker + 'import kr.moonseungjun.survivalascension.progress.PlayerLifecycleState;\n', 1)
marker = '        NeoForge.EVENT_BUS.addListener(FieldRecoveryService::onPlayerRespawn);\n'
assert marker in text
assert 'PlayerLifecycleState::onClone' not in text
text = text.replace(marker, marker + '        NeoForge.EVENT_BUS.addListener(PlayerLifecycleState::onClone);\n', 1)
main.write_text(text, encoding='utf-8')

text = props.read_text(encoding='utf-8')
assert 'mod_version=0.61.2-alpha.1' in text
props.write_text(text.replace('mod_version=0.61.2-alpha.1', 'mod_version=0.61.3-alpha.1', 1), encoding='utf-8')

print('SURVIVAL_AUDIT4_FIX_APPLIED')

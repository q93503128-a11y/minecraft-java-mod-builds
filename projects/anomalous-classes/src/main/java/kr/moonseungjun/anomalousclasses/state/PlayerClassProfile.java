package kr.moonseungjun.anomalousclasses.state;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class PlayerClassProfile {
    private ClassId currentClass;
    private ClassFocus equippedFocus;
    private final EnumMap<ClassId, ClassProgress> progress = new EnumMap<>(ClassId.class);
    private int revision;

    public PlayerClassProfile() {
        for (ClassId id : ClassId.values()) progress.put(id, ClassProgress.fresh());
    }

    public Optional<ClassId> currentClass() {
        return Optional.ofNullable(currentClass);
    }

    public ClassProgress progress(ClassId id) {
        return progress.getOrDefault(id, ClassProgress.fresh());
    }

    public Map<ClassId, ClassProgress> allProgress() {
        return Map.copyOf(progress);
    }

    public int revision() {
        return revision;
    }

    public Optional<ClassFocus> equippedFocus() {
        return Optional.ofNullable(equippedFocus);
    }

    public void select(ClassId id) {
        currentClass = id;
        progress.put(id, progress(id).markPlayed());
        revision++;
    }

    public void load(ClassId selected, Map<ClassId, ClassProgress> loaded, ClassFocus loadedFocus, int loadedRevision) {
        currentClass = selected;
        equippedFocus = loadedFocus;
        for (ClassId id : ClassId.values()) {
            progress.put(id, loaded.getOrDefault(id, ClassProgress.fresh()));
        }
        revision = Math.max(0, loadedRevision);
    }

    public void updateProgress(ClassId id, ClassProgress updated) {
        progress.put(id, updated);
        revision++;
    }

    public void updateProgressBatch(Map<ClassId, ClassProgress> updates) {
        if (updates.isEmpty()) return;
        EnumMap<ClassId, ClassProgress> validated = new EnumMap<>(ClassId.class);
        for (ClassId id : ClassId.values()) {
            ClassProgress value = updates.get(id);
            if (value != null) validated.put(id, value);
        }
        progress.putAll(validated);
        revision++;
    }

    public boolean equipFocus(ClassFocus focus) {
        if (java.util.Objects.equals(equippedFocus, focus)) return false;
        equippedFocus = focus;
        revision++;
        return true;
    }
}

package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentLookup;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Build-time validator for authored presentation coverage and boss-context uniqueness. */
public final class BossPresentationProfileValidator {
    public enum Code {
        MISSING_BOSS_PROFILE,
        DUPLICATE_PRESENTATION_CONTEXT,
        EMPTY_PRESENTATION_BINDINGS,
        MISSING_PATTERN_REFERENCE,
        MISSING_PRESENTATION_BINDING,
        UNUSED_PRESENTATION_BINDING
    }

    public record Issue(Code code, ContentId source, String message) { }

    public record Report(List<Issue> issues) {
        public Report { issues = List.copyOf(issues); }
        public boolean hasErrors() { return !issues.isEmpty(); }
        public List<Issue> byCode(Code code) { return issues.stream().filter(issue -> issue.code() == code).toList(); }
    }

    public Report validate(ContentLookup lookup, Collection<BossPresentationProfile> profiles) {
        Objects.requireNonNull(lookup, "lookup");
        Objects.requireNonNull(profiles, "profiles");
        List<Issue> issues = new ArrayList<>();
        Map<BossPresentationProfile.Context, ContentId> seenContexts = new HashMap<>();

        for (BossPresentationProfile profile : profiles) {
            Objects.requireNonNull(profile, "profile");
            ContentId existing = seenContexts.putIfAbsent(profile.context(), profile.id());
            if (existing != null) {
                issues.add(new Issue(Code.DUPLICATE_PRESENTATION_CONTEXT, profile.id(),
                    "presentation context already owned by " + existing + ": " + profile.context()));
            }
            if (profile.bindings().isEmpty()) {
                issues.add(new Issue(Code.EMPTY_PRESENTATION_BINDINGS, profile.id(), "presentation profile has no bindings"));
            }

            var bossDefinition = lookup.find(CoreDefinition.Kind.BOSS_PROFILE, profile.bossProfile());
            if (bossDefinition.isEmpty() || !(bossDefinition.get() instanceof CoreDefinition.BossProfile boss)) {
                issues.add(new Issue(Code.MISSING_BOSS_PROFILE, profile.id(), "missing boss profile: " + profile.bossProfile()));
                continue;
            }

            Set<BossPresentationProfile.BindingKey> expected = new HashSet<>();
            for (ContentId attackId : boss.attackPatterns()) {
                var attackDefinition = lookup.find(CoreDefinition.Kind.ATTACK_PATTERN, attackId);
                if (attackDefinition.isEmpty() || !(attackDefinition.get() instanceof CoreDefinition.AttackPattern attack)) {
                    issues.add(new Issue(Code.MISSING_PATTERN_REFERENCE, profile.id(), "missing attack pattern: " + attackId));
                    continue;
                }
                expected.add(new BossPresentationProfile.BindingKey(attack.presentationCue(), attack.delivery(), AttackTimeline.Phase.TELEGRAPH));
                expected.add(new BossPresentationProfile.BindingKey(attack.presentationCue(), attack.delivery(), AttackTimeline.Phase.ACTIVE));
                if (attack.recoveryTicks() > 0) {
                    expected.add(new BossPresentationProfile.BindingKey(attack.presentationCue(), attack.delivery(), AttackTimeline.Phase.RECOVERY));
                }
            }

            for (BossPresentationProfile.BindingKey required : expected) {
                if (!profile.bindings().containsKey(required)) {
                    issues.add(new Issue(Code.MISSING_PRESENTATION_BINDING, profile.id(), "missing selector " + required.selector()));
                }
            }
            for (BossPresentationProfile.BindingKey authored : profile.bindings().keySet()) {
                if (!expected.contains(authored)) {
                    issues.add(new Issue(Code.UNUSED_PRESENTATION_BINDING, profile.id(), "binding does not match boss attack semantics: " + authored.selector()));
                }
            }
        }

        issues.sort(Comparator.comparing((Issue issue) -> issue.source().toString())
            .thenComparing(issue -> issue.code().name())
            .thenComparing(Issue::message));
        return new Report(issues);
    }
}

package dev.moonseungjun.openworldrpg.integration.bootstrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;

public final class ValidationReport {
    private final List<ValidationIssue> issues = new ArrayList<>();

    public void add(ValidationSeverity severity, String code, String message) {
        issues.add(new ValidationIssue(severity, code, message));
    }

    public void addAll(ValidationReport other) {
        issues.addAll(other.issues);
    }

    public List<ValidationIssue> issues() {
        return Collections.unmodifiableList(issues);
    }

    public boolean hasErrors() {
        return issues.stream().anyMatch(issue -> issue.severity() == ValidationSeverity.ERROR);
    }

    public void logTo(Logger logger) {
        for (ValidationIssue issue : issues) {
            switch (issue.severity()) {
                case INFO -> logger.info("[{}] {}", issue.code(), issue.message());
                case WARN -> logger.warn("[{}] {}", issue.code(), issue.message());
                case ERROR -> logger.error("[{}] {}", issue.code(), issue.message());
            }
        }
    }
}

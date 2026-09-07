package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentLookup;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Server-side domain rules for the first expedition loop. Minecraft effects live in adapters. */
public final class ExpeditionLifecycle {
    private final ContentLookup content;

    public ExpeditionLifecycle(ContentLookup content) {
        this.content = Objects.requireNonNull(content, "content");
    }

    /** Legacy/test-fixture entry point. Production gameplay should pass an owner UUID. */
    public ExpeditionRun begin(long sequence, ContentId regionId, ContentId contractId, String fingerprint, long gameTime) {
        validateBegin(regionId, contractId);
        return ExpeditionRun.preparing(sequence, regionId, contractId, fingerprint, gameTime);
    }

    public ExpeditionRun begin(long sequence, ContentId regionId, ContentId contractId, UUID ownerId, String fingerprint, long gameTime) {
        validateBegin(regionId, contractId);
        return ExpeditionRun.preparing(sequence, regionId, contractId, ownerId, fingerprint, gameTime);
    }

    private void validateBegin(ContentId regionId, ContentId contractId) {
        CoreDefinition.Region region = require(CoreDefinition.Kind.REGION, regionId, CoreDefinition.Region.class);
        CoreDefinition.Contract contract = require(CoreDefinition.Kind.CONTRACT, contractId, CoreDefinition.Contract.class);
        if (!contract.region().equals(region.id())) {
            throw new IllegalArgumentException("Contract " + contractId + " does not belong to region " + regionId);
        }
        if (!region.contracts().contains(contract.id())) {
            throw new IllegalArgumentException("Region " + regionId + " does not expose contract " + contractId);
        }
    }

    public ExpeditionRun deploy(ExpeditionRun run) {
        requireRunDefinitions(run);
        return run.deploy();
    }

    public ExpeditionRun recover(ExpeditionRun run, ContentId resourceId, int amount) {
        requireRunDefinitions(run);
        CoreDefinition.ExpeditionResource resource = require(CoreDefinition.Kind.EXPEDITION_RESOURCE, resourceId, CoreDefinition.ExpeditionResource.class);
        if (!resource.region().equals(run.regionId())) {
            throw new IllegalArgumentException("Resource " + resourceId + " does not belong to active region " + run.regionId());
        }
        CoreDefinition.Region region = require(CoreDefinition.Kind.REGION, run.regionId(), CoreDefinition.Region.class);
        if (!region.resources().contains(resourceId)) {
            throw new IllegalArgumentException("Region " + run.regionId() + " does not expose resource " + resourceId);
        }
        return run.recover(resourceId, amount);
    }

    public ExpeditionRun requestExtraction(ExpeditionRun run) {
        requireRunDefinitions(run);
        requireContractObjectiveSatisfied(run);
        return run.requestExtraction();
    }

    public Resolution resolveExtraction(ExpeditionRun run, long gameTime) {
        requireRunDefinitions(run);
        if (run.status() != ExpeditionRun.Status.EXTRACTION_REQUESTED) {
            throw new IllegalStateException("Extraction can only resolve after it is requested");
        }
        CoreDefinition.Contract contract = requireContractObjectiveSatisfied(run);
        CoreDefinition.ExtractionResultProfile resultProfile = require(CoreDefinition.Kind.EXTRACTION_RESULT, contract.extractionResult(), CoreDefinition.ExtractionResultProfile.class);
        Map<ContentId, Integer> retained = retain(run.recoveredResources(), resultProfile.retainedPercent());
        ExpeditionRun completed = run.extract(gameTime);
        return new Resolution(completed, resultProfile, retained, contract.worldConsequence());
    }

    public ExpeditionRun fail(ExpeditionRun run, long gameTime, ExpeditionRun.EndReason reason) {
        requireRunDefinitions(run);
        return run.fail(gameTime, reason);
    }

    private void requireRunDefinitions(ExpeditionRun run) {
        Objects.requireNonNull(run, "run");
        require(CoreDefinition.Kind.REGION, run.regionId(), CoreDefinition.Region.class);
        CoreDefinition.Contract contract = require(CoreDefinition.Kind.CONTRACT, run.contractId(), CoreDefinition.Contract.class);
        if (!contract.region().equals(run.regionId())) throw new IllegalStateException("Persisted expedition references a contract from another region");
    }

    private CoreDefinition.Contract requireContractObjectiveSatisfied(ExpeditionRun run) {
        CoreDefinition.Contract contract = require(CoreDefinition.Kind.CONTRACT, run.contractId(), CoreDefinition.Contract.class);
        boolean objectiveSatisfied = contract.requiredResources().entrySet().stream()
            .allMatch(entry -> run.recoveredResources().getOrDefault(entry.getKey(), 0) >= entry.getValue());
        if (!objectiveSatisfied) {
            throw new IllegalStateException("Contract requirements are not satisfied for extraction: " + contract.id());
        }
        return contract;
    }

    private static Map<ContentId, Integer> retain(Map<ContentId, Integer> resources, int retainedPercent) {
        Map<ContentId, Integer> retained = new LinkedHashMap<>();
        resources.forEach((id, amount) -> {
            int kept = Math.floorDiv(Math.multiplyExact(amount, retainedPercent), 100);
            if (kept > 0) retained.put(id, kept);
        });
        return Map.copyOf(retained);
    }

    private <T extends CoreDefinition> T require(CoreDefinition.Kind kind, ContentId id, Class<T> type) {
        CoreDefinition definition = content.find(kind, id)
            .orElseThrow(() -> new IllegalArgumentException("Missing " + kind + " content definition: " + id));
        return type.cast(definition);
    }

    public record Resolution(ExpeditionRun run, CoreDefinition.ExtractionResultProfile resultProfile, Map<ContentId, Integer> retainedResources, String worldConsequence) {
        public Resolution {
            Objects.requireNonNull(run, "run");
            Objects.requireNonNull(resultProfile, "resultProfile");
            retainedResources = Map.copyOf(Objects.requireNonNull(retainedResources));
            worldConsequence = Objects.requireNonNull(worldConsequence, "worldConsequence");
        }
    }
}

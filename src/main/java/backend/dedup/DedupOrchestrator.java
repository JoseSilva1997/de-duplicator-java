package backend.dedup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import backend.dedup.strategies.EmailUsernameStrategy;
import backend.dedup.strategies.ExactEmailStrategy;
import backend.dedup.strategies.FuzzyKeyStrategy;
import backend.dedup.strategies.FuzzyNameStrategy;
import backend.dedup.strategies.NameCompanyKeyStrategy;
import backend.model.ContactRecord;
import backend.model.SheetData;

public final class DedupOrchestrator {
    
    private final List<DedupStrategy> strategies;

    public DedupOrchestrator() {
        this(defaultPipeline());
    }

    /** Allows custom strategy lists for tests or future variants. */
    public DedupOrchestrator(List<DedupStrategy> strategies) {
        this.strategies = List.copyOf(strategies);   // defensive copy
    }

    public DedupResult deduplicate(SheetData primary, List<ContactRecord> secondary) {
        List<ContactRecord> candidates = new ArrayList<>(primary.records());
        List<RemovedRecord> removed = new ArrayList<>();
        List<String> applied = new ArrayList<>();

        for (DedupStrategy strategy : strategies) {
            if (!primary.availableFields().containsAll(strategy.requiredFields())) {
                continue;   // sheet doesn't have the fields this strategy needs
            }
            Map<ContactRecord, Integer> hits = strategy.findMatches(candidates, secondary);
            if (hits.isEmpty()) {
                applied.add(strategy.name());
                continue;
            }

            // Iterate candidates and look up each in the hit map (rather than iterating
            // the map). If primary has duplicate records (e.g., the same person listed
            // twice), Map.put collapses them into one entry — but iterating candidates
            // and calling hits.get(r) returns the same confidence for each duplicate,
            // so all duplicates get correctly removed.
            List<ContactRecord> stillCandidates = new ArrayList<>(candidates.size());
            for (ContactRecord r : candidates) {
                Integer confidence = hits.get(r);
                if (confidence != null) {
                    removed.add(new RemovedRecord(r, strategy.name(), confidence));
                } else {
                    stillCandidates.add(r);
                }
            }
            candidates = stillCandidates;
            applied.add(strategy.name());
        }

        return new DedupResult(candidates, removed, applied);
    }

    public static List<DedupStrategy> defaultPipeline() {
        return List.of(
            new ExactEmailStrategy(),
            new EmailUsernameStrategy(),
            new NameCompanyKeyStrategy(),
            new FuzzyKeyStrategy(),
            new FuzzyNameStrategy()
        );
    }
}

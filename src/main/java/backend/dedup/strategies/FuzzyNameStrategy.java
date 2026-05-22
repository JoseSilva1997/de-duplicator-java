package backend.dedup.strategies;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import backend.dedup.DedupStrategy;
import backend.dedup.NormalisationUtils;
import backend.model.ContactField;
import backend.model.ContactRecord;
import me.xdrop.fuzzywuzzy.FuzzySearch;

public final class FuzzyNameStrategy implements DedupStrategy {

    private static final int THRESHOLD = 80;

    @Override public String name() { return "fuzzy_name"; }

    @Override public Set<ContactField> requiredFields() {
        // Per-row eligibility is checked inside findMatches (record needs either
        // FIRST_NAME+LAST_NAME or FULL_NAME). No sheet-level prerequisite.
        return Set.of();
    }

    @Override
    public Map<ContactRecord, Integer> findMatches(
            List<ContactRecord> primary, List<ContactRecord> secondary) {

        List<NameIndex> secondaryIndex = secondary.stream()
                .map(FuzzyNameStrategy::indexOf)
                .filter(Objects::nonNull)
                .toList();

        Map<ContactRecord, Integer> matches = new LinkedHashMap<>();
        for (ContactRecord r : primary) {
            NameIndex pi = indexOf(r);
            if (pi == null) continue;

            for (NameIndex sj : secondaryIndex) {
                if (!preFilterPasses(pi, sj)) continue;
                int score = FuzzySearch.tokenSetRatio(pi.displayName(), sj.displayName());
                if (score >= THRESHOLD) {
                    matches.put(r, score);
                    break;
                }
            }
        }
        return matches;
    }

    /** Build the name index for one record, or null if the record has no usable name. */
    private static NameIndex indexOf(ContactRecord r) {
        if (r.firstName() != null && r.lastName() != null) {
            String displayName = (r.firstName() + " " + r.lastName()).trim();
            return new NameIndex(
                displayName,
                NormalisationUtils.tokeniseName(r.firstName()),
                NormalisationUtils.tokeniseName(r.lastName()),
                /*structured=*/ true);
        }
        if (r.fullName() != null) {
            return new NameIndex(
                r.fullName(),
                NormalisationUtils.tokeniseName(r.fullName()),
                Set.of(),
                /*structured=*/ false);
        }
        return null;
    }

    private static boolean preFilterPasses(NameIndex a, NameIndex b) {
        if (a.structured() && b.structured()) {
            // First-name tokens overlap AND last-name tokens overlap.
            return !Collections.disjoint(a.firstTokens(), b.firstTokens())
                && !Collections.disjoint(a.lastTokens(),  b.lastTokens());
        }
        // At least one side lacks structure — collapse to combined-token overlap.
        Set<String> aAll = combine(a.firstTokens(), a.lastTokens());
        Set<String> bAll = combine(b.firstTokens(), b.lastTokens());
        return !Collections.disjoint(aAll, bAll);
    }

    private static Set<String> combine(Set<String> a, Set<String> b) {
        Set<String> all = new HashSet<>(a);
        all.addAll(b);
        return all;
    }

    /**
     * Pre-computed name representation. For structured records (had first+last),
     * firstTokens and lastTokens hold them separately. For full-name-only records,
     * firstTokens holds the entire name's tokens and lastTokens is empty.
     */
    private record NameIndex(
        String displayName,
        Set<String> firstTokens,
        Set<String> lastTokens,
        boolean structured
    ) {}
    
}

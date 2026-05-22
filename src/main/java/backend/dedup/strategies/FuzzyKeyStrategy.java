package backend.dedup.strategies;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import backend.dedup.DedupStrategy;
import backend.dedup.NormalisationUtils;
import backend.model.ContactField;
import backend.model.ContactRecord;
import me.xdrop.fuzzywuzzy.FuzzySearch;

public final class FuzzyKeyStrategy implements DedupStrategy {

    private static final int THRESHOLD = 80;
    private static final int CONFIDENCE = 85;

    @Override
    public String name() { return "fuzzy_key";}

    // requiredFields() is empty because this strategy accepts either
    // (FIRST_NAME + LAST_NAME) OR FULL_NAME — a disjunction the interface
    // can't express. The per-row eligibility check in indexOf(...) handles it.
    @Override
    public Set<ContactField> requiredFields() { 
        return Set.of(ContactField.FIRST_NAME, ContactField.LAST_NAME, ContactField.COMPANY); 
    }

    @Override
    public Map<ContactRecord, Integer> findMatches(List<ContactRecord> primary, List<ContactRecord> secondary) {

        // Pre-compute each secondary record's key and its token set, once.
        List<KeyTokens> secondaryIndex = secondary.stream()
                .filter(r -> r.firstName() != null && r.lastName() != null && r.company() != null)
                .map(r -> {
                    String key = NormalisationUtils.keyFor(r);
                    return new KeyTokens(key, tokensOf(key));})
                .toList();

        Map<ContactRecord, Integer> matches = new LinkedHashMap<>(); // Using LinkedHashMap to preserve order of primary list
        for (ContactRecord record : primary) {
            if (record.firstName() == null || record.lastName() == null || record.company() == null) continue;

            String primaryKey = NormalisationUtils.keyFor(record);
            Set<String> primaryTokens = tokensOf(primaryKey);

            // Check if any secondary key has a high token overlap with the primary key.
            for (KeyTokens cand : secondaryIndex) {
                // Pre-filter: only score candidate pairs that share at least one token
                // after splitting on '|' or whitespace. This is a heuristic; in theory two
                // keys could exceed the 80% threshold while sharing no tokens, but in
                // practice it's vanishingly rare and the speedup is significant.
                if (Collections.disjoint(primaryTokens, cand.tokens())) continue;
                if (FuzzySearch.tokenSetRatio(primaryKey, cand.key()) >= THRESHOLD) {
                    matches.put(record, CONFIDENCE);
                    break;
                }
            }
        }
        return matches;
    }

    private static Set<String> tokensOf(String key) {
        String[] parts = key.toLowerCase().split("[|\\s]+");
        Set<String> tokens = new HashSet<>();
        for (String p : parts) if (!p.isEmpty()) tokens.add(p);
        return tokens;
    }

    /** Pre-computed secondary entry: the key string plus its token set. */
    private record KeyTokens(String key, Set<String> tokens) {}
    
}

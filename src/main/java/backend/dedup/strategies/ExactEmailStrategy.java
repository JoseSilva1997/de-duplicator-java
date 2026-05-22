package backend.dedup.strategies;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import backend.dedup.DedupStrategy;
import backend.model.ContactField;
import backend.model.ContactRecord;

public final class ExactEmailStrategy implements DedupStrategy {

    private static final int CONFIDENCE = 100;

    @Override
    public String name() { return "email";}

    @Override
    public Set<ContactField> requiredFields() { return Set.of(ContactField.EMAIL); }

    @Override
    public Map<ContactRecord, Integer> findMatches(List<ContactRecord> primary, List<ContactRecord> secondary) {
        
        // Build a set of all emails in the secondary list for O(1) lookups.
        Set<String> secondaryEmails = secondary.stream()
            .map(ContactRecord::email)
            .filter(Objects::nonNull) // secondary records can have null emails
            .collect(Collectors.toSet());


        // For each record in the primary list, check if its email exists in the secondary set.
        Map<ContactRecord, Integer> matches = new LinkedHashMap<>(); // Using LinkedHashMap to preserve order of primary list
        for (ContactRecord record : primary) {
            if (record.email() != null && secondaryEmails.contains(record.email())) {
                matches.put(record, CONFIDENCE);
            }
        }

        return matches;
    }
    
}

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

public final class EmailUsernameStrategy implements DedupStrategy {

    private static final int CONFIDENCE = 90;

    @Override
    public String name() { return "email_username";}
    @Override
    public Set<ContactField> requiredFields() { return Set.of(ContactField.EMAIL); }

    @Override
    public Map<ContactRecord, Integer> findMatches(List<ContactRecord> primary, List<ContactRecord> secondary) {

        // Build a set of all email usernames in the secondary list.
        Set<String> secondaryUsernames = secondary.stream()
        .map(ContactRecord::email)
        .filter(Objects::nonNull) // secondary records can have null emails
        .map(EmailUsernameStrategy::usernameOf)
        .collect(Collectors.toSet());

        // For each record in the primary list, check if its email username exists in the secondary set.
        Map<ContactRecord, Integer> matches = new LinkedHashMap<>(); // Using LinkedHashMap to preserve order of primary list
        for (ContactRecord record : primary) {
            if (record.email() == null) continue;
            String username = usernameOf(record.email());
            if (secondaryUsernames.contains(username)) {
                matches.put(record, CONFIDENCE);
            }
        }
        return matches;

    }

    private static String usernameOf(String email) {
        int at = email.indexOf('@');
        return at < 0 ? email : email.substring(0, at);
    }
    
}

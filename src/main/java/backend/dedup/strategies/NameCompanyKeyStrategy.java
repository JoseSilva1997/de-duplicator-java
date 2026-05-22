package backend.dedup.strategies;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import backend.dedup.DedupStrategy;
import backend.dedup.NormalisationUtils;
import backend.model.ContactField;
import backend.model.ContactRecord;

public final class NameCompanyKeyStrategy implements DedupStrategy {

    private static final int CONFIDENCE = 90;

    @Override
    public String name() { return "name+company";}

     @Override
    public Set<ContactField> requiredFields() { 
        return Set.of(ContactField.FIRST_NAME, ContactField.LAST_NAME, ContactField.COMPANY); 
    }

    @Override
    public Map<ContactRecord, Integer> findMatches(List<ContactRecord> primary, List<ContactRecord> secondary) {
        // Only build keys for records that have all three fields.
        Set<String> secondaryKeys = secondary.stream()
                .filter(r -> r.firstName() != null && r.lastName() != null && r.company() != null)
                .map(NormalisationUtils::keyFor)
                .collect(Collectors.toSet());

        Map<ContactRecord, Integer> matches = new LinkedHashMap<>();
        for (ContactRecord record : primary) {
            if (record.firstName() == null || record.lastName() == null || record.company() == null) continue;
            String key = NormalisationUtils.keyFor(record);
            if (secondaryKeys.contains(key)) {
                matches.put(record, CONFIDENCE);
            }
        }
        return matches;
    }
    
}

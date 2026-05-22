package backend.readers;

import backend.model.ContactField;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves header cells to standard contact fields using normalization and alias matching.
 * The resolution process is case-insensitive and ignores non-alphanumeric characters.
 */
public final class HeaderResolver {
    private final Map<ContactField, Integer> resolved;

    public HeaderResolver(List<String> headerCells) {
        Map<ContactField, Integer> headerMap = new EnumMap<>(ContactField.class);

        List<String> normalized = headerCells.stream().map(HeaderResolver::normalize).toList();

        // Alias-order wins, NOT column-order: for each ContactField we iterate its
        // aliases in declared order and take the first matching header. A sheet with
        // both "Name" and "First Name" columns will claim FIRST_NAME via "firstname"
        // (declared first), regardless of which column came first in the source.
        for(ContactField field : ContactField.values()) {
            outer:
            for(String alias : field.aliases()) {
                for(int i = 0; i < headerCells.size(); i++) {
                    if (normalized.get(i).equals(alias)) {
                        headerMap.put(field, i);
                        break outer; // Stop searching aliases for this field once a match is found.
                    }
                }
            }
        }

        // If a sheet has structured first+last AND a "Full Name" column, treat the
        // split as canonical and ignore the redundant Name column..
        if (headerMap.containsKey(ContactField.FIRST_NAME) && headerMap.containsKey(ContactField.LAST_NAME)) {
            headerMap.remove(ContactField.FULL_NAME);
        }

        // No recognized fields found, likely a malformed header row.
        if (headerMap.isEmpty()) {
            throw new IllegalArgumentException("No recognized contact fields found in header: " + headerCells);
        }

        this.resolved = headerMap;
    }

    public int indexOf(ContactField field) {
        return resolved.getOrDefault(field, -1);
    }

    public boolean hasField(ContactField field) {
        return resolved.containsKey(field);
    }

    public Set<ContactField> availableFields() {
        return Collections.unmodifiableSet(resolved.keySet());
    }

    private static String normalize(String headerCell) {
        if (headerCell == null) return "";
        return headerCell.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}


package backend.file_readers;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import backend.data.ContactField;

/**
 * Resolves header cells to standard contact fields using normalization and alias matching.
 * The resolution process is case-insensitive and ignores non-alphanumeric characters.
 */
public final class HeaderResolver {
    private final Map<ContactField, Integer> resolved;

    public HeaderResolver(List<String> headerCells) {
        Map<ContactField, Integer> headerMap = new EnumMap<>(ContactField.class);

        List<String> normalized = headerCells.stream().map(HeaderResolver::normalize).toList();

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

        // If both FIRST_NAME and LAST_NAME mapped, drop FULL_NAME.
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

    private static String normalize(String headerCell) {
        if (headerCell == null) return "";
        return headerCell.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}

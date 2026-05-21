package backend.data;

import java.util.List;

/**
 * Defines the standard contact fields and their common aliases for mapping during file reading and deduplication.
 * This enum should only store normalised field names.
 * Entries should be ordered by likelihood of occurrence in real-world data to optimize header resolution performance.
 */
public enum ContactField {
    FIRST_NAME(List.of("firstname", "name")),
    LAST_NAME(List.of("lastname", "lname", "surname")),
    FULL_NAME(List.of("fullname")),
    EMAIL(List.of("email", "emailaddress")),
    COMPANY(List.of("company", "organization", "companyname", "organisation")),
    JOB_TITLE(List.of("jobtitle", "position", "job", "role"));

    private final List<String> aliases;
    ContactField(List<String> aliases) {
        this.aliases = aliases;
    }
    public List<String> aliases() {
        return aliases;
    }
}


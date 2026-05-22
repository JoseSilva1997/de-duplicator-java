package backend.model;

import java.util.List;

/**
 * Defines the standard contact fields and their common aliases for mapping during file reading and deduplication.
 * This enum should only store normalised field names.
 * Entries should be ordered by likelihood of occurrence in real-world data to optimize header resolution performance.
 */
public enum ContactField {
    FIRST_NAME("First Name", List.of("firstname", "name")),
    LAST_NAME("Last Name", List.of("lastname", "lname", "surname")),
    FULL_NAME("Full Name", List.of("fullname")),
    EMAIL("Email", List.of("email", "emailaddress")),
    COMPANY("Company", List.of("company", "organization", "companyname", "organisation")),
    JOB_TITLE("Job Title", List.of("jobtitle", "position", "job", "role"));

    private final String label;
    private final List<String> aliases;
    
    ContactField(String label, List<String> aliases) {
        this.label = label;
        this.aliases = aliases;
    }

    public String label() { return label; }

    public List<String> aliases() { return aliases; }
}



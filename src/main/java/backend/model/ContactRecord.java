package backend.model;

public record ContactRecord(
    String firstName,
    String lastName,
    String fullName,
    String email,
    String company,
    String jobTitle,
    String country
){

    public ContactRecord {
        firstName = normalize(firstName);
        lastName = normalize(lastName);
        fullName = normalize(fullName);
        email = normalize(email);
        // Email is the only field lowercased — emails are normally case-insensitive,
        // and dedup compares them with plain equals. Name/company keep original casing
        // so output to user looks like input; case-insensitivity in dedup is the
        // strategy layer's job.
        if (email != null) email = email.toLowerCase();
        company = normalize(company);
        jobTitle = normalize(jobTitle);
        country = normalize(country);
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }

    public boolean isEmpty() {
        return firstName == null
            && lastName == null
            && fullName == null
            && email == null
            && company == null
            && jobTitle == null
            && country == null;
    }

    /**
     * True if the record has at least one field that identifies a person — email or any
     * name field. Rows lacking all of these (e.g., only a company or job title) aren't
     * deduplicable and shouldn't enter the pipeline.
     */
    public boolean hasIdentifier() {
        return email != null
            || firstName != null
            || lastName != null
            || fullName != null;
    }

}

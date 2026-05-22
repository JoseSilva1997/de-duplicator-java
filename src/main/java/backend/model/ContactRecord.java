package backend.model;

public record ContactRecord(
    String firstName,
    String lastName,
    String fullName,
    String email,
    String company,
    String jobTitle
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
            && jobTitle == null;
    }

}


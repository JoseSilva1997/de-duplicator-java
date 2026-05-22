package backend.dedup;

import backend.model.ContactRecord;

public record RemovedRecord(
    ContactRecord contact,
    String reason, // which strategy matched: "email", "email_username", "name+company", "fuzzy_key", "fuzzy_name"
    int confidence // 0-100
) {}

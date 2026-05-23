package backend.readers;

import java.util.function.IntFunction;

import backend.model.ContactField;
import backend.model.ContactRecord;

public class ContactRecordMapper {

    private final HeaderResolver resolver;

    ContactRecordMapper(HeaderResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Builds a record from one row. Returns null if the row lacks an identifier field
     * (email or any name) — such rows can't be deduplicated and shouldn't enter the pipeline.
     */
    ContactRecord map(IntFunction<String> cellAt) {
        ContactRecord rec = new ContactRecord(
            valueOf(ContactField.FIRST_NAME, cellAt),
            valueOf(ContactField.LAST_NAME,  cellAt),
            valueOf(ContactField.FULL_NAME,  cellAt),
            valueOf(ContactField.EMAIL,      cellAt),
            valueOf(ContactField.COMPANY,    cellAt),
            valueOf(ContactField.JOB_TITLE,  cellAt),
            valueOf(ContactField.COUNTRY,    cellAt)
        );
        return rec.hasIdentifier() ? rec : null;
    }

    private String valueOf(ContactField field, IntFunction<String> cellAt) {
        int idx = resolver.indexOf(field);
        return idx < 0 ? null : cellAt.apply(idx);
    }
}

package backend.readers;

import java.util.function.IntFunction;

import backend.model.ContactField;
import backend.model.ContactRecord;

public class ContactRecordMapper {
    
    private final HeaderResolver resolver;

    ContactRecordMapper(HeaderResolver resolver) {
        this.resolver = resolver;
    }

    /** Builds a record from one row. Returns null if every field ended up null. */
    ContactRecord map(IntFunction<String> cellAt) {
        ContactRecord rec = new ContactRecord(
            valueOf(ContactField.FIRST_NAME, cellAt),
            valueOf(ContactField.LAST_NAME,  cellAt),
            valueOf(ContactField.FULL_NAME,  cellAt),
            valueOf(ContactField.EMAIL,      cellAt),
            valueOf(ContactField.COMPANY,    cellAt),
            valueOf(ContactField.JOB_TITLE,  cellAt)
        );
        return rec.isEmpty() ? null : rec;
    }

    private String valueOf(ContactField field, IntFunction<String> cellAt) {
        int idx = resolver.indexOf(field);
        return idx < 0 ? null : cellAt.apply(idx);
    }
}

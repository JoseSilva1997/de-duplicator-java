package backend.model;

import java.util.List;
import java.util.Set;

/**
 * The result of reading a sheet: the kept records and the set of fields
 * the header resolved.
 */
public record SheetData(
    List<ContactRecord> records,
    Set<ContactField> availableFields
) {}

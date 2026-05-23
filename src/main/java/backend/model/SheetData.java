package backend.model;

import java.util.List;
import java.util.Set;

/**
 * The result of reading a sheet: the kept records, the set of fields the header resolved,
 * and the count of rows the mapper rejected as junk (no identifier).
 */
public record SheetData(
    List<ContactRecord> records,
    Set<ContactField> availableFields,
    int junkRowsDropped
) {}

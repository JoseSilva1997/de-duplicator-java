package backend.model;

import java.util.List;
import java.util.Set;

/** 
 * The result of reading a sheet: the list of records, and the set of fields that were present. 
 */
public record SheetData(
    List<ContactRecord> records,
    Set<ContactField> availableFields
) {}

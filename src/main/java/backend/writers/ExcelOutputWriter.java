package backend.writers;

import backend.dedup.DedupResult;
import backend.dedup.RemovedRecord;
import backend.model.ContactField;
import backend.model.ContactRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;


public final class ExcelOutputWriter {
    
    public void write(Path primaryInputPath, Map<String, DedupResult> results) throws IOException {
    Path parent = primaryInputPath.getParent();
    String base = stripExtension(primaryInputPath.getFileName().toString());

    Path keptPath = parent.resolve("Updated guests list from " + base + ".xlsx");
    Path removedPath = parent.resolve("People removed from " + base + ".xlsx");

    writeKept(keptPath, results);
    writeRemoved(removedPath, results);
}

private void writeKept(Path out, Map<String, DedupResult> results) throws IOException {
    try (Workbook wb = new XSSFWorkbook(); OutputStream os = Files.newOutputStream(out)) {
        for (var entry : results.entrySet()) {
            Sheet sheet = wb.createSheet(safeSheetName(entry.getKey()));
            writeContactHeader(sheet);
            int row = 1;
            for (ContactRecord r : entry.getValue().kept()) {
                writeContactRow(sheet, row++, r);
            }
        }
        wb.write(os);
    }
}

private void writeRemoved(Path out, Map<String, DedupResult> results) throws IOException {
    boolean anyRemovals = results.values().stream().anyMatch(r -> !r.removed().isEmpty());
    if (!anyRemovals) return;   // no file at all if nothing was removed

    try (Workbook wb = new XSSFWorkbook(); OutputStream os = Files.newOutputStream(out)) {
        for (var entry : results.entrySet()) {
            if (entry.getValue().removed().isEmpty()) continue;
            Sheet sheet = wb.createSheet(safeSheetName(entry.getKey()));
            writeRemovedHeader(sheet);
            int row = 1;
            for (RemovedRecord r : entry.getValue().removed()) {
                writeRemovedRow(sheet, row++, r);
            }
        }
        wb.write(os);
    }
}

private static void writeContactHeader(Sheet sheet) {
    Row r = sheet.createRow(0);
    int col = 0;
    for (ContactField field : ContactField.values()) {
        r.createCell(col++).setCellValue(field.label());
    }
}

// Null fields stay as unwritten cells (we don't call setCellValue). POI/Excel
// treat absent cells as blank, which is what we want — keeps "blank" and
// "explicitly empty string" indistinguishable in output.
private static void writeContactRow(Sheet sheet, int rowIndex, ContactRecord rec) {
    Row r = sheet.createRow(rowIndex);
    int col = 0;
    for (ContactField field : ContactField.values()) {
        String v = valueFor(rec, field);
        if (v != null) r.createCell(col).setCellValue(v);
        col++;
    }
}

private static void writeRemovedHeader(Sheet sheet) {
    Row r = sheet.createRow(0);
    int col = 0;
    for (ContactField field : ContactField.values()) {
        r.createCell(col++).setCellValue(field.label());
    }
    r.createCell(col++).setCellValue("Reason");
    r.createCell(col).setCellValue("Confidence");
}

private static void writeRemovedRow(Sheet sheet, int rowIndex, RemovedRecord rec) {
    Row r = sheet.createRow(rowIndex);
    int col = 0;
    for (ContactField field : ContactField.values()) {
        String v = valueFor(rec.contact(), field);
        if (v != null) r.createCell(col).setCellValue(v);
        col++;
    }
    r.createCell(col++).setCellValue(rec.reason());
    r.createCell(col).setCellValue(rec.confidence());
}

private static String valueFor(ContactRecord r, ContactField field) {
    return switch (field) {
        case FIRST_NAME -> r.firstName();
        case LAST_NAME  -> r.lastName();
        case FULL_NAME  -> r.fullName();
        case EMAIL      -> r.email();
        case COMPANY    -> r.company();
        case JOB_TITLE  -> r.jobTitle();
    };
}

// Excel hard limit: sheet names cannot exceed 31 chars. Truncate to satisfy
// the constraint; collisions between long names that share a prefix will
// throw at sheet creation — deal with it if it ever happens.
private static String safeSheetName(String name) {
    return name.length() > 31 ? name.substring(0, 31) : name;
}

private static String stripExtension(String filename) {
    int dot = filename.lastIndexOf('.');
    return dot > 0 ? filename.substring(0, dot) : filename;
}


}

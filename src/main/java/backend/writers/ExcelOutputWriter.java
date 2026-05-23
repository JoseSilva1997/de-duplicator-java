package backend.writers;

import backend.dedup.DedupResult;
import backend.dedup.RemovedRecord;
import backend.model.ContactField;
import backend.model.ContactRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class ExcelOutputWriter {

    /** Writes the kept + removed workbooks and returns the directory they landed in. */
    public Path write(Path primaryInputPath, Map<String, DedupResult> results) throws IOException {
        Path outputDir = resolveOutputDir();
        Files.createDirectories(outputDir);

        String base = stripExtension(primaryInputPath.getFileName().toString());
        Path keptPath = outputDir.resolve("Updated guests list from " + base + ".xlsx");
        Path removedPath = outputDir.resolve("People removed from " + base + ".xlsx");

        writeKept(keptPath, results);
        writeRemoved(removedPath, results);
        return outputDir;
    }

    private void writeKept(Path out, Map<String, DedupResult> results) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); OutputStream os = Files.newOutputStream(out)) {
            for (var entry : results.entrySet()) {
                DedupResult res = entry.getValue();
                List<ContactField> columns = columnsFor(res.sourceFields());
                Sheet sheet = wb.createSheet(safeSheetName(entry.getKey()));
                writeContactHeader(sheet, columns);
                int row = 1;
                for (ContactRecord r : res.kept()) {
                    writeContactRow(sheet, row++, r, columns);
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
                DedupResult res = entry.getValue();
                if (res.removed().isEmpty()) continue;
                List<ContactField> columns = columnsFor(res.sourceFields());
                Sheet sheet = wb.createSheet(safeSheetName(entry.getKey()));
                writeRemovedHeader(sheet, columns);
                int row = 1;
                for (RemovedRecord r : res.removed()) {
                    writeRemovedRow(sheet, row++, r, columns);
                }
            }
            wb.write(os);
        }
    }

    /**
     * Picks which contact fields to output for a given source.
     *  - Full Name is dropped when the source had both structured First Name and Last Name
     *    (the column would be empty and the same info is in the split fields).
     *  - Country is dropped if the source didn't have it (opt-in pass-through field).
     *  - Other fields are always written, even if empty in this source, for consistency.
     */
    private static List<ContactField> columnsFor(Set<ContactField> sourceFields) {
        boolean hasStructuredName = sourceFields.contains(ContactField.FIRST_NAME)
                                 && sourceFields.contains(ContactField.LAST_NAME);
        boolean hasCountry = sourceFields.contains(ContactField.COUNTRY);

        List<ContactField> cols = new ArrayList<>(ContactField.values().length);
        for (ContactField f : ContactField.values()) {
            if (f == ContactField.FULL_NAME && hasStructuredName) continue;
            if (f == ContactField.COUNTRY && !hasCountry) continue;
            cols.add(f);
        }
        return cols;
    }

    private static void writeContactHeader(Sheet sheet, List<ContactField> columns) {
        Row r = sheet.createRow(0);
        int col = 0;
        for (ContactField field : columns) {
            r.createCell(col++).setCellValue(field.label());
        }
    }

    // Null fields stay as unwritten cells (we don't call setCellValue). POI/Excel
    // treat absent cells as blank, which is what we want — keeps "blank" and
    // "explicitly empty string" indistinguishable in output.
    private static void writeContactRow(Sheet sheet, int rowIndex, ContactRecord rec, List<ContactField> columns) {
        Row r = sheet.createRow(rowIndex);
        int col = 0;
        for (ContactField field : columns) {
            String v = valueFor(rec, field);
            if (v != null) r.createCell(col).setCellValue(v);
            col++;
        }
    }

    private static void writeRemovedHeader(Sheet sheet, List<ContactField> columns) {
        Row r = sheet.createRow(0);
        int col = 0;
        for (ContactField field : columns) {
            r.createCell(col++).setCellValue(field.label());
        }
        r.createCell(col++).setCellValue("Reason");
        r.createCell(col).setCellValue("Confidence");
    }

    private static void writeRemovedRow(Sheet sheet, int rowIndex, RemovedRecord rec, List<ContactField> columns) {
        Row r = sheet.createRow(rowIndex);
        int col = 0;
        for (ContactField field : columns) {
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
            case COUNTRY    -> r.country();
        };
    }

    // Excel hard limit: sheet names cannot exceed 31 chars. Truncate to satisfy
    // the constraint; collisions between long names that share a prefix will
    // throw at sheet creation — deal with it if it ever happens.
    private static String safeSheetName(String name) {
        return name.length() > 31 ? name.substring(0, 31) : name;
    }

    // In production (packaged jar) outputs go on the user's desktop, where the
    // stakeholder wants them visible without hunting through folders. In dev
    // (running from target/classes via IDE or `mvn exec:java`) they go to the
    // project's /outputs folder so the build tree stays clean and we don't
    // litter the developer's actual desktop.
    private static Path resolveOutputDir() {
        if (isDevelopmentRun()) {
            return Paths.get("").toAbsolutePath().resolve("outputs");
        }
        return Paths.get(System.getProperty("user.home"), "Desktop");
    }

    private static boolean isDevelopmentRun() {
        try {
            Path codeSource = Paths.get(
                ExcelOutputWriter.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            return Files.isDirectory(codeSource);
        } catch (URISyntaxException | NullPointerException e) {
            return false;
        }
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}

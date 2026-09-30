package org.aastrika.util;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.aastrika.exception.ApiException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Reads the user bulk-upload spreadsheet into plain rows. No I/O beyond the supplied bytes and no
 * network, so it is fully unit-testable.
 *
 * <p>Expected shape — the first sheet only, one header row, then data:
 * <pre>
 * firstName | lastName | email | phone
 * </pre>
 *
 * <p>Three deliberate differences from the source {@code UserBulkUploadService.processBulkUpload}:
 * <ul>
 *   <li><b>The header is validated.</b> The source reads by fixed column index and never checks the
 *       header, so a sheet with email and phone swapped is happily imported into the wrong fields.</li>
 *   <li><b>Every cell is read as display text</b> via {@link DataFormatter}. The source only read the
 *       phone when the cell was numeric, and held it in a variable declared outside the row loop — so a
 *       text-formatted phone silently inherited the previous row's number.</li>
 *   <li><b>Blank rows are skipped, not terminal.</b> The source breaks out of the loop on the first
 *       row whose first cell is null, so one stray blank row truncates the upload and still reports
 *       success for the partial file.</li>
 * </ul>
 */
@Component
public class UserSheetParser {

    private static final String API_ID = "api.user.bulk.upload";
    private static final List<String> EXPECTED_HEADERS = List.of("firstname", "lastname", "email", "phone");

    private final int maxRows;
    private final DataFormatter formatter = new DataFormatter();

    public UserSheetParser(@Value("${bulk-upload.max-rows:2000}") int maxRows) {
        this.maxRows = maxRows;
    }

    /** One data row, carrying the 1-based sheet row number the admin sees in Excel. */
    public record ParsedRow(int rowNumber, String firstName, String lastName, String email, String phone) {
    }

    /**
     * @param xlsx raw {@code .xlsx} bytes
     * @return the data rows in sheet order
     * @throws ApiException 400 if the file is unreadable, the header is wrong, there are no data
     *         rows, or the row cap is exceeded
     */
    public List<ParsedRow> parse(byte[] xlsx) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            if (workbook.getNumberOfSheets() == 0) {
                throw badRequest("The workbook contains no sheets");
            }
            Sheet sheet = workbook.getSheetAt(0);
            validateHeader(sheet.getRow(sheet.getFirstRowNum()));

            List<ParsedRow> rows = new ArrayList<>();
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String firstName = text(row, 0);
                String lastName = text(row, 1);
                String email = text(row, 2);
                String phone = text(row, 3);
                if (firstName.isEmpty() && lastName.isEmpty() && email.isEmpty() && phone.isEmpty()) {
                    continue;   // fully blank row — skip, do not stop
                }
                if (rows.size() >= maxRows) {
                    throw badRequest("The file exceeds the maximum of " + maxRows + " data rows");
                }
                rows.add(new ParsedRow(row.getRowNum() + 1, firstName, lastName, email, phone));
            }
            if (rows.isEmpty()) {
                throw badRequest("The file contains no data rows");
            }
            return rows;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            // Covers a non-xlsx upload (.xls, .csv, arbitrary bytes) and corrupt archives. The cause is
            // not echoed to the caller — SECURITY.md 2.6 keeps internal messages out of responses.
            throw badRequest("The file could not be read as a .xlsx workbook");
        }
    }

    private void validateHeader(Row header) {
        if (header == null) {
            throw badRequest("The first sheet is empty");
        }
        List<String> actual = new ArrayList<>();
        for (int c = 0; c < EXPECTED_HEADERS.size(); c++) {
            actual.add(text(header, c).toLowerCase());
        }
        if (!EXPECTED_HEADERS.equals(actual)) {
            throw badRequest("Unexpected header row. Expected columns in order: firstName, lastName, email, phone");
        }
    }

    /** Display text of a cell, trimmed; empty string when the cell is missing or blank. */
    private String text(Row row, int column) {
        Cell cell = row.getCell(column);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private static ApiException badRequest(String message) {
        return new ApiException(API_ID, HttpStatus.BAD_REQUEST, message);
    }
}

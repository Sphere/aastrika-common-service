package org.aastrika.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.aastrika.util.UserSheetParser.ParsedRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * CPU-only validation of parsed bulk-upload rows: field patterns, accepted email domain, and
 * duplicates within the file. Runs before the API responds, so a bad sheet is reported immediately
 * and only plausible rows go on to cost network calls.
 *
 * <p>Patterns are ported verbatim from the source {@code ProjectUtil}. Two source defects are fixed:
 * <ul>
 *   <li><b>Domain matching is exact.</b> The source did
 *       {@code configuredDomain.contains(emailDomain)} against a single configured value — a substring
 *       test, so with {@code gmail.com} configured, {@code mail.com}, {@code ail.com} and even
 *       {@code l.com} were all accepted. Here the domain must equal one of the configured values.</li>
 *   <li><b>A missing "@" no longer aborts the run.</b> The source called
 *       {@code email.split("@")[1]} in its domain check <i>before</i> the email-pattern check, so a
 *       single malformed address threw {@code ArrayIndexOutOfBoundsException} out of the row loop and
 *       failed the whole job with zeroed counts. Here the pattern is checked first and the domain
 *       check is skipped when there is no domain to read.</li>
 * </ul>
 *
 * <p>Duplicate detection is new — the source imported the same address twice and let the second row
 * fail downstream with a confusing "already registered".
 */
@Component
public class UserRecordValidator {

    private static final Pattern EMAIL = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
    private static final Pattern PHONE = Pattern.compile("^\\d{10}$");
    private static final Pattern FIRST_NAME = Pattern.compile("^[a-zA-Z]+((['][a-zA-Z])?[a-zA-Z]*)*$");
    private static final Pattern LAST_NAME = Pattern.compile("^[a-zA-Z]*$");

    private final Set<String> acceptedDomains;

    public UserRecordValidator(
            @Value("${bulk-upload.accepted-email-domains:gmail.com}") String acceptedDomains) {
        this.acceptedDomains = Arrays.stream(acceptedDomains.split(","))
                .map(String::trim)
                .filter(domain -> !domain.isEmpty())
                .map(domain -> domain.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * @return row number → validation errors, for rows that have any. Rows absent from the map are
     *         valid so far and become {@code PENDING}; the existence checks happen later, online.
     */
    public Map<Integer, List<String>> validate(List<ParsedRow> rows) {
        Map<String, Integer> firstEmailRow = new HashMap<>();
        Map<String, Integer> firstPhoneRow = new HashMap<>();
        Map<Integer, List<String>> errorsByRow = new LinkedHashMap<>();

        for (ParsedRow row : rows) {
            List<String> errors = new ArrayList<>();

            if (row.firstName().isEmpty()) {
                errors.add("First name is required");
            } else if (!FIRST_NAME.matcher(row.firstName()).matches()) {
                errors.add("Invalid first name");
            }
            // Last name is optional, matching the source's "[a-zA-Z]*" which accepts empty.
            if (!LAST_NAME.matcher(row.lastName()).matches()) {
                errors.add("Invalid last name");
            }

            boolean emailWellFormed = EMAIL.matcher(row.email()).matches();
            if (row.email().isEmpty()) {
                errors.add("Email is required");
            } else if (!emailWellFormed) {
                errors.add("Invalid email id");
            } else if (!acceptedDomains.contains(domainOf(row.email()))) {
                // Only reachable once the address is well-formed, so there is always a domain to read.
                errors.add("Email domain is not accepted");
            }

            if (row.phone().isEmpty()) {
                errors.add("Phone is required");
            } else if (!PHONE.matcher(row.phone()).matches()) {
                errors.add("Invalid phone number");
            }

            // Duplicates within this file. Keyed case-insensitively for email.
            if (emailWellFormed) {
                Integer seen = firstEmailRow.putIfAbsent(row.email().toLowerCase(Locale.ROOT), row.rowNumber());
                if (seen != null) {
                    errors.add("Duplicate email in file (first seen at row " + seen + ")");
                }
            }
            if (!row.phone().isEmpty()) {
                Integer seen = firstPhoneRow.putIfAbsent(row.phone(), row.rowNumber());
                if (seen != null) {
                    errors.add("Duplicate phone in file (first seen at row " + seen + ")");
                }
            }

            if (!errors.isEmpty()) {
                errorsByRow.put(row.rowNumber(), errors);
            }
        }
        return errorsByRow;
    }

    private static String domainOf(String email) {
        return email.substring(email.indexOf('@') + 1).toLowerCase(Locale.ROOT);
    }
}

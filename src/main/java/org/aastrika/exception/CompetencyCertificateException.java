package org.aastrika.exception;

/**
 * A competency certificate generation failure with a stable code and a safe, fixed message. The
 * message ({@code CODE: text}) is stored in {@code user_competency_certificate.message} and returned by
 * the status API, so it must never carry raw exception text, URLs or host names (SECURITY.md 2.6).
 */
public class CompetencyCertificateException extends RuntimeException {

    public static final String TEMPLATE_NOT_CONFIGURED = "TEMPLATE_NOT_CONFIGURED";
    public static final String RC_GENERATION_FAILED = "RC_GENERATION_FAILED";
    public static final String RC_UNAVAILABLE = "RC_UNAVAILABLE";
    public static final String RC_NO_CERTIFICATE_URL = "RC_NO_CERTIFICATE_URL";
    public static final String UNEXPECTED_ERROR = "UNEXPECTED_ERROR";

    private final String code;

    public CompetencyCertificateException(String code, String message) {
        this(code, message, null);
    }

    public CompetencyCertificateException(String code, String message, Throwable cause) {
        super(code + ": " + message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
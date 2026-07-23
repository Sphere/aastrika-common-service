package org.aastrika.controller;

import java.util.Map;

import jakarta.validation.Valid;
import org.aastrika.common.Constants;
import org.aastrika.dto.request.AdminPassbookReadRequest;
import org.aastrika.dto.request.PassbookReadRequest;
import org.aastrika.dto.request.PassbookUpdateRequest;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.service.PassbookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PassbookController {

    private final PassbookService passbookService;

    public PassbookController(PassbookService passbookService) {
        this.passbookService = passbookService;
    }

    /** Read the authenticated user's own passbook. */
    @PostMapping("/user/v1/passbook")
    public ResponseEntity<AppResponse<Map<String, Object>>> getUserPassbook(
            @RequestHeader(Constants.X_AUTH_USER_ID) String userId,
            @Valid @RequestBody PassbookReadRequest request) {
        return ResponseEntity.ok(passbookService.getPassbook(userId, request));
    }

    /** Admin read: passbooks of the users named in the request body. */
    @PostMapping("/admin/user/v1/passbook")
    public ResponseEntity<AppResponse<Map<String, Object>>> getPassbookByAdmin(
            @Valid @RequestBody AdminPassbookReadRequest request) {
        return ResponseEntity.ok(passbookService.getPassbookByAdmin(request));
    }

    /** Add passbook entries for the user named in the body; the header user is recorded as createdBy. */
    @PatchMapping("/user/v1/passbook")
    public ResponseEntity<AppResponse<Map<String, Object>>> updatePassbook(
            @RequestHeader(Constants.X_AUTH_USER_ID) String actingUserId,
            @Valid @RequestBody PassbookUpdateRequest request) {
        return ResponseEntity.ok(passbookService.updatePassbook(actingUserId, request));
    }
}

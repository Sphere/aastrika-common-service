package org.aastrika.controller;

import java.util.Map;

import jakarta.validation.Valid;
import org.aastrika.dto.request.LeaderboardRequest;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Leaderboard read API. Split into its own controller (the source repo bundled it into
 * PassbookController) because it is a distinct, Postgres-backed feature. The URL path is unchanged.
 */
@RestController
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @PostMapping("/user/v1/leaderboard")
    public ResponseEntity<AppResponse<Map<String, Object>>> getLeaderboard(
            @Valid @RequestBody LeaderboardRequest request) {
        return ResponseEntity.ok(leaderboardService.getLeaderboard(request));
    }
}

package org.aastrika.service;

import java.util.Map;

import org.aastrika.dto.request.LeaderboardRequest;
import org.aastrika.dto.response.AppResponse;

public interface LeaderboardService {

    /**
     * A ranked page of leaderboard users for the given filters, plus the active user's own rank.
     *
     * @throws org.aastrika.exception.ApiException 400 if a filter key is not a leaderboard column,
     *                                             a filter value is null, or no matching data exists
     */
    AppResponse<Map<String, Object>> getLeaderboard(LeaderboardRequest request);
}

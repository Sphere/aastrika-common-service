package org.aastrika.dto.response;

import java.util.List;

import org.aastrika.entity.LeaderboardEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Leaderboard read payload: the ranked page of users plus the active user's own ranked entry.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {

    private List<LeaderboardEntity> leaderboardList;
    private LeaderboardEntity activeUserDetails;
}

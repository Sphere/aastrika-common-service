package org.aastrika.service.impl;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.aastrika.dto.request.LeaderboardRequest;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.dto.response.LeaderboardResponse;
import org.aastrika.entity.LeaderboardEntity;
import org.aastrika.exception.ApiException;
import org.aastrika.repository.LeaderboardRepository;
import org.aastrika.service.LeaderboardService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.Column;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private static final String API_ID = "user.leaderboard.read";
    private static final String DATA_MISSING =
            "Some data is missing in leaderboard list or active user details";

    private final LeaderboardRepository leaderboardRepository;

    // Column names that may appear as filter keys — derived from the entity so filters can never
    // inject arbitrary SQL identifiers. Only mapped (@Column) fields are allowed; @Transient is out.
    private Set<String> allowedFilterColumns;

    public LeaderboardServiceImpl(LeaderboardRepository leaderboardRepository) {
        this.leaderboardRepository = leaderboardRepository;
    }

    @PostConstruct
    void init() {
        allowedFilterColumns = Arrays.stream(LeaderboardEntity.class.getDeclaredFields())
                .map(field -> field.getAnnotation(Column.class))
                .filter(Objects::nonNull)
                .map(Column::name)
                .collect(Collectors.toSet());
    }

    @Override
    public AppResponse<Map<String, Object>> getLeaderboard(LeaderboardRequest request) {
        validateFilters(request.getFilters());

        LeaderboardResponse content = buildResponse(request);
        int totalCount = leaderboardRepository
                .findAllUsersByDynamicFilters(request.getFilters(), null, null).size();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", totalCount);
        result.put("content", content);
        return AppResponse.success(API_ID, result, HttpStatus.OK);
    }

    private void validateFilters(Map<String, Object> filters) {
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            if (!allowedFilterColumns.contains(entry.getKey().toLowerCase())) {
                throw new ApiException(API_ID, HttpStatus.BAD_REQUEST,
                        "Invalid filter field: " + entry.getKey());
            }
            if (entry.getValue() == null) {
                throw new ApiException(API_ID, HttpStatus.BAD_REQUEST,
                        "Filter value cannot be empty for field: " + entry.getKey());
            }
        }
    }

    private LeaderboardResponse buildResponse(LeaderboardRequest request) {
        List<LeaderboardEntity> page = leaderboardRepository.findAllUsersByDynamicFilters(
                request.getFilters(), request.getLimit(), request.getOffset());
        if (page.isEmpty()) {
            throw new ApiException(API_ID, HttpStatus.BAD_REQUEST, DATA_MISSING);
        }

        int startRank = request.getOffset() + 1;
        for (int i = 0; i < page.size(); i++) {
            page.get(i).setRank(startRank + i);
        }

        LeaderboardEntity activeUser = leaderboardRepository
                .findUserByDynamicFilter(request.getUserId(), request.getFilters())
                .orElseThrow(() -> new ApiException(API_ID, HttpStatus.BAD_REQUEST, DATA_MISSING));
        activeUser.setRank(leaderboardRepository.findUserRank(activeUser, request.getFilters()) + 1);

        return new LeaderboardResponse(page, activeUser);
    }
}

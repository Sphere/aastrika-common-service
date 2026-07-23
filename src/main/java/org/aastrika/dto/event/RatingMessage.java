package org.aastrika.dto.event;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event emitted to the rating Kafka topic on a rating add/update. <b>Field names are intentionally
 * snake_case</b> ({@code activity_id}, {@code activity_Type}, {@code user_id}, {@code created_Date})
 * to match the wire format the external aggregation pipeline consumes — do not "clean up" these
 * names.
 */
@Data
@NoArgsConstructor
public class RatingMessage {

    private Integer version = 1;
    private String action = "ratingUpdate";
    private String activity_id;
    private String activity_Type;
    private String user_id;
    private String created_Date;
    private UpdatedValues prevValues;
    private UpdatedValues updatedValues;

    public RatingMessage(String action, String activity_id, String activity_Type, String user_id, String created_Date) {
        this.action = action;
        this.activity_id = activity_id;
        this.activity_Type = activity_Type;
        this.user_id = user_id;
        this.created_Date = created_Date;
    }

    @Data
    @NoArgsConstructor
    public static class UpdatedValues {
        private String updatedOn;
        private Float rating;
        private String review;
    }
}

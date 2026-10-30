package org.aastrika.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps {@code public.user_fcm_tokens} — one row per device. A device has exactly one current FCM
 * token, and an FCM token belongs to one device, so device_id is the primary key and fcm_token is unique.
 */
@Entity
@Table(name = "user_fcm_tokens")
@Getter
@Setter
@NoArgsConstructor
public class UserFcmToken {

    @Id
    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "fcm_token", nullable = false, unique = true)
    private String fcmToken;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
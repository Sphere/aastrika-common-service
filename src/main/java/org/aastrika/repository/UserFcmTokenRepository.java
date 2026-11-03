package org.aastrika.repository;

import java.util.Collection;
import java.util.List;
import org.aastrika.entity.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, String> {

    List<UserFcmToken> findByUserIdIn(Collection<String> userIds);

    @Transactional
    long deleteByUserIdAndDeviceId(String userId, String deviceId);

    @Transactional
    long deleteByFcmToken(String fcmToken);

    @Transactional
    long deleteByFcmTokenIn(Collection<String> fcmTokens);
}
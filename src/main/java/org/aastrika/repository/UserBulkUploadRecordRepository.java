package org.aastrika.repository;

import java.util.List;
import java.util.UUID;

import org.aastrika.entity.UserBulkUploadRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBulkUploadRecordRepository extends JpaRepository<UserBulkUploadRecord, Long> {

    /** All rows of a job in sheet order. Uses the (jobid, rownumber) unique index. */
    List<UserBulkUploadRecord> findByJobIdOrderByRowNumberAsc(UUID jobId);

    /** The work remaining for a job — this is the resume state, no cursor needed. */
    List<UserBulkUploadRecord> findByJobIdAndStatusOrderByRowNumberAsc(UUID jobId, String status);

    List<UserBulkUploadRecord> findByJobIdAndStatusInOrderByRowNumberAsc(UUID jobId, List<String> statuses);

    /** Per-status counts, derived rather than maintained as columns on the job row. */
    @Query("SELECT r.status, COUNT(r) FROM UserBulkUploadRecord r WHERE r.jobId = :jobId GROUP BY r.status")
    List<Object[]> countByStatus(@Param("jobId") UUID jobId);

    /**
     * Per-status counts for several jobs at once — one query rather than one per job, so listing an
     * org's history does not N+1.
     */
    @Query("SELECT r.jobId, r.status, COUNT(r) FROM UserBulkUploadRecord r "
            + "WHERE r.jobId IN :jobIds GROUP BY r.jobId, r.status")
    List<Object[]> countByJobIdAndStatus(@Param("jobIds") List<UUID> jobIds);
}

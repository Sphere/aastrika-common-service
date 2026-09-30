package org.aastrika.repository;

import java.util.List;
import java.util.UUID;

import org.aastrika.entity.UserBulkUploadJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBulkUploadJobRepository extends JpaRepository<UserBulkUploadJob, UUID> {

    /** An org's upload history, newest first. */
    List<UserBulkUploadJob> findByRootOrgIdOrderByCreatedAtDesc(String rootOrgId);

    /** Unfinished jobs, for the resume sweep. Backed by idx_bulk_job_status. */
    List<UserBulkUploadJob> findByStatusIn(List<String> statuses);
}

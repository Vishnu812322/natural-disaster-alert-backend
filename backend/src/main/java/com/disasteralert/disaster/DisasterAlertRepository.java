package com.disasteralert.disaster;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DisasterAlertRepository extends JpaRepository<DisasterAlert, Long> {

    // Get all alerts, newest first
    List<DisasterAlert> findAllByOrderByCreatedAtDesc();

    // Get alerts by status, newest first
    List<DisasterAlert> findByStatusOrderByCreatedAtDesc(String status);

    // Check whether a government source alert has already been imported
    boolean existsBySourceAndSourceAlertId(
            String source,
            String sourceAlertId
    );

    // Find an existing government alert by source + source alert ID
    Optional<DisasterAlert> findBySourceAndSourceAlertId(
            String source,
            String sourceAlertId
    );
}
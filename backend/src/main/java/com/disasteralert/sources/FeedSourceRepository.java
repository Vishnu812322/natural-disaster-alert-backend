package com.disasteralert.sources;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedSourceRepository extends JpaRepository<FeedSource, Long> {
    Optional<FeedSource> findByCode(String code);
}

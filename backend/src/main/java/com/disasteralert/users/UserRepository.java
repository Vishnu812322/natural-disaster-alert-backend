package com.disasteralert.users;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    long countByEnabledTrue();

    List<User> findAllByOrderByCreatedAtDesc();

    List<User> findByEnabledTrueAndLatitudeIsNotNullAndLongitudeIsNotNull();
}
package com.dips.simulator.repository;

import com.dips.simulator.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByVpa(String vpa);
}


package com.dips.simulator.repository;

import com.dips.simulator.domain.ExecutionQueueEntity;
import com.dips.simulator.domain.enums.ExecutionQueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExecutionQueueRepository extends JpaRepository<ExecutionQueueEntity, UUID> {
    long countByQueueStatus(ExecutionQueueStatus queueStatus);
}


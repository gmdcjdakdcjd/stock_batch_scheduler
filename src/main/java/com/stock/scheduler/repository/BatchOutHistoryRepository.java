package com.stock.scheduler.repository;

import com.stock.scheduler.entity.BatchOutHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchOutHistoryRepository extends JpaRepository<BatchOutHistory, Long> {
}

package com.stock.scheduler.repository;

import com.stock.scheduler.entity.BatchInHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchInHistoryRepository extends JpaRepository<BatchInHistory, Long> {
}

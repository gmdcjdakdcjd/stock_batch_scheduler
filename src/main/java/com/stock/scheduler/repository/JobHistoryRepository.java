/*
package com.stock.scheduler.repository;

import com.stock.scheduler.entity.JobHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobHistoryRepository extends JpaRepository<JobHistory, Integer> {

    // ✅ 날짜 기준으로 조회 (start_time 기준)
    @Query("SELECT j FROM JobHistory j WHERE DATE(j.startTime) = :date ORDER BY j.startTime ASC")
    List<JobHistory> findByDate(@Param("date") LocalDate date);

    // ✅ 특정 jobName에 대해 최신(endTime 기준 DESC) 1건 조회
    Optional<JobHistory> findTopByJobNameOrderByEndTimeDesc(String jobName);

    @Query("""
       SELECT j FROM JobHistory j
       WHERE j.startTime BETWEEN :start AND :end
       ORDER BY j.startTime ASC
       """)
    List<JobHistory> findByDateBetween(
            @Param("start") java.sql.Timestamp start,
            @Param("end") java.sql.Timestamp end);

}
*/

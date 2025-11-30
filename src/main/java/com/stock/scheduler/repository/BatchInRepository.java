package com.stock.scheduler.repository;

import com.stock.scheduler.entity.BatchIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BatchInRepository extends JpaRepository<BatchIn, Integer> {

    @Query("""
            SELECT j FROM BatchIn j
            WHERE j.isActive = 1
              AND j.actGb = 'N'
              AND (
                     (j.scheduleGb = 'D')
        
                  OR (j.scheduleGb = 'M'
                      AND (j.jobMonth IS NULL OR j.jobMonth = :todayMonth)
                      AND j.jobDay = :todayDay)
        
                  OR (j.scheduleGb = 'W'
                      AND j.jobWeek = :todayWeek)
              )
              AND j.jobHour = :nowHour
              AND j.jobMin = :nowMin
              ORDER BY j.jobId
            """)
    List<BatchIn> findExecutableJobs(
            @Param("todayMonth") String todayMonth,
            @Param("todayDay")   String todayDay,
            @Param("todayWeek")  String todayWeek,
            @Param("nowHour")    String nowHour,
            @Param("nowMin")     String nowMin
    );

}

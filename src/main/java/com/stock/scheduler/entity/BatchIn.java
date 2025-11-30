package com.stock.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Entity
@Table(name = "batch_in")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Integer jobId;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "job_info", length = 255)
    private String jobInfo;

    @Column(name = "schedule_gb", nullable = false, length = 1)
    private String scheduleGb;

    @Column(name = "job_month", length = 2)
    private String jobMonth;

    @Column(name = "job_day", length = 2)
    private String jobDay;

    @Column(name = "job_week", length = 3)
    private String jobWeek;

    @Column(name = "job_hour", length = 4)
    private String jobHour;

    @Column(name = "job_min", length = 4)
    private String jobMin;

    @Column(name = "act_gb", nullable = false, length = 1)
    private String actGb;

    @Column(name = "last_exec_info", length = 50)
    private String lastExecInfo;

    @Column(name = "next_exec_info", length = 50)
    private String nextExecInfo;

    @Column(name = "file_pattern", nullable = false, length = 255)
    private String filePattern;

    @Column(name = "is_active")
    private Integer isActive;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;
}

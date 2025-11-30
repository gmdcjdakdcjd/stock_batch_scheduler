package com.stock.scheduler.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.sql.Timestamp;

@Entity
@Table(name = "batch_out")
@Data
public class BatchOut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Integer jobId;

    @Column(name = "job_name")
    private String jobName;

    @Column(name = "job_info")
    private String jobInfo;

    @Column(name = "schedule_gb")
    private String scheduleGb;   // D=daily, W=weekly, M=monthly

    @Column(name = "job_month")
    private String jobMonth;     // 월 배치 (01~12)

    @Column(name = "job_day")
    private String jobDay;       // 일 배치 (01~31)

    @Column(name = "job_week")
    private String jobWeek;      // Weekly 배치 (MON/TUE/WED/THU/FRI/SAT/SUN)

    @Column(name = "job_hour")
    private String jobHour;      // 실행 시간 (HH)

    @Column(name = "job_min")
    private String jobMin;       // 실행 분 (MM)

    @Column(name = "act_gb")
    private String actGb;        // N / Y

    @Column(name = "last_exec_info")
    private String lastExecInfo; // 마지막 실행일 (yyyy-MM-dd)

    @Column(name = "next_exec_info")
    private String nextExecInfo; // 다음 실행 예정일 (yyyy-MM-dd)

    @Column(name = "shell_file_dir")
    private String shellFileDir; // 배치 실행 파일 경로

    @Column(name = "is_active")
    private Integer isActive;    // 사용 여부

    @Column(name = "created_at")
    private Timestamp createdAt; // 생성일
}

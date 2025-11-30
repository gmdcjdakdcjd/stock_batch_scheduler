/*
package com.stock.scheduler.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(name = "job_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String jobName;
    private String status;
    private int rowCount;
    private String errorMsg;
    private Timestamp startTime;
    private Timestamp endTime;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;

    @Column(name = "duration_sec")
    private int durationSec;

    @Column(name = "code_count")
    private int codeCount;

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = (errorMsg == null || errorMsg.isBlank()) ? "NO_ERROR" : errorMsg;
    }
}


*/

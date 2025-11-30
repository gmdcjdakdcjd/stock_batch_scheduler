package com.stock.scheduler.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.sql.*;

@Entity
@Table(name = "batch_out_h")
@Data
public class BatchOutHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long histId;

    private Integer jobId;
    private String jobName;
    private String jobInfo;

    private Timestamp execStartTime;
    private Timestamp execEndTime;
    private String execStatus;
    private String execMessage;

    private Date execDate;
    private Long durationMs;
}

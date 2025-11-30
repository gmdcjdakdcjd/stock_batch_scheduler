package com.stock.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.*;

@Entity
@Table(name = "batch_in_h")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BatchInHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long histId;

    private Integer jobId;

    private String jobName;
    private String jobInfo;

    private Timestamp execStartTime;
    private Timestamp execEndTime;

    private String execStatus;     // SUCCESS / FAIL
    private String execMessage;

    private Date execDate;
    private Long durationMs;
}

package com.stock.scheduler.job;

import com.stock.scheduler.entity.JobHistory;
import com.stock.scheduler.repository.JobHistoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Component
public class BatchScheduler {
    private final PythonBatchJob pythonBatchJob;
    private final JobHistoryRepository jobHistoryRepository;

    public BatchScheduler(PythonBatchJob pythonBatchJob, JobHistoryRepository jobHistoryRepository) {
        this.pythonBatchJob = pythonBatchJob;
        this.jobHistoryRepository = jobHistoryRepository;
    }

    // 공통 실행 메서드
    private void runJob(String jobName, String scriptPath) {
        long start = System.currentTimeMillis();

        PythonJobResult result = pythonBatchJob.runPythonScript(scriptPath);

        long end = System.currentTimeMillis();

        long durationSec = (end - start) / 1000;  // 밀리초 → 초 변환

        JobHistory history = new JobHistory(
                null,
                jobName,
                result.getStatus(),
                result.getRowCount(),
                result.getErrorMsg(),
                new Timestamp(start),
                new Timestamp(end),
                null,
                (int) durationSec,
                result.getCodeCount()
        );

        history.setDurationSec((int) durationSec);

        jobHistoryRepository.save(history);

        System.out.printf("[%s] Job '%s' finished with status=%s, rowCount=%d%n",
                new Timestamp(end), jobName, result.getStatus(), result.getRowCount());
    }

    // 매일 22시에 두 잡 실행
    @Scheduled(cron = "0 30 22 * * *")
    public void runDailyJobs() {
        runJob("DBUpdater",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\StockList\\DBUpdater.py");
    }


    // 매일 22시에 두 잡 실행
    @Scheduled(cron = "0 35 22 * * *")
    public void runETFDailyJobs() {
        runJob("ETFDBUpdater",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\StockList\\ETFDBUpdater.py");
    }
}

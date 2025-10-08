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
    @Scheduled(cron = "0 30 19 * * *")
    public void runDailyJobs() {
        runJob("DBUpdater",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\StockList\\DBUpdater.py");
    }

    @Scheduled(cron = "0 35 19 * * *")
    public void runBollingerReversalJob() {
        runJob("BollingerBand_Reversal",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy\\BollingerBand_Reversals.py");
    }

    @Scheduled(cron = "0 40 19 * * *")
    public void runBollingerTrendFollowingJob() {
        runJob("BollingerBand_TrendFollowing",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy\\BollingerBand_TrendFollowing.py");
    }

    @Scheduled(cron = "0 45 19 * * *")
    public void runTripleScreenJob() {
        runJob("TripleScreen",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy\\TripleScreen.py");
    }


    @Scheduled(cron = "0 50 19 * * *")
    public void runDualMomentumJob() {
        runJob("DualMomentum",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy\\DualMomentumBatch.py");
    }




    // 매일 22시에 두 잡 실행
    @Scheduled(cron = "0 00 20 * * *")
    public void runETFDailyJobs() {
        runJob("ETFDBUpdater",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\StockList\\ETFDBUpdater.py");
    }


    @Scheduled(cron = "0 05 20 * * *")
    public void runETFBollingerReversalJob() {
        runJob("ETF_BollingerBand_Reversal",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\ETFTradingStrategy\\BollingerBand_Reversals.py");
    }

    @Scheduled(cron = "0 10 20 * * *")
    public void runETFBollingerTrendFollowingJob() {
        runJob("ETF_BollingerBand_TrendFollowing",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\ETFTradingStrategy\\BollingerBand_TrendFollowing.py");
    }

    @Scheduled(cron = "0 15 20 * * *")
    public void runETFTripleScreenJob() {
        runJob("ETF_TripleScreen",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\ETFTradingStrategy\\TripleScreen.py");
    }

    @Scheduled(cron = "0 20 20 * * *")
    public void runETFDualMomentumJob() {
        runJob("ETF_DualMomentum",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\ETFTradingStrategy\\DualMomentumBatch.py");
    }


}

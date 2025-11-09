package com.stock.scheduler.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.scheduler.entity.JobHistory;
import com.stock.scheduler.entity.SignalRecord;
import com.stock.scheduler.job.markdown.*;
// import com.stock.scheduler.job.markdown.MarkdownGenerator;
import com.stock.scheduler.repository.JobHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class BatchScheduler {

    @Autowired
    private MarkdownDashboardGenerator dashboardGenerator;

    private final PythonBatchJob pythonBatchJob;
    private final JobHistoryRepository jobHistoryRepository;

    public BatchScheduler(PythonBatchJob pythonBatchJob, JobHistoryRepository jobHistoryRepository) {
        this.pythonBatchJob = pythonBatchJob;
        this.jobHistoryRepository = jobHistoryRepository;
    }

    // ====================================================
    // ✅ (1) 개별 잡 실행 (기존 그대로)
    // ====================================================
    private void runJob(String jobName, String scriptPath, boolean autoPost) {
        long start = System.currentTimeMillis();
        PythonJobResult result = pythonBatchJob.runPythonScript(scriptPath);
        long end = System.currentTimeMillis();

        JobHistory history = new JobHistory(
                null, jobName, result.getStatus(), result.getRowCount(), result.getErrorMsg(),
                new Timestamp(start), new Timestamp(end), null, (int) ((end - start) / 1000), result.getCodeCount()
        );
        jobHistoryRepository.save(history);

        if (!autoPost) return;

        try {
            Long resultId = result.getResultId();
            if (resultId == null) {
                System.err.println("⚠️ resultId가 null입니다. Python 로그에서 RESULT_ID= 누락됨.");
                return;
            }

            // ✅ DB에서 시그널 불러오기
            List<SignalRecord> signals = pythonBatchJob.fetchSignalsFromDB(resultId);

            // ✅ 전략별 Markdown Generator 선택
            MarkdownTemplate generator;

            switch (jobName) {
                case "RISE_SPIKE", "DROP_SPIKE", "Stock_Volume_Batch", "ETF_Volume_Batch", "DualMomentumBatch_20",
                     "DualMomentumBatch_60", "DualMomentumBatch_180", "DualMomentumBatch_365",
                     "RISE_SPIKE_US", "DROP_SPIKE_US", "Stock_Volume_Batch_US", "ETF_Volume_Batch_US",
                     "DualMomentumBatch_20_US",
                     "DualMomentumBatch_60_US", "DualMomentumBatch_180_US", "DualMomentumBatch_365_US" -> {
                    generator = new MarkdownSimpleGenerator(); // 표만 출력
                }
                default -> {
                    generator = new MarkdownDetailGenerator(); // 실행 정보 + 표 출력
                }
            }

            // ✅ Markdown 생성
            String markdown = generator.generate(jobName, signals, new Timestamp(start), new Timestamp(end));

            // ✅ 게시글 데이터 구성
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            Map<String, Object> board = new HashMap<>();
            board.put("title", String.format("[자동등록] %s 결과 보고 (%s)", jobName, today));
            board.put("content", markdown);
            board.put("writer", "system");
            board.put("boardGb", resolveBoardGb(jobName));

            // ✅ 게시글 업로드
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.postForObject("http://localhost:8090/api/board/auto", board, String.class);

            System.out.printf("✅ 게시판 자동 등록 완료: %s (result_id=%d, signals=%d)\n",
                    board.get("title"), resultId, signals.size());
        } catch (Exception e) {
            System.err.println("❌ 게시판 자동 등록 실패: " + e.getMessage());
        }

        System.out.printf("[%s] Job '%s' finished with status=%s, rowCount=%d, codeCount=%d%n",
                new Timestamp(end), jobName, result.getStatus(), result.getRowCount(), result.getCodeCount());
    }


    private String resolveBoardGb(String jobName) {
        return switch (jobName) {
            case "RISE_SPIKE" -> "1";
            case "DROP_SPIKE" -> "2";
            case "Stock_Volume_Batch" -> "3";
            case "ETF_Volume_Batch" -> "4";

//          case "DualMomentumBatch_7" -> "5";
            case "DualMomentumBatch_20" -> "6";
            case "DualMomentumBatch_60" -> "7";
            case "DualMomentumBatch_180" -> "8";
            case "DualMomentumBatch_365" -> "9";

            case "RSI_70_SELL" -> "11";
            case "RSI_30_UNHEATED" -> "12";
            case "HIGH_52" -> "13";
            case "LOW_52" -> "14";
            case "HIGH_120" -> "15";
            case "LOW_120" -> "16";
            case "TouchCandidatesTop" -> "17";
            case "TouchCandidatesBottom" -> "18";
            case "MovingAreaByWeek" -> "19";
            case "MovingAverageByDay" -> "20";

            case "RSI_70_SELL_US" -> "31";
            case "RSI_30_UNHEATED_US" -> "32";
            case "HIGH_52_US" -> "33";
            case "LOW_52_US" -> "34";
            case "HIGH_120_US" -> "35";
            case "LOW_120_US" -> "36";
            case "TouchCandidatesTop_US" -> "37";
            case "TouchCandidatesBottom_US" -> "38";
            case "MovingAreaByWeek_US" -> "39";
            case "MovingAverageByDay_US" -> "40";

            case "RISE_SPIKE_US" -> "41";
            case "DROP_SPIKE_US" -> "42";
            case "Stock_Volume_Batch_US" -> "43";
            case "ETF_Volume_Batch_US" -> "44";

//          case "DualMomentumBatch_7" -> "5";
            case "DualMomentumBatch_20_US" -> "46";
            case "DualMomentumBatch_60_US" -> "47";
            case "DualMomentumBatch_180_US" -> "48";
            case "DualMomentumBatch_365_US" -> "49";

            default -> null;
        };
    }

    // ====================================================
    // ✅ 추가된 통합 실행 메서드 👇
    // ====================================================
    // ✅ (3) 전체 배치 통합 실행 (스케줄 한 번만)
    // ====================================================
    @Scheduled(cron = "0 24 11 * * *", zone = "Asia/Seoul") // ✅ 매일 22시에 전부 순차 실행
    public void runAllBatches_KR() {
        System.out.println("🚀 [통합 배치 시작] 모든 전략 순차 실행\n");

        // 1️⃣ StockList 업데이트
        runJob("DBUpdater",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\StockList\\DBUpdater.py",
                false);
        runJob("ETFDBUpdater",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\StockList\\ETFDBUpdater.py",
                false);

        // 2️⃣ 거래량 그룹
        runJob("RISE_SPIKE",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\RISE_SPIKE.py",
                true);
        runJob("DROP_SPIKE",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\DROP_SPIKE.py",
                true);

        // 3️⃣ 스파이크 그룹
        runJob("Stock_Volume_Batch",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\Stock_Volume_Batch.py",
                true);
        runJob("ETF_Volume_Batch",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\ETF_Volume_Batch.py",
                true);


        // 4️⃣ 듀얼모멘텀
        runJob("DualMomentumBatch_20",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\DualMomentumBatch_20.py",
                true);
        runJob("DualMomentumBatch_60",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\DualMomentumBatch_60.py",
                true);
        runJob("DualMomentumBatch_180",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\DualMomentumBatch_180.py",
                true);
        runJob("DualMomentumBatch_365",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\DualMomentumBatch_365.py",
                true);

        // 5️⃣ 52주/120일 고저점
        runJob("HIGH_52",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\HIGH_52.py",
                true);
        runJob("LOW_52",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\LOW_52.py",
                true);
        runJob("HIGH_120",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\HIGH_120.py",
                true);
        runJob("LOW_120",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\LOW_120.py",
                true);


        // 6️⃣ 이동평균 + 볼린저 밴드
        runJob("MovingAreaByWeek",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\MovingAreaByWeek.py",
                true);
        runJob("MovingAverageByDay",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\MovingAverageByDay.py",
                true);
        runJob("TouchCandidatesBottom",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\TouchCandidatesBottom.py",
                true);
        runJob("TouchCandidatesTop",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\TouchCandidatesTop.py",
                true);

        runJob("RSI_70_SELL",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\RSI_70.py",
                true);
        runJob("RSI_30_UNHEATED",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch\\RSI_30.py",
                true);

        System.out.println("🎯 [통합 배치 완료]");


        // ✅ 통합 리포트 (PDF + 게시글)
        // ✅ 통합 리포트 (PDF + 게시글)
        generateAndUploadDailyReport("KR");
    }

    @Scheduled(cron = "0 04 14 * * *", zone = "Asia/Seoul") // ✅ 매일 22시에 전부 순차 실행
    public void runAllBatches_US() {
        System.out.println("🚀 [미국 통합 배치 시작] 모든 전략 순차 실행\n");

        // 1️⃣ StockList 업데이트
/*        runJob("DBUpdater_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\StockList\\USDBUpdater.py",
                false);
        runJob("ETFDBUpdater_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\StockList\\USETFDBUPdater.py",
                false);*/

        // 2️⃣ 거래량 그룹
        runJob("RISE_SPIKE_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\RISE_SPIKE_US.py",
                true);
        runJob("DROP_SPIKE_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\DROP_SPIKE_US.py",
                true);

        // 3️⃣ 스파이크 그룹
        runJob("Stock_Volume_Batch_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\Stock_Volume_Batch_US.py",
                true);
        runJob("ETF_Volume_Batch_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\ETF_Volume_Batch_US.py",
                true);


        // 4️⃣ 듀얼모멘텀
        runJob("DualMomentumBatch_20_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\DualMomentumBatch_20_US.py",
                true);
        runJob("DualMomentumBatch_60_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\DualMomentumBatch_60_US.py",
                true);
        runJob("DualMomentumBatch_180_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\DualMomentumBatch_180_US.py",
                true);
        runJob("DualMomentumBatch_365_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\DualMomentumBatch_365_US.py",
                true);

        // 5️⃣ 52주/120일 고저점
        runJob("HIGH_52_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\HIGH_52_US.py",
                true);
        runJob("LOW_52_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\LOW_52_US.py",
                true);
        runJob("HIGH_120_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\HIGH_120_US.py",
                true);
        runJob("LOW_120_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\LOW_120_US.py",
                true);


        // 6️⃣ 이동평균 + 볼린저 밴드
        runJob("MovingAreaByWeek_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\MovingAreaByWeek_US.py",
                true);
        runJob("MovingAverageByDay_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\MovingAverageByDay_US.py",
                true);
        runJob("TouchCandidatesBottom_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\TouchCandidatesBottom_US.py",
                true);
        runJob("TouchCandidatesTop_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\TouchCandidatesTop_US.py",
                true);

        runJob("RSI_70_SELL_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\RSI_70_US.py",
                true);
        runJob("RSI_30_UNHEATED_US",
                "D:\\STOCK_PROJECT\\python_stock_batch\\batch_code\\trading\\TradingStrategy_Batch_US\\RSI_30_US.py",
                true);

        System.out.println("🎯 [통합 배치 완료]");


        // ✅ 통합 리포트 (PDF + 게시글)
        // ✅ 통합 리포트 (PDF + 게시글)
        generateAndUploadDailyReport("US");

    }


    private void generateAndUploadDailyReport(String region) {
        try {
            MarkdownDetailGenerator detailGenerator = new MarkdownDetailGenerator();
            StringBuilder fullMarkdownBuilder = new StringBuilder();
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            List<String> jobNames = jobHistoryRepository.findByDate(today)
                    .stream()
                    .filter(j -> j.getJobName() != null && !j.getJobName().isBlank())
                    .map(JobHistory::getJobName)
                    .distinct()
                    .toList();

            String summaryMarkdown = dashboardGenerator.generate(today);

            fullMarkdownBuilder.append("# 📊 ")
                    .append(region.equals("KR") ? "국내" : "미국")
                    .append(" 일일 통합 배치 리포트\n\n")
                    .append("## 📅 전체 실행 요약\n\n")
                    .append(summaryMarkdown)
                    .append("\n\n---\n\n## ⚙️ 전략별 상세 결과\n\n");

            for (String jobName : jobNames) {
                String mappedName = resolveStrategyName(jobName);
                List<SignalRecord> signals = pythonBatchJob.fetchSignalsFromDBByJob(mappedName, today);
                if (signals.isEmpty()) signals = pythonBatchJob.fetchSignalsFromDBByJob(mappedName, yesterday);
                if (signals.isEmpty()) continue;

                Optional<JobHistory> optHistory = jobHistoryRepository.findTopByJobNameOrderByEndTimeDesc(jobName);
                JobHistory history = optHistory.orElse(null);
                Timestamp startTime = history != null ? history.getStartTime() : new Timestamp(System.currentTimeMillis());
                Timestamp endTime = history != null ? history.getEndTime() : new Timestamp(System.currentTimeMillis());

                String section = detailGenerator.generate(jobName, signals, startTime, endTime);
                fullMarkdownBuilder.append(section).append("\n\n---\n\n");
            }

            File pdfFile = PdfGenerator.generatePdfFromMarkdown(fullMarkdownBuilder.toString(),
                    String.format("%s_Batch_Report_%s", region, today));

            Map<String, Object> board = new HashMap<>();
            board.put("title", String.format("[자동등록][%s] %s 전체 배치 리포트", region, today));
            board.put("content", summaryMarkdown);
            board.put("writer", "system");
            board.put("boardGb", "99");

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("board", new ObjectMapper().writeValueAsString(board));
            body.add("file", new FileSystemResource(pdfFile));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            new RestTemplate().postForObject("http://localhost:8090/api/board/auto-with-file",
                    new HttpEntity<>(body, headers), String.class);

            System.out.println("✅ " + region + " 리포트 게시글 등록 완료");

        } catch (Exception e) {
            System.err.println("❌ " + region + " 리포트 생성 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private String resolveStrategyName(String jobName) {
        return switch (jobName) {
            // 1️⃣ 거래량 / 스파이크
            case "RISE_SPIKE" -> "DAILY_RISE_SPIKE";
            case "DROP_SPIKE" -> "DAILY_DROP_SPIKE";
            case "Stock_Volume_Batch" -> "DAILY_TOP20_VOLUME";
            case "ETF_Volume_Batch" -> "ETF_TOP20_VOLUME";

            case "RISE_SPIKE_US" -> "DAILY_RISE_SPIKE_US";
            case "DROP_SPIKE_US" -> "DAILY_DROP_SPIKE_US";
            case "Stock_Volume_Batch_US" -> "DAILY_TOP20_VOLUME_US";
            case "ETF_Volume_Batch_US" -> "ETF_TOP20_VOLUME_US";

            // 2️⃣ 듀얼모멘텀 (기간별)
            case "DualMomentumBatch_20" -> "DUAL_MOMENTUM_1M";
            case "DualMomentumBatch_60" -> "DUAL_MOMENTUM_3M";
            case "DualMomentumBatch_180" -> "DUAL_MOMENTUM_6M";
            case "DualMomentumBatch_365" -> "DUAL_MOMENTUM_1Y";

            case "DualMomentumBatch_20_US" -> "DUAL_MOMENTUM_1M_US";
            case "DualMomentumBatch_60_US" -> "DUAL_MOMENTUM_3M_US";
            case "DualMomentumBatch_180_US" -> "DUAL_MOMENTUM_6M_US";
            case "DualMomentumBatch_365_US" -> "DUAL_MOMENTUM_1Y_US";

            // 3️⃣ 52주 / 120일 고저점
            case "HIGH_52" -> "WEEKLY_52W_NEW_HIGH";
            case "LOW_52" -> "WEEKLY_52W_NEW_LOW";
            case "HIGH_120" -> "DAILY_120D_NEW_HIGH";
            case "LOW_120" -> "DAILY_120D_NEW_LOW";

            case "HIGH_52_US" -> "WEEKLY_52W_NEW_HIGH_US";
            case "LOW_52_US" -> "WEEKLY_52W_NEW_LOW_US";
            case "HIGH_120_US" -> "DAILY_120D_NEW_HIGH_US";
            case "LOW_120_US" -> "DAILY_120D_NEW_LOW_US";

            // 4️⃣ 이동평균/터치 전략
            case "TouchCandidatesTop" -> "DAILY_BB_UPPER_TOUCH";
            case "TouchCandidatesBottom" -> "DAILY_BB_LOWER_TOUCH";
            case "MovingAverageByDay" -> "DAILY_TOUCH_MA60";
            case "MovingAreaByWeek" -> "WEEKLY_TOUCH_MA60";

            case "TouchCandidatesTop_US" -> "DAILY_BB_UPPER_TOUCH_US";
            case "TouchCandidatesBottom_US" -> "DAILY_BB_LOWER_TOUCH_US";
            case "MovingAverageByDay_US" -> "DAILY_TOUCH_MA60_US";
            case "MovingAreaByWeek_US" -> "WEEKLY_TOUCH_MA60_US";


            case "RSI_70_SELL" -> "RSI_70_SELL";
            case "RSI_30_UNHEATED" -> "RSI_30_UNHEATED";

            case "RSI_70_SELL_US" -> "RSI_70_SELL_US";
            case "RSI_30_UNHEATED_US" -> "RSI_30_UNHEATED_US";

            // 기본값
            default -> jobName;
        };
    }

}


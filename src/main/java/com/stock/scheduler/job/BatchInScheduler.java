package com.stock.scheduler.job;

import com.stock.scheduler.entity.BatchIn;
import com.stock.scheduler.entity.BatchInHistory;
import com.stock.scheduler.repository.BatchInHistoryRepository;
import com.stock.scheduler.repository.BatchInRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchInScheduler implements ApplicationRunner {

    private final BatchInRepository batchInRepository;
    private final BatchInHistoryRepository historyRepository;
    private final BatchInProcessor processor;

    // ============================================
    // 🔥 1) 서버 시작 시 act_gb 초기화
    // ============================================
    @Override
    public void run(ApplicationArguments args) {

        String today = LocalDate.now().toString();

        List<BatchIn> jobs = batchInRepository.findAll();

        for (BatchIn job : jobs) {
            if (job.getLastExecInfo() == null ||
                    !today.equals(job.getLastExecInfo())) {
                job.setActGb("N");
            }
        }

        batchInRepository.saveAll(jobs);
        log.info("🚀 [BatchIn] 서버 시작 초기화 완료: act_gb 리셋 완료");
    }


    // ============================================
    // 🔥 2) 1분마다 배치 실행 체크
    // ============================================
    @Scheduled(fixedDelay = 60000)
    public void checkAndRun() {

        LocalDate now = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        String today = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String todayMonth = today.substring(4, 6);
        String todayDay   = today.substring(6, 8);
        String todayWeek  = String.valueOf(now.getDayOfWeek().getValue());

        String nowHour = nowTime.format(DateTimeFormatter.ofPattern("HH"));
        String nowMin  = nowTime.format(DateTimeFormatter.ofPattern("mm"));

        List<BatchIn> jobs = batchInRepository.findExecutableJobs(
                todayMonth, todayDay, todayWeek, nowHour, nowMin
        );

        if (jobs.isEmpty()) return;

        jobs.forEach(job -> runJob(job, today));
    }


    // ============================================
    // 🔥 3) next_exec_info 계산
    // ============================================
    private void updateNextExecInfo(BatchIn job) {

        LocalDate today = LocalDate.now();
        LocalDate next;

        switch (job.getScheduleGb()) {

            case "M": // --- Monthly ---
                int day = Integer.parseInt(job.getJobDay());
                LocalDate firstDay = LocalDate.of(today.getYear(), today.getMonthValue(), 1);
                LocalDate targetDay = firstDay.withDayOfMonth(day);

                next = today.isBefore(targetDay)
                        ? targetDay
                        : firstDay.plusMonths(1).withDayOfMonth(day);
                break;

            case "W": // --- Weekly ---
                int targetDow = Integer.parseInt(job.getJobWeek());
                next = today.plusDays(1);
                while (next.getDayOfWeek().getValue() != targetDow) {
                    next = next.plusDays(1);
                }
                break;

            default: // --- Daily ---
                next = today.plusDays(1);
        }

        job.setNextExecInfo(next.toString());
    }


    // ============================================
    // 🔥 4) BatchIn Job 실행
    // ============================================
    private void runJob(BatchIn job, String today) {

        long start = System.currentTimeMillis();
        String status = "SUCCESS";
        String message = null;

        try {
            if ("FOLDER_MOVE".equals(job.getFilePattern())) {
                // 🔥 폴더 이동 전용 job
                processor.moveTodayFolder(today);

            } else {
                // 🔥 file_pattern 기반 JSON insert
                List<Path> files = processor.getFiles(today, job.getFilePattern());

                if (files.isEmpty()) {
                    status  = "FAIL";
                    message = "No files for pattern: " + job.getFilePattern();
                } else {
                    for (Path f : files) {
                        processor.processFile(f);
                    }
                }
            }

        } catch (Exception e) {
            status = "FAIL";
            message = e.toString();
            log.error("[BatchIn] {} 실행 실패", job.getJobName(), e);
        }

        long end = System.currentTimeMillis();

        // --- history 저장 ---
        saveHistory(job, start, end, status, message);

        // --- 상태 업데이트 ---
        job.setActGb("Y");
        job.setLastExecInfo(LocalDate.now().toString());
        updateNextExecInfo(job);

        batchInRepository.save(job);

        log.info("✔ [BatchIn] {} 완료 → next_exec={}", job.getJobName(), job.getNextExecInfo());
    }


    private void saveHistory(BatchIn job, long start, long end, String status, String message) {
        BatchInHistory hist = BatchInHistory.builder()
                .jobId(job.getJobId())
                .jobName(job.getJobName())
                .jobInfo(job.getJobInfo())
                .execStartTime(new Timestamp(start))
                .execEndTime(new Timestamp(end))
                .execStatus(status)
                .execMessage(message)
                .execDate(java.sql.Date.valueOf(LocalDate.now()))
                .durationMs(end - start)
                .build();

        historyRepository.save(hist);
    }


    // ============================================
    // 🔥 5) 자정 act_gb 리셋
    // ============================================
    @Scheduled(cron = "0 0 0 * * *")
    public void resetActGbAtMidnight() {

        List<BatchIn> jobs = batchInRepository.findAll();
        jobs.forEach(j -> j.setActGb("N"));
        batchInRepository.saveAll(jobs);

        log.info("🌙 [BatchIn] 자정 리셋 완료");
    }
}

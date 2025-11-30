package com.stock.scheduler.job;

import com.stock.scheduler.entity.BatchOut;
import com.stock.scheduler.entity.BatchOutHistory;
import com.stock.scheduler.repository.BatchOutHistoryRepository;
import com.stock.scheduler.repository.BatchOutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BatchOutScheduler implements ApplicationRunner {

    private final BatchOutRepository batchJobRepository;
    private final BatchOutHistoryRepository historyRepository;
    private final PythonBatchJob pythonBatchJob;

    // ============================================
    // 서버 시작 시 실행 → 오늘 기준 act_gb 초기화
    // ============================================
    @Override
    public void run(ApplicationArguments args) {

        String today = LocalDate.now().toString();

        List<BatchOut> jobs = batchJobRepository.findAll();

        for (BatchOut job : jobs) {
            if (job.getLastExecInfo() == null ||
                    !job.getLastExecInfo().equals(today)) {
                job.setActGb("N");
            }
        }

        batchJobRepository.saveAll(jobs);
        System.out.println("🚀 서버 시작 초기화 완료: act_gb = 'N'");
    }

    // ============================================
    // 1분마다 실행 (Daily + Monthly + Weekly)
    // ============================================
    @Scheduled(fixedDelay = 60000)
    public void checkAndRun() {

        String todayMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("MM"));
        String todayDay   = LocalDate.now().format(DateTimeFormatter.ofPattern("dd"));
        String todayWeek  = LocalDate.now().getDayOfWeek().name().substring(0, 3); // MON, TUE ...

        String nowHour = LocalTime.now().format(DateTimeFormatter.ofPattern("HH"));
        String nowMin  = LocalTime.now().format(DateTimeFormatter.ofPattern("mm"));

        List<BatchOut> jobs = batchJobRepository.findExecutableJobs(
                todayMonth, todayDay, todayWeek, nowHour, nowMin
        );

        for (BatchOut job : jobs) {
            runJob(job);
        }
    }

    // ============================================
    // next_exec_info 업데이트 (Daily + Monthly + Weekly)
    // ============================================
    private void updateNextExecInfo(BatchOut job) {

        LocalDate today = LocalDate.now();
        LocalDate next;

        String gb = job.getScheduleGb();

        // MONTHLY
        if (gb.equals("M")) {

            int day = Integer.parseInt(job.getJobDay());
            LocalDate thisMonth = LocalDate.of(today.getYear(), today.getMonthValue(), 1);
            LocalDate target = thisMonth.withDayOfMonth(day);

            if (today.isBefore(target)) {
                next = target;
            } else {
                next = thisMonth.plusMonths(1).withDayOfMonth(day);
            }
        }

        // WEEKLY
        else if (gb.equals("W")) {

            String week = job.getJobWeek(); // MON, TUE ...
            DayOfWeek targetWeek = toFullDayOfWeek(week);

            next = today.plusDays(1);

            while (next.getDayOfWeek() != targetWeek) {
                next = next.plusDays(1);
            }
        }

        // DAILY
        else {
            next = today.plusDays(1);
        }

        job.setNextExecInfo(next.toString());
    }

    private DayOfWeek toFullDayOfWeek(String week) {

        return switch (week) {
            case "MON" -> DayOfWeek.MONDAY;
            case "TUE" -> DayOfWeek.TUESDAY;
            case "WED" -> DayOfWeek.WEDNESDAY;
            case "THU" -> DayOfWeek.THURSDAY;
            case "FRI" -> DayOfWeek.FRIDAY;
            case "SAT" -> DayOfWeek.SATURDAY;
            case "SUN" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Invalid job_week: " + week);
        };
    }


    // ============================================
    // 실제 job 실행 + 히스토리 적재
    // ============================================
    private void runJob(BatchOut job) {

        long start = System.currentTimeMillis();
        PythonJobResult result = pythonBatchJob.runPythonScript(job.getShellFileDir());
        long end = System.currentTimeMillis();

        BatchOutHistory hist = new BatchOutHistory();
        hist.setJobId(job.getJobId());
        hist.setJobName(job.getJobName());
        hist.setJobInfo(job.getJobInfo());
        hist.setExecStartTime(new Timestamp(start));
        hist.setExecEndTime(new Timestamp(end));
        hist.setExecStatus(result.getStatus());
        hist.setExecMessage(result.getErrorMsg());
        hist.setExecDate(new java.sql.Date(System.currentTimeMillis()));
        hist.setDurationMs(end - start);
        historyRepository.save(hist);

        job.setActGb("Y");
        job.setLastExecInfo(LocalDate.now().toString());
        updateNextExecInfo(job);

        batchJobRepository.save(job);

        System.out.printf("✔ %s 실행 완료 → next_exec_info=%s\n",
                job.getJobName(), job.getNextExecInfo());
    }

    // ============================================
    // 매일 00:00 act_gb 리셋
    // ============================================
    @Scheduled(cron = "0 0 0 * * *")
    public void resetActGbAtMidnight() {

        List<BatchOut> jobs = batchJobRepository.findAll();

        for (BatchOut job : jobs) {
            job.setActGb("N");
        }

        batchJobRepository.saveAll(jobs);

        System.out.println("🌙 자정 리셋 완료: act_gb = 'N'");
    }
}

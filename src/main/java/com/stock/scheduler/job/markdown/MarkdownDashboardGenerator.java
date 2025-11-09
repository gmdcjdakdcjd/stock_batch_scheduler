package com.stock.scheduler.job.markdown;

import com.stock.scheduler.entity.JobHistory;
import com.stock.scheduler.repository.JobHistoryRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class MarkdownDashboardGenerator {

    private final JobHistoryRepository jobHistoryRepository;

    public MarkdownDashboardGenerator(JobHistoryRepository jobHistoryRepository) {
        this.jobHistoryRepository = jobHistoryRepository;
    }

    public String generate(LocalDate date) {
        List<JobHistory> jobs = jobHistoryRepository.findByDate(date);

        if (jobs == null || jobs.isEmpty()) {
            return "⚠️ 오늘(" + date + ") 실행된 배치 내역이 없습니다.";
        }

        // ✅ 전체 통계
        long total = jobs.size();
        long success = jobs.stream().filter(j -> "SUCCESS".equals(j.getStatus())).count();
        int totalSec = jobs.stream().mapToInt(JobHistory::getDurationSec).sum();

        // ✅ 코스피 / ETF 개수 (동적 추출)
        int kospiCount = jobs.stream()
                .filter(j -> "DBUpdater".equals(j.getJobName()))
                .map(JobHistory::getRowCount)
                .findFirst()
                .orElse(0);

        int etfCount = jobs.stream()
                .filter(j -> "ETFDBUpdater".equals(j.getJobName()))
                .map(JobHistory::getRowCount)
                .findFirst()
                .orElse(0);

        // ✅ Markdown 빌드
        StringBuilder md = new StringBuilder();
        md.append("# 🧩 ").append(date.format(DateTimeFormatter.ISO_DATE))
                .append(" 전체 배치 실행 결과\n\n");

        md.append("| 날짜 | 배치 그룹 | 관리 종목 | 성공률 | 총 소요시간 |\n");
        md.append("|------|------------|------------|-----------|--------------|\n");
        md.append(String.format(
                "| %s | + %d개 배치 | 코스피 %d / ETF %d | ✅ %d / %d | ⏱️ %d분 %d초 |\n",
                date, total, kospiCount, etfCount, success, total, totalSec / 60, totalSec % 60
        ));

        md.append("\n---\n\n");
        md.append("### 📂 상세 배치 목록\n\n");
        md.append("| 배치명 | 상태 | 결과 종목 수 | 시작 시간 | 종료 시간 | 소요시간(초) |\n");
        md.append("|--------|-------|--------------|------------|------------|--------------|\n");

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        for (JobHistory j : jobs) {
            String start = (j.getStartTime() != null)
                    ? j.getStartTime().toLocalDateTime().format(timeFmt)
                    : "-";
            String end = (j.getEndTime() != null)
                    ? j.getEndTime().toLocalDateTime().format(timeFmt)
                    : "-";

            md.append(String.format("| %s | %s | %d | %s | %s | %d |\n",
                    j.getJobName(),
                    "SUCCESS".equals(j.getStatus()) ? "✅ 성공" : "❌ 실패",
                    j.getRowCount(),
                    start,
                    end,
                    j.getDurationSec()
            ));
        }

        md.append("\n---\n\n");
        md.append("### ⚠ 실패·미처리 내역\n\n");
        jobs.stream()
                .filter(j -> !"SUCCESS".equals(j.getStatus()))
                .forEach(j -> md.append(String.format("- **%s** → %s\n",
                        j.getJobName(),
                        j.getErrorMsg() != null ? j.getErrorMsg() : "알 수 없는 오류"
                )));

        md.append("\n\n🕓 **생성일시:** ").append(LocalDate.now());
        md.append("\n🧑‍💻 **작성자:** System");

        return md.toString();
    }
}

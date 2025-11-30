/*
package com.stock.scheduler.job.markdown;

import com.stock.scheduler.entity.SignalRecord;
import java.sql.Timestamp;
import java.util.List;

public class MarkdownDetailGenerator implements MarkdownTemplate {

    @Override
    public String generate(String jobName, List<SignalRecord> signals, Timestamp start, Timestamp end) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 📈 전략 실행 결과 - ").append(jobName).append("\n\n");
        sb.append("- **시작 시각:** ").append(start).append("\n");
        sb.append("- **종료 시각:** ").append(end).append("\n");
        sb.append("- **총 신호 개수:** ").append(signals.size()).append("개\n\n");
        sb.append("---\n\n");

        sb.append("### 💡 상위 매수 후보 종목\n\n");
        sb.append("| 순위 | 종목명 | 코드 | 액션 | 현재가 | 이전가 | 수익률(%) |\n");
        sb.append("|:---:|:---|:---:|:---:|---:|---:|---:|\n");

        // ✅ 모든 신호 출력
        for (SignalRecord s : signals) {
            sb.append(String.format("| %d | %s | %s | %s | %,d | %,d | %.2f |\n",
                    s.getRankOrder(), s.getName(), s.getCode(),
                    s.getAction(), s.getPrice(), s.getOldPrice(), s.getReturns()));
        }

        sb.append("\n---\n");
        sb.append("_자동 생성 시각: ").append(new Timestamp(System.currentTimeMillis())).append("_\n");
        return sb.toString();
    }

}
*/

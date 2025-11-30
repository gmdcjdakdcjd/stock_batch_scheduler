/*
package com.stock.scheduler.job.markdown;

import com.stock.scheduler.entity.SignalRecord;
import java.sql.Timestamp;
import java.util.List;

public class MarkdownSimpleGenerator implements MarkdownTemplate {

    @Override
    public String generate(String jobName, List<SignalRecord> signals, Timestamp start, Timestamp end) {
        StringBuilder sb = new StringBuilder();

        sb.append("- **배치 시각:** ").append(start).append("\n");
        sb.append("### 💡 상위 매수 후보 종목\n\n");
        sb.append("| 순위 | 종목명 | 코드 | 액션 | 현재가 | 이전가 | 수익률(%) |\n");
        sb.append("|:---:|:---|:---:|:---:|---:|---:|---:|\n");

        // ✅ 모든 신호 전부 출력
        for (int i = 0; i < signals.size(); i++) {
            SignalRecord s = signals.get(i);
            sb.append(String.format("| %d | %s | %s | %s | %,d | %,d | %.2f |\n",
                    s.getRankOrder(), s.getName(), s.getCode(),
                    s.getAction(), s.getPrice(), s.getOldPrice(), s.getReturns()));
        }

        return sb.toString();
    }

}
*/

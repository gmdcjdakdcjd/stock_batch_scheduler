
package com.stock.scheduler.job;

import com.stock.scheduler.entity.SignalRecord;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class PythonBatchJob {

    private final DataSource dataSource;

    public PythonBatchJob(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    public PythonJobResult runPythonScript(String scriptPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "C:\\Users\\ysh01\\anaconda3\\Scripts\\conda.exe",
                    "run", "-n", "stock_batch_py39", "--no-capture-output", "python", scriptPath
            );
            pb.directory(new File("D:\\STOCK_PROJECT\\python_stock_batch"));
            pb.environment().put("PYTHONPATH", "D:\\STOCK_PROJECT\\python_stock_batch");
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));

            String line;
            int rowCount = 0;
            int codeCount = 0;
            Long resultId = null;
            StringBuilder logs = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                System.out.println("[PYTHON] " + line);
                logs.append(line).append("\n");

                if (line.startsWith("ROWCOUNT=")) {
                    rowCount = Integer.parseInt(line.split("=")[1].trim());
                } else if (line.startsWith("CODECOUNT=")) {
                    codeCount = Integer.parseInt(line.split("=")[1].trim());
                } else if (line.startsWith("RESULT_ID=")) {
                    resultId = Long.parseLong(line.split("=")[1].trim());
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return new PythonJobResult("SUCCESS", rowCount, codeCount, "NO_ERROR", resultId);
            } else {
                return new PythonJobResult("FAIL", rowCount, codeCount, "Exit code: " + exitCode, resultId);
            }

        } catch (Exception e) {
            return new PythonJobResult("FAIL", 0, 0, e.getMessage(), null);
        }
    }

    public List<SignalRecord> fetchSignalsFromDB(Long resultId) {
        List<SignalRecord> signals = new ArrayList<>();

        try {
            Class.forName("org.mariadb.jdbc.Driver"); // ✅ 추가
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MariaDB 드라이버 로드 실패: " + e.getMessage());
            return signals;
        }

        String url = "jdbc:mariadb://localhost:3306/investar?useSSL=false&serverTimezone=Asia/Seoul"; // ✅ 수정
        String user = "root";
        String password = "0806";

        String sql = """
                SELECT code, name, action, price, old_price, returns, rank_order
                FROM strategy_signal
                WHERE result_id = ?
                ORDER BY rank_order ASC
                """;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, resultId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                SignalRecord record = new SignalRecord();
                record.setCode(rs.getString("code"));
                record.setName(rs.getString("name"));
                record.setAction(rs.getString("action"));
                record.setPrice(rs.getInt("price"));
                record.setOldPrice(rs.getInt("old_price"));
                record.setReturns(rs.getDouble("returns"));
                record.setRankOrder(rs.getInt("rank_order"));
                signals.add(record);
            }

        } catch (SQLException e) {
            System.err.println("❌ fetchSignalsFromDB() 오류: " + e.getMessage());
        }

        return signals;
    }

    public List<SignalRecord> fetchSignalsFromDBByJob(String jobName, LocalDate date) {
        String sql = """
        SELECT s.code, s.name, s.action, s.price, s.old_price, s.returns, s.rank_order
        FROM strategy_signal s
        WHERE s.result_id = (
            SELECT id FROM strategy_result
            WHERE strategy_name = ? AND signal_date = ?
            ORDER BY id DESC LIMIT 1
        )
        ORDER BY s.rank_order ASC
    """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, jobName);
            ps.setDate(2, java.sql.Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                List<SignalRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new SignalRecord(
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getString("action"),
                            rs.getInt("price"),
                            rs.getInt("old_price"),
                            rs.getDouble("returns"),
                            rs.getInt("rank_order")
                    ));
                }
                return list;
            }

        } catch (Exception e) {
            System.err.println("❌ fetchSignalsFromDBByJob 실패: " + e.getMessage());
            return List.of();
        }
    }
}


package com.stock.scheduler.job;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Component
public class PythonBatchJob {

    public PythonJobResult runPythonScript(String scriptPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "C:\\Users\\ysh01\\anaconda3\\Scripts\\conda.exe",
                    "run", "-n", "python_stock_batch", "--no-capture-output", "python", scriptPath
            );

            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));

            String line;
            int rowCount = 0;
            int codeCount = 0;
            StringBuilder logs = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                System.out.println("[PYTHON] " + line);
                logs.append(line).append("\n");

                if (line.startsWith("ROWCOUNT=")) {
                    try {
                        rowCount = Integer.parseInt(line.split("=")[1].trim());
                    } catch (NumberFormatException e) {
                        System.err.println("ROWCOUNT 파싱 실패: " + line);
                    }
                } else if (line.startsWith("CODECOUNT=")) {
                    try {
                        codeCount = Integer.parseInt(line.split("=")[1].trim());
                    } catch (NumberFormatException e) {
                        System.err.println("CODECOUNT 파싱 실패: " + line);
                    }
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return new PythonJobResult("SUCCESS", rowCount, codeCount, "NO_ERROR");
            } else {
                return new PythonJobResult("FAIL", rowCount, codeCount, "Exit code: " + exitCode);
            }

        } catch (Exception e) {
            return new PythonJobResult("FAIL", 0, 0, e.getMessage());
        }
    }
}
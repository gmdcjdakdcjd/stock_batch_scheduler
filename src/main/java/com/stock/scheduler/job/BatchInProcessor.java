package com.stock.scheduler.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class BatchInProcessor {

    private final DataSource dataSource;
    private final ObjectMapper mapper = new ObjectMapper();

    public BatchInProcessor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // -----------------------------
    // 🔥 1) 파일 필터링
    // -----------------------------
    public List<Path> getFiles(String today, String pattern) throws Exception {

        Path folder = Paths.get("D:/STOCK_PROJECT/batch_in/" + today);

        if (!Files.exists(folder)) return List.of();

        List<Path> files = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path file : stream) {
                String name = file.getFileName().toString();
                if (name.startsWith(pattern)) {
                    files.add(file);
                }
            }
        }

        return files;
    }

    // -----------------------------
    // 🔥 2) 단일 파일 처리 (CSV/JSON 자동 구분)
    // -----------------------------
    public void processFile(Path filePath) throws Exception {

        String fileName = filePath.getFileName().toString();
        String tableName = fileName.substring(0, fileName.lastIndexOf("_"));

        // ★ 이 한 줄 추가
        tableName = tableName.toLowerCase();

        if (fileName.endsWith(".csv")) {
            processCsv(filePath, tableName);
        } else {
            processJson(filePath, tableName);
        }

        System.out.println("[OK] Inserted into " + tableName + " → " + fileName);
    }


    // -----------------------------
    // 🔥 3) JSON 처리
    // -----------------------------
    private void processJson(Path filePath, String tableName) throws Exception {

        // BOM 제거 + 문자열 읽기
        String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)
                .replace("\uFEFF", "")
                .trim();

        if (content.startsWith("[")) {
            List<Map<String, Object>> list = mapper.readValue(content, List.class);
            for (Map<String, Object> row : list) insertDynamic(tableName, row);
        } else {
            Map<String, Object> row = mapper.readValue(content, Map.class);
            insertDynamic(tableName, row);
        }
    }

    // -----------------------------
    // 🔥 4) CSV 처리 (초고속)
    // -----------------------------
    // -----------------------------
// 🔥 4) CSV 처리 (Commons CSV 버전)
// -----------------------------
    private void processCsv(Path filePath, String tableName) throws Exception {

        // 0) 파일 BOM 제거
        byte[] bytes = Files.readAllBytes(filePath);
        String text = new String(bytes, StandardCharsets.UTF_8);
        text = text.replace("\uFEFF", "");
        Files.write(filePath, text.getBytes(StandardCharsets.UTF_8));

        try (Connection conn = dataSource.getConnection()) {

            conn.setAutoCommit(false);

            CSVParser parser = CSVParser.parse(
                    filePath.toFile(),
                    StandardCharsets.UTF_8,
                    CSVFormat.DEFAULT
                            .withFirstRecordAsHeader()
                            .withIgnoreHeaderCase()
                            .withTrim()
            );

            // 1) 헤더 정제
            List<String> headers = parser.getHeaderNames().stream()
                    .map(h -> h.replace("\uFEFF", "").trim())
                    .collect(Collectors.toList());

            // 2) SQL 생성
            String colPart = String.join(", ", headers);
            String placeHolder = headers.stream().map(h -> "?").collect(Collectors.joining(", "));

            String sql = "REPLACE INTO " + tableName +
                    " (" + colPart + ") VALUES (" + placeHolder + ")";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                int count = 0; // ← chunk counter

                for (CSVRecord record : parser) {

                    for (int i = 0; i < headers.size(); i++) {

                        String header = headers.get(i);
                        String value = record.get(header);

                        if (value == null || value.isBlank()) {
                            ps.setObject(i + 1, null);
                            continue;
                        }

                        String cleaned = value.replace(",", "").trim();

                        // 🔥 code 라면 무조건 문자열로 넣기
                        if (header.equalsIgnoreCase("code")) {
                            ps.setString(i + 1, cleaned);
                            continue;
                        }

                        if (cleaned.matches("-?\\d+(\\.\\d+)?")) {
                            ps.setObject(i + 1, Double.valueOf(cleaned));
                        } else {
                            ps.setObject(i + 1, cleaned);
                        }
                    }

                    ps.addBatch();
                    count++;

                    // 🔥 1000개마다 DB에 반영 + commit
                    if (count % 1000 == 0) {
                        ps.executeBatch();
                        conn.commit();
                    }
                }

                // 🔥 남은 레코드 처리
                ps.executeBatch();
                conn.commit();
            }
        }
    }




    // -----------------------------
    // 🔥 5) JSON insert
    // -----------------------------
    private void insertDynamic(String tableName, Map<String, Object> json) throws Exception {

        List<String> columns = new ArrayList<>(json.keySet());
        List<Object> values = columns.stream().map(json::get).collect(Collectors.toList());

        String colPart = String.join(", ", columns);
        String placeHolder = columns.stream().map(c -> "?").collect(Collectors.joining(", "));

        String sql = "REPLACE INTO " + tableName +
                " (" + colPart + ") VALUES (" + placeHolder + ")";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            for (Object v : values) {
                ps.setObject(idx++, v);
            }

            ps.executeUpdate();
        }
    }

    // -----------------------------
    // 🔥 6) 폴더 이동
    // -----------------------------
    public void moveTodayFolder(String today) {

        String srcDir = "D:/STOCK_PROJECT/batch_out/" + today;
        String destDir = "D:/STOCK_PROJECT/batch_in/" + today;
//        String historyDir = "D:/STOCK_PROJECT/batch_out_history/" + today;

        try {
            Path destPath = Paths.get(destDir);
//            Path historyPath = Paths.get(historyDir);

            // 목적지 폴더 없으면 생성
            if (!Files.exists(destPath)) {
                Files.createDirectories(destPath);
            }

            // 히스토리 폴더 없으면 생성
//            if (!Files.exists(historyPath)) {
//                Files.createDirectories(historyPath);
//            }

            // 원본 폴더의 파일들 읽기
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(srcDir))) {

                for (Path srcFile : stream) {

                    if (Files.isRegularFile(srcFile)) {

                        Path fileName = srcFile.getFileName();

                        // 1️⃣ batch_in 으로 COPY (복사)
                        Path destFile = destPath.resolve(fileName);
                        Files.copy(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);

                        // 2️⃣ batch_out_history 로 MOVE (이동)
//                        Path historyFile = historyPath.resolve(fileName);
//                        Files.move(srcFile, historyFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

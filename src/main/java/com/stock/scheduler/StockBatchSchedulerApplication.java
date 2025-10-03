package com.stock.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // @Scheduled 사용 활성화
public class StockBatchSchedulerApplication {
	public static void main(String[] args) {
		SpringApplication.run(StockBatchSchedulerApplication.class, args);
	}
}
package com.stock.scheduler.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data                   // Getter, Setter, toString, equals, hashCode 자동 생성
@NoArgsConstructor      // 기본 생성자 자동 생성
@AllArgsConstructor     // 모든 필드를 받는 생성자 자동 생성
public class SignalRecord {

    private String code;       // 종목코드
    private String name;       // 종목명
    private String action;     // 매수/매도
    private int price;         // 현재가
    private int oldPrice;      // 이전가
    private double returns;    // 수익률
    private int rankOrder;     // 순위
}

package com.streamxhub.streamx.flink.javacase.bean;


import lombok.Data;

@Data
public class OrderInfo {
    private String orderId;
    private String marketId;
    private Double price;
    private Long timestamp;
}

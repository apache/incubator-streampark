package com.streamxhub.flink.test;

import lombok.Data;

import java.io.Serializable;

@Data
public class LogBean implements Serializable {
    private String platenum;
    private String card_type;
    private Long in_time;
    private Long out_time;
    private String controlid;
    private String photourlcarout;
    private String market_carlogid;
}

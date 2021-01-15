package com.streamxhub.flink.javacase;

import java.io.Serializable;

import lombok.Data;

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

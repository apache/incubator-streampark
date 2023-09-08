package org.apache.streampark.console.core.bean;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class AlertProbeMsg {

  private List<Integer> alertId;

  private String user;

  private Integer probeJobs;

  private Integer failedJobs;

  private Integer lostJobs;

  private Integer cancelledJobs;

  public void incrementProbeJobs() {
    this.probeJobs++;
  }

  public void incrementFailedJobs() {
    this.failedJobs++;
  }

  public void incrementLostJobs() {
    this.lostJobs++;
  }

  public void incrementCancelledJobs() {
    this.cancelledJobs++;
  }
}

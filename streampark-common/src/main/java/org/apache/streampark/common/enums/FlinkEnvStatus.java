package org.apache.streampark.common.enums;

/* the flink environment status */
public enum FlinkEnvStatus {
  /* FLINK_HOME path invalid */
  INVALID(-1),
  /* this add/update operation are feasible */
  FEASIBLE(0),
  /* defined flink name repeated */
  NAME_REPEATED(1),

  /* dist Jar more than one */
  FLINK_DIST_REPEATED(2);
  private final int code;

  FlinkEnvStatus(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}

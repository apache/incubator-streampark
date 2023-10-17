package org.apache.streampark.console.core.service.datasource.config;

public enum DatasourceType {
  UNKNOWN(0),

  SUCCESS(1),

  FAILED(2);

  private final Integer state;

  DatasourceType(Integer state) {
    this.state = state;
  }

  public Integer getState() {
    return state;
  }
}

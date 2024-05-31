package org.apache.streampark.console.core.enums;

import java.util.Arrays;

public enum AuthenticationType {
  SIGN(1),

  OPENAPI(2);

  private final Integer value;

  AuthenticationType(int value) {
    this.value = value;
  }

  public int get() {
    return this.value;
  }

  public static AuthenticationType of(Integer value) {
    return Arrays.stream(values()).filter((x) -> x.value == value).findFirst().orElse(null);
  }
}

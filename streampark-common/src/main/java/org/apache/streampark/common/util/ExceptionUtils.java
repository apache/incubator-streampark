package org.apache.streampark.common.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/** Utils to process exception message. */
public class ExceptionUtils {

  private ExceptionUtils() {}

  public static String stringifyException(Throwable throwable) {
    if (throwable == null) {
      return "(null)";
    }
    try (StringWriter stm = new StringWriter();
        PrintWriter writer = new PrintWriter(stm)) {
      throwable.printStackTrace(writer);
      return stm.toString();
    } catch (IOException e) {
      return e.getClass().getName() + " (error while printing stack trace)";
    }
  }
}

package com.uber.profiling.reporters;

import com.uber.profiling.Reporter;
import com.uber.profiling.util.AgentLogger;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Metrics reporter class for Graphite.
 *
 * Check the "host" and "port" properties for Graphite and update accordingly.
 *
 * You can also pass Graphite connection properties from yaml file and those properties will be used
 * by this reporter.
 *
 * To uses GraphiteOutputReporter with properties pass it in command.
 *
 * reporter=com.uber.profiling.reporters.GraphiteOutputReporter
 *
 * To use properties from yaml file use below command.
 *
 * reporter=com.uber.profiling.reporters.GraphiteOutputReporter,configProvider=com.uber.profiling
 * .YamlConfigProvider,configFile=/opt/graphite.yaml
 */
public class GraphiteOutputReporter implements Reporter {

  private static final AgentLogger logger = AgentLogger
      .getLogger(GraphiteOutputReporter.class.getName());
  private String host = "127.0.0.1";
  private int port = 2003;
  private String prefix = "jvm";
  private Socket socket = null;
  private PrintWriter out = null;

  private Set whiteList = new HashSet();

  @Override
  public void report(String profilerName, Map<String, Object> metrics) {
    // get DB connection
    ensureGraphiteConnection();
    // format metrics
    logger.debug("Profiler Name : " + profilerName);
    String tag = ((String) metrics.computeIfAbsent("tag", v -> "default_tag"))
        .replaceAll("\\.", "-");
    String appId = ((String) metrics.computeIfAbsent("appId", v -> "default_app"))
        .replaceAll("\\.", "-");
    String host = ((String) metrics.computeIfAbsent("host", v -> "unknown_host"))
        .replaceAll("\\.", "-");
    String process = ((String) metrics.computeIfAbsent("processUuid", v -> "unknown_process"))
        .replaceAll("\\.", "-");
    String newPrefix = String.join(".", prefix, tag, appId, host, process);

    Map<String, Object> formattedMetrics = getFormattedMetrics(metrics);
    formattedMetrics.remove("tag");
    formattedMetrics.remove("appId");
    formattedMetrics.remove("host");
    formattedMetrics.remove("processUuid");
    long timestamp = System.currentTimeMillis() / 1000;
    for (Map.Entry<String, Object> entry : formattedMetrics.entrySet()) {
      try {
        if (whiteList.contains(entry.getKey())) {
          out.printf(
              newPrefix + "." + entry.getKey() + " " + entry.getValue() + " " + timestamp + "%n");
        }
      } catch (Exception e) {
        logger.warn("Unable to print metrics, newPrefix=" + newPrefix
            + ", entry.getKey()= " + entry.getKey()
            + ", entry.getValue()= " + entry.getValue()
            + ", timestamp= " + timestamp);
      }
    }
  }

  // Format metrics in key=value (line protocol)
  public Map<String, Object> getFormattedMetrics(Map<String, Object> metrics) {
    Map<String, Object> formattedMetrics = new HashMap<>();
    for (Map.Entry<String, Object> entry : metrics.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      logger.debug("Raw Metric-Name = " + key + ", Metric-Value = " + value);
      if (value != null) {
        if (value instanceof List) {
          List listValue = (List) value;
          addListMetrics(formattedMetrics, listValue, key);
        } else if (value instanceof Map) {
          Map<String, Object> metricMap = (Map<String, Object>) value;
          addMapMetrics(formattedMetrics, metricMap, key);
        } else {
          formattedMetrics.put(key, value);
        }
      }
    }
    return formattedMetrics;
  }

  private void addMapMetrics(Map<String, Object> formattedMetrics, Map<String, Object> metricMap,
      String keyPrefix) {
    for (Map.Entry<String, Object> entry1 : metricMap.entrySet()) {
      String key1 = entry1.getKey();
      Object value1 = entry1.getValue();
      if (value1 != null) {
        if (value1 instanceof List) {
          addListMetrics(formattedMetrics, (List) value1, keyPrefix + "." + key1);
        } else if (value1 instanceof Map) {
          addMapMetrics(formattedMetrics, (Map<String, Object>) value1, keyPrefix + "." + key1);
        } else {
          formattedMetrics.put(keyPrefix + "." + key1, value1);
        }
      }
    }
  }

  private void addListMetrics(Map<String, Object> formattedMetrics,
      List listValue, String keyPrefix) {
    if (listValue != null && !listValue.isEmpty()) {
      if (listValue.get(0) instanceof List) {
        for (int i = 0; i < listValue.size(); i++) {
          addListMetrics(formattedMetrics, (List) listValue.get(i), keyPrefix + "." + i);
        }
      } else if (listValue.get(0) instanceof Map) {
        for (int i = 0; i < listValue.size(); i++) {
          Map<String, Object> metricMap = (Map<String, Object>) listValue.get(i);
          if (metricMap != null) {
            String name = null;
            Object nameValue = metricMap.get("name");
            if (nameValue != null && nameValue instanceof String) {
              name = ((String) nameValue).replaceAll("\\s", "");
            }

            if (StringUtils.isNotEmpty(name)) {
              metricMap.remove("name");
              addMapMetrics(formattedMetrics, metricMap, keyPrefix + "." + name);
            } else {
              addMapMetrics(formattedMetrics, metricMap, keyPrefix + "." + i);
            }
          }
        }
      } else {
        List<String> metricList = (List<String>) listValue;
        formattedMetrics.put(keyPrefix, String.join(",", metricList));
      }
    }
  }

  private void ensureGraphiteConnection() {
    if (socket == null) {
      synchronized (this) {
        if (socket == null) {
          try {
            logger.info("connecting to graphite(" + host + ":" + port + ")!");
            socket = new Socket(host, port);
            OutputStream s = socket.getOutputStream();
            out = new PrintWriter(s, true);
          } catch (IOException e) {
            logger.warn("connect to graphite error!", e);
          }
        }
      }
    }
  }

  @Override
  public void close() {
    try {
      if (out != null) {
        out.close();
      }
      if (socket != null) {
        socket.close();
      }
    } catch (IOException e) {
      logger.warn("close connection to graphite error!", e);
    }
  }

  // properties from yaml file
  @Override
  public void updateArguments(Map<String, List<String>> connectionProperties) {
    for (Map.Entry<String, List<String>> entry : connectionProperties.entrySet()) {
      String key = entry.getKey();
      List<String> value = entry.getValue();
      if (StringUtils.isNotEmpty(key) && value != null && !value.isEmpty()) {
        String stringValue = value.get(0);
        if (key.equals("graphite.host")) {
          logger.info("Got value for host = " + stringValue);
          this.host = stringValue;
        } else if (key.equals("graphite.port")) {
          logger.info("Got value for port = " + stringValue);
          this.port = Integer.parseInt(stringValue);
        } else if (key.equals("graphite.prefix")) {
          logger.info("Got value for database = " + stringValue);
          this.prefix = stringValue;
        } else if (key.equals("graphite.whiteList")) {
          logger.info("Got value for whiteList = " + stringValue);
          if (stringValue != null && stringValue.length() > 0) {
            for (String pattern : stringValue.split(",")) {
              this.whiteList.add(pattern.trim());
            }
          }
        }
      }
    }
  }
}

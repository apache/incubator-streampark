package com.uber.profiling.reporters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

import com.uber.profiling.Reporter;
import com.uber.profiling.util.AgentLogger;

/**
 * Metrics reporter class for InfluxDB. Database name "metrics" is used by this
 * reporter. Create "metrics" database in InfluxDB before using this reporter.
 * If you want to use different database name then update the value of "database"
 * property in this class.
 * 
 * Check the "host" and "port" properties for InfluxDB and update accordingly.
 * If Authentication is enabled in your InfluxDB then update "username" and
 * "password" properties with appropriate values. If Authentication is not
 * enabled then these values will not be used.
 * 
 * You can also pass database connection properties from yaml file and those
 * properties will be used by this reporter.
 * 
 * To uses InfluxDBOutputReporter with default database connection properties
 * pass it in command.
 * 
 *     reporter=com.uber.profiling.reporters.InfluxDBOutputReporter
 * 
 * To use database connection properties from yaml file use below command. 
 * 
 *     reporter=com.uber.profiling.reporters.InfluxDBOutputReporter,configProvider=com.uber.profiling.YamlConfigProvider,configFile=/opt/influxdb.yaml
 *
 */
public class InfluxDBOutputReporter implements Reporter {

    private static final AgentLogger logger = AgentLogger.getLogger(InfluxDBOutputReporter.class.getName());
    private InfluxDB influxDB = null;
    // InfluxDB default connection properties
    private String host = "127.0.0.1";
    private String port = "8086";
    private String database = "metrics";
    private String username = "admin";
    private String password = "admin";

    @Override
    public void report(String profilerName, Map<String, Object> metrics) {
        // get DB connection
        ensureInfluxDBCon();
        // format metrics 
        logger.info("Profiler Name : " + profilerName);
        Map<String, Object> formattedMetrics = getFormattedMetrics(metrics);
        for (Map.Entry<String, Object> entry : formattedMetrics.entrySet()) {
            logger.info("Formatted Metric-Name = " + entry.getKey() + ", Metric-Value = " + entry.getValue());
        }
        // Point
        Point point = Point.measurement(profilerName)
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .fields(formattedMetrics)
                .tag("processUuid", (String)metrics.get("processUuid"))
                .build();
        // BatchPoints
        BatchPoints batchPoints = BatchPoints.database(database)
                .consistency(ConsistencyLevel.ALL)
                .retentionPolicy("autogen")
                .build();
        batchPoints.point(point);
        // Write
        this.influxDB.write(batchPoints);
    }

    // Format metrics in key=value (line protocol)
    private Map<String, Object> getFormattedMetrics(Map<String, Object> metrics) {
        Map<String, Object> formattedMetrics = new HashMap<>();
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            logger.info("Raw Metric-Name = " + key + ", Metric-Value = " + value);
            if (value != null && value instanceof List) {
                List listValue = (List) value;
                if (!listValue.isEmpty() && listValue.get(0) instanceof String) {
                    List<String> metricList = (List<String>) listValue;
                    formattedMetrics.put(key, String.join(",", metricList));
                } else if (!listValue.isEmpty() && listValue.get(0) instanceof Map) {
                    List<Map<String, Object>> metricList = (List<Map<String, Object>>) listValue;
                    int num = 1;
                    for (Map<String, Object> metricMap : metricList) {
                        String name = null;
                        if(metricMap.containsKey("name") && metricMap.get("name") != null && metricMap.get("name") instanceof String){
                            name = (String) metricMap.get("name");
                            name = name.replaceAll("\\s", "");
                        }
                        for (Map.Entry<String, Object> entry1 : metricMap.entrySet()) {
                            if(StringUtils.isNotEmpty(name)){
                                formattedMetrics.put(key + "-" + name + "-" + entry1.getKey(), entry1.getValue());
                            }else{
                                formattedMetrics.put(key + "-" + entry1.getKey() + "-" + num, entry1.getValue());
                           }
                        }
                        num++;
                    }
                }
            } else if (value != null && value instanceof Map) {
                Map<String, Object> metricMap = (Map<String, Object>) value;
                for (Map.Entry<String, Object> entry1 : metricMap.entrySet()) {
                    String key1 = entry1.getKey();
                    Object value1 = entry1.getValue();
                    if (value1 != null && value1 instanceof Map) {
                        Map<String, Object> value2 = (Map<String, Object>) value1;
                        int num = 1;
                        for (Map.Entry<String, Object> entry2 : value2.entrySet()) {
                            formattedMetrics.put(key + "-" + key1 + "-" + entry2.getKey() + "-" + num, entry2.getValue());
                        }
                        num++;
                    }
                }
            } else {
                formattedMetrics.put(key, value);
           }
        }
        return formattedMetrics;
    }

    @Override
    public void close() {
        synchronized (this) {
            this.influxDB.close();
            this.influxDB = null;
        }
    }

    private void ensureInfluxDBCon() {
        synchronized (this) {
            if (influxDB != null) {
                return;
            }
            String url = "http://" + host + ":" + port;
            logger.info("Trying to connect InfluxDB using url=" + url + ", database=" + database + ", username="
                    + username + ", password=" + password);
            this.influxDB = InfluxDBFactory.connect(url, username, password);
            // enable batch
            this.influxDB.enableBatch(BatchOptions.DEFAULTS);
            // set log level
            influxDB.setLogLevel(InfluxDB.LogLevel.BASIC);
        }
    }

    // properties from yaml file
    @Override
    public void updateArguments(Map<String, List<String>> connectionProperties) {
        for (Map.Entry<String,  List<String>> entry : connectionProperties.entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            if (StringUtils.isNotEmpty(key) && value != null && !value.isEmpty()) {
                String stringValue = value.get(0);
                if (key.equals("influxdb.host")) {
                    logger.info("Got value for host = "+stringValue);
                    this.host = stringValue;
                } else if (key.equals("influxdb.port")) {
                    logger.info("Got value for port = "+stringValue);
                    this.port = stringValue;
                } else if (key.equals("influxdb.database")) {
                    logger.info("Got value for database = "+stringValue);
                    this.database = stringValue;
                } else if (key.equals("influxdb.username")) {
                    logger.info("Got value for username = "+stringValue);
                    this.username = stringValue;
                } else if (key.equals("influxdb.password")) {
                    logger.info("Got value for password = "+stringValue);
                    this.password = stringValue;
                }
            }
        }
    }
}

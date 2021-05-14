package com.streamxhub.streamx.flink.javacase.tablesql;

import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;

public class JavaTableApp {

    public static void main(String[] args) {
        EnvironmentSettings bbSettings = EnvironmentSettings.newInstance().useBlinkPlanner().build();
        TableEnvironment bsTableEnv = TableEnvironment.create(bbSettings);

        String sourceDDL = "CREATE TABLE datagen (  " +
                " f_random INT,  " +
                " f_random_str STRING,  " +
                " ts AS localtimestamp,  " +
                " WATERMARK FOR ts AS ts  " +
                ") WITH (  " +
                " 'connector' = 'datagen',  " +
                " 'rows-per-second'='10',  " +
                " 'fields.f_random.min'='1',  " +
                " 'fields.f_random.max'='5',  " +
                " 'fields.f_random_str.length'='10'  " +
                ")";

        bsTableEnv.executeSql(sourceDDL);

        String sinkDDL = "CREATE TABLE print_table (" +
                " f_random int," +
                " c_val bigint, " +
                " wStart TIMESTAMP(3) " +
                ") WITH ('connector' = 'print') ";

        bsTableEnv.executeSql(sinkDDL);
    }

}





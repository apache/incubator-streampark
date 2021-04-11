import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.flink.common.util.SQLCommandUtil;
import org.junit.Test;

public class SQLCommandTest {

    @Test
    public void sqlParae() {

        String sql = "CREATE TABLE user_log (\n" +
                "    user_id VARCHAR,\n" +
                "    item_id VARCHAR,\n" +
                "    category_id VARCHAR,\n" +
                "    behavior VARCHAR,\n" +
                "    ts TIMESTAMP(3)\n" +
                ") WITH (\n" +
                "'connector.type' = 'kafka', -- 使用 kafka connector\n" +
                "'connector.version' = 'universal',  -- kafka 版本，universal 支持 0.11 以上的版本\n" +
                "'connector.topic' = 'user_behavior',  -- kafka topic\n" +
                "'connector.properties.bootstrap.servers'='test-hadoop-7:9092,test-hadoop-8:9092,test-hadoop-9:9092',\n" +
                "'connector.startup-mode' = 'earliest-offset', -- 从起始 offset 开始读取\n" +
                "'update-mode' = 'append',\n" +
                "'format.type' = 'json',  -- 数据源格式为 json\n" +
                "'format.derive-schema' = 'true' -- 从 DDL schema 确定 json 解析规则\n" +
                ");\n" +
                "\n" +
                "CREATE TABLE pvuv_sink (\n" +
                "    dt VARCHAR,\n" +
                "    pv BIGINT,\n" +
                "    uv BIGINT\n" +
                ") WITH (\n" +
                "'connector.type' = 'jdbc', -- 使用 jdbc connector\n" +
                "'connector.url' = 'jdbc:mysql://localhost:3306/test', -- jdbc url\n" +
                "'connector.table' = 'pvuv_sink', -- 表名\n" +
                "'connector.username' = 'root', -- 用户名\n" +
                "'connector.password' = '123456', -- 密码\n" +
                "'connector.write.flush.max-rows' = '1' -- 默认5000条，为了演示改为1条\n" +
                ");\n" +
                "\n" +
                "INSERT INTO pvuv_sink\n" +
                "SELECTx\n" +
                "  DATE_FORMAT(ts, 'yyyy-MM-dd HH:00') dt,\n" +
                "  COUNT(*) AS pv,\n" +
                "  COUNT(DISTINCT user_id) AS uv\n" +
                "FROM user_log\n" +
                "GROUP BY DATE_FORMAT(ts, 'yyyy-MM-dd HH:00');\n";

        RestResponse response = null;

        try {
            SQLCommandUtil.verifySQL(sql);
            response = RestResponse.create().data(true);
        } catch (Exception e) {
            String split = ",sql:";
            String message = e.getMessage();

            String errorMsg = message.substring(0, message.indexOf(split)).trim();
            String errorSQL = message.substring(message.indexOf(split) + split.length()).trim();

            String[] array = errorSQL.split("\n");
            String first = array[0];
            String end = array.length > 1 ? array[array.length - 1] : null;

            response = RestResponse.create()
                    .data(false)
                    .message(errorMsg)
                    .put("sql", errorSQL)
                    .put("first", first)
                    .put("end", end);

        }

        System.out.println(response);
    }

}

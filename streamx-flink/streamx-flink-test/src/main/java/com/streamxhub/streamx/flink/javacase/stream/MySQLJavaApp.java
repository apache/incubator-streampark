package com.streamxhub.streamx.flink.javacase.stream;

import com.streamxhub.streamx.flink.core.java.function.SQLQueryFunction;
import com.streamxhub.streamx.flink.core.java.function.SQLResultFunction;
import com.streamxhub.streamx.flink.core.java.source.JdbcSource;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import com.streamxhub.streamx.flink.core.scala.util.StreamEnvConfig;
import com.streamxhub.streamx.flink.javacase.bean.OrderInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MySQLJavaApp {

    public static void main(String[] args) {

        StreamEnvConfig envConfig = new StreamEnvConfig(args, null);

        StreamingContext context = new StreamingContext(envConfig);

        //读取MySQL数据源
        new JdbcSource<OrderInfo>(context)
                .getDataStream(
                        (SQLQueryFunction<OrderInfo>) lastOne -> {
                            //5秒抽取一次
                            Thread.sleep(5000);

                            Serializable lastOffset = lastOne == null
                                    ? "2020-10-10 23:00:00"
                                    : lastOne.getTimestamp();

                            return String.format(
                                    "select * from t_order " +
                                            "where timestamp > '%s' " +
                                            "order by timestamp asc ",
                                    lastOffset
                            );
                        },
                        (SQLResultFunction<OrderInfo>) map -> {
                            List<OrderInfo> result = new ArrayList<>();
                            map.forEach(item -> {
                                OrderInfo orderInfo = new OrderInfo();
                                orderInfo.setOrderId(item.get("order_id").toString());
                                orderInfo.setMarketId(item.get("market_id").toString());
                                orderInfo.setTimestamp(Long.parseLong(item.get("timestamp").toString()));
                                result.add(orderInfo);
                            });
                            return result;
                        })
                .returns(TypeInformation.of(OrderInfo.class));

        context.start();

    }
}

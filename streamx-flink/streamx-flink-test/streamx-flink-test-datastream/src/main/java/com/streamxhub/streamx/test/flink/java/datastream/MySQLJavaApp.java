/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.test.flink.java.datastream;

import com.streamxhub.streamx.flink.core.StreamEnvConfig;
import com.streamxhub.streamx.flink.core.java.function.SQLQueryFunction;
import com.streamxhub.streamx.flink.core.java.function.SQLResultFunction;
import com.streamxhub.streamx.flink.core.java.source.JdbcSource;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import com.streamxhub.streamx.test.flink.java.bean.OrderInfo;
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

                    Serializable lastOffset = lastOne == null ? "2020-10-10 23:00:00" : lastOne.getTimestamp();

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
                }, null)
            .returns(TypeInformation.of(OrderInfo.class));

        context.start();

    }
}

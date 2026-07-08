/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.spark.connector.kafka.offset;

import org.apache.streampark.common.util.HBaseClient;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.BinaryPrefixComparator;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.kafka.common.TopicPartition;
import org.apache.spark.SparkConf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** HBase offset manager. */
class HBaseOffset extends Offset {

    private final String tableName;
    private final String familyName;
    private final byte[] familyNameBytes;
    private final byte[] topicBytes = Bytes.toBytes("topic");
    private final byte[] partitionBytes = Bytes.toBytes("partition");
    private final byte[] offsetBytes = Bytes.toBytes("offset");
    private transient Table table;

    HBaseOffset(SparkConf sparkConf) {
        super(sparkConf);
        Map<String, String> params = getStoreParams();
        tableName = params.get("hbase.table");
        familyName = params.getOrDefault("hbase.table.family", "tpo");
        familyNameBytes = Bytes.toBytes(familyName);
    }

    private Table getTable() {
        if (table == null) {
            try {
                java.util.Properties props = toProperties(getStoreParams());
                HBaseClient client = HBaseClient.apply(props);
                if (!client.getConnection().getAdmin().tableExists(TableName.valueOf(tableName))) {
                    HTableDescriptor tableDesc = new HTableDescriptor(TableName.valueOf(tableName));
                    tableDesc.addFamily(new HColumnDescriptor(familyName));
                    client.getConnection().getAdmin().createTable(tableDesc);
                }
                table = client.getConnection().getTable(TableName.valueOf(tableName));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return table;
    }

    @Override
    public Map<TopicPartition, Long> get(String groupId, Set<String> topics) {
        Map<TopicPartition, Long> storedOffsetMap = new HashMap<>();
        Map<TopicPartition, Long> earliestOffsets = getEarliestOffsets(new ArrayList<>(topics));
        try {
            for (String topic : topics) {
                Scan scan = new Scan().setFilter(new PrefixFilter(Bytes.toBytes(key(groupId, topic))));
                ResultScanner result = getTable().getScanner(scan);
                for (Result r : result) {
                    String topicVal = "";
                    int partition = 0;
                    long offset = 0L;
                    for (Cell cell : r.listCells()) {
                        String qualifier = Bytes.toString(CellUtil.cloneQualifier(cell));
                        switch (qualifier) {
                            case "topic":
                                topicVal = Bytes.toString(CellUtil.cloneValue(cell));
                                break;
                            case "partition":
                                partition = Bytes.toInt(CellUtil.cloneValue(cell));
                                break;
                            case "offset":
                                offset = Bytes.toLong(CellUtil.cloneValue(cell));
                                break;
                            default:
                                break;
                        }
                    }
                    TopicPartition topicPartition = new TopicPartition(topicVal, partition);
                    Long left = earliestOffsets.get(topicPartition);
                    long finalOffset = offset;
                    if (left != null && left > offset) {
                        log.warn(
                            "storeType:HBase,consumer group:{},topic:{},partition:{} offsets was timeOut,updated: {}",
                            groupId,
                            topicPartition.topic(),
                            topicPartition.partition(),
                            left);
                        finalOffset = left;
                    }
                    storedOffsetMap.put(topicPartition, finalOffset);
                }
                result.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Map<TopicPartition, Long> offsetMaps;
        if ("latest".equalsIgnoreCase(getReset())) {
            offsetMaps = new HashMap<>(getLatestOffsets(new ArrayList<>(topics)));
        } else {
            offsetMaps = new HashMap<>(getEarliestOffsets(new ArrayList<>(topics)));
        }
        offsetMaps.putAll(storedOffsetMap);
        log.info("storeType:HBase,getOffsets [{},{}] ", groupId, offsetMaps);
        return offsetMaps;
    }

    @Override
    public void update(String groupId, Map<TopicPartition, Long> offsetInfos) {
        try {
            List<Put> puts = new ArrayList<>();
            for (Map.Entry<TopicPartition, Long> entry : offsetInfos.entrySet()) {
                TopicPartition tp = entry.getKey();
                Put put = new Put(Bytes.toBytes(key(groupId, tp.topic()) + "#" + tp.partition()));
                put.addColumn(familyNameBytes, topicBytes, Bytes.toBytes(tp.topic()));
                put.addColumn(familyNameBytes, partitionBytes, Bytes.toBytes(tp.partition()));
                put.addColumn(familyNameBytes, offsetBytes, Bytes.toBytes(entry.getValue()));
                puts.add(put);
            }
            getTable().put(puts);
            log.info("storeType:HBase,updateOffsets [ {},{} ]", groupId, offsetInfos);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String groupId, Set<String> topics) {
        try {
            FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ONE);
            for (String topic : topics) {
                filterList.addFilter(
                    new RowFilter(
                        CompareFilter.CompareOp.EQUAL,
                        new BinaryPrefixComparator(Bytes.toBytes(key(groupId, topic) + "#"))));
            }
            Scan scan = new Scan();
            scan.setFilter(filterList);
            ResultScanner rs = getTable().getScanner(scan);
            Iterator<Result> iter = rs.iterator();
            List<Delete> deletes = new ArrayList<>();
            while (iter.hasNext()) {
                Result r = iter.next();
                deletes.add(new Delete(r.getRow()));
            }
            rs.close();
            getTable().delete(deletes);
            log.info("storeType:HBase,deleteOffsets [ {},{} ]", groupId, topics);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package com.streamxhub.flink.monitor.base.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.lionsoul.ip2region.DataBlock;
import org.lionsoul.ip2region.DbConfig;
import org.lionsoul.ip2region.DbSearcher;
import org.lionsoul.ip2region.Util;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Objects;

@Slf4j
public class AddressUtil {

    private AddressUtil() {
    }

    public static String getCityInfo(int algorithm, String ip) {
        try {
            String dbPath = AddressUtil.class.getResource("/ip2region/ip2region.db").getPath();
            File file = new File(dbPath);
            if (!file.exists()) {
                String tmpDir = System.getProperties().getProperty("java.io.tmpdir");
                dbPath = tmpDir + "ip.db";
                file = new File(dbPath);
                FileUtils.copyInputStreamToFile(Objects.requireNonNull(AddressUtil.class.getClassLoader().getResourceAsStream("classpath:ip2region/ip2region.db")), file);
            }
            DbConfig config = new DbConfig();
            DbSearcher searcher = new DbSearcher(config, file.getPath());
            Method method;
            switch (algorithm) {
                case DbSearcher.BTREE_ALGORITHM:
                    method = searcher.getClass().getMethod("btreeSearch", String.class);
                    break;
                case DbSearcher.BINARY_ALGORITHM:
                    method = searcher.getClass().getMethod("binarySearch", String.class);
                    break;
                case DbSearcher.MEMORY_ALGORITYM:
                    method = searcher.getClass().getMethod("memorySearch", String.class);
                    break;
                default:
                    method = searcher.getClass().getMethod("memorySearch", String.class);
                    break;
            }
            if (!Util.isIpAddress(ip)) {
                log.info("Error: Invalid ip address");
            }
            DataBlock dataBlock = (DataBlock) method.invoke(searcher, ip);
            return dataBlock.getRegion();
        } catch (Exception e) {
            log.info("获取地址信息异常：{}", e.getMessage());
        }
        return "";
    }

}

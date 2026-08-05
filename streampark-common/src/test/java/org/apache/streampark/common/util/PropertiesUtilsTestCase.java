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

package org.apache.streampark.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertiesUtilsTestCase {

    @Test
    void testExtractProgramArgs() {
        String args =
            "mysql-sync-database "
                + "--database employees "
                + "--mysql-conf hostname=127.0.0.1 "
                + "--mysql-conf port=3306 "
                + "--mysql-conf username=root "
                + "--mysql-conf password=123456 "
                + "--mysql-conf database-name=employees "
                + "--including-tables 'test|test.*' "
                + "--excluding-tables \"emp_*\" "
                + "--query 'select * from employees where age > 20' "
                + "--sink-conf fenodes=127.0.0.1:8030 "
                + "--sink-conf username=root "
                + "--sink-conf password= "
                + "--sink-conf jdbc-url=jdbc:mysql://127.0.0.1:9030 "
                + "--sink-conf sink.label-prefix=label"
                + "--table-conf replication_num=1";
        List<String> programArgs = FlinkConfigurationUtils.extractArguments(args);
        assertTrue(programArgs.contains("username=root"));
    }

    @Test
    void testDynamicProperties() {
        String dynamicProperties =
            "\n"
                + "-Denv.java.opts1=\"-Dfile.encoding=UTF-8\"\n"
                + "-Denv.java.opts2 = \"-Dfile.enc\\\"oding=UTF-8\"\n"
                + "-Denv.java.opts3 = \" -Dfile.encoding=UTF-8\"\n"
                + "-Dyarn.application.id=123\n"
                + "-Dyarn.application.name=\"streampark job\"\n"
                + "-Dyarn.application.queue=flink\n"
                + "-Ddiy.param.name=apache streampark\n"
                + "\n";

        java.util.Map<String, String> map =
            FlinkConfigurationUtils.extractDynamicProperties(dynamicProperties);
        assertEquals("-Dfile.encoding=UTF-8", map.get("env.java.opts1"));
        assertEquals("-Dfile.enc\\\"oding=UTF-8", map.get("env.java.opts2"));
        assertEquals(" -Dfile.encoding=UTF-8", map.get("env.java.opts3"));
        assertEquals("123", map.get("yarn.application.id"));
        assertEquals("streampark job", map.get("yarn.application.name"));
        assertEquals("flink", map.get("yarn.application.queue"));
        assertEquals("apache streampark", map.get("diy.param.name"));
    }
}

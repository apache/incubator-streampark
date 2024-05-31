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

package org.apache.streampark.console.base.util;

import org.apache.streampark.common.util.PropertiesUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Map;

public class BashJavaUtils {

  public static void main(String[] args) throws IOException {
    String action = args[0].toLowerCase();
    String[] actionArgs = Arrays.copyOfRange(args, 1, args.length);

    switch (action) {
      case "--get_yaml":
        String key = actionArgs[0];
        String conf = actionArgs[1];
        Map<String, String> confMap = PropertiesUtils.fromYamlFileAsJava(conf);
        String value = confMap.get(key);
        System.out.println(value);
        break;
      case "--check_port":
        Integer port = Integer.parseInt(actionArgs[0]);
        try {
          new ServerSocket(port);
          System.out.println("free");
        } catch (Exception e) {
          System.out.println("used");
        }
        break;
      default:
        break;
    }
  }
}

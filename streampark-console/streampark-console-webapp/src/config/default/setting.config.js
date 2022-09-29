/*
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

// This configuration is the default setting of the system. To modify the setting items, you can add the modified items in src/config/config.js. It can also be modified directly in this file.
module.exports = {
  lang: 'CN',                           //Language, optional CN (Simplified), HK (Traditional), US (English), other languages can also be extended
  theme: {                              //theme
    color: '#1890ff',                   //theme color
    mode: 'dark',                       //Theme mode optional dark, light
    success: '#52c41a',                 //success color
    warning: '#faad14',                 //warning color
    error: '#f5222f',                   //wrong color
  }
}

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

// 此配置为系统默认设置，需修改的设置项，在src/config/config.js中添加修改项即可。也可直接在此文件中修改。
module.exports = {
  lang: 'CN',                           //语言，可选 CN(简体)、HK(繁体)、US(英语)，也可扩展其它语言
  theme: {                              //主题
    color: '#1890ff',                   //主题色
    mode: 'dark',                       //主题模式 可选 dark、 light
    success: '#52c41a',                 //成功色
    warning: '#faad14',                 //警告色
    error: '#f5222f',                   //错误色
  }
}

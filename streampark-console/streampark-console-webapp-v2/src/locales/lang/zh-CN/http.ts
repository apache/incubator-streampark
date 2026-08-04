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
export default {
    defaultTip: '请求出错，请稍候重试',
    400: '请求参数错误',
    401: '用户没有权限（令牌、用户名、密码错误）',
    403: '用户得到授权，但是访问是被禁止的',
    404: '网络请求错误,未找到该资源',
    405: '网络请求错误,请求方法未允许',
    408: '网络请求超时',
    500: '服务器错误,请联系管理员',
    501: '网络未实现',
    502: '网络错误',
    503: '服务不可用，服务器暂时过载或维护',
    504: '网络超时',
    505: 'http版本不支持该请求',
}

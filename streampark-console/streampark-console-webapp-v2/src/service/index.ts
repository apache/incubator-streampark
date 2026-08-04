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

export * from './api/passport'
export * from './api/notify'
export * from './api/menu'
export * from './api/member'
export * from './api/user'
export * from './api/role'
export * from './api/team'
export * from './api/token'
export * from './api/openapi'
export * from './api/system'
export * from './api/setting'
export * from './api/setting/alert'
export * from './api/resource/variable'
export * from './api/resource/upload'
export * from './api/resource/project'
export * from './api/flink/gateway'
export * from './api/flink/env'
export * from './api/flink/cluster'
export * from './api/flink/app'
export * from './api/flink/build'
export * from './api/flink/savepoint'
export * from './api/flink/config'
export * from './api/spark/env'
export * from './api/spark/build'
export * from './api/spark/sql'
export * from './api/spark/app'
export {
    fetchGetSparkConf,
    fetchSparkConfHistory,
    fetchSparkConfList,
    fetchSparkConfRemove,
    fetchSparkConfTemplate,
    fetchSysHadoopConf as fetchSparkSysHadoopConf,
} from './api/spark/conf'
export * from './api/flink/sql'
export * from './api/flink/podtmpl'
export * from './api/flink/history'
export * from './api/base/error'

/** Nova dict store stub — StreamPark uses backend enums instead */
export async function fetchDictList(_code: string) {
    return {
        isSuccess: true,
        data: [] as Array<{ label: string; value: string }>,
    }
}

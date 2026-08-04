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

import type { AppListRecord } from '@/types/api/flink/app.type'
import { DeployMode } from '@/enums/flinkEnum'

export function mapEditDeployFields(app: Partial<AppListRecord>) {
    const clusterFields = {
        remoteClusterId: null as string | null,
        yarnSessionClusterId: null as string | null,
        k8sSessionClusterId: null as string | null,
    }
    if (app.deployMode === DeployMode.STANDALONE)
        clusterFields.remoteClusterId = (app.flinkClusterId as string) ?? null
    else if (app.deployMode === DeployMode.YARN_SESSION)
        clusterFields.yarnSessionClusterId = (app.flinkClusterId as string) ?? null
    else if (app.deployMode === DeployMode.KUBERNETES_SESSION)
        clusterFields.k8sSessionClusterId = (app.flinkClusterId as string) ?? null

    return {
        ...clusterFields,
        alertId: (app.alertId as string) ?? null,
        resolveOrder: app.resolveOrder ?? 0,
        checkPointFailure: {
            cpMaxFailureInterval: app.cpMaxFailureInterval ?? null,
            cpFailureRateInterval: app.cpFailureRateInterval ?? null,
            cpFailureAction: app.cpFailureAction ?? null,
        },
        k8sNamespace: app.k8sNamespace ?? '',
        serviceAccount: app.serviceAccount ?? '',
        flinkImage: app.flinkImage ?? '',
        k8sRestExposedType: app.k8sRestExposedType ?? 0,
        useSysHadoopConf: Boolean(app.k8sHadoopIntegration),
    }
}

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
import { omit } from 'lodash-es';
import { optionsKeyMapping } from '../data/option';
import { fetchYarn } from '/@/api/flink/app/app';
import { AppListRecord } from '/@/api/flink/app/app.type';
import { fetchActiveURL } from '/@/api/flink/setting/flinkCluster';

export function handleAppBuildStatusColor(statusCode) {
  switch (statusCode) {
    case 0:
      return '#99A3A4';
    case 1:
      return '#F5B041';
    case 2:
      return '#3498DB';
    case 3:
      return '#2ECC71';
    case 4:
      return '#E74C3C';
    default:
      return '#99A3A4';
  }
}

export function handleAppBuildStatueText(statusCode) {
  switch (statusCode) {
    case 0:
      return 'UNKNOWN';
    case 1:
      return 'PENDING';
    case 2:
      return 'RUNNING';
    case 3:
      return 'SUCCESS';
    case 4:
      return 'FAILURE';
    default:
      return 'UNKNOWN';
  }
}

export function handleAppBuildStepTimelineColor(step) {
  if (step == null) {
    return 'gray';
  }
  switch (step.status) {
    case 0:
    case 1:
      return '#99A3A4';
    case 2:
      return '#3498DB';
    case 3:
      return '#2ECC71';
    case 4:
      return '#E74C3C';
    case 5:
      return '#F5B041';
    default:
      return '#99A3A4';
  }
}

export function handleAppBuildStepText(stepStatus) {
  switch (stepStatus) {
    case 0:
      return 'UNKNOWN';
    case 1:
      return 'WAITING';
    case 2:
      return 'RUNNING';
    case 3:
      return 'SUCCESS';
    case 4:
      return 'FAILURE';
    case 5:
      return 'SKIPPED';
  }
}

// Field Description Filter
export function descriptionFilter(option) {
  if (option.unit) {
    return option.description + ' (Unit ' + option.unit + ')';
  } else {
    return option.description;
  }
}

export async function handleView(app: AppListRecord, yarn: Nullable<string>) {
  const executionMode = app['executionMode'];
  if (executionMode === 1) {
    const res = await fetchActiveURL(app.flinkClusterId);
    window.open(res + '/#/job/' + app.jobId + '/overview');
  } else if ([2, 3, 4].includes(executionMode)) {
    if (yarn == null) {
      const res = await fetchYarn();
      window.open(res + '/proxy/' + app['appId'] + '/');
    } else {
      window.open(yarn + '/proxy/' + app['appId'] + '/');
    }
  } else {
    if (app.flinkRestUrl != null) {
      window.open(app.flinkRestUrl);
    }
  }
}

export function handleIsStart(app, optionApps) {
  /**
   * FAILED(7),
   * CANCELED(9),
   * FINISHED(10),
   * SUSPENDED(11),
   * LOST(13),
   * OTHER(15),
   * REVOKED(16),
   * TERMINATED(18),
   * POS_TERMINATED(19),
   * SUCCEEDED(20),
   * KILLED(-9)
   * @type {boolean}
   */
  const status = [0, 7, 9, 10, 11, 13, 16, 18, 19, 20, -9].includes(app.state);

  /**
   * Deployment failed FAILED(-1),
   * Done DONE(0),
   * After the task is modified, NEED_LAUNCH(1) needs to be reissued,
   * Online LAUNCHING(2),
   * After going online, you need to restart NEED_RESTART(3),
   * Need to rollback NEED_ROLLBACK(4),
   * When the project changes, the task needs to be checked (whether the jar needs to be re-selected) NEED_CHECK(5),
   * The posted task has been revoked REVOKED(10);
   */

  const launch = [0, 3].includes(app.launch);

  const optionState = !optionApps.starting.get(app.id) || app['optionState'] === 0 || false;

  return status && launch && optionState;
}

export function handleYarnQueue(values) {
  if (values.executionMode === 4) {
    const queue = values['yarnQueue'];
    if (queue != null && queue !== '' && queue !== undefined) {
      return queue;
    }
    return null;
  }
}

/* Splice parameters */
export function handleFormValue(values) {
  const options = {};
  for (const k in omit(values, ['totalOptions', 'jmOptions', 'tmOptions'])) {
    const v = values[k];
    if (v != null && v !== '' && v !== undefined) {
      if (k === 'parallelism') {
        options['parallelism.default'] = v;
      } else if (k === 'slot') {
        options['taskmanager.numberOfTaskSlots'] = v;
      } else {
        if (
          values.totalOptions?.includes(k) ||
          values.jmMemoryItems?.includes(k) ||
          values.tmMemoryItems?.includes(k)
        ) {
          const opt = optionsKeyMapping.get(k);
          const unit = opt?.['unit'] || '';
          const name = opt?.['name'];
          if (typeof v === 'string') {
            options[name] = v.replace(/[k|m|g]b$/g, '') + unit;
          } else if (typeof v === 'number') {
            options[name] = v + unit;
          } else {
            options[name] = v;
          }
        }
      }
    }
  }
  return options;
}

export function handleDependencyJsonToPom(json, pomMap, jarMap) {
  if (json != null && json.trim() !== '') {
    const deps = JSON.parse(json);
    const pom = deps.pom;
    if (pom && pom.length > 0) {
      pom.forEach((x) => {
        const groupId = x.groupId;
        const artifactId = x.artifactId;
        const version = x.version;
        const exclusions = x.exclusions || [];

        const id = groupId + '_' + artifactId;
        const mvnPom = {
          groupId: groupId,
          artifactId: artifactId,
          version: version,
          exclusions: [] as any[],
        };
        if (exclusions != null && exclusions.length > 0) {
          exclusions.forEach((e) => {
            if (e != null && e.length > 0) {
              const e_group = e.groupId;
              const e_artifact = e.artifactId;
              mvnPom.exclusions.push({
                groupId: e_group,
                artifactId: e_artifact,
              });
            }
          });
        }
        pomMap.set(id, mvnPom);
      });
    }
    const jar = deps.jar;
    if (jar != null && jar.length > 0) {
      jar.forEach((x) => {
        jarMap.set(x, x);
      });
    }
  }
}

export function handleSubmitParams(
  params: Recordable,
  values: Recordable,
  k8sTemplate: Recordable,
) {
  const options = handleFormValue(values);
  Object.assign(params, {
    executionMode: values.executionMode,
    versionId: values.versionId,
    jobName: values.jobName,
    tags: values.tags,
    args: values.args || null,
    options: JSON.stringify(options),
    yarnQueue: handleYarnQueue(values),
    cpMaxFailureInterval: values.cpMaxFailureInterval || null,
    cpFailureRateInterval: values.cpFailureRateInterval || null,
    cpFailureAction: values.cpFailureAction || null,
    dynamicOptions: values.dynamicOptions || null,
    resolveOrder: values.resolveOrder,
    k8sRestExposedType: values.k8sRestExposedType,
    restartSize: values.restartSize,
    alertId: values.alertId,
    description: values.description,
    k8sNamespace: values.k8sNamespace || null,
    clusterId: values.clusterId || null,
    flinkClusterId: values.flinkClusterId || null,
    flinkImage: values.flinkImage || null,
    yarnSessionClusterId: values.yarnSessionClusterId || null,
  });
  if (params.executionMode === 6) {
    Object.assign(params, {
      k8sPodTemplate: k8sTemplate.podTemplate,
      k8sJmPodTemplate: k8sTemplate.jmPodTemplate,
      k8sTmPodTemplate: k8sTemplate.tmPodTemplate,
      k8sHadoopIntegration: values.useSysHadoopConf,
    });
  }
}

export function filterOption(input, option) {
  return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0;
}

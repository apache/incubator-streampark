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
import { fetchSparkYarn } from '/@/api/spark/app';
import { AppStateEnum, OptionStateEnum } from '/@/enums/sparkEnum';
import { ConfigTypeEnum, ReleaseStateEnum, PipelineStepEnum } from '/@/enums/flinkEnum';
import type { SparkApplication } from '/@/api/spark/app.type';

export function handleAppBuildStatusColor(statusCode: number) {
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

export function handleAppBuildStepText(stepStatus: number) {
  const buildStepMap = {
    [PipelineStepEnum.UNKNOWN]: 'UNKNOWN',
    [PipelineStepEnum.WAITING]: 'WAITING',
    [PipelineStepEnum.RUNNING]: 'RUNNING',
    [PipelineStepEnum.SUCCESS]: 'SUCCESS',
    [PipelineStepEnum.FAILURE]: 'FAILURE',
    [PipelineStepEnum.SKIPPED]: 'SKIPPED',
  };
  return buildStepMap[stepStatus];
}

// Field Description Filter
export function descriptionFilter(option) {
  if (option.unit) {
    return option.description + ' (Unit ' + option.unit + ')';
  } else {
    return option.description;
  }
}

export async function handleView(app: SparkApplication, yarn: Nullable<string>) {
  if (!yarn) {
    const res = await fetchSparkYarn();
    window.open(res + '/proxy/' + app['clusterId'] + '/');
  } else {
    window.open(yarn + '/proxy/' + app['clusterId'] + '/');
  }
}

export function handleIsStart(app: Recordable, optionApps: Recordable) {
  const status = [
    AppStateEnum.ADDED,
    AppStateEnum.FAILED,
    AppStateEnum.FINISHED,
    AppStateEnum.LOST,
    AppStateEnum.REVOKED,
    AppStateEnum.SUCCEEDED,
    AppStateEnum.KILLED,
  ].includes(app.state);

  /**
   * Deployment failed FAILED(-1),
   * Done DONE(0),
   * After the task is modified, NEED_RELEASE(1) needs to be reissued,
   * Online releasing(2),
   * After going online, you need to restart NEED_RESTART(3),
   * Need to rollback NEED_ROLLBACK(4),
   * When the project changes, the task needs to be checked (whether the jar needs to be re-selected) NEED_CHECK(5),
   * The posted task has been revoked REVOKED(10);
   */

  const release = [ReleaseStateEnum.DONE, ReleaseStateEnum.NEED_RESTART].includes(app.release);

  const optionState =
    !optionApps.starting.get(app.id) || app['optionState'] === OptionStateEnum.NONE || false;

  return status && release && optionState;
}

export function getAppConfType(configFile: string): number {
  const config = configFile || '';
  if (config.endsWith('.yaml') || config.endsWith('.yml')) {
    return ConfigTypeEnum.YAML;
  } else if (config.endsWith('.properties')) {
    return ConfigTypeEnum.PROPERTIES;
  } else if (config.endsWith('.conf')) {
    return ConfigTypeEnum.HOCON;
  }
  return ConfigTypeEnum.UNKNOWN;
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
        const classifier = x.classifier;
        const exclusions = x.exclusions || [];

        const id =
          classifier != null
            ? groupId + '_' + artifactId + '_' + classifier
            : groupId + '_' + artifactId;
        const mvnPom = {
          groupId: groupId,
          artifactId: artifactId,
          version: version,
          classifier: classifier,
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

export const filterOption = (input: string, options: Recordable) => {
  return options.label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
};

export function handleTeamResource(resource: string) {
  if (resource != null && resource !== '') {
    return JSON.parse(resource);
  }
  return [];
}

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
import { FailoverStrategyEnum, ReleaseStateEnum } from '/@/enums/flinkEnum';
import { DeployMode } from '/@/enums/sparkEnum';

/* Get diff editor configuration */
export const getMonacoOptions = (readOnly: boolean) => {
  return {
    selectOnLineNumbers: false,
    foldingStrategy: 'indentation', // code fragmentation
    overviewRulerBorder: false, // Don't scroll bar borders
    autoClosingBrackets: 'always',
    autoClosingDelete: 'always',
    tabSize: 2, // tab indent length
    readOnly,
    inherit: true,
    scrollBeyondLastLine: false,
    lineNumbersMinChars: 5,
    lineHeight: 24,
    automaticLayout: true,
    cursorBlinking: 'line',
    cursorStyle: 'line',
    cursorWidth: 3,
    renderFinalNewline: true,
    renderLineHighlight: 'all',
    quickSuggestionsDelay: 100, // Code prompt delay
    scrollbar: {
      useShadows: false,
      vertical: 'visible',
      horizontal: 'visible',
      horizontalSliderSize: 5,
      verticalSliderSize: 5,
      horizontalScrollbarSize: 15,
      verticalScrollbarSize: 15,
    },
  };
};

export const resolveOrder = [
  { label: 'parent-first', value: 0 },
  { label: 'child-first', value: 1 },
];

export const k8sRestExposedType = [
  { label: 'LoadBalancer', value: 0 },
  { label: 'ClusterIP', value: 1 },
  { label: 'NodePort', value: 2 },
];

export const deployModes = [
  { label: 'Standalone', value: DeployMode.REMOTE, disabled: false },
  { label: 'Yarn-Cluster', value: DeployMode.YARN_CLUSTER, disabled: false },
  { label: 'Yarn-Client', value: DeployMode.YARN_CLIENT, disabled: false },
];

export const cpTriggerAction = [
  { label: 'alert', value: FailoverStrategyEnum.ALERT },
  { label: 'restart', value: FailoverStrategyEnum.RESTART },
];

export const releaseTitleMap = {
  [ReleaseStateEnum.FAILED]: 'release failed',
  [ReleaseStateEnum.NEED_RELEASE]: 'current job need release',
  [ReleaseStateEnum.RELEASING]: 'releasing',
  [ReleaseStateEnum.NEED_RESTART]: 'release finished,need restart',
  [ReleaseStateEnum.NEED_ROLLBACK]: 'application is rollbacked,need release',
};

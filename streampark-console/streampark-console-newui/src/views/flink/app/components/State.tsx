/* 
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. 
*/

import { defineComponent, toRefs, unref } from 'vue';
import { Tag, Tooltip } from 'ant-design-vue';
import './State.less';

/*  state map*/
const stateMap = {
  0: { color: '#2f54eb', title: 'ADDED' },
  1: { color: '#738df8', title: 'INITIALIZING', class: 'status-processing-initializing' },
  2: { color: '#2f54eb', title: 'CREATED' },
  3: { color: '#1AB58E', title: 'STARTING', class: 'status-processing-starting' },
  4: { color: '#13c2c2', title: 'RESTARTING', class: 'status-processing-restarting' },
  5: { color: '#52c41a', title: 'RUNNING', class: 'status-processing-running' },
  6: { color: '#fa541c', title: 'FAILING', class: 'status-processing-failing' },
  7: { color: '#f5222d', title: 'FAILED' },
  8: { color: '#faad14', title: 'CANCELLING' },
  9: { color: '#fa8c16', title: 'CANCELED' },
  10: { color: '#1890ff', title: 'FINISHED' },
  19: { color: '#1890ff', title: 'FINISHED' },
  11: { color: '#722ed1', title: 'SUSPENDED' },
  12: { color: '#eb2f96', title: 'RECONCILING', class: 'status-processing-reconciling' },
  13: { color: '#000000', title: 'LOST' },
  14: { color: '#13c2c2', title: 'MAPPING', class: 'status-processing-restarting' },
  17: { color: '#738df8', title: 'SILENT', class: 'status-processing-initializing' },
  18: { color: '#8E50FF', title: 'TERMINATED' },
};
/*  option state map*/
const optionStateMap = {
  1: { color: '#1ABBDC', title: 'LAUNCHING', class: 'status-processing-deploying' },
  2: { color: '#faad14', title: 'CANCELLING', class: 'status-processing-cancelling' },
  3: { color: '#1AB58E', title: 'STARTING', class: 'status-processing-starting' },
  4: { color: '#faad14', title: 'SAVEPOINT', class: 'status-processing-cancelling' },
};

/* launch state map*/
const launchStateMap = {
  0: { color: '#52c41a', title: 'DONE' },
  1: { color: '#fa8c16', title: 'WAITING' },
  2: {
    color: '#52c41a',
    title: 'LAUNCHING',
    class: 'status-processing-deploying',
  },
  3: { color: '#fa8c16', title: 'PENDING' },
  4: { color: '#fa8c16', title: 'WAITING' },
};
launchStateMap[-1] = { color: '#f5222d', title: 'FAILED' };

/* build state map*/
const buildStatusMap = {
  0: { color: '#99A3A4', title: 'UNKNOWN' },
  1: { color: '#F5B041', title: 'PENDING' },
  2: {
    color: '#3498DB',
    title: 'BUILDING',
    class: 'status-processing-deploying',
  },
  3: { color: '#2ECC71', title: 'SUCCESS' },
  4: { color: '#E74C3C', title: 'FAILURE' },
};
const overviewMap = {
  running: { color: '#52c41a', title: 'RUNNING' },
  canceled: { color: '#fa8c16', title: 'CANCELED' },
  canceling: { color: '#faad14', title: 'CANCELING' },
  created: { color: '#2f54eb', title: 'CREATED' },
  deploying: { color: '#eb2f96', title: 'RECONCILING' },
  reconciling: { color: '#13c2c2', title: 'LAUNCHING' },
  scheduled: { color: '#722ed1', title: 'SCHEDULED' },
};

export default defineComponent({
  name: 'State',
  props: {
    option: {
      type: String,
      default: 'state',
    },
    data: {
      type: Object as PropType<Recordable>,
      default: () => ({}),
    },
  },
  setup(props) {
    const { data, option } = toRefs(props);
    const renderTag = (map: Recordable, key: number) => {
      if (!Reflect.has(map, key)) {
        return;
      }
      return <Tag {...map[key]}>{map[key].title}</Tag>;
    };

    const renderState = () => {
      if (unref(data).optionState === 0) {
        return <div class="app_state">{renderTag(stateMap, unref(data).state)}</div>;
      } else {
        return <div class="app_state">{renderTag(optionStateMap, unref(data).optionState)}</div>;
      }
    };
    function renderOverview() {
      if (!unref(data).overview) {
        return;
      }
      return Object.keys(overviewMap).map((k) => {
        if (unref(data)?.overview[k]) {
          const item = overviewMap[k];
          return (
            <Tooltip title={item.title}>
              <Tag color={item.color}>{unref(data)?.overview[k]}</Tag>
            </Tooltip>
          );
        } else {
          return;
        }
      });
    }
    const renderOtherOption = () => {
      if ([4, 5, 6].includes(unref(data)?.state || unref(data)?.optionState === 4)) {
        return (
          <div class="task-tag">
            {unref(data).totalTask && (
              <Tooltip title="TOTAL">
                <Tag color="#102541">{unref(data)?.totalTask}</Tag>
              </Tooltip>
            )}
            {renderOverview()}
          </div>
        );
      }
      return <div>-</div>;
    };

    return () => {
      if (unref(option) === 'state') {
        return <span>{renderState()}</span>;
      }

      if (unref(option) === 'launch') {
        return <span class="app_state">{renderTag(launchStateMap, unref(data).launch)}</span>;
      }

      if (unref(option) === 'build') {
        return <span class="app_state">{renderTag(buildStatusMap, unref(data).buildStatus)}</span>;
      }

      return <span>{renderOtherOption()}</span>;
    };
  },
});

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

import { computed, defineComponent, toRefs, unref } from 'vue';
import { Tag, Tooltip } from 'ant-design-vue';
import '../../../flink/app/components/State.less';
import { AppStateEnum, OptionStateEnum } from '/@/enums/sparkEnum';
import { ReleaseStateEnum } from '/@/enums/flinkEnum';
import { useI18n } from '/@/hooks/web/useI18n';
const { t } = useI18n();

/*  state map*/
export const stateMap = {
  [AppStateEnum.ADDED]: { color: '#477de9', title: t('spark.app.runState.added') },
  [AppStateEnum.NEW_SAVING]: {
    color: '#738df8',
    title: t('spark.app.runState.saving'),
    class: 'status-processing-initializing',
  },
  [AppStateEnum.NEW]: { color: '#2f54eb', title: t('spark.app.runState.new') },
  [AppStateEnum.STARTING]: {
    color: '#1AB58E',
    title: t('spark.app.runState.starting'),
    class: 'status-processing-starting',
  },
  [AppStateEnum.SUBMITTED]: {
    color: '#13c2c2',
    title: t('spark.app.runState.submitted'),
    class: 'status-processing-restarting',
  },
  [AppStateEnum.ACCEPTED]: {
    color: '#13c2c2',
    title: t('spark.app.runState.accept'),
    class: 'status-processing-restarting',
  },
  [AppStateEnum.SUCCEEDED]: {
    color: '#1890ff',
    title: t('spark.app.runState.success'),
    class: 'status-processing-success',
  },
  [AppStateEnum.RUNNING]: {
    color: '#52c41a',
    title: t('spark.app.runState.running'),
    class: 'status-processing-running',
  },
  [AppStateEnum.FINISHED]: { color: '#1890ff', title: t('spark.app.runState.finished') },
  [AppStateEnum.FAILED]: { color: '#f5222d', title: t('spark.app.runState.failed') },
  [AppStateEnum.LOST]: { color: '#333333', title: t('spark.app.runState.lost') },
  [AppStateEnum.MAPPING]: {
    color: '#13c2c2',
    title: t('spark.app.runState.mapping'),
    class: 'status-processing-restarting',
  },
  [AppStateEnum.OTHER]: { color: '#722ed1', title: t('spark.app.runState.other') },
  [AppStateEnum.REVOKED]: {
    color: '#eb2f96',
    title: t('spark.app.runState.revoked'),
    class: 'status-processing-reconciling',
  },

  [AppStateEnum.STOPPING]: {
    color: '#faad14',
    title: t('spark.app.runState.stopping'),
    class: 'status-processing-cancelling',
  },
  [AppStateEnum.KILLED]: { color: '#8E50FF', title: t('spark.app.runState.killed') },
};
/*  option state map*/
export const optionStateMap = {
  [OptionStateEnum.RELEASING]: {
    color: '#1ABBDC',
    title: t('spark.app.releaseState.releasing'),
    class: 'status-processing-deploying',
  },
  [OptionStateEnum.STOPPING]: {
    color: '#faad14',
    title: t('spark.app.runState.cancelling'),
    class: 'status-processing-cancelling',
  },
  [OptionStateEnum.STARTING]: {
    color: '#1AB58E',
    title: t('spark.app.runState.starting'),
    class: 'status-processing-starting',
  },
};

/* release state map*/
export const releaseStateMap = {
  [ReleaseStateEnum.FAILED]: { color: '#f5222d', title: t('spark.app.releaseState.failed') },
  [ReleaseStateEnum.DONE]: { color: '#52c41a', title: t('spark.app.releaseState.success') },
  [ReleaseStateEnum.NEED_RELEASE]: { color: '#fa8c16', title: t('spark.app.releaseState.waiting') },
  [ReleaseStateEnum.RELEASING]: {
    color: '#52c41a',
    title: t('spark.app.releaseState.releasing'),
    class: 'status-processing-deploying',
  },
  [ReleaseStateEnum.NEED_RESTART]: { color: '#fa8c16', title: t('spark.app.releaseState.pending') },
  [ReleaseStateEnum.NEED_ROLLBACK]: {
    color: '#fa8c16',
    title: t('spark.app.releaseState.waiting'),
  },
};

/* build state map*/
export const buildStatusMap = {
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
  reconciling: { color: '#13c2c2', title: 'RELEASING' },
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
    maxTitle: String,
  },
  setup(props) {
    const { data, option } = toRefs(props);

    const tagWidth = computed(() => {
      if (props.maxTitle === undefined) return 0;
      // create a dom to calculate the width of the tag
      const dom = document.createElement('span');
      dom.style.display = 'inline-block';
      dom.style.fontSize = '10px';
      dom.style.padding = '0 2px';
      dom.style.borderRadius = '2px';
      dom.textContent = props.maxTitle;
      document.body.appendChild(dom);
      const width = dom.clientWidth + 2;
      document.body.removeChild(dom);
      return width;
    });

    const renderTag = (map: Recordable, key: number) => {
      if (!Reflect.has(map, key)) {
        return;
      }
      return (
        <Tag {...map[key]} style={getStyle.value}>
          {map[key].title}
        </Tag>
      );
    };

    const getStyle = computed(() => {
      if (tagWidth.value > 0) {
        return { width: `${tagWidth.value}px`, textAlign: 'center' };
      }
      return {};
    });
    const renderState = () => {
      if (unref(data).optionState === OptionStateEnum.NONE) {
        return <div class="bold-tag">{renderTag(stateMap, unref(data).state)}</div>;
      } else {
        return <div class="bold-tag">{renderTag(optionStateMap, unref(data).optionState)}</div>;
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
      if ([AppStateEnum.RUNNING, AppStateEnum.FAILED].includes(unref(data)?.state)) {
        return (
          <div class="bold-tag">
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

      if (unref(option) === 'release') {
        return <span class="bold-tag">{renderTag(releaseStateMap, unref(data).release)}</span>;
      }

      if (unref(option) === 'build') {
        return <span class="bold-tag">{renderTag(buildStatusMap, unref(data).buildStatus)}</span>;
      }

      return <span>{renderOtherOption()}</span>;
    };
  },
});

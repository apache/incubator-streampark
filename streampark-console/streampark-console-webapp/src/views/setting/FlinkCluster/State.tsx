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
import { Tag } from 'ant-design-vue';
import './State.less';
import { ClusterStateEnum } from '/@/enums/flinkEnum';

/*  state map*/
const stateMap = {
  [ClusterStateEnum.CREATED]: { color: '#2f54eb', title: 'CREATED' },
  [ClusterStateEnum.STARTED]: {
    color: '#52c41a',
    title: 'RUNNING',
    class: 'status-processing-running',
  },
  [ClusterStateEnum.CANCELED]: { color: '#fa8c16', title: 'CANCELED' },
  [ClusterStateEnum.LOST]: { color: '#99A3A4', title: 'LOST' },
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
    const { data } = toRefs(props);
    const renderTag = (map: Recordable, key: number) => {
      if (!Reflect.has(map, key)) {
        return;
      }
      return <Tag {...map[key]}>{map[key].title}</Tag>;
    };

    const renderState = () => {
      return <div class="bold-tag">{renderTag(stateMap, unref(data).clusterState)}</div>;
    };

    return () => {
      return <span>{renderState()}</span>;
    };
  },
});

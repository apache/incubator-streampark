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

import { AppListRecord } from '/@/api/flink/app/app.type';
import { ActionItem } from '/@/components/Table';

export const useAppTable = () => {
  /* Operation button */
  function getTableActions(record: AppListRecord): ActionItem[] {
    return [
      {
        tooltip: { title: 'Edit Application' },
        auth: 'app:update',
        icon: 'clarity:note-edit-line',
        onClick: handleEdit.bind(null, record),
      },
      {
        tooltip: { title: 'Launch Application' },
        ifShow: [-1, 1, 4].includes(record.launch) && record['optionState'] === 0,
        icon: 'ant-design:cloud-upload-outlined',
        onClick: handleCheckLaunchApp.bind(null, record),
      },
      {
        tooltip: { title: 'Launching Progress Detail' },
        ifShow: [-1, 2].includes(record.launch) || record['optionState'] === 1,
        auth: 'app:update',
        icon: 'ant-design:container-outlined',
        onClick: openBuildProgressDetailDrawer.bind(null, record),
      },
      {
        tooltip: { title: 'Start Application' },
        ifShow: handleIsStart(record, optionApps),
        auth: 'app:start',
        icon: 'ant-design:play-circle-outlined',
        onClick: handleAppCheckStart.bind(null, record),
      },
      {
        tooltip: { title: 'Cancel Application' },
        ifShow: record.state === 5 && record['optionState'] === 0,
        auth: 'app:cancel',
        icon: 'ant-design:pause-circle-outlined',
        onClick: handleCancel.bind(null, record),
      },
      {
        tooltip: { title: 'View Application Detail' },
        auth: 'app:detail',
        icon: 'ant-design:eye-outlined',
        onClick: handleDetail.bind(null, record),
      },
      {
        tooltip: { title: 'See Flink Start log' },
        ifShow: [5, 6].includes(record.executionMode),
        auth: 'app:detail',
        icon: 'ant-design:sync-outlined',
        onClick: handleSeeLog.bind(null, record),
      },
      {
        tooltip: { title: 'Forced Stop Application' },
        ifShow: handleCanStop(record),
        auth: 'app:cancel',
        icon: 'ant-design:pause-circle-outlined',
        onClick: handleForcedStop.bind(null, record),
      },
    ];
  }
  /* Click to edit */
  function handleEdit(app: AppListRecord) {
    flinkAppStore.setApplicationId(app.id);
    if (app.appType === 1) {
      // jobType( 1 custom code 2: flinkSQL)
      router.push({ path: '/flink/app/edit_streampark', query: { appId: app.id } });
    } else if (app.appType === 2) {
      //Apache Flink
      router.push({ path: '/flink/app/edit_flink' });
    }
  }
  /* pull down button */
  function getActionDropdown(record: AppListRecord): ActionItem[] {
    return [
      {
        label: 'Copy Application',
        auth: 'app:copy',
        icon: 'ant-design:copy-outlined',
        onClick: handleCopy.bind(null, record),
      },
      {
        label: 'Remapping Application',
        ifShow: [0, 7, 10, 11, 13].includes(record.state),
        auth: 'app:mapping',
        icon: 'ant-design:deployment-unit-outlined',
        onClick: handleMapping.bind(null, record),
      },
      {
        label: 'View FlameGraph',
        ifShow: record.flameGraph,
        auth: 'app:flameGraph',
        icon: 'ant-design:fire-outlined',
        onClick: handleFlameGraph.bind(null, record),
      },
      {
        popConfirm: {
          title: 'Are you sure delete this job ?',
          confirm: handleDelete.bind(null, record),
        },
        label: 'Delete',
        ifShow: [0, 7, 9, 10, 13, 18, 19].includes(record.state),
        auth: 'app:delete',
        icon: 'ant-design:delete-outlined',
        color: 'error',
      },
    ];
  }
  return {};
};

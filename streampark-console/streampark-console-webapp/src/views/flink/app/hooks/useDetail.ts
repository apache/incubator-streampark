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
import { fetchRemoveSavePoint } from '/@/api/flink/app/savepoint';
import { ActionItem } from '/@/components/Table';
import { useMessage } from '/@/hooks/web/useMessage';
import { useClipboard } from '@vueuse/core';
import { fetchRemoveConf } from '/@/api/flink/config';

export const userDetail = (
  getDataSource: Fn,
  reloadSavePoint: Fn,
  reloadConf: Fn,
  handleConfDetail: Fn,
  openCompareModal: Fn,
) => {
  const { createMessage } = useMessage();

  const { copy } = useClipboard({
    legacy: true,
  });

  function getConfAction(record: Recordable): ActionItem[] {
    return [
      {
        tooltip: { title: 'View Config Detail' },
        shape: 'circle',
        icon: 'ant-design:eye-outlined',
        onClick: handleConfDetail.bind(null, record),
      },
      {
        tooltip: { title: 'Compare Config' },
        shape: 'circle',
        icon: 'ant-design:swap-outlined',
        onClick: handleCompare.bind(null, record),
        ifShow: getDataSource().length > 1,
      },
      {
        popConfirm: {
          title: 'Are you sure delete this record ',
          confirm: handleDeleteConf.bind(null, record),
        },
        auth: 'conf:delete',
        shape: 'circle',
        icon: 'ant-design:delete-outlined',
        type: 'danger' as any,
        ifShow: !record.effective,
      },
    ];
  }

  /* delete configuration */
  async function handleDeleteConf(record: Recordable) {
    await fetchRemoveConf({ id: record.id });
    reloadConf();
  }

  function handleCompare(record: Recordable) {
    openCompareModal(true, {
      id: record.id,
      version: record.version,
      createTime: record.createTime,
    });
  }

  function getSavePointAction(record: Recordable): ActionItem[] {
    return [
      {
        tooltip: { title: 'Copy Path' },
        shape: 'circle',
        icon: 'ant-design:copy-outlined',
        onClick: handleCopy.bind(null, record),
      },
      {
        popConfirm: {
          title: 'Are you sure delete?',
          confirm: handleDeleteSavePoint.bind(null, record),
        },
        shape: 'circle',
        icon: 'ant-design:delete-outlined',
        type: 'danger' as any,
      },
    ];
  }

  /* copy path */
  function handleCopy(record: Recordable) {
    try {
      copy(record.path);
      createMessage.success('copied to clipboard successfully');
    } catch (error) {
      console.error(error);
      createMessage.error('copied to clipboard failed');
    }
  }
  /* delete savePoint */
  async function handleDeleteSavePoint(record: Recordable) {
    await fetchRemoveSavePoint({ id: record.id });
    reloadSavePoint();
  }

  return { getConfAction, getSavePointAction };
};

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

  const { copy } = useClipboard();

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

  /* 删除配置 */
  async function handleDeleteConf(record) {
    await fetchRemoveConf({ id: record.id });
    reloadConf();
  }

  function handleCompare(record) {
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

  /* 复制路径 */
  function handleCopy(record: Recordable) {
    try {
      copy(record.path);
      createMessage.success('copied to clipboard successfully');
    } catch (error) {
      console.error(error);
      createMessage.error('copied to clipboard failed');
    }
  }
  /* 删除 savePoint */
  async function handleDeleteSavePoint(record) {
    await fetchRemoveSavePoint({ id: record.id });
    reloadSavePoint();
  }

  return { getConfAction, getSavePointAction };
};

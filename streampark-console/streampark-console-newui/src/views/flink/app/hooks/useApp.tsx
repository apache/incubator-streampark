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
import { Alert, Form, Input, Tag } from 'ant-design-vue';
import { h, onMounted, ref, reactive, VNode, unref } from 'vue';
import { handleAppBuildStatueText } from '../utils';
import { fetchCopy, fetchForcedStop, fetchMapping } from '/@/api/flink/app/app';
import { fetchBuild, fetchBuildDetail } from '/@/api/flink/app/flinkBuild';
import { fetchLatest, fetchSavePonitHistory } from '/@/api/flink/app/savepoint';
import { getUserList } from '/@/api/sys/user';
import { SvgIcon } from '/@/components/Icon';
import { useMessage } from '/@/hooks/web/useMessage';

export const useFlinkApplication = (openStartModal: Fn) => {
  const { createConfirm, createMessage, createWarningModal } = useMessage();
  const users = ref<Recordable>([]);
  const appBuildDetail = reactive<Recordable>({});
  const historySavePoint = ref<any>([]);
  const optionApps = {
    starting: new Map(),
    stopping: new Map(),
    launch: new Map(),
  };

  /* check */
  function handleCheckLaunchApp(app) {
    if (app['appControl']['allowBuild'] === true) {
      handleLaunchApp(app, false);
    } else {
      createWarningModal({
        title: 'WARNING',
        content: `
          <p>The current launch of the application is in progress.</p>
          <p>are you sure you want to force another build?</p>
        `,
        okType: 'danger',
        onOk: () => handleLaunchApp(app, true),
      });
    }
  }

  /* Launch App */
  async function handleLaunchApp(app, force: boolean) {
    const { data } = await fetchBuild({
      appId: app.id,
      forceBuild: force,
    });
    if (!data.data) {
      createConfirm({
        iconType: 'error',
        title: 'Failed',
        content:
          'lanuch application failed, ' + (data.message || '').replaceAll(/\[StreamPark]/g, ''),
      });
    } else {
      createMessage.success('Current Application is launching');
    }
  }

  /* start application */
  function handleAppCheckStart(app) {
    // when then app is building, show forced starting modal
    if (app['appControl']['allowStart'] === false) {
      handleFetchBuildDetail(app);
      createWarningModal({
        title: 'WARNING',
        content: () => {
          const content: Array<VNode> = [];
          if (appBuildDetail.pipeline == null) {
            content.push(h('p', null, 'No build record exists for the current application.'));
          } else {
            content.push(
              h('p', null, [
                'The current build state of the application is',
                h(
                  Tag,
                  { color: 'orange' },
                  handleAppBuildStatueText(appBuildDetail.pipeline.pipeStatus),
                ),
              ]),
            );
          }
          content.push(h('p', null, 'Are you sure to force the application to run?'));
          return content;
        },
        okType: 'danger',
        onOk: () => {
          handleStart(app);
          return Promise.resolve(true);
        },
      });
    } else {
      handleStart(app);
    }
  }

  async function handleStart(app) {
    if (app.flinkVersion == null) {
      createMessage.error('please set flink version first.');
    } else {
      if (!optionApps.starting.get(app.id) || app['optionState'] === 0) {
        const res = await fetchLatest({
          appId: app.id,
        });
        if (!res) {
          const resp = await fetchSavePonitHistory({
            appId: app.id,
            pageNum: 1,
            pageSize: 9999,
          });
          historySavePoint.value = resp.records.filter((x) => x.path);
        }
        openStartModal(true, {
          latestSavePoint: res,
          executionMode: app.executionMode,
          application: app,
          historySavePoint: historySavePoint.value,
        });
      }
    }
  }

  async function handleFetchBuildDetail(app) {
    const res = await fetchBuildDetail({
      appId: app.id,
    });
    appBuildDetail.pipeline = res.pipeline;
    appBuildDetail.docker = res.docker;
  }

  function handleCanStop(app) {
    const optionTime = new Date(app['optionTime']).getTime();
    const nowTime = new Date().getTime();
    if (nowTime - optionTime >= 60 * 1000) {
      const state = app['optionState'];
      if (state === 0) {
        return [3, 4, 8].includes(app.state) || false;
      }
      return true;
    }
    return false;
  }
  function handleForcedStop(app) {
    let option = 'starting';
    const optionState = app['optionState'];
    const stateMap = { 3: 'starting', 4: 'restarting', 8: 'cancelling' };
    const optionStateMap = { 1: 'launching', 2: 'cancelling', 3: 'starting', 4: 'savePointing' };
    if (optionState === 0) {
      option = stateMap[app.state];
    } else {
      option = optionStateMap[optionState];
    }
    createConfirm({
      iconType: 'warning',
      title: 'Are you sure?',
      content: `current job is ${option}, are you sure forced stop?`,
      okText: 'Yes, forced stop!',
      cancelText: 'No, cancel',
      onOk: async () => {
        const res = await fetchForcedStop({
          id: app.id,
        });
        if (res) {
          createMessage.success('forced stopping');
        }
        return Promise.resolve();
      },
    });
  }

  /* copy application */
  function handleCopy(item) {
    const validateStatus = ref<'' | 'error' | 'validating' | 'success' | 'warning'>('');
    let help = '';
    let copyAppName: string | undefined = '';
    createConfirm({
      width: '600px',
      iconType: 'info',
      title: () => [
        h(SvgIcon, { name: 'copy', style: { color: 'red', display: 'inline-block' } }),
        'Copy Application',
      ],
      content: () => {
        return (
          <Form>
            <Form.Item
              label="Application Name"
              labelCol={{ lg: { span: 7 }, sm: { span: 7 } }}
              wrapperCol={{ lg: { span: 16 }, sm: { span: 4 } }}
              validateStatus={unref(validateStatus)}
              help={help}
              rules={[{ required: true }]}
            >
              <Input
                type="text"
                placeholder="New Application Name"
                onInput={(e) => {
                  copyAppName = e.target.value;
                }}
              ></Input>
            </Form.Item>
          </Form>
        );
      },
      okText: 'Apply',
      cancelText: 'Close',
      onOk: async () => {
        if (!copyAppName) {
          validateStatus.value = 'error';
          help = 'Sorry, Application Name cannot be empty';
          console.log(validateStatus);
          return Promise.reject('error');
        }
        try {
          const { data } = await fetchCopy({
            id: item.id,
            jobName: copyAppName,
          });
          const status = data.status || 'error';
          if (status === 'success') {
            createMessage.success('copy successful');
          } else {
            createMessage.error(data.message || 'copy failed');
          }
        } catch (error: any) {
          if (error?.response?.data?.message) {
            createMessage.error(
              error.response.data.message
                .replaceAll(/\[StreamPark\]/g, '')
                .replaceAll(/\[StreamX\]/g, '') || 'copy failed',
            );
          }
          return Promise.reject();
        }
      },
    });
  }

  /* mapping */
  function handleMapping(app) {
    const mappingRef = ref();
    const formValue = reactive<any>({});
    createConfirm({
      width: '600px',
      iconType: 'info',
      title: () => [
        h(SvgIcon, { name: 'mapping', style: { color: 'green', display: 'inline-block' } }),
        'Mapping Application',
      ],
      content: () => {
        return (
          <Form
            class="!mt-20px"
            ref={mappingRef}
            name="mappingForm"
            labelCol={{ lg: { span: 7 }, sm: { span: 7 } }}
            wrapperCol={{ lg: { span: 16 }, sm: { span: 4 } }}
            v-model:model={formValue}
          >
            <Form.Item label="Application Name">
              <Alert message={app.jobName} type="info" />
            </Form.Item>
            <Form.Item
              label="Application Id"
              name="appId"
              rules={[{ required: true, message: 'ApplicationId is required' }]}
            >
              <Input type="text" placeholder="ApplicationId" v-model:value={formValue.appId} />
            </Form.Item>
            <Form.Item
              label="JobId"
              name="jobId"
              rules={[{ required: true, message: 'ApplicationId is required' }]}
            >
              <Input type="text" placeholder="JobId" v-model:value={formValue.jobId} />
            </Form.Item>
          </Form>
        );
      },
      okText: 'Apply',
      cancelText: 'Close',
      onOk: async () => {
        try {
          await mappingRef.value.validate();
          console.log(formValue);
          await fetchMapping({
            id: app.id,
            appId: formValue.appId,
            jobId: formValue.jobId,
          });
          createMessage.success('The current job is mapping');
          return Promise.resolve();
        } catch (error) {
          return Promise.reject(error);
        }
      },
    });
  }

  onMounted(async () => {
    const res = await getUserList({ pageSize: '9999' });
    users.value = res.records;
  });

  return {
    handleCheckLaunchApp,
    handleAppCheckStart,
    handleCanStop,
    handleForcedStop,
    handleCopy,
    handleMapping,
    users,
  };
};

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
import { h, onMounted, reactive, ref, unref, VNode } from 'vue';
import { handleAppBuildStatueText } from '../utils';
import { fetchCheckName, fetchCopy, fetchAbort, fetchMapping } from '/@/api/flink/app';
import { fetchBuild, fetchBuildDetail } from '/@/api/flink/flinkBuild';
import { fetchSavePointHistory } from '/@/api/flink/savepoint';
import { fetchAppOwners } from '/@/api/system/user';
import { SvgIcon } from '/@/components/Icon';
import { AppStateEnum, DeployMode, OptionStateEnum } from '/@/enums/flinkEnum';
import { useI18n } from '/@/hooks/web/useI18n';
import { useMessage } from '/@/hooks/web/useMessage';

export const useFlinkApplication = (openStartModal: Fn) => {
  const { t } = useI18n();
  const { Swal, createConfirm, createMessage, createWarningModal } = useMessage();
  const users = ref<Recordable>([]);
  const appBuildDetail = reactive<Recordable>({});
  const historySavePoint = ref<any>([]);
  const optionApps = {
    starting: new Map(),
    stopping: new Map(),
    release: new Map(),
    savepointing: new Map(),
  };

  /* check */
  function handleCheckReleaseApp(app: Recordable) {
    if (app['appControl']['allowBuild'] === true) {
      handleReleaseApp(app, false);
    } else {
      createWarningModal({
        title: 'WARNING',
        content: `
          <p class="pt-10px">${t('flink.app.release.releaseTitle')}</p>
          <p>${t('flink.app.release.releaseDesc')}</p>
        `,
        okType: 'danger',
        onOk: () => handleReleaseApp(app, true),
      });
    }
  }

  /* Release App */
  async function handleReleaseApp(app: Recordable, force: boolean) {
    const { data } = await fetchBuild({
      appId: app.id,
      forceBuild: force,
    });
    if (!data.data) {
      let message = data.message || '';
      if (!message) {
        message = t('flink.app.release.releaseFail') + message.replaceAll(/\[StreamPark]/g, '');
      }
      Swal.fire('Failed', message, 'error');
    } else {
      Swal.fire({
        icon: 'success',
        title: t('flink.app.release.releasing'),
        showConfirmButton: false,
        timer: 2000,
      });
    }
  }

  /* start application */
  function handleAppCheckStart(app: Recordable) {
    // when then app is building, show forced starting modal
    if (app['appControl']['allowStart'] === false) {
      handleFetchBuildDetail(app);
      createWarningModal({
        title: 'WARNING',
        content: () => {
          const content: Array<VNode> = [];
          if (appBuildDetail.pipeline == null) {
            content.push(
              h('p', { class: 'pt-10px' }, 'No build record exists for the current application.'),
            );
          } else {
            content.push(
              h('p', { class: 'pt-10px' }, [
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

  async function handleStart(app: Recordable) {
    if (app.flinkVersion == null) {
      Swal.fire('Failed', 'please set flink version first.', 'error');
    } else {
      if (!optionApps.starting.get(app.id) || app['optionState'] === OptionStateEnum.NONE) {
        const resp = await fetchSavePointHistory({
          appId: app.id,
          pageNum: 1,
          pageSize: 9999,
        });
        historySavePoint.value = resp.records.filter((x: Recordable) => x.path);
        const latest = resp.records.find((x: Recordable) => x.latest) || null;
        openStartModal(true, {
          deployMode: app.deployMode,
          application: app,
          historySavePoint: historySavePoint.value,
          selected: latest,
        });
      }
    }
  }

  async function handleFetchBuildDetail(app: Recordable) {
    const res = await fetchBuildDetail({
      appId: app.id,
    });
    appBuildDetail.pipeline = res.pipeline;
    appBuildDetail.docker = res.docker;
  }

  function handleCanStop(app: Recordable) {
    const optionTime = new Date(app['optionTime']).getTime();
    const nowTime = new Date().getTime();
    if (nowTime - optionTime >= 60 * 1000) {
      const state = app['optionState'];
      if (state === OptionStateEnum.NONE) {
        return (
          [
            AppStateEnum.INITIALIZING,
            AppStateEnum.STARTING,
            AppStateEnum.RESTARTING,
            AppStateEnum.CANCELLING,
            AppStateEnum.RECONCILING,
            AppStateEnum.MAPPING,
          ].includes(app.state) || false
        );
      }
      return true;
    }
    return false;
  }
  function handleAbort(app: Recordable) {
    let option = 'starting';
    const optionState = app['optionState'];
    const stateMap = {
      [AppStateEnum.STARTING]: 'starting',
      [AppStateEnum.RESTARTING]: 'restarting',
      [AppStateEnum.CANCELLING]: 'cancelling',
    };
    const optionStateMap = {
      [OptionStateEnum.RELEASING]: 'releasing',
      [OptionStateEnum.CANCELLING]: 'cancelling',
      [OptionStateEnum.STARTING]: 'starting',
      [OptionStateEnum.SAVEPOINTING]: 'savePointing',
    };
    if (optionState === OptionStateEnum.NONE) {
      option = stateMap[app.state];
    } else {
      option = optionStateMap[optionState];
    }
    Swal.fire({
      title: 'Are you sure?',
      text: `current job is ${option}, are you sure abort the job?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Yes, abort job!',
      denyButtonText: `No, cancel`,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
    }).then(async (result) => {
      if (result.isConfirmed) {
        Swal.fire('abort job', '', 'success');
        const res = await fetchAbort({
          id: app.id,
        });
        if (res) {
          createMessage.success('abort job starting');
        }
        return Promise.resolve();
      }
    });
  }

  /* copy application */
  function handleCopy(item) {
    const validateStatus = ref<'' | 'error' | 'validating' | 'success' | 'warning'>('');
    let help = '';
    let copyAppName: string | undefined = '';
    createConfirm({
      width: '600px',
      title: () => [
        h(SvgIcon, {
          name: 'copy',
          style: { color: 'red', display: 'inline-block', marginRight: '10px' },
        }),
        'Copy Application',
      ],
      content: () => {
        return (
          <Form class="!pt-50px" layout="vertical" baseColProps={{ span: 22, offset: 1 }}>
            <Form.Item
              label="Job Name"
              validateStatus={unref(validateStatus)}
              help={help}
              rules={[{ required: true }]}
            >
              <Input
                type="text"
                placeholder="New Job Name"
                onInput={(e) => {
                  copyAppName = e.target.value;
                }}
              ></Input>
            </Form.Item>
          </Form>
        );
      },
      okText: t('common.apply'),
      cancelText: t('common.closeText'),
      onOk: async () => {
        //1) check empty
        if (copyAppName == null) {
          validateStatus.value = 'error';
          help = 'Sorry, Job Name cannot be empty';
          return Promise.reject('copy application error');
        }
        //2) check name
        const params = { jobName: copyAppName };
        const resp = await fetchCheckName(params);
        const code = parseInt(resp);
        if (code === 0) {
          try {
            const { data } = await fetchCopy({
              id: item.id,
              jobName: copyAppName,
            });
            const status = data.status || 'error';
            if (status === 'success') {
              Swal.fire({
                icon: 'success',
                title: 'copy successful',
                timer: 1500,
              });
            }
          } catch (error: any) {
            if (error?.response?.data?.message) {
              createMessage.error(
                error.response.data.message
                  .replaceAll(/\[StreamPark\]/g, '')
                  .replaceAll(/\[StreamPark\]/g, '') || 'copy failed',
              );
            }
            return Promise.reject('copy application error');
          }
        } else {
          validateStatus.value = 'error';
          if (code === 1) {
            help = t('flink.app.addAppTips.appNameNotUniqueMessage');
          } else if (code === 2) {
            help = t('flink.app.addAppTips.appNameExistsInYarnMessage');
          } else if (code === 3) {
            help = t('flink.app.addAppTips.appNameExistsInK8sMessage');
          } else {
            help = t('flink.app.addAppTips.appNameNotValid');
          }
          return Promise.reject('copy application error');
        }
      },
    });
  }

  /* mapping */
  function handleMapping(app: Recordable) {
    const mappingRef = ref();
    const formValue = reactive<any>({});
    createConfirm({
      width: '600px',
      title: () => [
        h(SvgIcon, {
          name: 'mapping',
          style: { color: 'green', display: 'inline-block', marginRight: '10px' },
        }),
        'Mapping Application',
      ],
      content: () => {
        return (
          <Form
            class="!pt-40px"
            ref={mappingRef}
            name="mappingForm"
            layout="vertical"
            baseColProps={{ span: 22, offset: 1 }}
            v-model:model={formValue}
          >
            <Form.Item label="Job Name">
              <Alert message={app.jobName} type="info" />
            </Form.Item>
            {[
              DeployMode.YARN_PER_JOB,
              DeployMode.YARN_SESSION,
              DeployMode.YARN_APPLICATION,
            ].includes(app.deployMode) && (
              <Form.Item
                label="YARN Application Id"
                name="clusterId"
                rules={[{ required: true, message: 'YARN ApplicationId is required' }]}
              >
                <Input
                  type="text"
                  placeholder="ApplicationId"
                  v-model:value={formValue.clusterId}
                />
              </Form.Item>
            )}
            <Form.Item
              label="JobId"
              name="jobId"
              rules={[{ required: true, message: 'jobId is required' }]}
            >
              <Input type="text" placeholder="JobId" v-model:value={formValue.jobId} />
            </Form.Item>
          </Form>
        );
      },
      okText: t('common.apply'),
      cancelText: t('common.closeText'),
      onOk: async () => {
        try {
          await mappingRef.value.validate();
          await fetchMapping({
            id: app.id,
            clusterId: formValue.clusterId,
            jobId: formValue.jobId,
          });
          Swal.fire({
            icon: 'success',
            title: 'The current job is mapping',
            showConfirmButton: false,
            timer: 2000,
          });
          return Promise.resolve();
        } catch (error) {
          return Promise.reject(error);
        }
      },
    });
  }

  onMounted(() => {
    fetchAppOwners({}).then((res) => {
      users.value = res;
    });
  });

  return {
    handleCheckReleaseApp,
    handleAppCheckStart,
    handleCanStop,
    handleAbort,
    handleCopy,
    handleMapping,
    users,
  };
};

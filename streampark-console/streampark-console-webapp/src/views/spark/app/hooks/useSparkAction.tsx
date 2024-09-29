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
import {
  fetchCheckSparkAppStart,
  fetchCheckSparkName,
  fetchCopySparkApp,
  fetchSparkAppForcedStop,
  fetchSparkAppStart,
  fetchSparkMapping,
} from '/@/api/spark/app';
import { fetchAppOwners } from '/@/api/system/user';
import { SvgIcon } from '/@/components/Icon';
import { AppExistsStateEnum, AppStateEnum, DeployMode, OptionStateEnum } from '/@/enums/sparkEnum';
import { useI18n } from '/@/hooks/web/useI18n';
import { useMessage } from '/@/hooks/web/useMessage';
import { fetchBuildSparkApp, fetchBuildProgressDetail } from '/@/api/spark/build';
import type { SparkApplication } from '/@/api/spark/app.type';
import { exceptionPropWidth } from '/@/utils';
import { useRouter } from 'vue-router';

export const useSparkAction = (optionApps: Recordable) => {
  const { t } = useI18n();
  const router = useRouter();
  const { Swal, createConfirm, createMessage, createWarningModal } = useMessage();
  const users = ref<Recordable>([]);
  const appBuildDetail = reactive<Recordable>({});

  /* check */
  function handleCheckReleaseApp(app: Recordable) {
    if (app['appControl']['allowBuild'] === true) {
      handleReleaseApp(app, false);
    } else {
      createWarningModal({
        title: 'WARNING',
        content: `
          <p class="pt-10px">${t('spark.app.release.releaseTitle')}</p>
          <p>${t('spark.app.release.releaseDesc')}</p>
        `,
        okType: 'danger',
        onOk: () => handleReleaseApp(app, true),
      });
    }
  }

  /* Release App */
  async function handleReleaseApp(app: Recordable, force: boolean) {
    const res = await fetchBuildSparkApp({
      appId: app.id,
      forceBuild: force,
    });
    if (!res.data) {
      let message = res.message || '';
      if (!message) {
        message = t('spark.app.release.releaseFail') + message.replaceAll(/\[StreamPark]/g, '');
      }
      await Swal.fire('Failed', message, 'error');
    } else {
      await Swal.fire({
        icon: 'success',
        title: t('spark.app.release.releasing'),
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

  async function handleStart(app: SparkApplication) {
    if (app.sparkVersion == null) {
      Swal.fire('Failed', 'please set spark version first.', 'error');
    } else {
      if (!optionApps.starting.get(app.id) || app['optionState'] === OptionStateEnum.NONE) {
        // when then app is building, show forced starting modal
        const resp = await fetchCheckSparkAppStart({
          id: app.id,
        });
        if (+resp === AppExistsStateEnum.IN_YARN) {
          await fetchSparkAppForcedStop({
            id: app.id,
          });
        }
        await handleDoSubmit(app);
      }
    }
  }
  /* submit */
  async function handleDoSubmit(data: SparkApplication) {
    try {
      const res = await fetchSparkAppStart({
        id: data.id,
      });
      if (res.data) {
        Swal.fire({
          icon: 'success',
          title: t('spark.app.operation.starting'),
          showConfirmButton: false,
          timer: 2000,
        });
        optionApps.starting.set(data.id, new Date().getTime());
      } else {
        Swal.fire({
          title: 'Failed',
          icon: 'error',
          width: exceptionPropWidth(),
          html:
            '<pre class="api-exception"> startup failed, ' +
            res.message.replaceAll(/\[StreamPark]/g, '') +
            '</pre>',
          showCancelButton: true,
          confirmButtonColor: '#55BDDDFF',
          confirmButtonText: 'Detail',
          cancelButtonText: 'Close',
        }).then((isConfirm: Recordable) => {
          if (isConfirm.value) {
            router.push({
              path: '/spark/app/detail',
              query: { appId: data.id },
            });
          }
        });
      }
    } catch (error) {
      console.error(error);
    }
  }
  async function handleFetchBuildDetail(app: Recordable) {
    const res = await fetchBuildProgressDetail(app.id);
    appBuildDetail.pipeline = res.pipeline;
    appBuildDetail.docker = res.docker;
  }

  function handleCanStop(app: Recordable) {
    const optionTime = new Date(app['optionTime']).getTime();
    const nowTime = new Date().getTime();
    if (nowTime - optionTime >= 60 * 1000) {
      const state = app['optionState'];
      if (state === OptionStateEnum.NONE) {
        return [AppStateEnum.STARTING, AppStateEnum.MAPPING].includes(app.state) || false;
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
    };
    const optionStateMap = {
      [OptionStateEnum.RELEASING]: 'releasing',
      [OptionStateEnum.STARTING]: 'starting',
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
        const res = await fetchSparkAppForcedStop({
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
        const params = { appName: copyAppName };
        const resp = await fetchCheckSparkName(params);
        const code = parseInt(resp);
        if (code === 0) {
          try {
            const res = await fetchCopySparkApp({
              id: item.id,
              appName: copyAppName,
            });
            const status = res.status || 'error';
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
            help = t('spark.app.addAppTips.appNameNotUniqueMessage');
          } else if (code === 2) {
            help = t('spark.app.addAppTips.appNameExistsInYarnMessage');
          } else if (code === 3) {
            help = t('spark.app.addAppTips.appNameExistsInK8sMessage');
          } else {
            help = t('spark.app.addAppTips.appNameNotValid');
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
              <Alert message={app.appName} type="info" />
            </Form.Item>
            {[DeployMode.YARN_CLIENT, DeployMode.YARN_CLUSTER].includes(app.deployMode) && (
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
          await fetchSparkMapping({
            id: app.id,
            clusterId: formValue.clusterId,
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

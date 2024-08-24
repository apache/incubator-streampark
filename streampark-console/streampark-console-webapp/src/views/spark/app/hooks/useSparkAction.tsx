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
import { Alert, Form, Input } from 'ant-design-vue';
import { h, onMounted, reactive, ref, unref } from 'vue';
import { fetchAppOwners } from '/@/api/system/user';
import { SvgIcon } from '/@/components/Icon';
import { AppExistsStateEnum } from '/@/enums/sparkEnum';
import { useI18n } from '/@/hooks/web/useI18n';
import { useMessage } from '/@/hooks/web/useMessage';
import { fetchCheckSparkName, fetchCopySparkApp, fetchSparkMapping } from '/@/api/spark/app';

export const useSparkAction = () => {
  const { t } = useI18n();
  const { Swal, createConfirm, createMessage } = useMessage();
  const users = ref<Recordable>([]);

  /* copy application */
  function handleCopy(item: Recordable) {
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
          <Form class="!pt-50px">
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
      okText: t('common.apply'),
      cancelText: t('common.closeText'),
      onOk: async () => {
        //1) check empty
        if (copyAppName == null) {
          validateStatus.value = 'error';
          help = 'Sorry, Application Name cannot be empty';
          return Promise.reject('copy application error');
        }
        //2) check name
        const params = { jobName: copyAppName };
        const resp = await fetchCheckSparkName(params);
        const code = parseInt(resp);
        if (code === AppExistsStateEnum.NO) {
          try {
            const { data } = await fetchCopySparkApp({
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
          if (code === AppExistsStateEnum.IN_DB) {
            help = t('flink.app.addAppTips.appNameNotUniqueMessage');
          } else if (code === AppExistsStateEnum.IN_YARN) {
            help = t('flink.app.addAppTips.appNameExistsInYarnMessage');
          } else if (code === AppExistsStateEnum.IN_KUBERNETES) {
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
            labelCol={{ lg: { span: 7 }, sm: { span: 7 } }}
            wrapperCol={{ lg: { span: 16 }, sm: { span: 4 } }}
            v-model:model={formValue}
          >
            <Form.Item label="Application Name">
              <Alert message={app.jobName} type="info" />
            </Form.Item>
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
            appId: formValue.appId,
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
    handleCopy,
    handleMapping,
    users,
  };
};

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
import Icon from '/@/components/Icon';
import { useMessage } from '/@/hooks/web/useMessage';
import { useI18n } from '/@/hooks/web/useI18n';
import { Form, Input, Switch } from 'ant-design-vue';
import { fetchCheckSavepointPath } from '/@/api/flink/app';
import { trigger } from '/@/api/flink/savepoint';
import { ref, unref } from 'vue';

export const useSavepoint = (updateOption: Fn) => {
  const { createErrorSwal, createConfirm, Swal } = useMessage();
  const { t } = useI18n();
  const submitLoading = ref(false);
  const appId = ref('');
  const customSavepoint = ref('');
  const nativeFormat = ref(false);

  async function handleSavepointAction(savepointTriggerReq: {
    appId: string | number;
    savepointPath: string | null;
  }) {
    await trigger(savepointTriggerReq);
    Swal.fire({
      icon: 'success',
      title: 'The current savepoint request is sent.',
      showConfirmButton: false,
      timer: 2000,
    });
  }
  const openSavepoint = (application: Recordable) => {
    appId.value = application.id;
    createConfirm({
      title: () => (
        <div>
          <Icon icon="ant-design:camera-outlined" color="#3c7eff" />
          <span class="pl-10px"> {t('flink.app.view.savepoint')} </span>
        </div>
      ),
      okText: t('common.apply'),
      width: 600,
      okButtonProps: {
        loading: submitLoading.value,
      },
      cancelButtonProps: {
        loading: submitLoading.value,
      },
      content: () => {
        return (
          <Form class="!pt-50px">
            <Form.Item
              name="customSavepoint"
              label="Custom Savepoint"
              label-col={{ lg: { span: 7, offset: 0 }, sm: { span: 7, offset: 0 } }}
              wrapper-col={{ lg: { span: 16, offset: 0 }, sm: { span: 4, offset: 0 } }}
            >
              <Input
                placeholder="Optional: Entry the custom savepoint path"
                allowClear={true}
                value={customSavepoint.value}
                onInput={(e) => (customSavepoint.value = e.target.value || '')}
              />
            </Form.Item>

            <Form.Item
              name="nativeFormat"
              label="Native Format"
              label-col={{ lg: { span: 7, offset: 0 }, sm: { span: 7, offset: 0 } }}
              wrapper-col={{ lg: { span: 16, offset: 0 }, sm: { span: 4, offset: 0 } }}
            >
              <Switch
                checkedValue={true}
                unCheckedValue={false}
                checked={nativeFormat.value}
                onClick={(checked, _) => (nativeFormat.value = (checked as boolean) || false)}
              />
            </Form.Item>
          </Form>
        );
      },
      onOk: () => {
        return new Promise(async (resolve, reject) => {
          try {
            const savepointReq = {
              appId: appId.value,
              savepointPath: unref(customSavepoint),
              nativeFormat: unref(nativeFormat),
            };
            if (unref(customSavepoint)) {
              submitLoading.value = true;
              const { data } = await fetchCheckSavepointPath({
                savepointPath: unref(customSavepoint),
              });
              if (data.data === false) {
                createErrorSwal('custom savepoint path is invalid, ' + data.message);
                reject('custom savepoint path is invalid');
              } else {
                await handleSavepointAction(savepointReq);
                updateOption({
                  type: 'savepointing',
                  key: appId.value,
                  value: new Date().getTime(),
                });
                customSavepoint.value = '';
                resolve(true);
              }
            } else {
              const { data } = await fetchCheckSavepointPath({ id: appId.value });
              if (data.data) await handleSavepointAction(savepointReq);
              else createErrorSwal(data.message);
              reject();
            }
          } catch (error) {
            console.error(error);
            reject(error);
          } finally {
            submitLoading.value = false;
          }
        });
      },
      onCancel: () => {
        customSavepoint.value = '';
        nativeFormat.value = false;
      },
    });
  };

  return { openSavepoint };
};

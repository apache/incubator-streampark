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
import { defineComponent, ref } from 'vue';
import { BasicModal, useModalInner } from '/@/components/Modal';
import { OpenApi } from '/@/components/OpenApi';
import { fetchCheckToken } from '/@/api/system/token';
import { useMessage } from '/@/hooks/web/useMessage';
import { useI18n } from '/@/hooks/web/useI18n';
import { fetchCopyCurl } from '/@/api/system/openapi';
import { baseUrl } from '/@/api';
import { useClipboard } from '@vueuse/core';

export default defineComponent({
  name: 'RequestModal',
  emits: ['register'],
  setup() {
    const currentRef = ref<Recordable>({});
    const [registerModal, { closeModal, changeOkLoading }] = useModalInner(async (data) => {
      currentRef.value = data;
    });
    const { Swal, createMessage } = useMessage();
    const { copy } = useClipboard({
      legacy: true,
    });
    const { t } = useI18n();
    const handleCopyCurl = async () => {
      try {
        changeOkLoading(true);
        const resp = await fetchCheckToken({});
        const result = parseInt(resp);
        if (result === 0) {
          Swal.fire({
            icon: 'error',
            title: t('flink.app.detail.nullAccessToken'),
            showConfirmButton: true,
            timer: 3500,
          });
        } else if (result === 1) {
          Swal.fire({
            icon: 'error',
            title: t('flink.app.detail.invalidAccessToken'),
            showConfirmButton: true,
            timer: 3500,
          });
        } else {
          const res = await fetchCopyCurl({
            baseUrl: baseUrl(),
            appId: currentRef.value.app.id,
            name: currentRef.value.name,
          });
          copy(res);
          createMessage.success(t('flink.app.detail.detailTab.copySuccess'));
          closeModal();
        }
      } catch (error) {
        console.log(error);
      } finally {
        changeOkLoading(false);
      }
    };
    return () => (
      <>
        <BasicModal
          width={900}
          onRegister={registerModal}
          title={t('flink.app.detail.apiTitle')}
          minHeight={400}
          okText={t('flink.app.detail.copyCurl')}
          onOk={handleCopyCurl}
        >
          {currentRef.value.name && <OpenApi name={currentRef.value.name}></OpenApi>}
        </BasicModal>
      </>
    );
  },
});

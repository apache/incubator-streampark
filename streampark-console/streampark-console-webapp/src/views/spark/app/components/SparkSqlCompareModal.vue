<!--
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
-->
<script setup lang="ts">
  import { computed, reactive, ref, toRaw } from 'vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { Select, Tag } from 'ant-design-vue';
  import { Icon, SvgIcon } from '/@/components/Icon';
  import { useDetailProviderContext } from '../hooks/useDetailContext';
  import { fetchSparkSql, fetchSparkSqlList } from '/@/api/spark/sql';
  import { useDrawer } from '/@/components/Drawer';
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { decodeByBase64 } from '/@/utils/cipher';
  import { CandidateTypeEnum } from '/@/enums/flinkEnum';
  import SparkSqlDifferent from './SparkSqlDifferent.vue';

  const SelectOption = Select.Option;
  const { t } = useI18n();
  defineEmits(['register']);
  const allSparkSqlVersions = ref<any[]>([]);
  const submitLoading = ref<boolean>(false);
  const compareRecord = reactive<Recordable>({});
  const values = useDetailProviderContext();
  const appId = ref<any>();

  const [registerSparkSqlDifferentDrawer, { openDrawer: openSparkSqlDiffDrawer }] = useDrawer();
  const [registerModal, { closeModal }] = useModalInner((data) => {
    data && onReceiveModalData(data);
  });

  async function onReceiveModalData(data) {
    appId.value = toRaw(values).app.id;
    const res = await fetchSparkSqlList({
      appId: appId.value,
      pageNo: 1,
      pageSize: 999999,
    });
    Object.assign(compareRecord, data);
    allSparkSqlVersions.value = res.records;
  }

  const [registerForm, { resetFields, submit }] = useForm({
    labelWidth: 180,
    colon: true,
    name: 'compareForm',
    labelCol: { lg: { span: 7, offset: 0 }, sm: { span: 7, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 4, offset: 0 } },
    baseColProps: { span: 24 },
    showActionButtonGroup: false,
    schemas: [
      { label: 'source version', field: 'source', component: 'Input', slot: 'source' },
      {
        label: 'target version',
        field: 'target',
        defaultValue: undefined,
        component: 'Select',
        slot: 'target',
        required: true,
      },
    ],
  });

  // submit form
  async function handleCompareOk(values: Recordable) {
    submitLoading.value = true;
    try {
      const source = await fetchSparkSql({
        id: compareRecord.id,
        appId: appId.value,
      });
      const sourceSql = decodeByBase64(source.sql);
      const sourceVersion = source.version;
      const target = await fetchSparkSql({
        id: values.target,
        appId: appId.value,
      });
      const targetSql = decodeByBase64(target.sql);
      const targetVersion = target.version;
      closeModal();
      openSparkSqlDiffDrawer(true, {
        immediate: true,
        param: [
          {
            name: 'Spark SQL',
            format: 'sql',
            original: sourceSql,
            modified: targetSql,
          },
        ],
        original: sourceVersion,
        modified: targetVersion,
      });
    } catch (error) {
      console.error(error);
    } finally {
      submitLoading.value = false;
    }
  }

  const filterNotCurrSql = computed(() => {
    return allSparkSqlVersions.value.filter((x) => x.version !== compareRecord.version);
  });
</script>
<template>
  <BasicModal @register="registerModal" :afterClose="resetFields">
    <template #title>
      <SvgIcon name="swap" />
      {{ t('spark.app.detail.compareSparkSql') }}
    </template>
    <BasicForm @register="registerForm" @submit="handleCompareOk">
      <template #source>
        <a-button type="primary" shape="circle" size="small" style="margin-right: 10px">
          {{ compareRecord.version }}
        </a-button>
        <Icon icon="ant-design:clock-circle-outlined" style="color: darkgrey" />
        <span style="color: darkgrey">{{ compareRecord.createTime }}</span>
      </template>
      <template #target="{ model }">
        <Select v-model:value="model.target" :placeholder="t('spark.app.detail.compareSelectTips')">
          <SelectOption v-for="(ver, index) in filterNotCurrSql" :value="ver.id" :key="index">
            <div style="padding-left: 5px">
              <a-button type="primary" shape="circle" size="small" style="margin-right: 10px">
                {{ ver.version }}
              </a-button>
              <Tag color="green" style="margin-left: 10px" size="small" v-if="ver.effective">
                Effective
              </Tag>
              <Tag
                color="cyan"
                class="ml-5px"
                size="small"
                v-if="[CandidateTypeEnum.NEW, CandidateTypeEnum.HISTORY].includes(ver.candidate)"
              >
                {{ t('spark.app.detail.candidate') }}
              </Tag>
            </div>
          </SelectOption>
        </Select>
      </template>
    </BasicForm>
    <template #footer>
      <a-button key="back" @click="closeModal">
        {{ t('common.closeText') }}
      </a-button>
      <a-button key="submit" type="primary" @click="submit" :loading="submitLoading">
        {{ t('spark.app.detail.compare') }}
      </a-button>
    </template>
  </BasicModal>
  <SparkSqlDifferent @register="registerSparkSqlDifferentDrawer" />
</template>

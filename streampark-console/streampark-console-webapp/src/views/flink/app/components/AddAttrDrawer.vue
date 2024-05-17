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
<!-- <script lang="ts">
export default defineComponent({
  name: 'AddAttrDrawer',
});
</script> -->
<script setup lang="ts" name="AddAttrDrawer">
import { computed, ref, unref, reactive, defineExpose } from 'vue';
import { BasicForm, useForm } from '/@/components/Form';
// import { formSchema } from '../user.data';
import { FormTypeEnum } from '/@/enums/formEnum';
import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
import { useCreateSchema } from '../hooks/useCreateSchema';
// import { addUser, updateUser } from '/@/api/system/user';
import Icon from '/@/components/Icon';
import { useI18n } from '/@/hooks/web/useI18n';
import { AppListRecord } from '/@/api/flink/app/app.type';
import PomTemplateTab from './PodTemplate/PomTemplateTab.vue';
import { useMessage } from '/@/hooks/web/useMessage';
import { createAsyncComponent } from '/@/utils/factory/createAsyncComponent';
import UseSysHadoopConf from './UseSysHadoopConf.vue';
const Dependency = createAsyncComponent(() => import('./Dependency.vue'), {
  loading: true,
});
const emit = defineEmits(['addAttrsuccess', 'register', 'addAttrFailed']);
const { t } = useI18n();
const formType = ref(FormTypeEnum.Edit);
const dependencyRef = ref();
const app = reactive<Partial<AppListRecord>>({}); // 属性提交的参数
const { createMessage } = useMessage();
let isSubmitConfig = ref(false)
const k8sTemplate = reactive({
  podTemplate: '',
  jmPodTemplate: '',
  tmPodTemplate: '',
});
const { getAttrCreateFormSchema, flinkEnvs } = useCreateSchema(dependencyRef, true);
const [registerForm, { resetFields, setFieldsValue, getFieldsValue, clearValidate, validate }] = useForm({
  labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
  wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
  baseColProps: { span: 24 },
  colon: true,
  showActionButtonGroup: false
});
const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
  console.log("data:", data)
  // formType.value = data.formType;
  resetFields();
  clearValidate();
  // updateSchema(formSchema(unref(formType)));
  setDrawerProps({
    confirmLoading: false
  });
  Object.assign(app, data);
  const parmas = {...data, checkPointFailure: {}}// init checkPoint失败策略 防止组件异步加载告警
  setFieldsValue(parmas);
  // if (unref(formType) !== FormTypeEnum.Create) {
  //   const roleIds = data.record?.roleId ?? [];
  //   data.record.roleId = Array.isArray(roleIds) ? roleIds : roleIds.split(',');
  //   setFieldsValue(data);
  // }
});

const getTitle = computed(() => {
  return {
    // [FormTypeEnum.Create]: t('system.user.form.create'),
    [FormTypeEnum.Edit]: t('flink.app.addDrawerMenu.editAttr'),
    // [FormTypeEnum.View]: t('system.user.form.view'),
  }[unref(formType)];
});

async function handleSubmit() {
  try {
    const values = await validate();
    setDrawerProps({ confirmLoading: true });
    const oldValues = JSON.parse(sessionStorage.getItem('AddJobModalParams') || '{}');
    const dependency = await getDependency()
    const params = { ...oldValues, ...values, dependency, k8sTemplate}
    sessionStorage.setItem('AddJobModalParams', JSON.stringify(params));
    isSubmitConfig.value = true
    closeDrawer();
    emit('addAttrsuccess');
  } catch (e) {
    createMessage.warning(t('flink.app.addDrawerMenu.attributeValidateTips'))
    isSubmitConfig.value = false
    emit('addAttrFailed', 'attr');
  } finally {
    setDrawerProps({ confirmLoading: false });
  }
}

async function handleClose() {
  const values = getFieldsValue()
  console.log(values)
  const oldValues = JSON.parse(sessionStorage.getItem('AddJobModalParams') || '{}');
  const params = { ...oldValues, ...values }
  sessionStorage.setItem('AddJobModalParams', JSON.stringify(params))
}
async function getDependency() {
  // Trigger a pom confirmation operation.
  await unref(dependencyRef)?.handleApplyPom();
  // common params...
  const dependency: { pom?: string; jar?: string } = {};
  console.log("unref(dependencyRef)", unref(dependencyRef))
  const dependencyRecords = unref(dependencyRef)?.dependencyRecords;
  const uploadJars = unref(dependencyRef)?.uploadJars;
  if (unref(dependencyRecords) && unref(dependencyRecords).length > 0) {
    Object.assign(dependency, {
      pom: unref(dependencyRecords),
    });
  }
  if (uploadJars && unref(uploadJars).length > 0) {
    Object.assign(dependency, {
      jar: unref(uploadJars),
    });
  }
  return dependency.pom === undefined && dependency.jar === undefined
    ? null
    : JSON.stringify(dependency);
}
defineExpose({
  handleSubmit,
  isSubmitConfig
});
</script>
<template>
  <BasicDrawer class="app_controller" :showCancelBtn="false" :okText="t('common.closeText')" @register="registerDrawer"
    showFooter width="50%" @ok="handleSubmit" @close="handleClose">
    <template #title>
      <Icon icon="ant-design:user-add-outlined" />
      {{ getTitle }}
    </template>
    <BasicForm ref="basicForm" @register="registerForm" :schemas="getAttrCreateFormSchema">
      <!-- <template #flinkSql="{ model, field }">
        <FlinkSqlEditor
          ref="flinkSql"
          v-model:value="model[field]"
          :versionId="model['versionId']"
          :suggestions="suggestions"
          @preview="(value) => openReviewDrawer(true, { value, suggestions })"
        />
      </template> -->
      <template #dependency="{ model, field }">
        <Dependency ref="dependencyRef" v-model:value="model[field]" :form-model="model" :flink-envs="flinkEnvs" />
      </template>
      <!-- <template #isSetConfig="{ model, field }">
        <Switch checked-children="ON" un-checked-children="OFF" v-model:checked="model[field]" />
        <SettingTwoTone
          v-if="model[field]"
          class="ml-10px"
          theme="twoTone"
          two-tone-color="#4a9ff5"
          @click="handleSQLConf(true, model)"
        />
      </template> -->
      <!-- <template #uploadJobJar>
        <UploadJobJar :custom-request="handleCustomJobRequest" v-model:loading="uploadLoading">
          <template #uploadInfo>
            <Alert v-if="uploadJar" class="uploadjar-box" type="info">
              <template #message>
                <span class="tag-dependency-pom">
                  {{ uploadJar }}
                </span>
              </template>
            </Alert>
          </template>
        </UploadJobJar>
      </template> -->
      <template #podTemplate>
        <PomTemplateTab v-model:podTemplate="k8sTemplate.podTemplate" v-model:jmPodTemplate="k8sTemplate.jmPodTemplate"
          v-model:tmPodTemplate="k8sTemplate.tmPodTemplate" />
      </template>
      <!-- <template #args="{ model }">
        <ProgramArgs
          v-model:value="model.args"
          :suggestions="suggestions"
          @preview="(value) => openReviewDrawer(true, { value, suggestions })"
        />
      </template> -->
      <template #useSysHadoopConf="{ model, field }">
        <UseSysHadoopConf v-model:hadoopConf="model[field]" />
      </template>
    </BasicForm>
  </BasicDrawer>
</template>
<style lang="less">
@import url('../styles/Add.less');
</style>
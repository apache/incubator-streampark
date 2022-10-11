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
<script lang="ts">
  import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue';
  import { useI18n } from '/@/hooks/web/useI18n';

  export default defineComponent({
    name: 'Dependency',
  });
</script>

<script setup lang="ts" name="Dependency">
  import { getMonacoOptions } from '../data';
  import { Icon } from '/@/components/Icon';
  import { useMonaco } from '/@/hooks/web/useMonaco';
  import { Select, Tabs } from 'ant-design-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { fetchUpload } from '/@/api/flink/app/app';
  import { fetchUploadJars } from '/@/api/flink/app/flinkHistory';
  import UploadJobJar from './UploadJobJar.vue';

  const TabPane = Tabs.TabPane;
  const SelectOption = Select.Option;
  const activeTab = ref('pom');
  const pomBox = ref();
  const dependency = reactive({
    jar: {},
    pom: {},
  });
  const selectedHistoryUploadJars = ref<string[]>([]);
  const uploadJars = ref<string[]>([]);
  const loading = ref(false);

  const emit = defineEmits(['update:value']);
  const props = defineProps({
    value: {
      type: String,
      default: '',
    },
    formModel: {
      type: Object as PropType<Recordable>,
      required: true,
    },
    flinkEnvs: {
      type: Array as PropType<Array<Recordable>>,
      default: () => [],
    },
  });
  const { t } = useI18n();
  const defaultValue = '';
  const { createMessage, createConfirm } = useMessage();
  const { onChange, setContent } = useMonaco(pomBox, {
    language: 'xml',
    code: props.value || defaultValue,
    options: {
      minimap: { enabled: true },
      ...(getMonacoOptions(false) as any),
    },
  });

  function handleApplyPom() {
    const versionId = props.formModel?.versionId;
    if (versionId == null) {
      createMessage.error(t('flink.app.dependencyError'));
      return;
    }
    const scalaVersion = props.flinkEnvs.find((v) => v.id === versionId)?.scalaVersion;
    if (props.value == null || props.value.trim() === '') {
      return;
    }
    const groupExp = /<groupId>([\s\S]*?)<\/groupId>/;
    const artifactExp = /<artifactId>([\s\S]*?)<\/artifactId>/;
    const versionExp = /<version>([\s\S]*?)<\/version>/;
    const exclusionsExp = /<exclusions>([\s\S]*?)<\/exclusions>/;
    const invalidArtifact: Array<string> = [];
    props.value
      .split('</dependency>')
      .filter((x) => x.replace(/\\s+/, '') !== '')
      .forEach((dep) => {
        const groupId = dep.match(groupExp) ? groupExp.exec(dep)![1].trim() : null;
        const artifactId = dep.match(artifactExp) ? artifactExp.exec(dep)![1].trim() : null;
        const version = dep.match(versionExp) ? versionExp.exec(dep)![1].trim() : null;
        const exclusion = dep.match(exclusionsExp) ? exclusionsExp.exec(dep)![1].trim() : null;
        if (groupId != null && artifactId != null && version != null) {
          if (/flink-(.*)_(.*)/.test(artifactId)) {
            const depScalaVersion = artifactId.substring(artifactId.lastIndexOf('_') + 1);
            if (scalaVersion !== depScalaVersion) {
              invalidArtifact.push(artifactId);
            }
          }
          if (invalidArtifact.length === 0) {
            const id = groupId + '_' + artifactId;
            const mvnPom: Recordable = {
              groupId: groupId,
              artifactId: artifactId,
              version: version,
            };
            const pomExclusion = new Map();
            if (exclusion != null) {
              const exclusions = exclusion.split('<exclusion>');
              exclusions.forEach((e) => {
                if (e != null && e.length > 0) {
                  const e_group = e.match(groupExp) ? groupExp.exec(e)![1].trim() : null;
                  const e_artifact = e.match(artifactExp) ? artifactExp.exec(e)![1].trim() : null;
                  const id = e_group + '_' + e_artifact;
                  pomExclusion.set(id, {
                    groupId: e_group,
                    artifactId: e_artifact,
                  });
                }
              });
            }
            mvnPom.exclusions = pomExclusion;
            dependency.pom[id] = mvnPom;
          }
        } else {
          console.error('dependency error...');
        }
      });

    if (invalidArtifact.length > 0) {
      alertInvalidDependency(scalaVersion, invalidArtifact);
      return;
    }
    setContent(defaultValue);
  }

  function alertInvalidDependency(scalaVersion, invalidArtifact) {
    let depCode = '';
    invalidArtifact.forEach((dep) => {
      depCode += `<div class="text-base pb-1">${dep}</div>`;
    });
    createConfirm({
      iconType: 'error',
      title: 'Dependencies invalid',
      content: h('div', { class: 'text-left' }, [
        h('div', { class: 'p-2 text-base' }, [
          h('span', {}, 'current flink scala version:'),
          h('strong', {}, scalaVersion),
          h('span', {}, ',some dependencies scala version is invalid,dependencies list:'),
        ]),
        h('div', { class: 'text-red-500 text-base p-2' }, depCode),
      ]),
    });
  }
  /* custom http  */
  async function handleCustomDepsRequest(data) {
    try {
      const formData = new FormData();
      formData.append('file', data.file);
      await fetchUpload(formData);
      Object.assign(props.formModel.historyjar, {
        [data.file.name]: data.file.name,
      });
    } catch (error) {
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  /* load history config records */
  async function handleReloadHistoryUploads() {
    selectedHistoryUploadJars.value = [];
    const res = await fetchUploadJars();
    uploadJars.value = res;
  }

  const filteredHistoryUploadJarsOptions = computed(() => {
    return uploadJars.value.filter((o) => !Reflect.has(dependency.jar, o));
  });

  onMounted(() => {
    handleReloadHistoryUploads();
  });

  onChange((data) => {
    emit('update:value', data);
  });
  defineExpose({
    handleApplyPom,
    dependency,
  });
</script>

<template>
  <Tabs type="card" v-model:activeKey="activeTab">
    <TabPane key="pom" tab="Maven pom">
      <div ref="pomBox" class="pom-box syntax-true" style="height: 300px"></div>
      <a-button type="primary" class="apply-pom" @click="handleApplyPom()">
        {{ t('common.apply') }}
      </a-button>
    </TabPane>
    <TabPane key="jar" tab="Upload Jar">
      <template v-if="[5, 6].includes(formModel?.executionMode)">
        <Select
          mode="multiple"
          placeholder="Search History Uploads"
          v-model:value="selectedHistoryUploadJars"
          style="width: 100%"
          :showArrow="true"
        >
          <SelectOption v-for="item in filteredHistoryUploadJarsOptions" :key="item" :value="item">
            <template #suffixIcon>
              <Icon icon="ant-design:file-done-outlined" />
            </template>
            {{ item }}
          </SelectOption>
        </Select>
      </template>

      <UploadJobJar :custom-request="handleCustomDepsRequest" v-model:loading="loading" />
    </TabPane>
  </Tabs>
</template>

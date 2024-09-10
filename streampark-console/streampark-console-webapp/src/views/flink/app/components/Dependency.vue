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
  import { computed, defineComponent, onMounted, reactive, ref } from 'vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { toPomString } from '../utils/Pom';

  export default defineComponent({
    name: 'Dependency',
  });
</script>

<script setup lang="ts" name="Dependency">
  import { getMonacoOptions } from '../data';
  import { Icon } from '/@/components/Icon';
  import { useMonaco } from '/@/hooks/web/useMonaco';
  import { Select, Tabs, Alert, Tag, Space, Form } from 'ant-design-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { fetchUpload, fetchUploadJars } from '/@/api/resource/upload';
  import UploadJobJar from './UploadJobJar.vue';
  import { fetchFlinkEnv } from '/@/api/flink/flinkEnv';

  interface DependencyType {
    artifactId: string;
    exclusions: string[];
    groupId: string;
    version: string;
    classifier: string;
  }

  const TabPane = Tabs.TabPane;
  const SelectOption = Select.Option;
  const activeTab = ref('pom');
  const pomBox = ref();
  const dependency = reactive({
    jar: {},
    pom: {},
  });
  const selectedHistoryUploadJars = ref<string[]>([]);
  const dependencyRecords = ref<DependencyType[]>([]);
  const uploadJars = ref<string[]>([]);
  const historyUploadJars = ref<string[]>([]);
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
  const { Swal } = useMessage();
  const { onChange, setContent, getContent } = useMonaco(pomBox, {
    language: 'xml',
    code: props.value || defaultValue,
    options: {
      minimap: { enabled: true },
      ...(getMonacoOptions(false) as any),
    },
  });

  async function handleApplyPom() {
    const versionId = props.formModel?.versionId;
    if (versionId == null) {
      Swal.fire('Failed', t('flink.app.dependencyError'), 'error');
      return;
    }

    let flinkEnv = props.flinkEnvs || [];
    if (props.flinkEnvs?.length == 0) {
      flinkEnv = await fetchFlinkEnv();
    }
    const scalaVersion = flinkEnv.find((v) => v.id === versionId)?.scalaVersion;
    if (props.value == null || props.value.trim() === '') {
      return;
    }

    const groupExp = /<groupId>([\s\S]*?)<\/groupId>/;
    const artifactExp = /<artifactId>([\s\S]*?)<\/artifactId>/;
    const versionExp = /<version>([\s\S]*?)<\/version>/;
    const classifierExp = /<classifier>([\s\S]*?)<\/classifier>/;
    const exclusionsExp = /<exclusions>([\s\S]*?)<\/exclusions>/;
    const invalidArtifact: Array<string> = [];
    const propsValue = await getContent();
    propsValue
      .split('</dependency>')
      .filter((x) => x.replace(/\\s+/, '') !== '')
      .forEach((dep) => {
        const groupId = dep.match(groupExp) ? groupExp.exec(dep)![1].trim() : null;
        const artifactId = dep.match(artifactExp) ? artifactExp.exec(dep)![1].trim() : null;
        const version = dep.match(versionExp) ? versionExp.exec(dep)![1].trim() : null;
        const classifier = dep.match(classifierExp) ? classifierExp.exec(dep)![1].trim() : null;
        const exclusion = dep.match(exclusionsExp) ? exclusionsExp.exec(dep)![1].trim() : null;
        if (groupId != null && artifactId != null && version != null) {
          if (/flink-(.*)_(.*)/.test(artifactId)) {
            const depScalaVersion = artifactId.substring(artifactId.lastIndexOf('_') + 1);
            if (scalaVersion !== depScalaVersion) {
              invalidArtifact.push(artifactId);
            }
          }
          if (invalidArtifact.length === 0) {
            const mvnPom: Recordable = {
              groupId: groupId,
              artifactId: artifactId,
              version: version,
            };
            if (classifier != null) {
              mvnPom.classifier = classifier;
            }
            const id = getId(mvnPom);
            const pomExclusion = [];
            if (exclusion != null) {
              const exclusions = exclusion.split('<exclusion>');
              exclusions.forEach((e) => {
                if (e != null && e.length > 0) {
                  const e_group = e.match(groupExp) ? groupExp.exec(e)![1].trim() : null;
                  const e_artifact = e.match(artifactExp) ? artifactExp.exec(e)![1].trim() : null;
                  pomExclusion.push({
                    groupId: e_group,
                    artifactId: e_artifact,
                  });
                }
              });
            }
            mvnPom.exclusions = pomExclusion;
            dependency.pom[id] = mvnPom;
          }
        }
      });

    if (invalidArtifact.length > 0) {
      alertInvalidDependency(scalaVersion, invalidArtifact);
      return;
    }
    handleUpdateDependency();
    setContent(defaultValue);
  }

  function alertInvalidDependency(scalaVersion, invalidArtifact) {
    let depCode = '';
    invalidArtifact.forEach((dep) => {
      depCode += `<div class="text-base pb-1">${dep}</div>`;
    });
    Swal.fire({
      title: 'Dependencies invalid',
      icon: 'error',
      width: 500,
      html: `
        <div style="text-align: left;">
         <div style="padding:0.5em;font-size: 1rem">
         current flink scala version: <strong>${scalaVersion}</strong>,some dependencies scala version is invalid,dependencies list:
         </div>
         <div style="color: red;font-size: 1em;padding:0.5em;">
           ${depCode}
         </div>
        </div>`,
      focusConfirm: false,
    });
  }
  /* custom http  */
  async function handleCustomDepsRequest(data) {
    try {
      loading.value = true;
      const formData = new FormData();
      formData.append('file', data.file);
      await fetchUpload(formData);
      // eslint-disable-next-line vue/no-mutating-props
      if (!props?.formModel?.historyjar) props.formModel.historyjar = {};
      Object.assign(props.formModel.historyjar, {
        [data.file.name]: data.file.name,
      });
      dependency.jar[data.file.name] = data.file.name;
      handleUpdateDependency();
    } catch (error) {
      console.error(error);
    } finally {
      loading.value = false;
    }
  }
  // update the dependency list
  function handleUpdateDependency() {
    const deps: DependencyType[] = [];
    const jars: string[] = [];
    Object.keys(dependency.pom).forEach((v: string) => {
      deps.push(dependency.pom[v]);
    });
    Object.keys(dependency.jar).forEach((v: string) => {
      jars.push(v);
    });
    dependencyRecords.value = deps;
    uploadJars.value = jars;
  }
  /* load history config records */
  async function handleReloadHistoryUploads() {
    selectedHistoryUploadJars.value = [];
    historyUploadJars.value = await fetchUploadJars();
  }

  const filteredHistoryUploadJarsOptions = computed(() => {
    return historyUploadJars.value.filter((o) => !Reflect.has(dependency.jar, o));
  });
  function handleRemoveJar(jar: string) {
    delete dependency.jar[jar];
    selectedHistoryUploadJars.value.splice(selectedHistoryUploadJars.value.indexOf(jar), 1);
    handleUpdateDependency();
  }
  function handleRemovePom(pom: Recordable) {
    const id = getId(pom);
    delete dependency.pom[id];
    handleUpdateDependency();
  }

  function handleEditPom(pom: DependencyType) {
    const pomString = toPomString(pom);
    activeTab.value = 'pom';
    setContent(pomString);
  }
  // set default value
  function setDefaultValue(dataSource: { pom?: DependencyType[]; jar?: string[] }) {
    dependencyRecords.value = dataSource.pom || [];
    uploadJars.value = dataSource.jar || [];
    dependency.pom = {};
    dependency.jar = {};
    dataSource.pom?.map((pomRecord: DependencyType) => {
      const id = getId(pomRecord);
      dependency.pom[id] = pomRecord;
    });
    dataSource.jar?.map((fileName: string) => {
      dependency.jar[fileName] = fileName;
    });
  }
  function addHistoryUploadJar(item: string) {
    dependency.jar[item] = item;
    handleUpdateDependency();
  }
  function deleteHistoryUploadJar(item: string) {
    delete dependency.jar[item];
    handleUpdateDependency();
  }

  function getId(pom) {
    if (pom.classifier != null) {
      return pom.groupId + '_' + pom.artifactId + '_' + pom.classifier;
    }
    return pom.groupId + '_' + pom.artifactId;
  }

  onMounted(() => {
    handleReloadHistoryUploads();
  });

  onChange((data) => {
    emit('update:value', data);
  });

  defineExpose({
    setDefaultValue,
    dependency,
    handleApplyPom,
    dependencyRecords,
    uploadJars,
  });
</script>

<template>
  <Tabs type="card" v-model:activeKey="activeTab" class="pom-card">
    <TabPane key="pom" tab="Maven pom">
      <div class="relative">
        <div ref="pomBox" class="pom-box syntax-true" style="height: 330px"></div>
        <a-button type="primary" class="apply-pom" @click="handleApplyPom()">
          {{ t('common.apply') }}
        </a-button>
      </div>
    </TabPane>
    <TabPane key="jar" tab="Upload Jar">
      <template v-if="filteredHistoryUploadJarsOptions.length > 0">
        <Form.ItemRest>
          <Select
            mode="multiple"
            placeholder="Search History Uploads"
            v-model:value="selectedHistoryUploadJars"
            @select="addHistoryUploadJar"
            @deselect="deleteHistoryUploadJar"
            style="width: 100%"
          >
            <SelectOption
              v-for="item in filteredHistoryUploadJarsOptions"
              :key="item"
              :value="item"
            >
              <template #suffixIcon>
                <Icon icon="ant-design:file-done-outlined" />
              </template>
              {{ item }}
            </SelectOption>
          </Select>
        </Form.ItemRest>
      </template>

      <UploadJobJar :custom-request="handleCustomDepsRequest" v-model:loading="loading" />
    </TabPane>
  </Tabs>
  <div class="dependency-box" v-if="dependencyRecords.length > 0 || uploadJars.length > 0">
    <Alert
      class="dependency-item"
      v-for="(dept, index) in dependencyRecords"
      :key="`dependency_${index}`"
      type="info"
      @click="handleEditPom(dept)"
    >
      <template #message>
        <Space @click="handleEditPom(dept)" class="tag-dependency-pom">
          <Tag class="tag-dependency" color="#2db7f5">POM</Tag>
          <template v-if="dept.classifier != null">
            {{ dept.artifactId }}-{{ dept.version }}-{{ dept.classifier }}.jar
          </template>
          <template v-else> {{ dept.artifactId }}-{{ dept.version }}.jar </template>
          <Icon
            :size="12"
            icon="ant-design:close-outlined"
            class="icon-close cursor-pointer"
            @click.stop="handleRemovePom(dept)"
          />
        </Space>
      </template>
    </Alert>
    <Alert
      class="dependency-item"
      v-for="jar in uploadJars"
      :key="`upload_jars_${jar}`"
      type="info"
    >
      <template #message>
        <Space>
          <Tag class="tag-dependency" color="#108ee9">JAR</Tag>
          {{ jar }}
          <Icon
            icon="ant-design:close-outlined"
            class="icon-close cursor-pointer"
            :size="12"
            @click="handleRemoveJar(jar)"
          />
        </Space>
      </template>
    </Alert>
  </div>
</template>

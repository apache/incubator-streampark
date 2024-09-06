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
  import { defineComponent, onMounted, reactive, ref } from 'vue';
  import { toPomString } from '/@/views/flink/app/utils/Pom';
  export default defineComponent({
    name: 'Upload',
  });
</script>

<script setup lang="ts" name="Upload">
  import { getMonacoOptions } from '/@/views/flink/app/data';
  import { Icon } from '/@/components/Icon';
  import { useMonaco } from '/@/hooks/web/useMonaco';
  import { Tabs, Alert, Tag, Space } from 'ant-design-vue';
  import { fetchUpload } from '/@/api/resource/upload';

  import UploadJobJar from '/@/views/flink/app/components/UploadJobJar.vue';
  import { ResourceTypeEnum } from '../upload.data';

  interface DependencyType {
    artifactId: string;
    exclusions: string[];
    groupId: string;
    version: string;
    classifier: string;
  }

  const TabPane = Tabs.TabPane;
  const activeTab = ref('pom');
  const pomBox = ref();
  const dependency = reactive({
    jar: {},
    pom: {},
  });
  const dependencyRecords = ref<DependencyType[]>([]);
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
  });
  const defaultValue = '';
  const { onChange, setContent } = useMonaco(pomBox, {
    language: 'xml',
    code: props.value || defaultValue,
    options: {
      minimap: { enabled: false },
      ...(getMonacoOptions(false) as any),
    },
  });

  async function handleApplyPom() {
    dependency.pom = {};
    dependencyRecords.value = [];

    if (props.value == null || props.value.trim() === '') {
      return;
    }

    const groupExp = /<groupId>([\s\S]*?)<\/groupId>/;
    const artifactExp = /<artifactId>([\s\S]*?)<\/artifactId>/;
    const versionExp = /<version>([\s\S]*?)<\/version>/;
    const classifierExp = /<classifier>([\s\S]*?)<\/classifier>/;
    const exclusionsExp = /<exclusions>([\s\S]*?)<\/exclusions>/;

    const poms = props.value
      .split('</dependency>')
      .filter((x) => x.trim().replace(/\\s+/, '') !== '');

    poms.forEach((dep) => {
      const groupId = dep.match(groupExp) ? groupExp.exec(dep)![1].trim() : null;
      const artifactId = dep.match(artifactExp) ? artifactExp.exec(dep)![1].trim() : null;
      const version = dep.match(versionExp) ? versionExp.exec(dep)![1].trim() : null;
      const classifier = dep.match(classifierExp) ? classifierExp.exec(dep)![1].trim() : null;
      const exclusion = dep.match(exclusionsExp) ? exclusionsExp.exec(dep)![1].trim() : null;
      if (groupId != null && artifactId != null && version != null) {
        const mvnPom: Recordable = {
          groupId: groupId,
          artifactId: artifactId,
          version: version,
        };
        if (classifier != null) {
          mvnPom.classifier = classifier;
        }
        const id = getId(mvnPom);
        const pomExclusion = {};
        if (exclusion != null) {
          const exclusions = exclusion.split('<exclusion>');
          exclusions.forEach((e) => {
            if (e != null && e.length > 0) {
              const e_group = e.match(groupExp) ? groupExp.exec(e)![1].trim() : null;
              const e_artifact = e.match(artifactExp) ? artifactExp.exec(e)![1].trim() : null;
              const id = e_group + '_' + e_artifact;
              pomExclusion[id] = {
                groupId: e_group,
                artifactId: e_artifact,
              };
            }
          });
        }
        mvnPom.exclusions = pomExclusion;
        dependency.pom[id] = mvnPom;
      } else {
        console.error('dependency error...');
      }
    });

    handleUpdateDependency();
  }

  /* custom http  */
  async function handleCustomDepsRequest(data) {
    try {
      loading.value = true;
      const formData = new FormData();
      formData.append('file', data.file);
      dependency.jar = {};
      const uploadResponse = await fetchUpload(formData);
      dependency.jar[data.file.name] = uploadResponse.path;
      handleUpdateDependency();
      if (props.formModel.resourceType === ResourceTypeEnum.APP) {
        props.formModel.mainClass = uploadResponse.mainClass;
      }
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
      jars.push(v + ':' + dependency.jar[v]);
    });
    dependencyRecords.value = deps;
    uploadJars.value = jars;
  }

  function handleRemoveJar(jar: string) {
    console.log(jar);
    delete dependency.jar[jar.split(':')[0]];
    handleUpdateDependency();
  }

  // set default value
  function setDefaultValue(dataSource: { pom?: DependencyType[]; jar?: string[] }) {
    dependencyRecords.value = dataSource.pom || [];
    uploadJars.value = dataSource.jar || [];
    dependency.pom = {};
    dependency.jar = {};
    if (dataSource.pom === undefined) {
      setContent(defaultValue);
    }
    dataSource.pom?.map((pomRecord: DependencyType) => {
      const id = getId(pomRecord);
      dependency.pom[id] = pomRecord;
      setContent(toPomString(pomRecord));
    });
    dataSource.jar?.map((fileName: string) => {
      dependency.jar[fileName] = fileName;
    });
  }

  function getId(pom) {
    if (pom.classifier != null) {
      return pom.groupId + '_' + pom.artifactId + '_' + pom.classifier;
    }
    return pom.groupId + '_' + pom.artifactId;
  }

  onChange((data) => {
    emit('update:value', data);
  });

  onMounted(async () => {
    setDefaultValue(JSON.parse(props?.formModel?.dependency || '{}'));
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
  <div class="upload_container">
    <template v-if="props.formModel.resourceType === ResourceTypeEnum.APP">
      <UploadJobJar :custom-request="handleCustomDepsRequest" v-model:loading="loading" />
    </template>
    <template v-else>
      <Tabs type="card" v-model:activeKey="activeTab" class="pom-card">
        <TabPane key="pom" tab="Maven pom">
          <div class="relative">
            <div ref="pomBox" class="pom-box" style="height: 300px"></div>
          </div>
        </TabPane>
        <TabPane key="jar" tab="Upload Jar">
          <UploadJobJar :custom-request="handleCustomDepsRequest" v-model:loading="loading" />
        </TabPane>
      </Tabs>
    </template>

    <div class="dependency-box" v-if="uploadJars.length > 0">
      <Alert
        class="dependency-item"
        v-for="jar in uploadJars"
        :key="`upload_jars_${jar}`"
        type="info"
      >
        <template #message>
          <Space>
            <Tag class="tag-dependency" color="#108ee9">JAR</Tag>
            {{ jar.split(':')[0] }}
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
  </div>
</template>

<style lang="less">
  .upload_container > {
    .dependency-box {
      margin-top: 10px;
    }
    .ant-tabs-top > .ant-tabs-nav {
      margin: 0;
    }
    .pom-box {
      border: 1px solid @border-color-base;
    }
    .apply-pom {
      z-index: 99;
      position: absolute;
      bottom: 20px;
      float: right;
      right: 20px;
      cursor: pointer;
      height: 26px;
      padding: 0 12px;
      font-size: 12px;
    }
  }
</style>

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
  import { toPomString } from '/@/views/flink/app/utils/Pom';

  export default defineComponent({
    name: 'Resource',
  });
</script>

<script setup lang="ts" name="Resource">
  import { getMonacoOptions } from '/@/views/flink/app/data';
  import { Icon } from '/@/components/Icon';
  import { useMonaco } from '/@/hooks/web/useMonaco';
  import { Select, Tabs, Alert, Tag, Space, Form } from 'ant-design-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { fetchUpload } from '/@/api/flink/app/app';
  import UploadJobJar from '/@/views/flink/app/components/UploadJobJar.vue';

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
  const { t } = useI18n();
  const defaultValue = '';
  const { Swal } = useMessage();
  const { onChange, setContent } = useMonaco(pomBox, {
    language: 'xml',
    code: props.value || defaultValue,
    options: {
      minimap: { enabled: true },
      ...(getMonacoOptions(false) as any),
    },
  });

  async function handleApplyPom() {
    if (props.value == null || props.value.trim() === '') {
      return;
    }
    const groupExp = /<groupId>([\s\S]*?)<\/groupId>/;
    const artifactExp = /<artifactId>([\s\S]*?)<\/artifactId>/;
    const versionExp = /<version>([\s\S]*?)<\/version>/;
    const classifierExp = /<classifier>([\s\S]*?)<\/classifier>/;
    const exclusionsExp = /<exclusions>([\s\S]*?)<\/exclusions>/;
    props.value
      .split('</dependency>')
      .filter((x) => x.replace(/\\s+/, '') !== '')
      .forEach((dep) => {
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
    setContent(defaultValue);
  }

  /* custom http  */
  async function handleCustomDepsRequest(data) {
    try {
      loading.value = true;
      const formData = new FormData();
      formData.append('file', data.file);
      await fetchUpload(formData);
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

  function handleRemoveJar(jar: string) {
    delete dependency.jar[jar];
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

  function getId(pom) {
    if (pom.classifier != null) {
      return pom.groupId + '_' + pom.artifactId + '_' + pom.classifier;
    }
    return pom.groupId + '_' + pom.artifactId;
  }

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

<style lang="less">
@import url('/@/views/flink/app/styles/Add.less');
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
</style>

<template>
  <Tabs type="card" v-model:activeKey="activeTab" class="pom-card">
    <TabPane key="pom" tab="Maven pom">
      <div class="relative">
        <div ref="pomBox" class="pom-box syntax-true" style="height: 300px"></div>
        <a-button type="primary" class="apply-pom" @click="handleApplyPom()">
          {{ t('common.apply') }}
        </a-button>
      </div>
    </TabPane>
    <TabPane key="jar" tab="Upload Jar">
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

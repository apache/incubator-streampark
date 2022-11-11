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
  import { defineComponent } from 'vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  export default defineComponent({
    name: 'PomTemplateTab',
  });
</script>
<script setup lang="ts" name="PomTemplateTab">
  import { reactive, ref } from 'vue';
  import { Tabs } from 'ant-design-vue';
  import { useDrawer } from '/@/components/Drawer';
  import { getMonacoOptions } from '../../data';
  import { useMonaco } from '/@/hooks/web/useMonaco';
  import {
    fetchExtractHostAliasFromPodTemplate,
    fetchInitPodTemplate,
    fetchPreviewHostAlias,
  } from '/@/api/flink/app/flinkPodtmpl';
  import {
    fetchFlinkJmPodTemplates,
    fetchFlinkPodTemplates,
    fetchFlinkTmPodTemplates,
  } from '/@/api/flink/app/flinkHistory';
  import HistoryDrawer from './HistoryDrawer.vue';
  import HostAliasDrawer from './HostAliasDrawer.vue';
  import TemplateButtonGroup from './TemplateButtonGroup.vue';
  const TabPane = Tabs.TabPane;

  const props = defineProps({
    podTemplate: {
      type: String,
      default: '',
    },
    jmPodTemplate: {
      type: String,
      default: '',
    },
    tmPodTemplate: {
      type: String,
      default: '',
    },
  });
  const emit = defineEmits(['update:podTemplate', 'update:jmPodTemplate', 'update:tmPodTemplate']);

  const { t } = useI18n();
  const podTemplateTab = ref('pod-template');
  const podTemplateRef = ref<HTMLDivElement>();
  const jmpodTemplateRef = ref<HTMLDivElement>();
  const tmpodTemplateRef = ref<HTMLDivElement>();
  const historyRecord = reactive<{
    podTemplate: string[];
    jmPodTemplate: string[];
    tmPodTemplate: string[];
  }>({
    podTemplate: [],
    jmPodTemplate: [],
    tmPodTemplate: [],
  });

  const { setContent: setPodContent, onChange: podContentChange } = useMonaco(
    podTemplateRef,
    {
      language: 'yaml',
      ...getMonacoOptions(false),
    },
    beforeMonacoMount,
  );
  podContentChange((value) => {
    emit('update:podTemplate', value);
  });

  const { setContent: setJmPodContent, onChange: jmpodContentChange } = useMonaco(
    jmpodTemplateRef,
    {
      language: 'yaml',
      ...getMonacoOptions(false),
    },
    beforeMonacoMount,
  );
  jmpodContentChange((value) => {
    emit('update:jmPodTemplate', value);
  });

  const { setContent: setTmPodContent, onChange: tmpodContentChange } = useMonaco(
    tmpodTemplateRef,
    {
      language: 'yaml',
      ...getMonacoOptions(false),
    },
    beforeMonacoMount,
  );
  tmpodContentChange((value) => {
    emit('update:tmPodTemplate', value);
  });

  async function beforeMonacoMount(monaco) {
    monaco.languages.registerCompletionItemProvider('xml', {
      provideCompletionItems: function (model, position) {
        const textUntilPosition = model.getValueInRange({
          startLineNumber: 1,
          startColumn: 1,
          endLineNumber: position.lineNumber,
          endColumn: position.column,
        });
        //dependency...
        if (textUntilPosition.match(/\s*<dep(.*)\s*\n*(.*\n*)*(<\/dependency>|)?$/)) {
          const word = model.getWordUntilPosition(position);
          const range = {
            startLineNumber: position.lineNumber,
            endLineNumber: position.lineNumber,
            startColumn: word.startColumn,
            endColumn: word.endColumn,
          };
          const suggestions = [
            {
              label: '"dependency"',
              insertText:
                'dependency>\n' +
                '    <groupId></groupId>\n' +
                '    <artifactId></artifactId>\n' +
                '    <version></version>\n' +
                '</dependency',
            },
            { label: '"group"', insertText: 'groupId></groupId' },
            { label: '"artifactId"', insertText: 'artifactId></artifactId' },
            { label: '"version"', insertText: 'version></version' },
          ];

          if (textUntilPosition.indexOf('<exclusions>') > 0) {
            suggestions.push({
              label: '"exclusion"',
              insertText:
                'exclusion>\n' +
                '  <artifactId></artifactId>\n' +
                '  <groupId></groupId>\n' +
                '</exclusion',
            });
          } else {
            suggestions.push({
              label: '"exclusions"',
              insertText:
                'exclusions>\n' +
                '  <exclusion>\n' +
                '    <artifactId></artifactId>\n' +
                '    <groupId></groupId>\n' +
                '  </exclusion>\n' +
                '</exclusions',
            });
          }
          suggestions.forEach((x: any) => {
            x.kind = monaco.languages.CompletionItemKind.Function;
            x.range = range;
          });
          return { suggestions: suggestions };
        }
      },
    });
  }

  const [registerPodHistoryDrawer, { openDrawer: openHistoryDrawer }] = useDrawer();
  const [registerHostAliasDrawer, { openDrawer: openHostAliasDrawer }] = useDrawer();

  /* history button */
  async function showPodTemplateDrawer(visualType) {
    switch (visualType) {
      case 'ptVisual':
        if (historyRecord.podTemplate.length === 0) {
          const res = await fetchFlinkPodTemplates({});
          historyRecord.podTemplate = res;
        }
        openHistoryDrawer(true, {
          visualType: 'ptVisual',
          dataSource: historyRecord.podTemplate,
        });
        break;
      case 'jmPtVisual':
        if (historyRecord.jmPodTemplate.length === 0) {
          const res = await fetchFlinkJmPodTemplates({});
          historyRecord.jmPodTemplate = res;
        }
        openHistoryDrawer(true, {
          visualType: 'jmPtVisual',
          dataSource: historyRecord.jmPodTemplate,
        });
        break;
      case 'tmPtVisual':
        if (historyRecord.tmPodTemplate.length === 0) {
          const res = await fetchFlinkTmPodTemplates({});
          historyRecord.tmPodTemplate = res;
        }
        openHistoryDrawer(true, {
          visualType: 'tmPtVisual',
          dataSource: historyRecord.tmPodTemplate,
        });
        break;
    }
  }

  async function handleGetInitPodTemplate(visualType) {
    const content = await fetchInitPodTemplate({});
    if (content != null && content !== '') {
      handleChoicePodTemplate(visualType, content);
    }
  }
  /* click host alias button */
  async function showTemplateHostAliasDrawer(visualType) {
    let tmplContent = '';
    switch (visualType) {
      case 'ptVisual':
        tmplContent = props.podTemplate;
        break;
      case 'jmPtVisual':
        tmplContent = props.jmPodTemplate;
        break;
      case 'tmPtVisual':
        tmplContent = props.tmPodTemplate;
        break;
    }
    let selectValue = [];
    if (tmplContent !== '') {
      const param = {};
      param['podTemplate'] = tmplContent;
      const resp = await fetchExtractHostAliasFromPodTemplate(param);
      selectValue = resp || [];
      await fetchPreviewHostAlias({ hosts: resp.join(',') });
    }
    openHostAliasDrawer(true, {
      selectValue,
      visualType,
      podTemplate: tmplContent,
    });
  }

  function handleChoicePodTemplate(visualType, content) {
    switch (visualType) {
      case 'ptVisual':
        emit('update:podTemplate', content);
        setPodContent(content);
        break;
      case 'jmPtVisual':
        emit('update:jmPodTemplate', content);
        setJmPodContent(content);
        break;
      case 'tmPtVisual':
        setTmPodContent(content);
        emit('update:tmPodTemplate', content);
        break;
      default:
        break;
    }
    openHistoryDrawer(false);
  }

  defineExpose({
    handleChoicePodTemplate,
  });
</script>
<template>
  <Tabs type="card" v-model:activeKey="podTemplateTab" class="pom-card">
    <TabPane key="pod-template" tab="Pod Template" forceRender>
      <div ref="podTemplateRef" class="pod-template-box syntax-true"></div>
      <TemplateButtonGroup
        visualType="ptVisual"
        @click-history="showPodTemplateDrawer('ptVisual')"
        @click-init="handleGetInitPodTemplate('ptVisual')"
        @click-host-alias="showTemplateHostAliasDrawer('ptVisual')"
      />
    </TabPane>

    <TabPane key="jm-pod-template" tab="JM Pod Template" forceRender>
      <div ref="jmpodTemplateRef" class="jm-pod-template-box syntax-true"></div>
      <TemplateButtonGroup
        visualType="ptVisual"
        @click-history="showPodTemplateDrawer('jmPtVisual')"
        @click-init="handleGetInitPodTemplate('jmPtVisual')"
        @click-host-alias="showTemplateHostAliasDrawer('jmPtVisual')"
      />
    </TabPane>

    <TabPane key="tm-pod-template" tab="TM Pod Template" forceRender>
      <div ref="tmpodTemplateRef" class="tm-pod-template-box syntax-true"></div>
      <TemplateButtonGroup
        visualType="ptVisual"
        @click-history="showPodTemplateDrawer('tmPtVisual')"
        @click-init="handleGetInitPodTemplate('tmPtVisual')"
        @click-host-alias="showTemplateHostAliasDrawer('tmPtVisual')"
      />
    </TabPane>
  </Tabs>
  <!--  history  -->
  <HistoryDrawer @register="registerPodHistoryDrawer">
    <template #extra="{ data }">
      <a @click="handleChoicePodTemplate('ptVisual', data)">
        {{ t('flink.app.pod.choice') }}
      </a>
    </template>
  </HistoryDrawer>
  <!-- host alias  -->
  <HostAliasDrawer
    @register="registerHostAliasDrawer"
    @complete="({ visualType, content }) => handleChoicePodTemplate(visualType, content)"
  />
</template>

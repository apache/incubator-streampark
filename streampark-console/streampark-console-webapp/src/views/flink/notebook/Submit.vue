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
  import { defineComponent, ref, watchEffect, computed } from 'vue';
  import { fetchNotebookSubmit } from '/@/api/flink/notebook';

  export default defineComponent({
    name: 'Notebook',
  });
</script>
<script lang="ts" setup name="Notebook">
  import { PageWrapper } from '/@/components/Page';
  import { Row, Col, Alert, Select } from 'ant-design-vue';
  import {
    PlayCircleTwoTone,
    FullscreenExitOutlined,
    FullscreenOutlined,
  } from '@ant-design/icons-vue';
  import { useMonaco } from '/@/hooks/web/useMonaco';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useAppStore } from '/@/store/modules/app';

  const { Swal } = useMessage();
  const appStore = useAppStore();

  const SelectOption = Select.Option;

  const envs = ref([
    {
      env: 'flink',
      introduction: function () {
        return 'Creates ExecutionEnvironment/StreamExecutionEnvironment and provides a Scala environment as env';
      },
    },
    {
      env: 'pyflink',
      introduction: function () {
        return 'Provides a python environment ';
      },
    },
    {
      env: 'ipyflink',
      introduction: function () {
        return ' Provides an ipython environment ';
      },
    },
    {
      env: 'sql',
      introduction: function () {
        return 'Provides a StreamTableEnvironment ';
      },
    },
  ]);
  const codeEditor = ref();
  const introduction = ref<Nullable<string>>(null);
  // const tutorial = ref<Nullable<string>>(null);
  const env = ref('flink');

  const isFullscreen = computed(() => {
    return appStore.getProjectConfig?.fullContent;
  });
  const woldCount =
    '\n%flink.repl.out=true\n' +
    '%flink.yarn.queue=default\n' +
    '%flink.execution.mode=yarn\n' +
    '%flink.yarn.appName=Socket Window WordCount with StreamPark NoteBook\n\n' +
    '// the host and the port to connect to\n' +
    'val hostname = "localhost"\n' +
    'val port = 9999\n' +
    '\n' +
    '// get the execution environment\n' +
    'val env = StreamExecutionEnvironment.getExecutionEnvironment\n' +
    '\n' +
    'env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)\n' +
    '\n' +
    '// get input data by connecting to the socket\n' +
    'val text = env.socketTextStream(hostname, port, "\\n")\n' +
    '\n' +
    '// parse the data, group it, window it, and aggregate the counts\n' +
    'val windowCounts = text.flatMap(new FlatMapFunction[String,(String,Long)] {\n' +
    '    override def flatMap(value: String, out: Collector[(String, Long)]): Unit = {\n' +
    '       for (word <- value.split("\\\\s")) {\n' +
    '           out.collect(word,1L)\n' +
    '       }\n' +
    '    }\n' +
    '}).keyBy(0).timeWindow(Time.seconds(5)).reduce(new ReduceFunction[(String,Long)]() {\n' +
    '    override def reduce(a: (String, Long), b: (String, Long)): (String, Long) = (a._1,a._2 + b._2)\n' +
    '})\n' +
    '\n' +
    '// print the results with a single thread, rather than in parallel\n' +
    'windowCounts.print.setParallelism(1)\n' +
    'env.execute("Socket Window WordCount with StreamPark NoteBook")\n';
  const { getContent } = useMonaco(codeEditor, {
    language: 'java',
    code: woldCount,
    options: {
      selectOnLineNumbers: false,
      foldingStrategy: 'indentation', // code fragmentation
      overviewRulerBorder: false, // Don't scroll bar borders
      tabSize: 2, // tab indent length
      readOnly: false,
      scrollBeyondLastLine: false,
      lineNumbersMinChars: 4,
      lineHeight: 24,
      automaticLayout: true,
      cursorStyle: 'line',
      cursorWidth: 3,
      renderFinalNewline: true,
      renderLineHighlight: 'line',
      quickSuggestionsDelay: 100, // Code prompt delay
      minimap: { enabled: true },
      scrollbar: {
        useShadows: false,
        vertical: 'visible',
        horizontal: 'visible',
        horizontalSliderSize: 5,
        verticalSliderSize: 5,
        horizontalScrollbarSize: 15,
        verticalScrollbarSize: 15,
      },
    },
  });

  watchEffect(() => {
    const filterEnv = envs.value.find((x) => x.env === env.value);
    if (filterEnv) {
      introduction.value = filterEnv.introduction();
    }
  });

  /* submit */
  async function handleReplSubmit() {
    const code = await getContent();
    try {
      await fetchNotebookSubmit({
        env: 'senv',
        text: code,
      });
      Swal.fire({
        icon: 'success',
        title: 'this features is experimental\ncurrent job is starting...',
        showConfirmButton: false,
        timer: 2000,
      });
    } catch (error) {
      console.error(error);
    }
  }
  /* Set/unset fullscreen content */
  function toggle() {
    appStore.setProjectConfig({ fullContent: !isFullscreen.value });
  }
  // async function handleReadmd() {
  //   const res = await fetchReadmd({ name: 'repl' });
  //   tutorial.value = res.content;
  //   setContent(tutorial.value);
  // }
  // handleReadmd();
</script>
<template>
  <PageWrapper contentFullHeight contentBackground contentClass="p-24px">
    <div class="nodebook-submit">
      <Alert
        show-icon
        message="this is Experimental Features,and only supported ExecutionEnvironment/StreamExecutionEnvironment "
        type="info"
      />
      <br />
      <Row :gutter="24" type="flex" justify="space-between">
        <Col :span="22">
          <span style="height: 40px; margin-left: 17px" class="code-prefix"></span>
          <Select class="z-5 w-full" v-model:value="env">
            <SelectOption v-for="e in envs" :key="e.env">
              {{ e.env }}
            </SelectOption>
          </Select>
          <div class="ml-15px my-10px" style="color: gray"> {{ introduction }} </div>
        </Col>
        <Col :span="2" class="z-2">
          <PlayCircleTwoTone
            twoToneColor="#4a9ff5"
            @click="handleReplSubmit"
            class="pl-10px mt-10px"
          />
          <span class="pl-10px cursor" @click="toggle">
            <FullscreenExitOutlined v-if="isFullscreen" />
            <FullscreenOutlined v-else />
          </span>
        </Col>
        <Col :span="24">
          <div class="code-box" ref="codeEditor"></div>
        </Col>
      </Row>
    </div>
  </PageWrapper>
</template>
<style lang="less">
  @import url('./Submit.less');
</style>

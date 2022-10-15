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

<template>
  <a-modal
    v-model="show"
    :centered="true"
    :keyboard="false"
    :footer="null"
    :width="600"
    @cancel="handleCancelClick">
    <template slot="title">
      <a-icon type="code" />
      Variable Info
    </template>
    <a-layout class="variable-info">
      <a-layout-content class="variable-content">
        <p>
          <a-icon type="code" />Variable Code：{{ data.variableCode }}
        </p>
        <p>
          <a-icon type="down-circle" />Variable Value：<br>{{ data.variableValue }}
        </p>
        <p>
          <a-icon type="user" />Create User：{{ data.userName }}
        </p>
        <p>
          <a-icon type="team" />Team：{{ data.teamName }}
        </p>
        <p>
          <a-icon type="clock-circle" />Create Time：{{ data.createTime }}
        </p>
        <p>
          <a-icon type="clock-circle" />Modify Time：{{ data.modifyTime }}
        </p>
        <p>
          <a-icon type="message" />Description：{{ data.description }}
        </p>
      </a-layout-content>
    </a-layout>
  </a-modal>
</template>
<script>
export default {
  name: 'VariableInfo',
  props: {
    visible: {
      type: Boolean,
      require: true,
      default: false
    },
    data: {
      type: Object,
      default: () => ({}),
      require: true
    }
  },
  computed: {
    show: {
      get: function () {
        return this.visible
      },
      set: function () {
      }
    }
  },
  methods: {
    handleCancelClick () {
      this.$emit('close')
    }
  }
}
</script>
<style lang="less" scoped>
  .variable-info {
    background: @body-background;
    padding: 10px;
  }
  .variable-content{
    margin-right: 1.2rem;
    float: left;
  }
  p {
    margin-bottom: 1rem;
  }
  i {
    margin-right: .8rem;
  }
</style>

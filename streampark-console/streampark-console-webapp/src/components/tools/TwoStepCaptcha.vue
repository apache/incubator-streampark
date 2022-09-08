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
  <!-- 两步验证 -->
  <a-modal
    centered
    v-model="visible"
    @cancel="handleCancel"
    :mask-closable="false">
    <div
      slot="title"
      :style="{ textAlign: 'center' }">
      两步验证
    </div>
    <template
      slot="footer">
      <div
        :style="{ textAlign: 'center' }">
        <a-button
          key="back"
          @click="handleCancel">
          返回
        </a-button>
        <a-button
          key="submit"
          type="primary"
          :loading="stepLoading"
          @click="handleStepOk">
          继续
        </a-button>
      </div>
    </template>

    <a-spin
      :spinning="stepLoading">
      <a-form
        layout="vertical"
        :auto-form-create="(form)=>{this.form = form}">
        <div
          class="step-form-wrapper">
          <p
            style="text-align: center"
            v-if="!stepLoading">
            请在手机中打开 Google Authenticator 或两步验证 APP<br>输入 6 位动态码
          </p>
          <p
            style="text-align: center"
            v-else>
            正在验证..<br>请稍后
          </p>
          <a-form-item
            :style="{ textAlign: 'center' }"
            has-feedback
            field-decorator-id="stepCode"
            :field-decorator-options="{rules: [{ required: true, message: '请输入 6 位动态码!', pattern: /^\d{6}$/, len: 6 }]}">
            <a-input
              :style="{ textAlign: 'center' }"
              @keyup.enter.native="handleStepOk"
              placeholder="000000" />
          </a-form-item>
          <p
            style="text-align: center">
            <a
              @click="onForgeStepCode">遗失手机?</a>
          </p>
        </div>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script>
export default {
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      stepLoading: false,

      form: null
    }
  },
  methods: {
    handleStepOk () {
      const vm = this
      this.stepLoading = true
      this.form.validateFields((err, values) => {
        if (!err) {
          console.log('values', values)
          setTimeout(() => {
            vm.stepLoading = false
            vm.$emit('success', { values })
          }, 2000)
          return
        }
        this.stepLoading = false
        this.$emit('error', { err })
      })
    },
    handleCancel () {
      this.visible = false
      this.$emit('cancel')
    },
    onForgeStepCode () {

    }
  }
}
</script>
<style lang="less" scoped>
  .step-form-wrapper {
    margin: 0 auto;
    width: 80%;
    max-width: 400px;
  }
</style>

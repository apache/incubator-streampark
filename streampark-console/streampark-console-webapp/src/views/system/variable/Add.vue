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
  <a-drawer
    :mask-closable="false"
    width="700"
    placement="right"
    :closable="true"
    @close="onClose"
    :visible="visible"
    style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
    <template slot="title">
      <a-icon type="code" />
      Add Variable
    </template>
    <a-form
      :form="form">
      <a-form-item
        label="Variable Code"
        v-bind="formItemLayout"
        :validate-status="validateStatus"
        :help="help">
        <a-input
          @blur="handleVariableCodeBlur"
          v-decorator="['variableCode', {rules: [{ required: true, max: 50, message: 'please enter Variable Code'}]}]" />
      </a-form-item>
      <a-form-item
        label="Variable Value"
        v-bind="formItemLayout">
        <a-textarea
          rows="4"
          placeholder="Please enter Variable Value"
          v-decorator="['variableValue', {rules: [{ required: true, max: 100, message: 'please enter Variable Value' }]}]" />
      </a-form-item>
      <a-form-item
        label="Description"
        v-bind="formItemLayout">
        <a-textarea
          rows="4"
          placeholder="Please enter description for this variable"
          v-decorator="['description', {rules: [{ max: 100, message: 'exceeds maximum length limit of 100 characters'}]}]" />
      </a-form-item>
    </a-form>
    <div
      class="drawer-bootom-button">
      <a-button
        @click="onClose">
        Cancel
      </a-button>
      <a-button
        @click="handleSubmit"
        type="primary"
        :loading="loading">
        Submit
      </a-button>
    </div>
  </a-drawer>
</template>
<script>
import { post, checkVariableCode } from '@/api/variable'

const formItemLayout = {
  labelCol: { span: 4 },
  wrapperCol: { span: 18 }
}
export default {
  name: 'VariableAdd',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      loading: false,
      formItemLayout,
      form: this.$form.createForm(this),
      validateStatus: '',
      help: ''
    }
  },
  methods: {
    reset () {
      this.validateStatus = ''
      this.help = ''
      this.loading = false
      this.form.resetFields()
    },
    onClose () {
      this.reset()
      this.$emit('close')
    },
    handleSubmit() {
      this.form.validateFields((err, values) => {
        if (!err && this.validateStatus === 'success') {
          post({
            ...values
          }).then((r) => {
            if (r.status === 'success') {
              this.reset()
              this.$emit('success')
            }
          }).catch(() => {
            this.loading = false
          })
        }
      })
    },
    handleVariableCodeBlur (e) {
      const variableCode = (e && e.target.value) || ''
      if (variableCode.length) {
        this.validateStatus = 'validating'
        checkVariableCode({
          variableCode: variableCode
        }).then((resp) => {
          if (resp.status !== 'success') {
            this.validateStatus = 'error'
            this.help = resp.message
          } else if (resp.data) {
            this.validateStatus = 'success'
            this.help = ''
          } else {
            this.validateStatus = 'error'
            this.help = 'Sorry, the Variable Code already exists'
          }
        })
      } else {
        this.validateStatus = 'error'
        this.help = 'Variable Code cannot be empty'
      }
    }
  }
}
</script>

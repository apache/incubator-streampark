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
      Modify Variable
    </template>
    <a-form
      :form="form">
      <a-form-item
        label="Variable Code"
        v-bind="formItemLayout">
        <a-input
          read-only
          v-decorator="['variableCode', {rules: [{ required: true }]}]" />
      </a-form-item>
      <a-form-item
        label="Variable Value"
        v-bind="formItemLayout">
        <a-textarea
          v-decorator="['variableValue', {rules: [{ required: true }]}]" />
      </a-form-item>
      <a-form-item
        label="Variable Name"
        v-bind="formItemLayout"
        :validate-status="validateStatus"
        :help="help">
        <a-input
          @blur="handleVariableNameBlur"
          v-decorator="['variableName', {rules: [{ required: true }]}]" />
      </a-form-item>
      <a-form-item
        label="Description"
        v-bind="formItemLayout">
        <a-textarea
          v-decorator="['description']" />
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
import { update, checkUpdateVariableName } from '@/api/variable'

const formItemLayout = {
  labelCol: { span: 4 },
  wrapperCol: { span: 18 }
}
export default {
  name: 'VariableEdit',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      formItemLayout,
      form: this.$form.createForm(this),
      loading: false,
      validateStatus: 'success',
      help: ''
    }
  },

  methods: {
    onClose () {
      this.loading = false
      this.form.resetFields()
      this.$emit('close')
    },

    setFormValues ({ ...variable }) {
      this.id = variable.id
      const fields = ['variableCode', 'variableValue', 'variableName', 'Description']
      Object.keys(variable).forEach((key) => {
        if (fields.indexOf(key) !== -1) {
          this.form.getFieldDecorator(key)
          const obj = {}
          obj[key] = variable[key]
          this.form.setFieldsValue(obj)
        }
      })
    },

    handleVariableNameBlur () {
      const variable = this.form.getFieldsValue()
      variable.id = this.id
      if (variable.variableName.length) {
        if (variable.variableName.length > 100) {
          this.validateStatus = 'error'
          this.help = 'Variable Name should not be longer than 100 characters'
        } else {
          this.validateStatus = 'validating'
          checkUpdateVariableName(variable).then((resp) => {
            if (resp.data) {
              this.validateStatus = 'success'
              this.help = ''
            } else {
              this.validateStatus = 'error'
              this.help = 'Sorry, the Variable Name already exists'
            }
          })
        }
      } else {
        this.validateStatus = 'error'
        this.help = 'Variable Name cannot be empty'
      }
    },

    handleSubmit () {
      this.form.validateFields((err, values) => {
        if (!err && this.validateStatus === 'success') {
          this.loading = true
          const variable = this.form.getFieldsValue()
          variable.id = this.id
          update(variable).then((r) => {
            if (r.status === 'success') {
              this.loading = false
              this.$emit('success')
            } else {
              this.loading = false
            }
          }).catch(() => {
            this.loading = false
          })
        }
      })
    }
  }
}
</script>

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
    width="650"
    placement="right"
    :closable="true"
    :visible="visible"
    @close="onClose"
    style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
    <template slot="title">
      <a-icon type="team"/>
      Modify Team
    </template>
    <a-form
      :form="form">
      <a-form-item
        label="Team Name"
        v-bind="formItemLayout">
        <a-input
          read-only
          v-decorator="['teamName']"/>
      </a-form-item>
      <a-form-item
        label="Description"
        v-bind="formItemLayout">
        <a-textarea
          :rows="4"
          v-decorator="[
            'description',
            {rules: [
              { max: 100, message: 'exceeds maximum length limit of 100 characters'}
            ]}]"/>
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
import {update} from '@/api/team'

const formItemLayout = {
  labelCol: {span: 4},
  wrapperCol: {span: 18}
}
export default {
  name: 'TeamEdit',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      formItemLayout,
      form: this.$form.createForm(this),
      teamId: '',
      loading: false
    }
  },

  methods: {

    onClose() {
      this.loading = false
      this.form.resetFields()
      this.$emit('close')
    },

    setFormValues({...team}) {
      this.teamId = team.id
      const fields = ['teamName', 'description']
      Object.keys(team).forEach((key) => {
        if (fields.indexOf(key) !== -1) {
          this.form.getFieldDecorator(key)
          const obj = {}
          obj[key] = team[key]
          this.form.setFieldsValue(obj)
        }
      })
    },

    handleSubmit() {
      this.form.validateFields((err, team) => {
        if (!err) {
          this.loading = true
          const team = this.form.getFieldsValue()
          team.id = this.teamId
          update(team).then((r) => {
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

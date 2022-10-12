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
      Add Team
    </template>
    <a-form
      :form="form">
      <a-form-item
        label="Team Name"
        v-bind="formItemLayout"
        :validate-status="validateStatus"
        :help="help">
        <a-input
          @blur="handleTeamNameBlur"
          v-decorator="
            ['teamName',
             {rules: [{ required: true }]}]"/>
      </a-form-item>
      <a-form-item
        label="Description"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['description',{rules: [
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
import {checkName, post} from '@/api/team'

const formItemLayout = {
  labelCol: {span: 4},
  wrapperCol: {span: 18}
}
export default {
  name: 'TeamAdd',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      loading: false,
      formItemLayout,
      form: this.$form.createForm(this),
      validateStatus: '',
      help: ''
    }
  },
  methods: {
    reset() {
      this.validateStatus = ''
      this.help = ''
      this.loading = false
      this.form.resetFields()
    },

    onClose() {
      this.reset()
      this.$emit('close')
    },

    handleSubmit() {
      this.form.validateFields((err, team) => {
        if (!err && this.validateStatus === 'success') {
          this.loading = true
          post(team).then((r) => {
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
    handleTeamNameBlur(e) {
      const teamName = (e && e.target.value) || ''
      if (teamName.length) {
        if (teamName.length > 20) {
          this.validateStatus = 'error'
          this.help = 'Team name should not be longer than 20 characters'
        } else if (teamName.length < 4) {
          this.validateStatus = 'error'
          this.help = 'Team name should not be less than 4 characters'
        } else {
          this.validateStatus = 'validating'
          checkName({
            teamName: teamName
          }).then((r) => {
            if (r.data) {
              this.validateStatus = 'success'
              this.help = ''
            } else {
              this.validateStatus = 'error'
              this.help = 'Sorry, the team name already exists'
            }
          })
        }
      } else {
        this.validateStatus = 'error'
        this.help = 'Team name cannot be empty'
      }
    }
  }
}
</script>

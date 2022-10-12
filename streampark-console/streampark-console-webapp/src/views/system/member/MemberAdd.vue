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
      <a-icon type="user-add"/>
      Add Member
    </template>
    <a-form
      :form="form">
      <a-form-item
        label="User Name"
        v-bind="formItemLayout"
        :validate-status="validateStatus"
        :help="help">
        <a-input
          @blur="handleUserNameBlur"
          v-decorator="
            ['userName',
             {rules: [{ required: true }]}]"/>
      </a-form-item>
      <a-form-item
        label="Role"
        v-bind="formItemLayout">
        <a-select
          mode="single"
          :allow-clear="true"
          v-decorator="['roleId',{rules: [{ required: true, message: 'please select role' }]}]">
          <a-select-option
            v-for="r in roleData"
            :key="r.roleId">
            {{ r.roleName }}
          </a-select-option>
        </a-select>
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
import {post} from '@/api/member'
import {list as getRole} from '@/api/role'
import {checkUserName} from '@/api/user'

const formItemLayout = {
  labelCol: {span: 4},
  wrapperCol: {span: 18}
}
export default {
  name: 'MemberAdd',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      loading: false,
      roleData: [],
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
      this.form.validateFields((err, member) => {
        if (!err && this.validateStatus === 'success') {
          this.loading = true
          post(member).then((r) => {
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
    handleUserNameBlur(e) {
      const username = (e && e.target.value) || ''
      if (username.length) {
        if (username.length > 20) {
          this.validateStatus = 'error'
          this.help = 'User name should not be longer than 20 characters'
        } else if (username.length < 4) {
          this.validateStatus = 'error'
          this.help = 'User name should not be less than 4 characters'
        } else {
          this.validateStatus = 'validating'
          checkUserName({
            username: username
          }).then((r) => {
            if (r.data) {
              this.validateStatus = 'error'
              this.help = 'Sorry, the user name doesn\'t exists'
            } else {
              this.validateStatus = 'success'
              this.help = ''
            }
          })
        }
      } else {
        this.validateStatus = 'error'
        this.help = 'User name cannot be empty'
      }
    }
  },
  watch: {
    visible() {
      if (this.visible) {
        getRole(
          {'pageSize': '9999'}
        ).then((resp) => {
          this.roleData = resp.data.records
        })
      }
    }
  }
}
</script>

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
      <a-icon type="user"/>
      Modify Member
    </template>
    <a-form
      :form="form">
      <a-form-item
        label="User Name"
        v-bind="formItemLayout">
        <a-input
          read-only
          v-decorator="['userName']"/>
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
import {update} from '@/api/member'
import {list as getRole} from '@/api/role'

const formItemLayout = {
  labelCol: {span: 4},
  wrapperCol: {span: 18}
}
export default {
  name: 'MemberEdit',
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
      memberId: '',
      userId: '',
      roleData: [],
      loading: false
    }
  },

  methods: {

    onClose() {
      this.loading = false
      this.form.resetFields()
      this.$emit('close')
    },

    setFormValues({...member}) {
      this.memberId = member.id
      this.userId = member.userId
      const fields = ['userName', 'roleId']
      Object.keys(member).forEach((key) => {
        if (fields.indexOf(key) !== -1) {
          this.form.getFieldDecorator(key)
          const obj = {}
          obj[key] = member[key]
          this.form.setFieldsValue(obj)
        }
      })
    },

    handleSubmit() {
      this.form.validateFields((err, member) => {
        if (!err) {
          this.loading = true
          const member = this.form.getFieldsValue()
          member.id = this.memberId
          member.userId = this.userId
          update(member).then((r) => {
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

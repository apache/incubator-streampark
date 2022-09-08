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
    @close="onClose"
    :visible="visible"
    style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
    <template slot="title">
      <a-icon type="user" />
      Modify User
    </template>
    <a-form
      :form="form">
      <a-form-item
        label="User Name"
        v-bind="formItemLayout">
        <a-input
          read-only
          v-decorator="['username']" />
      </a-form-item>
      <a-form-item
        label="Nick Name"
        v-bind="formItemLayout">
        <a-input
          read-only
          v-decorator="['nickName']" />
      </a-form-item>
      <a-form-item
        label="E-Mail"
        v-bind="formItemLayout">
        <a-input
          v-decorator="[
            'email',
            {rules: [
              { type: 'email', message: 'please enter a valid email address' },
              { max: 50, message: 'exceeds maximum length limit of 50 characters'}
            ]}
          ]" />
      </a-form-item>
      <a-form-item
        label="Role"
        v-bind="formItemLayout">
        <a-select
          mode="multiple"
          :allow-clear="true"
          style="width: 100%"
          v-decorator="[
            'roleId',
            {rules: [{ required: true, message: 'please select role' }]}
          ]">
          <a-select-option
            v-for="r in roleData"
            :key="r.roleId.toString()">
            {{ r.roleName }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        label="Status"
        v-bind="formItemLayout">
        <a-radio-group
          v-decorator="[
            'status',
            {rules: [{ required: true, message: 'please select status' }]}
          ]">
          <a-radio
            value="0">
            locked
          </a-radio>
          <a-radio
            value="1">
            effective
          </a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item
        label="Gender"
        v-bind="formItemLayout">
        <a-radio-group
          v-decorator="[
            'sex',
            {rules: [{ required: true, message: 'please select gender' }]}
          ]">
          <a-radio
            value="0">
            male
          </a-radio>
          <a-radio
            value="1">
            female
          </a-radio>
          <a-radio
            value="2">
            secret
          </a-radio>
        </a-radio-group>
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
import { mapState, mapMutations } from 'vuex'
import { list as getRole } from '@/api/role'
import { update, get } from '@/api/user'

const formItemLayout = {
  labelCol: { span: 4 },
  wrapperCol: { span: 18 }
}
export default {
  name: 'UserEdit',
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
      userTypeData: [
        { 'value': 1, 'name': '内部用户' },
        { 'value': 2, 'name': '外部用户' }
      ],
      roleData: [],
      userId: '',
      loading: false
    }
  },

  computed: {
    ...mapState({
      currentUser: state => state.account.user
    })
  },

  methods: {
    ...mapMutations({
      setUser: 'account/setUser'
    }),

    onClose () {
      this.loading = false
      this.form.resetFields()
      this.$emit('close')
    },

    setFormValues ({ ...user }) {
      this.userId = user.userId
      this.userType = user.userType
      const fields = ['username', 'nickName', 'email', 'status', 'sex']
      Object.keys(user).forEach((key) => {
        if (fields.indexOf(key) !== -1) {
          this.form.getFieldDecorator(key)
          const obj = {}
          obj[key] = user[key]
          this.form.setFieldsValue(obj)
        }
      })

      if (user.roleId) {
        this.form.getFieldDecorator('roleId')
        const roles = user.roleId.split(',')
        this.form.setFieldsValue({ 'roleId': roles })
      }
    },

    handleSubmit () {
      this.form.validateFields((err, values) => {
        if (!err) {
          this.loading = true
          const user = this.form.getFieldsValue()
          user.roleId = user.roleId.join(',')
          user.userId = this.userId
          update(user).then((r) => {
            if (r.status === 'success') {
              this.loading = false
              this.$emit('success')
              // 如果修改用户就是当前登录用户的话，更新其state
              if (user.username === this.currentUser.username) {
                get({
                  username: user.username
                }).then((r) => {
                  this.setUser(r.data)
                })
              }
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
    visible () {
      if (this.visible) {
        getRole({ 'pageSize': '9999' }).then((resp) => {
          this.roleData = resp.data.records
        })
      }
    }
  }
}
</script>

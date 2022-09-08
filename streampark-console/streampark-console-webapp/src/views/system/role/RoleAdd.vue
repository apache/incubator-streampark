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
    :visible="roleAddVisiable"
    style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
    <template slot="title">
      <a-icon type="smile" />
      Add New Role
    </template>
    <a-form
      :form="form">
      <a-form-item
        label="Role Name"
        v-bind="formItemLayout"
        :validate-status="validateStatus"
        :help="help">
        <a-input
          @blur="handleRoleNameBlur"
          v-decorator="['roleName']" />
      </a-form-item>
      <a-form-item
        label="Description"
        v-bind="formItemLayout">
        <a-textarea
          :rows="4"
          v-decorator="[
            'remark',
            {rules: [
              { max: 50, message: '长度不能超过50个字符'}
            ]}]" />
      </a-form-item>
      <a-form-item
        label="Permission"
        style="margin-bottom: 2rem"
        :validate-status="menuSelectStatus"
        :help="menuSelectHelp"
        v-bind="formItemLayout">
        <a-tree
          :key="menuTreeKey"
          ref="menuTree"
          :checkable="true"
          :check-strictly="checkStrictly"
          @check="handleCheck"
          @expand="handleExpand"
          :expanded-keys="expandedKeys"
          :tree-data="menuTreeData" />
      </a-form-item>
    </a-form>
    <div
      class="drawer-bootom-button">
      <a-button
        style="margin-right: .8rem"
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
import { list as getMenu } from '@/api/menu'
import { checkName, post } from '@/api/role'

const formItemLayout = {
  labelCol: { span: 4 },
  wrapperCol: { span: 18 }
}
export default {
  name: 'RoleAdd',
  props: {
    roleAddVisiable: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      menuTreeKey: +new Date(),
      loading: false,
      formItemLayout,
      form: this.$form.createForm(this),
      validateStatus: '',
      menuSelectStatus: '',
      help: '',
      menuSelectHelp: '',
      checkedKeys: [],
      expandedKeys: [],
      menuTreeData: [],
      allTreeKeys: [],
      checkStrictly: false,
      selectedKeysAndHalfCheckedKeys: [],
    }
  },
  methods: {
    reset () {
      this.menuTreeKey = +new Date()
      this.expandedKeys = this.checkedKeys = []
      this.validateStatus = this.help = ''
      this.loading = false
      this.form.resetFields()
    },
    onClose () {
      this.reset()
      this.$emit('close')
    },
    expandAll () {
      this.expandedKeys = this.allTreeKeys
    },
    closeAll () {
      this.expandedKeys = []
    },
    enableRelate () {
      this.checkStrictly = false
    },
    disableRelate () {
      this.checkStrictly = true
    },
    handleCheck (checkedKeys, info) {
      // 半选中的父节点不参与校验
      this.selectedKeysAndHalfCheckedKeys = checkedKeys.concat(info.halfCheckedKeys)
      this.checkedKeys = checkedKeys
      const checkedArr = Object.is(checkedKeys.checked, undefined) ? checkedKeys : checkedKeys.checked
      if (checkedArr.length) {
        this.menuSelectStatus = ''
        this.menuSelectHelp = ''
      } else {
        this.menuSelectStatus = 'error'
        this.menuSelectHelp = '请选择相应的权限'
      }
    },
    handleExpand (expandedKeys) {
      this.expandedKeys = expandedKeys
    },
    handleSubmit () {
      const checkedArr = this.selectedKeysAndHalfCheckedKeys
      if (this.validateStatus !== 'success') {
        this.handleRoleNameBlur()
      } else if (checkedArr.length === 0) {
        this.menuSelectStatus = 'error'
        this.menuSelectHelp = '请选择相应的权限'
      } else {
        this.form.validateFields((err, values) => {
          if (!err) {
            this.loading = true
            const role = this.form.getFieldsValue()
            role.menuId = checkedArr.join(',')
            post({
              ...role
            }).then((resp) => {
              if (resp.status === 'success') {
                this.reset()
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
    handleRoleNameBlur (e) {
      const roleName = (e && e.target.value) || ''
      if (roleName.length) {
        if (roleName.length > 10) {
          this.validateStatus = 'error'
          this.help = '角色名称不能超过10个字符'
        } else {
          this.validateStatus = 'validating'
          checkName({
            roleName: roleName
          }).then((resp) => {
            if (resp.data) {
              this.validateStatus = 'success'
              this.help = ''
            } else {
              this.validateStatus = 'error'
              this.help = '抱歉，该角色名称已存在'
            }
          })
        }
      } else {
        this.validateStatus = 'error'
        this.help = '角色名称不能为空'
      }
    }
  },
  watch: {
    roleAddVisiable () {
      if (this.roleAddVisiable) {
        getMenu().then((r) => {
          this.menuTreeData = r.data.rows.children
          this.allTreeKeys = r.data.ids
        })
      }
    }
  }
}
</script>

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
    title="Modify button"
    :mask-closable="false"
    width="650"
    placement="right"
    :closable="true"
    @close="onClose"
    :visible="buttonEditVisiable"
    style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
    <a-form
      :form="form">
      <a-form-item
        label="button name"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['menuName',
                        {rules: [
                          { required: true, message: 'Button name cannot be empty'},
                          { max: 20, message: 'Length cannot exceed 20 characters'}
                        ]}]" />
      </a-form-item>
      <a-form-item
        label="Related permissions"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['perms',
                        {rules: [
                          { max: 50, message: 'Length cannot exceed 50 characters'}
                        ]}]" />
      </a-form-item>
      <a-form-item
        label="parent menu"
        style="margin-bottom: 2rem"
        v-bind="formItemLayout">
        <a-tree
          :key="menuTreeKey"
          :checkable="true"
          :check-strictly="true"
          @check="handleCheck"
          @expand="handleExpand"
          :expanded-keys="expandedKeys"
          :default-checked-keys="defaultCheckedKeys"
          :tree-data="menuTreeData" />
      </a-form-item>
    </a-form>
    <div
      class="drawer-bootom-button">
      <a-dropdown
        :trigger="['click']"
        placement="topCenter">
        <a-menu
          slot="overlay">
          <a-menu-item
            key="1"
            @click="expandAll">
            expand all
          </a-menu-item>
          <a-menu-item
            key="2"
            @click="closeAll">
            close all
          </a-menu-item>
        </a-menu>
        <a-button>
          tree operate <a-icon
            type="up" />
        </a-button>
      </a-dropdown>
      <a-button
        @click="onClose">
        cancel
      </a-button>
      <a-button
        @click="handleSubmit"
        type="primary"
        :loading="loading">
        submit
      </a-button>
    </div>
  </a-drawer>
</template>

<script>
import { update, list } from '@/api/menu'
const formItemLayout = {
  labelCol: { span: 4 },
  wrapperCol: { span: 18 }
}
export default {
  name: 'ButtonEdit',
  props: {
    buttonEditVisiable: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      loading: false,
      formItemLayout,
      form: this.$form.createForm(this),
      menuTreeKey: +new Date(),
      button: {},
      checkedKeys: [],
      expandedKeys: [],
      defaultCheckedKeys: [],
      menuTreeData: []
    }
  },
  methods: {
    reset () {
      this.loading = false
      this.menuTreeKey = +new Date()
      this.expandedKeys = this.checkedKeys = this.defaultCheckedKeys = []
      this.button = {}
      this.form.resetFields()
    },
    onClose () {
      this.reset()
      this.$emit('close')
    },
    handleCheck (checkedKeys) {
      this.checkedKeys = checkedKeys
    },
    expandAll () {
      this.expandedKeys = this.allTreeKeys
    },
    closeAll () {
      this.expandedKeys = []
    },
    handleExpand (expandedKeys) {
      this.expandedKeys = expandedKeys
    },
    setFormValues ({ ...menu }) {
      this.form.getFieldDecorator('menuName')
      this.form.setFieldsValue({ 'menuName': menu.text })
      this.form.getFieldDecorator('perms')
      this.form.setFieldsValue({ 'perms': menu.permission })

      this.defaultCheckedKeys.push(menu.parentId)
      this.checkedKeys = this.defaultCheckedKeys
      this.expandedKeys = this.checkedKeys
      this.button.menuId = menu.id
    },
    handleSubmit () {
      const checkedArr = Object.is(this.checkedKeys.checked, undefined) ? this.checkedKeys : this.checkedKeys.checked
      if (!checkedArr.length) {
        this.$message.error('Please select a parent menu for the button')
        return
      }
      if (checkedArr.length > 1) {
        this.$message.error('At most one parent menu can be selected, please modify')
        return
      }
      this.form.validateFields((err, values) => {
        if (!err) {
          this.loading = true
          const button = this.form.getFieldsValue()
          button.parentId = checkedArr[0]
          // 0 for menu 1 for button
          button.type = '1'
          button.menuId = this.button.menuId
          update({
            ...button
          }).then((resp) => {
            if (resp.success === 'success') {
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
  watch: {
    buttonEditVisiable () {
      if (this.buttonEditVisiable) {
        list({
          type: '0'
        }).then((r) => {
          const data = r.data
          this.menuTreeData = data.rows.children
          this.allTreeKeys = data.ids
          this.menuTreeKey = +new Date()
        })
      }
    }
  }
}
</script>

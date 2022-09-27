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
    :visible="menuAddVisiable"
    style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
    <template slot="title">
      <a-icon type="menu" />
      Add menu
    </template>
    <a-form
      :form="form">
      <a-form-item
        label="menu name"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['menuName',
                        {rules: [
                          { required: true, message: 'Menu name cannot be empty'},
                          { max: 20, message: 'Length cannot exceed 20 characters'}
                        ]}]" />
      </a-form-item>
      <a-form-item
        label="Menu URL"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['path',
                        {rules: [
                          { required: true, message: 'Menu URL cannot be empty'},
                          { max: 50, message: 'Length cannot exceed 50 characters'}
                        ]}]" />
      </a-form-item>
      <a-form-item
        label="component address"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['component',
                        {rules: [
                          { required: true, message: 'Component address cannot be empty'},
                          { max: 100, message: 'Length cannot exceed 100 characters'}
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
        label="menu icon"
        v-bind="formItemLayout">
        <a-input
          placeholder="Click the right button to select the icon"
          v-model="icon">
          <a-icon
            v-if="icon"
            slot="suffix"
            type="close-circle"
            @click="deleteIcons" />
          <a-icon
            slot="addonAfter"
            type="setting"
            style="cursor: pointer"
            @click="chooseIcons" />
        </a-input>
      </a-form-item>
      <a-form-item
        label="menu sorting"
        v-bind="formItemLayout">
        <a-input-number
          v-decorator="['orderNum']"
          style="width: 100%" />
      </a-form-item>

      <a-form-item
        label="whether to display"
        v-bind="formItemLayout">
        <a-switch
          v-decorator="['display']"
          checked-children="Yes"
          un-checked-children="No"
          default-checked
          :checked="display"
          @change="display=!display" />
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
          :tree-data="menuTreeData" />
      </a-form-item>
    </a-form>
    <div class="drawer-bootom-button">
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
    <icons
      @choose="handleIconChoose"
      @close="handleIconCancel"
      :icon-choose-visible="iconChooseVisible" />
  </a-drawer>
</template>
<script>
import Icons from './Icons'
import { list, post } from '@/api/menu'

const formItemLayout = {
  labelCol: { span: 4 },
  wrapperCol: { span: 18 }
}
export default {
  name: 'MenuAdd',
  components: { Icons },
  props: {
    menuAddVisiable: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      display: true,
      loading: false,
      formItemLayout,
      form: this.$form.createForm(this),
      icon: '',
      menuTreeKey: +new Date(),
      checkedKeys: [],
      expandedKeys: [],
      menuTreeData: [],
      iconChooseVisible: false
    }
  },
  methods: {
    reset () {
      this.loading = false
      this.menuTreeKey = +new Date()
      this.expandedKeys = this.checkedKeys = []
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
    chooseIcons () {
      this.iconChooseVisible = true
    },
    handleIconCancel () {
      this.iconChooseVisible = false
    },
    handleIconChoose (value) {
      this.icon = value
      this.iconChooseVisible = false
    },
    deleteIcons () {
      this.icon = ''
    },
    handleSubmit () {
      const checkedArr = Object.is(this.checkedKeys.checked, undefined) ? this.checkedKeys : this.checkedKeys.checked
      if (checkedArr.length > 1) {
        this.$message.error('At most one parent menu can be selected, please modify')
        return
      }
      this.form.validateFields((err, values) => {
        if (!err) {
          this.loading = true
          const menu = this.form.getFieldsValue()
          if (checkedArr.length) {
            menu.parentId = checkedArr[0]
          } else {
            menu.parentId = ''
          }
          // 0 for menu 1 for button
          menu.type = '0'
          menu.icon = this.icon
          menu.display = this.display
          post({
            ...menu
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
  watch: {
    menuAddVisiable () {
      if (this.menuAddVisiable) {
        list({
          type: '0'
        }).then((resp) => {
          const data = resp.data
          this.menuTreeData = data.rows.children
          this.allTreeKeys = data.ids
        })
      }
    }
  }
}
</script>

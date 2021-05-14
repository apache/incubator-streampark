<template>
  <a-drawer
    title="修改按钮"
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
        label="按钮名称"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['menuName',
                        {rules: [
                          { required: true, message: '按钮名称不能为空'},
                          { max: 20, message: '长度不能超过20个字符'}
                        ]}]" />
      </a-form-item>
      <a-form-item
        label="相关权限"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['perms',
                        {rules: [
                          { max: 50, message: '长度不能超过50个字符'}
                        ]}]" />
      </a-form-item>
      <a-form-item
        label="上级菜单"
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
            展开所有
          </a-menu-item>
          <a-menu-item
            key="2"
            @click="closeAll">
            合并所有
          </a-menu-item>
        </a-menu>
        <a-button>
          树操作 <a-icon
            type="up" />
        </a-button>
      </a-dropdown>
      <a-button
        @click="onClose">
        取消
      </a-button>
      <a-button
        @click="handleSubmit"
        type="primary"
        :loading="loading">
        提交
      </a-button>
    </div>
  </a-drawer>
</template>

<script>
import { update, list } from '@/api/menu'
const formItemLayout = {
  labelCol: { span: 3 },
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
        this.$message.error('请为按钮选择一个上级菜单')
        return
      }
      if (checkedArr.length > 1) {
        this.$message.error('最多只能选择一个上级菜单，请修改')
        return
      }
      this.form.validateFields((err, values) => {
        if (!err) {
          this.loading = true
          const button = this.form.getFieldsValue()
          button.parentId = checkedArr[0]
          // 0 表示菜单 1 表示按钮
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

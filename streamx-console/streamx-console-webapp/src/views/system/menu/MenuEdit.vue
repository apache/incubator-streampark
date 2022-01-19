<template>
  <a-drawer
    :mask-closable="false"
    width="650"
    placement="right"
    :closable="true"
    @close="onClose"
    :visible="menuEditVisiable"
    style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
    <template slot="title">
      <a-icon type="menu" />
      修改菜单
    </template>
    <a-form
      :form="form">
      <a-form-item
        label="菜单名称"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['menuName',
                        {rules: [
                          { required: true, message: '菜单名称不能为空'},
                          { max: 20, message: '长度不能超过20个字符'}
                        ]}]" />
      </a-form-item>
      <a-form-item
        label="菜单URL"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['path',
                        {rules: [
                          { required: true, message: '菜单URL不能为空'},
                          { max: 50, message: '长度不能超过50个字符'}
                        ]}]" />
      </a-form-item>
      <a-form-item
        label="组件地址"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['component',
                        {rules: [
                          { required: true, message: '组件地址不能为空'},
                          { max: 100, message: '长度不能超过100个字符'}
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
        label="菜单图标"
        v-decorator="['icon']"
        v-bind="formItemLayout">
        <a-input
          placeholder="点击右侧按钮选择图标"
          v-model="menu.icon">
          <a-icon
            v-if="menu.icon"
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
        label="菜单排序"
        v-bind="formItemLayout">
        <a-input-number
          v-decorator="['orderNum']"
          style="width: 100%" />
      </a-form-item>

      <a-form-item
        label="是否显示"
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
        label="上级菜单"
        style="margin-bottom: 2rem"
        v-bind="formItemLayout">
        <a-tree
          ref="menuTree"
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
import { list, update } from '@/api/menu'

const formItemLayout = {
  labelCol: { span: 4 },
  wrapperCol: { span: 18 }
}
export default {
  name: 'MenuEdit',
  components: { Icons },
  props: {
    menuEditVisiable: {
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
      menu: {
        icon: ''
      },
      display: true,
      checkedKeys: [],
      expandedKeys: [],
      menuTreeData: [],
      defaultCheckedKeys: [],
      iconChooseVisible: false
    }
  },
  methods: {
    reset () {
      this.loading = false
      this.menuTreeKey = +new Date()
      this.expandedKeys = this.checkedKeys = this.defaultCheckedKeys = []
      this.menu = {}
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
      this.menu.icon = value
      this.iconChooseVisible = false
    },
    deleteIcons () {
      this.menu.icon = ''
    },
    setFormValues ({ ...menu }) {
      this.display = menu.display === '1'
      const fields = ['path', 'component', 'icon']
      Object.keys(menu).forEach((key) => {
        if (fields.indexOf(key) !== -1) {
          this.form.getFieldDecorator(key)
          const obj = {}
          obj[key] = menu[key]
          this.form.setFieldsValue(obj)
        }
      })
      this.form.getFieldDecorator('menuName')
      this.form.setFieldsValue({ 'menuName': menu.text })
      this.form.getFieldDecorator('perms')
      this.form.setFieldsValue({ 'perms': menu.permission })
      this.form.getFieldDecorator('orderNum')
      this.form.setFieldsValue({ 'orderNum': menu.order })

      this.menu.icon = menu.icon
      if (menu.parentId !== '0') {
        this.defaultCheckedKeys.push(menu.parentId)
        this.checkedKeys = this.defaultCheckedKeys
        this.expandedKeys = this.checkedKeys
      }
      this.menu.menuId = menu.id
      this.menuTreeKey = +new Date()
    },
    handleSubmit () {
      const checkedArr = Object.is(this.checkedKeys.checked, undefined) ? this.checkedKeys : this.checkedKeys.checked
      if (checkedArr.length > 1) {
        this.$message.error('最多只能选择一个上级菜单，请修改')
        return
      }
      if (checkedArr[0] === this.menu.menuId) {
        this.$message.error('不能选择自己作为上级菜单，请修改')
        return
      }
      this.form.validateFields((err, values) => {
        if (!err) {
          this.loading = true
          const menu = this.form.getFieldsValue()
          Object.assign(menu, this.menu)
          if (checkedArr.length) {
            menu.parentId = checkedArr[0]
          } else {
            menu.parentId = ''
          }
          // 0 表示菜单 1 表示按钮
          menu.type = '0'
          menu.display = this.display ? '1' : '0'
          update({
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
    menuEditVisiable () {
      if (this.menuEditVisiable) {
        list({
          type: '0'
        }).then((resp) => {
          const data = resp.data
          this.menuTreeData = data.rows.children
          this.allTreeKeys = data.ids
          this.menuTreeKey = +new Date()
        })
      }
    }
  }
}
</script>

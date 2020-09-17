<template>
  <a-drawer
    title="新增菜单"
    :maskClosable="false"
    width="650"
    placement="right"
    :closable="false"
    @close="onClose"
    :visible="menuAddVisiable"
    style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
    <a-form :form="form">
      <a-form-item label="菜单名称" v-bind="formItemLayout">
        <a-input
          v-decorator="['menuName',
                        {rules: [
                          { required: true, message: '菜单名称不能为空'},
                          { max: 10, message: '长度不能超过10个字符'}
                        ]}]"/>
      </a-form-item>
      <a-form-item
        label="菜单URL"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['path',
                        {rules: [
                          { required: true, message: '菜单URL不能为空'},
                          { max: 50, message: '长度不能超过50个字符'}
                        ]}]"/>
      </a-form-item>
      <a-form-item
        label="组件地址"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['component',
                        {rules: [
                          { required: true, message: '组件地址不能为空'},
                          { max: 100, message: '长度不能超过100个字符'}
                        ]}]"/>
      </a-form-item>
      <a-form-item label="相关权限" v-bind="formItemLayout">
        <a-input
          v-decorator="['perms',
                        {rules: [
                          { max: 50, message: '长度不能超过50个字符'}
                        ]}]"/>
      </a-form-item>
      <a-form-item
        label="菜单图标"
        v-bind="formItemLayout">
        <a-input placeholder="点击右侧按钮选择图标" v-model="icon">
          <a-icon v-if="icon" slot="suffix" type="close-circle" @click="deleteIcons"/>
          <a-icon slot="addonAfter" type="setting" style="cursor: pointer" @click="chooseIcons"/>
        </a-input>
      </a-form-item>
      <a-form-item label="菜单排序" v-bind="formItemLayout">
        <a-input-number v-decorator="['orderNum']" style="width: 100%"/>
      </a-form-item>

      <a-form-item label="是否显示" v-bind="formItemLayout">
        <a-switch
          v-decorator="['display']"
          checkedChildren="Yes"
          unCheckedChildren="No"
          defaultChecked
          :checked="display"
          @change="display=!display"/>
      </a-form-item>

      <a-form-item
        label="上级菜单"
        style="margin-bottom: 2rem"
        v-bind="formItemLayout">
        <a-tree
          :key="menuTreeKey"
          :checkable="true"
          :checkStrictly="true"
          @check="handleCheck"
          @expand="handleExpand"
          :expandedKeys="expandedKeys"
          :treeData="menuTreeData">
        </a-tree>
      </a-form-item>
    </a-form>
    <div class="drawer-bootom-button">
      <a-dropdown style="float: left" :trigger="['click']" placement="topCenter">
        <a-menu slot="overlay">
          <a-menu-item key="1" @click="expandAll">展开所有</a-menu-item>
          <a-menu-item key="2" @click="closeAll">合并所有</a-menu-item>
        </a-menu>
        <a-button>
          树操作
          <a-icon type="up"/>
        </a-button>
      </a-dropdown>
      <a-button style="margin-right: .8rem" @click="onClose">取消</a-button>
      <a-button @click="handleSubmit" type="primary" :loading="loading">提交</a-button>
    </div>
    <icons
      @choose="handleIconChoose"
      @close="handleIconCancel"
      :iconChooseVisible="iconChooseVisible">
    </icons>
  </a-drawer>
</template>
<script>
import Icons from './Icons'
import { list, post } from '@/api/menu'

const formItemLayout = {
  labelCol: { span: 3 },
  wrapperCol: { span: 18 }
}
export default {
  name: 'MenuAdd',
  components: { Icons },
  props: {
    menuAddVisiable: {
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
        this.$message.error('最多只能选择一个上级菜单，请修改')
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
          // 0 表示菜单 1 表示按钮
          menu.type = '0'
          menu.icon = this.icon
          menu.display = this.display ? '1' : '0'
          post({
            ...menu
          }).then(() => {
            this.reset()
            this.$emit('success')
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
          this.menuTreeData = resp.rows.children
          this.allTreeKeys = resp.ids
        })
      }
    }
  }
}
</script>

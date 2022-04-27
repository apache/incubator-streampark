<template>
  <a-drawer
    title="新增按钮"
    :mask-closable="false"
    width="650"
    placement="right"
    :closable="true"
    @close="onClose"
    :visible="buttonAddVisiable"
    style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
    <a-form
      :form="form">
      <a-form-item
        label="按钮名称"
        v-bind="formItemLayout">
        <a-input
          v-model="button.menuName"
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
          v-model="button.perms"
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
import { list, post as submit } from '@/api/menu'

const formItemLayout = {
  labelCol: { span: 3 },
  wrapperCol: { span: 18 }
}
export default {
  name: 'ButtonAdd',
  props: {
    buttonAddVisiable: {
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
      menuTreeData: []
    }
  },
  methods: {
    reset () {
      this.loading = false
      this.menuTreeKey = +new Date()
      this.expandedKeys = this.checkedKeys = []
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
          if (checkedArr.length) {
            this.button.parentId = checkedArr[0]
          } else {
            this.button.parentId = ''
          }
          // 0 表示菜单 1 表示按钮
          this.button.type = '1'
          submit({
            ...this.button
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
    buttonAddVisiable () {
      if (this.buttonAddVisiable) {
        list({
          type: '0'
        }).then((r) => {
          const data = r.data
          this.menuTreeData = data.rows.children
          this.allTreeKeys = data.ids
        })
      }
    }
  }
}
</script>

<template>
  <a-drawer
    :mask-closable="false"
    width="650"
    placement="right"
    :closable="true"
    @close="onClose"
    :visible="roleEditVisiable"
    style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
    <template slot="title">
      <a-icon type="smile" />
      Update Role
    </template>
    <a-form
      :form="form">
      <a-form-item
        label="Role Name"
        v-bind="formItemLayout">
        <a-input
          read-only
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
          :default-checked-keys="defaultCheckedKeys[0]"
          @check="handleCheck"
          @expand="handleExpand"
          :expanded-keys="expandedKeys"
          :tree-data="menuTreeData" />
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
import { list as getMenu } from '@/api/menu'
import { roleMenu, update } from '@/api/role'

const formItemLayout = {
  labelCol: { span: 4 },
  wrapperCol: { span: 18 }
}
export default {
  name: 'RoleEdit',
  props: {
    roleEditVisiable: {
      type: Boolean,
      default: false
    },
    roleInfoData: {
      type: Object,
      default: () => ({}),
      require: true
    }
  },
  data () {
    return {
      menuTreeKey: +new Date(),
      loading: false,
      formItemLayout,
      form: this.$form.createForm(this),
      menuSelectStatus: '',
      menuSelectHelp: '',
      role: {
        menuId: ''
      },
      menuTreeData: [],
      allTreeKeys: [],
      checkedKeys: [],
      defaultCheckedKeys: [],
      expandedKeys: [],
      checkStrictly: false,
      selectedKeysAndHalfCheckedKeys:[],
      leftNodes: []
    }
  },
  methods: {
    reset () {
      this.menuTreeKey = +new Date()
      this.expandedKeys = []
      this.checkedKeys = []
      this.defaultCheckedKeys = []
      this.menuSelectStatus = this.menuSelectHelp = ''
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
    handleCheck (checkedKeys,info) {
      // 半选中的父节点不参与校验
      this.selectedKeysAndHalfCheckedKeys =  checkedKeys.concat(info.halfCheckedKeys)
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
    setFormValues ({ ...role }) {
      const fields = ['roleName', 'remark']
      Object.keys(role).forEach((key) => {
        if (fields.indexOf(key) !== -1) {
          this.form.getFieldDecorator(key)
          const obj = {}
          obj[key] = role[key]
          this.form.setFieldsValue(obj)
        }
      })
    },
    handleSubmit () {
      const checkedArr = this.selectedKeysAndHalfCheckedKeys
      if (checkedArr.length === 0) {
        this.menuSelectStatus = 'error'
        this.menuSelectHelp = '请选择相应的权限'
      } else {
        this.form.validateFields((err, values) => {
          if (!err) {
            this.loading = true
            const role = this.form.getFieldsValue()
            role.roleId = this.roleInfoData.roleId
            role.menuId = checkedArr.join(',')
            update({
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
    // 默认父节点为 "/"
    deepList(data) {
      data.map((item) => {
        if (item.children && item.children.length >0) {
          this.deepList(item.children)
        } else {
          // 存放所有叶子节点
          this.leftNodes.push (item.id)
        }
      })
    }
  },
  watch: {
    roleEditVisiable () {
      if (this.roleEditVisiable) {
        getMenu().then((r) => {
          // 得到所有叶子节点
          this.deepList(r.data.rows.children)
          const data = r.data
          this.menuTreeData = data.rows.children
          this.allTreeKeys = data.ids
          roleMenu({
            roleId: this.roleInfoData.roleId
          }).then((resp) => {
            // 后台返回的数据与叶子节点做交集,得到选中的子节点
            const result = [...new Set(this.leftNodes)].filter((item) => new Set(eval(resp.data)).has(item))
            //将结果赋值给v-model绑定的属性
            this.checkedKeys = [...result]
            const data = this.checkedKeys
            this.defaultCheckedKeys.splice(0, this.defaultCheckedKeys.length, data)
            this.checkedKeys = data
            this.expandedKeys = data
            this.menuTreeKey = +new Date()
          })
        })
      }
    }
  }
}
</script>

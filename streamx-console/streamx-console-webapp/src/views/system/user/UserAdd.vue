<template>
  <a-drawer
    title="新增用户"
    :maskClosable="false"
    width="650"
    placement="right"
    :closable="false"
    @close="onClose"
    :visible="userAddVisiable"
    style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
    <a-form :form="form">
      <a-form-item
        label="用户名"
        v-bind="formItemLayout"
        :validateStatus="validateStatus"
        :help="help">
        <a-input
          @blur="handleUserNameBlur"
          v-decorator="
            ['username',
             {rules: [{ required: true, message: '用户名不能为空'}]}]"/>
      </a-form-item>
      <a-form-item
        label="昵称"
        v-bind="formItemLayout"
        :validateStatus="validateStatus"
        :help="help">
        <a-input v-decorator="['nickName',{rules: [{ required: true, message: '昵称不能为空'}]}]"/>
      </a-form-item>
      <a-form-item label="密码" v-bind="formItemLayout">
        <a-input
          v-decorator="['password',{rules: [
            { required: true, message: '密码不能为空'},
            { min: 8, message: '密码长度不能少于8个字符'}
          ]}]"/>
      </a-form-item>
      <a-form-item label="邮箱" v-bind="formItemLayout">
        <a-input
          v-decorator="['email',{rules: [
            { type: 'email', message: '请输入正确的邮箱' },
            { max: 50, message: '长度不能超过50个字符'}
          ]}]"/>
      </a-form-item>
      <a-form-item label="手机" v-bind="formItemLayout">
        <a-input
          v-decorator="['mobile', {rules: [
            { pattern: '^0?(13[0-9]|15[012356789]|17[013678]|18[0-9]|14[57])[0-9]{8}$', message: '请输入正确的手机号'}
          ]}]"/>
      </a-form-item>
      <a-form-item label="角色" v-bind="formItemLayout">
        <a-select
          mode="multiple"
          :allowClear="true"
          style="width: 100%"
          v-decorator="['roleId',{rules: [{ required: true, message: '请选择角色' }]}]">
          <a-select-option v-for="r in roleData" :key="r.roleId">{{ r.roleName }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="部门" v-bind="formItemLayout">
        <a-tree-select
          :allowClear="true"
          :dropdownStyle="{ maxHeight: '220px', overflow: 'auto' }"
          :treeData="deptTreeData"
          v-decorator="['deptId']">
        </a-tree-select>
      </a-form-item>
      <a-form-item label="状态" v-bind="formItemLayout">
        <a-radio-group
          v-decorator="['status',{rules: [{ required: true, message: '请选择状态'}]}]">
          <a-radio value="0">锁定</a-radio>
          <a-radio value="1">有效</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="性别" v-bind="formItemLayout">
        <a-radio-group
          v-decorator="['sex',{rules: [{ required: true, message: '请选择性别' }]}]">
          <a-radio value="0">男</a-radio>
          <a-radio value="1">女</a-radio>
          <a-radio value="2">保密</a-radio>
        </a-radio-group>
      </a-form-item>
    </a-form>
    <div class="drawer-bootom-button">
      <a-button style="margin-right: .8rem" @click="onClose">取消</a-button>
      <a-button @click="handleSubmit" type="primary" :loading="loading">提交</a-button>
    </div>
  </a-drawer>
</template>
<script>
import { list as getDept } from '@/api/dept'
import { list as getRole } from '@/api/role'
import { checkUserName, post } from '@/api/user'

const formItemLayout = {
  labelCol: { span: 3 },
  wrapperCol: { span: 18 }
}
export default {
  name: 'UserAdd',
  props: {
    userAddVisiable: {
      default: false
    }
  },
  data () {
    return {
      loading: false,
      roleData: [],
      deptTreeData: [],
      formItemLayout,
      form: this.$form.createForm(this),
      validateStatus: '',
      help: ''
    }
  },
  methods: {
    reset () {
      this.validateStatus = ''
      this.help = ''
      this.loading = false
      this.form.resetFields()
    },
    onClose () {
      this.reset()
      this.$emit('close')
    },
    handleSubmit () {
      if (this.validateStatus !== 'success') {
        this.handleUserNameBlur()
      }
      this.form.validateFields((err, user) => {
        if (!err && this.validateStatus === 'success') {
          user.roleId = user.roleId.join(',')
          post({
            ...user
          }).then((r) => {
            this.reset()
            this.$emit('success')
          }).catch(() => {
            this.loading = false
          })
        }
      })
    },
    handleUserNameBlur (e) {
      const username = (e && e.target.value) || ''
      if (username.length) {
        if (username.length > 20) {
          this.validateStatus = 'error'
          this.help = '用户名不能超过10个字符'
        } else if (username.length < 4) {
          this.validateStatus = 'error'
          this.help = '用户名不能少于4个字符'
        } else {
          this.validateStatus = 'validating'
          checkUserName({
            username: username
          }).then((r) => {
            if (r) {
              this.validateStatus = 'success'
              this.help = ''
            } else {
              this.validateStatus = 'error'
              this.help = '抱歉，该用户名已存在'
            }
          })
        }
      } else {
        this.validateStatus = 'error'
        this.help = '用户名不能为空'
      }
    }
  },
  watch: {
    userAddVisiable () {
      if (this.userAddVisiable) {
        getRole(
          { 'pageSize': '9999' }
        ).then((r) => {
          this.roleData = r.rows
        })
        getDept().then((r) => {
          this.deptTreeData = r.rows.children
        })
      }
    }
  }
}
</script>

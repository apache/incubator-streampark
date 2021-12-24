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
      添加用户
    </template>
    <a-form
      :form="form">
      <a-form-item
        label="User Name"
        v-bind="formItemLayout"
        :validate-status="validateStatus"
        :help="help">
        <a-input
          @blur="handleUserNameBlur"
          v-decorator="
            ['username',
             {rules: [{ required: true }]}]" />
      </a-form-item>
      <a-form-item
        label="Nick Name"
        v-bind="formItemLayout"
        :validate-status="validateStatus"
        :help="help">
        <a-input
          v-decorator="['nickName',{rules: [{ required: true }]}]" />
      </a-form-item>
      <a-form-item
        label="Password"
        v-bind="formItemLayout">
        <a-input-password
          placeholder="input password"
          v-decorator="['password',{rules: [
            { required: true, message: 'password is required'},
            { min: 8, message: 'Password length cannot be less than 8 characters'}
          ]}]" />
      </a-form-item>
      <a-form-item
        label="Mail"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['email',{rules: [
            { type: 'email', message: '请输入正确的邮箱' },
            { max: 50, message: '长度不能超过50个字符'}
          ]}]" />
      </a-form-item>
      <a-form-item
        label="Mobile"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['mobile', {rules: [
            { pattern: '^0?(13[0-9]|15[012356789]|17[013678]|18[0-9]|14[57])[0-9]{8}$', message: '请输入正确的手机号'}
          ]}]" />
      </a-form-item>
      <a-form-item
        label="Description"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['description',{rules: [
            { max: 100, message: '长度不能超过100个字符'}
          ]}]" />
      </a-form-item>
      <a-form-item
        label="Role"
        v-bind="formItemLayout">
        <a-select
          mode="multiple"
          :allow-clear="true"
          style="width: 100%"
          v-decorator="['roleId',{rules: [{ required: true, message: 'please select role' }]}]">
          <a-select-option
            v-for="r in roleData"
            :key="r.roleId">
            {{ r.roleName }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        label="Status"
        v-bind="formItemLayout">
        <a-radio-group
          v-decorator="['status',{rules: [{ required: true, message: 'please select status'}]}]">
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
          v-decorator="['sex',{rules: [{ required: true, message: 'please select gender' }]}]">
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
import { list as getRole } from '@/api/role'
import { checkUserName, post } from '@/api/user'

const formItemLayout = {
  labelCol: { span: 4 },
  wrapperCol: { span: 18 }
}
export default {
  name: 'UserAdd',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      loading: false,
      roleData: [],
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
            if (r.status === 'success') {
              this.reset()
              this.$emit('success')
            }
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
            if (r.data) {
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
    visible () {
      if (this.visible) {
        getRole(
          { 'pageSize': '9999' }
        ).then((resp) => {
          this.roleData = resp.data.records
        })
      }
    }
  }
}
</script>

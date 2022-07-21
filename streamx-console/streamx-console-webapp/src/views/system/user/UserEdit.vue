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
      修改用户
    </template>
    <a-form
      :form="form">
      <a-form-item
        label="用户名"
        v-bind="formItemLayout">
        <a-input
          read-only
          v-decorator="['username']" />
      </a-form-item>
      <a-form-item
        label="昵称"
        v-bind="formItemLayout">
        <a-input
          read-only
          v-decorator="['nickName']" />
      </a-form-item>
      <a-form-item
        label="邮箱"
        v-bind="formItemLayout">
        <a-input
          v-decorator="[
            'email',
            {rules: [
              { type: 'email', message: '请输入正确的邮箱' },
              { max: 50, message: '长度不能超过50个字符'}
            ]}
          ]" />
      </a-form-item>
      <a-form-item
        label="手机"
        v-bind="formItemLayout">
        <a-input
          v-decorator="['mobile', {rules: [
            { pattern: '^0?(13[0-9]|15[012356789]|17[013678]|18[0-9]|14[57])[0-9]{8}$', message: '请输入正确的手机号'}
          ]}]" />
      </a-form-item>
      <a-form-item
        label="角色"
        v-bind="formItemLayout">
        <a-select
          @change="handleRoleEdit"
          mode="multiple"
          :allow-clear="true"
          style="width: 100%"
          v-decorator="[
            'roleId',
            {rules: [{ required: true, message: '请选择角色' }]}
          ]">
          <a-select-option
            v-for="r in roleData"
            :key="r.roleId.toString()">
            {{ r.roleName }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        v-if="!roles.includes('100000')"
        label="团队"
        v-bind="formItemLayout">
        <a-select
          mode="multiple"
          :allow-clear="true"
          style="width: 100%"
          v-decorator="[
            'teamId',
            {rules: [{ required: true, message: '请选择团队' }]}
          ]">
          <a-select-option
            v-for="t in teamData"
            :key="t.teamId.toString()">
            {{ t.teamName }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        label="状态"
        v-bind="formItemLayout">
        <a-radio-group
          v-decorator="[
            'status',
            {rules: [{ required: true, message: '请选择状态' }]}
          ]">
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
        label="性别"
        v-bind="formItemLayout">
        <a-radio-group
          v-decorator="[
            'sex',
            {rules: [{ required: true, message: '请选择性别' }]}
          ]">
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
import { mapState, mapMutations } from 'vuex'
import { listByUser as getRole } from '@/api/role'
import { listByUser as getUserTeam } from '@/api/team'
import { update, get } from '@/api/user'

const formItemLayout = {
  labelCol: { span: 3 },
  wrapperCol: { span: 18 }
}
export default {
  name: 'UserEdit',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      formItemLayout,
      form: this.$form.createForm(this),
      userTypeData: [
        { 'value': 1, 'name': '内部用户' },
        { 'value': 2, 'name': '外部用户' }
      ],
      roleData: [],
      teamData: [],
      userId: '',
      loading: false,
      roles:[]
    }
  },

  computed: {
    ...mapState({
      currentUser: state => state.account.user
    })
  },

  methods: {
    ...mapMutations({
      setUser: 'account/setUser'
    }),
    onClose () {
      this.loading = false
      this.form.resetFields()
      this.$emit('close')
    },
    setFormValues ({ ...user }) {
      this.userId = user.userId
      this.userType = user.userType
      const fields = ['username', 'nickName', 'email', 'status', 'sex', 'mobile']
      Object.keys(user).forEach((key) => {
        if (fields.indexOf(key) !== -1) {
          this.form.getFieldDecorator(key)
          const obj = {}
          obj[key] = user[key]
          this.form.setFieldsValue(obj)
        }
      })
      if (user.roleId) {
        this.form.getFieldDecorator('roleId')
        const roleArr = user.roleId.split(',')
        this.roles = roleArr
        this.form.setFieldsValue({ 'roleId': roleArr })
      }
      if (user.teamId) {
        this.form.getFieldDecorator('teamId')
        const teamArr = user.teamId.split(',')
        this.form.setFieldsValue({ 'teamId': teamArr })
      }
    },
    handleRoleEdit(v) {
      this.roles=v
    },
    handleSubmit () {
      this.form.validateFields((err, values) => {
        if (!err) {
          this.loading = true
          const user = this.form.getFieldsValue()
          user.roleId = user.roleId.join(',')
          if (user != undefined && user.teamId != undefined) {
            user.teamId = user.teamId.join(',')
          }
          user.userId = this.userId
          update(user).then((r) => {
            if (r.status === 'success') {
              this.loading = false
              this.$emit('success')
              // 如果修改用户就是当前登录用户的话，更新其state
              if (user.username === this.currentUser.username) {
                get({
                  username: user.username
                }).then((r) => {
                  this.setUser(r.data)
                })
              }
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
    visible () {
      if (this.visible) {
        getRole({ 'pageSize': '9999' }).then((resp) => {
          this.roleData = resp.data.records
        })

        getUserTeam({ 'pageSize': '9999' }).then((resp) => {
          this.teamData = resp.data.records
        })

      }
    }
  }
}
</script>

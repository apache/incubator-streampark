<template>
  <a-drawer
    title="编辑资料"
    :maskClosable="false"
    width=650
    placement="right"
    :closable="false"
    @close="onClose"
    :visible="profileEditVisiable"
    style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
    <a-form :form="form">
      <a-form-item label='邮箱' v-bind="formItemLayout">
        <a-input
          v-decorator="[
          'email',
          {rules: [
            { type: 'email', message: '请输入正确的邮箱' },
            { max: 50, message: '长度不能超过50个字符'}
          ]}
        ]"/>
      </a-form-item>
      <a-form-item label="手机" v-bind="formItemLayout">
        <a-input
          v-decorator="['mobile', {rules: [
            { pattern: '^0?(13[0-9]|15[012356789]|17[013678]|18[0-9]|14[57])[0-9]{8}$', message: '请输入正确的手机号'}
          ]}]"/>
      </a-form-item>
      <a-form-item label='部门' v-bind="formItemLayout">
        <a-tree-select
          :allowClear="true"
          :dropdownStyle="{ maxHeight: '220px', overflow: 'auto' }"
          :treeData="deptTreeData"
          @change="onDeptChange"
          :value="userDept">
        </a-tree-select>
      </a-form-item>
      <a-form-item label='性别' v-bind="formItemLayout">
        <a-radio-group
          v-decorator="[
            'sex',
            {rules: [{ required: true, message: '请选择性别' }]}
          ]">
          <a-radio value="0">男</a-radio>
          <a-radio value="1">女</a-radio>
          <a-radio value="2">保密</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label='描述' v-bind="formItemLayout">
        <a-textarea
          :rows="4"
          v-decorator="[
          'description',
          {rules: [
            { max: 100, message: '长度不能超过100个字符'}
          ]}]">
        </a-textarea>
      </a-form-item>
    </a-form>
    <div class="drawer-bootom-button">
      <a-button style="margin-right: .8rem" @click="onClose">取消</a-button>
      <a-button @click="handleSubmit" type="primary" :loading="loading">提交</a-button>
    </div>
  </a-drawer>
</template>
<script>
import {mapState, mapMutations} from 'vuex'

const formItemLayout = {
  labelCol: { span: 3 },
  wrapperCol: { span: 18 }
}
export default {
  props: {
    profileEditVisiable: {
      default: false
    }
  },
  data () {
    return {
      formItemLayout,
      form: this.$form.createForm(this),
      deptTreeData: [],
      userDept: [],
      userId: '',
      roleId: '',
      status: '',
      username: '',
      loading: false
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
    setFormValues ({...user}) {
      this.userId = user.userId
      let fields = ['email', 'sex', 'description', 'mobile']
      Object.keys(user).forEach((key) => {
        if (fields.indexOf(key) !== -1) {
          this.form.getFieldDecorator(key)
          let obj = {}
          obj[key] = user[key]
          this.form.setFieldsValue(obj)
        }
      })
      if (user.deptId) {
        this.userDept = [user.deptId]
      }
      this.status = user.status
      this.roleId = user.roleId
      this.username = user.username
    },
    onDeptChange (value) {
      this.userDept = value
    },
    handleSubmit () {
      this.form.validateFields((err, values) => {
        if (!err) {
          this.loading = true
          let user = this.form.getFieldsValue()
          user.userId = this.userId
          user.deptId = this.userDept
          user.roleId = this.roleId
          user.status = this.status
          user.username = this.username
          this.$put('user/profile', {
            ...user
          }).then((r) => {
            this.loading = false
            this.$emit('success')
            // 更新其state
            this.$get(`user/${user.username}`).then((r) => {
              this.setUser(r.data)
            })
          }).catch(() => {
            this.loading = false
          })
        }
      })
    }
  },
  watch: {
    profileEditVisiable () {
      if (this.profileEditVisiable) {
        this.$get('dept').then((r) => {
          this.deptTreeData = r.data.rows.children
        })
      }
    }
  }
}
</script>

<template>
<div>
  <!-- 密码修改 -->
  <a-modal
    title="密码修改"
    :keyboard="false"
    :maskClosable="false"
    :closable="false"
    v-model="show"
    @cancel="cancelUpdatePassword"
    @ok="handleUpdatePassword">
    <a-form :autoFormCreate="(form)=>{this.form = form}">
      <a-form-item
        label='旧密码'
        v-bind="formItemLayout"
        fieldDecoratorId="oldPassword"
        :fieldDecoratorOptions="{rules: [{ required: true, message: '请输入旧密码'}, { validator: this.handleOldPassowrd }], validateTrigger: ['blur']}">
        <a-input type="password"
                 autocomplete="false"
                 v-model="oldPassword"
                 placeholder="请输入旧密码"></a-input>
      </a-form-item>
      <a-popover placement="rightTop" trigger="click" :visible="state.passwordLevelChecked">
        <template slot="content">
          <div :style="{ width: '240px' }">
            <div :class="['update-password', passwordLevelClass]">强度：<span>{{ passwordLevelName }}</span></div>
            <a-progress :percent="state.percent" :showInfo="false" :strokeColor=" passwordLevelColor "/>
            <div style="margin-top: 10px;">
              <span>请至少输入 6 个字符。请不要使用容易被猜到的密码。</span>
            </div>
          </div>
        </template>
        <a-form-item
          label='新密码'
          v-bind="formItemLayout"
          fieldDecoratorId="password"
          :fieldDecoratorOptions="{rules: [{ required: true, message: '至少6位密码，区分大小写'}, { validator: this.handlePasswordLevel }], validateTrigger: ['change', 'blur']}">
          <a-input type="password"
                   @click="handlePasswordInputClick"
                   v-model="newPassword"
                   autocomplete="false"
                   placeholder="至少6位密码，区分大小写"></a-input>
        </a-form-item>
      </a-popover>
      <a-form-item
        label='再次确认'
        v-bind="formItemLayout"
        fieldDecoratorId="password2"
        :fieldDecoratorOptions="{rules: [{ required: true, message: '至少6位密码，区分大小写' }, { validator: this.handlePasswordCheck }], validateTrigger: ['change', 'blur']}">
        <a-input type="password" autocomplete="false" placeholder="确认密码"></a-input>
      </a-form-item>
    </a-form>
  </a-modal>
</div>
</template>
<script>
const formItemLayout = {
  labelCol: { span: 4 },
  wrapperCol: { span: 18 }
}
const levelNames = {
  0: '低',
  1: '低',
  2: '中',
  3: '强'
}
const levelClass = {
  0: 'error',
  1: 'error',
  2: 'warning',
  3: 'success'
}
const levelColor = {
  0: '#ff0000',
  1: '#ff0000',
  2: '#ff7e05',
  3: '#52c41a'
}

export default {
  props: {
    updatePasswordModelVisible: {
      default: false
    },
    user: {
      required: true
    }
  },
  data () {
    return {
      form: null,
      formItemLayout,
      state: {
        passwordLevel: 0,
        passwordLevelChecked: false,
        percent: 10,
        progressColor: '#FF0000'
      },
      oldPassword: '',
      newPassword: '',
      validateStatus: '',
      help: ''
    }
  },
  computed: {
    show: {
      get: function () {
        return this.updatePasswordModelVisible
      },
      set: function () {
      }
    },
    passwordLevelClass () {
      return levelClass[this.state.passwordLevel]
    },
    passwordLevelName () {
      return levelNames[this.state.passwordLevel]
    },
    passwordLevelColor () {
      return levelColor[this.state.passwordLevel]
    }
  },
  methods: {
    isMobile () {
      return this.$store.state.setting.isMobile
    },
    cancelUpdatePassword () {
      this.state.passwordLevelChecked = false
      this.form.resetFields()
      this.$emit('cancel')
    },
    handleUpdatePassword () {
      this.form.validateFields((err, values) => {
        if (!err) {
          this.$put('user/password', {
            password: this.newPassword,
            username: this.user.username
          }).then(() => {
            this.state.passwordLevelChecked = false
            this.$emit('success')
            this.form.resetFields()
          })
        }
      })
    },
    handlePasswordLevel (rule, value, callback) {
      let level = 0
      // 判断这个字符串中有没有数字
      if (/[0-9]/.test(value)) {
        level++
      }
      // 判断字符串中有没有字母
      if (/[a-zA-Z]/.test(value)) {
        level++
      }
      // 判断字符串中有没有特殊符号
      if (/[^0-9a-zA-Z_]/.test(value)) {
        level++
      }
      this.state.passwordLevel = level
      this.state.percent = level * 30
      if (level >= 2) {
        if (level >= 3) {
          this.state.percent = 100
        }
        callback()
      } else {
        if (level === 0) {
          this.state.percent = 10
        }
        callback(new Error('密码强度不够'))
      }
    },
    handlePasswordCheck (rule, value, callback) {
      let password = this.form.getFieldValue('password')
      if (value === undefined) {
        callback(new Error('请输入密码'))
      }
      if (value && password && value.trim() !== password.trim()) {
        callback(new Error('两次密码不一致'))
      }
      callback()
    },
    handlePasswordInputClick () {
      if (!this.isMobile()) {
        this.state.passwordLevelChecked = true
        return
      }
      this.state.passwordLevelChecked = false
    },
    handleOldPassowrd (rule, value, callback) {
      let password = this.oldPassword
      if (this.oldPassword.trim().length) {
        this.$get('user/password/check', {
          password: password,
          username: this.user.username
        }).then((r) => {
          if (r.data) {
            callback()
          } else {
            callback(new Error('旧密码不正确'))
          }
        })
      } else {
        callback()
      }
    }
  }
}
</script>
<style lang="less">
  .update-password {
    &.error {
      color: #ff0000;
    }
    &.warning {
      color: #ff7e05;
    }
    &.success {
      color: #52c41a;
    }
  }
</style>

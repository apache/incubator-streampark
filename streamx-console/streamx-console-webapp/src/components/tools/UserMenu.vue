<template>
  <div
    class="user-wrapper">
    <div class="content-box">
      <a>
        <span
          class="action">
          <a-icon
            :style="{color: themeDark ? 'rgba(255,255,255, 0.45)' : 'rgba(0,0,0, 0.55)' }"
            type="dashboard"
            @click.native="handleChangeTheme(false)"/>
        </span>
      </a>
      <a
        href="http://www.streamxhub.com/zh/doc/"
        target="_blank">
        <span
          class="action">
          <a-icon
            :style="{color: themeDark ? 'rgba(255,255,255, 0.45)' : 'rgba(0,0,0, 0.55)' }"
            type="question-circle-o" />
        </span>
      </a>
      <a-dropdown>
        <span
          style="margin-top: -10px"
          class="action ant-dropdown-link user-dropdown-menu">
          <a-avatar
            class="avatar"
            src="https://avatars.githubusercontent.com/u/13284744?s=180&u=5a0e88e9edeb806f957fc25938522d057f8a2e85&v=4" />
        </span>
        <a-menu
          slot="overlay"
          class="user-dropdown-menu-wrapper">
          <a-menu-item>
            <a
              @click="handleChangePassword">
              <a-icon type="setting" />
              <span>Change password</span>
            </a>
          </a-menu-item>
          <a-menu-item>
            <a
              @click="handleLogout">
              <a-icon type="logout" />
              <span>Sign Out</span>
            </a>
          </a-menu-item>
        </a-menu>
      </a-dropdown>
    </div>

    <a-modal
      v-model="passwordVisible"
      on-ok="handleChangeOk">
      <template
        slot="title">
        <a-icon
          slot="icon"
          type="setting"
          style="color: green"/>
        Change password
      </template>
      <a-form
        @submit="handleChangeOk"
        :form="formPassword">
        <a-form-item
          label="User Name"
          :label-col="{lg: {span: 7}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-alert
            :message="userName"
            type="info"/>
        </a-form-item>

        <a-form-item
          :label-col="{lg: {span: 7}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }"
          label="Password"
          has-feedback>
          <a-input
            v-decorator="['password',{
              rules: [ { required: true, message: 'Please input your password!' }, { validator: validateToNextPassword}]}]"
            type="password"/>
        </a-form-item>

        <a-form-item
          label="Confirm Password"
          :label-col="{lg: {span: 7}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }"
          has-feedback>
          <a-input
            v-decorator="['confirm', {
              rules: [
                { required: true, message: 'Please confirm your password!',},
                { validator: compareToFirstPassword},
              ],
            },
            ]"
            type="password"
            @blur="handleConfirmBlur"/>
        </a-form-item>
      </a-form>

      <template slot="footer">
        <a-button
          key="back"
          @click="handleChangeCancel">
          Cancel
        </a-button>
        <a-button
          key="submit"
          type="primary"
          @click="handleChangeOk">
          Submit
        </a-button>
      </template>
    </a-modal>

  </div>
</template>

<script>
import NoticeIcon from '@/components/NoticeIcon'
import SvgIcon from '@/components/SvgIcon'

import { mapState, mapActions } from 'vuex'
import { password } from '@api/user'
import themeUtil from '@/utils/themeUtil'

export default {
  name: 'UserMenu',
  components: {
    NoticeIcon,
    SvgIcon
  },

  data() {
    return {
      passwordVisible: false,
      formPassword: null,
      confirmDirty: false,
      themeDark: false
    }
  },

  computed: {
    ...mapState({
      // 动态主路由
      userName: state => state.user.name,
      myTheme: state => state.app.theme
    })
  },

  beforeMount() {
    this.formPassword = this.$form.createForm(this)
  },

  mounted() {
    this.handleChangeTheme(true)
  },

  methods: {
    ...mapActions(['SignOut','ChangeTheme']),
    handleLogout () {
      const that = this
      this.$confirm({
        content: 'Are you sure Sign Out ?',
        onOk () {
          return that.SignOut({}).then(() => {
            window.location.reload()
          }).catch(err => {
            that.$message.error({
              description: err.message
            })
          })
        },
        onCancel () {
        }
      })
    },

    handleChangePassword () {
      this.passwordVisible = true
    },

    handleChangeOk() {
      this.formPassword.validateFields((err, values) => {
        if (!err) {
          this.handleChangeCancel()
          password({
            username: this.userName,
            password: values.password
          }).then((resp) => {
            this.$swal.fire({
              icon: 'success',
              title: 'password changed successful',
              showConfirmButton: false,
              timer: 2000
            }).then((r)=> {
              this.SignOut({}).then(() => {
                window.location.reload()
              })
            })
          })
        }
      })
    },

    handleChangeCancel () {
        this.passwordVisible = false
        setTimeout(() => {
          this.formPassword.resetFields()
        }, 1000)
    },

    handleChangeTheme() {
      let _theme
      if(arguments[0]) {
        _theme = this.myTheme || 'dark'
        this.themeDark = _theme === 'dark'
        themeUtil.changeThemeColor(null, _theme)
      } else {
        this.themeDark = !this.themeDark
        _theme = this.themeDark ? 'dark': 'light'
        this.ChangeTheme(_theme)
        const closeMessage = this.$message.loading(`您选择了主题模式 ${_theme}, 正在切换...`)
        themeUtil.changeThemeColor(null, _theme).then(closeMessage)
      }
    },

    handleConfirmBlur(e) {
      const value = e.target.value
      this.confirmDirty = this.confirmDirty || !!value
    },

    compareToFirstPassword(rule, value, callback) {
      const form = this.formPassword
      if (value && value !== form.getFieldValue('password')) {
        callback('Two passwords that you enter is inconsistent!')
      } else {
        callback()
      }
    },

    validateToNextPassword(rule, value, callback) {
      const form = this.formPassword
      if (value && this.confirmDirty) {
        form.validateFields(['confirm'], { force: true })
      }
      callback()
    },

  }
}
</script>
<style scoped lang="less"></style>

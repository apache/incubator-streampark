<template>
  <div :class="['container-wrapper', device]">
    <div class="container">
      <div class="main">
        <div class="top">
          <div class="header">
            <a href="/">
              <img src="~@/assets/logo.png" class="logo" alt="logo">
            </a>
          </div>
          <div class="desc">
            StreamX, Let's Bigdata easy
          </div>
        </div>
        <a-form
          id="formLogin"
          class="login-form"
          ref="formLogin"
          :form="form"
          @submit="handleSubmit">
          <a-form-item>
            <a-input
              size="large"
              type="text"
              placeholder="帐户名或邮箱地址 / admin"
              v-decorator="[
                'username',
                {rules: [{ required: true, message: '请输入帐户名或邮箱地址' }], validateTrigger: 'change'}
              ]">
              <a-icon slot="prefix" type="user" class="icon"/>
            </a-input>
          </a-form-item>

          <a-form-item>
            <a-input
              size="large"
              type="password"
              autocomplete="false"
              placeholder="密码 / admin"
              v-decorator="[
                'password',
                {rules: [{ required: true, message: '请输入密码' }], validateTrigger: 'blur'}
              ]">
              <a-icon slot="prefix" type="lock" class="icon"/>
            </a-input>
          </a-form-item>

          <a-form-item>
            <a-switch defaultChecked @change="handleRember"/>
            <span style="margin-left: 10px; color: rgba(255,255,255,.65)">自动登录</span>
          </a-form-item>

          <a-form-item style="margin-top:24px">
            <a-button
              size="large"
              type="primary"
              htmlType="submit"
              class="login-button"
              :loading="state.loginBtn"
              :disabled="state.loginBtn">确定
            </a-button>
          </a-form-item>
        </a-form>
      </div>

      <div class="footer">
        <div class="links">
          <a href="_self">帮助</a>
          <a href="_self">隐私</a>
          <a href="_self">条款</a>
        </div>
        <div class="copyright">
          Copyright &copy; 2015~2019 benjobs
        </div>
      </div>
    </div>
    <vue-particles
      color="#dedede"
      :particleOpacity="0.7"
      :particlesNumber="80"
      shapeType="circle"
      :particleSize="4"
      linesColor="#dedede"
      :linesWidth="1"
      :lineLinked="true"
      :lineOpacity="0.4"
      :linesDistance="150"
      :moveSpeed="5"
      :hoverEffect="true"
      hoverMode="grab"
      :clickEffect="true"
      clickMode="push"
    >
    </vue-particles>
  </div>
</template>

<script type="application/ecmascript">
import { mapActions } from 'vuex'
import { mixinDevice } from '@/utils/mixin'
import { timeFix } from '@/utils/util'

export default {
  mixins: [mixinDevice],
  data () {
    return {
      loginBtn: false,
      form: this.$form.createForm(this),
      state: {
        time: 60,
        loginBtn: false
      }
    }
  },
  methods: {
    ...mapActions(['Login']),
    // handler
    handleRember () {

    },
    handleSubmit (e) {
      e.preventDefault()
      const {
        form: { validateFields },
        state,
        Login
      } = this
      state.loginBtn = true
      const validateFieldsKey = ['username', 'password']
      validateFields(validateFieldsKey, { force: true }, (err, values) => {
        if (!err) {
          const loginParams = { ...values }
          Login(loginParams)
            .then(resp => this.loginSuccess(resp))
            .catch(err => this.requestFailed(err))
            .finally(() => {
              state.loginBtn = false
            })
        } else {
          setTimeout(() => {
            state.loginBtn = false
          }, 600)
        }
      })
    },
    loginSuccess (resp) {
      this.$router.push({ path: '/flink/app' })
      // 延迟 1 秒显示欢迎信息
      setTimeout(() => {
        this.$notification.success({
          message: '欢迎',
          description: `${timeFix()}，欢迎回来`
        })
      }, 1000)
    },
    requestFailed (err) {
      console.log(err)
    }
  }
}
</script>

<style lang="less" scoped>
  .particles {
    position: absolute;
    z-index: 1;
    width: 100%;
  }

  .container-wrapper {
    height: 100%;

    &.mobile {
      .container {
        .main {
          max-width: 368px;
          width: 98%;
          z-index: 20;
        }
      }
    }

    #particles-js {
      background: #f0f2f5 url(~@/assets/1.jpg) no-repeat 50%;
      background-size: cover;
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
    }

    .container {
      position: relative;
      -webkit-align-items: center;
      -ms-flex-align: center;
      align-items: center;
      display: -webkit-flex;
      display: flex;
      height: 100%;
      justify-content: center;

      a {
        text-decoration: none;
      }

      .top {
        text-align: center;

        .header {
          height: 160px;
          line-height: 160px;

          .badge {
            position: absolute;
            display: inline-block;
            line-height: 1;
            vertical-align: middle;
            margin-left: -12px;
            margin-top: -10px;
            opacity: 0.8;
          }

          .logo {
            height: 160px;
            vertical-align: top;
            margin-right: 16px;
            border-style: none;
          }

          .title {
            font-size: 33px;
            color: rgba(0, 0, 0, .85);
            font-family: Avenir, 'Helvetica Neue', Arial, Helvetica, sans-serif;
            font-weight: 600;
            position: relative;
            top: 2px;
          }
        }

        .desc {
          font-size: 14px;
          color: rgba(255, 255, 255, .65);
          margin-top: 12px;
          margin-bottom: 20px;
        }
      }

      .main {
        min-width: 260px;
        width: 368px;
        margin: 0 auto;
        z-index: 20;
        background: rgba(0, 0, 0, .2);
        padding: 20px 30px;

      }

      .footer {
        position: absolute;
        width: 100%;
        bottom: 0;
        padding: 0 16px;
        margin: 48px 0 24px;
        text-align: center;

        .links {
          margin-bottom: 8px;
          font-size: 14px;

          a {
            color: rgba(0, 0, 0, 0.45);
            transition: all 0.3s;

            &:not(:last-child) {
              margin-right: 40px;
            }
          }
        }

        .copyright {
          color: rgba(0, 0, 0, 0.45);
          font-size: 14px;
        }
      }
    }

    button.login-button {
      padding: 0 15px;
      font-size: 16px;
      height: 40px;
      width: 100%;
    }

    .icon {
      color: rgba(255, 255, 255, .55);
      margin-top: 10px;
    }
  }
</style>

<style lang="less">
  .login-form {
    .ant-input {
      border: 1px solid rgba(255, 255, 255, .55);
      border-radius: 1px;
      color: rgba(255, 255, 255, .65);
      margin-top: 10px;
      background: unset;

      .ant-input-affix-wrapper:hover, .ant-input:not(.ant-input-disabled) {
        border-color: rgba(255, 255, 255, .95);
      }
    }

    .ant-btn-primary {
      color: #fff;
      background-color: rgba(255, 255, 255, 0.35);
      border: none;
      border-radius: 0px;
    }

    .ant-checkbox-wrapper {
      color: rgba(255, 255, 255, .75);
    }

    .ant-switch-checked {
      background-color: rgba(255, 255, 255, 0.35);
    }
  }
</style>

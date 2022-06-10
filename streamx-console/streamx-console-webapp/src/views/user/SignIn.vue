<template>
  <div :class="['container-wrapper', device]">
    <div class="container">
      <div class="main">
        <div class="top">
          <div class="header">
            <a href="/">
              <img
                src="~@/assets/imgs/logo.svg"
                class="logo"
                alt="logo">
            </a>
          </div>
          <div class="desc">
            StreamX ── Make stream processing easier!
          </div>
        </div>
        <a-form
          id="formLogin"
          class="signin-form"
          ref="formLogin"
          :form="form"
          @submit="handleSubmit">
          <a-form-item>
            <a-input
              size="large"
              type="text"
              placeholder="username / admin"
              v-decorator="[
                'username',
                {rules: [{ required: true, message: 'please enter username' }], validateTrigger: 'change'}
              ]">
              <a-icon
                slot="prefix"
                type="user"
                class="icon"/>
            </a-input>
          </a-form-item>

          <a-form-item>
            <a-input
              size="large"
              type="password"
              autocomplete="false"
              placeholder="password / streamx"
              v-decorator="[
                'password',
                {rules: [{ required: true, message: 'please enter password' }], validateTrigger: 'blur'}
              ]">
              <a-icon
                slot="prefix"
                type="lock"
                class="icon"/>
            </a-input>
          </a-form-item>

          <a-form-item
            style="margin-top:40px">
            <a-button
              size="large"
              type="primary"
              html-type="submit"
              class="signin-button"
              :loading="state.loginBtn"
              :disabled="state.loginBtn">
              Sign in
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
        <div
          class="copyright">
          Copyright &copy; 2015~{{ year }} benjobs
        </div>
      </div>
    </div>
    <vue-particles
      color="#dedede"
      :particle-opacity="0.7"
      :particles-number="80"
      shape-type="circle"
      :particle-size="4"
      lines-color="#dedede"
      :lines-width="1"
      :line-linked="true"
      :line-opacity="0.4"
      :lines-distance="150"
      :move-speed="5"
      :hover-effect="true"
      hover-mode="grab"
      :click-effect="true"
      click-mode="push"/>
  </div>
</template>

<script type="application/ecmascript">
import {mapActions} from 'vuex'
import {mixinDevice} from '@/utils/mixin'

export default {
  mixins: [mixinDevice],
  data() {
    return {
      loginBtn: false,
      form: this.$form.createForm(this),
      state: {
        time: 60,
        loginBtn: false
      },
      year: new Date().getFullYear()
    }
  },

  mounted() {
    const index = this.randomNum(1, 4)
    $('.main').css({
      'background': index > 3 ? 'rgba(100, 150, 255, .3)' : 'rgba(0, 0, 0, .4)'
    })
    const bgUrl = require('@assets/bg/' + index + '.png')
    $('#particles-js').css('background-image', 'url(' + bgUrl + ')')
  },

  methods: {
    ...mapActions(['SignIn']),
    handleSubmit(e) {
      e.preventDefault()
      const {
        form: { validateFields },
        state,
        SignIn
      } = this
      state.loginBtn = true
      const validateFieldsKey = ['username', 'password']
      validateFields(validateFieldsKey, {force: true}, (err, values) => {
        if (!err) {
          const loginParams = {...values}
          SignIn(loginParams)
            .then(resp => {
              if (resp.code != null) {
                const message = 'SignIn failed,' + (resp.code === 0 ? ' authentication error' : ' current User is locked.')
                this.$message.error(message)
              } else {
                this.$router.push({path: '/flink/app'})
              }
            })
            .catch(err => console.log(err))
            .finally(() => {
              state.loginBtn = false
            })
        } else {
          setTimeout(() => {
            state.loginBtn = false
          }, 600)
        }
      })
    }
  }
}
</script>
<style lang="less">
@import "SignIn";
</style>

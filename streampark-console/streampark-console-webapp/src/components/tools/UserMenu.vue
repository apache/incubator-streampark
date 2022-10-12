<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->

<template>
  <div
    class="user-wrapper">

    <div class="slogan">
      <span class="slogan-streampark">Apache StreamPark</span>,
      <span class="slogan-action">
        <span class="slogan-make">Make</span>&nbsp;
        <span class="slogan-target">stream processing</span>&nbsp;
        <span class="slogan-result">easier!</span>
      </span>
    </div>

    <div class="content-box header-icon">
      <a> Version: 1.2.4 </a>
      <a-divider type="vertical" />
      <a title="theme">
        <svg-icon name="theme" size="small" class="icon" @click.native="handleChangeTheme(false)"></svg-icon>
      </a>

      <a
        href="https://streampark.apache.org/docs/user-guide/quick-start"
        title="How to use"
        target="_blank">
        <svg-icon name="question" size="small" class="icon"></svg-icon>
      </a>

      <a
        href="https://github.com/apache/streampark"
        title="GitHub"
        target="_blank">
        <svg-icon name="github" size="small" class="icon"></svg-icon>
      </a>

      <a
        href="https://github.com/apache/streampark"
        title="GitHub"
        target="_blank">
        <img src="https://img.shields.io/github/stars/streamxhub/streampark.svg?sanitize=true" class="shields">
      </a>

      <a
        href="https://github.com/apache/streampark"
        title="GitHub"
        target="_blank">
        <img src="https://img.shields.io/github/forks/streamxhub/streampark.svg?sanitize=true" class="shields">
      </a>

      <a> Team : </a>

      <a-select
        v-model="teamId"
        mode="single"
        style="min-width: 100px; margin-right: 10px"
        @change="handleChangeTeam"
        class="team-select">
        <a-select-option
          v-for="team in teamList"
          :key="team.id"
          :value="team.id">
          {{ team.teamName }}
        </a-select-option>
      </a-select>

      <a-dropdown>
        <a class="ant-dropdown-link username" @click="e => e.preventDefault()">
          {{ userName }} <a-icon type="caret-down" />
        </a>
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

      <notice class="action"/>

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
        :form="form">
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
import Notice from '@/components/Notice'
import SvgIcon from '@/components/SvgIcon'

import { mapState, mapActions } from 'vuex'
import { password } from '@api/user'
import {teams} from '@/api/member'
import themeUtil from '@/utils/themeUtil'
import storage from '@/utils/storage'
import {TEAM_ID, USER_INFO, USER_NAME} from '@/store/mutation-types'
import {message} from 'ant-design-vue'

export default {
  name: 'UserMenu',
  components: {
    Notice,
    SvgIcon
  },

  data() {
    return {
      passwordVisible: false,
      form: null,
      confirmDirty: false,
      themeDark: false,
      teamList: [],
      teamId: null
    }
  },

  computed: {
    ...mapState({
      myTheme: state => state.app.theme
    }),
    userName() {
      return storage.get(USER_NAME)
    }
  },

  beforeMount() {
    this.form = this.$form.createForm(this)
  },

  mounted() {
    this.handleChangeTheme(true)
    this.handlePrepareTeam()
    this.fetchTeams()
  },

  methods: {
    ...mapActions(['SignOut','ChangeTheme', 'SetTeam']),
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
      this.form.validateFields((err, values) => {
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
          this.form.resetFields()
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
        const closeMessage = this.$message.loading(`You have selected the theme ${_theme}, switching...`)
        themeUtil.changeThemeColor(null, _theme).then(closeMessage)
      }
    },

    handleConfirmBlur(e) {
      const value = e.target.value
      this.confirmDirty = this.confirmDirty || !!value
    },

    compareToFirstPassword(rule, value, callback) {
      if (value && value !== this.form.getFieldValue('password')) {
        callback('Two passwords that you enter is inconsistent!')
      } else {
        callback()
      }
    },

    validateToNextPassword(rule, value, callback) {
      if (value && this.confirmDirty) {
        this.form.validateFields(['confirm'], { force: true })
      }
      callback()
    },

    handlePrepareTeam() {
      let id = sessionStorage.getItem(TEAM_ID)
      if (id == null) {
        id = storage.get(TEAM_ID)
        sessionStorage.setItem(TEAM_ID, id)
      }
      this.teamId = id.toString()
    },

    handleChangeTeam(teamId) {
      this.SetTeam({
        teamId: teamId
      }).then(() => {
        //refresh...
        this.handleRefreshPage()
      }).catch((err) => message.error(err))
    },

    handleRefreshPage() {
      const defaultPage = '/flink/app'
      const pages = [
        '/system/user',
        '/system/role',
        '/system/menu',
        '/system/token',
        '/system/team',
        '/system/member',
        '/flink/project',
        '/flink/app'
      ]
      const skipPages = [
        '/flink/notebook/view',
        '/flink/setting'
      ]
      const currPath = location.href.replace(/(.*)#/,'')
      if (!skipPages.includes(currPath)) {
        if (pages.includes(currPath)) {
          window.location.reload()
        } else {
          this.$router.push({path: defaultPage})
        }
      }
    },

    fetchTeams() {
      teams({
        userId: storage.get(USER_INFO).userId
      }).then((r) => {
        this.teamList = r.data
      })
    },
  },
  watch: {
    visible() {
    }
  }
}
</script>
<style lang="less">
.header-icon {
  float: right;
  .icon {
    margin-left: 3px;
    margin-right: 3px;
    vertical-align: -0.175em;
    & > svg {
      & > path {
        fill: @text-color;
      }
    }
  }
}
.username {
  font-size: 15px;
  font-weight: 500;
  color: @text-color;
}
.shields {
  padding-left: 5px;
  padding-right: 5px;
}
.slogan {
  float: left;
  font-size: 1.5rem;
  font-weight: bolder;
  line-height: 60px;
  font-family: -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"Helvetica Neue",Arial,"Noto Sans",sans-serif,"Apple Color Emoji","Segoe UI Emoji","Segoe UI Symbol","Noto Color Emoji";
  .slogan-streampark,.slogan-action {
    background: linear-gradient(130deg, #24c6dc, #5433ff 41.07%, #f09 76.05%);
    -webkit-text-fill-color: transparent;
    -webkit-background-clip: text;
    -webkit-box-decoration-break: clone;
  }
}

.team-select {
  .ant-select-selection__rendered {
    position: relative;
    display: block;
    margin: 0px 6px;
    line-height: 22px;
  }
  .ant-select-selection--single {
    height: unset;
  }
  .ant-select-arrow {
    right: 5px;
    margin-top: -6px;
    font-size: 8px;
  }
}

</style>

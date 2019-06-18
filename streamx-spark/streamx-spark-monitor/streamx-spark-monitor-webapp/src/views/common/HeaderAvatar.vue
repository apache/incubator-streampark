<template>
  <div>
    <a-dropdown style="display: inline-block; height: 100%; vertical-align: initial">
      <span style="cursor: pointer">
        <a-avatar class="avatar" size="small" shape="circle"
                  :src="avatar"/>
        <span class="curr-user">{{user.username}}</span>
      </span>
      <a-menu style="width: 150px" slot="overlay">
        <a-menu-item @click="openProfile">
          <a-icon type="user"/>
          <span>个人中心</span>
        </a-menu-item>
        <a-menu-item @click="updatePassword">
          <a-icon type="key"/>
          <span>密码修改</span>
        </a-menu-item>
        <a-menu-divider></a-menu-divider>
        <a-menu-item @click="handleSettingClick">
          <a-icon type="setting"/>
          <span>系统定制</span>
        </a-menu-item>
        <a-menu-divider></a-menu-divider>
        <a-menu-item @click="logout">
          <a-icon type="logout"/>
          <span>退出登录</span>
        </a-menu-item>
      </a-menu>
    </a-dropdown>
    <update-password
      @success="handleUpdate"
      @cancel="handleCancelUpdate"
      :user="user"
      :updatePasswordModelVisible="updatePasswordModelVisible">
    </update-password>
  </div>
</template>

<script>
import { mapMutations, mapState } from 'vuex'
import UpdatePassword from '../personal/UpdatePassword'

export default {
  name: 'HeaderAvatar',
  components: {UpdatePassword},
  data () {
    return {
      updatePasswordModelVisible: false
    }
  },
  computed: {
    ...mapState({
      settingBar: state => state.setting.settingBar.opened,
      user: state => state.account.user
    }),
    avatar () {
      return `static/avatar/${this.user.avatar}`
    }
  },
  methods: {
    handleSettingClick () {
      this.setSettingBar(!this.settingBar)
    },
    openProfile () {
      this.$router.push('/profile')
    },
    updatePassword () {
      this.updatePasswordModelVisible = true
    },
    handleCancelUpdate () {
      this.updatePasswordModelVisible = false
    },
    handleUpdate () {
      this.updatePasswordModelVisible = false
      this.$message.success('更新密码成功，请重新登录系统')
      setTimeout(() => {
        this.logout()
      }, 1500)
    },
    logout () {
      this.$get(`logout/${this.user.id}`).then(() => {
        return new Promise((resolve, reject) => {
          this.$db.clear()
          location.reload()
        })
      }).catch(() => {
        this.$message.error('退出系统失败')
      })
    },
    ...mapMutations({setSettingBar: 'setting/setSettingBar'})
  }
}
</script>

<style lang="less" scoped>
  .ant-avatar-sm {
    width: 30px;
    height: 30px;
  }
  .avatar {
    margin: 20px 4px 20px 0;
    color: #1890ff;
    background: hsla(0, 0%, 100%, .85);
    vertical-align: middle;
  }
  .curr-user {
    font-weight: 600;
    margin-left: 6px
  }
</style>

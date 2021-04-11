<template>
  <div
    class="user-wrapper">
    <div
      class="content-box">
      <a
        href="http://www.streamxhub.com/docs/getting-started"
        target="_blank">
        <span
          class="action">
          <a-icon
            style="color:#1890ff"
            type="question-circle-o" />
        </span>
      </a>
      <notice-icon
        style="color:#1890ff"
        class="action" />
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
          <a-menu-item
            key="3">
            <a
              href="javascript:;"
              @click="handleLogout">
              <a-icon type="logout" />
              <span>Sign Out</span>
            </a>
          </a-menu-item>
        </a-menu>
      </a-dropdown>
    </div>
  </div>
</template>

<script>
import NoticeIcon from '@/components/NoticeIcon'
import { mapActions, mapGetters } from 'vuex'

export default {
  name: 'UserMenu',
  components: {
    NoticeIcon
  },
  methods: {
    ...mapActions(['SignOut']),
    ...mapGetters(['nickname', 'avatar']),
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
    }
  }
}
</script>

<template>
  <a-modal
    v-model="show"
    :centered="true"
    :keyboard="false"
    :footer="null"
    :width="450"
    @cancel="handleCancleClick">
    <template slot="title">
      <a-icon type="user" />
      User Info
    </template>
    <a-layout class="user-info">
      <a-layout-content class="user-content">
        <p>
          <a-icon
            type="user" />User Name：{{ data.username }}
        </p>
        <p
          :title="data.roleName">
          <a-icon
            type="star" />Role：{{ data.roleName? data.roleName: '' }}
        </p>
        <p>
          <a-icon
            type="skin" />Gender：{{ sex }}
        </p>
        <p>
          <a-icon
            type="mail" />E-Mail：{{ data.email ? data.email : '' }}
        </p>
      </a-layout-content>
      <a-layout-content
        class="user-content">
        <p>
          <a-icon
            type="smile"
            v-if="data.status === '1'" />
          <a-icon
            type="frown"
            v-else />Status：
          <template
            v-if="data.status === '0'">
            <a-tag
              color="red">
              locked
            </a-tag>
          </template>
          <template
            v-else-if="data.status === '1'">
            <a-tag
              color="cyan">
              effective
            </a-tag>
          </template>
          <template
            v-else>
            {{ data.status }}
          </template>
        </p>
        <p>
          <a-icon
            type="clock-circle" />Creation Time：{{ data.createTime }}
        </p>
        <p>
          <a-icon
            type="login" />Recent Login：{{ data.lastLoginTime }}
        </p>
        <p
          :title="data.description">
          <a-icon
            type="message" />Description：{{ data.description }}
        </p>
      </a-layout-content>
    </a-layout>
  </a-modal>
</template>
<script>
export default {
  name: 'UserInfo',
  props: {
    visible: {
      type: Boolean,
      require: true,
      default: false
    },
    data: {
      type: Object,
      default: () => ({}),
      require: true
    }
  },
  computed: {
    show: {
      get: function () {
        return this.visible
      },
      set: function () {
      }
    },
    sex () {
      switch (this.data.sex) {
        case '0':
          return 'male'
        case '1':
          return 'female'
        case '2':
          return 'secret'
        default:
          return this.data.sex
      }
    }
  },
  methods: {
    handleCancleClick () {
      this.$emit('close')
    }
  }
}
</script>
<style lang="less" scoped>
  .user-info {
    background: @body-background;
    padding: 10px;
  }
  .user-content{
    margin-right: 1.2rem;
    float: left;
  }
  p {
    margin-bottom: 1rem;
    max-width: 16rem;
  }
  i {
    margin-right: .8rem;
  }
</style>

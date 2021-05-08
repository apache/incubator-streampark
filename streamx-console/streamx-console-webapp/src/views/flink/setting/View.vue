<template>
  <div>
    <a-card
      :bordered="false"
      style="margin-top: 24px"
      class="system_setting"
      title="System Setting">
      <a-list>
        <a-list-item v-for="(item,index) in settings" :key="index">
          <a-list-item-meta style="width: 50%">
            <svg-icon class="avatar" name="workspace" size="large" slot="avatar" v-if="item.key.match(/(.*)workspace(.*)/g)"></svg-icon>
            <svg-icon class="avatar" name="maven" size="large" slot="avatar" v-if="item.key.match(/(.*)maven(.*)/g)"></svg-icon>
            <svg-icon class="avatar" name="http" size="large" slot="avatar" v-if="item.key.match(/(.*)webapp.address(.*)/g)"></svg-icon>
            <svg-icon class="avatar" name="flink" size="large" slot="avatar" v-if="item.key.match(/env.flink(.*)/g)"></svg-icon>
            <svg-icon class="avatar" name="host" size="large" slot="avatar" v-if="item.key==='alert.email.host'"></svg-icon>
            <svg-icon class="avatar" name="port" size="large" slot="avatar" v-if="item.key==='alert.email.port'"></svg-icon>
            <svg-icon class="avatar" name="mail" size="large" slot="avatar" v-if="item.key==='alert.email.address'"></svg-icon>
            <svg-icon class="avatar" name="keys" size="large" slot="avatar" v-if="item.key==='alert.email.password'"></svg-icon>
            <svg-icon class="avatar" name="ssl" size="large" slot="avatar" v-if="item.key==='alert.email.ssl'"></svg-icon>
            <span slot="title">
              {{ item.title }}
            </span>
            <span slot="description">
              {{ item.description }}
            </span>
          </a-list-item-meta>
          <div class="list-content" style="width: 50%">
            <div class="list-content-item" style="width: 100%">
              <template v-if="item.type === 1">
                <input
                  v-if="item.editable"
                  :value="item.value"
                  :class="item.key.replace(/\./g,'_')"
                  class="ant-input"/>
                <div v-else style="width: 100%;text-align: right">
                  {{ item.value }}
                </div>
              </template>
              <template v-else>
                <a-switch :defaultChecked="item.value === 'true'" @change="handleSwitch(item)" />
              </template>
            </div>
          </div>
          <div slot="actions" v-if="item.type === 1">
            <a v-if="!item.submitting" @click="handleEdit(item)">Edit</a>
            <a v-else @click="handleSubmit(item)">Submit</a>
          </div>
        </a-list-item>
      </a-list>
    </a-card>
  </div>
</template>

<script>
import {all, get, update} from '@api/setting'
import SvgIcon from '@/components/SvgIcon'

export default {
  name: 'Setting',
  components: { SvgIcon },
  data() {
    return {
      settings: []
    }
  },

  mounted() {
    this.form = this.$form.createForm(this)
    this.handleAll()
  },

  methods: {

    handleAll() {
      all({}).then((resp) => {
        this.settings = resp.data
      })
    },

    handleEdit(setting) {
      if (!setting.editable) {
        setting.submitting = true
      }
      setting.editable = !setting.editable
    },
    handleSubmit(setting) {
      setting.submitting = false
      setting.editable = false
      const className = setting.key.replace(/\./g, '_')
      const elem = document.querySelector('.' + className)
      const value = elem.value
      update({
        key: setting.key,
        value: value
      }).then((resp) => {
        this.handleAll()
      })
    },

    handleSwitch(setting) {
      update({
        key: setting.key,
        value: !setting.value
      }).then((resp) => {
        this.handleAll()
      })
    }
  }

}
</script>

<style lang="less">
@import "View";
</style>

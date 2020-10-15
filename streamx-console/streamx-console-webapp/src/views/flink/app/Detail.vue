<template>
  <a-card :bordered="false" style="margin-top: 20px;">
    <div>
      <a-descriptions title="Application Info" bordered size="middle" layout="vertical">
        <a-descriptions-item label="Application Name">
          {{ app.jobName }}
        </a-descriptions-item>
        <a-descriptions-item label="Module">
          {{ app.module }}
        </a-descriptions-item>
        <a-descriptions-item label="Project">
          {{ app.projectName }}
        </a-descriptions-item>
        <a-descriptions-item label="Application Type">
          <span v-if="app.appType == 1">
            <a-tag color="cyan">
              StreamX Flink
            </a-tag>
          </span>
          <span v-else-if="app.appType == 2">
            <a-tag color="blue">
              Apache Flink
            </a-tag>
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="Status">
          <State :state="app.state"></State>
        </a-descriptions-item>
        <a-descriptions-item label="Start Time">
          <template v-if="app.startTime">
            <a-icon type="clock-circle" /> {{ app.startTime }}
          </template>
        </a-descriptions-item>
        <a-descriptions-item v-if="app.endTime" label="End Time">
          <a-icon type="clock-circle" /> {{ app.endTime }}
        </a-descriptions-item>
        <a-descriptions-item v-if="app.duration" label="Duration">
          {{ app.duration | duration }}
        </a-descriptions-item>
        <a-descriptions-item label="Description" :span="3">
          {{ app.description }}
        </a-descriptions-item>
      </a-descriptions>
    </div>
    <div class="desc-item">
      <a-descriptions title="Options Info" bordered size="middle" layout="vertical">
        <a-descriptions-item v-for="(v,k) in options" :key="k" :label="k">
          {{ v }}
        </a-descriptions-item>
      </a-descriptions>
    </div>
  </a-card>
</template>
<script>
import { mapActions, mapGetters } from 'vuex'
import { get } from '@api/application'
import State from './State'
import configOptions from './option'

export default {
  components: { State },
  data () {
    return {
      app: null,
      options: {}
    }
  },
  mounted () {
    const appId = this.applicationId()
    if (appId) {
      this.CleanAppId()
      this.handleGet(appId)
      const timer = window.setInterval(() => this.handleGet(appId), 2000)
      this.$once('hook:beforeDestroy', () => {
        clearInterval(timer)
      })
    } else {
      this.$router.back(-1)
    }
  },
  methods: {
    ...mapActions(['CleanAppId']),
    ...mapGetters(['applicationId']),
    handleGet (appId) {
      get({ id: appId }).then((resp) => {
        this.app = resp.data
        this.options = JSON.parse(this.app.options)
      }).catch((error) => {
        this.$message.error(error.message)
      })
    }
  }
}
</script>

<style scoped>
.desc-item {
  padding-top: 20px;
}
</style>

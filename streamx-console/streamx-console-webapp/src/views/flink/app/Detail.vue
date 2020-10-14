<template>
  <a-card :bordered="false" style="margin-top: 20px;">
    <div class="desc-item">
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
          <div class="app_state">
            <a-tag color="#2f54eb" v-if="app.state === 0">CREATED</a-tag>
            <a-tag color="#1ABBDC" v-if="app.state === 1" class="status-processing-deploying">DEPLOYING</a-tag>
            <a-tag color="#108ee9" v-if="app.state === 2">DEPLOYED</a-tag>
            <a-tag color="#1AB58E" v-if="app.state === 3" class="status-processing-starting">STARTING</a-tag>
            <a-tag color="#13c2c2" v-if="app.state === 4" class="status-processing-restarting">RESTARTING</a-tag>
            <a-tag color="#52c41a" v-if="app.state === 5" class="status-processing-running">RUNNING</a-tag>
            <a-tag color="#fa541c" v-if="app.state === 6" class="status-processing-failing">FAILING</a-tag>
            <a-tag color="#f5222d" v-if="app.state === 7">FAILED</a-tag>
            <a-tag color="#faad14" v-if="app.state === 8" class="status-processing-cancelling">CANCELLING</a-tag>
            <a-tag color="#fa8c16" v-if="app.state === 9">CANCELED</a-tag>
            <a-tag color="#1890ff" v-if="app.state === 10">FINISHED</a-tag>
            <a-tag color="#722ed1" v-if="app.state === 11">SUSPENDED</a-tag>
            <a-tag color="#eb2f96" v-if="app.state === 12" class="status-processing-reconciling">RECONCILING</a-tag>
            <a-tag color="#000000" v-if="app.state === 13">LOST</a-tag>
            <a-tag color="#13c2c2" v-if="app.state === 14" class="status-processing-restarting">MAPPING</a-tag>
          </div>
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
      <a-descriptions title="Config Info" bordered size="middle" layout="vertical">
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
import { jars } from '@api/project'
export default {
  data () {
    return {
      app: null,
      jars: [],
      options: {}
    }
  },
  mounted () {
    const appId = this.applicationId()
    if (appId) {
      this.handleGet(appId)
      this.CleanAppId()
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
        jars({
          id: this.app.projectId,
          module: this.app.module
        }).then((resp) => {
          this.jars = resp.data
        }).catch((error) => {
          this.$message.error(error.message)
        })
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

.app_state {
  width: 80px;
}

.app_state > .ant-tag {
  border-radius: 0;
  font-weight: 700;
  font-size: 13px;
  text-align: center;
  padding: 0 4px;
  cursor: default;
}

.status-processing-deploying {
  animation: deploying 800ms ease-out infinite alternate;
}

.status-processing-starting {
  animation: starting 800ms ease-out infinite alternate;
}

.status-processing-restarting {
  animation: restarting 800ms ease-out infinite alternate;
}

.status-processing-running {
  animation: running 800ms ease-out infinite alternate;
}

.status-processing-failing {
  animation: failing 800ms ease-out infinite alternate;
}

.status-processing-cancelling {
  animation: cancelling 800ms ease-out infinite alternate;
}

.status-processing-reconciling {
  animation: reconciling 800ms ease-out infinite alternate;
}

@keyframes deploying {
  0% {
    border-color: #1ABBDC;
    box-shadow: 0 0 1px #1ABBDC, inset 0 0 2px #1ABBDC;
  }
  100% {
    border-color: #1ABBDC;
    box-shadow: 0 0 10px #1ABBDC, inset 0 0 5px #1ABBDC;
  }
}

@keyframes starting {
  0% {
    border-color: #1AB58E;
    box-shadow: 0 0 1px #1AB58E, inset 0 0 2px #1AB58E;
  }
  100% {
    border-color: #1AB58E;
    box-shadow: 0 0 10px #1AB58E, inset 0 0 5px #1AB58E;
  }
}

@keyframes restarting {
  0% {
    border-color: #13c2c2;
    box-shadow: 0 0 1px #13c2c2, inset 0 0 2px #13c2c2;
  }
  100% {
    border-color: #13c2c2;
    box-shadow: 0 0 10px #13c2c2, inset 0 0 5px #13c2c2;
  }
}

@keyframes running {
  0% {
    border-color: #52c41a;
    box-shadow: 0 0 1px #52c41a, inset 0 0 2px #52c41a;
  }
  100% {
    border-color: #52c41a;
    box-shadow: 0 0 10px #52c41a, inset 0 0 5px #52c41a;
  }
}

@keyframes failing {
  0% {
    border-color: #fa541c;
    box-shadow: 0 0 1px #fa541c, inset 0 0 2px #fa541c;
  }
  100% {
    border-color: #fa541c;
    box-shadow: 0 0 10px #fa541c, inset 0 0 5px #fa541c;
  }
}

@keyframes cancelling {
  0% {
    border-color: #faad14;
    box-shadow: 0 0 1px #faad14, inset 0 0 2px #faad14;
  }
  100% {
    border-color: #faad14;
    box-shadow: 0 0 10px #faad14, inset 0 0 5px #faad14;
  }
}

@keyframes reconciling {
  0% {
    border-color: #eb2f96;
    box-shadow: 0 0 1px #eb2f96, inset 0 0 2px #eb2f96;
  }
  100% {
    border-color: #eb2f96;
    box-shadow: 0 0 10px #eb2f96, inset 0 0 5px #eb2f96;
  }
}
</style>

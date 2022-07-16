<template>
  <span v-if="option === 'state'">
    <div
      v-if="data['optionState'] === 0"
      class="app_state">
      <a-tag
        color="#2f54eb"
        v-if="data['state'] === 0">ADDED</a-tag>
      <a-tag
        color="#738df8"
        v-if="data['state'] === 1"
        class="status-processing-initializing">INITIALIZING</a-tag>
      <a-tag
        color="#2f54eb"
        v-if="data['state'] === 2">CREATED</a-tag>
      <a-tag
        color="#1AB58E"
        v-if="data['state'] === 3"
        class="status-processing-starting">STARTING</a-tag>
      <a-tag
        color="#13c2c2"
        v-if="data['state'] === 4"
        class="status-processing-restarting">RESTARTING</a-tag>
      <a-tag
        color="#52c41a"
        v-if="data['state'] === 5"
        class="status-processing-running">RUNNING</a-tag>
      <a-tag
        color="#fa541c"
        v-if="data['state'] === 6"
        class="status-processing-failing">FAILING</a-tag>
      <a-tag
        color="#f5222d"
        v-if="data['state'] === 7">FAILED</a-tag>
      <a-tag
        color="#faad14"
        v-if="data['state'] === 8"
        class="status-processing-cancelling">CANCELLING</a-tag>
      <a-tag
        color="#fa8c16"
        v-if="data['state'] === 9">CANCELED</a-tag>
      <a-tag
        color="#1890ff"
        v-if="data['state'] === 10 || data['state'] === 19">FINISHED</a-tag>
      <a-tag
        color="#722ed1"
        v-if="data['state'] === 11">SUSPENDED</a-tag>
      <a-tag
        color="#eb2f96"
        v-if="data['state'] === 12"
        class="status-processing-reconciling">RECONCILING</a-tag>
      <a-tag
        color="#000000"
        v-if="data['state'] === 13">LOST</a-tag>
      <a-tag
        color="#13c2c2"
        v-if="data['state'] === 14"
        class="status-processing-restarting">MAPPING</a-tag>
      <a-tag
        color="#738df8"
        v-if="data['state'] === 17"
        class="status-processing-initializing">SILENT</a-tag>
      <a-tag
        color="#8E50FF"
        v-if="data['state'] === 18">TERMINATED</a-tag>
    </div>
    <div v-else class="app_state">
      <a-tag
        v-if="data['optionState'] === 1"
        color="#1ABBDC"
        class="status-processing-deploying">LAUNCHING</a-tag>
      <a-tag
        v-if="data['optionState'] === 2"
        color="#faad14"
        class="status-processing-cancelling">CANCELLING</a-tag>
      <a-tag
        v-if="data['optionState'] === 3 "
        color="#1AB58E"
        class="status-processing-starting">STARTING</a-tag>
      <a-tag
        v-if="data['optionState'] === 4"
        color="#faad14"
        class="status-processing-cancelling">SAVEPOINT</a-tag>
    </div>
  </span>
  <span v-else-if="option === 'launch'" class="app_state">
    <a-tag
      v-if="data.launch === -1"
      color="#f5222d">FAILED</a-tag>
    <a-tag
      v-if="data.launch === 0"
      color="#52c41a">DONE</a-tag>
    <a-tag
      v-if="data.launch === 1 || data.launch === 4"
      color="#fa8c16">WAITING</a-tag>
    <a-tag
      v-if="data.launch === 2"
      class="status-processing-deploying"
      color="#52c41a">LAUNCHING</a-tag>
    <a-tag
      v-if="data.launch === 3"
      color="#fa8c16">PENDING</a-tag>
  </span>
  <span v-else-if="option === 'build'" class="app_state">
    <a-tag
      v-if="data['buildStatus'] === 0"
      color="#99A3A4">UNKNOWN</a-tag>
    <a-tag
      v-if="data['buildStatus'] === 1"
      color="#F5B041">PENDING</a-tag>
    <a-tag
      v-if="data['buildStatus'] === 2"
      color="#3498DB"
      class="status-processing-deploying">BUILDING</a-tag>
    <a-tag
      v-if="data['buildStatus'] === 3"
      color="#2ECC71">SUCCESS</a-tag>
    <a-tag
      v-if="data['buildStatus'] === 4"
      color="#E74C3C">FAILURE</a-tag>
  </span>
  <span v-else>
    <div
      class="task-tag"
      v-if="data['state'] === 4 || data['state'] === 5 || data['state'] === 7 || data['optionState'] === 4">
      <a-tooltip
        v-if="data['totalTask']">
        <template slot="title">
          TOTAL
        </template>
        <a-tag color="#102541">{{ data['totalTask'] }}</a-tag>
      </a-tooltip>
      <a-tooltip v-if="data.overview && data.overview['running']">
        <template slot="title">
          RUNNING
        </template>
        <a-tag color="#52c41a">{{ data.overview['running'] }}</a-tag>
      </a-tooltip>
      <a-tooltip v-if="data.overview && data.overview['canceled']">
        <template slot="title">
          CANCELED
        </template>
        <a-tag color="#fa8c16">{{ data.overview['canceled'] }}</a-tag>
      </a-tooltip>
      <a-tooltip v-if="data.overview && data.overview['canceling']">
        <template slot="title">
          CANCELING
        </template>
        <a-tag color="#faad14">{{ data.overview['canceling'] }}</a-tag>
      </a-tooltip>
      <a-tooltip v-if="data.overview && data.overview['created']">
        <template slot="title">
          CREATED
        </template>
        <a-tag color="#2f54eb">{{ data.overview['created'] }}</a-tag>
      </a-tooltip>
      <a-tooltip v-if="data.overview && data.overview['deploying']">
        <template slot="title">
          LAUNCHING
        </template>
        <a-tag color="#13c2c2">{{ data.overview['deploying'] }}</a-tag>
      </a-tooltip>
      <a-tooltip v-if="data.overview && data.overview['failed']">
        <template slot="title">
          FAILED
        </template>
        <a-tag color="#f5222d">{{ data.overview['failed'] }}</a-tag>
      </a-tooltip>
      <a-tooltip v-if="data.overview && data.overview['finished']">
        <template slot="title">
          FINISHED
        </template>
        <a-tag color="#1890ff">{{ data.overview['finished'] }}</a-tag>
      </a-tooltip>
      <a-tooltip v-if="data.overview && data.overview['reconciling']">
        <template slot="title">
          RECONCILING
        </template>
        <a-tag color="#eb2f96">{{ data.overview['reconciling'] }}</a-tag>
      </a-tooltip>
      <a-tooltip v-if="data.overview && data.overview['scheduled']">
        <template slot="title">
          SCHEDULED
        </template>
        <a-tag color="#722ed1">{{ data.overview['scheduled'] }}</a-tag>
      </a-tooltip>
    </div>
    <div v-else>-</div>
  </span>
</template>
<script>
export default {
  name: 'State',
  props: {
    option: {
      type: String,
      default: 'state'
    },
    data: {
      type: Object,
      default: null
    }
  }
}
</script>

<style lang='less' scoped>
.app_state, .task-tag {
  width: 80px;
}

.status-processing-deploying {
  animation: deploying-color 800ms ease-out infinite alternate;
}

.status-processing-initializing {
  animation: initializing-color 800ms ease-out infinite alternate;
}

.status-processing-starting {
  animation: starting-color 800ms ease-out infinite alternate;
}

.status-processing-restarting {
  animation: restarting-color 800ms ease-out infinite alternate;
}

.status-processing-running {
  animation: running-color 800ms ease-out infinite alternate;
}

.status-processing-failing {
  animation: failing-color 800ms ease-out infinite alternate;
}

.status-processing-cancelling {
  animation: cancelling-color 800ms ease-out infinite alternate;
}

.status-processing-reconciling {
  animation: reconciling-color 800ms ease-out infinite alternate;
}


@keyframes deploying-color {
  0% {
    border-color: #1ABBDC;
    box-shadow: 0 0 1px #1ABBDC, inset 0 0 2px #1ABBDC;
  }
  100% {
    border-color: #1ABBDC;
    box-shadow: 0 0 10px #1ABBDC, inset 0 0 5px #1ABBDC;
  }
}

@keyframes initializing-color {
  0% {
    border-color: #738df8;
    box-shadow: 0 0 1px #738df8, inset 0 0 2px #738df8;
  }
  100% {
    border-color: #738df8;
    box-shadow: 0 0 10px #738df8, inset 0 0 5px #738df8;
  }
}

@keyframes starting-color {
  0% {
    border-color: #1AB58E;
    box-shadow: 0 0 1px #1AB58E, inset 0 0 2px #1AB58E;
  }
  100% {
    border-color: #1AB58E;
    box-shadow: 0 0 10px #1AB58E, inset 0 0 5px #1AB58E;
  }
}

@keyframes restarting-color {
  0% {
    border-color: #13c2c2;
    box-shadow: 0 0 1px #13c2c2, inset 0 0 2px #13c2c2;
  }
  100% {
    border-color: #13c2c2;
    box-shadow: 0 0 10px #13c2c2, inset 0 0 5px #13c2c2;
  }
}

@keyframes running-color {
  0% {
    border-color: #52c41a;
    box-shadow: 0 0 1px #52c41a, inset 0 0 2px #52c41a;
  }
  100% {
    border-color: #52c41a;
    box-shadow: 0 0 10px #52c41a, inset 0 0 5px #52c41a;
  }
}

@keyframes failing-color {
  0% {
    border-color: #fa541c;
    box-shadow: 0 0 1px #fa541c, inset 0 0 2px #fa541c;
  }
  100% {
    border-color: #fa541c;
    box-shadow: 0 0 10px #fa541c, inset 0 0 5px #fa541c;
  }
}

@keyframes cancelling-color {
  0% {
    border-color: #faad14;
    box-shadow: 0 0 1px #faad14, inset 0 0 2px #faad14;
  }
  100% {
    border-color: #faad14;
    box-shadow: 0 0 10px #faad14, inset 0 0 5px #faad14;
  }
}

@keyframes reconciling-color {
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

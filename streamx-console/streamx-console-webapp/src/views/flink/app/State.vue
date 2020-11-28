<template>
  <!--
   CREATED(0),
  DEPLOYING(1),
  DEPLOYED(2),
  STARTING(3),
  RESTARTING(4),
  RUNNING(5),
  FAILING(6),
  FAILED(7),
  CANCELLING(8),
  CANCELED(9),
  FINISHED(10),
  SUSPENDED(11),
  RECONCILING(12),
  LOST(13);

 TOTAL: '#112641',
 RUNNING: '#52c41a',
 FAILED: '#f5222d',
 FINISHED: '#1890ff',
 CANCELED: '#fa8c16',
 CANCELING: '#faad14',
 CREATED: '#2f54eb',
 DEPLOYING: '#13c2c2',
 RECONCILING: '#eb2f96',
 IN_PROGRESS: '#faad14',
 SCHEDULED: '#722ed1',
 COMPLETED: '#1890ff',
 RESTARTING: '#13c2c2'
 SAVEPOINT: #FAAD14
  -->
  <div class="app_state" v-if="option === 0">
    <a-tag color="#2f54eb" v-if="state === 0">CREATED</a-tag>
    <a-tag color="#1ABBDC" v-if="state === 1" class="status-processing-deploying">DEPLOYING</a-tag>
    <a-tag color="#108ee9" v-if="state === 2">DEPLOYED</a-tag>
    <a-tag color="#1AB58E" v-if="state === 3" class="status-processing-starting">STARTING</a-tag>
    <a-tag color="#13c2c2" v-if="state === 4" class="status-processing-restarting">RESTARTING</a-tag>
    <a-tag color="#52c41a" v-if="state === 5" class="status-processing-running">RUNNING</a-tag>
    <a-tag color="#fa541c" v-if="state === 6" class="status-processing-failing">FAILING</a-tag>
    <a-tag color="#f5222d" v-if="state === 7">FAILED</a-tag>
    <a-tag color="#faad14" v-if="state === 8" class="status-processing-cancelling">CANCELLING</a-tag>
    <a-tag color="#fa8c16" v-if="state === 9">CANCELED</a-tag>
    <a-tag color="#1890ff" v-if="state === 10">FINISHED</a-tag>
    <a-tag color="#722ed1" v-if="state === 11">SUSPENDED</a-tag>
    <a-tag color="#eb2f96" v-if="state === 12" class="status-processing-reconciling">RECONCILING</a-tag>
    <a-tag color="#000000" v-if="state === 13">LOST</a-tag>
    <a-tag color="#13c2c2" v-if="state === 14" class="status-processing-restarting">MAPPING</a-tag>
  </div>
  <div class="app_state" v-else>
    <a-tag color="#1ABBDC" v-if="state === 1 || option === 1" class="status-processing-deploying">DEPLOYING</a-tag>
    <a-tag color="#faad14" v-if="state === 8 || option === 2" class="status-processing-cancelling">CANCELLING</a-tag>
    <a-tag color="#faad14" v-if="state === 8 || option === 4" class="status-processing-savepointing">SAVEPOINTING</a-tag>
    <a-tag color="#1AB58E" v-if="state === 3 || option === 3 " class="status-processing-starting">STARTING</a-tag>
  </div>
</template>
<script>
export default {
  name: 'State',
  props: {
    state: {
      type: Number,
      default: null
    },
    option: {
      type: Number,
      default: null
    }
  }
}
</script>

<style scoped>

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

.status-processing-savepointing {
  animation: cancelling 800ms ease-out infinite alternate;
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

@keyframes savepointing {
  0% {
    border-color: #faad14;
    box-shadow: 0 0 1px #faad14, inset 0 0 2px #faad14;
  }
  100% {
    border-color: #faad14;
    box-shadow: 0 0 10px #faad14, inset 0 0 5px #faad14;
  }
}
</style>

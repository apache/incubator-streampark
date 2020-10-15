<template>
  <a-card :bordered="false" style="margin-top: 20px;">
    <div v-if="app">
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
        <a-descriptions-item v-for="(v,k) in options" :key="k">
          <template slot="label">
            {{ k | optionKey }} <span style="color: darkgrey">({{ k }})</span>
          </template>
          {{ v }}
        </a-descriptions-item>
      </a-descriptions>
    </div>

    <div class="desc-item" v-if="app && app.appType == 1">
      <a-descriptions title="Conf History">
        <a-descriptions-item class="desc-item">
          <a-table
            ref="TableInfo"
            :columns="columns"
            size="middle"
            rowKey="id"
            style="margin-top: -24px"
            :dataSource="configVersions"
            :pagination='pagination'
            class="desc-table">
            <template slot="format" slot-scope="text, record">
              <a-tag color="#2db7f5" v-if="record.format == 1">
                yaml
              </a-tag>
              <a-tag color="#108ee9" v-if="record.format == 2">
                properties
              </a-tag>
            </template>
            <template  slot="version" slot-scope="text, record">
              <a-button type="primary" shape="circle" size="small" style="margin-right: 10px;">
                {{ record.version }}
              </a-button>
            </template>
            <template  slot="actived" slot-scope="text, record">
              <a-tag color="green" v-if="record.actived">current</a-tag>
            </template>
            <template slot="operation"  slot-scope="text, record">
              <a-icon
                type="eye"
                theme="twoTone"
                twoToneColor="#4a9ff5"
                @click="handleConfDetail(record)"
                title="查看">
              </a-icon>
              <a-icon
                v-if="configVersions.length>1"
                type="branches"
                style="color: #4a9ff5"
                @click="handleDetail(record)"
                title="比较">
              </a-icon>
            </template>
          </a-table>
        </a-descriptions-item>
      </a-descriptions>
    </div>

    <div v-if="app && savePoints" :class="{'desc-item': app && app.appType == 2}">
      <a-descriptions title="SavePoints">
        <a-descriptions-item class="desc-item">
          <a-table
            ref="TableInfo"
            :columns="savePointsColumns"
            size="middle"
            rowKey="id"
            style="margin-top: -24px"
            :dataSource="savePoints"
            :pagination='pagination'
            class="desc-table">
            <template slot="createTime" slot-scope="text, record">
              <a-icon type="clock-circle"/>{{record.createTime}}
            </template>
            <template  slot="lastest" slot-scope="text, record">
              <a-tag color="green" v-if="record.lastest">lastest</a-tag>
            </template>
          </a-table>
        </a-descriptions-item>
      </a-descriptions>
    </div>

    <conf ref="confEdit" @close="handleEditConfClose" @ok="handleEditConfOk" :visiable="confVisiable" :readOnly="true"></Conf>

  </a-card>
</template>
<script>
import { mapActions, mapGetters } from 'vuex'
import { get } from '@api/application'
import State from './State'
import configOptions from './option'
import {get as getVer, list as listVer} from '@api/config'
import { history } from '@api/savepoint'
import Conf from './Conf'
const Base64 = require('js-base64').Base64

configOptions.push(
  {
    key: '-p',
    name: 'parallelism'
  },
  {
    key: '-ys',
    name: 'yarnslots'
  }
)

export default {
  components: { State, Conf },
  data () {
    return {
      app: null,
      options: {},
      defaultConfigId: null,
      configVersions: null,
      savePoints: null,
      pagination: false,
      confVisiable: false
    }
  },

  computed: {
    columns () {
      return [
        {
          title: 'Job Id',
          dataIndex: 'appId'
        },
        {
          title: 'Conf Format',
          dataIndex: 'format',
          scopedSlots: { customRender: 'format' }
        },
        {
          title: 'Version',
          dataIndex: 'version',
          scopedSlots: { customRender: 'version' }
        },
        {
          title: 'Actived',
          dataIndex: 'actived',
          scopedSlots: { customRender: 'actived' }
        },
        {
          title: 'Operation',
          dataIndex: 'operation',
          key: 'operation',
          scopedSlots: { customRender: 'operation' },
          fixed: 'right',
          width: 150
        }
      ]
    },
    savePointsColumns () {
      return [
        {
          title: 'SavePoint',
          dataIndex: 'savePoint',
          width: '50%'
        },
        {
          title: 'Create Time',
          dataIndex: 'createTime',
          scopedSlots: { customRender: 'createTime' }
        },
        {
          title: 'Lastest',
          dataIndex: 'lastest',
          scopedSlots: { customRender: 'lastest' }
        }
      ]
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
  filters: {
    optionKey: function (title) {
      for (const opt of configOptions) {
        if (opt.name === title) {
          return opt.key
        }
      }
    }
  },
  methods: {
    ...mapActions(['CleanAppId']),
    ...mapGetters(['applicationId']),
    handleGet (appId) {
      get({ id: appId }).then((resp) => {
        this.app = resp.data
        this.handleListConfVersion()
        this.handleSavePoint()
        this.options = JSON.parse(this.app.options)
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },
    handleListConfVersion () {
      listVer({
        id: this.app.id
      }).then((resp) => {
        resp.data.forEach((value, index) => {
          if (value.actived) {
            this.defaultConfigId = value.id
          }
        })
        this.configVersions = resp.data
      })
    },
    handleSavePoint () {
      history({
        appId: this.app.id
      }).then((resp) => {
        this.savePoints = resp.data || []
      })
    },

    handleEditConfClose () {
      this.confVisiable = false
    },

    handleEditConfOk (value) {
      this.configOverride = value
    },

    handleConfDetail (record) {
      this.confVisiable = true
      getVer({
        id: record.id
      }).then((resp) => {
        const text = Base64.decode(resp.data.content)
        this.$refs.confEdit.set(text)
      })
    }

  }
}
</script>

<style scoped>
.desc-item {
  padding-top: 20px;
}

.desc-table {
  margin-top: unset !important;
}
</style>

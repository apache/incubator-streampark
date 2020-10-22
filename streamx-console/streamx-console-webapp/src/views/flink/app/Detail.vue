<template>
  <a-card :bordered="false" style="margin-top: 20px;">
    <a-descriptions v-if="app" bordered size="middle" layout="vertical">
      <template slot="title">
        <span class="app-bar">Application Info</span>
        <a-button type="primary" shape="circle" icon="arrow-left" @click="handleGoBack()" style="float: right;margin-top: -8px"></a-button>
        <a-divider style="margin-top: 5px;margin-bottom: -5px"></a-divider>
      </template>
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
          <a-icon type="clock-circle"/>
          {{ app.startTime }}
        </template>
      </a-descriptions-item>
      <a-descriptions-item v-if="app.endTime" label="End Time">
        <a-icon type="clock-circle"/>
        {{ app.endTime }}
      </a-descriptions-item>
      <a-descriptions-item v-if="app.duration" label="Duration">
        {{ app.duration | duration }}
      </a-descriptions-item>
      <a-descriptions-item label="Description" :span="3">
        {{ app.description }}
      </a-descriptions-item>
    </a-descriptions>
    <a-divider style="margin-top: 20px;margin-bottom: -17px"></a-divider>
    <a-tabs
      v-if="app"
      default-active-key="1"
      style="margin-top: 15px"
      :animated="animated"
      :tabBarGutter="tabBarGutter"
      @change="handleChangeTab">
      <a-tab-pane key="1" tab="Option" force-render>
        <a-descriptions bordered size="middle" layout="vertical">
          <a-descriptions-item v-for="(v,k) in options" :key="k">
            <template slot="label">
              {{ k | optionKey }} <span style="color: darkgrey">({{ k }})</span>
            </template>
            {{ v }}
          </a-descriptions-item>
        </a-descriptions>
      </a-tab-pane>
      <a-tab-pane key="2" tab="Configuration" v-if="app && app.appType == 1">
        <a-descriptions>
          <a-descriptions-item class="desc-item">
            <a-table
              ref="TableConf"
              :columns="column.conf"
              size="middle"
              rowKey="id"
              style="margin-top: -24px"
              :dataSource="configVersions"
              :pagination="pagination.config"
              :loading="pager.config.loading"
              class="detail-table">
              <template slot="format" slot-scope="text, record">
                <a-tag color="#2db7f5" v-if="record.format == 1">
                  yaml
                </a-tag>
                <a-tag color="#108ee9" v-if="record.format == 2">
                  properties
                </a-tag>
              </template>
              <template slot="version" slot-scope="text, record">
                <a-button type="primary" shape="circle" size="small" style="margin-right: 10px;">
                  {{ record.version }}
                </a-button>
              </template>
              <template slot="actived" slot-scope="text, record">
                <a-tag color="green" v-if="record.actived">current</a-tag>
              </template>
              <template slot="createTime" slot-scope="text, record">
                <a-icon type="clock-circle"/>
                {{ record.createTime }}
              </template>
              <template slot="operation" slot-scope="text, record">
                <a-icon
                  type="eye"
                  theme="twoTone"
                  twoToneColor="#4a9ff5"
                  @click="handleConfDetail(record)"
                  title="查看">
                </a-icon>
                <icon-font
                  v-if="configVersions.length>1"
                  type="icon-git-compare"
                  @click="handleCompare(record)"
                  title="比较">
                </icon-font>
                <a-popconfirm
                  v-if="!record.actived"
                  title="确定要删除吗?"
                  cancel-text="No"
                  ok-text="Yes"
                  @confirm="handleDeleteConf(record)">
                  <a-icon
                    type="delete"
                    v-permit="'conf:delete'"
                    theme="twoTone"
                    twoToneColor="#4a9ff5">
                  </a-icon>
                </a-popconfirm>
              </template>
            </a-table>
          </a-descriptions-item>
        </a-descriptions>
      </a-tab-pane>

      <a-tab-pane key="3" tab="Savepoints" v-if="app && savePoints && savePoints.length>0">
        <a-descriptions>
          <a-descriptions-item class="desc-item">
            <a-table
              ref="TableSavePoints"
              :columns="column.savePoints"
              size="middle"
              rowKey="id"
              style="margin-top: -24px"
              :dataSource="savePoints"
              :pagination="pagination.savePoints"
              :loading="pager.savePoints.loading"
              class="detail-table">
              <template slot="createTime" slot-scope="text, record">
                <a-icon type="clock-circle"/>
                {{ record.createTime }}
              </template>
              <template slot="lastest" slot-scope="text, record">
                <a-tag color="green" v-if="record.lastest">lastest</a-tag>
              </template>
              <template slot="operation" slot-scope="text, record">
                <a-icon
                  type="copy"
                  style="color:#4a9ff5"
                  v-clipboard:copy="record.savePoint"
                  v-clipboard:success="handleCopySuccess"
                  v-clipboard:error="handleCopyError">
                </a-icon>
                <a-popconfirm
                  title="确定要删除吗?"
                  cancel-text="No"
                  ok-text="Yes"
                  @confirm="handleDeleteSavePoint(record)">
                  <a-icon
                    type="delete"
                    v-permit="'savepoint:delete'"
                    theme="twoTone"
                    twoToneColor="#4a9ff5">
                  </a-icon>
                </a-popconfirm>
              </template>
            </a-table>
          </a-descriptions-item>
        </a-descriptions>
      </a-tab-pane>

      <a-tab-pane key="4" tab="Backups" v-if="app && backUpList && backUpList.length > 0">
        <a-descriptions>
          <a-descriptions-item>
            <a-table
              ref="TableBackUp"
              :columns="column.backUps"
              size="middle"
              rowKey="id"
              style="margin-top: -24px"
              :dataSource="backUpList"
              :pagination="pagination.backUp"
              :loading="pager.backUp.loading"
              class="detail-table"
              @change="handleTableChange">
              <template slot="createTime" slot-scope="text, record">
                <a-icon type="clock-circle"/>
                {{ record.createTime }}
              </template>
              <template slot="operation" slot-scope="text, record">
                <icon-font
                  type="icon-deploy"
                  v-permit="'backup:resume'"
                  style="color:#4a9ff5"
                  @click="handleResume(record)">
                </icon-font>
                <a-popconfirm
                  title="确定要删除吗?"
                  cancel-text="No"
                  ok-text="Yes"
                  @confirm="handleDeleteBackUp(record)">
                  <a-icon
                    type="delete"
                    v-permit="'backup:delete'"
                    theme="twoTone"
                    twoToneColor="#4a9ff5">
                  </a-icon>
                </a-popconfirm>
              </template>
            </a-table>
          </a-descriptions-item>
        </a-descriptions>
      </a-tab-pane>

      <a-tab-pane key="5" tab="Start Logs" v-if="app && backUpList && backUpList.length > 0">
        <a-descriptions>
          <a-descriptions-item>
            <a-table
              ref="TableStartLog"
              :columns="column.startLog"
              size="middle"
              rowKey="id"
              style="margin-top: -24px"
              :dataSource="startLogList"
              :pagination="pagination.startLog"
              :loading="pager.startLog.loading"
              @change="handleTableChange"
              class="detail-table">
              <template slot="startTime" slot-scope="text, record">
                <a-icon type="clock-circle"/>
                {{ record.startTime }}
              </template>
              <template slot="success" slot-scope="text, record">
                <a-tag class="start-state" color="#52c41a" v-if="record.success">SUCCESS</a-tag>
                <a-tag class="start-state" color="#f5222d" v-else>FAILED</a-tag>
              </template>
              <template slot="operation" slot-scope="text, record">
                <a-icon
                  v-if="!record.success"
                  type="eye"
                  theme="twoTone"
                  twoToneColor="#4a9ff5"
                  @click="handleException(record)"
                  title="查看">
                </a-icon>
              </template>
            </a-table>
          </a-descriptions-item>
        </a-descriptions>
      </a-tab-pane>

    </a-tabs>
    <conf
      ref="confEdit"
      @close="handleEditConfClose"
      @ok="handleEditConfOk"
      :visiable="confVisiable"
      :readOnly="true"></Conf>

    <a-modal v-model="compareVisible" on-ok="handleCompareOk" v-if="compareVisible">
      <template slot="title">
        <icon-font slot="icon" type="icon-git-compare" style="color: green"/>
        Compare Config
      </template>
      <template slot="footer">
        <a-button key="back" @click="handleCompareCancel">
          取消
        </a-button>
        <a-button key="submit" type="primary" @click="handleCompareOk">
          确定
        </a-button>
      </template>
      <a-form @submit="handleCompareOk" :form="formCompare">
        <a-form-item
          label="source version"
          :labelCol="{lg: {span: 7}, sm: {span: 7}}"
          :wrapperCol="{lg: {span: 16}, sm: {span: 4} }">
          <a-button type="primary" shape="circle" size="small" style="margin-right: 10px;">
            {{ compare.version }}
          </a-button>
          <a-icon type="clock-circle" style="color:darkgrey"/>
          <span style="color:darkgrey">{{ compare.createTime }}</span>
        </a-form-item>
        <a-form-item
          label="target version"
          :labelCol="{lg: {span: 7}, sm: {span: 7}}"
          :wrapperCol="{lg: {span: 16}, sm: {span: 4} }">
          <a-select @change="handleCompareTarget">
            <a-select-option
              v-for="(ver,index) in configVersions"
              :value="ver.id"
              v-if="compare.version !== ver.version"
              :key="index">
              <div style="padding-left: 5px">
                <a-button type="primary" shape="circle" size="small" style="margin-right: 10px;">
                  {{ ver.version }}
                </a-button>
                <a-tag color="green" style=";margin-left: 10px;" size="small" v-if="ver.actived">current</a-tag>
              </div>
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model="execOption.visible"
      width="80%"
      :bodyStyle="execOption.modalStyle"
      :destroyOnClose="execOption.modalDestroyOnClose"
      @ok="handleExpClose">
      <template slot="title">
        <a-icon type="code" style="color:RED"/>&nbsp; Exception Info
      </template>
      <template slot="footer">
        <a-button key="submit" type="primary" @click="handleExpClose">
          确定
        </a-button>
      </template>
      <textarea id="startExp" ref="startExp" class="startExp"></textarea>
    </a-modal>

  </a-card>
</template>
<script>
import { mapActions, mapGetters } from 'vuex'
import { get, backUps, startLog, removeBak } from '@api/application'
import State from './State'
import configOptions from './option'
import { get as getVer, list as listVer, remove as removeConf } from '@api/config'
import { history, remove as removeSp } from '@api/savepoint'
import Conf from './Conf'
import 'codemirror/lib/codemirror.css'
import { Icon } from 'ant-design-vue'
import notification from 'ant-design-vue/lib/notification'

const IconFont = Icon.createFromIconfontCN({
  scriptUrl: '//at.alicdn.com/t/font_2006309_bo5pga6ctds.js'
})
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
  components: { IconFont, State, Conf },
  data () {
    return {
      app: null,
      options: {},
      defaultConfigId: null,
      configVersions: null,
      savePoints: null,
      confVisiable: false,
      backUpList: null,
      compareVisible: false,
      formCompare: null,
      compare: null,
      startLogList: null,
      queryParams: {},
      animated: false,
      tabBarGutter: 0,
      pager: {
        config: {
          key: '2',
          info: null,
          loading: false
        },
        savePoints: {
          key: '3',
          info: null,
          loading: false
        },
        backUp: {
          key: '4',
          info: null,
          loading: false
        },
        startLog: {
          key: '5',
          info: null,
          loading: false
        }
      },
      pagination: {
        config: {
          pageSizeOptions: ['10', '20', '30', '40', '100'],
          defaultCurrent: 1,
          defaultPageSize: 10,
          showQuickJumper: true,
          showSizeChanger: true,
          showTotal: (total, range) => `显示 ${range[0]} ~ ${range[1]} 条记录，共 ${total} 条记录`
        },
        savePoints: {
          pageSizeOptions: ['10', '20', '30', '40', '100'],
          defaultCurrent: 1,
          defaultPageSize: 10,
          showQuickJumper: true,
          showSizeChanger: true,
          showTotal: (total, range) => `显示 ${range[0]} ~ ${range[1]} 条记录，共 ${total} 条记录`
        },
        backUp : {
          pageSizeOptions: ['10', '20', '30', '40', '100'],
          defaultCurrent: 1,
          defaultPageSize: 10,
          showQuickJumper: true,
          showSizeChanger: true,
          showTotal: (total, range) => `显示 ${range[0]} ~ ${range[1]} 条记录，共 ${total} 条记录`
        },
        startLog: {
          pageSizeOptions: ['10', '20', '30', '40', '100'],
          defaultCurrent: 1,
          defaultPageSize: 10,
          showQuickJumper: true,
          showSizeChanger: true,
          showTotal: (total, range) => `显示 ${range[0]} ~ ${range[1]} 条记录，共 ${total} 条记录`
        }
      },
      codeMirror: null,
      execOption: {
        modalStyle: {
          height: '600px',
          padding: '5px'
        },
        visible: false,
        modalDestroyOnClose: true,
        content: null
      },
      activeTab: '1',
      column: {
        conf: [
          {
            title: 'Version',
            dataIndex: 'version',
            scopedSlots: { customRender: 'version' }
          },
          {
            title: 'Conf Format',
            dataIndex: 'format',
            scopedSlots: { customRender: 'format' }
          },
          {
            title: 'Actived',
            dataIndex: 'actived',
            scopedSlots: { customRender: 'actived' }
          },
          {
            title: 'Modify Time',
            dataIndex: 'createTime',
            scopedSlots: { customRender: 'createTime' }
          },
          {
            title: 'Operation',
            dataIndex: 'operation',
            key: 'operation',
            scopedSlots: { customRender: 'operation' },
            fixed: 'right',
            width: 150
          }
        ],
        savePoints: [
          {
            title: 'SavePoint',
            dataIndex: 'savePoint',
            width: '50%'
          },
          {
            title: 'Trigger Time',
            dataIndex: 'createTime',
            scopedSlots: { customRender: 'createTime' }
          },
          {
            title: 'Lastest',
            dataIndex: 'lastest',
            scopedSlots: { customRender: 'lastest' }
          },
          {
            title: 'Operation',
            dataIndex: 'operation',
            key: 'operation',
            scopedSlots: { customRender: 'operation' },
            fixed: 'right',
            width: 150
          }
        ],
        backUps: [
          {
            title: 'Save Path',
            dataIndex: 'path',
            width: '40%'
          },
          {
            title: 'Description',
            dataIndex: 'description',
            width: '30%'
          },
          {
            title: 'Backup Time',
            dataIndex: 'createTime',
            scopedSlots: { customRender: 'createTime' }
          },
          {
            title: 'Operation',
            dataIndex: 'operation',
            key: 'operation',
            scopedSlots: { customRender: 'operation' },
            fixed: 'right',
            width: 150
          }
        ],
        startLog: [
          {
            title: 'Application Id',
            dataIndex: 'yarnAppId',
            width: '40%'
          },
          {
            title: 'Start Statue',
            dataIndex: 'success',
            scopedSlots: { customRender: 'success' }
          },
          {
            title: 'Start Time',
            dataIndex: 'startTime',
            scopedSlots: { customRender: 'startTime' }
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
      }
    }
  },

  beforeMount () {
    this.formCompare = this.$form.createForm(this)
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
        if (!this.app) {
          this.app = resp.data
          this.options = JSON.parse(this.app.options)
          this.handleConfig()
          this.handleSavePoint()
          this.handleBackUps()
          this.handleStartLog()
        } else {
          this.app = resp.data
          this.options = JSON.parse(this.app.options)
        }
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },
    handleConfig () {
      const params = {
        appId: this.app.id
      }
      if (this.pager.config.info) {
        // 如果分页信息不为空，则设置表格当前第几页，每页条数，并设置查询分页参数
        this.$refs.TableConf.pagination.current = this.pager.config.info.current
        this.$refs.TableConf.pagination.pageSize = this.pager.config.info.pageSize
        params.pageSize = this.pager.config.info.pageSize
        params.pageNum = this.pager.config.info.current
      } else {
        // 如果分页信息为空，则设置为默认值
        params.pageSize = this.pagination.config.defaultPageSize
        params.pageNum = this.pagination.config.defaultCurrent
      }
      this.handlePagerLoading()
      listVer({ ...params }).then((resp) => {
        const pagination = { ...this.pagination.config }
        pagination.total = parseInt(resp.data.total)
        resp.data.records.forEach((value, index) => {
          if (value.actived) {
            this.defaultConfigId = value.id
          }
        })
        this.configVersions = resp.data.records
        this.pagination.config = pagination
        this.pager.config.loading = false

      })
    },
    handleSavePoint () {
      const params = {
        appId: this.app.id
      }
      if (this.pager.savePoints.info) {
        // 如果分页信息不为空，则设置表格当前第几页，每页条数，并设置查询分页参数
        this.$refs.TableSavePoints.pagination.current = this.pager.savePoints.info.current
        this.$refs.TableSavePoints.pagination.pageSize = this.pager.savePoints.info.pageSize
        params.pageSize = this.pager.savePoints.info.pageSize
        params.pageNum = this.pager.savePoints.info.current
      } else {
        // 如果分页信息为空，则设置为默认值
        params.pageSize = this.pagination.savePoints.defaultPageSize
        params.pageNum = this.pagination.savePoints.defaultCurrent
      }
      this.handlePagerLoading()
      history({ ...params }).then((resp) => {
        const pagination = { ...this.pagination.savePoints }
        pagination.total = parseInt(resp.data.total)
        this.savePoints = resp.data.records
        this.pagination.savePoints = pagination
        this.pager.savePoints.loading = false
      })
    },

    handleBackUps () {
      const params = {
        appId: this.app.id
      }
      if (this.pager.backUp.info) {
        // 如果分页信息不为空，则设置表格当前第几页，每页条数，并设置查询分页参数
        this.$refs.TableBackUp.pagination.current = this.pager.backUp.info.current
        this.$refs.TableBackUp.pagination.pageSize = this.pager.backUp.info.pageSize
        params.pageSize = this.pager.backUp.info.pageSize
        params.pageNum = this.pager.backUp.info.current
      } else {
        // 如果分页信息为空，则设置为默认值
        params.pageSize = this.pagination.backUp.defaultPageSize
        params.pageNum = this.pagination.backUp.defaultCurrent
      }
      this.handlePagerLoading()
      backUps({ ...params }).then((resp) => {
        const pagination = { ...this.pagination.backUp }
        pagination.total = parseInt(resp.data.total)
        this.backUpList = resp.data.records
        this.pagination.backUp = pagination
        this.pager.backUp.loading = false
      })
    },

    handleStartLog () {
      const params = {
        appId: this.app.id
      }
      if (this.pager.startLog.info) {
        // 如果分页信息不为空，则设置表格当前第几页，每页条数，并设置查询分页参数
        this.$refs.TableStartLog.pagination.current = this.pager.startLog.info.current
        this.$refs.TableStartLog.pagination.pageSize = this.pager.startLog.info.pageSize
        params.pageSize = this.pager.startLog.info.pageSize
        params.pageNum = this.pager.startLog.info.current
      } else {
        // 如果分页信息为空，则设置为默认值
        params.pageSize = this.pagination.startLog.defaultPageSize
        params.pageNum = this.pagination.startLog.defaultCurrent
      }
      this.handlePagerLoading()
      startLog({ ...params }).then((resp) => {
        const pagination = { ...this.pagination.startLog }
        pagination.total = parseInt(resp.data.total)
        this.startLogList = resp.data.records
        this.pagination.startLog = pagination
        this.pager.startLog.loading = false
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
    },

    handleDeleteSavePoint (record) {
      removeSp({
        id: record.id
      }).then((resp) => {
        this.handleSavePoint()
      })
    },

    handleDeleteBackUp (record) {
      removeBak({
        id: record.id
      }).then((resp) => {
        this.handleBackUps()
      })
    },

    handleDeleteConf (record) {
      removeConf({
        id: record.id
      }).then((resp) => {
        this.handleConfig()
      })
    },

    handleCopySuccess () {
      notification.success({
        message: '复制成功',
        description: '该SavePoint路径已经复制到剪切板'
      })
    },

    handleCopyError () {
      notification.error({
        message: '复制失败',
        description: '该SavePoint路径复制失败'
      })
    },

    handleCompare (record) {
      this.compareVisible = true
      this.compare = {
        id: record.id,
        version: record.version,
        createTime: record.createTime
      }
    },

    handleCompareTarget (v) {
      this.compare.target = v
    },

    handleCompareCancel () {
      this.compareVisible = false
    },

    handleCompareOk () {
      getVer({
        id: this.compare.id
      }).then((resp) => {
        const conf1 = Base64.decode(resp.data.content)
        getVer({
          id: this.compare.target
        }).then((resp) => {
          const conf2 = Base64.decode(resp.data.content)
          this.confVisiable = true
          this.$refs.confEdit.compact(conf1, conf2)
          this.handleCompareCancel()
        })
      })
    },

    handleResume (record) {
    },

    handleException (record) {
      this.execOption.visible = true
      this.execOption.content = record.exception
      this.$nextTick(() => {
        this.handleCodeMirror()
      })
    },

    handleExpClose () {
      this.execOption.visible = false
    },

    handleGoBack () {
      this.$router.back(-1)
    },

    handleTableChange (pagination, filters, sorter) {
      // 将这两个参数赋值给Vue data，用于后续使用
      switch (this.activeTab) {
        case '2':
          this.pager.config.info = pagination
          this.handleConfig()
          break
        case '3':
          this.pager.savePoints.info = pagination
          this.handleSavePoint()
          break
        case '4':
          this.pager.backUp.info = pagination
          this.handleBackUps()
          break
        case '5':
          this.pager.startLog.info = pagination
          this.handleStartLog()
          break
      }
    },

    handleCodeMirror () {
      this.codeMirror = CodeMirror.fromTextArea(document.querySelector('.startExp'), {
        tabSize: 8,
        styleActiveLine: true,
        lineNumbers: true,
        line: true,
        foldGutter: true,
        styleSelectedText: true,
        matchBrackets: true,
        showCursorWhenSelecting: true,
        extraKeys: { 'Ctrl': 'autocomplete' },
        lint: true,
        readOnly: true,
        autoMatchParens: true,
        indentWithTabs: true,
        smartIndent: true,
        cursorHeight: 1, // 光标高度
        autoRefresh: true,
        modes: [
          {
            value: 'x-java',
            label: 'Java'
          },
          {
            value: 'x-scala',
            label: 'Scala'
          }
        ],
        theme: 'default',	// 设置主题
        gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter', 'CodeMirror-lint-markers']
      })
      this.codeMirror.setSize('auto', '600px')
      this.$nextTick(() => {
        this.codeMirror.setValue(this.execOption.content)
        setTimeout(() => {
          this.codeMirror.refresh()
        }, 1)
      })
    },

    handleChangeTab (key) {
      this.activeTab = key
    },

    handlePagerLoading () {
      for (const k in this.pager) {
        this.pager[k]['loading'] = this.pager[k]['key'] === this.activeTab
      }
    }
  }
}
</script>

<style scoped>
.desc-item {
  padding-top: 20px;
}

.start-state {
  border-radius: 0;
  font-weight: 700;
  font-size: 13px;
  text-align: center;
  padding: 0 4px;
  cursor: default;
}

>>> .ant-tabs-nav .ant-tabs-tab-active {
  font-weight: unset !important;
  background-color: #f0f2f5;
}

>>> .ant-tabs-nav .ant-tabs-tab {
  margin: 0 32px 0 0;
  padding: 8px 15px;
}

.app-bar {
  background-color: #f0f2f5;
  color: rgba(0, 0, 0, 0.65);
  height: 100%;
  font-weight: normal;
  margin: 0 32px 0 0;
  padding: 8px 12px;
}

>>> .ant-descriptions-bordered.ant-descriptions-middle .ant-descriptions-item-content {
  padding: 10px 24px;
}

.detail-table {
  margin-top: unset !important;
}

.detail-table >>> .ant-table-thead > tr > td, .detail-table >>> .ant-table-tbody > tr > td {
  padding: 9px 9px !important;
}

</style>
